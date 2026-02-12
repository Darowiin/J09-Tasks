package ru.teamscore.securities;

import com.opencsv.CSVWriter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Приложение для поиска и сохранения информации о ценных бумагах с Московской Биржи (MOEX).
 * <p>
 * Класс выполняет асинхронные запросы к публичному API MOEX, фильтрует полученные данные
 * (оставляя только торгуемые бумаги) и сохраняет результаты в формате CSV.
 * </p>
 */
public class MoexSecuritiesParser {
    private static final String BASE_URL = "https://iss.moex.com/iss/securities.json?q=";
    private static final String[] TARGET_COLUMNS = {
            "secid", "shortname", "regnumber", "name",
            "emitent_title", "emitent_inn", "emitent_okpo"
    };

    private static final List<CompletableFuture<Void>> activeTasks = new CopyOnWriteArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Точка входа в программу.
     * <p>
     * Инициализирует HTTP-клиент, создает директорию для вывода и запускает цикл
     * обработки пользовательского ввода.
     * </p>
     *
     * @param args аргументы командной строки (не используются).
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        HttpClient client = HttpClient.newHttpClient();

        Path outputDir = Paths.get(System.getProperty("user.home"), "MOEX securities");

        try {
            if (Files.notExists(outputDir)) {
                Files.createDirectories(outputDir);
            }
        } catch (IOException e) {
            System.err.println("Ошибка создания директории: " + e.getMessage());
            return;
        }

        System.out.println("Введите поисковый запрос (например: Газпром) или /exit для выхода:");

        while (true) {
            String query = scanner.nextLine().trim();

            if (query.equalsIgnoreCase("/exit")) break;

            if (query.isEmpty()) continue;

            CompletableFuture<Void> task = processQueryAsync(client, query, outputDir);
            activeTasks.add(task);

            activeTasks.removeIf(CompletableFuture::isDone);
        }

        System.out.println("Ожидание завершения активных загрузок...");
        CompletableFuture.allOf(activeTasks.toArray(new CompletableFuture[0])).join();
        System.out.println("Программа завершена.");
    }

    /**
     * Выполняет асинхронный процесс поиска и сохранения данных.
     * <p>
     * Метод строит цепочку асинхронных вызовов:
     * <br>
     * 1. Отправка HTTP-запроса.
     * <br>
     * 2. Получение тела ответа.
     * <br>
     * 3. Парсинг JSON и фильтрация данных.
     * <br>
     * 4. Сохранение результата в CSV.
     * </p>
     *
     * @param client экземпляр {@link HttpClient} для выполнения запросов.
     * @param query строка поискового запроса.
     * @param outputDir путь к директории для сохранения файла.
     * @return {@link CompletableFuture}, представляющий асинхронную задачу.
     */
    static CompletableFuture<Void> processQueryAsync(HttpClient client, String query, Path outputDir) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create(BASE_URL + encodedQuery);

        System.out.println("Начало загрузки: " + query);

        HttpRequest request = HttpRequest.newBuilder(uri).build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(MoexSecuritiesParser::parseSecurities)
                .thenAccept(data -> saveToCsv(data, query, outputDir))
                .thenRun(() -> System.out.println("Загрузка завершена: " + query))
                .exceptionally(e -> {
                    System.err.println("Ошибка обработки '" + query + "': " + e.getMessage());
                    return null;
                });
    }

    /**
     * Разбирает JSON-ответ от сервера MOEX.
     * <p>
     * Ответ MOEX разделен на массивы метаданных (columns) и данных (data).
     * Метод сопоставляет имена колонок с их индексами, находит индекс колонки `is_traded`
     * и фильтрует записи, оставляя только торгуемые бумаги.
     * </p>
     *
     * @param jsonBody строка, содержащая JSON-ответ сервера.
     * @return список массивов строк, подготовленных для записи в CSV.
     * @throws RuntimeException если формат JSON некорректен или произошла ошибка парсинга.
     */
    static List<String[]> parseSecurities(String jsonBody) {
        List<String[]> resultRows = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode securitiesNode = root.path("securities");
            JsonNode columnsNode = securitiesNode.path("columns");
            JsonNode dataNode = securitiesNode.path("data");

            if (columnsNode.isMissingNode() || dataNode.isMissingNode()) {
                throw new RuntimeException("Некорректный формат ответа от MOEX");
            }

            Map<String, Integer> colIndexMap = new HashMap<>();
            for (int i = 0; i < columnsNode.size(); i++) {
                colIndexMap.put(columnsNode.get(i).asString(), i);
            }

            Integer isTradedIdx = colIndexMap.get("is_traded");
            if (isTradedIdx == null) return resultRows;

            int[] targetIndices = new int[TARGET_COLUMNS.length];
            for (int i = 0; i < TARGET_COLUMNS.length; i++) {
                targetIndices[i] = colIndexMap.getOrDefault(TARGET_COLUMNS[i], -1);
            }

            for (JsonNode rowNode : dataNode) {
                if (rowNode.get(isTradedIdx).asInt() == 1) {
                    String[] csvRow = new String[TARGET_COLUMNS.length];
                    for (int i = 0; i < targetIndices.length; i++) {
                        int idx = targetIndices[i];
                        csvRow[i] = (idx != -1 && !rowNode.get(idx).isNull())
                                ? rowNode.get(idx).asString()
                                : "";
                    }
                    resultRows.add(csvRow);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга JSON: " + e.getMessage());
        }
        return resultRows;
    }

    /**
     * Сохраняет подготовленные данные в CSV-файл.
     *
     * @param data      список строк для записи.
     * @param filename  имя файла (без расширения), соответствующее запросу.
     * @param outputDir путь к директории вывода.
     * @throws RuntimeException если произошла ошибка ввода-вывода при записи файла.
     */
    private static void saveToCsv(List<String[]> data, String filename, Path outputDir) {
        if (data.isEmpty()) {
            System.out.println("Не найдено торгуемых ценных бумаг для: " + filename);
            return;
        }

        Path filePath = outputDir.resolve(filename + ".csv");

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

            try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 CSVWriter writer = new CSVWriter(osw, ';',
                         CSVWriter.NO_QUOTE_CHARACTER,
                         CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                         CSVWriter.DEFAULT_LINE_END)) {

                writer.writeNext(TARGET_COLUMNS);
                writer.writeAll(data);
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка записи файла " + filename + ": " + e.getMessage());
        }
    }
}
