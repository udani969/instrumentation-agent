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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.AgentHolder;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.das.javaagent.exception.InstrumentationAgentException;
import org.wso2.das.javaagent.schema.AgentConnection;

import java.io.File;
import java.util.Map;

/**
 * AgentPublisherHolder handles the publishing of events to DAS server.
 */
public class AgentPublisherHolder {

    private static final Log log = LogFactory.getLog(AgentPublisherHolder.class);
    private static AgentPublisherHolder instance = null;
    private String streamId;
    private DataPublisher dataPublisher;
    protected final String THRIFT_AGENT_TYPE = "Thrift";

    protected AgentPublisherHolder() {
    }

    public static AgentPublisherHolder getInstance() {
        if (instance == null) {
            instance = new AgentPublisherHolder();
        }
        return instance;
    }

    public void addAgentConfiguration(AgentConnection agentConnection) throws InstrumentationAgentException {
        locateConfigurationFiles();
        setupAgentPublisher(agentConnection, agentConnection.getStreamName(), agentConnection.getStreamVersion());
        if(log.isDebugEnabled()){
            log.debug("Publisher created successfully");
        }
    }

    private void setupAgentPublisher(AgentConnection agentConnection, String agentStream, String version)
            throws InstrumentationAgentException {
        try {
            this.dataPublisher = new DataPublisher(THRIFT_AGENT_TYPE, agentConnection.getReceiverURL(),
                    agentConnection.getAuthURL(), agentConnection.getUsername(), agentConnection.getPassword());
        } catch (DataEndpointException | DataEndpointAgentConfigurationException | DataEndpointAuthenticationException
                | TransportException e) {
            throw new InstrumentationAgentException("Failed to establish connection with server : " + e.getMessage(), e);
        } catch (DataEndpointConfigurationException e) {
            throw new InstrumentationAgentException("Failed to initialize agent publisher : " + e.getMessage(), e);
        }
        this.streamId = DataBridgeCommonsUtils.generateStreamId(agentStream, version);
    }

    public void locateConfigurationFiles() {
        InstrumentationDataHolder dataHolder = InstrumentationDataHolder.getInstance();
        String trustStorePath = dataHolder.getConfigFilePathHolder();
        if (dataHolder.isCarbonProduct()) {
            trustStorePath += File.separator + "repository" + File.separator + "conf" + File.separator;
        }
        trustStorePath += "client-truststore.jks";
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        if (!dataHolder.isCarbonProduct()) {
            AgentHolder.setConfigPath(dataHolder.getConfigFilePathHolder() + "data-agent-config.xml");
        }
    }

    /**
     * Publish the obtained queries to DAS using normal publish method which passes
     * only metadata, correlation data and payload data. Five parameters concatenated in
     * payload data (scenario name, class name, method name, instrumentation location, duration)
     * would be separated into an object array.
     *
     * @param timeStamp current timestamp
     * @param payloadData string containing payload data values
     */
    public void publishEvents(long timeStamp, long correlationData, String payloadData) {
        Object[] payload = payloadData.split(":");
        Object[] correlation = { correlationData };
        dataPublisher.publish(streamId, timeStamp, null, correlation, payload, null);
    }

    /**
     * Overloaded the above publishEvents method, with extra parameter to pass
     * key,value pairs obtained in situations with extra attributes.
     *
     * @param timeStamp current time in milli seconds
     * @param payloadData string containing payload data values
     * @param arbitraryMap map containing <key,value> pairs of parameters
     */
    public void publishEvents(long timeStamp, long correlationData, String payloadData, Map<String, String> arbitraryMap) {
        Object[] payload = payloadData.split(":");
        Object[] correlation = { correlationData };
        dataPublisher.publish(streamId, timeStamp, null, correlation, payload, arbitraryMap);
    }
}
