package de.kuschku.ircbot.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.kuschku.ircbot.Helper;
import de.kuschku.ircbot.format.BoldText;

public class TranslationHandler extends ListenerAdapter<PircBotX> {

	static final String[][] HEADERS = new String[][] {
			{ "user-agent","NokiaN97/21.1.107 (SymbianOS/9.4; Series60/5.0 Mozilla/5.0; Profile/MIDP-2.1 Configuration/CLDC-1.1) AppleWebkit/525 (KHTML, like Gecko) BrowserNG/7.1.4" }
			};

	static final String URL = "https://translate.google.com/translate_a/single?client=webapp&sl=%lang_origin&tl=%lang_target&dt=bd&dt=ld&dt=qc&dt=rm&dt=t&dj=1&q=";

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(event.getMessage().toLowerCase().startsWith("!trans ") || event.getMessage().toLowerCase().startsWith("!trans:") ) {
			Thread async = new Thread() {
				@Override
				public void run() {
					try {
						event.getChannel().send().message(executeAction(event.getMessage()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			async.start();
		}
	}
	
	static final String executeAction(String input) throws UnsupportedEncodingException, IOException {
		String args = input.substring("!trans".length());
		String lang_args = args.substring(0,args.indexOf(" ")).trim();
		String[] specified_languages = lang_args.split(":");
		
		String request = args.substring(args.indexOf(" ")).trim();
		
		JsonObject json = getResults(URLEncoder.encode(request,"UTF-8"),specified_languages);
		String sentence = getSentencesFromJson(json);
		String langs = formatLangs(getLanguagesFromJson(json));
		
		return langs + " " + new BoldText(sentence).toString();
	}

	static final JsonObject getResults(String request, String[] langs) throws IOException {
		URL resourceUrl;
		switch (langs.length) {
		case 1:
			resourceUrl = new URL(URL.replaceAll("%lang_origin", "auto").replaceAll("%lang_target", "en") + request);
			break;
		case 2:
			resourceUrl = new URL(URL.replaceAll("%lang_origin", "auto").replaceAll("%lang_target", langs[1]) + request);
			break;
		default:
			resourceUrl = new URL(URL.replaceAll("%lang_origin", langs[2]).replaceAll("%lang_target", langs[1]) + request);
		}
		HttpURLConnection.setFollowRedirects(true);
		HttpURLConnection conn = (HttpURLConnection) resourceUrl
				.openConnection();
		conn.setInstanceFollowRedirects(true);
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		for (String[] pair : HEADERS) {
			conn.setRequestProperty(
					pair[0],
					pair[1]);
		}
		conn.connect();
		
		String result = "";
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			result = Helper.readAll(reader);
		}
		
		JsonObject json = new JsonParser().parse(result)
				.getAsJsonObject();
		return json;
	}
	
	static final String getSentencesFromJson(JsonObject object) {
		List<String> sentences = new ArrayList<String>();
		object.get("sentences").getAsJsonArray().forEach(sentence -> sentences.add(sentence.getAsJsonObject().get("trans").getAsString()));
		
		String result = "";
		for (String sentence : sentences) {
			result += sentence;
		}
		return result;
	}
	
	static final List<String> getLanguagesFromJson(JsonObject object) {
		List<String> langs = new ArrayList<String>();
		object.get("ld_result").getAsJsonObject().get("srclangs").getAsJsonArray().forEach(lang -> langs.add(lang.getAsString()));
		return langs;
	}
	
	static final String formatLangs(List<String> langs) {
		String origin = "";
		for (String lang : langs) {
			origin += "|" + lang.toUpperCase();
		}
		return "[" + origin.substring(1) + "]";
	}
	
	public static void main(String[] args) throws MalformedURLException {
		
		
	}
}
