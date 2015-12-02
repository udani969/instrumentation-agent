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

import org.json.simple.parser.ParseException;
import org.wso2.das.javaagent.instrumentation.AgentPublisher;

import java.io.IOException;
import java.net.Socket;

public class AgentConnectionWorker implements Runnable{

    public void run() {
        waitForConnection(AgentPublisher.getAgentConnection().getHostName(),
                Integer.parseInt(AgentPublisher.getAgentConnection().getServicePort()));

        System.out.println("connected to server");

        if(!AgentPublisher.getArbitraryFields().isEmpty()){
            try {
                Thread.sleep(5000);
                AgentPublisher.updateCurrentSchema(
                        AgentPublisher.generateConnectionURL(AgentPublisher.getAgentConnection()),
                        AgentPublisher.getAgentConnection().getUsername(), AgentPublisher.getAgentConnection().getPassword(),
                        AgentPublisher.getArbitraryFields());
                System.out.println("Schema modification complete......");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }

    public static void waitForConnection(String host, int port) {
        boolean connectionCheck = false;

        while(!connectionCheck) {
            Socket s = null;
            try {
                s = new Socket(host, port);
                connectionCheck = true;
            } catch (Exception e) {
                try
                {
                    connectionCheck = false;
                    Thread.sleep(2000);
                }
                catch(InterruptedException ignored){
                }
            } finally {
                if(s != null) {
                    try {
                        s.close();
                    }
                    catch(Exception ignored) {
                    }
                }
            }
        }
    }
}
