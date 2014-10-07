package de.kuschku.ircbot.plugins;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import com.google.gson.JsonObject;

import de.kuschku.ircbot.Client;
import de.kuschku.ircbot.plugins.helpers.GoogleKeepHelper;

public class KeepHandler extends ListenerAdapter<PircBotX> {
	GoogleKeepHelper helper;
	Random rand = new Random();

	public KeepHandler() throws IOException, NoSuchAlgorithmException {
		JsonObject authData = Client.getConfig(
				this.getClass().getCanonicalName()).getAsJsonObject("auth");
		this.helper = new GoogleKeepHelper(authData.get("username")
				.getAsString(), authData.get("password").getAsString());
	}

	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event)
			throws Exception {
		if (event.getMessage().startsWith("!keep test")) {
			if (event.getUser().getHostmask().equalsIgnoreCase("kuschku.de")) {
				helper.request("{\"requestHeader\":{\"clientVersion\":{\"major\":\"2\",\"minor\":\"4\",\"build\":\"0\",\"revision\":\"0\"},\"clientPlatform\":\"WEB\"},\"targetVersion\":\"207bf\",\"nodes\":[{\"id\":\"1412546318871.6342052876\",\"kind\":\"notes#node\",\"parentId\":\"148e234db8c.bce612d81c7ffd04\",\"sortValue\":-4194304,\"timestamps\":{\"kind\":\"notes#timestamps\",\"created\":\"2060-10-05T21:58:38.871Z\"},\"type\":\"LIST_ITEM\",\"text\":\"testing\",\"checked\":false}]}");
			} else {
				event.getUser()
						.send()
						.notice("You do not have the permissions necessary for this");
			}
		}

	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if (event.getMessage().startsWith("!todo")) {
			if (event.getUser().getHostmask().equalsIgnoreCase("kuschku.de")) {
				String text = event.getMessage().substring(event.getMessage().indexOf(" ")).trim();
				JsonObject response = helper.request("{\"requestHeader\":{\"clientVersion\":{\"major\":\"2\",\"minor\":\"4\",\"build\":\"0\",\"revision\":\"0\"},\"clientPlatform\":\"WEB\"},\"targetVersion\":\"207bf\",\"nodes\":[{\"id\":\""+(Integer.valueOf(Math.round(System.nanoTime()/1000)))+"."+(long)(rand.nextDouble()*10000000000L)+"\",\"kind\":\"notes#node\",\"parentId\":\"148e234db8c.bce612d81c7ffd04\",\"sortValue\":-4194304,\"timestamps\":{\"kind\":\"notes#timestamps\",\"created\":\""+new Timestamp(new Date().getTime()).toString().replace(' ', 'T')+"Z\"},\"type\":\"LIST_ITEM\",\"text\":\""+text+"\",\"checked\":false}]}");
			} else {
				event.getUser()
						.send()
						.notice("You do not have the permissions necessary for this");
			}
		}
	}
}
