# 设计文档（S3 预签名 URL GET）

## 引言

本设计在 AWS SDK Java v2 中引入通过预签名 URL 下载 S3 对象，与 [v1](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/PresignedUrlDownload.html) 功能对齐。部分客户需要通过预签名 URL 下载 S3 对象，同时仍希望获得客户端侧 SDK 能力（自动重试、指标收集、类型化响应对象等）。

本文说明该能力在 Java SDK v2 中的实现方式，回应客户功能请求（[GitHub Issue #2731](https://github.com/aws/aws-sdk-java-v2/issues/2731)、[GitHub Issue #181](https://github.com/aws/aws-sdk-java-v2/issues/181)），并降低临时访问场景下的使用复杂度。

## 设计评审

决策记录见：[DecisionLog.md](DecisionLog.md)

Java SDK 团队决定实现独立的 `AsyncPresignedUrlExtension`。相较直接集成到 `S3AsyncClient`，团队选择辅助 API 模式，以保持职责清晰并保留 SDK 既有能力。

## 概述

设计引入新的辅助 API `AsyncPresignedUrlExtension`，可通过现有 `S3AsyncClient` 实例化。该扩展在保留 SDK 能力的同时，处理预签名 URL 请求的特殊要求。

本设计为 `S3AsyncClient` 实现预签名 URL 的 GET/下载，含大对象多段下载支持。同步 `S3Client` 实现留待后续工作。

## 拟议 API

v2 SDK 将支持面向异步客户端的预签名 URL 扩展，以利用预签名 URL 下载。

### 实例化
从已有客户端实例化：

```java
// Async Presigned URL Extension
S3AsyncClient s3Client = S3AsyncClient.create();
AsyncPresignedUrlExtension presignExtension = s3Client.presignedUrlExtension();
```

### 一般用法示例

```java
// Create presigned URL request
PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                .presignedUrl(presignedUrl)
                                                .range("range=0-1024")
                                                .build();

// Async usage
S3AsyncClient s3Client = S3AsyncClient.create();
AsyncPresignedUrlExtension presignedUrlExtension = s3Client.presignedUrlExtension();
CompletableFuture<GetObjectResponse> response = presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());
```

### 多段下载支持

当对象大小大于 `minimumPartSizeInBytes` 且启用多段时，SDK 自动使用带 HTTP Range 头的多段下载：

```java
// Enable multipart downloads
S3AsyncClient s3Client = S3AsyncClient.builder()
    .multipartEnabled(true)
    .build();

// Or with custom configuration
MultipartConfiguration config = MultipartConfiguration.builder()
    .minimumPartSizeInBytes(8 * 1024 * 1024)   // 8MB parts
    .build();

S3AsyncClient multipartClient = S3AsyncClient.builder()
    .multipartConfiguration(config)
    .build();

// Download automatically uses multipart for objects larger than the configured part size
CompletableFuture<ResponseBytes<GetObjectResponse>> response = 
    multipartClient.presignedUrlExtension().getObject(request, AsyncResponseTransformer.toBytes());
```

多段实现使用 Range 头（例如 `bytes=0-8388607`），而非 `partNumber` 参数，以保留预签名 URL 签名。首次请求下载首段并通过 `Content-Range` 头发现对象总大小。

### AsyncPresignedUrlExtension 接口

```java
/**
 * Interface for presigned URL operations used by Async clients.
 */
@SdkPublicApi
public interface AsyncPresignedUrlExtension {
    
    /**
     * Downloads S3 objects using pre-signed URLs with custom response transformation.
     *
     * @param request the presigned URL request.
     * @param responseTransformer custom transformer for processing the response.
     * @return a CompletableFuture of the transformed response.
     */
    <T> CompletableFuture<T> getObject(PresignedUrlDownloadRequest request,
                                      AsyncResponseTransformer<GetObjectResponse, T> responseTransformer);
    
    // Additional getObject() overloads for file downloads, byte arrays, etc.
    // Standard Builder interface with client() and overrideConfiguration() methods
}
```

### PresignedUrlDownloadRequest

```java
/**
 * Request object for presigned URL GET operations.
 */
@SdkPublicApi
@ThreadSafe
public final class PresignedUrlDownloadRequest 
        implements ToCopyableBuilder<PresignedUrlDownloadRequest.Builder, PresignedUrlDownloadRequest> {
    
    private final URL presignedUrl;
    private final String range;
    
    // Standard getters: presignedUrl(), range()
    // Standard builder methods: builder(), toBuilder()
    // Standard Builder class with presignedUrl(), range() setter methods
}
```

## Transfer Manager 集成

`S3TransferManager` 扩展支持预签名 URL 下载，在既有 `AsyncPresignedUrlExtension` 之上提供进度跟踪、多段优化等高层能力。

### 新增 Transfer Manager API

```java
// File-based presigned URL download
FileDownload download = transferManager.downloadFileWithPresignedUrl(
    PresignedDownloadFileRequest.builder()
        .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
            .presignedUrl(presignedUrl)
            .build())
        .destination(Paths.get("downloaded-file.txt"))
        .addTransferListener(LoggingTransferListener.create())
        .build());

// Generic download with custom response transformer
Download<ResponseBytes<GetObjectResponse>> download = transferManager.downloadWithPresignedUrl(
    PresignedDownloadRequest.builder()
        .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
            .presignedUrl(presignedUrl)
            .build())
        .responseTransformer(AsyncResponseTransformer.toBytes())
        .build());
```

### 主要能力

- **进度跟踪**：完整 `TransferListener` 支持，含 `bytesTransferred` 回调
- **多段下载**：大对象自动多段，使用 HTTP Range 头
- **API 一致**：返回类型与普通传输相同的 `FileDownload`/`Download`
- **错误处理**：对 `Content-Range` 头与响应完整性做充分校验

### 限制

- **无暂停/恢复**：因 URL 过期与认证约束，预签名 URL 下载不支持暂停/恢复
- **S3CrtTransferManager**：因预签名 URL 与签名冲突，当前不支持

## 常见问题

### 为何不在 `S3AsyncClient` 上直接实现预签名 URL 下载/GET？

曾考虑三种方案：

1. **独立 `AsyncPresignedUrlExtension`（选定）**：通过 `s3AsyncClient.presignedUrlExtension()` 访问的独立扩展
   - **优点**：职责清晰、保留 SDK 能力、符合 v2 模式
   - **缺点**：客户需学习新 API 面

2. **直接在 `S3Client` 集成**：在 `S3Client` 上增加预签名 URL 方法
   - **优点**：接口熟悉、从 v1 迁移路径直接
   - **缺点**：需改核心拦截器、集成复杂，且易与标准服务 API 混淆

3. **扩展 `S3Presigner` 以执行 URL**：在现有 `S3Presigner` 上扩展
   - **优点**：概念上延续 presigner
   - **缺点**：打破当前无状态 presigner 模式

**决策：** 选项 1 在保留 SDK 能力的同时职责清晰，并符合 v2 工具类模式。

### 同步 `S3Client` 与 S3 CRT 客户端支持呢？

同步 `S3Client` 因多段下载等复杂度延后。待 AWS CRT 团队解决当前预签名 URL 处理限制后，再补充 `S3CrtAsyncClient` 支持。

### 为何 `PresignedUrlDownloadRequest` 不继承 `S3Request`？

若继承 `S3Request` 虽可获得 `RequestOverrideConfiguration`，但其中许多配置（如凭证提供者、签名器）在预签名 URL 执行场景不适用。因此采用仅含必要参数（`presignedUrl`、`range`）的独立请求；内部再封装为继承 `S3Request` 的类以供 `ClientHandler` 使用。

## 参考资料

**GitHub 功能请求：**
- [S3 Presigned URL Support #2731](https://github.com/aws/aws-sdk-java-v2/issues/2731)
- [Presigned URL GET Support #181](https://github.com/aws/aws-sdk-java-v2/issues/181)

**AWS 文档：**
- [S3 Pre-signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/presigned-urls.html)

**SDK 文档：**
- [AWS SDK for Java v1 implementation](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html)
- [S3 Client architecture patterns](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html)
