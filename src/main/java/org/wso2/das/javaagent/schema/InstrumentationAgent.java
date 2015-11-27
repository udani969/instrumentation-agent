package org.wso2.das.javaagent.schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "instrumentationAgent")
public class InstrumentationAgent {
    private AgentConnection agentConnection;
    private List<Scenario> scenarios;

    public InstrumentationAgent(){ }
    public InstrumentationAgent(AgentConnection agentConnection, List<Scenario> scenarios) {
        super();
        this.agentConnection = agentConnection;
        this.scenarios = scenarios;
    }

    @XmlElement(name = "agentConnection")
    public AgentConnection getAgentConnection() {
        return agentConnection;
    }

    public void setAgentConnection(AgentConnection agentConnection) {
        this.agentConnection = agentConnection;
    }

    @XmlElementWrapper(name = "scenarios")
    @XmlElement(name="scenario", nillable = false)
    public List<Scenario> getScenarios() {
        return scenarios;
    }
    public void setScenarios(List<Scenario> scenarios) {
        this.scenarios = scenarios;
    }

}

