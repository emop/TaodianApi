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

public abstract class HTTPClient {
	protected Log log = LogFactory.getLog("com.taodian.emop.http");

	public static HTTPClient create(){
		return create("apache");
	}
	
	public static HTTPClient create(String type){
		if(type != null && type.equals("apache")){
			return ApacheHTTPClient.create();
		}else {
			return new SimpleHttpClient();
		}
	}

	public HTTPResult post(String url, Map<String, Object>param){
		return post(url, param, "json");
	}
	
	public abstract HTTPResult post(String url, Map<String, Object>param, String format);
	
}
