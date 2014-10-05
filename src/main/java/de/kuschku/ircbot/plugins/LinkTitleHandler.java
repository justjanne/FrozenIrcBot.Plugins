package de.kuschku.ircbot.plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.gson.JsonObject;

import de.kuschku.ircbot.Helper;
import de.kuschku.ircbot.format.BoldText;

public class LinkTitleHandler extends ListenerAdapter<PircBotX> {
	static final String regexURL = "(((http|https|spdy)\\:\\/\\/){1}\\S+)";
	static final Pattern patternURL = Pattern.compile(regexURL);

	static final String formatData(JsonObject data) {
		if (data==null || data.toString().trim().equalsIgnoreCase(""))
			return null;
		
		DecimalFormat formatter = (DecimalFormat) NumberFormat
				.getInstance(Locale.US);

		String result =
			"%title [%duration|%views views]"
			.replaceAll(
				"%title",
				new BoldText(StringEscapeUtils.unescapeHtml4(data.get("title").getAsString())).toString()
			).replaceAll(
				"%duration",
				nicetime(data.get("duration").getAsString())
			).replaceAll(
				"%views",
				formatter.format(data.get("viewCount").getAsInt())
			);
		return result;
	}

	static final JsonObject getData(String id) throws MalformedURLException,
			IOException {
		if (id==null || id.trim().equalsIgnoreCase(""))
			return null;
		
		final String json_api = "http://gdata.youtube.com/feeds/api/videos/%id?v=2&alt=jsonc";
		JsonObject data = Helper
				.readJsonFromUrl(json_api.replaceAll("%id", id));
		return data.get("data").getAsJsonObject();
	}

	static final String getID(String address) throws MalformedURLException {
		if (address==null || address.trim().equalsIgnoreCase(""))
			return null;
		
		final URL url = new URL(address);
		if (url.getHost().equalsIgnoreCase("www.youtube.com")) {
			return Helper.parseUrlQueryString(url.getQuery()).get("v")[0];
		} else {
			throw new MalformedURLException("No YouTube link");
		}
	}

	public static Document getPage(String address) throws IOException {
		if (address==null || address.trim().equalsIgnoreCase(""))
			return null;
		
		try {
			Document doc = Jsoup.connect(address).followRedirects(true).get();
			for (int redirect_count = 0; redirect_count < 5; redirect_count ++) {
			    Elements meta = doc.select("html head meta");
			    if (meta != null)
			    {
			        String lvHttpEquiv = meta.attr("http-equiv");
			        if (lvHttpEquiv != null && lvHttpEquiv.toLowerCase().contains("refresh"))
			        {
			            String lvContent = meta.attr("content");
			            if (lvContent != null)
			            {
			                String[] lvContentArray = lvContent.split("=");
			                if (lvContentArray.length > 1)
			                    doc = Jsoup.connect(lvContentArray[1]).get();
			            }
			        }
			    } else {
			    	break;
			    }
			}
			
			return doc;
		} catch (UnsupportedMimeTypeException e) {
			throw new MalformedURLException("No YouTube link");
		}
	}
	
	public static String getNiceUrl(Document doc) throws MalformedURLException {
		if (doc==null || doc.toString().trim().equalsIgnoreCase(""))
			return null;
		
		Elements meta = doc.select("meta[property=\"al:android:url\"]");
	    if (meta != null)
	    {
	        return meta.attr("content");
	    } else {
	    	throw new MalformedURLException("No YouTube link");
	    }
	}

	public static String nicetime(String time) {
		if (time==null || time.trim().equalsIgnoreCase(""))
			return null;
		
		final long l = Long.valueOf(time) * 1000;

		final long hr = TimeUnit.MILLISECONDS.toHours(l);
		final long min = TimeUnit.MILLISECONDS.toMinutes(l
				- TimeUnit.HOURS.toMillis(hr));
		final long sec = TimeUnit.MILLISECONDS.toSeconds(l
				- TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
		if (l > 3600000) {
			return String.format("%02d:%02d:%02d", hr, min, sec);
		} else {
			return String.format("%02d:%02d", min, sec);
		}
	}

	public static String[] stringToURLList(String input) {
		if (input==null || input.trim().equalsIgnoreCase(""))
			return null;
		
		List<String> results = new ArrayList<String>();

		Matcher matcher = patternURL.matcher(input);
		while (matcher.find()) {
			results.add(matcher.group());
		}

		return results.toArray(new String[0]);
	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		String[] results = stringToURLList(event.getMessage());
		for (String result : results) {
			try {
				event.getChannel()
					.send()
					.message(
						formatData(getData(getID(getNiceUrl(getPage(result))))));
			} catch (MalformedURLException e) {
				if (!e.getMessage().equalsIgnoreCase("No YouTube link"))
					throw e;
			}
		}
	}
}
