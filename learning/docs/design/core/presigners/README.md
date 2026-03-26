**设计：** 新功能，**状态：** [已发布](../../../README.md)

# 请求预签名（Request Presigners）

「Presigned URLs」通常指使用 [SigV4 查询参数签名](https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-query-string-auth.html) 签名过的 AWS 请求，可在一定期限内由浏览器等发起。

1.x SDK 家族可生成多种类型的预签名请求，其中 S3 GetObject 是最常见的预签名 URL 形态。客户对 2.x 尚未包含该能力反响强烈（[示例 1](https://github.com/aws/aws-sdk-java-v2/issues/203)、[示例 2](https://dzone.com/articles/s3-and-the-aws-java-sdk-20-look-before-you-leap)）。本文说明 Java SDK 2.x 应如何支持预签名 URL。

**什么是请求预签名？**

请求预签名允许**签名创建者**使用其保密**签名凭证**生成 AWS 请求。该**预签名请求**可由另一**签名使用者**在固定时间窗口内执行，无需额外认证。

例如，后端支持工程师可：(1) 将日志上传到 S3；(2) 对日志对象预签名 S3 GetObject 请求；(3) 将预签名请求发给客户。客户即可用该请求下载日志。预签名请求在支持工程师签名时指定的过期时间前一直有效。

**什么是预签名 URL？**

口语中并非所有预签名请求都叫「预签名 URL」。例如预签名的 DynamoDB PutItem 可由签名使用者执行，但签名创建者需一并分享签名时包含的 header 与负载，签名使用者才能在请求中携带。

本文约定：
1. **预签名请求**：使用查询参数签名、且目的为供他方稍后执行的任意请求。
2. **预签名 URL**：同时满足：(1) 无请求体；(2) 无 content-type 或 x-amz-* 头；(3) HTTP 方法为 GET 的预签名请求。

该区分有意义，因为预签名 URL 可被浏览器直接打开执行。

*示例*

在此定义下，预签名的 S3 GetObjectRequest 当且仅当**未**包含以下字段之一时才是预签名 URL：

1. sseCustomerAlgorithm（头：x-amz-server-side-encryption-customer-algorithm）
2. sseCustomerKey（头：x-amz-server-side-encryption-customer-key）
3. sseCustomerKeyMD5（头：x-amz-server-side-encryption-customer-key-MD5）
4. requesterPays（头：x-amz-request-payer）

若生成预签名请求时包含上述字段，却在浏览器中打开 URL，签名使用者会得到「签名不匹配」错误，因为这些头参与签名，而浏览器不会发送这些值。



## 拟议 API

SDK 2.x 将同时支持预签名请求与预签名 URL；API 将易于区分二者，并可在实现 AWS SDK HTTP 客户端阻塞或非阻塞 SPI 的 HTTP 客户端上执行预签名请求。

*下文 FAQ：「预签名请求的执行怎么办？」*

为尽快满足对预签名 URL 的强烈需求，首版仅支持生成预签名 S3 GetObject 请求；后续里程碑将扩展其他操作与服务，并支持代码生成的 presigner。

*下文章节：「里程碑」*

### 使用示例

#### 示例 1：生成预签名请求

```Java
S3Presigner s3Presigner = S3Presigner.create();
PresignedGetObjectRequest presignedRequest =
        s3Presigner.presignGetObject(r -> r.getObject(get -> get.bucket("bucket").key("key"))
                                           .signatureDuration(Duration.ofMinutes(15)));
URL URL = presignedRequest.url();
```

#### 示例 2：判断预签名请求是否可在浏览器中使用

```Java
S3Presigner s3Presigner = S3Presigner.create();
PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(...);

Validate.isTrue(presignedRequest.isBrowserCompatible());

System.out.println("Click the following link to download the object: " + presignedRequest.url());
```

#### 示例 3：使用 URL connection HTTP 客户端执行预签名请求。

```Java
S3Presigner s3Presigner = S3Presigner.create();
PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(...);

try (SdkHttpClient httpClient = UrlConnectionHttpClient.create()) {
    ContentStreamProvider payload = presignedRequest.payload()
                                                    .map(SdkBytes::asInputStream)
                                                    .map(is -> () -> is)
                                                    .orElseNull();

    HttpExecuteRequest executeRequest =
            HttpExecuteRequest.builder()
                              .request(presignedRequest.httpRequest())
                              .contentStreamProvider(payload)
                              .build();

    HttpExecuteResponse httpRequest = client.prepareRequest(executeRequest).call();

    Validate.isTrue(httpRequest.httpResponse().isSuccessful());
}
```

### `{Service}Presigner`

每个服务将新增类 `{Service}Presigner`（例如 `S3Presigner`）。命名与现有 `{Service}Client`、`{Service}Utilities` 一致。

#### 示例

```Java
/**
 * Allows generating presigned URLs for supported S3 operations.
 */
public interface S3Presigner {
    static S3Presigner create();
    static S3Presigner.Builder builder();

    /**
     * Presign a `GetObjectRequest` so that it can be invoked directly via an HTTP client.
     */
    PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest request);
    PresignedGetObjectRequest presignGetObject(Consumer<GetObjectPresignRequest.Builder> request);

    interface Builder {
        Builder region(Region region);
        Builder credentialsProvider(AwsCredentialsProvider credentials);
        Builder endpointOverride(URL endpointOverride);
        // ...
        S3Presigner build();
    }
}
```

#### 实例化

可通过多种方式实例化该类：

**create 方法**

使用默认区域/凭证链，类似 `S3Client.create()`。

```Java
S3Presigner s3Presigner = S3Presigner.create();
```

**Builder**

可配置区域/凭证，类似 `S3Client.builder().build()`。

```Java
S3Presigner s3Presigner = S3Presigner.builder().region(Region.US_WEST_2).build();
```

**从已有 S3Client**

使用已有 `S3Client` 实例的区域/凭证，类似 `s3.utilities()`。

```Java
S3Client s3 = S3Client.create();
S3Presigner s3Presigner = s3.presigner();
```

**从 S3 网关类**

（实现日期待定）作为 `create()` 与 `builder()` 的可发现别名。

```Java
S3Presigner s3Presigner = S3.presigner();
S3Presigner s3Presigner = S3.presignerBuilder().build();
```

#### 方法

每个操作将生成方法：`Presigned{Operation}Request presign{Operation}({Operation}PresignRequest)`
（例如 `PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest)`）。

*下文 FAQ：「为何每个操作输入形态不同？」与「为何每个操作输出形态不同？」*。

#### 内部类：`{Service}Presigner.Builder`

每个服务 presigner 将有内部类 `{Service}Presigner.Builder`（例如 `S3Presigner.Builder`）。
命名与现有 `{Service}Utilities` 一致。

##### 方法

presigner 构建器至少包含以下配置：

1. `region(Region)`：生成预签名 URL 时使用的区域。
2. `endpointOverride(URI)`：替代由区域推导的端点。
3. `credentialsProvider(AwsCredentialsProvider)`：签名请求时使用的凭证。

更多配置（如 signer、服务专属配置）将在后续调研后补充。

### `{Operation}PresignRequest`

每个支持预签名的操作将生成新的输入类。这些请求继承共同基类，以便用共用代码配置请求。

*下文 FAQ：「为何每个操作输入形态不同？」*。

#### 示例

```Java
/**
 * A request to generate presigned GetObjectRequest, passed to S3Presigner#getObject.
 */
public interface GetObjectPresignRequest extends PresignRequest {
    /**
     * The GetObjectRequest that should be presigned.
     */
    GetObjectRequest getObject();
    // Plus builder boilerplate
}

public interface PresignRequest {
    /**
     * The duration for which this presigned request should be valid. After this time has expired,
     * attempting to use the presigned request will fail.
     */
    Duration signatureDuration();
    // Plus builder boilerplate
}
```

### `Presigned{Operation}Request`

每个支持预签名的操作将生成新的输出类。这些预签名请求继承共同基类，以便用共用代码处理响应。

*下文 FAQ：「为何每个操作输出形态不同？」*。

#### 示例

```Java
/**
 * A presigned GetObjectRequest, returned by S3Presigner#getObject.
 */
public interface PresignedGetObjectRequest extends PresignedRequest {
    // Builder boilerplate
}

/**
 * A generic presigned request. The isBrowserCompatible method can be used to determine whether this request
 * can be executed by a web browser.
 */
public interface PresignedRequest {
    /**
     * The URL that the presigned request will execute against. The isBrowserCompatible method can be used to
     * determine whether this request will work in a browser.
     */
    URL url();

    /**
     * The exact SERVICE time that the request will expire. After this time, attempting to execute the request
     * will fail.
     *
     * This may differ from the local clock, based on the skew between the local and AWS service clocks.
     */
    Instant expiration();

    /**
     * Returns true if the url returned by the url method can be executed in a browser.
     *
     * This is true when the HTTP request method is GET, and hasSignedHeaders and hasSignedPayload are false.
     * 
     * TODO: This isn't a universally-agreed-upon-good method name. We should iterate on it before GA.
     */
    boolean isBrowserCompatible();

    /**
     * Returns true if there are signed headers in the request. Requests with signed headers must have those
     * headers sent along with the request to prevent a "signature mismatch" error from the service.
     */
    boolean hasSignedHeaders();

    /**
     * Returns the subset of headers that were signed, and MUST be included in the presigned request to prevent
     * the request from failing.
     */
    Map<String, List<String>> signedHeaders();

    /**
     * Returns true if there is a signed payload in the request. Requests with signed payloads must have those
     * payloads sent along with the request to prevent a "signature mismatch" error from the service.
     */
    boolean hasSignedPayload();

    /**
     * Returns the payload that was signed, or Optional.empty() if hasSignedPayload is false.
     */
    Optional<SdkBytes> signedPayload();

    /**
     * The entire SigV4 query-parameter signed request (minus the payload), that can be transmitted as-is to a
     * service using any HTTP client that implement the SDK's HTTP client SPI.
     *
     * This request includes signed AND unsigned headers.
     */
    SdkHttpRequest httpRequest();

    // Plus builder boilerplate
}
```



## 里程碑

### M1：手写 S3 GetObject Presigner

**完成标准：** 客户可使用 SDK 提供的 S3 presigner 生成预签名 S3 GetObject 请求。

**预期任务：**

1. 按本文手写相关接口与类定义。
2. 暂不创建 `S3Client#presigner` 方法。
3. 以对核心类最小重构实现 `presignGetObject`。

### M2：手写 S3 PutObject Presigner

**完成标准：** 客户可使用 SDK 提供的 S3 presigner 生成预签名 S3 PutObject 请求。

**预期任务：**

1. 按本文手写相关接口与类定义。
2. 以对核心类最小重构实现 `presignPutObject`。

### M3：手写 Polly SynthesizeSpeech Presigner

**完成标准：** 客户可使用 SDK 提供的 Polly presigner 生成预签名 Polly SynthesizeSpeech 请求。

**预期任务：**

1. 按本文手写相关接口与类定义。
2. 手写生成浏览器兼容 HTTP 请求的 `SynthesizeSpeech` marshaller。
3. 以对核心类最小重构实现 `presignSynthesizeSpeech`。（评估浏览器兼容 HTTP marshaller 是否应为默认。）

### M4：生成的 Presigner

**完成标准：** 既有 presigner 改为生成，客户无需改代码。

**预期任务：**

1. 重构核心类，消除 `presignGetObject`、`presignPutObject` 与 `presignSynthesizeSpeech` 之间不必要重复。
2. 实现定制能力以允许使用浏览器兼容的 `SynthesizeSpeech` marshaller。
3. 更新手写 `presign*` 方法以使用重构后的模型。
4. 更新代码生成以生成 `presign*` 输入/输出。
5. 更新代码生成以生成 `{Service}Presigner` 类。

### M5：生成现有 1.11.x Presigner

**完成标准：** 1.11.x 中存在的操作级 presigner 在 2.x 均可用。

**预期任务：**

1. 生成 EC2 Presigner
2. 生成 RDS Presigner

### M6：生成全部 Presigner

**完成标准：** 所有（可支持预签名的）操作均支持预签名。

*下文 FAQ：「会为哪些操作生成 URL presigner？」*

**预期任务：**

1. 对代表性操作集合测试生成的 presigner

### M7：实例化与可发现性简化

**完成标准：** 所有客户端包含 `{Service}Client#presigner()` 方法。

**预期任务：**

1. 确定需从服务客户端继承哪些配置及继承方式（例如 execution interceptor 如何处理）。
2. 确定异步客户端是否使用独立 presigner 接口，以便未来增加阻塞型凭证/区域提供者时向前兼容。
3. 更新生成客户端，以合理方式支持继承该配置。
4. 为所有支持的服务在客户端上生成该方法。



## 常见问题（FAQ）

### 哪些服务会生成 URL presigner？

若服务存在需要 presigner 支持的操作，将生成 `{Service}Presigner` 类。

### 哪些操作会生成 URL presigner？

随实现里程碑而变化（见上文「里程碑」）。

本文假定支持的操作集合为**全部**操作，但排除带已签名流式负载的操作。后者需额外建模（例如分块编码负载或事件流），若客户有足够需求可在进一步设计后实现。

### 为何每个操作输入形态不同？

输入形态必须感知操作入参，因此选项为：(1) 每个操作各不相同的生成输入形态，或 (2) 用操作入参参数化的通用「核心」输入形态。

以下对比选项 1 与选项 2，说明为何选择选项 1。

**选项 1：** `presignGetObject(GetObjectPresignRequest)` 与 `presignGetObject(Consumer<GetObjectPresignRequest.Builder>)`：
Generated `GetObjectPresignRequest`

```Java
s3.presignGetObject(GetObjectPresignRequest.builder()
                                           .getObject(GetObjectRequest.builder().bucket("bucket").key("key").build())
                                           .signatureDuration(Duration.ofMinutes(15))
                                           .build());
```

```Java
s3.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(15))
                          .getObject(go -> go.bucket("bucket").key("key")));
```

**选项 2：** `presignGetObject(PresignRequest<GetObject>)` 与
`presignGetObject(GetObject, Consumer<PresignRequest.Builder<GetObject>>)`

```Java
s3.presignGetObject(PresignRequest.builder(GetObjectRequest.builder().bucket("bucket").key("key").build())
                                  .signatureDuration(Duration.ofMinutes(15))
                                  .build());
```

```Java
s3.presignGetObject(GetObjectRequest.builder().bucket("bucket").key("key").build(),
                    r -> r.signatureDuration(Duration.ofMinutes(15)));
```

**选项 1 优点：**

1. 对初级开发者更易读
2. 更接近 `S3Client` 签名
3. 构造更简单（实例化 builder 不必先提供操作输入）
4. `Consumer<Builder>` 重载简单得多

**选项 2 优点：**

1. JAR 更小

### 为何每个操作输出形态不同？

该决策不那么直观。输出形态在技术上不必理解输入形态，因此选项为：(1) 每个操作各不相同的生成输出形态；(2) 直接返回 `PresignedRequest`。

以下对比选项 1 与选项 2，说明为何选择选项 1。

**选项 1：** `PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest)`

```Java
PresignedGetObjectRequest presignedRequest = s3.presignGetObject(...);
URL presignedUrl = presignedRequest.getUrl();
```

**选项 2：** `PresignedRequest presignGetObject(GetObjectPresignRequest)`

```Java
PresignedRequest presignedRequest = s3.presignGetObject(...);
URL presignedUrl = presignedRequest.getUrl();
```

**选项 1 优点：**

1. 可对预签名请求做类型安全的执行。
2. 最接近 `S3Client` 方法签名

**选项 2 优点：**

1. JAR 更小

**决策：** 采用选项 1，因空接口成本很小，且未来可支持类型安全的预签名请求执行。

*下文 FAQ：「预签名请求的执行怎么办？」*

### 预签名请求的执行怎么办？

上文设计使任意实现 AWS SDK HTTP 客户端 SPI 的 HTTP 客户端均可执行已签名请求。

未来若允许签名使用者用**完整** SDK 栈（而非仅 HTTP 客户端部分）**执行**预签名 URL，将带来：

1. 网络或服务故障时的自动重试
2. 对有模型响应时的响应体解组
3. SDK 指标集成（实现后）

以下仅为示例（非设计提案）：若 `DynamoDbClient` 支持执行预签名 URL，应确保请求对应正确操作，以便重试与响应处理与服务/操作匹配。

```Java
DynamoDbClient dynamo = DynamoDbClient.create();
PresignedPutItemRequest presignedRequest = dynamo.presigner().presignPutItem(...);
PutItemResponse response = dynamo.putItem(presignedRequest);
```

### 非阻塞请求预签名怎么办？

上文提案未区分阻塞与非阻塞预签名，因为 SDK 目前仅在 HTTP 客户端实现层面区分阻塞与非阻塞。

生成的预签名请求可由阻塞**或**非阻塞 HTTP 客户端执行。

未来若 SDK 实现非阻塞区域提供者与非阻塞凭证提供者，区分阻塞与非阻塞 URL presigner 可能变得重要。

因此需决定：用于获取预配置 URL presigner 的 `presigner()` 是否仅出现在阻塞 `{Service}Client` 上（并为 `{Service}AsyncClient` 提供单独的非阻塞 `{Service}Presigner`），还是同步与异步共用同一 `{Service}Presigner`。
