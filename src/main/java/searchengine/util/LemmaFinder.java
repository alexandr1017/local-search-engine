package searchengine.util;

import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LemmaFinder {
    private static final String REGEX_RUS = "^[а-я]+$";
    private static final String REGEX_ENG = "^[a-z]+$";

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "CONJ", "ADJECTIVE", "PREP", "ADVERB", "PART"};
    private final RussianLuceneMorphology russianLuceneMorphology;
    private final EnglishLuceneMorphology englishLuceneMorphology;


    private LemmaFinder(RussianLuceneMorphology russianLuceneMorphology, EnglishLuceneMorphology englishLuceneMorphology) {
        this.russianLuceneMorphology = russianLuceneMorphology;
        this.englishLuceneMorphology = englishLuceneMorphology;
    }

    public static LemmaFinder getInstance() throws IOException {
        RussianLuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
        EnglishLuceneMorphology englishLuceneMorphology = new EnglishLuceneMorphology();
        return new LemmaFinder(russianLuceneMorphology, englishLuceneMorphology);
    }


    public Map<String, Integer> wordAndCountsCollector(String text) {
        Map<String, Integer> wordCounts = new HashMap<>();
        String pureText = clearHtmlToText(text);
        String[] words = pureText.split(" ");

        for (String word : words) {
            if (word.length() < 2) {
                continue;
            }

            if (word.matches(REGEX_RUS)) {
                List<String> russianNormalForms = russianLuceneMorphology.getNormalForms(word);

                List<String> wordBaseForms = russianLuceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                if (russianNormalForms.isEmpty()) {
                    continue;
                }
                addWordAndCountToMap(wordCounts, russianNormalForms);
            }

            if (word.matches(REGEX_ENG)) {
                List<String> englishNormalForms = englishLuceneMorphology.getNormalForms(word);

                List<String> wordBaseForms = englishLuceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                if (englishNormalForms.isEmpty()) {
                    continue;
                }
                addWordAndCountToMap(wordCounts, englishNormalForms);
            }
        }
        return wordCounts;
    }

    private void addWordAndCountToMap(Map<String, Integer> wordCounts, List<String> normalFormsList) {
        String normalWord = normalFormsList.get(0);
        if (wordCounts.containsKey(normalWord)) {
            wordCounts.put(normalWord, wordCounts.get(normalWord) + 1);
        } else {
            wordCounts.put(normalWord, 1);
        }
    }

    private String clearHtmlToText(String textHtml) {
        return textHtml.toLowerCase(Locale.ROOT)
                .replaceAll("<script.*?</script>", " ")
                .replaceAll("<[^>]*>", " ")
                .replaceAll("([^а-яa-z])", " ")
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
