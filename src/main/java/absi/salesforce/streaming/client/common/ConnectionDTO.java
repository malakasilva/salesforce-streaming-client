package absi.salesforce.streaming.client.common;

public class ConnectionDTO {
    private String serverUrl;
    private String sessionId;
    
    public ConnectionDTO(String serverUrl, String sessionId) {
    	this.serverUrl = serverUrl;
    	this.sessionId = sessionId;
    }

	public String getServerUrl() {
		return serverUrl;
	}

	public String getSessionId() {
		return sessionId;
	}

}
