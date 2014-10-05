package de.kuschku.ircbot.plugins;

import java.util.Random;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import de.kuschku.ircbot.format.BoldText;

public class Magic8BallHandler extends ListenerAdapter<PircBotX> {

	Random random = new Random();

	static final String[][] answers = new String[][] {
			new String[] {
					"It is certain",
					"It is decidedly so",
					"Without a doubt",
					"Yes – definitely",
					"You may rely on it",
					"As I see it, yes",
					"Most Likely",
					"Outlook good",
					"Yes",
					"Signs point to yes.",
					},
			new String[] {
					"Don’t count on it",
					"My reply is no",
					"My sources say no",
					"Outlook not so good",
					"very doubtful"
					},
			new String[] {
					"Reply hazy",
					"try again",
					"Ask again later",
					"Better not tell you now",
					"Cannot predict now",
					"Concentrate and ask again"
					}
			};

	public String getAnswer() {
		int answer_type = random.nextInt(answers.length);
		int answer_sub = random.nextInt(answers[answer_type].length);

		return answers[answer_type][answer_sub];
	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		System.out.println(event.getMessage());
		if (event.getMessage().toLowerCase().startsWith("!8ball ")) {

			event.getChannel().send()
					.message(new BoldText(getAnswer()).toString());
		}
	}
}
