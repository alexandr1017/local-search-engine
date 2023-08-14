package searchengine.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class SnippetGenerator {
    public static String getSnippet(String url, String query) {
        // Извлечь текст из веб-страницы
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String text = doc.body().text();

        // Найти все вхождения поискового запроса в тексте страницы
        int index = text.indexOf(query);
        int maxCount = 0;
        int startIndex = 0;
        while (index >= 0) {
            // Подсчитать количество вхождений в текущем фрагменте текста
            int count = 1;
            int endIndex = index + query.length();
            while ((index = text.indexOf(query, endIndex)) >= 0 && index - endIndex <= 50) {
                count++;
                endIndex = index + query.length();
            }

            // Обновить максимальное количество вхождений и начальный индекс фрагмента текста
            if (count > maxCount) {
                maxCount = count;
                startIndex = Math.max(0, endIndex - 100);
            }
        }

        // Выбрать фрагмент текста, содержащий наибольшее количество вхождений
        String snippet = text.substring(startIndex, Math.min(text.length(), startIndex + 200)) + "...";

        // Выделить совпадения с исходным поисковым запросом жирным шрифтом
        index = snippet.indexOf(query);
        while (index >= 0) {
            snippet = snippet.substring(0, index) + "<b>" + snippet.substring(index, index + query.length()) + "</b>" + snippet.substring(index + query.length());
            index = snippet.indexOf(query, index + query.length() + 7);
        }

        // Вернуть полученный сниппет
        return snippet;
    }
    public static void main(String[] args) {
        String url = "https://dombulgakova.ru/2023/04/10/mistika-v-zhizni-bulgakova/";
        String query = "холодочек";
        String snippet = getSnippet(url, query);
        System.out.println(snippet);
    }
}

