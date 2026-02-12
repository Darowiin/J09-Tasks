package ru.teamscore.securities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MoexSecuritiesParserTest {

    @Mock
    HttpClient mockClient;

    @Mock
    HttpResponse<String> mockResponse;

    @TempDir
    Path tempDir;

    private final String SAMPLE_JSON = """
            {
               "securities": {
                   "columns": ["secid", "shortname", "regnumber", "name", "emitent_title", "emitent_inn", "emitent_okpo", "is_traded"],
                   "data": [
                       ["YNDX", "Yandex", "1-01", "Yandex N.V.", "Yandex LLC", "12345", "67890", 1],
                       ["TRASH", "BadPaper", "0-00", "Not Traded", "Unknown", "000", "111", 0]
                   ]
               }
            }
            """;

    @Test
    void testParseSecurities_FiltersTradedOnly() {
        List<String[]> result = MoexSecuritiesParser.parseSecurities(SAMPLE_JSON);

        Assertions.assertEquals(1, result.size());

        String[] row = result.get(0);
        Assertions.assertEquals("YNDX", row[0]);
        Assertions.assertEquals("Yandex", row[1]);
    }

    @Test
    void testProcessQueryAsync_CreatesFile() throws IOException {
        Mockito.when(mockResponse.body()).thenReturn(SAMPLE_JSON);

        Mockito.when(mockClient.sendAsync(any(), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        String query = "Yandex";
        CompletableFuture<Void> future = MoexSecuritiesParser.processQueryAsync(mockClient, query, tempDir);

        future.join();

        Path expectedFile = tempDir.resolve("Yandex.csv");

        Assertions.assertTrue(Files.exists(expectedFile), "Файл CSV должен быть создан");

        List<String> lines = Files.readAllLines(expectedFile, StandardCharsets.UTF_8);

        Assertions.assertEquals(2, lines.size());
        Assertions.assertTrue(lines.get(0).contains("secid;shortname"));
        Assertions.assertTrue(lines.get(1).contains("YNDX;Yandex"));
    }

    @Test
    void testParseSecurities_HandleEmptyOrMissingData() {
        String emptyJson = "{ \"securities\": { \"columns\": [], \"data\": [] } }";
        List<String[]> result = MoexSecuritiesParser.parseSecurities(emptyJson);
        Assertions.assertTrue(result.isEmpty());
    }
}
