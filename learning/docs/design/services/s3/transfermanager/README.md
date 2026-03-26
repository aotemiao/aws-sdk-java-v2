**设计：** 新功能，**状态：**
[开发中](../../../README.md)

# 项目信条（除非你有更好的）

1. 在客户问题空间中与其对接，使其能快速交付价值。
2. 满足客户预期驱动易用性。
3. 可发现性驱动使用量。

# 引言

本项目通过提供构建在 S3 客户端之上的高层库 `S3TransferManager`，显著改善需要轻松在本地与 S3 之间上传、下载对象的客户体验。

# 项目目标

1. 在其覆盖的用例（对象与 S3 之间的传输）中，`S3TransferManager` 为首选方案：比直接使用 S3 客户端更简单直观；在多数场景下性能更好。
1. `S3TransferManager` 提供真正异步、非阻塞的 API，与 SDK 其余部分惯例一致。
1. `S3TransferManager` 高效利用系统资源。
1. `S3TransferManager` 补充而非替代底层 S3 客户端。

# 非目标

1. 使用阻塞式同步客户端的能力。

   使用阻塞客户端会严重阻碍实现上述目标 #2 与 #3。

# 相对 1.11.x 的客户诉求变更

* `S3TransferManager` 支持更易用的进度监听器。

  参考：https://github.com/aws/aws-sdk-java-v2/issues/37#issuecomment-316218667

* `S3TransferManager` 提供上传与下载的带宽限制。

  参考：https://github.com/aws/aws-sdk-java/issues/1103

* 用户配置的 Transfer Manager 资源规模不应影响其稳定性。

  例如，线程池配置大小不应影响操作能否成功完成。

  参考：https://github.com/aws/aws-sdk-java/issues/939

* `S3TransferManager` 支持任意对象的并行下载。

  存储在 S3 中的任意对象都应能多部分并行下载，而不仅限于通过 Multipart API 上传的对象。

* `S3TransferManager` 支持向预签名 URL 上传及从预签名 URL 下载。

* `S3TransferManager` 支持在内存与 S3 之间上传、下载。

  参考：https://github.com/aws/aws-sdk-java/issues/474

* 能轻松对所有传至 S3 的传输使用预置 ACL 策略。

  参考：https://github.com/aws/aws-sdk-java/issues/1207

* 并行上传与下载的尾部校验和（trailing checksums）。
