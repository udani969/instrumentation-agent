package org.wso2.das.javaagent.schema;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * Created by udani on 10/16/15.
 */
public class ParameterName {
    private String key;
    private String parameterValue;

    public ParameterName() { }
    public ParameterName(String key, String parameterValue) {
        this.key = key;
        this.parameterValue = parameterValue;
    }

    @XmlAttribute(name = "key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlValue
    public String getParameterValue() {
        return parameterValue;
    }

    public void setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
    }
}
