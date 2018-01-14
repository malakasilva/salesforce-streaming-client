package absi.salesforce.streaming.client.common;

public class StreamingClientException extends Exception{

	public StreamingClientException(String sMassage) {
		super(sMassage);
	}
	
	public StreamingClientException(String sMassage, Throwable throwable) {
		super(sMassage, throwable);
	}
	
}
