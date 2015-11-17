package org.wso2.das.javaagent.instrumentation;

import org.wso2.das.javaagent.schema.InstrumentationMethod;

/**
 * Created by udani on 11/16/15.
 */
public class InstrumentationClassData {
    private String scenarioName;
    private InstrumentationMethod instrumentationMethod;

    public InstrumentationClassData(String scenarioName, InstrumentationMethod instrumentationMethod) {
        this.scenarioName = scenarioName;
        this.instrumentationMethod = instrumentationMethod;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public InstrumentationMethod getInstrumentationMethod() {
        return instrumentationMethod;
    }

    public void setInstrumentationMethod(InstrumentationMethod instrumentationMethod) {
        this.instrumentationMethod = instrumentationMethod;
    }
}
