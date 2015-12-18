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
import org.apache.log4j.PropertyConfigurator;
import org.wso2.das.javaagent.exception.InstrumentationAgentException;
import org.wso2.das.javaagent.schema.*;
import org.wso2.das.javaagent.worker.AgentConnectionWorker;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InstrumentingAgent is executed before the rest of the instrumenting application execute.
 * It fill a Map using the configuration details read from the configuration file.
 * Map contains lists with instrumentation method details mapped against respective class names.
 * Parallel to the creation of Map it will create a separate list including all
 * the unique key names used in the configuration file.
 * Eg: <parameterName key='key_1'></parameterName> insert '_key_1' as a list entry.
 * Once, filtering is completed, start a seperate thread 'AgentConnectionWorker'
 * to modify the current schema of the DAS table. Finally, add a transformer to start the
 * instrumentation of class files.
 */
public class InstrumentingAgent {

    private static final Log log = LogFactory.getLog(InstrumentingAgent.class);
    private Map<String, List<InstrumentationClassData>> classMap = new HashMap<>();

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        InstrumentingAgent agent = new InstrumentingAgent();
        InstrumentationDataHolder instDataHolder = InstrumentationDataHolder.getInstance();
        agent.setConfigurationFilePath(agentArgs);
        agent.setLoggingConfiguration();
        log.debug("Starting Instrumentation javaagent");
        try {
            File file = new File(createAgentConfigFilepath(instDataHolder));
            JAXBContext jaxbContext = JAXBContext.newInstance(InstrumentationAgent.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            InstrumentationAgent instAgent = (InstrumentationAgent) jaxbUnmarshaller.unmarshal(file);
            AgentConnection agentConnection = instAgent.getAgentConnection();
            if (agentConnection != null) {
                List<Scenario> scenarios = instAgent.getScenarios();
                for (Scenario scenario : scenarios) {
                    List<InstrumentationClass> instClasses = scenario.getinstrumentationClasses();
                    for (InstrumentationClass instClass : instClasses) {
                        agent.processClassData(agent, scenario, instClass);
                    }
                }
                instDataHolder.setAgentConnection(agentConnection);
                instDataHolder.setClassMap(agent.classMap);
                AgentPublisherHolder.getInstance().addAgentConfiguration(agentConnection);
                Thread connectionWorker = new Thread(new AgentConnectionWorker());
                connectionWorker.start();
                if (log.isDebugEnabled()) {
                    log.debug("Add transformer to instrumenting classes.");
                }
                instrumentation.addTransformer(new InstrumentationClassTransformer());
            }
        } catch (InstrumentationAgentException | JAXBException e) {
            if (log.isDebugEnabled()) {
                log.debug("InstrumentationAgent failed due to : " + e.getMessage());
            }
        }
    }

    private static String createAgentConfigFilepath(InstrumentationDataHolder instDataHolder) {
        String filePath = instDataHolder.getConfigFilePathHolder();
        if (instDataHolder.isCarbonProduct()) {
            filePath += File.separator + "repository" + File.separator + "conf" + File.separator + "javaagent"
                    + File.separator;
        }
        filePath += "inst-agent-config.xml";
        return filePath;
    }

    private void setConfigurationFilePath(String agentArgs) {
        String[] agentArg = agentArgs.split(",");
        InstrumentationDataHolder.getInstance().setConfigFilePathHolder(agentArg);
    }

    private void processClassData(InstrumentingAgent agent, Scenario scenario, InstrumentationClass instrumentationClass) {
        List<InstrumentationClassData> methodList = new ArrayList<>();
        methodList = fillMethodList(methodList, instrumentationClass, scenario.getScenarioName());
        if (agent.classMap.keySet().contains(instrumentationClass.getClassName())) {
            methodList = fillMethodList(agent.classMap.get(instrumentationClass.getClassName()), instrumentationClass,
                    scenario.getScenarioName());
        }
        agent.classMap.put(instrumentationClass.getClassName(), methodList);
        // obtain required fields to update schema
        initializeArbitraryFieldList(instrumentationClass);
    }

    /**
     * For a given arraylist which is mapped to a specific className in classMap, create new objects
     * using instrumentation method details and scenario names and fill the list.
     * 
     * @param classData List of type Instrumentation Class data. It can be new list or halfway filled list.
     * @param instrumentationClass InstrumentationClass object for currently processing className.
     * @param scenarioName Name of the scenario processed.
     * @return Filled array list of type InstrumentationClassData.
     */
    private List<InstrumentationClassData> fillMethodList(List<InstrumentationClassData> classData,
            InstrumentationClass instrumentationClass, String scenarioName) {
        List<InstrumentationMethod> instrumentationMethods = instrumentationClass.getInstrumentationMethods();
        for (InstrumentationMethod instrumentationMethod : instrumentationMethods) {
            classData.add(new InstrumentationClassData(scenarioName, instrumentationMethod));
        }
        return classData;
    }

    /**
     * Fill the arbitraryFields list using parameters read from the configuration file.
     * 
     * @param instClass instrumentationClass object generated from unmarshalling.
     */
    public void initializeArbitraryFieldList(InstrumentationClass instClass) {
        List<InstrumentationMethod> instMethods = instClass.getInstrumentationMethods();
        for (InstrumentationMethod instMethod : instMethods) {
            InsertBefore insertBefore = instMethod.getInsertBefore();
            filterInsertBefore(insertBefore);
            List<InsertAt> insertAts = instMethod.getInsertAts();
            filterInsertAts(insertAts);
            InsertAfter insertAfter = instMethod.getInsertAfter();
            filterInsertAfter(insertAfter);
        }
    }

    private void filterInsertBefore(InsertBefore insertBefore) {
        if (insertBefore != null && !insertBefore.getParameterNames().isEmpty()) {
            filterParameterNames(insertBefore.getParameterNames());
        }
    }

    private void filterInsertAfter(InsertAfter insertAfter) {
        if (insertAfter != null && !insertAfter.getParameterNames().isEmpty()) {
            filterParameterNames(insertAfter.getParameterNames());
        }
    }

    private void filterInsertAts(List<InsertAt> insertAts) {
        if (insertAts != null && !insertAts.isEmpty()) {
            for (InsertAt insertAt : insertAts) {
                List<ParameterName> parameterNames = insertAt.getParameterNames();
                filterParameterNames(parameterNames);
            }
        }
    }

    private void filterParameterNames(List<ParameterName> parameterNames) {
        InstrumentationDataHolder agentDataHolder = InstrumentationDataHolder.getInstance();
        if (!parameterNames.isEmpty()) {
            for (ParameterName parameterName : parameterNames) {
                /*
                 * When setting setting the schema of the table we have to add a '_'
                 * before each table name. But when publishing data in map, use key name
                 * given in configuration file without '_'
                 */
                if (!agentDataHolder.getArbitraryFields().contains("_" + parameterName.getKey())) {
                    agentDataHolder.setArbitraryFields("_" + parameterName.getKey());
                }
            }
        }
    }

    public void setLoggingConfiguration() {
        InstrumentationDataHolder instDataHolder = InstrumentationDataHolder.getInstance();
        String log4jConfPath = instDataHolder.getConfigFilePathHolder();
        if (instDataHolder.isCarbonProduct()) {
            log4jConfPath += File.separator + "repository" + File.separator + "conf" + File.separator + "javaagent";
        }
        log4jConfPath += File.separator + "log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);
    }
}
