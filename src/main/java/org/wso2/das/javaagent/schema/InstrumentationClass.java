package org.wso2.das.javaagent.schema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class InstrumentationClass {
    private String className;
    private List<InstrumentationMethod> instrumentationMethods;

    public InstrumentationClass() { }
    public InstrumentationClass(String className, List<InstrumentationMethod> instrumentationMethods) {
        super();
        this.className = className;
        this.instrumentationMethods = instrumentationMethods;
    }

    @XmlAttribute(name = "name")
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @XmlElement(name = "instrumentingMethod")
    public void setInstrumentationMethods(List<InstrumentationMethod> instrumentationMethods) {
        this.instrumentationMethods = instrumentationMethods;
    }

    public List<InstrumentationMethod> getInstrumentationMethods() {
        return instrumentationMethods;
    }
}
