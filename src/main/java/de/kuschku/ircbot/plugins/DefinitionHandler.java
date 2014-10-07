package de.kuschku.ircbot.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wordnik.client.api.WordApi;
import com.wordnik.client.common.ApiException;
import com.wordnik.client.model.Definition;

import de.kuschku.ircbot.Client;
import de.kuschku.ircbot.Helper;
import de.kuschku.ircbot.Helper.URLParamEncoder;
import de.kuschku.ircbot.format.BoldText;
import de.kuschku.ircbot.format.ItalicText;

public class DefinitionHandler extends ListenerAdapter<PircBotX> {

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		ImmutableList<String> args = Helper.parseArgs(event.getMessage(), "!");
		if (args != null
				&& (args.get(0).equalsIgnoreCase("def") || args.get(0)
						.equalsIgnoreCase("ud"))) {
			int amount = 3;
			if (event.getChannel().isChannelPrivate())
				amount = 20;

			String word = String.join(" ", args.asList());
			word = word.substring(args.get(0).length() + 1);

			Backend backend = null;
			JsonObject keys = Client.getConfig(this.getClass().getCanonicalName()).getAsJsonObject("keys");

			if (args.get(0).equalsIgnoreCase("def")) {
				backend = new WordNetBackend(
						keys.get("wordnet").getAsString());
			} else if (args.get(0).equalsIgnoreCase("ud")) {
				backend = new UrbanBackend(
						keys.get("urbandictionary").getAsString());
			}

			List<String> list = backend.getDefinition(word, amount);

			if (list.size() > 0) {
				list.forEach(msg -> event.getChannel().send().message(Helper.truncate(msg,150)));

				event.getChannel().send().message(backend.getFooter(word));
			} else {
				event.getChannel()
						.send()
						.message(
								new BoldText(String.format(
										"Error: no definitions found for %s",
										word)).toString());
			}
		}
	}

	interface Backend {
		public List<String> getDefinition(String word, int amount);

		public String getFooter(String word);
	}

	class WordNetBackend implements Backend {

		final String key;

		WordNetBackend(String key) {
			this.key = key;
		}

		public Optional<List<Definition>> getDefinitions(String word, int amount) {
			word = URLParamEncoder.encode(word);
			try {
				WordApi api = new WordApi();
				api.getInvoker().addDefaultHeader("api_key", key);
				List<Definition> definitions = api.getDefinitions(word, "all",
						"wordnet", amount, "false", "true", "false");
				return Optional.of(definitions);
			} catch (ApiException e) {
			}
			return Optional.ofNullable(null);
		}

		@Override
		public List<String> getDefinition(String word, int amount) {
			List<Definition> list = getDefinitions(word, amount).get();
			List<String> result = new ArrayList<String>();
			for (Definition definition : list) {
				result.add(formatDefinition(definition));
			}
			return result;
		}

		public String formatDefinition(Definition definition) {
			String text = definition.getText().substring(0,
					(definition.getText()+"\n").indexOf("\n"));
			return String.format(
					"%s %s %s",
					new BoldText(String.valueOf((Integer.valueOf(definition
							.getSequence()) + 1))),
					new ItalicText(definition.getPartOfSpeech()), text);
		}

		@Override
		public String getFooter(String word) {
			return String
					.format("More definitions at http://wordnetweb.princeton.edu/perl/webwn?s=%s",
							URLParamEncoder.encode(word));
		}
	}

	class UrbanBackend implements Backend {

		final String key;

		UrbanBackend(String key) {
			this.key = key;
		}

		@Override
		public List<String> getDefinition(String word, int amount) {
			final String url = "http://api.urbandictionary.com/v0/define?term=%query&key=key".replaceAll("%query", word).replaceAll("%key", key);
			
			word = URLParamEncoder.encode(word);
			try {
				JsonObject result = Helper
						.readJsonFromUrl(url);
				JsonArray definitions = result.get("list").getAsJsonArray();
				List<String> results = new ArrayList<String>();
				String definition;
				for (int i = 0; i < amount && i < definitions.size(); i++) {
					definition = definitions.get(i).getAsJsonObject()
							.get("definition").getAsString();
					definition = definition.substring(0,
							(definition+"\n").indexOf("\n"));
					results.add(String.format("%s %s",
							new BoldText(String.valueOf(i+1)), definition));
				}
				return results;
			} catch (IOException e) {
			}
			return new ArrayList<String>();
		}

		@Override
		public String getFooter(String word) {
			return String
					.format("More definitions at http://www.urbandictionary.com/define.php?term=%s",
							URLParamEncoder.encode(word));
		}
	}

}
