# 命名约定

本页说明命名约定、名词与常用术语。

## 类命名

### 一般规则
* 类名优先用单数：`SdkSystemSettings` 应写作 `SdkSystemSetting`。
* 将首字母缩写视为单个词：`DynamoDBClient` 应写作 `DynamoDbClient`。
  
### 负责实例化其他类的类

* 若类的主要职责是返回另一类实例：
  * 若「获取」方法无参数：
    * 若类实现 `Supplier`：命名为 `{Noun}Supplier`（例如 `CachedSupplier`）
    * 若类不实现 `Supplier`：命名为 `{Noun}Provider`（例如 `AwsCredentialsProvider`）
  * 若「获取」方法带参数：命名为 `{Noun}Factory`（例如 `AwsJsonProtocolFactory`）

### 与服务相关的类

* 若类会发起服务调用：
  * 若能用于调用**所有**数据面操作：
    * 若为代码生成：
      * 若使用同步 HTTP：命名为 `{ServiceName}Client`（例如 `DynamoDbClient`）
      * 若使用异步 HTTP：命名为 `{ServiceName}AsyncClient`（例如 `DynamoDbAsyncClient`）
    若为手写：
      * 若使用同步 HTTP：命名为 `{ServiceName}EnhancedClient`（例如 `DynamoDbEnhancedClient`）
      * 若使用异步 HTTP：命名为 `{ServiceName}EnhancedAsyncClient`（例如 `DynamoDbEnhancedAsyncClient`）
  * 若仅能调用**部分**数据面操作：
    * 若使用同步 HTTP：命名为 `{ServiceName}{Noun}Manager`（例如 `SqsBatchManager`）
    * 若使用异步 HTTP：命名为 `{ServiceName}Async{Noun}Manager`（例如 `SqsAsyncBatchManager`）
    * 说明：若唯一实现使用异步 HTTP，可省略 `Async`（例如 `S3TransferManager`）
* 若类不发起服务调用：
  * 若用于创建预签名 URL：命名为 `{ServiceName}Presigner`（例如 `S3Presigner`）
  * 若为多种无关「辅助」方法的集合：命名为 `{ServiceName}Utilities`（例如 `S3Utilities`）

## 测试命名

测试名称**应**遵循 `methodToTest_when_expectedBehavior`（例如 `close_withCustomExecutor_shouldNotCloseCustomExecutor`、`uploadDirectory_withDelimiter_filesSentCorrectly`）
