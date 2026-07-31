# AI-Agent 数据分析平台

基于 **Claude Code + DeepSeek** 的自然语言数据分析平台。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4 + Java 21 |
| 前端框架 | Vue 3 + TypeScript + Vite |
| 数据库 | MySQL 8.0 |
| AI 模型 | DeepSeek（通过 Spring AI 集成） |
| 开发协作 | Claude Code |

## 项目结构

```
agent数据分析/
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/agent/
│       │   ├── AgentAnalysisApplication.java
│       │   ├── config/          # 配置（数据源、安全、DeepSeek）
│       │   ├── controller/      # REST 控制器
│       │   └── dto/             # 数据传输对象
│       └── test/                # 测试
├── frontend/                    # Vue 3 前端
│   ├── src/
│   │   ├── api/                 # API 客户端
│   │   ├── router/              # 路由配置
│   │   └── views/               # 页面组件
│   └── tests/                   # 前端测试
├── database/
│   └── init/                    # 数据库初始化脚本
├── docker-compose.yml           # MySQL 容器
└── README.md
```

## 快速启动

### 前置条件

- Java 21+
- Node.js 20+
- Docker（用于 MySQL）

### 1. 启动 MySQL

```bash
docker compose up -d
```

### 2. 配置 DeepSeek API Key

```bash
cp backend/src/main/resources/application-local.yml.template \
   backend/src/main/resources/application-local.yml
# 编辑 application-local.yml，填入真实的 DEEPSEEK_API_KEY
```

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

验证：`curl http://localhost:8080/api/health`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开：`http://localhost:5173`

## 运行测试

```bash
# 后端测试
cd backend && ./mvnw test

# 前端测试
cd frontend && npm test
```

## 开发阶段

- [x] **T01** — 项目初始化 + 前后端骨架 + 健康检查（当前）
- [ ] **Phase 1** — 元数据底座（数据集、字段、指标定义）
- [ ] **Phase 2** — MVP 对话分析（意图识别 → SQL → 安全校验 → 执行 → 解释）
- [ ] **Phase 3** — 可视化增强（图表推荐、SSE 进度）
- [ ] **Phase 4** — 计划型 Agent（多步执行、失败重试）
- [ ] **Phase 5** — 产品化（多轮对话、报告、监控）

## 安全设计

- **双数据源**：`app_user`（读写）+ `app_readonly`（SELECT only）
- **SQL 安全管道**：生成 → AST 校验 → 字段白名单 → 参数绑定 → 只读执行
- **密钥隔离**：DeepSeek API Key 通过环境变量/`application-local.yml` 注入，不入库
- **审计追踪**：所有 SQL 执行记录写入 `audit_log` 表
