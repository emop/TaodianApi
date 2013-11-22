package com.taodian.emop.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class SimpleHttpClient extends HTTPClient{
	private Log log = LogFactory.getLog("SimpleHttpClient");
	private boolean hasProxy = false;
	private URL requestURL = null;
	private URLConnection conn = null;
	
	private HttpResponse response = null;
	
	private Map<String, String> header = new HashMap<String, String>();
	private byte[] body = null;
	
	//private ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
	private OutputStream out = null;//new PrintWriter(buffer);
	private InputStream in = null;
	
	private InputStream bis = null;	
	
	public SimpleHttpClient(){
		
	}
	
	public SimpleHttpClient(URL url){
		this.requestURL = url;
		this.hasProxy = System.getProperty("http.proxyHost", null) != null;
	}	

	

	
	public HttpResponse post(String uri, Map<String, String> param) throws IOException{
		return post(uri, param, new HashMap<String, String>());
	}	
	
	
	public HttpResponse post(String uri, Map<String, String> param, Map<String, String> header) throws IOException{
		this.createSocket();
		this.createHttpRequest("POST", uri);
		//this.setHeader(header);
		this.buildBody(param);		
		this.commit();
		this.processResponse();
		this.close();
		return this.response;
	}	
	
	private HttpResponse post(Map<String, String> param, Map<String, String> head) throws IOException{
		this.createSocket();
		this.createHttpRequest("POST", this.requestURL.getPath());
		//this.setHeader(head);
		this.buildBody(param);
		this.commit();	
		this.processResponse();
		this.close();
		return this.response;
	}
	
	private void processResponse() throws IOException{
		//conn.setDoInput(true);
		in = conn.getInputStream();

		response = new HttpResponse();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		byte[] buf = new byte[1024];
		for(int len = 0; len >= 0; ){
			len = in.read(buf);
			if(len > 0){
				buffer.write(buf, 0, len);
			}
		}
		
		response.setContent(buffer.toByteArray());
		
	}
	
	private void createHttpRequest(String method, String uri){

	}
	
	private void commit() throws IOException{
		if(body != null){
			out.write(body);
		}
		out.flush();
		out.close();
	}

	private void buildBody(Map<String, String> param) throws IOException{
		//this.header.putAll(head);
		String body = "";
		for(String name: param.keySet()){
			try {
				if(body.length() > 0) body += "&";
				body += name + "=" + URLEncoder.encode(param.get(name), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IOException("encoding error, param key " + name);
			}
		}
		this.body = body.getBytes();
	}
		
	private void createSocket() throws IOException{
		 conn = this.requestURL.openConnection();
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);
		out = conn.getOutputStream(); //new PrintWriter(buffer);
	}
	
	public void close() throws IOException{
		if(this.out != null){this.out.close();};
		if(this.in != null){this.in.close();};
	};
	
	//public static
	public static HttpResponse post(URL url, Map<String, String> param) throws IOException{
		return new SimpleHttpClient(url).post(param, new HashMap<String, String>());
	}
	
	public static HttpResponse post(URL url, Map<String, String> param, Map<String, String> head) throws IOException{
		return new SimpleHttpClient(url).post(param, head);
	}


	@Override
	public HTTPResult post(String url, Map<String, Object> param, String format) {
		
		Map<String, String> params = new HashMap<String, String>();
		for(Entry<String, Object> s : param.entrySet()){
			params.put(s.getKey(), s.getValue() + "");
		}
		
		HttpResponse r = null;
		try {
			r = SimpleHttpClient.post(new URL(url), params);
		} catch (IOException e) {
		}
		
		HTTPResult result = new HTTPResult();
		
		if(r != null){
	    	if(format != null && format.equals("json")){
	    		result.json = (JSONObject)JSONValue.parse(r.getResponseMessage());
	    		if(result.json == null){
	    			log.warn("Failed to parse result as JSON object.");
	    		}else if(log.isDebugEnabled()){
	    			log.debug("resp:" + result.json.toJSONString());
	    		}
	    		if(result.json != null){
	    			result.isOK = true;
	    		}
	    	}else {
	    		result.text = r.getResponseMessage();
	    		if(log.isDebugEnabled()){
	    			log.debug("response text:" + result.text);
	    		}	    		
	    	}		
		}
		return result;
	}
	
}
