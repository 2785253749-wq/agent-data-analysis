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
| **技术栈** | Spring Boot 3.4 + Java 17 + Vue 3 + TypeScript + MySQL + DeepSeek |
| **开发协作** | Claude Code 作为开发协作器（读代码、写代码、运行测试）；DeepSeek 作为 Agent 推理模型 |

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
│       │   ├── config/         # SecurityConfig, DeepSeekConfig, DataSourceConfig
│       │   ├── controller/     # REST 控制器
│       │   ├── dto/            # Java record DTOs
│       │   ├── entity/         # JPA 实体
│       │   ├── repository/     # Spring Data Repositories
│       │   ├── service/        # 业务逻辑
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
| **M3** | SQL 安全校验 | ⬜ 待开始 | AST 校验 + 字段白名单 + 权限 |
| **M4** | 只读查询执行 | ⬜ 待开始 | 参数绑定 + 超时控制 + 审计 |
| **M5** | 数据解释 | ⬜ 待开始 | 自然语言结论生成 |
| **M6** | 图表推荐 | ⬜ 待开始 | ECharts 渲染 |
| **M7** | 分析编排器 | ⬜ 待开始 | SSE 进度推送 |
| **M8** | 前端对话界面 | ⬜ 待开始 | 对话式分析 UI |

---

## 四、环境速查

```bash
# Java
export JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.13.11-hotspot"
export MAVEN_HOME="/tmp/maven_inst/apache-maven-3.9.9"
export PATH="$MAVEN_HOME/bin:$PATH"

# 运行后端测试
cd backend && mvn test -Dspring.profiles.active=test --no-transfer-progress

# 运行前端测试
cd frontend && npx vitest run

# 前端类型检查
cd frontend && npx vue-tsc --noEmit

# Git 操作
cd "d:/Users/Asus/Desktop/agent数据分析"
```

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

## 十、待开始：M3 SQL 安全校验

### 依赖 T02 的产出
- 数据集、字段、指标元数据已可通过 `/api/datasets/{id}/context` 获取
- 管理后台可配置元数据

### M1 需要做的事
1. 设计意图识别的 JSON Schema（intentType, metrics, dimensions, filters, timeRange, comparison, needsClarification）
2. 编写 DeepSeek Prompt（system prompt + 元数据上下文注入）
3. 创建 `IntentDTO`（Java record, 对应 JSON Schema）
4. 创建 `IntentRecognitionService`（调用 DeepSeek, 解析 JSON 响应）
5. 创建 `IntentRecognitionServiceTest`（正常识别、模糊输入→needsClarification、超时降级、JSON 解析失败）
6. 创建测试用 Prompt 文件 `prompts/intent-recognition/system.txt` 和 `v1.json`

### 关键风险
- DeepSeek 返回非 JSON → 重试 + 降级为可读错误
- 模糊时间范围（如"最近"）→ needsClarification=true，不猜测
- 温度设置：0.1（SQL 生成用 0）

---

## 十一、安全备忘

- [x] DeepSeek API Key 通过 `application-local.yml`（gitignored）注入
- [x] 数据库双账号设计（`app_user` + `app_readonly`）
- [x] 管理端点需要 HTTP Basic 认证
- [x] 表名有正则校验（`^[a-zA-Z_][a-zA-Z0-9_]*$`）
- [x] 全局异常处理器防止错误信息泄露
- [ ] SQL 注入防护（M3 实现）
- [ ] 字段白名单校验（M3 实现）
- [ ] 只读数据源执行 SQL（M4 实现）
- [ ] JWT 认证替换 HTTP Basic（待排期）

---

## 十二、最后更新

**2026-07-31**：完成 T01 + T02 + M1 + M2。测试 45/45 全部通过。下一步 M3 SQL 安全校验。
