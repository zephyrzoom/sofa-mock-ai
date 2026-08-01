# Contributing to Mock Agent

## 开发环境要求

- Java 8+
- Maven 3.6+

## 构建

```bash
# 构建所有模块
mvn clean package

# 仅构建 mock-agent
mvn clean package -pl mock-agent

# 仅构建 demo-app
mvn clean package -pl demo-app
```

## 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -pl mock-agent -Dtest=PartialJsonMatchEngineTest

# 运行特定测试方法
mvn test -pl mock-agent -Dtest=PartialJsonMatchEngineTest#shouldMatchPartialRequestBodyFields
```

## 调试

### 本地调试 Agent

1. 构建 mock-agent：`mvn clean package -pl mock-agent`
2. 启动 demo-app 并附加 Agent：

```bash
cd demo-app
java -javaagent:../mock-agent/target/mock-agent-1.0.0.jar \
     -Dmock.cases.dir=../mock-cases \
     -Dmock.agent.debug=true \
     -jar target/demo-app-1.0.0.jar
```

3. 测试接口：

```bash
# Feign 客户端测试
curl http://localhost:8080/test

# RestTemplate 测试
curl http://localhost:8080/test-rest
```

### ByteBuddy 调试

如果遇到类转换问题，启用 ByteBuddy 调试日志：

```bash
java -javaagent:mock-agent.jar \
     -Dmock.agent.bytebuddy.debug=true \
     -jar your-app.jar
```

## 代码规范

- 使用 `MockAgentLogger` 进行日志输出，不要直接使用 `System.out.println`
- 新增 HTTP 客户端支持时，实现 `InterceptorRegistrar` 接口
- 新增匹配策略时，实现 `MatchEngine` 接口
- 新增 Case 加载源时，实现 `CaseLoader` 接口

## 提交规范

- 使用清晰的提交信息描述变更内容
- 每个提交专注于一个逻辑变更
- 提交前确保所有测试通过：`mvn test`

## 问题反馈

请通过 Issue 提交问题反馈，包含以下信息：

- 问题描述
- 复现步骤
- 期望行为
- 实际行为
- 环境信息（Java 版本、操作系统、依赖版本等）
