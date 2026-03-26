# AWS SDK v2 的 Javadoc 指南

## API 分类

AWS SDK for Java v2 使用注解对 API 进行分类：

- **SDK 公开 API**：标注 `@SdkPublicApi` 的类/接口
- **SDK 受保护 API**：标注 `@SdkProtectedApi` 的类/接口
- **SDK 内部 API**：标注 `@SdkInternalApi` 的类/接口

## 文档要求

### 必备文档

- 所有 SDK 公开 API **必须**具备文档
- SDK 公开 API 中所有公开方法 **必须**具备文档，以下情况除外：
  - 实现或重写接口中方法的方法
  - 重写父类中方法的方法
- 对于 SDK 受保护 API 与内部 API，文档为建议项而非强制
- 高层库（例如 DynamoDB Enhanced Client）中 SDK 公开 API 的 Javadoc **应**包含代码片段

## 风格指南

### 一般格式

- Javadoc **必须**遵循 [Javadoc 标准](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html) 正确排版
- 段落开头（首段除外）**必须**单独使用一个 `<p>` 起始标签，且**不要**闭合标签，如下例所示：

  ```java
  /**
   * First paragraph with no <p> tag.
   *
   * <p>Second paragraph starts with a <p> tag.
   *
   * <p>Third paragraph also starts with a <p> tag.
   */
  ```

- 首句应概括方法/类的用途
- 使用完整句子与正确标点
- 使用第三人称、客观陈述（英文文档中对应 "Returns the value"，避免命令式 "Return the value"；中文可用「返回……」等客观表述）

### Javadoc 标签

- 使用 `@param` 为所有参数编写清晰说明
- 使用 `@return` 说明返回值（`void` 方法除外）
- 使用 `@throws` 标明方法可能抛出的异常
- 使用 `@link` 与 `@see` 指向相关方法/类
- 对弃用方法使用 `@deprecated`，并包含：
  - 何时弃用
  - 弃用原因
  - 替代方法，并用 `{@link}` 链接
- 避免使用 `@version` 与 `@since` 标签

### 代码片段

- 使用 `@snippet` 添加代码片段
- **应**优先采用[外部代码片段](https://docs.oracle.com/en/java/javase/18/code-snippet/index.html#external-snippets)，而非内联片段
- 代码片段应：
  - 简明且聚焦演示 API
  - 可编译且正确
  - 用注释说明要点
  - 与代码库其余部分风格一致

## 示例

### 类文档示例

```java
/**
 * A high-level library for uploading and downloading objects to and from Amazon S3.
 * This can be created using the static {@link #builder()} method.
 *
 * <p>S3TransferManager provides a simplified API for efficient transfers between a local environment
 * and S3. It handles multipart uploads/downloads, concurrent transfers, progress tracking, and
 * automatic retries.
 *
 * <p>See {@link S3TransferManagerBuilder} for information on configuring an S3TransferManager.
 *
 * <p>Example usage:
 * {@snippet :
 *   S3TransferManager transferManager = S3TransferManager.builder()
 *       .s3ClientConfiguration(b -> b.credentialsProvider(credentialsProvider)
 *                                    .region(Region.US_WEST_2))
 *       .build();
 *
 *   // Upload a file
 *   UploadFileRequest uploadRequest = UploadFileRequest.builder()
 *       .putObjectRequest(req -> req.bucket("bucket").key("key"))
 *       .source(Paths.get("file.txt"))
 *       .build();
 *
 *   FileUpload upload = transferManager.uploadFile(uploadRequest);
 *   CompletedFileUpload uploadResult = upload.completionFuture().join();
 * }
 *
 * @see S3TransferManagerBuilder
 */
@SdkPublicApi
public interface S3TransferManager extends SdkAutoCloseable {
    // ...
}
```

### 方法文档示例

```java
/**
 * Uploads a file from a specified path to an S3 bucket.
 *
 * <p>This method handles large files efficiently by using multipart uploads when appropriate.
 * Progress can be tracked through the returned {@link FileUpload} object.
 *
 * <p>Example:
 * {@snippet :
 *   UploadFileRequest request = UploadFileRequest.builder()
 *       .putObjectRequest(r -> r.bucket("bucket-name").key("key"))
 *       .source(Paths.get("my-file.txt"))
 *       .build();
 *   
 *   FileUpload upload = transferManager.uploadFile(request);
 *   CompletedFileUpload completedUpload = upload.completionFuture().join();
 * }
 *
 * @param request Object containing the bucket, key, and file path for the upload
 * @return A {@link FileUpload} object to track the upload and access the result
 * @throws S3Exception If any errors occur during the S3 operation
 * @throws SdkClientException If any client-side errors occur
 * @throws IOException If the file cannot be read
 */
FileUpload uploadFile(UploadFileRequest request);
```

### 弃用方法示例

```java
/**
 * Returns the value of the specified header.
 *
 * <p>This method provides direct access to the header value.
 *
 * @param name The name of the header
 * @return The value of the specified header
 * @deprecated Use {@link #firstMatchingHeader(String)} instead, as it properly handles
 *             headers with multiple values.
 */
@Deprecated
String header(String name);
```
