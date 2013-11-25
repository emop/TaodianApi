package com.taodian.emop.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.taodian.api.monitor.Benchmark;
//import org.emop.monitor.Benchmark;

public class ApacheHTTPClient extends HTTPClient{
	private Log log = LogFactory.getLog("com.taodian.emop.http");

	private DefaultHttpClient httpclient = null; //new DefaultHttpClient();

	
	public static ApacheHTTPClient create(){
		ApacheHTTPClient client = new ApacheHTTPClient();
		
		/**
		 * 配置多线程共享连接。
		 */
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(
		         new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
		schemeRegistry.register(
		         new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

		PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
		// Increase max total connection to 200
		cm.setMaxTotal(300);
		// Increase default max connection per route to 20
		cm.setDefaultMaxPerRoute(100);
		
		//client.httpclient = new DefaultHttpClient(cm);

		client.httpclient = new DefaultHttpClient(cm);
		HttpParams param = client.httpclient.getParams();
		param.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
		param.setParameter(CoreConnectionPNames.SO_TIMEOUT, 30 * 1000);
		
		client.supportGzip();
		
		return client;
	}
	
	protected void supportGzip(){
        httpclient.addRequestInterceptor(new HttpRequestInterceptor() {

            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
            }

        });

        httpclient.addResponseInterceptor(new HttpResponseInterceptor() {

            public void process(
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Header ceheader = entity.getContentEncoding();
                    if (ceheader != null) {
                        HeaderElement[] codecs = ceheader.getElements();
                        for (int i = 0; i < codecs.length; i++) {
                            if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                            	/*
                            	if(log.isDebugEnabled()){
                            		log.debug("Get gzip entity");
                            	}
                            	*/
                                response.setEntity(
                                        new GzipDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }
            }

        });		
	}

	public HTTPResult post(String url, Map<String, Object>param){
		return post(url, param, "json");
	}
	
	public HTTPResult post(String url, Map<String, Object>param, String format){
		HttpResponse response = null;
		List<NameValuePair> nameValuePairs = null;
		
		Benchmark mark = Benchmark.start(Benchmark.HTTP_REQUEST);
		HTTPResult result = new HTTPResult();		
		HttpPost httppost = new HttpPost(url);
		
		
		StringBuffer query = new StringBuffer(url + "?");        
        if(param == null) param = new HashMap<String, Object>();
        InputStreamReader reader = null;
		try{
	    	nameValuePairs = new ArrayList<NameValuePair>(param.size());
	        for(Entry<String, Object> item : param.entrySet()){
	        	if(item.getValue() != null){
	        		query.append("&" + item.getKey() + "=" + item.getValue());
	        		nameValuePairs.add(new BasicNameValuePair(item.getKey(), item.getValue().toString()));
	        	}else {
	        		query.append("&" + item.getKey() + "=");
	        		nameValuePairs.add(new BasicNameValuePair(item.getKey(), ""));	        		
	        	}
	        }
	        if(log.isDebugEnabled()){
	        	log.debug("Post Request:" + query.toString());
	        }
	        mark.attachObject(query.toString());
	        
	    	if(nameValuePairs != null){
	    		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
	    		httppost.setEntity(entity);
	    	}
	    	response = httpclient.execute(httppost);
	    	if(format != null && format.equals("json")){
		    	if(response != null){
		    		reader = new InputStreamReader(response.getEntity().getContent(), 
		    				"UTF-8");
		    		result.json = (JSONObject)JSONValue.parse(reader);
		    		if(result.json == null){
		    			log.warn("Failed to parse result as JSON object.");
		    		}else if(log.isDebugEnabled()){
		    			log.debug("resp:" + result.json.toJSONString());
		    		}
		    		if(result.json != null){
		    			result.isOK = true;
		    		}
		    		//reader.close();
		    	}
	    	}else {
	    		result.text = new String(EntityUtils.toByteArray(response.getEntity()));
	    		if(log.isDebugEnabled()){
	    			log.debug("response text:" + result.text);
	    		}	    		
	    	}
		}catch(ConnectException e){
			log.info("Network error:" + e.toString());
		}catch (Throwable e) {
			log.warn(e, e);
		}finally{
			if(reader != null){
				try{
					reader.close();
				}catch(IOException e) {
					log.error(e, e);
				}
			}
			mark.done();
		}
		return result;
	}
}
