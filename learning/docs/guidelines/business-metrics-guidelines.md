# 业务指标实现指南

## 目录
- [概述](#概述)
- [核心原则](#核心原则)
- [实现模式](#实现模式)
- [性能考量](#性能考量)
- [版本与向后兼容](#版本与向后兼容)
- [测试要求](#测试要求)
- [示例与参考](#示例与参考)

## 概述

业务指标是添加到 User-Agent 请求头中的短标识符，用于遥测追踪。它们帮助 AWS 了解各功能在 SDK 中的使用模式。本文基于团队架构决策与性能考量，说明如何在 AWS SDK for Java v2 中实现业务指标。

**关键概念：**
- **业务指标**：表示功能使用的短字符串标识（例如 `"S"`、`"A"`、`"B"`）
- **User-Agent 请求头**：承载业务指标以供遥测的 HTTP 头

## 核心原则

### 以功能为中心放置

**必须**在能够最终确定/确认功能确实被使用时添加业务指标。若功能可被覆盖，应在功能使用已确认并定型的位置记录指标。

**理由：** 经团队讨论，相较在 `ApplyUserAgentStage` 中集中放置，本方式更合适，因为：
- **职责更清晰**：`ApplyUserAgentStage` 无需了解内部功能实现细节
- **更易维护**：重构功能时不必改多处
- **耦合更低**：避免流水线阶段与功能实现紧耦合


## 实现模式

以 GZIP 压缩为例，请求在 `CompressRequestStage` 中被压缩，因此在该处添加业务指标。以校验和为例，校验和在 `HttpChecksumStage` 中确定，因此在该处添加业务指标。

```java
// CompressRequestStage 示例
private void updateContentEncodingHeader(SdkHttpFullRequest.Builder input,
                                         Compressor compressor,
                                         ExecutionAttributes executionAttributes) {
    // 在确实应用压缩时记录业务指标
    executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS)
                       .addMetric(BusinessMetricFeatureId.GZIP_REQUEST_COMPRESSION.value());
    
    if (input.firstMatchingHeader(COMPRESSION_HEADER).isPresent()) {
        input.appendHeader(COMPRESSION_HEADER, compressor.compressorType());
    } else {
        input.putHeader(COMPRESSION_HEADER, compressor.compressorType());
    }
}

// HttpChecksumStage 示例 — 展示业务指标记录位置
@Override
public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, 
                                          RequestExecutionContext context) throws Exception {
    // ... 功能解析逻辑 ...
    
    SdkHttpFullRequest.Builder result = processChecksum(request, context);
    
    // 在功能定型后记录业务指标
    recordChecksumBusinessMetrics(context.executionAttributes());
    
    return result;
}
```

## 性能考量

### 避免为业务指标而变更请求

**不应**通过变更请求（`.toBuilder().build()`）来添加业务指标，否则会不必要地复制对象并带来性能开销。

**避免以下模式**（曾用于 waiter/paginator 实现）：
```java
// 会创建新对象（性能开销）
Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = 
    b -> b.addApiName(ApiName.builder().name("sdk-metrics").version("B").build());

AwsRequestOverrideConfiguration overrideConfiguration =
    request.overrideConfiguration().map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
    .orElse(AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build());

return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
```

 **优先使用 ExecutionAttributes 模式**：
```java
// 直接收集业务指标（不创建额外对象）
private void recordFeatureBusinessMetric(ExecutionAttributes executionAttributes) {
    BusinessMetricCollection businessMetrics = 
        executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
    
    if (businessMetrics != null) {
        businessMetrics.addMetric(BusinessMetricFeatureId.FEATURE_ID.value());
    }
}
```

对于在构建 ExecutionContext 之前就已确定的高层功能（例如 Transfer Manager、Batch Manager、跨区域操作），且无法访问 `ExecutionAttributes` 时，若可从客户端配置或执行参数检测到该功能（例如客户端配置中的重试模式，或执行参数中的 RPC v2 CBOR 协议），优先使用 `AwsExecutionContextBuilder.resolveUserAgentBusinessMetrics()`。若无其他选择，则允许使用请求变更。

## 版本与向后兼容

### 业务指标变更通常向后兼容

一般而言，对现有业务指标的修改可视为向后兼容，因为业务指标不影响 SDK 功能或客户代码行为，客户应用不受影响。业务指标纯属观测性遥测，因此修改现有指标通常是安全的。若变更现有业务指标，请与团队讨论，并在需要时做次版本号提升，以便各团队从该版本起识别新指标。


## 测试要求

### 使用 Mock HTTP 客户端做功能测试

**必须**使用 Mock HTTP 客户端进行功能测试，而不是基于拦截器的测试。

**为何使用 Mock HTTP 客户端：**
- **可靠**：测试不受拦截器顺序变更或 SDK 内部修改影响
- **端到端验证**：从功能使用到 User-Agent 包含指标的完整链路可验证
- **简单**：无需配置拦截器即可拿到最终 HTTP 请求
- **易维护**：内部流水线阶段重构时测试仍稳定

**测试模式：**
1. 创建 Mock HTTP 客户端并桩响应
2. 使用该 Mock 构建 SDK 客户端
3. 执行应触发业务指标的操作
4. 从捕获的请求中提取 User-Agent 头
5. 用模式匹配验证业务指标存在

```java
@Test
void testBusinessMetric_withMockHttpClient() {
    MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
    mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                   .response(SdkHttpResponse.builder()
                                            .statusCode(200)
                                            .build())
                                   .build());
    
    // 使用 mock HTTP 客户端创建客户端并发起请求
    S3Client client = S3Client.builder()
                              .httpClient(mockHttpClient)
                              .build();
    
    client.listBuckets();
    
    // 从最后一次请求提取 User-Agent
    SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
    String userAgent = lastRequest.firstMatchingHeader("User-Agent").orElse("");
    
    // 验证业务指标存在
    assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply("A"));
}
```

**异步客户端：**
对异步操作使用 `MockAsyncHttpClient`，模式相同。

### 参考测试文件
- `test/auth-tests/src/it/java/software/amazon/awssdk/auth/source/UserAgentProviderTest.java`
- `test/codegen-generated-classes-test/src/test/java/software/amazon/awssdk/services/rpcv2cbor/RpcV2CborUserAgentTest.java`


## 示例与参考

部分示例实现如下：

### 关键文件与类
- **BusinessMetricFeatureId**：`core/sdk-core/src/main/java/software/amazon/awssdk/core/useragent/BusinessMetricFeatureId.java`
- **BusinessMetricsUtils**：`core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/useragent/BusinessMetricsUtils.java`
- **ApplyUserAgentStage**：`core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/pipeline/stages/ApplyUserAgentStage.java`
- **HttpChecksumStage**：`core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/pipeline/stages/HttpChecksumStage.java`
- **CompressRequestStage**：`core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/pipeline/stages/CompressRequestStage.java`
- **AuthSchemeInterceptorSpec**：`codegen/src/main/java/software/amazon/awssdk/codegen/poet/auth/scheme/AuthSchemeInterceptorSpec.java`
