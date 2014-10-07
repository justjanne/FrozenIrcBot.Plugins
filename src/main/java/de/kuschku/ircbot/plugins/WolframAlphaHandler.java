package de.kuschku.ircbot.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.kuschku.ircbot.Client;
import de.kuschku.ircbot.Helper;

public class WolframAlphaHandler extends ListenerAdapter<PircBotX> {

	static final String URL = "http://www.wolframalpha.com/autocomplete/v1/query?appid=%id&i=%query";

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(event.getMessage().toLowerCase().startsWith("!wa ")) {
			String args = event.getMessage().substring("!wa ".length());			
			String request = args.trim();
			
			try {
				JsonObject json = getResults(request);
				String answer = parseData(json);
			
				event.getChannel().send().message(answer);
			} catch (IOException e) {
				event.getChannel().send().message("WolframAlpha could not parse your request, please try it again.");
			}
		}
	}

	public static JsonObject getResults(String request) throws IOException {
		JsonObject keys = Client.getConfig(WolframAlphaHandler.class.getCanonicalName()).getAsJsonObject("keys");
		URL resourceUrl = new URL(URL.replaceAll("%query",URLEncoder.encode(request,"UTF-8")).replaceAll("%key", keys.get("main").getAsString()));
		HttpURLConnection.setFollowRedirects(true);
		HttpURLConnection conn = (HttpURLConnection) resourceUrl
				.openConnection();
		conn.setInstanceFollowRedirects(true);
		conn.setConnectTimeout(2000);
		conn.setReadTimeout(2000);
		conn.connect();
		
		String result = "";
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			result = Helper.readAll(reader);
		}
		
		JsonObject json = new JsonParser().parse(result)
				.getAsJsonObject();
		return json;
	}
	
	static final String parseData(JsonObject data) {
		if (data.has("instantMath")) {
			return data.get("instantMath").getAsJsonObject().get("exactResult").getAsString();
		} else {
			return "WolframAlpha did not return a single definite result, please try again.";
		}
	}

}
