
# S3 预签名 URL GET — 决策记录

## 评审会议：2025-06-17
**出席**：Alban Gicquel、John Viegas、Zoe Wang、Dongie Agnir、Bole Yi、Ran Vaknin、Saranya Somepalli

### 已关闭决策

1. 是否为预签名 URL 新建 `PresignedUrlGetObjectResponse`，还是沿用现有 `GetObjectResponse`？决定沿用现有 `GetObjectResponse`，因预签名 URL GET 的 HTTP 响应与标准 S3 GetObject 相同。

2. 使用现有 SDK 与 S3 异常，还是为过期 URL 等校验错误新增专用异常？决定复用现有 SDK 异常，不为预签名 URL 单独造异常体系。

3. 是否在客户端做额外校验并以服务端校验为兜底，还是完全依赖 S3 服务端校验？不对预签名 URL 增加客户端校验。

### 已讨论议题

1. 是否有其他方式跳过签名，例如使用 `NoOpSigner()`，而非设置额外 Execution 属性？已在设计文档中补充使用 `NoOpSigner()`。

2. S3 响应是否包含校验和？若包含，是否在本项目实现校验和相关支持，还是等 Transfer Manager 支持后再做？S3 响应不包含校验和。

3. 辅助 API 应如何命名？候选包括 `PresignedURLManager` 或 `PresignedUrlExtension`。将在 Surface API 评审中确定。

## 评审会议：2025-06-23
**出席**：John Viegas、Zoe Wang、Dongie Agnir、Bole Yi、Ran Vaknin、Saranya Somepalli、David Ho

### 已决策议题

1. `PresignedUrlGetObjectRequest` 是否应继承 `S3Request`/`SdkRequest`？决定使用仅含最少参数（`presignedUrl`、`rangeStart`、`rangeEnd`）的独立请求类，避免暴露凭证、签名器等不兼容配置；内部再转换为 `S3Request` 以兼容 `ClientHandler`。

2. 将 `IS_DISCOVERED_ENDPOINT` 执行属性替换为语义更贴切的方案。决定新增 `SKIP_ENDPOINT_RESOLUTION` 执行属性，专用于应跳过端点解析的预签名 URL 场景；`IS_DISCOVERED_ENDPOINT` 与已弃用的端点发现功能绑定。

3. 使用独立 `rangeStart`/`rangeEnd` 字段还是单一 range 字符串？曾决定使用独立 `Long` 字段以改善体验；后见下文会议调整。

## 决策投票会议：2025-06-30
**出席**：John Viegas、Zoe Wang、Dongie Agnir、Bole Yi、Ran Vaknin、Saranya Somepalli、David Ho、Alex Woods

### 已决策议题
决定在请求对象上使用 `String range` 字段，以支持 RFC 7233 全部格式（含后缀范围 `bytes=-100`）及未来多范围支持；S3 当前不支持多范围，但未来若支持可无需再改 SDK。

## 站会后会议：2025-07-14 
**出席**：Alban Gicquel、John Viegas、Zoe Wang、Dongie Agnir、Bole Yi、Ran Vaknin、Saranya Somepalli、David Ho、Alex Woods

### 已决策议题
团队决定现阶段仅实现 S3 异步客户端功能，同步 `S3Client` 延后，因同步路径需支持多段下载能力。

## API 面评审：2025-07-21
**出席**：John Viegas、Zoe Wang、Dongie Agnir、Bole Yi、Ran Vaknin、Saranya Somepalli、David Ho、Alex Woods

### 已决策议题

1. 表面 API 命名：`AsyncPresignedUrlExtension` 为核心新 API；从 `AsyncS3Client` 调用的方法为 `presignedUrlExtension()`；Get Object 请求为 `PresignedUrlDownloadRequest`。指标收集的操作名为 `PresignedUrlDownload`。

2. 从 Get 请求模型中移除 consumer builder 模式。

3. 多段 S3 客户端与 S3 CRT 客户端暂时抛出 `UnsupportedOperationException`。

## 设计评审：2025-07-31
**来源**：设计：预签名 URL 的多段下载支持  
**出席**：John Viegas、Zoe Wang、Dongie Agnir、Bole Yi、Ran Vaknin、Saranya Somepalli、David Ho、Olivier Lepage Applin

### 已决策议题
曾考虑先单独发起发现请求（`bytes=0-0`）再下载；最终决定遵循 AWS Transfer Manager SEP，使用 `Range: bytes=0-{partSizeInBytes-1}` 同时下载首段并从 `Content-Range` 响应头发现对象总大小。

## Transfer Manager 集成评审：2025-08-19
**出席**：John Viegas、Zoe Wang、Dongie Agnir、Alex Woods、Bole Yi、Olivier Lepage Applin、Ran Vaknin、David Ho、Saranya Somepalli

### 已决策议题

1. **API 名称**：在后续 Surface API 评审中最终确定 Transfer Manager 方法 `downloadWithPresignedUrl` 与 `downloadFileWithPresignedUrl`。

2. **暂停/恢复支持**：决定不支持预签名 URL 下载的暂停/恢复，与 AWS SDK for Java v1 对该场景的行为一致（v1 亦无此能力）。
