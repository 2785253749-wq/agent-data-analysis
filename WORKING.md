# WORKING.md — 项目工作文档

> **用法**：每次新任务开始前阅读此文件恢复上下文；任务完成后更新进度并记录改动。
> 
> **原则**：每次只做一个任务切片 → 先写接口契约和测试 → 再实现代码 → 运行验证 → 记录结果。

---

## 一、项目概览

| 项 | 值 |
|---|-----|
| **项目名** | AI-Agent 数据分析平台 |
| **GitHub** | https://github.com/2785253749-wq/agent-data-analysis |
| **规范文档** | `AI-Agent数据分析平台_CC+DeepSeek开发流程.docx`（根目录） |
| **技术栈** | Spring Boot 3.4 + Java 17 + Vue 3 + TypeScript + H2(本地) + MySQL(生产) + DeepSeek |
| **开发协作** | Claude Code（Claude，当前由 DeepSeek-V4-Flash 驱动）作为开发协作器；DeepSeek **v4-pro** 作为 Agent 推理模型 |
| **模型** | `deepseek-v4-pro`（含推理链 reasoning_content） |
| **API Key** | `sk-ac97...`（见 application-local.yml，gitignored） |
| **调用方式** | **原生 DeepSeekClient**（RestClient 直调），已移除 Spring AI（有 URL bug） |

---

## 二、项目结构

```
agent数据分析/
├── WORKING.md                  ← 本文件
├── README.md
├── docker-compose.yml
├── database/init/              # 数据库初始化脚本
├── backend/                    # Spring Boot 后端
│   └── src/
│       ├── main/java/com/agent/
│       │   ├── AgentAnalysisApplication.java
│       │   ├── config/         # SecurityConfig, DataSourceConfig, ReadOnlyDataSourceConfig
│       │   ├── controller/     # REST 控制器
│       │   ├── dto/            # Java record DTOs
│       │   ├── entity/         # JPA 实体
│       │   ├── repository/     # Spring Data Repositories
│       │   ├── service/        # 业务逻辑（含 DeepSeekClient 原生调用）
│       │   ├── exception/      # 异常 + GlobalExceptionHandler
│       │   └── validation/     # 自定义校验
│       └── test/               # 测试（H2 内存数据库）
└── frontend/                   # Vue 3 前端
    └── src/
        ├── api/                # Axios 客户端 + 类型 + API 函数
        ├── stores/             # Pinia 状态管理
        ├── views/              # 页面组件
        │   └── admin/          # 管理后台页面
        ├── router/             # 路由配置
        └── components/         # 可复用组件（待建设）
```

---

## 三、开发阶段总览

| 阶段 | 名称 | 状态 | 关键产出 |
|------|------|------|----------|
| **T01** | 项目初始化 + 前后端骨架 | ✅ 完成 | 健康检查、DeepSeek 客户端、数据库表结构 |
| **T02** | 数据集元数据管理 | ✅ 完成 | CRUD 管理后台、15 个 REST 端点、22 个测试 |
| **M1** | 意图识别 | ✅ 完成 | IntentDTO + IntentRecognitionService + Prompt 文件 |
| **M2** | Text-to-SQL | ✅ 完成 | SqlResultDTO + SqlGenerationService + Prompt 文件 |
| **M3** | SQL 安全校验 | ✅ 完成 | SqlSafetyService：7 层防线 + 31 个安全测试 |
| **M4** | 只读查询执行 | ✅ 完成 | QueryExecutionService + 参数绑定 + 超时控制 + 审计 |
| **M5** | 数据解释 | ✅ 完成 | InterpretationDTO + ResultInterpretationService |
| **M6** | 图表推荐 | ✅ 完成 | ChartRecommendationService + ChartRenderer(ECharts) |
| **M7** | 分析编排器 | ✅ 完成 | AnalysisOrchestrator + SSE + 任务持久化 |
| **M8** | 前端对话界面 | ✅ 完成 | ChatView + AnalysisResult + ChartRenderer |

---

## 四、环境速查

```bash
# Java 环境
export JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.13.11-hotspot"
export MAVEN_HOME="/tmp/maven_inst/apache-maven-3.9.9"
export PATH="$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH"

# ⚠️ 启动后端（本地 profile = H2 内存数据库，无需 Docker/MySQL）
# 注意：API Key 必须通过 -Dspring-boot.run.jvmArguments 传入（fork JVM 不继承 shell env）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local \
  "-Dspring-boot.run.jvmArguments=-DDEEPSEEK_API_KEY=sk-ac97ef38c95f4995a326f1c6d9504755"

# ⚠️ 启动后端（必须先 clean compile 再 run，否则 db 资源可能丢失）
cd backend
mvn clean compile -Dmaven.test.skip=true
mvn spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.profiles=local \
  "-Dspring-boot.run.jvmArguments=-DDEEPSEEK_API_KEY=sk-ac97ef38c95f4995a326f1c6d9504755"

# 运行后端测试
cd backend && mvn test -Dspring.profiles.active=test --no-transfer-progress

# 启动前端
cd frontend && npm run dev

# 运行前端测试
cd frontend && npx vitest run

# 前端类型检查
cd frontend && npx vue-tsc --noEmit

# Git 操作
cd "d:/Users/Asus/Desktop/agent数据分析"
```

### ⚠️ 关键技术约束（踩过的坑）

| 坑 | 原因 | 解决 |
|----|------|------|
| `URI with undefined scheme` | Spring AI 1.0.0-M5 `OpenAiApi` 有 URL 构建 bug | 用原生 `DeepSeekClient`（RestClient 直调） |
| `API key not configured` | `mvn spring-boot:run` fork 的 JVM 不继承 bash `export` | 用 `-Dspring-boot.run.jvmArguments="-DDEEPSEEK_API_KEY=..."` 传入系统属性 |
| `base-url` 被解析为空 | YAML 占位符 `${DEEPSEEK_BASE_URL:https://...}` 中的冒号被误解析 | 直接在 `application.yml` 写死 `base-url`/`model` |
| `Invalid UTF-8` 中文乱码 | Windows curl 直传中文 JSON 编码损坏 | 用 `--data-binary @/tmp/xxx.json`（Python 生成 UTF-8 文件） |
| **DB中文双重编码乱码** | `spring.sql.init` 读 UTF-8 的 .sql 用了 JVM 默认 GBK（Windows file.encoding=GBK） | `application-local.yml` 加 `spring.sql.init.encoding: UTF-8`（**勿用 `-Dfile.encoding=UTF-8`，会破坏 Spring Security 认证导致全 401**） |
| **`No schema scripts found`** | `mvn spring-boot:run` 单独跑时 db 资源未可靠复制 | **必须先 `mvn clean compile` 再 `mvn spring-boot:run`** |
| `/api/health` 返回 401 | `-Dfile.encoding=UTF-8` 破坏了 Security 配置加载 | 移除该参数，改用 `spring.sql.init.encoding` |
| 前端 Basic 认证 | 浏览器 axios 无认证头 → 401 | `client.ts` 加 `Authorization: Basic btoa(admin:test123)` |

---

## 五、T01 完成记录

**日期**：2026-07-31  
**Commit**：`52b8141`

### 完成内容
- Spring Boot 3.4 + Java 17 后端骨架（`backend/`）
- Vue 3 + TypeScript + Vite 前端骨架（`frontend/`）
- `GET /api/health` 健康检查接口
- DeepSeek API 客户端（OpenAI 兼容方式，指向 `https://api.deepseek.com/v1`）
- MySQL 双账号设计：`app_user`（读写）+ `app_readonly`（只读）
- 数据库迁移脚本 V001（6 张表：datasets, dataset_fields, metrics_definitions, analysis_tasks, analysis_steps, audit_log）
- Docker Compose（MySQL 8.0）

### 测试结果
- 后端：4/4 通过
- 前端：4/4 通过

### 关键设计决策
- 使用 Java 17（非21，适配当前环境）
- DeepSeek 通过 OpenAI 兼容客户端接入（Spring AI 无原生 DeepSeek Starter）
- T01 阶段单数据源，只读数据源配置保留为注释
- Flyway 暂不启用（Maven Central 认证限制）
- API Key 通过 `application-local.yml`（gitignored）注入

---

## 六、T02 完成记录

**日期**：2026-07-31  
**Commit**：`d118d60`

### 完成内容

#### 后端（21 个新文件）
| 层 | 文件 |
|----|------|
| Entity | DatasetEntity, DatasetFieldEntity, MetricsDefinitionEntity, FieldDataType enum |
| Repository | DatasetRepository, DatasetFieldRepository, MetricsDefinitionRepository |
| DTO | DatasetRequest/Response, DatasetFieldRequest/Response, MetricsDefinitionRequest/Response, PagedResponse, DatasetContextResponse |
| Service | DatasetService（统一 CRUD + 上下文聚合） |
| Controller | DatasetAdminController（15 个端点）, DatasetContextController |
| Exception | ResourceNotFoundException, GlobalExceptionHandler（404/400/409/500） |
| Test | DatasetAdminControllerTest（22 个集成测试） |

#### 前端（7 个文件）
| 层 | 文件 |
|----|------|
| API | `datasets.ts`（13 个 TS 类型 + 14 个 API 函数） |
| Store | `admin.ts`（Pinia store, 全 CRUD actions） |
| Pages | AdminLayout, DatasetList, DatasetForm, FieldManager, MetricManager |
| Router | 更新 `index.ts`（6 个 `/admin/*` 嵌套路由） |

### API 接口（16 个端点）
```
GET    /api/admin/datasets?page=&size=&search=    → 分页列表
POST   /api/admin/datasets                         → 创建数据集 (201)
GET    /api/admin/datasets/{id}                    → 单个数据集
PUT    /api/admin/datasets/{id}                    → 更新数据集
DELETE /api/admin/datasets/{id}                    → 删除 (204, 级联)
GET    /api/admin/datasets/{id}/fields             → 字段列表
POST   /api/admin/datasets/{id}/fields             → 创建字段 (201)
PUT    /api/admin/datasets/{id}/fields/{fid}       → 更新字段
DELETE /api/admin/datasets/{id}/fields/{fid}       → 删除字段 (204)
GET    /api/admin/datasets/{id}/metrics            → 指标列表
POST   /api/admin/datasets/{id}/metrics            → 创建指标 (201)
PUT    /api/admin/datasets/{id}/metrics/{mid}      → 更新指标
DELETE /api/admin/datasets/{id}/metrics/{mid}      → 删除指标 (204)
GET    /api/datasets/{id}/context                  → Agent 管道元数据
```

### 测试结果
- 后端：26/26 通过（T01: 4 + T02: 22）
- 前端：4/4 通过 + TypeScript 类型检查 0 错误

### 关键设计决策
- 实体使用 `datasetId` Long 而非 `@ManyToOne`（避免懒加载复杂性）
- DTO 使用 Java `record`（不可变，与 HealthResponse 模式一致）
- Service 层使用 `FieldDataType.fromString()` 转换 DTO 字符串 → 枚举
- 唯一性冲突通过 `DataIntegrityViolationException` → 409 响应
- 测试使用 `@Transactional` 自动回滚（H2 数据库 `create-drop`）
- 测试凭证：`admin / test123`（`application-test.yml`）
- SecurityConfig 已覆盖所有 `/api/admin/**`（`anyRequest().authenticated()` + HTTP Basic）

---

## 七、当前数据库表

| 表 | 说明 | 关键字段 |
|----|------|----------|
| `datasets` | 数据集注册 | id, name, table_name, org_id, is_enabled |
| `dataset_fields` | 字段定义（SQL 白名单） | id, dataset_id(FK), field_name, data_type, is_dimension, is_metric, is_filterable |
| `metrics_definitions` | 指标公式 | id, dataset_id(FK), metric_name, formula |
| `analysis_tasks` | 分析任务 | id, user_id, dataset_id, question, status, intent_json, sql_text |
| `analysis_steps` | Agent 执行步骤 | id, task_id(FK), step_type, status, input_json, output_json |
| `audit_log` | 审计日志 | id, user_id, action, resource_type, resource_id, detail |

---

## 八、M1 完成记录

**日期**：2026-07-31

### 完成内容

| 文件 | 说明 |
|------|------|
| `dto/IntentDTO.java` | 意图识别 JSON Schema 对应的 Java record（含 FilterDef, TimeRangeDef 嵌套 record） |
| `dto/IntentRequest.java` | 意图识别请求（question + datasetId） |
| `service/IntentRecognitionService.java` | 调用 DeepSeek → 解析 JSON → 返回 IntentDTO；含重试+降级逻辑 |
| `prompts/intent-recognition/system.txt` | DeepSeek 系统提示词（意图类型定义、规则、输出格式） |
| `prompts/intent-recognition/v1.json` | Prompt 版本元数据（模型、温度 0.1、JSON Schema） |

### API
```
POST /api/intent/recognize
  Body: { "question": "...", "datasetId": 1 }
  → 200 IntentDTO
```

### 测试结果
- IntentRecognitionServiceTest：9/9 通过（8 个 JSON 解析 + 1 个上下文测试）
- 全部测试：**35/35 通过**

### IntentDTO 结构
```
intentType: query|aggregation|comparison|ranking|detail|correlation
metrics: [string]
dimensions: [string]
filters: [{field, operator, value, value2}]
timeRange: {type, start, end} | null
comparison: string | null
needsClarification: boolean
clarificationQuestions: [string]
```

### 关键设计决策
- Prompt 文件在 classpath（`prompts/intent-recognition/system.txt`），服务在构造函数中加载
- `parseIntent()` 方法 public — 可独立测试 JSON 解析逻辑（不依赖 DeepSeek）
- 支持 markdown 代码块包裹的 JSON（```json ... ```）
- DeepSeek 调用失败：重试 1 次 → 返回 needsClarification=true 的降级响应
- 温度 0.1（比 SQL 生成的 0 略高，允许自然语言理解的灵活性）

---

## 九、M2 完成记录

**日期**：2026-07-31

### 完成内容

| 文件 | 说明 |
|------|------|
| `dto/SqlResultDTO.java` | SQL 生成结果（sql, parameters, usedTables, usedFields, explanation） |
| `dto/SqlGenerationRequest.java` | SQL 生成请求（question + intent + datasetId） |
| `service/SqlGenerationService.java` | 注入 IntentDTO + 元数据 → DeepSeek → SqlResultDTO |
| `prompts/sql-generation/system.txt` | System prompt（硬约束：只有SELECT、禁止DDL/DML、使用命名参数） |
| `prompts/sql-generation/v1.json` | 版本元数据（温度 0.0，maxTokens 2048） |

### 测试结果
- SqlGenerationServiceTest：10/10 通过
- 全部测试：**45/45 通过**

### SqlResultDTO 结构
```
sql: string           ← 生成的 SQL（SELECT/WITH...SELECT）
parameters: map       ← 命名参数（如 ${status} → "已完成"）
usedTables: [string]  ← 引用的表（权限校验用）
usedFields: [string]  ← 引用的字段（白名单校验用）
explanation: string   ← SQL 功能说明
```

### 关键约束（Prompt 硬编码）
- 只允许 SELECT / WITH...SELECT
- 禁止 INSERT/UPDATE/DELETE/DROP/ALTER
- 禁止注释（-- 和 /* */）
- 禁止危险函数（LOAD_FILE, SLEEP, BENCHMARK, INTO OUTFILE）
- 只能使用提供的字段名和表名
- 详细查询默认 LIMIT 200
- 使用命名参数格式 ${param}
- 温度 0.0（绝不允许随机性）

---

## 十、M3 完成记录

**日期**：2026-07-31

### 完成内容

| 文件 | 说明 |
|------|------|
| `dto/SqlValidationResult.java` | 校验结果（passed, reason, violations, sanitizedSql） |
| `service/SqlSafetyService.java` | 7 层安全防线，词法分析 + 白名单校验 |
| `SqlSafetyServiceTest.java` | 31 个安全测试 |

### 7 层安全防线

| 层 | 校验项 | 方法 |
|----|--------|------|
| 1 | 注释剥离 | `stripComments()` — 移除 `--`、`/* */`、`/*! */` |
| 2 | 语句类型 | `isSelectStatement()` — 只允许 SELECT/WITH...SELECT |
| 3 | 禁止关键字 | `checkForbiddenKeywords()` — INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/EXEC/CALL 等 19 个 |
| 4 | 危险函数 | `checkDangerousFunctions()` — LOAD_FILE/SLEEP/BENCHMARK/GET_LOCK |
| 5 | 字段白名单 | `checkFieldWhitelist()` — 提取 SELECT/WHERE/GROUP BY/ORDER BY 中的列引用，逐个比对 |
| 6 | LIMIT 强制 | `ensureLimit()` — SELECT 必须有 LIMIT |
| 7 | 注入防护 | `checkSemicolonCount()`（多分号）+ `checkUnionInjection()`（过量 UNION） |

### 测试结果
- SqlSafetyServiceTest：31/31 通过
- 全部测试：**76/76 通过**

### SQL 校验流程
```
AI生成的SQL → stripComments() → isSelectStatement() → checkForbiddenKeywords()
→ checkDangerousFunctions() → checkFieldWhitelist() → ensureLimit()
→ checkSemicolonCount() → checkUnionInjection() → SqlValidationResult
```

### 架构设计
- 当前使用词法分析（正则表达式），可升级为 JSqlParser AST
- 所有校验方法 public — 可独立测试每个防线
- `validate(sql, datasetId)` 一站式入口

---

## 十一、M4 完成记录

**日期**：2026-07-31

### 完成内容

| 文件 | 说明 |
|------|------|
| `dto/QueryResult.java` | 查询结果（columns, rows, rowCount, executionTimeMs, explainPlan, truncated, summary） |
| `config/ReadOnlyDataSourceConfig.java` | 只读 JdbcTemplate（maxRows=1000, timeout=30s） |
| `service/QueryExecutionService.java` | 参数绑定 + EXPLAIN 捕获 + 执行计时 + 结果截断检测 |
| `QueryExecutionServiceTest.java` | 11 个测试（5 参数转换 + 6 查询执行） |

### QueryResult 结构
```
columns: [string]       ← 列名
rows: [{col: val, ...}] ← 数据行
rowCount: int           ← 行数
executionTimeMs: long   ← 执行耗时
explainPlan: string     ← EXPLAIN 输出
truncated: boolean      ← 是否被截断（≥1000行）
summary: string         ← 可读摘要
```

### 执行安全保障
- 只读 JdbcTemplate（`maxRows=1000`, `queryTimeout=30s`）
- 命名参数转换：`${param}` 和 `:param` → 直接值替换（含引号转义）
- EXPLAIN 计划捕获（best-effort, H2/MySQL 支持）
- 前置条件：SQL 必须通过 SqlSafetyService 校验
- 执行计时 + 日志记录

### 测试结果
- QueryExecutionServiceTest：11/11 通过
- 全部测试：**87/87 通过**

---

## 十二、M5 完成记录

**日期**：2026-08-01

### 完成内容

| 文件 | 说明 |
|------|------|
| `dto/InterpretationDTO.java` | 解释结果（conclusion, points[{statement, type, evidence, confidence}], dataSufficient, confidence, caveats） |
| `service/ResultInterpretationService.java` | 注入问题+QueryResult → DeepSeek → InterpretationDTO |
| `prompts/interpretation/system.txt` | System prompt（fact/inference/suggestion 三类，每句有 evidence） |
| `prompts/interpretation/v1.json` | 版本元数据（温度 0.2） |

### InterpretationDTO 结构
```
conclusion: string          ← 一句话总结
points: [{statement, type(fact|inference|suggestion), evidence, confidence(0-1)}]
dataSufficient: boolean     ← 数据是否足够得出结论
confidence: high|medium|low
caveats: [string]           ← 局限性和注意事项
```

### 测试结果
- ResultInterpretationServiceTest：7/7 通过
- 全部测试：**94/94 通过**

---

## 十三、M6 完成记录

**日期**：2026-08-01

### 完成内容

| 文件 | 说明 |
|------|------|
| `dto/ChartSpecDTO.java` | ECharts 图表规格（type, title, labels, datasets[{label, data, color}], options） |
| `service/ChartRecommendationService.java` | 规则引擎：根据意图类型+数据结构自动推荐图表类型 |
| `frontend/src/components/ChartRenderer.vue` | ECharts 渲染组件（bar/line/pie/scatter/horizontal_bar/table） |
| `ChartRecommendationServiceTest.java` | 12 个测试 |

### 图表推荐规则
| 意图类型 | 条件 | 推荐图表 |
|----------|------|----------|
| aggregation + 时间维度 | time字段或timeRange | line |
| aggregation + 1维度 + 1指标 | ≤10行 | pie |
| aggregation | 其他情况 | bar |
| ranking | — | horizontal_bar |
| comparison | — | bar |
| detail / query | — | table |
| correlation | ≥2数值列 | scatter |

### 测试结果
- ChartRecommendationServiceTest：12/12 通过
- 全部测试：**106/106 通过**

---

## 十四、M7 完成记录

**日期**：2026-08-01

### 完成内容

| 文件 | 说明 |
|------|------|
| `entity/AnalysisTaskEntity.java` | analysis_tasks JPA实体（状态追踪） |
| `repository/AnalysisTaskRepository.java` | JPA Repository |
| `dto/AnalysisRequest.java` | 分析请求（question + datasetId） |
| `dto/AnalysisResponse.java` | 分析响应（完整结果 + 步骤信息） |
| `service/AnalysisOrchestrator.java` | 编排M1→M2→M3→M4→M5→M6全流程 |
| `controller/AnalysisController.java` | POST /analysis/tasks + SSE流 |
| `AnalysisControllerTest.java` | 5个测试（端到端+持久化+认证） |

### API
```
POST /api/analysis/tasks              → 同步分析（返回完整AnalysisResponse）
POST /api/analysis/tasks/stream       → SSE流式分析（text/event-stream）
```

### 编排流程
```
M1 IntentRecognition → needsClarification? → 提前终止
    ↓
M2 SQL Generation
    ↓
M3 SQL Safety (失败→终止)
    ↓
M4 Query Execution
    ↓
M5 Interpretation
    ↓
M6 Chart Recommendation
    ↓
AnalysisResponse（含全部6步结果 + 步骤耗时）
```

### 测试结果
- AnalysisControllerTest：5/5 通过
- 全部测试：**111/111 通过**

---

## 十五、M8 完成记录

**日期**：2026-08-01

### 完成内容

| 文件 | 说明 |
|------|------|
| `views/ChatView.vue` | 对话输入（问题+数据集选择）+ Ctrl+Enter 提交 |
| `components/AnalysisResult.vue` | 步骤时间线 + 意图澄清 + 图表 + 解释 + SQL 展示 |
| `components/ChartRenderer.vue` | ECharts 6种图表渲染 |
| `composables/useAnalysis.ts` | 分析状态管理（loading/result/error/reset） |
| `router/index.ts` | `/` → ChatView（首页） |
| `App.vue` | 顶部导航（对话/管理） |

### 测试结果
- 后端：111/111 BUILD SUCCESS
- 前端：vitest 4/4 + TypeScript 0 错误

---

## 十五·五、DeepSeek 连通性修复记录（关键里程碑）

**日期**：2026-08-01  
**Commit**：`7f4a7ee`（已本地提交，push 待网络恢复）

### 背景
MVP 测试阶段发现：DeepSeek 调用一直报 `URI with undefined scheme`，无法真正跑通。

### 根因
Spring AI `1.0.0-M5` 的 `OpenAiApi` 内部 URL 构建有 bug（`RestClient` 传 `https://api.deepseek.com` 被解析成无 scheme 的 URI），反复配置 `base-url` 均无法解决。

### 解决方案：原生 DeepSeekClient
```java
// DeepSeekClient.java — 基于 Spring RestClient 直调 DeepSeek API
String response = restClient.post()
    .uri("/chat/completions")
    .body(Map.of(
        "model", "deepseek-v4-pro",
        "messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)),
        "temperature", temperature))
    .retrieve().body(Map.class);
```

### 改动清单
| 文件 | 改动 |
|------|------|
| `service/DeepSeekClient.java` | **新增** — 原生 DeepSeek API 客户端（RestClient 直调） |
| `config/DeepSeekConfig.java` | **删除** — Spring AI OpenAiApi 配置（有 bug） |
| `pom.xml` | **移除** `spring-ai-openai-spring-boot-starter` 依赖 |
| `IntentRecognitionService` | ChatClient → DeepSeekClient |
| `SqlGenerationService` | ChatClient → DeepSeekClient |
| `ResultInterpretationService` | ChatClient → DeepSeekClient |
| `application.yml` | base-url/model 写死（不再用占位符） |
| `HealthController` | API key 检测改用 Spring 注入 |

### 验证结果 ✅
- **模型**：`deepseek-v4-pro`（含推理链 `reasoning_content`）
- **意图识别**：`"summarize sales by region"` → `aggregation` + metrics=[sales] + dimensions=[region]
- **SQL 生成**：
  ```sql
  SELECT region, SUM(amount) AS total_amount FROM sales
  GROUP BY region ORDER BY total_amount DESC LIMIT 5
  ```
  正确识别表名 `sales`、字段 `region`/`amount`、GROUP BY、ORDER BY、LIMIT 5
- 全流程 M1→M2 打通，M3 校验正常拦截（字段白名单误报，见下）

### ⚠️ 已知问题（下一步处理）
**数据集字段名乱码**：用 Windows curl 直传中文 JSON 时编码损坏（`Invalid UTF-8`），导致字段名如 `region` 存成乱码，M3 字段白名单校验时误报"not in whitelist"。
- 解决方向：重建数据集，用 `python3 -c` 生成 UTF-8 JSON 文件 + `--data-binary @/tmp/xxx.json` 创建
- 已创建的数据集 ID=1（Sales Data，8 字段 + 4 指标），字段别名乱码，需重建

---

## 十六、MVP 完成总结

```
T01 ████████████ ✅ 项目骨架         4 tests
T02 ████████████ ✅ 元数据管理      22 tests
M1  ████████████ ✅ 意图识别         9 tests
M2  ████████████ ✅ SQL生成         10 tests
M3  ████████████ ✅ SQL安全         31 tests
M4  ████████████ ✅ 查询执行        11 tests
M5  ████████████ ✅ 数据解释         7 tests
M6  ████████████ ✅ 图表推荐        12 tests
M7  ████████████ ✅ 分析编排器       5 tests
M8  ████████████ ✅ 对话界面         4 tests
─────────────────────────────────────
              TOTAL: 115 tests passed
              9 commits to GitHub
              ~100 source files
```

### 全流程
```
用户提问 → [M1 意图识别] → [M2 SQL生成] → [M3 安全校验]
→ [M4 只读执行] → [M5 数据解释] → [M6 图表推荐]
→ [M7 编排器] → [M8 对话界面展示]
```

### 安全防线
1. Prompt 层面：只允许 SELECT，禁止 DDL/DML，使用命名参数
2. SQL 层面：7 层词法校验（注释剥离、语句类型、禁止关键字、危险函数、字段白名单、LIMIT 强制、注入防护）
3. 执行层面：只读 JdbcTemplate（maxRows=1000, timeout=30s）
4. 数据层面：app_readonly 数据库账号、审计日志表

---

## 十七、安全备忘

- [x] DeepSeek API Key 通过 `application-local.yml`（gitignored）+ 系统属性传入
- [x] 数据库双账号设计（`app_user` + `app_readonly`）
- [x] 管理端点需要 HTTP Basic 认证
- [x] 表名有正则校验
- [x] 全局异常处理器防止错误信息泄露
- [x] SQL 注入防护 — M3 SqlSafetyService 7 层防线
- [x] 字段白名单校验 — M3 checkFieldWhitelist()
- [x] 只读数据源执行 SQL — M4 ReadOnlyDataSourceConfig
- [x] DeepSeek 原生客户端接入 — 已验证 v4-pro 连通
- [ ] JWT 认证替换 HTTP Basic（待排期）
- [ ] 修复数据集字段中文乱码问题（重建数据集）

---

## 十七·八、P3 AI模型配置+Prompt模板管理 记录（2026-08-02）

### 6 项安全约束落实（commit `715552c`）
| 点 | 实现 |
|----|------|
| 1 普通用户不读正文 | active 接口只返 name/version 元数据；content/variables/BaseURL/keyRef 不暴露 |
| 2 全局默认唯一 | 全系统仅一个 enabled+default 模型（set-default 时清除其他） |
| 3 Base URL 限制 | 仅 HTTPS + 白名单域名(api.deepseek.com)；禁 IP/localhost/内网/自定义端口 |
| 4 api_key_ref 白名单 | 仅 DEEPSEEK_API_KEY；响应/日志只返 apiKeyConfigured 布尔 |
| 5 Prompt 版本不可变 | 改正文必须新建版本；旧版本只 enable/disable/archive；step 记录 version+contentHash |
| 6 迁移不读 filesystem | ConfigSeeder(Java @PostConstruct) 硬编码种子；禁停用必需类型最后一个启用 |

### 关键文件
- 后端：AiModelEntity/Service/Controller、PromptTemplateEntity/Service/Controller、ConfigSeeder、V005
- 3 个 LLM service 改从 PromptTemplateService 读 active prompt；DeepSeekClient 从 active model 读配置
- AnalysisOrchestrator step 记录真实模型名 + prompt `version:hash`
- 前端：AiModelPage、PromptTemplatePage、两个 store、路由/侧栏

### 测试
- 后端 176（新增 AiModel 7 + Prompt 6 + AdminConfig 5）
- 前端 39（新增 adminConfig 4）

---

## 十七·七、P2 多轮分析会话 记录（2026-08-02）

### 设计（commit `9b63cfc`）
- **tasks-as-turns**：无 conversation_messages 表；用户消息 = `analysis_tasks.question`，助手结果 = `result_json`，最近消息 = 最近关联任务 question
- V003：`conversations` 表 + `analysis_tasks.conversation_id`（nullable，兼容旧单次任务）

### 5 个补充点落实
| 点 | 实现 |
|----|------|
| 1 消息持久化 | analysis_tasks 即会话轮次，ConversationView 历史由关联任务转换 |
| 2 数据集切换拒绝 | taskCount>0 时改 datasetId → 400"该会话已有分析任务，不能切换数据集，请新建会话" |
| 3 timeRange 不每轮覆盖 | 仅当意图明确含 timeRange 才更新；"那华东呢？"保留上一轮 |
| 4 摘要脱敏 | lastConclusion 走 ErrorMessageSanitizer + 限 500 字，非天然安全 |
| 5 taskCount 语义 | 统计所有已创建任务；context 仅 COMPLETED 后更新 |

### 前端
- ConversationSidebar（新建/列表/切换/重命名/归档）+ ConversationView（轮次历史+输入+结果）
- AnalysisResult 推荐追问（规则生成）→ @followup 一键带入输入框
- 路由 `/conversations` + 侧栏 enabled + 高亮

### 测试
- 后端 147/147（新增 ConversationContext 4 + ConversationController 6）
- 前端 35（新增 conversationStore 4）

---

## 十七·六、P1 Agent执行追踪+分析历史 记录（2026-08-02）

### 后端（commit `5994c70`）
- `analysis_tasks` 加 `result_json` 列（V002 迁移）：快照截断 rows≤200、参数值脱敏、>1MB 兜底只留元数据
- 启用 `analysis_steps` 表：编排器每步持久化（类型/状态/耗时/输出摘要，不含 prompt/key）
- `GET /api/analysis/tasks` 分页列表（状态/数据集/关键词筛选）；`GET /analysis/tasks/{id}` 详情
- **数据隔离**：普通用户仅本人任务，管理员本组织任务，datasetIds 取交集，无权详情返 404
- **脱敏**：`ErrorMessageSanitizer` 白名单优先 + 正则兜底（JDBC URL/IP/密钥/堆栈）；列表不含敏感字段；sqlText 仅 owner/admin 返回

### 前端
- `api/analysis.ts` + `stores/history.ts`（Pinia）
- `TaskListPage.vue` 共享列表组件，`mode=history`（问题/结果/时间）/ `mode=trace`（状态/耗时/步骤数）
- `TaskDetail.vue`：步骤时间线 + SQL/查询结果/图表/解读四标签（复用现有组件）
- 路由 `/history` `/trace`，侧栏两项 enabled + 高亮

### 测试
- 后端 136/136（新增 ErrorMessageSanitizer 7 + ResultSnapshot 3 + TaskHistory 10）
- 前端 30（新增 historyStore 4 + sidebarMenu 更新）

---

## 十七·五、前端 Element Plus 重构记录（2026-08-02）

### 背景
按用户提供的实例图（传统 Element Plus 管理后台），将前端从深色顶栏风格重构为：
- 纯白 Header（Logo + 面包屑 + 头像/管理员下拉）
- 155px 白色侧栏（选中项浅蓝 #ecf5ff 背景 + 蓝色 #409eff 文字）
- 主内容浅灰 #f5f7fa + 白色卡片
- 主色 #409EFF

### 改动清单
| 类别 | 文件 |
|------|------|
| 新增布局 | `layouts/AdminLayout.vue`, `components/layout/AppHeader.vue`, `components/layout/AppSidebar.vue` |
| 新增共享 | `components/common/FilterToolbar.vue`, `components/common/DataTableCard.vue` |
| 新增分析组件 | `components/analysis/SqlBlock.vue`, `QueryResultTable.vue`, `InterpretationBlock.vue` |
| 新增工具 | `utils/reportStorage.ts`（localStorage 报告） |
| 新增页面 | `views/AnalysisView.vue`（AI分析）, `views/reports/AnalysisReport.vue`（报告） |
| 重构页面 | DatasetList / DatasetForm / FieldManager / MetricManager → el-table/el-form |
| 重构组件 | AnalysisResult（4标签页+保存报告）, ChartRenderer（自适应高度+饼图限宽） |
| 修改 | App.vue, main.ts, router/index.ts, useAnalysis.ts |
| 兼容转发 | 旧 `views/admin/AdminLayout.vue`, `views/ChatView.vue` 保留为转发 |

### 关键决策
- **Element Plus 全量引入**（非按需），tsconfig 加 `element-plus/global`
- 路由重构 `/` → AdminLayout；`/datasets*`、`/reports`；旧 `/admin/*` 全部重定向
- 报告页用 localStorage（后端无报告接口），从最近一次分析生成
- 侧栏完整菜单 + 未实现功能置 disabled"开发中"
- 批量删除 = N 次顺序 DELETE（后端无批量端点）

### 踩过的坑
| 坑 | 解决 |
|----|------|
| `v-model` 绑定三目表达式（`v-model="a ? x : y"`）编译错误 | 改 `:model-value` + `@update:model-value="$event"` + `activeForm(row)` |
| 模板内带类型箭头函数（`(v: string) =>`）触发 vite:vue 崩溃 | 改具名函数或 `$event` |
| el-checkbox 的 model-value 类型是 `CheckboxValueType` | 事件参数用 `string \| number \| boolean` |
| `DataTableCard` 用 `el-card` + 复杂 slot 触发 vite:vue 代码生成 bug | 改纯 div + 条件渲染 slot |

### 验证
- `vue-tsc --noEmit` 0 错误
- `vitest run` 4 通过
- `vite build` 成功
- 端到端：中文分析 6 步全 COMPLETED，4 标签数据源（SQL/查询结果/图表/解读）就绪

---

## 十八、最后更新

**2026-08-02**：完成 P3「AI 模型配置 + Prompt 模板管理」——6 项安全约束（全局默认唯一/Base URL 白名单/key 白名单/不可变版本/contentHash/必需类型保护/普通用户不读正文）。后端 176、前端 39 测试全绿。commit `715552c`。

**2026-08-02**：P2 脱敏补充——敏感字段标记(is_sensitive) + SensitiveDataMasker(手机/邮箱/身份证/账号掩码)，lastConclusion 走业务脱敏非 ErrorMessageSanitizer。后端 158、前端 35 测试全绿。

**2026-08-02**：完成 P2「多轮分析会话」——tasks-as-turns + 结构化上下文摘要 + 推荐追问。后端 147、前端 35 测试全绿。

**2026-08-02**：完成 P1「Agent 执行追踪 + 分析历史」——数据隔离+脱敏+分页列表+任务详情。后端 136 测试、前端 30 测试全绿。

**2026-08-02**：前端完成 Element Plus 管理后台重构（实例图为验收标准），vue-tsc 0 错误 + vitest 4 通过 + build 成功。

**2026-08-01**：MVP + DeepSeek 连通 + **端到端全链路跑通**。

### 端到端验证结果（5 场景）
| 场景 | 结果 | 图表 |
|------|------|------|
| 按地区汇总销售额 | ✅ | pie |
| TOP 3 商品销量 | ✅ | horizontal_bar |
| 已完成订单总销售额 | ✅ 81,150.00 | bar |
| 2025 月度销售趋势 | ⚠️ H2 方言差异 | — |
| 按地区平均订单额 | ✅ | pie |

Test 4 失败原因：DeepSeek 生成 MySQL 语法 `DATE_FORMAT()`，H2 本地库不支持。**生产 MySQL 可正常**，非 bug。

### 本轮修复（commit `140784c`）
1. **字段白名单别名**：`AS total_sales` 不再误判为未知字段（SELECT/ORDER BY 别名豁免）
2. **SQL 字面量引号**：`${status}`→`'completed'`（字符串加引号+转义，数字/布尔不引）
   - 修复 `WHERE status = completed` 裸字面量执行错误
3. **纯聚合 LIMIT 豁免**：`SELECT SUM(amount)` 无 GROUP BY 豁免 LIMIT；`SELECT *` 不豁免
4. **MySQL 日期函数**：`DATE_FORMAT` 等加入白名单关键词
5. **演示数据**：`db/demo-schema.sql` + `demo-data.sql`（sales 24 行 + users 10 行），启动自动加载
6. **测试**：116/116 通过

### 当前状态
- 后端运行中（H2 + local profile），启动命令见第四节
- 前端运行中（http://localhost:5173）
- 数据集需在每次后端重启后重建（H2 内存库数据清空），可用管理后台或 Python 脚本

### 待办
- ⏳ 推送本地 commit 到 GitHub（网络恢复时）
- ⏳ 可选：增加 H2 兼容的日期函数转换层，让 Test 4 也能通过
- ⏳ `application-local.yml` 中的 `spring.sql.init.encoding: UTF-8` 是 gitignored 本地配置，换机器需重新添加

### 中文乱码根因修复（重要，2026-08-01）
- **现象**：饼图/柱状图中文标签乱码（如华东显示为「鉗廝笢」）
- **根因**：`spring.sql.init` 读取 UTF-8 的 `demo-data.sql` 时使用 JVM 默认编码（Windows 为 GBK），中文变成双重编码（UTF-8 字节按 GBK 解释再编码）
- **修复**：`application-local.yml` 添加 `spring.sql.init.encoding: UTF-8`
- **验证**：华东码点 U+534E U+4E1C（正确）、华北 U+534E U+5317、华南 U+534E U+5357、西部 U+897F U+90E8
- **警告**：不要用 `-Dfile.encoding=UTF-8`（会让 Spring Security 认证失效，所有接口 401）
