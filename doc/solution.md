# Mock Agent 方案文档

## 1. 方案概述

本方案实现了一个 **Java Mock Agent**，通过 Java Instrumentation + ByteBuddy 字节码增强技术，在运行时拦截 HTTP 客户端调用（Feign / RestTemplate / Apache HttpClient / OkHttp），无需修改业务代码即可返回预定义的 Mock 响应。

**核心价值：** 无侵入、热加载、多 HTTP 客户端支持。

## 2. 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 字节码增强 | ByteBuddy | 1.12.23 |
| HTTP 客户端拦截 | Spring Cloud OpenFeign | - |
| Web 框架 | Spring Boot | 2.6.13 |
| JSON 解析 | Jackson | 2.13.5 |
| 构建工具 | Maven | - |
| JDK | Java 8+ | - |

## 3. 项目结构

```
demo-project/
├── pom.xml                          # 父 POM（多模块）
├── mock-agent/                      # Mock Agent 模块（核心）
│   ├── pom.xml
│   └── src/main/java/com/mock/agent/
│       ├── MockAgent.java           # Agent 入口（premain）
│       ├── FeignInterceptor.java    # Feign 客户端拦截器
│       ├── RestTemplateInterceptor.java  # RestTemplate 拦截器
│       ├── HttpClientInterceptor.java    # Apache HttpClient 拦截器
│       ├── OkHttpClientInterceptor.java  # OkHttp 拦截器
│       ├── DecoderInterceptor.java  # Feign Decoder 调试拦截器
│       ├── MockCaseLoader.java      # Mock 用例加载 & 匹配引擎
│       ├── MockCase.java            # Mock 用例数据模型
│       ├── MockResponseBuilder.java # Feign 响应构建器
│       ├── MockRestResponseBuilder.java  # RestTemplate 响应构建器
│       ├── MockHttpClientResponseBuilder.java  # Apache HttpClient 响应构建器
│       ├── MockOkHttpResponseBuilder.java      # OkHttp 响应构建器
│       └── registrar/
│           ├── InterceptorRegistrar.java
│           ├── FeignInterceptorRegistrar.java
│           ├── RestTemplateInterceptorRegistrar.java
│           ├── HttpClientInterceptorRegistrar.java
│           └── OkHttpClientInterceptorRegistrar.java
├── demo-app/                        # 演示应用
│   ├── pom.xml
│   └── src/main/java/com/demo/
│       ├── DemoApplication.java     # Spring Boot 启动类
│       ├── UserController.java      # 测试 Controller
│       └── UserClient.java          # Feign 客户端接口
└── mock-cases/                      # Mock 用例目录
    └── user-query.json
```

## 4. 核心流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        JVM 启动                                 │
│  java -javaagent:mock-agent.jar -jar demo-app.jar              │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   MockAgent.premain()                           │
│  通过 ByteBuddy 注册字节码转换规则：                               │
│  1. feign.Client.execute()           → FeignInterceptor         │
│  2. SynchronousMethodHandler          → DecoderInterceptor      │
│  3. feign.Decoder.decode()           → DecoderInterceptor       │
│  4. ClientHttpRequest.execute()      → RestTemplateInterceptor  │
│  5. HttpClient.execute()             → HttpClientInterceptor    │
│  6. OkHttpClient.newCall()           → OkHttpClientInterceptor  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   运行时拦截                                     │
│                                                                 │
│  业务代码发起 HTTP 请求                                          │
│       │                                                         │
│       ▼                                                         │
│  拦截器提取: method / path / requestBody                         │
│       │                                                         │
│       ▼                                                         │
│  MockCaseLoader.findMatch() 匹配 Mock 用例                      │
│       │                                                         │
│       ├── 匹配成功 → 构建 Mock 响应并返回                        │
│       │                                                         │
│       └── 未匹配   → 调用原始方法（callable.call()）             │
└─────────────────────────────────────────────────────────────────┘
```

## 5. 核心模块详解

### 5.1 MockAgent — Agent 入口

**文件：** `MockAgent.java`

作为 `Premain-Class`，在 JVM 启动时执行。通过 ByteBuddy 的 `AgentBuilder` API 注册六组字节码转换规则：

```java
// 1. 拦截所有 feign.Client 实现类的 execute() 方法
.type(ElementMatchers.nameStartsWith("feign.Client").and(not(isInterface())))
.transform(...) → MethodDelegation.to(FeignInterceptor.class)

// 2. 拦截 SynchronousMethodHandler.executeAndDecode()
.type(ElementMatchers.nameContains("SynchronousMethodHandler"))
.transform(...) → MethodDelegation.to(DecoderInterceptor.class)

// 3. 拦截所有 feign.Decoder 实现类的 decode() 方法
.type(hasSuperType(named("feign.Decoder")).and(not(isInterface())))
.transform(...) → MethodDelegation.to(DecoderInterceptor.class)

// 4. 拦截所有 ClientHttpRequest 实现类的 execute() 方法
.type(hasSuperType(named("org.springframework.http.client.ClientHttpRequest")))
.transform(...) → MethodDelegation.to(RestTemplateInterceptor.class)

// 5. 拦截所有 Apache HttpClient 实现类的 execute() 方法
.type(hasSuperType(named("org.apache.http.client.HttpClient")).and(not(isInterface())))
.transform(...) → MethodDelegation.to(HttpClientInterceptor.class)

// 6. 拦截所有 OkHttp OkHttpClient 实现类的 newCall() 方法
.type(hasSuperType(named("okhttp3.OkHttpClient")).and(not(isInterface())))
.transform(...) → MethodDelegation.to(OkHttpClientInterceptor.class)
```

**系统属性：**

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `mock.agent.debug` | 启用调试日志 | `false` |
| `mock.agent.bytebuddy.debug` | 启用 ByteBuddy 字节码调试输出 | `false` |

### 5.2 FeignInterceptor — Feign 拦截器

**文件：** `FeignInterceptor.java`

通过 ByteBuddy 的 `@SuperCall` + `@AllArguments` 注解拦截 `feign.Client.execute()` 方法。

**处理逻辑：**

1. 从 `Request` 对象中通过反射提取 `url`、`httpMethod`、`body`
2. 解析 URL 获取 `path`
3. 调用 `MockCaseLoader.findMatch()` 查找匹配的 Mock 用例
4. 匹配成功 → 调用 `MockResponseBuilder.build()` 构建响应
5. 未匹配 → 调用 `callable.call()` 执行原始请求

**反射兼容性：** 兼容 Feign 9.x（`method()` 方法）和 10.x+（`httpMethod()` 方法）两种 API。

### 5.3 RestTemplateInterceptor — RestTemplate 拦截器

**文件：** `RestTemplateInterceptor.java`

通过 `@This` + `@SuperCall` 注解拦截 `ClientHttpRequest.execute()` 方法。

**处理逻辑：**

1. 从请求对象中通过反射提取 `getURI()`、`getMethodValue()`、`getBody()`
2. 调用 `MockCaseLoader.findMatch()` 查找匹配
3. 匹配成功 → 调用 `MockRestResponseBuilder.build()` 构建 `ClientHttpResponse` 代理对象
4. 未匹配 → 调用原始方法

### 5.4 MockCaseLoader — 用例加载 & 匹配引擎

**文件：`MockCaseLoader.java`**

**加载机制：**

- 从 `mock.cases.dir` 系统属性指定的目录读取 `.json` 文件
- 使用 `ConcurrentHashMap` 存储用例，key 格式为 `METHOD:path`
- 通过文件时间戳检测变更，支持热加载
- 文件被删除时自动清理对应用例

**匹配优先级：**

```
1. Method + Path 精确匹配
2. RequestBody 字段部分匹配（JSON 对象递归匹配）
3. 兜底匹配（无 requestBody 约束的用例）
```

**部分匹配算法：**

`containsAll(actual, expected)` 递归检查 expected 中的所有字段是否在 actual 中存在且值相等，支持嵌套对象。这意味着 Mock 用例只需定义关心的字段，不要求请求体完全一致。

### 5.5 MockResponseBuilder — Feign 响应构建

**文件：`MockResponseBuilder.java`**

通过反射构建 `feign.Response` 对象，兼容多个 Feign 版本：

**Body 设置策略（按优先级）：**

1. `body(byte[])` — OpenFeign 10.x+ / 11.x+
2. `body(InputStream, Integer)` — 部分 Feign fork
3. `body(InputStream, int)` — 原始类型变体
4. 直接设置 `body` 字段（`Response$InputStreamBody`）— 反射兜底
5. `body(Object)` — 通用兜底

**Request 创建策略：**

1. `Request.create(HttpMethod, String, Map, byte[], Charset, RequestTemplate)` — 10.x+
2. `Request.create(String, String, Map, byte[], Charset)` — 9.x
3. `Request.create(String, String, Map, byte[], Charset, RequestTemplate)` — 9.x 变体

### 5.6 MockRestResponseBuilder — RestTemplate 响应构建

**文件：`MockRestResponseBuilder.java`**

使用 Java 动态代理（`Proxy.newProxyInstance`）创建 `ClientHttpResponse` 代理对象，实现以下方法：

- `getStatusCode()` → 返回 `HttpStatus`
- `getRawStatusCode()` → 返回状态码 int
- `getStatusText()` → 返回原因短语
- `getBody()` → 返回 `ByteArrayInputStream`
- `getHeaders()` → 返回 `HttpHeaders`
- `close()` → 空操作

## 6. Mock 用例定义

### 文件格式

每个 `.json` 文件定义一个 Mock 用例，放在 `mock-cases/` 目录下：

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

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `method` | String | 是 | HTTP 方法（GET / POST / PUT / DELETE 等） |
| `path` | String | 是 | 请求路径 |
| `requestBody` | String | 否 | 请求体匹配条件（JSON 部分匹配） |
| `response.status` | int | 是 | 响应状态码 |
| `response.body` | String | 是 | 响应体（JSON 字符串） |

### 匹配规则

- **有 `requestBody`：** 请求体必须包含 `requestBody` 中定义的所有字段（部分匹配）
- **无 `requestBody`：** 作为兜底用例，匹配同 method+path 的所有请求
- 同一 method+path 可定义多个用例，精确匹配优先于兜底匹配

## 7. 使用方式

### 构建

```bash
cd demo-project
mvn clean package
```

### 运行

```bash
java -javaagent:mock-agent/target/mock-agent-1.0.0.jar \
     -Dmock.cases.dir=mock-cases \
     -Dmock.agent.debug=true \
     -jar demo-app/target/demo-app-1.0.0.jar
```

### 测试

```bash
# Feign 客户端调用（走 /test 接口）
curl http://localhost:8080/test

# RestTemplate 调用（走 /test-rest 接口）
curl http://localhost:8080/test-rest
```

### 热加载

直接修改 `mock-cases/` 目录下的 `.json` 文件，无需重启应用。Agent 会在下次请求时自动检测文件变更并重新加载。

## 8. 架构设计要点

### 8.1 无侵入性

- 通过 `-javaagent` 挂载，业务代码零修改
- 未匹配的请求自动透传到真实服务
- 可随时移除 Agent 恢复原始行为

### 8.2 兼容性设计

- **Feign 多版本兼容：** 通过反射 + 多策略降级支持 Feign 9.x / 10.x / 11.x
- **RestTemplate 兼容：** 使用动态代理而非硬编码实现类，兼容不同 Spring 版本
- **ClassLoader 隔离：** 使用目标类的 ClassLoader 加载依赖类，避免类加载冲突

### 8.3 热加载机制

- 基于文件时间戳检测变更
- 文件删除时自动清理对应用例
- 使用 `ConcurrentHashMap` 保证线程安全

### 8.4 匹配引擎

- JSON 字段级部分匹配，降低用例编写门槛
- 支持嵌套对象递归匹配
- 两级优先级：精确匹配 > 兜底匹配
