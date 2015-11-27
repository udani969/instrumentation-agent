package org.wso2.das.javaagent.schema;

import javax.xml.bind.annotation.XmlElement;

public class AgentConnection {
    private String streamName;
    private String tableName;
    private String streamVersion;
    private String receiverURL;
    private String authURL;
    private String username;
    private String password;
    private String hostName;
    private String servicePort;

    public AgentConnection() { }
    public AgentConnection(String streamName, String tableName, String  streamVersion,
                           String receiverURL, String authURL, String username, String password,
                           String hostName, String servicePort) {
        this.streamName = streamName;
        this.tableName = tableName;
        this.streamVersion = streamVersion;
        this.receiverURL = receiverURL;
        this.authURL = authURL;
        this.username = username;
        this.password = password;
        this.hostName = hostName;
        this.servicePort = servicePort;
    }

    @XmlElement(name = "streamName")
    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    @XmlElement(name = "tableName")
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @XmlElement(name = "version")
    public String getStreamVersion() {
        return streamVersion;
    }

    public void setStreamVersion(String streamVersion) {
        this.streamVersion = streamVersion;
    }

    @XmlElement(name = "receiverURL")
    public String getReceiverURL() {
        return receiverURL;
    }

    public void setReceiverURL(String receiverURL) {
        this.receiverURL = receiverURL;
    }

    @XmlElement(name = "authURL")
    public String getAuthURL() {
        return authURL;
    }

    public void setAuthURL(String authURL) {
        this.authURL = authURL;
    }

    @XmlElement(name = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @XmlElement(name = "password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @XmlElement(name = "hostName")
    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @XmlElement(name = "servicePort")
    public String getServicePort() {
        return servicePort;
    }

    public void setServicePort(String servicePort) {
        this.servicePort = servicePort;
    }
}
