package org.wso2.das.javaagent.instrumentation;

import org.wso2.das.javaagent.schema.InstrumentationClass;

public class SchemaClass {
    private Class clazz;
    private String scenarioName;
    private InstrumentationClass instrumentationClass;

    public SchemaClass(String scenarioName, Class clazz, InstrumentationClass instrumentationClass){
        this.scenarioName = scenarioName;
        this.clazz = clazz;
        this.instrumentationClass = instrumentationClass;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public InstrumentationClass getInstrumentationClass() {
        return instrumentationClass;
    }

    public void setInstrumentationClass(InstrumentationClass instrumentationClass) {
        this.instrumentationClass = instrumentationClass;
    }

}
