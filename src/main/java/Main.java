import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.util.LemmaFinder;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);

        LemmaFinder.getInstance().wordAndCountsCollector("Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).\n" +
                        "Чтобы выводить результаты порционно, также можно задать параметры offset (сдвиг от начала списка результатов) и limit (количество результатов, которое необходимо вывести).\n" +
                        "В ответе выводится общее количество результатов (count), не зависящее от значений параметров offset и limit, и массив data с результатами поиска. Каждый результат — это объект, содержащий свойства результата поиска (см. ниже структуру и описание каждого свойства).\n" +
                        "Если поисковый запрос не задан или ещё нет готового индекса (сайт, по которому ищем, или все сайты сразу не проиндексированы), метод должен вернуть соответствующую ошибку (см. ниже пример). Тексты ошибок должны быть понятными и отражать суть ошибок.\n" +
                        "\n" +
                        "Параметры:\n" +
                        "\n" +
                        "query — поисковый запрос;\n" +
                        "site — сайт, по которому осуществлять поиск (если 245345565465454654 не задан, поиск должен происходить по всем проиндексированным сайтам); задаётся в формате адреса, например: http://www.site.com (без слэша в конце);\n" +
                        "offset — сдвиг от 0 для постраничного вывода (параметр необязательный; если не установлен, то значение по умолчанию равно нулю);\n" +
                        "limit — количество результатов, которое необходимо вывести (параметр необязательный; если не установлен, то значение по умолчанию равно 20).\n")
                .forEach((k,v)-> System.out.println(k+ " - " + v));
    }
}
