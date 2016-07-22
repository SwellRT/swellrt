package org.swellrt.server.box.events.http;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.swellrt.server.box.events.Event;
import org.swellrt.server.box.events.EventDispatcherTarget;
import org.swellrt.server.box.events.EventRule;
import org.waveprotocol.wave.util.logging.Log;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

/**
 * Dispatch events to HTTP endpoints depending of events
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class HttpDispatcher implements EventDispatcherTarget {
	
	private static final Log LOG = Log.get(HttpDispatcher.class);
	
	public static final String NAME = "http";
	private static final String CONFIG_PREFIX = "dispatch."+NAME;
	
	private final HttpClient httpClient;
	private final Config config;
	
	
	@Inject
	public HttpDispatcher(Config config) {
		this.config = config;
		this.httpClient = new HttpClient();
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void dispatch(EventRule rule, Event event, String payload) {
				
		
		String configBasePath = CONFIG_PREFIX+"."+rule.getId()+".";
		PostMethod request = null;
		
		try {
		
			String host = config.getString(configBasePath+"host");
			String contentType = config.getString(configBasePath+"contentType");
			List<String> headers = config.getStringList(configBasePath+"headers");
			
		    request = new PostMethod(host);
	
		    request.setRequestHeader("Content-Type", contentType);
		    for (String h: headers) {
		    	String[] headerParts = h.split(":");
		    	if (headerParts != null && headerParts.length == 2)
		    		request.setRequestHeader(headerParts[0], headerParts[1]);
		    }
		    
		    JSONObject jsonData = new JSONObject();
		    
		    jsonData.put("waveid", event.getWaveId().serialise());
		    jsonData.put("waveletid", event.getWaveletId().serialise());
		    jsonData.put("blipid", event.getBlipId());
		    jsonData.put("path", event.getPath());
		    jsonData.put("data", new JSONObject(payload));
		    
		    LOG.info("HTTP dispatcher. Message to send: "+jsonData.toString());
		    
		    RequestEntity requestData = new ByteArrayRequestEntity(jsonData.toString().getBytes(Charset.forName("UTF-8")));
		    request.setRequestEntity(requestData);
	
		     int resultCode = httpClient.executeMethod(request);
	
		     if (resultCode != HttpStatus.SC_OK) {
		        LOG.severe("HTTP dispatcher error, response status "+resultCode);
		     }


	    } catch (JSONException e) {
	    	LOG.severe("HTTP dispatcher error", e);

	    } catch (HttpException e) {
	    	LOG.severe("HTTP dispatcher error", e);

	    } catch (IOException e) {
	    	LOG.severe("HTTP dispatcher error", e);

	    } catch(ConfigException e) {
	    	LOG.severe("HTTP dispatcher, configuration error", e);	    	
	    } finally {
	    	if (request != null)
	    		request.releaseConnection();
	    }		
		

	}

}
