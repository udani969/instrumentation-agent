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
 * /
 */

package org.wso2.das.javaagent.instrumentation;

import org.wso2.das.javaagent.schema.AgentConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InstrumentationDataHolder holds set of data structures and
 * configuration file paths required for the agent operations.
 */
public class InstrumentationDataHolder {

    private static InstrumentationDataHolder instance = null;
    private List<String> arbitraryFields = new ArrayList<>();
    private Map<String, List<InstrumentationClassData>> classMap = new HashMap<>();
    private AgentConnection agentConnection;
    private String configFilePathHolder;
    private boolean carbonProduct;

    protected InstrumentationDataHolder() {
    }

    public static InstrumentationDataHolder getInstance() {
        if (instance == null) {
            instance = new InstrumentationDataHolder();
        }
        return instance;
    }

    /**
     * @return Arbitrary fields read from the configuration file as a list
     */
    public List<String> getArbitraryFields() {
        return arbitraryFields;
    }

    public void setArbitraryFields(String arbitraryField) {
        arbitraryFields.add(arbitraryField);
    }

    /**
     * @return Map containing <className, instrumentation details list>
     */
    public Map<String, List<InstrumentationClassData>> getClassMap() {
        return classMap;
    }

    public void setClassMap(Map<String, List<InstrumentationClassData>> classMap) {
        this.classMap = classMap;
    }

    public AgentConnection getAgentConnection() {
        return agentConnection;
    }

    public void setAgentConnection(AgentConnection agentConnection) {
        this.agentConnection = agentConnection;
    }

    public String getConfigFilePathHolder() {
        return configFilePathHolder;
    }

    public void setConfigFilePathHolder(String[] agentArg) {
        if (agentArg[0].equals("true")) {
            this.configFilePathHolder = org.wso2.carbon.utils.CarbonUtils.getCarbonHome();
            carbonProduct = true;
        }
        if (agentArg[0].equals("false")) {
            this.configFilePathHolder = agentArg[1];
            carbonProduct = false;
        }
    }

    public boolean isCarbonProduct() {
        return this.carbonProduct;
    }
}
