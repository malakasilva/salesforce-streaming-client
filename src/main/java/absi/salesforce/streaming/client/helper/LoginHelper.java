package absi.salesforce.streaming.client.helper;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import absi.salesforce.streaming.client.common.ConnectionDTO;
import absi.salesforce.streaming.client.common.StreamingClientException;

public final class LoginHelper {

	//String containing the request payload for login call
	private static final String LOGIN_REQUEST_XML = "<soapenv:Envelope "
			+ "xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' "
			+ "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
			+ "xmlns:urn='urn:partner.soap.sforce.com'><soapenv:Body>"
			+ "<urn:login><urn:username>STREAMING_USER_NAME</urn:username>"
			+ "<urn:password>STREAMING_PASSWORD</urn:password></urn:login>"
			+ "</soapenv:Body></soapenv:Envelope>";
	
    private static final String SERVICES_SOAP_PARTNER_ENDPOINT = "/services/Soap/u/";
    private static final String PRODUCTION_LOGIN_ENDPOINT = "https://login.salesforce.com";
    private static final String SANDBOX_LOGIN_ENDPOINT = "https://test.salesforce.com";
    private static final String DEFAULT_VERSION = "22.0/";
    
    private static class LoginResponseParser extends DefaultHandler {

        private String buffer;
        private String faultstring;

        private boolean reading = false;
        private String serverUrl;
        private String sessionId;

        @Override
        public void characters(char[] ch, int start, int length) {
            if (reading) {
            	buffer = new String(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            reading = false;
            switch (localName) {
            case "sessionId":
                sessionId = buffer;
                break;
            case "serverUrl":
                serverUrl = buffer;
                break;
            case "faultstring":
                faultstring = buffer;
                break;
            default:
            }
            buffer = null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (localName) {
            case "sessionId":
                reading = true;
                break;
            case "serverUrl":
                reading = true;
                break;
            case "faultstring":
                reading = true;
                break;
            default:
            }
        }
    }
    
	public static ConnectionDTO login(String userName, String password, String securityToken, boolean isSandbox, String version)
			throws StreamingClientException {
		HttpClient client = new HttpClient(new SslContextFactory());
		try {
			//TODO Add support via proxy service
			//client.getProxyConfiguration().getProxies().addAll();
			client.start();
			
			//Consuruct the login endpoint
			String loginEndpoint;
			if(isSandbox) {
				loginEndpoint = SANDBOX_LOGIN_ENDPOINT;
			} else {
				loginEndpoint = PRODUCTION_LOGIN_ENDPOINT;
			}
			//Add the context and version
			loginEndpoint += SERVICES_SOAP_PARTNER_ENDPOINT;
			if(version != null) {
				loginEndpoint += loginEndpoint + "/";
			}else {
				loginEndpoint += DEFAULT_VERSION;
			}						
			URL endpoint = new URL(loginEndpoint);
			
			//Construct Post request
			Request postRequest = client.POST(endpoint.toURI());
			postRequest
					.content(
							new ByteBufferContentProvider("text/xml",
									ByteBuffer.wrap((LOGIN_REQUEST_XML.replace("STREAMING_USER_NAME", userName)
											.replace("STREAMING_PASSWORD", password + securityToken))
													.getBytes("UTF-8"))));
			postRequest.header("SOAPAction", "''");
			postRequest.header("PrettyPrint", "Yes");
			
			//Call the endpoint
            ContentResponse response = postRequest.send();
            
            //Parse the response
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            LoginResponseParser loginResponseParser = new LoginResponseParser();
            saxParser.parse(new ByteArrayInputStream(response.getContent()), loginResponseParser);
            
            //If login fault not continue the flow
			if (loginResponseParser.faultstring != null) {
				throw new StreamingClientException("Login Fault." + loginResponseParser.faultstring);
			}

			return new ConnectionDTO(loginResponseParser.serverUrl, loginResponseParser.sessionId);
		} catch (Exception e) {
			throw new StreamingClientException("Error while making the login request.", e);
		} finally {
            try {
				client.stop();
				client.destroy();
			} catch (Exception e) {	}
            
		}
	}
}
