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

package org.wso2.das.javaagent.instrumentation;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.databridge.agent.AgentHolder;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.AuthenticationException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.das.javaagent.schema.*;
import org.wso2.das.javaagent.worker.AgentConnectionWorker;

import java.io.*;
import java.net.*;
import java.util.*;

public class AgentPublisher {
    private static String agentStream;
    private static String version;
    private static String streamId;
    private static DataPublisher dataPublisher;
    private static Set<String> currentSchemaFieldsSet = new HashSet<String>();
    private static List<String> arbitraryFields = new ArrayList<String>();
    private static AgentConnection agentConnection;
    private static Map<String,List<InstrumentationClassData>> classMap = new HashMap<String, List<InstrumentationClassData>>();
    protected static final String CARBON_HOME = org.wso2.carbon.utils.CarbonUtils.getCarbonHome();
    protected static final String THRIFT_AGENT_TYPE = "Thrift";

    public AgentPublisher(AgentConnection agentConnection)
            throws DataEndpointConfigurationException, DataEndpointException,
            DataEndpointAgentConfigurationException, AuthenticationException,
            SocketException, UnknownHostException, TransportException, MalformedURLException,
            DataEndpointAuthenticationException {

        AgentPublisher.agentStream = agentConnection.getStreamName();
        AgentPublisher.version = agentConnection.getStreamVersion();
        AgentPublisher.agentConnection = agentConnection;

        setProperties();

        AgentPublisher.dataPublisher = new DataPublisher(THRIFT_AGENT_TYPE, agentConnection.getReceiverURL(),
                agentConnection.getAuthURL(), agentConnection.getUsername(), agentConnection.getPassword());
        AgentPublisher.streamId = DataBridgeCommonsUtils.generateStreamId(agentStream, version);

        Thread connectionWorker = new Thread(new AgentConnectionWorker());
        connectionWorker.start();

    }

    /**
     * @return Fields of current schema as a set
     */
    public static Set<String> getCurrentSchemaFieldsSet() {
        return currentSchemaFieldsSet;
    }

    public static void setCurrentSchemaFieldsSet(String field) {
        AgentPublisher.currentSchemaFieldsSet.add(field);
    }

    /**
     * @return Arbitrary fields read from the configuration file as a list
     */
    public static List<String> getArbitraryFields() {
        return arbitraryFields;
    }

    public static void setArbitraryFields(String arbitraryField) {
        AgentPublisher.arbitraryFields.add(arbitraryField);
    }

    /**
     * @return Map containing <className, instrumentation details list>
     */
    public static Map<String, List<InstrumentationClassData>> getClassMap() {
        return classMap;
    }

    public static void setClassMap(Map<String, List<InstrumentationClassData>> classMap) {
        AgentPublisher.classMap = classMap;
    }

    public static AgentConnection getAgentConnection() {
        return agentConnection;
    }

    public static void setProperties(){
        System.setProperty("javax.net.ssl.trustStore",
                CARBON_HOME + "/repository/conf/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        AgentHolder.setConfigPath(getDataAgentConfigPath());
    }

    public static String getDataAgentConfigPath() {
        File filePath = new File("repository" + File.separator + "conf" + File.separator + "data-bridge");
        return filePath.getAbsolutePath() + File.separator + "data-agent-config.xml";
    }

    /**
     * Publish the obtained queries to DAS using normal publish method which passes
     * only metadata, correlation data and payload data. Five parameters concatenated in
     * payload data (scenario name, class name, method name, instrumentation location, duration)
     * would be separated into an object array.
     * @param timeStamp current timestamp
     * @param payloadData string containing payload data values
     * @throws FileNotFoundException
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static void publishEvents(long timeStamp, String payloadData)
            throws IOException, ParseException, AuthenticationException, DataEndpointException,
            DataEndpointConfigurationException, DataEndpointAuthenticationException,
            DataEndpointAgentConfigurationException, TransportException {
        Object[] payload;
        payload = payloadData.split(":");
        dataPublisher.publish(streamId, timeStamp, null, null, payload, null);
    }

    /**
     * Overloaded the above publishEvents method, with extra parameter to pass
     * key,value pairs obtained in situations with extra attributes.
     * @param timeStamp current time in milli seconds
     * @param payloadData string containing payload data values
     * @param arbitraryMap map containing <key,value> pairs of parameters
     * @throws FileNotFoundException
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static void publishEvents(long timeStamp, String payloadData,
                                     Map<String,String> arbitraryMap)
            throws IOException, ParseException, AuthenticationException, DataEndpointException,
            DataEndpointConfigurationException, DataEndpointAuthenticationException,
            DataEndpointAgentConfigurationException, TransportException {
        Object[] payload;
        payload = payloadData.split(":");
        dataPublisher.publish(streamId, timeStamp, null, null, payload, arbitraryMap);
    }

    /**
     * Obtain the current schema of the given table. Filter the column names of currently
     * available fields. For each field read from the configuration file, check against the
     * current filtered fields set. Add only the new fields to the schema. Finally return the
     * modified schema using REST API
     * @param connectionUrl url to connect to server
     * @param username username of the server
     * @param password password of the server
     * @param arbitraryFields List of fields read from the configuration file,
     *                        which need to be inserted in schema
     * @throws IOException
     * @throws ParseException
     */
    public static void updateCurrentSchema(String connectionUrl, String username,
                                           String password, List<String> arbitraryFields)
            throws IOException, ParseException {
        String currentSchema = AgentPublisher.getCurrentSchema(connectionUrl, username, password);
        AgentPublisher.filterCurrentSchemaFields(currentSchema);
        String modifiedSchema = AgentPublisher.addArbitraryFieldsToSchema(currentSchema, arbitraryFields);
        if(!modifiedSchema.equals(currentSchema)){
            AgentPublisher.setModifiedSchema(connectionUrl, username, password, modifiedSchema);
        }
    }

    /**
     * Method to retrieve the current schema of the given table
     * @param connectionUrl https request to sent to the REST API
     * @param username Username of the server
     * @param password Password of the server
     * @return Current schema
     * @throws IOException
     */
    public static String getCurrentSchema(String connectionUrl, String username, String password)
            throws IOException {
        String currentSchema = "";

        try {
            URL url = new URL(connectionUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            String authString = username + ":" + password;
            String authStringEnc = new String(Base64.encodeBase64(authString.getBytes()));
            conn.setRequestProperty("Authorization", "Basic " + authStringEnc);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            currentSchema = br.readLine();
        }catch (Exception e){
            e.printStackTrace();
        }
        return currentSchema;
    }

    /**
     * Modify current schema by adding relevant definition of new fields
     * @param currentSchema currentSchema
     * @param arbitraryFields list of all fields read from configuration file
     * @return modified schema to update on server
     */
    public static String addArbitraryFieldsToSchema(String currentSchema, List<String> arbitraryFields){
        for(String arbitraryField : arbitraryFields) {
            if (!AgentPublisher.getCurrentSchemaFieldsSet().contains(arbitraryField)) {
                int insertionPoint = currentSchema.indexOf("},\"primaryKeys\":[", 0);
                String columnSection = currentSchema.substring(0, insertionPoint);
                String primaryKeySection = currentSchema.substring(insertionPoint);
                currentSchema = columnSection + generateSchemaForNewField(arbitraryField) + primaryKeySection;
                AgentPublisher.getCurrentSchemaFieldsSet().add(arbitraryField);
            }
        }
        return currentSchema;
    }

    private static String generateSchemaForNewField(String field){
        StringBuilder builder = new StringBuilder();
        builder.append(",\"");
        builder.append(field);
        builder.append("\":{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}");
        return builder.toString();
    }

    /**
     * Update the current schema of the persisted table using REST API of DAS
     * @param connectionUrl https request to sent to the REST API
     * @param username Username of the server
     * @param password Password of the server
     * @param newSchema modified schema use to update currentSchema
     */
    public static void setModifiedSchema(String connectionUrl, String username,
                                         String password, String newSchema){
        try{
            URL url = new URL(connectionUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtain the current schema and obtain the key set of schema using JSON parser
     * @param currentSchema currentSchema of the table
     * @throws ParseException
     */
    @SuppressWarnings("unchecked")
    public static void filterCurrentSchemaFields(String currentSchema) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(currentSchema);
        JSONObject keys = (JSONObject)json.get("columns");
        Set keySet = keys.keySet();
        Iterator i = keySet.iterator();
        while(i.hasNext()) {
            AgentPublisher.setCurrentSchemaFieldsSet(String.valueOf(i.next()));
        }
    }

    public static String generateConnectionURL(AgentConnection agentConnection){
        return "https://"+ agentConnection.getHostName() + ":"
                + agentConnection.getServicePort() + "/analytics/tables/"
                + agentConnection.getTableName() + "/schema";
    }
}
