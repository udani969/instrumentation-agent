package org.wso2.das.javaagent.schema;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class InstrumentationMethod {
    private String methodName;
    private String methodSignature;
    private String insertBefore;
    private List<InsertAt> insertAts;
    private String insertAfter;

    public InstrumentationMethod() { }

    public InstrumentationMethod(String methodName, String methodSignature,
                                 String insertBefore, List<InsertAt> insertAts, String insertAfter) {
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

    @XmlElement(name = "insertBeforeContent")
    public String getinsertBefore() {
        return insertBefore;
    }

    public void setinsertBefore(String insertBefore) {
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

    @XmlElement(name = "insertAfterContent")
    public String getInsertAfter() {
        return insertAfter;
    }

    public void setInsertAfter(String insertAfter) {
        this.insertAfter = insertAfter;
    }
}
