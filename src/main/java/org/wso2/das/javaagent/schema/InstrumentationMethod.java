package org.wso2.das.javaagent.schema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

public class InstrumentationMethod {
    private String methodName;
    private String methodSignature;
    private InsertBefore insertBefore;
    private List<InsertAt> insertAts;
    private InsertAfter insertAfter;

    public InstrumentationMethod() { }
    public InstrumentationMethod(String methodName, String methodSignature,
                                 InsertBefore insertBefore, List<InsertAt> insertAts, InsertAfter insertAfter) {
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.insertBefore = insertBefore;
        this.insertAts = insertAts;
        this.insertAfter = insertAfter;
    }

    @XmlAttribute(name = "name")
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @XmlAttribute(name = "signature")
    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    @XmlElement(name = "insertBefore")
    public InsertBefore getInsertBefore() {
        return insertBefore;
    }

    public void setInsertBefore(InsertBefore insertBefore) {
        this.insertBefore = insertBefore;
    }

    @XmlElementWrapper(name = "insertAts")
    @XmlElement(name = "insertAt")
    public List<InsertAt> getInsertAts() {
        return insertAts;
    }

    public void setInsertAts(List<InsertAt> insertAts) {
        this.insertAts = insertAts;
    }

    @XmlElement(name = "insertAfter")
    public InsertAfter getInsertAfter() {
        return insertAfter;
    }

    public void setInsertAfter(InsertAfter insertAfter) {
        this.insertAfter = insertAfter;
    }
}
