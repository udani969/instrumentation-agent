/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.das.javaagent.worker;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.das.javaagent.exception.InstrumentationAgentException;
import org.wso2.das.javaagent.instrumentation.InstrumentationDataHolder;
import org.wso2.das.javaagent.schema.AgentConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AgentConnectionWorker implements Runnable {
    private Set<String> currentSchemaFieldsSet = new HashSet<>();
    private static final Log log = LogFactory.getLog(AgentConnectionWorker.class);

    public void run() {
        InstrumentationDataHolder instDataHolder = InstrumentationDataHolder.getInstance();
        waitForConnection(instDataHolder.getAgentConnection().getHostName(),
                Integer.parseInt(instDataHolder.getAgentConnection().getServicePort()));
        if (!instDataHolder.getArbitraryFields().isEmpty()) {
            try {
                Thread.sleep(5000);
                updateCurrentSchema(instDataHolder);
            } catch (InstrumentationAgentException | InterruptedException e) {
                if (log.isDebugEnabled()) {
                    log.debug("AgentConnectionWorker : " + e.getMessage());
                }
            }
        }
    }

    /**
     * @return Fields of current schema as a set.
     */
    public Set<String> getCurrentSchemaFieldsSet() {
        return currentSchemaFieldsSet;
    }

    public void setCurrentSchemaFieldsSet(String field) {
        currentSchemaFieldsSet.add(field);
    }

    public void waitForConnection(String host, int port) {
        boolean connectionCheck = false;
        while (!connectionCheck) {
            Socket socket = null;
            try {
                socket = new Socket(host, port);
                connectionCheck = true;
            } catch (Exception e) {
                try {
                    connectionCheck = false;
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Obtain the current schema of the given table. Filter the column names of currently
     * available fields. For each field read from the configuration file, check against the
     * current filtered fields set. Add only the new fields to the schema. Finally return the
     * modified schema using REST API.
     *
     * @param instDataHolder Static instance of AgentPublisherHolder class.
     * @throws InstrumentationAgentException
     */
    public void updateCurrentSchema(InstrumentationDataHolder instDataHolder) throws InstrumentationAgentException {
        AgentConnection agentConnection = instDataHolder.getAgentConnection();
        String connectionUrl = generateConnectionURL(agentConnection);
        String currentSchema = getCurrentSchema(agentConnection, connectionUrl, agentConnection.getUsername(),
                agentConnection.getPassword());
        filterCurrentSchemaFields(currentSchema);
        String modifiedSchema = addArbitraryFieldsToSchema(currentSchema, instDataHolder.getArbitraryFields());
        if (!modifiedSchema.equals(currentSchema)) {
            setModifiedSchema(agentConnection, connectionUrl, agentConnection.getUsername(),
                    agentConnection.getPassword(), modifiedSchema);
        }
    }

    /**
     * Method to retrieve the current schema of the given table.
     * 
     * @param connectionUrl https request to sent to the REST API
     * @param username Username of the server
     * @param password Password of the server
     * @return Current schema
     * @throws InstrumentationAgentException
     */
    public String getCurrentSchema(AgentConnection agentConnection, String connectionUrl, String username,
            String password) throws InstrumentationAgentException {
        InputStreamReader inputStreamReader = null;
        HttpURLConnection conn = null;
        try {
            String currentSchema;
            URL url = new URL(connectionUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            String authString = username + ":" + password;
            String authStringEnc = new String(Base64.encodeBase64(authString.getBytes()));
            conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            inputStreamReader = new InputStreamReader((conn.getInputStream()));
            BufferedReader br = new BufferedReader(inputStreamReader);
            currentSchema = br.readLine();
            return currentSchema;
        } catch (IOException e) {
            throw new InstrumentationAgentException("Failed to obtain Current Schema of table : "
                    + agentConnection.getTableName() + "." + e.getMessage(), e);
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                    conn.disconnect();
                } catch (IOException e) {
                    throw new InstrumentationAgentException(
                            "Connection exception occurred while trying to modify table schema : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Modify current schema by adding relevant definition of new fields.
     * 
     * @param currentSchema currentSchema
     * @param arbitraryFields list of all fields read from configuration file
     * @return modified schema to update on server
     */
    public String addArbitraryFieldsToSchema(String currentSchema, List<String> arbitraryFields) {
        for (String arbitraryField : arbitraryFields) {
            if (!getCurrentSchemaFieldsSet().contains(arbitraryField)) {
                int insertionPoint = currentSchema.indexOf("},\"primaryKeys\":[", 0);
                String columnSection = currentSchema.substring(0, insertionPoint);
                String primaryKeySection = currentSchema.substring(insertionPoint);
                currentSchema = columnSection + generateSchemaForNewField(arbitraryField) + primaryKeySection;
                getCurrentSchemaFieldsSet().add(arbitraryField);
            }
        }
        return currentSchema;
    }

    private String generateSchemaForNewField(String field) {
        StringBuilder builder = new StringBuilder();
        builder.append(",\"");
        builder.append(field);
        builder.append("\":{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}");
        return builder.toString();
    }

    /**
     * Update the current schema of the persisted table using REST API of DAS.
     * 
     * @param connectionUrl https request to sent to the REST API
     * @param username Username of the server
     * @param password Password of the server
     * @param newSchema modified schema use to update currentSchema
     */
    public void setModifiedSchema(AgentConnection agentConnection, String connectionUrl, String username,
            String password, String newSchema) throws InstrumentationAgentException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(connectionUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            String authString = username + ":" + password;
            String authStringEnc = new String(Base64.encodeBase64(authString.getBytes()));
            conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
            OutputStream os = conn.getOutputStream();
            os.write(newSchema.getBytes());
            os.flush();
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
        } catch (IOException e) {
            throw new InstrumentationAgentException("Failed to update existing Schema of table : "
                    + agentConnection.getTableName() + "." + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Obtain the current schema and obtain the key set of schema using JSON parser.
     * 
     * @param currentSchema current schema of the persisted table
     * @throws InstrumentationAgentException
     */
    @SuppressWarnings("unchecked")
    public void filterCurrentSchemaFields(String currentSchema) throws InstrumentationAgentException {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(currentSchema);
            JSONObject keys = (JSONObject) json.get("columns");
            Set keySet = keys.keySet();
            Iterator i = keySet.iterator();
            while (i.hasNext()) {
                setCurrentSchemaFieldsSet(String.valueOf(i.next()));
            }
        } catch (ParseException e) {
            throw new InstrumentationAgentException("Failed to obtain fields in current schema : " + e.getMessage(), e);
        }
    }

    public String generateConnectionURL(AgentConnection agentConnection) {
        return "https://" + agentConnection.getHostName() + ":" + agentConnection.getServicePort()
                + "/analytics/tables/" + agentConnection.getTableName() + "/schema";
    }
}
