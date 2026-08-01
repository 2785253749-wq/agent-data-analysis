package com.agent;

import com.agent.dto.QueryResult;
import com.agent.service.ResultSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResultSnapshotService")
class ResultSnapshotServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResultSnapshotService svc = new ResultSnapshotService(objectMapper);

    @Test
    void shouldTruncateRowsTo200() throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            rows.add(Map.of("region", "r" + i, "amount", i));
        }
        QueryResult qr = new QueryResult(List.of("region", "amount"), rows, 500, 100,
                null, true, "500 rows");

        String json = svc.build(1L, null, "SELECT * FROM t", null, true,
                List.of(), qr, null, null);

        var node = objectMapper.readTree(json);
        assertEquals(200, node.at("/queryResult/rows").size());
        assertEquals(500, node.at("/queryResult/rowCount").asInt());
        assertNull(node.get("explainPlan"));
    }

    @Test
    void shouldRedactParameters() throws Exception {
        String json = svc.build(1L, null, "SELECT * FROM t WHERE x = ${x}", Map.of("x", "secret-value"),
                true, List.of(), null, null, null);

        var node = objectMapper.readTree(json);
        assertEquals("***", node.at("/parameters/x").asText());
        assertFalse(json.contains("secret-value"));
    }

    @Test
    void shouldCapSnapshotSize() throws Exception {
        // Huge rows to force the 1MB cap → slim snapshot drops chart/interpretation.
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int c = 0; c < 50; c++) row.put("col" + c, "x".repeat(200));
            rows.add(row);
        }
        QueryResult qr = new QueryResult(List.of(), rows, 200, 1, null, false, "");

        String json = svc.build(1L, "{\"a\":1}", "SELECT 1", null, true, List.of(),
                qr, "{\"conclusion\":\"big\"}", "{\"type\":\"bar\"}");

        assertTrue(json.getBytes().length < 1_048_576, "snapshot must stay under 1MB");
        // Slim snapshot drops chart/interpretation AND queryResult rows to fit the cap.
        assertFalse(json.contains("\"chartSpec\""), "chart dropped in slim snapshot");
        assertFalse(json.contains("\"conclusion\""), "interpretation dropped in slim snapshot");
        assertFalse(json.contains("\"rows\""), "queryResult rows dropped in slim snapshot");
    }
}
