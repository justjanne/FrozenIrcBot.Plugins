package de.kuschku.ircbot.plugins;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.collect.ImmutableList;

import de.kuschku.ircbot.Helper;
import de.kuschku.ircbot.format.BoldText;

public class ShipHandler extends ListenerAdapter<PircBotX> {
	
	public static List<String> getSyllablesFromWord(String word) {
        word = word.toLowerCase();
        List<String> syllables = new ArrayList<String>();
        int marker = 0;
        while(true) {
            List<String> clusters = getClustersFromWord(word.substring(marker));
            String currentSyllable = "";
            // corner cases with the ends of words
            if(clusters.size() == 1 && syllables.size() == 0) {
                // this word is just consonants. Not a real word. Return the word to avoid an exception
                syllables.add(clusters.get(0));
                break;

            } else if(clusters.size() == 2 && clusters.get(1).equals("e") && syllables.size() == 0) {
                // e.g. the word "the"
                syllables.add(clusters.get(0) + clusters.get(1));
                break;

            } else if(clusters.size() == 1) {
                // just a few consonants at the end of the word. Slap this onto the preceding syllable and end
                String lastSyllable = syllables.remove(syllables.size() - 1);
                lastSyllable = lastSyllable + clusters.get(0);
                syllables.add(lastSyllable);
                break;

            } else if(clusters.size() == 2 && clusters.get(1).equals("e")) {
                // perhaps a few consonants and an e (presumably silent) at the end. Slap all this onto the preceding vowel
                String lastSyllable = syllables.remove(syllables.size() - 1);
                if(clusters.get(0) != null) {
                    lastSyllable = lastSyllable + clusters.get(0);
                }
                lastSyllable = lastSyllable + clusters.get(1);
                syllables.add(lastSyllable);
                break;
            }

            if(clusters.get(0) != null) {
                // the opening consonant cluster always makes it in this syllable
                currentSyllable = currentSyllable + clusters.get(0);
            }

            if(isMultisyllableVowelCluster(clusters.get(1))) {
                // if the vowel cluster has multiple syllables, we break it up with the
                // over-simplified rule that the break after the first vowel
                currentSyllable = currentSyllable + clusters.get(1).substring(0, 1);
            } else {
                // if it's not a multisyllable vowel cluster then we add the whole
                // cluster, and maybe some consonants
                currentSyllable = currentSyllable + clusters.get(1);
                if(clusters.size() > 2 && clusters.get(2) != null && clusters.get(2).length() > 1) {
                    // if the next consonant cluster has multiple consonants, we break it
                    // up by breaking after the first consonant.
                    currentSyllable = currentSyllable + clusters.get(2).substring(0, 1);
                }
            }
            syllables.add(currentSyllable);
            marker += currentSyllable.length();
        }
        return syllables;
    }
    
    static boolean isMultisyllableVowelCluster(String cluster) {
        if(cluster.length() > 2) {
            return true;
        } else if(cluster.equals("eo")
                || cluster.equals("ia")
                || cluster.equals("io")
                || cluster.equals("ua")
                || cluster.equals("uo")) {
            return true;
        }
        return false;
    }
    
    static List<String> getClustersFromWord(String word) {
        List<String> clusters = new ArrayList<String>();
        StringBuffer currentCluster = new StringBuffer();
        boolean buildingConsonantCluster = true;
        for(int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if(i == 0 && isVowel(c)) {
                // corner case: we start with a vowel
                clusters.add(null); // the first cluster is null if we start with a vowel
                currentCluster.append(c);

            } else if(buildingConsonantCluster && isVowel(c)) {
                // we just finished a consonant cluster
                clusters.add(currentCluster.toString());
                currentCluster.delete(0, currentCluster.length());
                currentCluster.append(c);
                buildingConsonantCluster = false;

            } else if(!buildingConsonantCluster && isConsonant(c)) {
                // we just finished a vowel cluster
                clusters.add(currentCluster.toString());
                currentCluster.delete(0, currentCluster.length());
                currentCluster.append(c);
                buildingConsonantCluster = true;

            } else if(buildingConsonantCluster && isConsonant(c)) {
                // still building a consonant cluster
                currentCluster.append(c);

            } else if(!buildingConsonantCluster && isVowel(c)) {
                // still building vowel cluster
                currentCluster.append(c);
            }
        }
        clusters.add(currentCluster.toString());
        return clusters;
    }
    
    static boolean isConsonant(char c) {
        return !isVowel(c);
    }
    static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y'
                || c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U' || c == 'Y';
    }

    @Override
    public void onMessage(MessageEvent<PircBotX> event) throws Exception {
        ImmutableList<String> args = Helper.parseArgs(event.getMessage(),"!");
        if (args!=null && args.get(0).equalsIgnoreCase("ship")) {
            if (args.size()==3) {
            	event.getChannel().send().message(ship(args.get(1), args.get(2))+" or "+ship(args.get(2), args.get(1)));
            } else {
            	event.getChannel().send().message(new BoldText("Error: Please use exactly 2 names").toString());
            }
        }
    }
	
	public final static String ship(String a, String b) {
		String[] syllablesA = getSyllablesFromWord(a).toArray(new String[0]);
		String[] syllablesB = getSyllablesFromWord(b).toArray(new String[0]);
		
		String begin = "";
		String end = "";
		
		for (int i = 0; i <= (syllablesA.length-1)/2 || begin.length()<2; i++) {
			begin += syllablesA[i];
		}
		
		for (int i = 0; i <= (syllablesB.length-1)/2-0.5 || end.length()<2; i++) {
			end = syllablesB[syllablesB.length-i-1] + end;
		}
		
		return begin + end;
	}
}
