package org.wso2.das.javaagent.schema;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Created by udani on 10/16/15.
 */
public class InsertAt {
    private int lineNo;
    private List<ParameterName> parameterNames;

    public InsertAt() { }
    public InsertAt(int lineNo, List<ParameterName> parameterNames) {
        this.lineNo = lineNo;
        this.parameterNames = parameterNames;
    }

    @XmlAttribute(name = "lineNo")
    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    @XmlElement(name = "parameterName")
    public List<ParameterName> getParameterNames() {
        return parameterNames;
    }

    public void setParameterNames(List<ParameterName> parameterNames) {
        this.parameterNames = parameterNames;
    }
}
