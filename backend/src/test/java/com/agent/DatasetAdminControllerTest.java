package com.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DatasetAdminController")
class DatasetAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE = "/api/admin/datasets";

    // Helper to create a dataset and return its ID
    private Long createTestDataset(String name, String tableName) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "name", name,
                "tableName", tableName,
                "orgId", 0
        ));
        String resp = mockMvc.perform(post(BASE)
                        .with(httpBasic("admin", "test123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asLong();
    }

    // ==================== DATASETS ====================

    @Nested
    @DisplayName("GET /api/admin/datasets")
    class ListDatasets {

        @Test
        @DisplayName("should return empty paginated list when no datasets exist")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get(BASE)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.page").value(0));
        }

        @Test
        @DisplayName("should return paginated datasets")
        void shouldReturnPaginatedDatasets() throws Exception {
            createTestDataset("数据集A", "table_a");
            createTestDataset("数据集B", "table_b");

            mockMvc.perform(get(BASE)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("should support search by name")
        void shouldSupportSearch() throws Exception {
            createTestDataset("销售数据", "sales");
            createTestDataset("用户数据", "users");

            mockMvc.perform(get(BASE)
                            .with(httpBasic("admin", "test123"))
                            .param("search", "销售")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("销售数据"));
        }

        @Test
        @DisplayName("should require authentication")
        void shouldRequireAuth() throws Exception {
            mockMvc.perform(get(BASE).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/datasets/{id}")
    class GetDataset {

        @Test
        @DisplayName("should return dataset by id")
        void shouldReturnDataset() throws Exception {
            Long id = createTestDataset("测试数据集", "test_table");

            mockMvc.perform(get(BASE + "/{id}", id)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value("测试数据集"))
                    .andExpect(jsonPath("$.tableName").value("test_table"));
        }

        @Test
        @DisplayName("should return 404 for nonexistent id")
        void shouldReturn404() throws Exception {
            mockMvc.perform(get(BASE + "/{id}", 9999)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/datasets")
    class CreateDataset {

        @Test
        @DisplayName("should create dataset and return 201")
        void shouldCreateDataset() throws Exception {
            String json = objectMapper.writeValueAsString(Map.of(
                    "name", "新数据集",
                    "tableName", "new_table",
                    "orgId", 0
            ));

            mockMvc.perform(post(BASE)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("新数据集"))
                    .andExpect(jsonPath("$.tableName").value("new_table"))
                    .andExpect(jsonPath("$.isEnabled").value(true))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldRejectBlankName() throws Exception {
            String json = objectMapper.writeValueAsString(Map.of(
                    "name", "",
                    "tableName", "t",
                    "orgId", 0
            ));

            mockMvc.perform(post(BASE)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when tableName has invalid characters")
        void shouldRejectInvalidTableName() throws Exception {
            String json = objectMapper.writeValueAsString(Map.of(
                    "name", "test",
                    "tableName", "bad table!",
                    "orgId", 0
            ));

            mockMvc.perform(post(BASE)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 on duplicate (org_id, table_name)")
        void shouldRejectDuplicate() throws Exception {
            createTestDataset("first", "dup_table");

            String json = objectMapper.writeValueAsString(Map.of(
                    "name", "second",
                    "tableName", "dup_table",
                    "orgId", 0
            ));

            mockMvc.perform(post(BASE)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/datasets/{id}")
    class UpdateDataset {

        @Test
        @DisplayName("should update dataset and return 200")
        void shouldUpdateDataset() throws Exception {
            Long id = createTestDataset("旧名称", "old_table");

            String json = objectMapper.writeValueAsString(Map.of(
                    "name", "新名称",
                    "tableName", "old_table",
                    "orgId", 0
            ));

            mockMvc.perform(put(BASE + "/{id}", id)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("新名称"));
        }

        @Test
        @DisplayName("should return 404 for nonexistent id")
        void shouldReturn404() throws Exception {
            String json = objectMapper.writeValueAsString(Map.of(
                    "name", "x", "tableName", "y", "orgId", 0
            ));

            mockMvc.perform(put(BASE + "/{id}", 9999)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/datasets/{id}")
    class DeleteDataset {

        @Test
        @DisplayName("should delete dataset and return 204")
        void shouldDeleteDataset() throws Exception {
            Long id = createTestDataset("待删除", "del_table");

            mockMvc.perform(delete(BASE + "/{id}", id)
                            .with(httpBasic("admin", "test123")))
                    .andExpect(status().isNoContent());

            // Verify it's gone
            mockMvc.perform(get(BASE + "/{id}", id)
                            .with(httpBasic("admin", "test123")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for nonexistent id")
        void shouldReturn404() throws Exception {
            mockMvc.perform(delete(BASE + "/{id}", 9999)
                            .with(httpBasic("admin", "test123")))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== FIELDS ====================

    @Nested
    @DisplayName("POST /api/admin/datasets/{id}/fields")
    class CreateField {

        @Test
        @DisplayName("should create field and return 201")
        void shouldCreateField() throws Exception {
            Long dsId = createTestDataset("字段测试", "field_test");

            String json = objectMapper.writeValueAsString(Map.of(
                    "fieldName", "amount",
                    "fieldAlias", "金额",
                    "dataType", "decimal",
                    "isDimension", false,
                    "isMetric", true,
                    "isFilterable", true
            ));

            mockMvc.perform(post(BASE + "/{id}/fields", dsId)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.fieldName").value("amount"))
                    .andExpect(jsonPath("$.datasetId").value(dsId.intValue()));
        }

        @Test
        @DisplayName("should return 400 for invalid dataType")
        void shouldRejectInvalidDataType() throws Exception {
            Long dsId = createTestDataset("类型测试", "type_test");

            String json = objectMapper.writeValueAsString(Map.of(
                    "fieldName", "x",
                    "dataType", "invalid_type"
            ));

            mockMvc.perform(post(BASE + "/{id}/fields", dsId)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when dataset not found")
        void shouldReturn404() throws Exception {
            String json = objectMapper.writeValueAsString(Map.of(
                    "fieldName", "x",
                    "dataType", "varchar"
            ));

            mockMvc.perform(post(BASE + "/{id}/fields", 9999)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 on duplicate field name")
        void shouldRejectDuplicateField() throws Exception {
            Long dsId = createTestDataset("重复字段测试", "dup_field_test");

            String json = objectMapper.writeValueAsString(Map.of(
                    "fieldName", "unique_field",
                    "dataType", "varchar"
            ));

            // First create succeeds
            mockMvc.perform(post(BASE + "/{id}/fields", dsId)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated());

            // Second create should fail with 409
            mockMvc.perform(post(BASE + "/{id}/fields", dsId)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/datasets/{id}/fields")
    class ListFields {

        @Test
        @DisplayName("should return paginated fields")
        void shouldReturnFields() throws Exception {
            Long dsId = createTestDataset("字段列表测试", "field_list");

            // Create 2 fields
            for (int i = 0; i < 2; i++) {
                String json = objectMapper.writeValueAsString(Map.of(
                        "fieldName", "field_" + i,
                        "dataType", "varchar"
                ));
                mockMvc.perform(post(BASE + "/{id}/fields", dsId)
                                .with(httpBasic("admin", "test123"))
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isCreated());
            }

            mockMvc.perform(get(BASE + "/{id}/fields", dsId)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }
    }

    // ==================== DATASET CONTEXT ====================

    @Nested
    @DisplayName("GET /api/datasets/{id}/context")
    class DatasetContext {

        @Test
        @DisplayName("should return full context with dataset, fields, and metrics")
        void shouldReturnFullContext() throws Exception {
            Long dsId = createTestDataset("上下文测试", "ctx_test");

            // Add a field
            String fieldJson = objectMapper.writeValueAsString(Map.of(
                    "fieldName", "revenue",
                    "fieldAlias", "收入",
                    "dataType", "decimal",
                    "isDimension", false,
                    "isMetric", true
            ));
            mockMvc.perform(post(BASE + "/{id}/fields", dsId)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON).content(fieldJson))
                    .andExpect(status().isCreated());

            // Add a metric
            String metricJson = objectMapper.writeValueAsString(Map.of(
                    "metricName", "总收入",
                    "formula", "SUM(revenue)"
            ));
            mockMvc.perform(post(BASE + "/{id}/metrics", dsId)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON).content(metricJson))
                    .andExpect(status().isCreated());

            // Get context
            mockMvc.perform(get("/api/datasets/{id}/context", dsId)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dataset.name").value("上下文测试"))
                    .andExpect(jsonPath("$.fields.length()").value(1))
                    .andExpect(jsonPath("$.fields[0].fieldName").value("revenue"))
                    .andExpect(jsonPath("$.metrics.length()").value(1))
                    .andExpect(jsonPath("$.metrics[0].metricName").value("总收入"));
        }

        @Test
        @DisplayName("should return empty fields/metrics arrays when none exist")
        void shouldReturnEmptyArrays() throws Exception {
            Long dsId = createTestDataset("空数据集", "empty_ctx");

            mockMvc.perform(get("/api/datasets/{id}/context", dsId)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fields").isArray())
                    .andExpect(jsonPath("$.fields.length()").value(0))
                    .andExpect(jsonPath("$.metrics").isArray())
                    .andExpect(jsonPath("$.metrics.length()").value(0));
        }

        @Test
        @DisplayName("should return 404 for nonexistent dataset")
        void shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/datasets/{id}/context", 9999)
                            .with(httpBasic("admin", "test123"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}
