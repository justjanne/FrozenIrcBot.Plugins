package de.kuschku.ircbot.plugins.helpers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class GoogleKeepHelper {
	Map<String,String> sessionData = new HashMap<String,String>();
	
	String password;
	String username;

	static Map<String, String> getCookies() throws IOException {
		Map<String,String> cookies = new HashMap<String,String>();
		
		URL obj = new URL("https://accounts.google.com/ManageAccount");
		URLConnection conn = obj.openConnection();
		Map<String, List<String>> map = conn.getHeaderFields();
		cookies.put("GAPS", map.get("Set-Cookie").parallelStream().filter(x -> x.contains("GAPS=")).map(x -> x.substring(x.indexOf("GAPS="))).map(x -> x.substring(x.indexOf("=")+1,x.indexOf(";"))).findFirst().get());
		
		obj = new URL("https://accounts.google.com/ServiceLogin");
		conn = obj.openConnection();
		map = conn.getHeaderFields();
		cookies.put("GALX", map.get("Set-Cookie").parallelStream().filter(x -> x.contains("GALX=")).map(x -> x.substring(x.indexOf("GALX="))).map(x -> x.substring(x.indexOf("=")+1,x.indexOf(";"))).findFirst().get());
		
		return cookies;
	}

	Map<String,String> getSession(Map<String,String> cookies) throws IOException {		
		
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("https://accounts.google.com/ServiceLoginAuth");

		httppost.addHeader("User-Agent", "Mozilla/5.0");
		httppost.addHeader("Cookie", String.format("GAPS=%s; GALX=%s",cookies.get("GAPS"),cookies.get("GALX")));

		// Request parameters and other properties.
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("GALX", cookies.get("GALX")));
		params.add(new BasicNameValuePair("Passwd", this.password));
		params.add(new BasicNameValuePair("Email", this.username));
		params.add(new BasicNameValuePair("PersistentCookie", "yes"));
		params.add(new BasicNameValuePair("rmShown","1"));
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		//Execute and get the response.
		HttpResponse response = httpclient.execute(httppost);
		
		Map<String,String> session = new HashMap<String,String>();

		for (Header header : response.getHeaders("Set-Cookie")) {
			session.put(header.getValue().substring(0,header.getValue().indexOf("=")),header.getValue().substring(header.getValue().indexOf("=")+1,header.getValue().indexOf(";")));
		}
		return session;
	}
	
	public GoogleKeepHelper(String username, String password) throws IOException, NoSuchAlgorithmException {
		this.username = username;
		this.password = password;
		Map<String, String> cookies = GoogleKeepHelper.getCookies();
		Map<String, String> session = getSession(cookies);
		this.sessionData.putAll(cookies);
		this.sessionData.putAll(session);
		this.sessionData.put("SAPISIDHASH", HashHelper.sha1(this.sessionData.get("SAPISID") + " https://keep.google.com"));
	}
	
	/**
	 * This method takes a JSON request and sends it to the server, returning the response
	 * @param data The data that's supposed to be submitted encoded as JSON.
	 * @return JsonObject The Server's response, also encoded in JSON.
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JsonObject request(String data) throws ClientProtocolException, IOException {
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("https://clients6.google.com/notes/v1/changes?alt=json&key=AIzaSyDzSyl-DPNxSyc7eghRsB4oNNetrnvnH0I");
		String cookie = this.sessionData.entrySet().parallelStream().map(x -> x.getKey()+"="+x.getValue()+"; ").reduce(String::concat).get();

		httppost.addHeader("User-Agent", "Mozilla/5.0");
		httppost.addHeader("Host", "clients6.google.com");
		httppost.addHeader("Content-Type", "application/json; charset=UTF-8");
		httppost.addHeader("Authorization", "SAPISIDHASH "+this.sessionData.get("SAPISIDHASH"));
		httppost.addHeader("X-Origin", "https://keep.google.com");
		httppost.addHeader("X-Referer", "https://keep.google.com");
		httppost.addHeader("Cookie",cookie);
		httppost.setEntity(new StringEntity(data));
		
		HttpResponse response = httpclient.execute(httppost);
		try (JsonReader reader = new JsonReader(new InputStreamReader(response.getEntity().getContent()))) {
			JsonParser parser = new JsonParser();
			return parser.parse(reader).getAsJsonObject();
		}
	}
}
