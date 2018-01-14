package absi.salesforce.streaming.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import absi.salesforce.streaming.client.common.ConnectionDTO;
import absi.salesforce.streaming.client.common.StreamingClientException;
import absi.salesforce.streaming.client.helper.LoginHelper;

public class SalesforceStreamingClient {

	private HttpClient httpClient;
	private ConnectionDTO connectionDTO;
	private BayeuxClient client;
	
	public SalesforceStreamingClient() {
	    httpClient = new HttpClient(new SslContextFactory());
	    //TODO
	    //httpClient.getProxyConfiguration().getProxies().addAll(parameters.proxies());
	}
	
	public void start(String topic) {
		try {
			connectionDTO = LoginHelper.login("malaka@wso2.com", "xxxx" ,"xxxxxx", false, null);
		} catch (StreamingClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		client = connect();

        if (client != null) {
            client.getChannel(Channel.META_HANDSHAKE).addListener(new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    boolean success = message.isSuccessful();
                    if (!success) {
                        String error = (String) message.get("error");
                        if (error != null && !"".equals(error)) {
                            //TODO 
                        }
                        Exception exception = (Exception) message.get("exception");
                        if (exception != null) {
                        	//TODO 
                        }
                    }
                }
            });

            client.getChannel(Channel.META_CONNECT).addListener(new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {                    
                    boolean success = message.isSuccessful();
                    boolean clientConnection = client.isConnected();
                    if (!success && !clientConnection) {
                        channel.unsubscribe();
                        client.disconnect();
                        String error = (String) message.get("error");
                        if (error != null && !"".equals(error)) {
                            //TODO
                        }
                    }
                }
            });

            client.getChannel(Channel.META_SUBSCRIBE).addListener(new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    boolean success = message.isSuccessful();
                    if (!success) {
                        //TODO
                    }
                }
            });

            client.handshake();
            boolean handshaken = client.waitFor(10000, BayeuxClient.State.CONNECTED);
            if (!handshaken) {
                boolean success = client.isHandshook();
                if (!success) {
                    client.handshake();
                }
            }

            client.getChannel(topic).subscribe(new MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    System.out.println(message);
                }
            });
        }        
	}
    private BayeuxClient connect() {
    	//TODO Set timeouts
        //httpClient.setConnectTimeout(connectionTimeout);
        //httpClient.setTimeout(readTimeout);
        try {
            httpClient.start();
            
            Map<String, Object> options = new HashMap<>();
            options.put("maxNetworkDelay", 15000);
            options.put("maxBufferSize", 1048576);

            LongPollingTransport transport = new LongPollingTransport(options, httpClient) {

                @Override
                protected void customize(Request request) {
                    request.header("Authorization", connectionDTO.getSessionId());
                }
            };

            client = new BayeuxClient((new URL("https://malakawso2-dev-ed.my.salesforce.com/cometd/39.0")).toExternalForm(), transport);            
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return client;
    }
	
    public static void main(String[]args) {
    	SalesforceStreamingClient client = new SalesforceStreamingClient();
    	client.start("/event/Test__e");
    }
    
}
