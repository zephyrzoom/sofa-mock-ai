# Mock Agent 后续实施方案

> 最后更新：2026-06-15

## 背景

MVP 已完成核心能力：Java Agent + ByteBuddy 拦截 Feign/RestTemplate、JSON mock case 匹配、热加载。现在需要全面提升：扩展拦截能力、增强匹配与管理、工程化完善、平台化管理端。

## 约束

- 保持 Java 8
- Phase 0 先行，Phase 1 优先于 Phase 2
- Phase 3 聚焦管理端（可上传案例文件），不做 Nacos/ZK

---

## Phase 0：结构重构（基础） ✅ 已完成

> 必须先做。后续所有阶段都依赖代码可测试、可扩展。

### Task 0.1：引入日志抽象

- 创建 `MockAgentLogger` 轻量日志门面
- 默认 `System.out`，通过 `mock.agent.log.level` 系统属性控制级别（OFF/ERROR/WARN/INFO/DEBUG）
- 可选 SLF4J 桥接（通过反射检测）
- 修改所有 8 个 `.java` 文件，替换 `System.out.println`

### Task 0.2：提取接口，启用可测试性

关键接口：
- `CaseStore` — 抽象存储，方法：`List<MockCase> findByKey(String method, String path)`
- `CaseLoader` — 抽象加载，方法：`List<LoadedCase> loadAll()`
- `MatchEngine` — 抽象匹配，方法：`MockCase findMatch(String method, String path, String requestBody, List<MockCase> candidates)`
- `ResponseBuilder` — 抽象响应构建，方法：`boolean supports(String clientType)`, `Object build(ClassLoader cl, MockCase mockCase)`

`MockCaseLoader` 重构为 `MockCaseManager`，组合 CaseLoader + CaseStore + MatchEngine。

`MockCase` 新增字段（向后兼容默认值）：
- `Map<String, String> requestHeaders`（可选）
- `Map<String, String> responseHeaders`（可选）
- `long delayMs`（可选，默认 0）

### Task 0.3：MockAgent.premain() 模块化

- 创建 `InterceptorRegistrar` 接口：`void register(AgentBuilder builder, Instrumentation inst)`
- 创建 `FeignInterceptorRegistrar`、`RestTemplateInterceptorRegistrar`
- `premain()` 遍历 registrar 列表，每个用 try-catch 包裹
- 直接为 Phase 2（新 HTTP 客户端）铺路

### Task 0.4：单元测试基础设施

- 添加 JUnit 5 + Mockito + AssertJ
- 测试 `PartialJsonMatchEngine`：精确匹配、部分匹配、嵌套对象、无匹配、catch-all 优先级
- 测试 `JsonFileCaseLoader`：有效文件、无效文件、缺失目录、时间戳变更、文件删除
- 测试 `MockCase` 序列化/反序列化往返
- 目标：MatchEngine 和 CaseLoader 80%+ 行覆盖率

### Task 0.5：README 和文档

- `README.md`：项目描述、快速开始、配置参考、mock case 格式参考、架构图
- `CONTRIBUTING.md`：构建/测试/调试说明
- 公共接口和 `MockAgent` 入口的 Javadoc

---

## Phase 1：增强匹配引擎 + 管理 API ✅ 已完成

### Task 1.1：扩展 MockCase 数据模型

新增字段：
- `String pathPattern` — 支持正则或 Ant 风格模式（如 `/user/{id}`、`/user/.*`）
- `String matchType` — 枚举：`EXACT`（默认）、`REGEX`、`ANT`
- `Map<String, String> requestHeaders` — 请求头匹配条件
- `Map<String, String> responseHeaders` — mock 响应头
- `long delayMs` — 模拟延迟
- `String description` — 人类可读标签
- `boolean enabled` — 不删除即可禁用
- `int priority` — 多 case 匹配时的显式排序

### Task 1.2：实现高级匹配策略

责任链模式：
- `ExactPathMatcher` — 当前行为
- `RegexPathMatcher` — `java.util.regex.Pattern`，编译后缓存
- `AntPathMatcher` — 轻量内置实现（Java 8 无 Spring 也能用）
- `HeaderMatcher` — 检查请求头
- `RequestBodyMatcher` — 当前 `containsAll` 逻辑提升为独立匹配器

优先级评分：
1. 精确路径 + requestBody 匹配 = 最高
2. 精确路径 + catch-all = 次高
3. 正则/Ant 路径 + requestBody 匹配 = 第三
4. 正则/Ant 路径 + catch-all = 第四

### Task 1.3：条件表达式支持（可选，可延后）

- MockCase 新增 `condition` 字段
- 初期仅支持 `==`、`!=`、`contains`、`matches` 操作符
- 基于 headers 和顶层 body 字段

### Task 1.4：Mock Case 管理 REST API

使用 JDK 内置 `com.sun.net.httpserver.HttpServer`（零外部依赖，Java 8 可用）：
- `GET /mock/cases` — 列出所有 case
- `GET /mock/cases/{id}` — 获取单个 case
- `POST /mock/cases` — 新增 case
- `PUT /mock/cases/{id}` — 更新 case
- `DELETE /mock/cases/{id}` — 删除 case
- `POST /mock/cases/reload` — 强制重新加载
- `GET /mock/status` — 健康信息

通过 `mock.agent.api.port` 系统属性启用（默认禁用）。

### Task 1.5：请求统计和实时监控

`MockStatistics` 类跟踪：
- 总拦截请求数、匹配 vs 透传计数
- 每个 case 的匹配次数
- 平均响应时间
- 最近 N 条匹配请求详情

`MatchEvent` 模型（每条请求的匹配记录）：
- 时间戳、HTTP 方法、URL 路径、请求体摘要
- 是否匹配、匹配到的 case ID
- 匹配耗时、响应状态码
- 匹配失败原因（无 case、路径不匹配、body 不匹配等）

暴露方式：
- `GET /mock/stats` — 统计概览
- `GET /mock/events` — 最近 N 条匹配事件列表
- `GET /mock/events/stream` — SSE 实时事件流（Server-Sent Events），管理端前端可实时订阅
- 可选 JMX MBean 暴露统计

### Task 1.6：Phase 1 测试

- 正则/Ant 路径匹配单元测试
- 请求头匹配单元测试
- 优先级评分单元测试
- 管理 API 集成测试
- 向后兼容性测试：现有 `user-query.json` 格式不变

---

## Phase 2：扩展 HTTP 客户端拦截（延后）

> Phase 1 完成后再推进。

### Task 2.1：OkHttp 拦截器

- `OkHttpInterceptorRegistrar` 实现 `InterceptorRegistrar`
- ByteBuddy 目标：`okhttp3.internal.http.RealInterceptorChain.proceed()`
- `OkHttpResponseBuilder` 通过反射构建 `okhttp3.Response`
- 处理 OkHttp 3.x 和 4.x API 差异

### Task 2.2：Apache HttpClient 拦截器

- `ApacheHttpClientInterceptorRegistrar`
- ByteBuddy 目标：`org.apache.http.impl.client.CloseableHttpClient.execute()`
- `ApacheHttpResponseBuilder` 构建 `CloseableHttpResponse`

### Task 2.3：Spring WebClient 拦截器

- ByteBuddy 目标：`ExchangeFunction.exchange()`
- 必须正确处理 `Mono<ClientResponse>` 返回类型
- 最复杂的客户端，可能需要推迟

### Task 2.4：HTTP 客户端自动检测

- `premain()` 自动检测 classpath 上的 HTTP 客户端类
- 只注册存在的客户端拦截器

### Task 2.5：新客户端 Demo App + 测试

---

## Phase 3：管理端（案例文件上传）

> 核心需求：提供一个管理服务，可以上传/管理案例文件，Agent 从管理端拉取案例。

### Task 3.1：管理端后端服务

独立 Spring Boot 模块 `mock-agent-server`：
- **案例文件管理**：
  - `POST /api/cases/upload` — 上传 JSON 案例文件（支持批量）
  - `GET /api/cases` — 列出所有案例（支持按应用名筛选）
  - `GET /api/cases/{id}` — 获取单个案例
  - `PUT /api/cases/{id}` — 更新案例
  - `DELETE /api/cases/{id}` — 删除案例
  - `GET /api/cases/export` — 批量导出为 JSON
- **应用管理**：
  - `GET /api/apps` — 列出注册的应用
  - `GET /api/apps/{appName}/cases` — 获取某应用的所有案例
- **Agent 实例管理**：
  - `GET /api/agents` — 列出在线 Agent 实例
  - Agent 启动时自动注册（上报 appName、instanceId、IP）
- **存储**：文件系统或 H2 数据库（轻量，无需外部 DB）
- **认证**：简单 Token 认证

### Task 3.2：Agent 端远程案例加载

- 创建 `RemoteCaseSource` 实现 `CaseSource` 接口
- 从管理端 `GET /api/apps/{appName}/cases` 拉取案例
- 支持轮询刷新（可配置间隔）或 WebSocket 推送
- 配置：`mock.agent.server.url`、`mock.agent.server.appName`
- 本地文件和远程案例可共存（本地优先）

### Task 3.3：前端管理控制台

轻量 SPA（内嵌在 Spring Boot 中，静态资源）：
- **案例列表页**：搜索、筛选、启用/禁用切换、批量操作
- **案例编辑页**：JSON 编辑器（语法高亮）、表单模式、实时校验
- **案例上传**：拖拽上传 JSON 文件、批量导入
- **实时监控页**：类日志流界面，通过 SSE 订阅 Agent 上报的匹配事件，实时展示每条请求的匹配过程和结果（方法、路径、是否命中、命中 case、失败原因），支持按 Agent/应用/关键词过滤，支持暂停/继续滚动
- **仪表盘**：在线 Agent 数、案例总数、匹配率、最近匹配趋势图
- **Agent 监控**：在线/离线状态、最近心跳时间

技术：Vue.js 3 + Element Plus（或类似 UI 库），打包为静态资源。

### Task 3.4：Agent-Server 通信协议

- Agent 启动时通过 HTTP 注册到管理端（上报 appName、instanceId、IP）
- 心跳上报（可配置间隔）
- 管理端通过轮询推送案例更新
- Agent 实时上报匹配事件（`MatchEvent`）到管理端，管理端存入内存环形缓冲区，前端通过 SSE 订阅消费
- Agent 上报聚合统计数据（定时批量）

### Task 3.5：Phase 3 测试

- 管理端 API 集成测试
- Agent 远程加载案例的端到端测试
- 前端基本功能测试

---

## 依赖关系图

```
Phase 0（基础）──────────────────────────┐
  0.1 日志                                │
  0.2 接口/可测试性                        │
  0.3 模块化注册                           │
  0.4 单元测试                             │
  0.5 README                              │
       │                                  │
       ▼                                  │
Phase 1（匹配增强 + 管理 API）             │
  1.1 扩展 MockCase                       │
  1.2 高级匹配器                           │
  1.3 条件表达式（可选）                    │
  1.4 管理 REST API ──────────────────────┼── 为 Phase 3 提供 API 基础
  1.5 统计                                │
  1.6 测试                                │
       │                                  │
       ▼                                  │
Phase 2（HTTP 客户端扩展，延后）            │
  2.1 OkHttp                              │
  2.2 Apache HttpClient                   │
  2.3 WebClient                           │
  2.4 自动检测                             │
  2.5 Demo + 测试                          │
       │                                  │
       ▼                                  ▼
Phase 3（管理端）
  3.1 后端服务（案例上传/管理）
  3.2 Agent 远程加载
  3.3 前端控制台
  3.4 通信协议
  3.5 测试
```

---

## 工作量估算

| 阶段 | 任务数 | 个人开发 | 小团队 (2-3人) |
|------|--------|---------|---------------|
| Phase 0 | 5 | 2 周 | 1 周 |
| Phase 1 | 6 | 3-4 周 | 2 周 |
| Phase 2 | 5 | 3-4 周 | 2 周 |
| Phase 3 | 5 | 3-4 周 | 2 周 |
| **合计** | **21** | **11-14 周** | **7-8 周** |

---

## 关键文件

- `demo-project/mock-agent/src/main/java/com/mock/agent/MockCaseLoader.java` — 重构核心
- `demo-project/mock-agent/src/main/java/com/mock/agent/MockAgent.java` — 模块化改造
- `demo-project/mock-agent/src/main/java/com/mock/agent/MockCase.java` — 数据模型扩展
- `demo-project/mock-agent/src/main/java/com/mock/agent/MockResponseBuilder.java` — 接口提取
- `demo-project/mock-agent/pom.xml` — 依赖和构建配置
