package de.kuschku.ircbot.plugins;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import de.kuschku.ircbot.Client;

public class WolframAlphaHandler extends ListenerAdapter<PircBotX> {

	static String URL = "http://api.wolframalpha.com/v2/query?parsetimeout=1&scantimeout=2&podtimeout=4&format=plaintext&appid=%key&async=true&input=%query";
	static final String ERROR_NO_RESULTS = "WolframAlpha returned no results";
	static final String ERROR_CRASH = "Request returned an unknown error";
	static final String[] INCLUDED_PODS = new String[] {
		"Result",
		"NutritionLabelSingle:ExpandedFoodData",
		"InstantaneousWeather:WeatherData",
		"IndefiniteIntegral",
		"PossibleNamedRelationships",
		"BasicInformation:MusicAlbumData",
		"BasicInformation:MovieData"
	};
	
	static {
		for(String pod : INCLUDED_PODS) {
			URL += "&includepodid="+pod;
		}
	}
	
	String key;
	
	public WolframAlphaHandler() {
		this.key = Client.getConfig(WolframAlphaHandler.class.getCanonicalName()).getAsJsonObject("keys").get("main").getAsString();
	}
	public WolframAlphaHandler(String key) {
		this.key = key;
	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(event.getMessage().toLowerCase().startsWith("!wa ")) {
			final Channel channel = event.getChannel();
			Thread async = new Thread() {
				@Override
				public void run() {
					String result = executeAction(event.getMessage()).trim();
					if (result.contains("\n")) {
						System.out.println("Multiline");
						for (String line : result.split("\n")) {
							event.getChannel().send().message(line);
						}
					} else {
						channel.send().message(result);
					}
				}
			};
			async.start();
		}
	}
	
	public String executeAction(String input) {
		String args = input.substring("!wa ".length());			
		String request = args.trim();
		String result = ERROR_NO_RESULTS;
		
		try {
			Document doc = getResults(request);
			Optional<List<Element>> answers = parseData(doc);
			if (answers.isPresent()) {
				Optional<String> message = formatResults(answers.get());
				if (message.isPresent())
					result = message.get();
			}
		} catch (IOException e) {
			e.printStackTrace();
			result = ERROR_CRASH;
		}
		return result;
	}

	final Document getResults(String request) throws IOException {
		String resourceUrl = URL.replaceAll("%query",URLEncoder.encode(request,"UTF-8")).replaceAll("%key", key);
		return Jsoup.connect(resourceUrl).followRedirects(true).timeout(8000).parser(Parser.xmlParser()).get();
	}
	
	final Optional<List<Element>> parseData(Document data) {
		Elements results = data.select("pod");
		if (results.size()>0)
			return Optional.of(results.get(0).select("plaintext").stream().collect(Collectors.toList()));
		else
			return Optional.empty();
	}
	
	final Optional<String> formatResults(List<Element> answers) {
		return answers.stream().limit(3).map(x -> x.text()).reduce((x,y) -> (x + " | " + y));
	}
}
