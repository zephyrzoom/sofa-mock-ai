# Mock Agent

非侵入式 HTTP Mock 工具，基于 Java Instrumentation + ByteBuddy，在运行时拦截 HTTP 客户端调用并返回预定义的 Mock 响应，无需修改业务代码。

## 特性

- **非侵入式**：通过 Java Agent 机制，无需修改业务代码
- **热加载**：支持运行时更新 Mock Case，无需重启应用
- **多客户端支持**：支持 Feign 和 RestTemplate（计划支持 OkHttp、Apache HttpClient、WebClient）
- **灵活匹配**：支持精确路径匹配、正则匹配、Ant 风格路径匹配、请求体部分匹配
- **多版本兼容**：支持 Feign 9.x/10.x/11.x 多版本

## 快速开始

### 1. 构建

```bash
mvn clean package
```

### 2. 准备 Mock Case 文件

在 `mock-cases` 目录下创建 JSON 文件：

```json
{
  "method": "POST",
  "path": "/user/query",
  "requestBody": "{\"userId\":\"123\"}",
  "response": {
    "status": 200,
    "body": "{\"code\":1,\"name\":\"张三\"}"
  }
}
```

### 3. 启动应用

```bash
java -javaagent:mock-agent.jar \
     -Dmock.cases.dir=mock-cases \
     -Dmock.agent.debug=true \
     -jar your-app.jar
```

## 配置参考

| 系统属性 | 默认值 | 说明 |
|---------|--------|------|
| `mock.cases.dir` | `mock-cases` | Mock Case 文件目录 |
| `mock.agent.debug` | `false` | 是否启用调试日志 |
| `mock.agent.bytebuddy.debug` | `false` | 是否启用 ByteBuddy 调试日志 |
| `mock.agent.log.level` | `INFO` | 日志级别（OFF/ERROR/WARN/INFO/DEBUG） |

## Mock Case 格式

```json
{
  "method": "HTTP方法",
  "path": "请求路径",
  "requestBody": "可选，请求体JSON（支持部分字段匹配）",
  "response": {
    "status": 状态码,
    "body": "响应体JSON字符串"
  }
}
```

### 匹配规则

1. **精确匹配**：method + path + requestBody 部分字段匹配（优先级最高）
2. **通配匹配**：method + path，无 requestBody 约束（兜底）

### 匹配示例

```json
{
  "method": "POST",
  "path": "/user/query",
  "requestBody": "{\"userId\":\"123\"}",
  "response": {
    "status": 200,
    "body": "{\"name\":\"张三\"}"
  }
}
```

将匹配以下请求（只要包含 `userId: 123`）：
- `{"userId":"123"}`
- `{"userId":"123","extra":"field"}`
- `{"userId":"123","nested":{"key":"value"}}`

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Your Application                       │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │ Feign Client │    │RestTemplate  │    │   Future...  │  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘  │
│         │                   │                   │           │
│  ┌──────▼───────────────────▼───────────────────▼───────┐  │
│  │              ByteBuddy Interceptors                  │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │              MockCaseManager                          │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │  │
│  │  │CaseLoader│  │ CaseStore│  │   MatchEngine    │   │  │
│  │  └──────────┘  └──────────┘  └──────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Mock Response Builders                   │  │
│  │  ┌──────────────┐    ┌──────────────────────────┐   │  │
│  │  │Feign Response│    │RestTemplate Response     │   │  │
│  │  └──────────────┘    └──────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 项目结构

```
demo-project/
├── mock-agent/                    # 核心模块 - Java Agent
│   └── src/main/java/com/mock/agent/
│       ├── MockAgent.java         # Agent 入口 (premain)
│       ├── MockCaseManager.java   # Case 管理协调器
│       ├── MockCase.java          # Case 数据模型
│       ├── MockCaseLoader.java    # Case 加载门面
│       ├── FeignInterceptor.java  # Feign 拦截器
│       ├── RestTemplateInterceptor.java  # RestTemplate 拦截器
│       ├── DecoderInterceptor.java       # Decoder 调试拦截器
│       ├── MockResponseBuilder.java       # Feign 响应构建
│       ├── MockRestResponseBuilder.java   # RestTemplate 响应构建
│       ├── log/                   # 日志抽象
│       ├── loader/                # Case 加载器
│       ├── store/                 # Case 存储
│       ├── match/                 # 匹配引擎
│       └── registrar/             # 拦截器注册
├── demo-app/                      # 示例应用
└── mock-cases/                    # Mock Case 文件目录
```

## 开发

### 运行测试

```bash
mvn test
```

### 调试

启用调试日志：

```bash
java -javaagent:mock-agent.jar \
     -Dmock.agent.debug=true \
     -Dmock.agent.log.level=DEBUG \
     -jar your-app.jar
```

启用 ByteBuddy 调试：

```bash
java -javaagent:mock-agent.jar \
     -Dmock.agent.bytebuddy.debug=true \
     -jar your-app.jar
```
