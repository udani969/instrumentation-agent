package org.wso2.das.javaagent.schema;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "scenario")
public class Scenario {
    private String scenarioName;
    private List<InstrumentationClass> instrumentationClasses;

    public Scenario() { }
    public Scenario(String scenarioName, List<InstrumentationClass> instrumentationClasses) {
        super();
        this.scenarioName = scenarioName;
        this.instrumentationClasses = instrumentationClasses;
    }

    @XmlAttribute(name = "name")
    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    @XmlElement(name = "instrumentingClass")
    public List<InstrumentationClass> getinstrumentationClasses() {
        return instrumentationClasses;
    }

    public void setinstrumentationClasses(List<InstrumentationClass> instrumentationClasses) {
        this.instrumentationClasses = instrumentationClasses;
    }
}
