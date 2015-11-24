
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
                    AgentPublisher.getAgentConnection().getUsername(),AgentPublisher.getAgentConnection().getPassword(),
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
                    System.out.println("Failed");
                    connectionCheck = false;
                    Thread.sleep(2000);
                }
                catch(InterruptedException ignored){
                }
            } finally {
                if(s != null) {
                    try {
                        System.out.println("connection closed");
                        s.close();
                    }
                    catch(Exception ignored) {
                    }
                }
            }
        }
    }
}
