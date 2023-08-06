package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LemmaFinder {

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorphology;



    private LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public static LemmaFinder getInstance() throws IOException {
        LuceneMorphology morphology= new RussianLuceneMorphology();
        return new LemmaFinder(morphology);
    }


    public Map<String, Integer> wordAndCountsCollector(String text) {
        Map<String, Integer> wordCounts = new HashMap<>();

        String pureText = clearHtmlToCyrillicText(text);

        String[] words = pureText.split(" ");

        for (String word : words) {
            if (word.length() < 3) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (wordCounts.containsKey(normalWord)) {
                wordCounts.put(normalWord, wordCounts.get(normalWord) + 1);
            } else {
                wordCounts.put(normalWord, 1);
            }
        }
        return wordCounts;
    }

    private String clearHtmlToCyrillicText(String textHtml) {
        return textHtml.toLowerCase(Locale.ROOT)
                .replaceAll("<script.*?</script>", " ")
                .replaceAll("<[^>]*>", " ")
                .replaceAll("([^а-я])", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }


    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}
