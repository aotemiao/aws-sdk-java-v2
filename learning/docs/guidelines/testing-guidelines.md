# AWS SDK v2 测试指南

## 通用指南

- 新测试类**优先**使用 **JUnit 5**
- 断言**优先**使用 **AssertJ**，而非 Hamcrest
- 每个测试场景使用独立测试方法
- 多个相似用例**优先**使用参数化测试
- 测试带依赖的模块时，**优先**通过构造函数注入依赖，而不是仅为测试暴露 getter/setter
- 仅用于测试的方法/构造函数**不得**为 public
- 测试覆盖率虽非硬性要求，**应**力求达到约 80% 代码覆盖

## 测试命名约定

测试名称**应**遵循 `methodToTest_when_expectedBehavior` 模式：

- 示例：`close_withCustomExecutor_shouldNotCloseCustomExecutor`
- 示例：`uploadDirectory_withDelimiter_filesSentCorrectly`

该约定能明确表达：
1. 测的是哪个方法
2. 在什么条件下测试
3. 期望的行为是什么

## 单元测试最佳实践

- 每个测试聚焦单一行为或方面
- 使用能说明测试目的的描述性名称
- Arrange-Act-Assert（AAA）模式：
  - Arrange：准备数据与前置条件
  - Act：执行被测操作
  - Assert：校验期望结果
- 针对具体条件选择合适的断言
- 避免测试相互依赖 — 测试应能以任意顺序运行
- 在 @After 或 @AfterEach 中清理资源
- 测试错误路径时，校验异常类型及部分错误信息

## Mock 指南

- Mock 外部依赖，不要 Mock 被测类本身
- 只 Mock 测试中需要控制的部分
- **优先**使用构造函数注入，使依赖可被 Mock
- 使用 Mockito 的 verify() 确认与 Mock 的交互符合预期
- 避免过度打桩 — 易使测试脆弱、难以维护
- 功能测试中常用 [Wiremock](https://wiremock.org/) 模拟服务端响应

## 异步代码测试

- 使用合适机制等待异步操作完成
- 设置合理超时，避免测试挂起
- 对 CompletableFuture：
  - 测试中使用 `join()` 或带超时的 `get()` 等待完成
  - 同时测试成功完成与异常完成路径
- 对 Reactive Streams：
  - 测试背压处理
  - 测试取消场景
  - 测试错误传播
  - 对 Publisher/Subscriber 实现，使用 TCK 测试（见下文 [Reactive Streams TCK 测试](#reactive-streams-tck-测试)）

## 测试套件

### 单元测试

单元测试验证单个源码单元的行为。

- **目标**：验证特定组件行为并捕获回归
- **位置**：各模块的 `src/test` 目录
- **何时添加**：任何新改动**应**补充新单元测试
- **命名约定**：ClassNameTest、MethodNameTest
- **自动化**：每个 PR 与发布前运行
- **示例**：[S3TransferManagerTest](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/s3-transfer-manager/src/test/java/software/amazon/awssdk/transfer/s3/internal/S3TransferManagerTest.java)

### 功能测试

功能测试验证 SDK 功能的端到端行为。

- **目标**：验证 SDK 功能的端到端行为
- **位置**：
  - 生成的 SDK 通用功能：在 [codegen-generated-classes-test](https://github.com/aws/aws-sdk-java-v2/tree/master/test/codegen-generated-classes-test) 与 [protocol-test](https://github.com/aws/aws-sdk-java-v2/tree/master/test/protocol-tests) 模块
  - 服务专属功能：该服务模块的 `src/test`
  - 高层库（HLL）：该 HLL 模块的 `src/test`
- **何时添加**：新 SDK 功能或关键缺陷修复**应**补充功能测试
- **命名约定**：BehaviorTest、OperationTest
- **自动化**：每个 PR 与发布前运行
- **示例**：[WaitersSyncFunctionalTest](https://github.com/aws/aws-sdk-java-v2/blob/2532ec4f8ab36bab545689a2406d6a61d1696650/test/codegen-generated-classes-test/src/test/java/software/amazon/awssdk/services/waiters/WaitersSyncFunctionalTest.java)、[SelectObjectContentTest](https://github.com/aws/aws-sdk-java-v2/blob/master/services/s3/src/test/java/software/amazon/awssdk/services/s3/functionaltests/SelectObjectContentTest.java)

### Reactive Streams TCK 测试

[Reactive Streams TCK 测试](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) 依据 [Reactive Streams 规范](https://github.com/reactive-streams/reactive-streams-jvm) 校验实现是否合规。

- **目标**：确保实现符合规范
- **位置**：各模块 `src/test`，测试类名始终以 `TckTest` 结尾
- **何时添加**：新增 Subscriber/Publisher 实现时**必须**新增对应 TCK 测试
- **命名约定**：ClassNameTckTest
- **自动化**：每个 PR 与发布前运行
- **示例**：[FileAsyncRequestPublisherTckTest](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/test/java/software/amazon/awssdk/core/async/FileAsyncRequestPublisherTckTest.java)

### 集成测试

集成测试在真实 AWS 服务上验证 SDK 的端到端行为。

- **目标**：在真实服务上验证功能的端到端行为
- **位置**：各模块 `src/it`，测试类名始终以 `IntegrationTest` 结尾
- **何时添加**：新功能**可以**添加集成测试（更推荐功能测试）
- **命名约定**：ClassNameIntegrationTest、OperationIntegrationTest、BehaviorIntegrationTest
- **重要说明**：每次测试后**必须**清理所创建资源
- **自动化**：发布前运行
- **示例**：[S3TransferManagerDownloadDirectoryIntegrationTest](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/s3-transfer-manager/src/it/java/software/amazon/awssdk/transfer/s3/S3TransferManagerDownloadDirectoryIntegrationTest.java)

### 稳定性测试

[稳定性回归测试](https://github.com/aws/aws-sdk-java-v2/tree/master/test/stability-tests) 通过高并发请求检测稳定性回归。

- **目标**：在高并发环境下发现 SDK 错误
- **位置**：`stability-tests` 模块的 `src/it`，测试类名以 `StabilityTest` 结尾
- **何时添加**：开发关键功能（如 HTTP 客户端）时**应**添加稳定性测试
- **命名约定**：ClassNameStabilityTest
- **自动化**：发布前运行
- **示例**：[KinesisStabilityTest](https://github.com/aws/aws-sdk-java-v2/blob/master/test/stability-tests/src/it/java/software/amazon/awssdk/stability/tests/kinesis/KinesisStabilityTest.java)

### 长期运行金丝雀

长期运行的金丝雀以恒定速率向真实或模拟服务发请求，收集资源使用、错误率与延迟指标。

- **目标**：发现资源泄漏、延迟上升与性能问题
- **位置**：独立内部仓库
- **何时添加**：开发关键功能（如 HTTP 客户端）时**应**添加金丝雀
- **命名约定**：FeatureTxnCreator
- **自动化**：持续运行，每周部署

### 性能测试

性能测试基于 [Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh) 构建，测量吞吐量与延迟。

- **目标**：发现性能回归
- **位置**：[sdk-benchmarks](https://github.com/aws/aws-sdk-java-v2/tree/master/test/sdk-benchmarks) 模块
- **何时添加**：关键功能或需验证 SDK 性能影响时**应**添加基准代码
- **命名约定**：FeatureBenchmark
- **自动化**：无自动化，手动触发
- **示例**：[ApacheHttpClientBenchmark](https://github.com/aws/aws-sdk-java-v2/blob/master/test/sdk-benchmarks/src/main/java/software/amazon/awssdk/benchmark/apicall/httpclient/sync/ApacheHttpClientBenchmark.java)

### 协议测试

协议测试针对不同[协议](https://smithy.io/2.0/aws/protocols/index.html)（含 rest-json、rest-xml、json、xml、query 等）验证 SDK 行为。

- **目标**：验证不同协议下的编组/解组
- **位置**：[protocol-tests](https://github.com/aws/aws-sdk-java-v2/tree/master/test/protocol-tests) 与 [protocol-tests-core](https://github.com/aws/aws-sdk-java-v2/tree/master/test/protocol-tests-core) 模块
- **何时添加**：新增结构支持时**必须**添加协议测试
- **命名约定**：XmlProtocolTest
- **自动化**：每个 PR 与发布前运行
- **示例**：[RestJsonProtocolTest](https://github.com/aws/aws-sdk-java-v2/blob/master/test/protocol-tests/src/test/java/software/amazon/awssdk/protocol/tests/RestJsonProtocolTest.java)

## 测试覆盖

- 力求高覆盖率，尤其是关键路径
- 不要只看行覆盖 — 考虑分支与路径覆盖
- 识别并测试边界与极端情况
- 测试错误处理与异常路径
- 覆盖率虽非硬性要求，**应**力求约 80% 代码覆盖

## 参考资料

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ 文档](https://assertj.github.io/doc/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Wiremock 文档](https://wiremock.org/)
- [AWS SDK for Java 开发人员指南 - 测试](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/testing.html)
- [Reactive Streams 规范](https://github.com/reactive-streams/reactive-streams-jvm)
- [Reactive Streams TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master)
