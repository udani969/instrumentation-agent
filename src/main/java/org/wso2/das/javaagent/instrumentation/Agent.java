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

import org.json.simple.parser.ParseException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.AuthenticationException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.das.javaagent.schema.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;



public class Agent {
//    private static final Log log = LogFactory.getLog(InstrumentationClassTransformer.class);
    public static void premain(String agentArgs, Instrumentation instrumentation) throws JAXBException,
            ClassNotFoundException, UnmodifiableClassException, DataEndpointException,
            IOException, DataEndpointConfigurationException,
            DataEndpointAuthenticationException, DataEndpointAgentConfigurationException,
            TransportException, AuthenticationException, ParseException {

        File file = new File(AgentPublisher.CARBON_HOME + "/repository/conf/javaagent/inst-agent-config.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(InstrumentationAgent.class);

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        InstrumentationAgent agent = (InstrumentationAgent) jaxbUnmarshaller.unmarshal(file);

        AgentConnection agentConnection = agent.getAgentConnection();
        if(agentConnection!=null) {
            AgentPublisher publisherObj = new AgentPublisher(agentConnection);
            List<Scenario> scenarios = agent.getScenarios();

            for (Scenario scenario : scenarios) {
                List<InstrumentationClass> instrumentationClasses = scenario.getinstrumentationClasses();
                for (InstrumentationClass instrumentationClass : instrumentationClasses){
                    if(AgentPublisher.getClassMap().keySet().contains(instrumentationClass.getClassName())){
                        Agent.fillMethodList(AgentPublisher.getClassMap().get(instrumentationClass.getClassName()),
                                instrumentationClass, scenario.getScenarioName());
                    }else{
                        List<InstrumentationClassData> newClass = new ArrayList<InstrumentationClassData>();
                        AgentPublisher.getClassMap().put(instrumentationClass.getClassName(),
                                Agent.fillMethodList(newClass, instrumentationClass, scenario.getScenarioName()));
                    }
                    AgentPublisher.initializeArbitraryFieldList(instrumentationClass);
                }
            }

            instrumentation.addTransformer(new InstrumentationClassTransformer());
        }

        System.out.println("Instrumentation complete");

    }

    /**
     * For a given arraylist which is mapped to a specific className in classMap, create new objects
     * using instrumentation method details and scenario names and fill the list.
     * @param classData List of type Instrumentation Class data. It can be new list or halfway filled list
     * @param instrumentationClass InstrumentationClass object for currently processing className
     * @param scenarioName Name of the scenario processed
     * @return Filled array list of type InstrumentationClassData
     */
    private static List<InstrumentationClassData> fillMethodList(List<InstrumentationClassData> classData,
                                  InstrumentationClass instrumentationClass, String scenarioName){
        List<InstrumentationMethod> instrumentationMethods = instrumentationClass.getInstrumentationMethods();
        for(InstrumentationMethod instrumentationMethod : instrumentationMethods){
            classData.add(new InstrumentationClassData(scenarioName, instrumentationMethod));
        }
        return classData;
    }
}
