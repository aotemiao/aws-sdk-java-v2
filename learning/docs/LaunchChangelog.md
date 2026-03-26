# 1.11 至 2.0 变更日志

- [1. 客户端](#1-客户端)
    - [1.1. 客户端创建默认值](#11-客户端创建默认值)
    - [1.2. AWS 客户端配置：自定义区域、凭证与终端节点](#12-aws-客户端配置自定义区域凭证与终端节点)
        - [1.2.1. 客户端区域](#121-客户端区域)
        - [1.2.2. 客户端凭证](#122-客户端凭证)
    - [1.3. SDK 客户端配置](#13-sdk-客户端配置)
        - [1.3.1. 客户端 HTTP 配置](#131-客户端-http-配置)
        - [1.3.2. 客户端 HTTP 代理配置](#132-客户端-http-代理配置)
        - [1.3.3. 客户端覆盖配置](#133-客户端覆盖配置)
        - [1.3.4. 客户端覆盖重试配置](#134-客户端覆盖重试配置)
        - [1.3.5. 异步配置](#135-异步配置)
        - [1.3.6. 其他选项](#136-其他选项)
- [2. 操作、请求与响应的变更](#2-操作请求与响应的变更)
    - [2.1. 流式操作](#21-流式操作)
- [3. 异常变更](#3-异常变更)
- [4. 服务相关变更](#4-服务相关变更)
    - [4.1. S3 变更](#41-s3-变更)
        - [4.1.1. S3 操作迁移](#411-s3-操作迁移)
    - [4.2. SNS 变更](#42-sns-变更)
    - [4.3. SQS 变更](#43-sqs-变更)
- [5. 配置文件（Profile）变更](#5-配置文件profile变更)
- [6. 对照表](#6-对照表)
    - [6.1. 环境变量与系统属性](#61-环境变量与系统属性)
    - [6.2. 凭证提供程序](#62-凭证提供程序)
    - [6.3. 客户端名称](#63-客户端名称)
- [7. 高级库](#7-高级库)

# 1. 客户端

与 AWS 服务通信最直接的方式是使用诸如 `DynamoDbClient` 之类的客户端。有关 S3 Transfer Manager、DynamoDB Mapper、S3 加密客户端与 waiter 等高级库的状态，请参阅 [7. 高级库](#7-高级库)。

在 2.0 中，客户端发生了以下变更：

1. 客户端不再可变。
2. 客户端不再能通过默认构造函数创建，必须使用静态方法 `create` 或 `builder`：`new AmazonDynamoDBClient` 现为 `DynamoDbClient.create`，`AmazonDynamoDBClient.builder` 现为 `DynamoDbClient.builder`。
3. 客户端构建器不再包含静态方法，应使用客户端上的静态方法：`AmazonDynamoDBClientBuilder.defaultClient` 现为 `DynamoDbClient.create`，`AmazonDynamoDBClientBuilder.standard` 现为 `DynamoDbClient.builder`。
4. 客户端类已重命名。2.0 中对应的客户端名称见 [6.3. 客户端名称](#63-客户端名称)。
5. 异步客户端现使用非阻塞 IO。
6. 异步操作现返回 `CompletableFuture`。
7. 异步客户端现仅将内部执行器用于在 `CompletableFuture` 上调用 `complete` 以及重试。

## 1.1. 客户端创建默认值

在 2.0 中，默认客户端创建逻辑发生如下变更：

1. S3 的默认凭证提供链不再包含匿名凭证。对 S3 的匿名访问须通过 `AnonymousCredentialsProvider` 手动指定。
2. 与默认客户端创建相关的下列环境变量已变更：
   1. `AWS_CBOR_DISABLED` 现为 `CBOR_ENABLED`
   2. `AWS_ION_BINARY_DISABLE` 现为 `BINARY_ION_ENABLED`
3. 与默认客户端创建相关的下列系统属性已变更：
   1. `com.amazonaws.sdk.disableEc2Metadata` 现为 `aws.disableEc2Metadata`。
   2. `com.amazonaws.sdk.ec2MetadataServiceEndpointOverride` 现为 `aws.ec2MetadataServiceEndpoint`。
   3. `com.amazonaws.sdk.disableCbor` 现为 `aws.cborEnabled`。
   4. `com.amazonaws.sdk.disableIonBinary` 现为 `aws.binaryIonEnabled`。
   5. 下列系统属性不再支持：`com.amazonaws.sdk.disableCertChecking`、`com.amazonaws.sdk.enableDefaultMetrics`、`com.amazonaws.sdk.enableThrottledRetry`、`com.amazonaws.regions.RegionUtils.fileOverride`、`com.amazonaws.regions.RegionUtils.disableRemote`、`com.amazonaws.services.s3.disableImplicitGlobalClients`、`com.amazonaws.sdk.enableInRegionOptimizedMode`
4. 不再支持从自定义 `endpoints.json` 文件加载区域配置。
5. 默认凭证逻辑已修改。更多说明见下文 `com.amazonaws.auth.DefaultAWSCredentialsProviderChain` 相关变更。
6. 配置文件格式已变更，以更贴近 CLI 行为。详见 [5. 配置文件（Profile）变更](#5-配置文件profile变更)。

## 1.2. AWS 客户端配置：自定义区域、凭证与终端节点

在 2.0 中，区域、凭证与终端节点必须通过客户端构建器指定。

| 设置 | 1.x（客户端） | 1.x（构建器） | 2.0 |
|-------------|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| 区域 | `new AmazonDynamoDBClient()`<br />`.withRegion(Regions.US_EAST_1)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withRegion(Regions.US_EAST_1)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.region(Region.US_EAST_1)`<br />`.build()` |
| 凭证 | `new AmazonDynamoDBClient(credentials)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withCredentials(credentials)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.credentials(credentials)`<br />`.build()` |
| 终端节点 | `new AmazonDynamoDBClient()`<br />`.withRegion(signingRegion)`<br />`.withEndpoint(endpoint)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion))`<br />`.build()` | `DynamoDbClient.builder()`<br />`.region(signingRegion)`<br />`.endpointOverride(endpoint)`<br />`.build()` |

### 1.2.1. 客户端区域

在 2.0 中，与区域相关的变更如下：

1. 若使用的服务当前没有区域专属终端节点，必须使用 `Region.AWS_GLOBAL` 或 `Region.AWS_CN_GLOBAL`，而不能使用区域专属终端节点。
2. `com.amazonaws.regions.Regions` 的变更：
    1. 该类已由 `software.amazon.awssdk.regions.Region` 替代。
    2. `Regions.fromName` 现为 `Region.of`。
    3. `Regions.getName` 现为 `Region.id`。
    4. 下列 `Regions` 方法与字段不再支持：`DEFAULT_REGION`、`getDescription`、`getCurrentRegion`、`name`。
3. `com.amazonaws.regions.Region` 的变更：
    1. 用于区域标识：
        1. 该类已由 `software.amazon.awssdk.regions.Region` 替代，通过 `Region.of` 创建。
        2. `Region.getName` 现为 `Region.id`。
    2. 用于区域元数据：
        1. 该类已由 `software.amazon.awssdk.regions.RegionMetadata` 替代，通过 `RegionMetadata.of` 创建。
        2. `Region.getName` 现为 `RegionMetadata.name`。
        3. `Region.getDomain` 现为 `RegionMetadata.domain`。
        4. `Region.getPartition` 现为 `RegionMetadata.partition`。
    3. 用于服务元数据：
        1. 该类已由 `software.amazon.awssdk.regions.ServiceMetadata` 替代，通过调用 `ServiceMetadata.of`（或任意服务客户端上的 `serviceMetadata` 方法）创建。
        2. `Region.getServiceEndpoint` 现为 `ServiceMetadata.endpointFor(Region)`。
        3. `Region.isServiceSupported` 现为 `ServiceMetadata.regions().contains(Region)`。
    4. 下列 `Region` 方法不再支持：`hasHttpsEndpoint`、`hasHttpEndpoint`、`getAvailableEndpoints`、`createClient`。

### 1.2.2. 客户端凭证

在 2.0 中，与凭证提供程序相关的变更如下：

1. `com.amazonaws.auth.AWSCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.AwsCredentialsProvider` 替代。
    2. `AWSCredentialsProvider.getCredentials` 现为 `AwsCredentialsProvider.resolveCredentials`。
    3. 下列 `AWSCredentialsProvider` 方法不再支持：`refresh`。
2. `com.amazonaws.auth.DefaultAWSCredentialsProviderChain` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider` 替代。
    2. `new DefaultAWSCredentialsProviderChain` 现为 `DefaultCredentialsProvider.create`。
    3. 系统属性的优先级高于环境变量。
    4. 更多变更见下文 `EnvironmentVariableCredentialsProvider`、`SystemPropertiesCredentialsProvider`、`ProfileCredentialsProvider` 与 `EC2ContainerCredentialsProviderWrapper`。
    5. 下列 `DefaultAWSCredentialsProviderChain` 方法不再支持：`getInstance`。
3. `com.amazonaws.auth.AWSStaticCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.StaticCredentialsProvider` 替代。
    2. `new AWSStaticCredentialsProvider` 现为 `StaticCredentialsProvider.create`。
4. `com.amazonaws.auth.EnvironmentVariableCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider` 替代。
    2. `new EnvironmentVariableCredentialsProvider` 现为 `EnvironmentVariableCredentialsProvider.create`。
    3. 环境变量 `AWS_ACCESS_KEY` 现为 `AWS_ACCESS_KEY_ID`。
    4. 环境变量 `AWS_SECRET_KEY` 现为 `AWS_SECRET_ACCESS_KEY`。
5. `com.amazonaws.auth.SystemPropertiesCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider` 替代。
    2. `new SystemPropertiesCredentialsProvider` 现为 `SystemPropertyCredentialsProvider.create`。
    3. 系统属性 `aws.secretKey` 现为 `aws.secretAccessKey`。
6. `com.amazonaws.auth.profile.ProfileCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider` 替代。
    2. `new ProfileCredentialsProvider` 现为 `ProfileCredentialsProvider.create`。
    3. 自定义配置文件路径现通过 `ProfileCredentialsProvider.builder` 指定。
    4. 环境变量 `AWS_CREDENTIAL_PROFILES_FILE` 现为 `AWS_SHARED_CREDENTIALS_FILE`。
    5. 配置文件格式已变更，以更贴近 CLI 行为。详见 [5. 配置文件（Profile）变更](#5-配置文件profile变更)。
7. `com.amazonaws.auth.ContainerCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider` 替代。
    2. 异步刷新通过 `ContainerCredentialsProvider.builder` 指定。
    3. `new ContainerCredentialsProvider` 现为 `ContainerCredentialsProvider.create`。
8. `com.amazonaws.auth.InstanceProfileCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider` 替代。
    2. 异步刷新通过 `InstanceProfileCredentialsProvider.builder` 指定。
    3. `new InstanceProfileCredentialsProvider` 现为 `InstanceProfileCredentialsProvider.create`。
    4. 系统属性 `com.amazonaws.sdk.disableEc2Metadata` 现为 `aws.disableEc2Metadata`。
    5. 系统属性 `com.amazonaws.sdk.ec2MetadataServiceEndpointOverride` 现为 `aws.ec2MetadataServiceEndpoint`。
    6. 下列 `AWSCredentialsProvider` 方法不再支持：`getInstance`。
9. `com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider` 替代。
    2. 异步刷新不再是默认行为，但可通过 `StsAssumeRoleCredentialsProvider.builder` 指定。
    3. `new STSAssumeRoleSessionCredentialsProvider` 与 `new STSAssumeRoleSessionCredentialsProvider.Builder` 现为 `StsAssumeRoleCredentialsProvider.builder`。
    4. 所有构建器配置已改为通过指定 `StsClient` 与 `AssumeRoleRequest` 完成。
10. `com.amazonaws.auth.STSSessionCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider` 替代。
    2. 异步刷新不再是默认行为，但可通过 `StsGetSessionTokenCredentialsProvider.builder` 指定。
    3. `new STSAssumeRoleSessionCredentialsProvider` 现为 `StsGetSessionTokenCredentialsProvider.builder`。
    4. 所有构造函数参数已改为在构建器中通过指定 `StsClient` 与 `GetSessionTokenRequest` 完成。
11. `com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider` 的变更：
    1. 该类已由 `software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider` 替代。
    2. 异步刷新不再是默认行为，但可通过 `StsAssumeRoleWithWebIdentityCredentialsProvider.builder` 指定。
    3. `new WebIdentityFederationSessionCredentialsProvider` 现为 `StsAssumeRoleWithWebIdentityCredentialsProvider.builder`。
    4. 所有构造函数参数已改为在构建器中通过指定 `StsClient` 与 `AssumeRoleWithWebIdentityRequest` 完成。
12. `com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper` 已移除，请改用 `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider` 与 `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider`。
13. `com.amazonaws.services.s3.S3CredentialsProviderChain` 已移除，请改用 `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider` 与 `software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider`。
14. `com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider` 与 `com.amazonaws.auth.PropertiesFileCredentialsProvider` 已移除。

## 1.3. SDK 客户端配置

在 1.x 中，通过在客户端或客户端构建器上设置 `ClientConfiguration` 实例来修改 SDK 客户端配置：

| 1.x（客户端） | 1.x（构建器） |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `new AmazonDynamoDBClient(clientConfiguration)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` |

在 2.0 中，SDK 客户端配置拆分为多项独立设置：

**同步配置**

```Java
ProxyConfiguration.Builder proxyConfig =
        ProxyConfiguration.builder();

ApacheHttpClient.Builder httpClientBuilder = 
        ApacheHttpClient.builder()
                        .proxyConfiguration(proxyConfig.build());

ClientOverrideConfiguration.Builder overrideConfig =
        ClientOverrideConfiguration.builder();

DynamoDbClient client = 
        DynamoDbClient.builder()
                      .httpClientBuilder(httpClientBuilder)
                      .overrideConfiguration(overrideConfig.build())
                      .build();
```

**异步配置**

```Java
NettyNioAsyncHttpClient.Builder httpClientBuilder = 
        NettyNioAsyncHttpClient.builder();

ClientOverrideConfiguration.Builder overrideConfig =
        ClientOverrideConfiguration.builder();

ClientAsyncConfiguration.Builder asyncConfig =
        ClientAsyncConfiguration.builder();

DynamoDbAsyncClient client = 
        DynamoDbAsyncClient.builder()
                           .httpClientBuilder(httpClientBuilder)
                           .overrideConfiguration(overrideConfig.build())
                           .asyncConfiguration(asyncConfig.build())
                           .build();
```

### 1.3.1. 客户端 HTTP 配置

1. 现可通过 `clientBuilder.httpClientBuilder` 在运行时指定所用 HTTP 客户端实现。
2. 传入 `clientBuilder.httpClient` 的 HTTP 客户端默认不会关闭，可在多个 AWS 客户端之间共享。
3. 异步客户端的 HTTP 现使用非阻塞 IO。
4. 部分操作现使用 HTTP/2 以提升性能。

| 设置 | 1.x | 2.0（同步，Apache） | 2.0（异步，Netty） |
|---------------------------|-----------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `ApacheHttpClient.Builder httpClientBuilder =`<br />`ApacheHttpClient.builder()` | `NettyNioAsyncHttpClient.Builder httpClientBuilder =`<br />`NettyNioAsyncHttpClient.builder()` |
| 最大连接数 | `clientConfig.setMaxConnections(...)`<br />`clientConfig.withMaxConnections(...)` | `httpClientBuilder.maxConnections(...)` | `httpClientBuilder.maxConcurrency(...)` |
| 连接超时 | `clientConfig.setConnectionTimeout(...)`<br />`clientConfig.withConnectionTimeout(...)` | `httpClientBuilder.connectionTimeout(...)` | `httpClientBuilder.connectionTimeout(...)` |
| 套接字超时 | `clientConfig.setSocketTimeout(...)`<br />`clientConfig.withSocketTimeout(...)` | `httpClientBuilder.socketTimeout(...)` | `httpClientBuilder.writeTimeout(...)` <br /> `httpClientBuilder.readTimeout(...)` |
| 连接 TTL | `clientConfig.setConnectionTTL(...)`<br />`clientConfig.withConnectionTTL(...)` | `httpClientBuilder.connectionTimeToLive(...)` | `httpClientBuilder.connectionTimeToLive(...)` |
| 连接最大空闲时间 | `clientConfig.setConnectionMaxIdleMillis(...)`<br />`clientConfig.withConnectionMaxIdleMillis(...)` | `httpClientBuilder.connectionMaxIdleTime(...)` | `httpClientBuilder.connectionMaxIdleTime(...)` |
| 不活跃后校验 | `clientConfig.setValidateAfterInactivityMillis(...)`<br />`clientConfig.withValidateAfterInactivityMillis(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 本地地址 | `clientConfig.setLocalAddress(...)`<br />`clientConfig.withLocalAddress(...)` | `httpClientBuilder.localAddress(...)` | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/857) |
| Expect-Continue | `clientConfig.setUseExpectContinue(...)`<br />`clientConfig.withUseExpectContinue(...)` | `httpClientBuilder.expectContinueEnabled(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 连接回收器 | `clientConfig.setUseReaper(...)`<br />`clientConfig.withReaper(...)` | `httpClientBuilder.useIdleConnectionReaper(...)` | `httpClientBuilder.useIdleConnectionReaper(...)` |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.httpClientBuilder(httpClientBuilder)`<br />`.build()` | `DynamoDbAsyncClient.builder()`<br />`.httpClientBuilder(httpClientBuilder)`<br />`.build()` |


### 1.3.2. 客户端 HTTP 代理配置

| 设置 | 1.x | 2.0（同步，Apache） | 2.0（异步，Netty） |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `ProxyConfiguration.Builder proxyConfig =`<br />`ProxyConfiguration.builder()` | `ProxyConfiguration.Builder proxyConfig =`<br />`ProxyConfiguration.builder()` |
| 代理主机 | `clientConfig.setProxyHost(...)`<br />`clientConfig.withProxyHost(...)` | `proxyConfig.endpoint(...)` | `proxyConfig.host(...)` |
| 代理端口 | `clientConfig.setProxyPort(...)`<br />`clientConfig.withProxyPort(...)` | `proxyConfig.endpoint(...)` | `proxyConfig.port(...)` |
| 代理用户名 | `clientConfig.setProxyUsername(...)`<br />`clientConfig.withProxyUsername(...)` | `proxyConfig.username(...)` | `proxyConfig.username(...)` |
| 代理密码 | `clientConfig.setProxyPassword(...)`<br />`clientConfig.withProxyPassword(...)` | `proxyConfig.password(...)` | `proxyConfig.password(...)` |
| 代理域 | `clientConfig.setProxyDomain(...)`<br />`clientConfig.withProxyDomain(...)` | `proxyConfig.ntlmDomain(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 代理工作站 | `clientConfig.setProxyWorkspace(...)`<br />`clientConfig.withProxyWorkstation(...)` | `proxyConfig.ntlmWorkstation(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 代理身份验证方法 | `clientConfig.setProxyAuthenticationMethods(...)`<br />`clientConfig.withProxyAuthenticationMethods(...)` | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/858) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 抢先式基本代理身份验证 | `clientConfig.setPreemptiveBasicProxyAuth(...)`<br />`clientConfig.withPreemptiveBasicProxyAuth(...)` | `proxyConfig.preemptiveBasicAuthenticationEnabled(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 非代理主机 | `clientConfig.setNonProxyHosts(...)`<br />`clientConfig.withNonProxyHosts(...)` | `proxyConfig.nonProxyHosts(...)` | `proxyConfig.nonProxyHosts(...)` |
| 禁用套接字代理 | `clientConfig.setDisableSocketProxy(...)`<br />`clientConfig.withDisableSocketProxy(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `httpClientBuilder.proxyConfiguration(proxyConfig.build())` | `httpClientBuilder.proxyConfiguration(proxyConfig.build())` |

### 1.3.3. 客户端覆盖配置

| 设置 | 1.x | 2.0 |
|------------------------------|---------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `ClientOverrideConfiguration.Builder overrideConfig =`<br />`ClientOverrideConfiguration.builder()` |
| User Agent 前缀 | `clientConfig.setUserAgentPrefix(...)`<br />`clientConfig.withUserAgentPrefix(...)` | `overrideConfig.advancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX, ...)` |
| User Agent 后缀 | `clientConfig.setUserAgentSuffix(...)`<br />`clientConfig.withUserAgentSuffix(...)` | `overrideConfig.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX, ...)` |
| 签名器 | `clientConfig.setSignerOverride(...)`<br />`clientConfig.withSignerOverride(...)` | `overrideConfig.advancedOption(SdkAdvancedClientOption.SIGNER, ...)` |
| 附加请求头 | `clientConfig.addHeader(...)`<br />`clientConfig.withHeader(...)` | `overrideConfig.putHeader(...)` |
| 请求超时 | `clientConfig.setRequestTimeout(...)`<br />`clientConfig.withRequestTimeout(...)` | `overrideConfig.apiCallAttemptTimeout(...)` |
| 客户端执行超时 | `clientConfig.setClientExecutionTimeout(...)`<br />`clientConfig.withClientExecutionTimeout(...)` | `overrideConfig.apiCallTimeout(...)` |
| 使用 Gzip | `clientConfig.setUseGzip(...)`<br />`clientConfig.withGzip(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 套接字缓冲区大小提示 | `clientConfig.setSocketBufferSizeHints(...)`<br />`clientConfig.withSocketBufferSizeHints(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 缓存响应元数据 | `clientConfig.setCacheResponseMetadata(...)`<br />`clientConfig.withCacheResponseMetadata(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| 响应元数据缓存大小 | `clientConfig.setResponseMetadataCacheSize(...)`<br />`clientConfig.withResponseMetadataCacheSize(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| DNS 解析器 | `clientConfig.setDnsResolver(...)`<br />`clientConfig.withDnsResolver(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| TCP Keepalive | `clientConfig.setUseTcpKeepAlive(...)`<br />`clientConfig.withTcpKeepAlive(...)` | 该选项现位于 HTTP 客户端配置中：<br />`- ApacheHttpClient.builder().tcpKeepAlive(true)`<br /> `- NettyNioAsyncHttpClient.builder().tcpKeepAlive(true)` |
| 安全随机数 | `clientConfig.setSecureRandom(...)`<br />`clientConfig.withSecureRandom(...)` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.httpClientBuilder(httpClientBuilder)`<br />`.build()` |

### 1.3.4. 客户端覆盖重试配置

在 2.0 中，重试配置完全通过 `ClientOverrideConfiguration` 中的 `RetryPolicy` 控制。

| 设置 | 1.x | 2.0 |
|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `RetryPolicy.Builder retryPolicy =`<br />`RetryPolicy.builder()` |
| 最大错误重试次数 | `clientConfig.setMaxErrorRetry(...)`<br />`clientConfig.withMaxErrorRetry(...)` | `retryPolicy.numRetries(...)` |
| 使用节流重试 | `clientConfig.setUseThrottleRetries(...)`<br />`clientConfig.withUseThrottleRetries(...)` | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/645) |
| 节流前最大连续重试次数 | `clientConfig.setMaxConsecutiveRetriesBeforeThrottling(...)`<br />`clientConfig.withMaxConsecutiveRetriesBeforeThrottling(...)` | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/645) |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `overrideConfig.retryPolicy(retryPolicy.build())` | |

### 1.3.5. 异步配置

1. 传入 `asyncConfig.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, ...)` 的异步执行器须由用户自行关闭。

| 设置 | 1.x | 2.0 |
|----------|------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| | | `ClientAsyncConfiguration.Builder asyncConfig =`<br />`ClientAsyncConfiguration.builder()` |
| 执行器 | `AmazonDynamoDBAsyncClientBuilder.standard()`<br />`.withExecutorFactory(...)`<br />`.build()` | `asyncConfig.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, ...)` |
| | | `AmazonDynamoDBAsyncClientBuilder.standard()`<br />`.withExecutorFactory(...)`<br />`.build()` | `DynamoDbAsyncClient.builder()`<br />`.asyncConfiguration(asyncConfig.build())`<br />`.build()` |

### 1.3.6. 其他选项

下列来自 1.x 的 `ClientConfiguration` 选项在 2.0 中已变更，且无直接等价项。

| 设置 | 1.x | 2.0 等价方式 |
|----------|-------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 协议 | `clientConfig.setProtocol(Protocol.HTTP)`<br />`clientConfig.withProtocol(Protocol.HTTP)` | 协议现默认为 HTTPS，仅可通过在客户端构建器上设置 HTTP 终端节点修改：`clientBuilder.endpointOverride(URI.create("http://..."))` |

# 2. 操作、请求与响应的变更

诸如 `DynamoDbClient` 的 `PutItemRequest` 等请求会传给客户端操作（例如 `DynamoDbClient.putItem`）。这些操作返回来自 AWS 服务的响应，例如 `PutItemResponse`。

在 2.0 中，操作发生如下变更：

1. 具有多页响应的操作现提供 `Paginator` 方法，用于自动遍历响应中的所有条目。
2. 请求与响应不再可变。
3. 请求与响应不再能通过默认构造函数创建，必须使用静态方法 `builder`：`new PutItemRequest().withTableName(...)` 现为 `PutItemRequest.builder().tableName(...).build()`。
4. 操作与请求支持创建请求的简写方式：`dynamoDbClient.putItem(request -> request.tableName(...))`。

## 2.1. 流式操作

诸如 `S3Client` 的 `getObject` 与 `putObject` 等流式操作接受或返回字节流，而无需将整个负载载入内存。

1. 流式操作的请求对象不再包含负载。
2. 同步流式请求方法现接受 `RequestBody` 作为请求负载，以简化常见加载逻辑，例如 `RequestBody.fromFile(...)`。
3. 异步流式请求方法现接受 `AsyncRequestBody` 作为请求负载，例如 `AsyncRequestBody.fromFile(...)`。
4. 同步流式响应方法现通过 `ResponseTransformer` 指定响应处理，例如 `ResponseTransformer.toFile(...)`。
5. 异步流式响应方法现通过 `AsyncResponseTransformer` 指定响应处理，例如 `AsyncResponseTransformer.toFile(...)`。
6. 流式响应操作现提供 `AsBytes` 方法，可将响应载入内存并简化常见的内存内类型转换。

# 3. 异常变更

在 2.0 中，与异常相关的变更如下：

1. `com.amazonaws.SdkBaseException` 与 `com.amazonaws.AmazonClientException` 的变更：
    1. 二者已合并，并由 `software.amazon.awssdk.core.exception.SdkException` 替代。
    2. `AmazonClientException.isRetryable` 现为 `SdkException.retryable`。
2. `com.amazonaws.SdkClientException` 的变更：
    1. 该类已由 `software.amazon.awssdk.core.exception.SdkClientException` 替代。
    2. 该类现继承 `software.amazon.awssdk.core.exception.SdkException`。
3. `com.amazonaws.AmazonServiceException` 的变更：
    1. 该类已由 `software.amazon.awssdk.awscore.exception.AwsServiceException` 替代。
    2. 该类现继承 `software.amazon.awssdk.core.exception.SdkServiceException`（新的异常类型，继承自 `software.amazon.awssdk.core.exception.SdkException`）。
    3. `AmazonServiceException.getRequestId` 现为 `SdkServiceException.requestId`。
    4. `AmazonServiceException.getServiceName` 现为 `AwsServiceException.awsErrorDetails().serviceName`。
    5. `AmazonServiceException.getErrorCode` 现为 `AwsServiceException.awsErrorDetails().errorCode`。
    6. `AmazonServiceException.getErrorMessage` 现为 `AwsServiceException.awsErrorDetails().errorMessage`。
    7. `AmazonServiceException.getStatusCode` 现为 `AwsServiceException.awsErrorDetails().sdkHttpResponse().statusCode`。
    8. `AmazonServiceException.getHttpHeaders` 现为 `AwsServiceException.awsErrorDetails().sdkHttpResponse().headers`。
    9. `AmazonServiceException.rawResponse` 现为 `AwsServiceException.awsErrorDetails().rawResponse`。
    10. `AmazonServiceException.getErrorType` 不再支持。

# 4. 服务相关变更

## 4.1. S3 变更

2.0 中的 S3 客户端与 1.11 差异很大，因为现与其他服务一样由模型生成。

1. 不再支持跨区域访问。客户端仅能访问与其配置区域一致的存储桶。
2. 默认禁用匿名访问，须通过 `AnonymousCredentialsProvider` 启用。

### 4.1.1. S3 操作迁移

| 1.x 操作 | 2.0 操作 |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `abortMultipartUpload`                 | `abortMultipartUpload`                                                                                                                                                                        |
| `changeObjectStorageClass`             | `copyObject`                                                                                                                                                                                  |
| `completeMultipartUpload`              | `completeMultipartUpload`                                                                                                                                                                     |
| `copyObject`                           | `copyObject`                                                                                                                                                                                  |
| `copyPart`                             | `uploadPartCopy`                                                                                                                                                                              |
| `createBucket`                         | `createBucket`                                                                                                                                                                                |
| `deleteBucket`                         | `deleteBucket`                                                                                                                                                                                |
| `deleteBucketAnalyticsConfiguration`   | `deleteBucketAnalyticsConfiguration`                                                                                                                                                          |
| `deleteBucketCrossOriginConfiguration` | `deleteBucketCors`                                                                                                                                                                            |
| `deleteBucketEncryption`               | `deleteBucketEncryption`                                                                                                                                                                      |
| `deleteBucketInventoryConfiguration`   | `deleteBucketInventoryConfiguration`                                                                                                                                                          |
| `deleteBucketLifecycleConfiguration`   | `deleteBucketLifecycle`                                                                                                                                                                       |
| `deleteBucketMetricsConfiguration`     | `deleteBucketMetricsConfiguration`                                                                                                                                                            |
| `deleteBucketPolicy`                   | `deleteBucketPolicy`                                                                                                                                                                          |
| `deleteBucketReplicationConfiguration` | `deleteBucketReplication`                                                                                                                                                                     |
| `deleteBucketTaggingConfiguration`     | `deleteBucketTagging`                                                                                                                                                                         |
| `deleteBucketWebsiteConfiguration`     | `deleteBucketWebsite`                                                                                                                                                                         |
| `deleteObject`                         | `deleteObject`                                                                                                                                                                                |
| `deleteObjectTagging`                  | `deleteObjectTagging`                                                                                                                                                                         |
| `deleteObjects`                        | `deleteObjects`                                                                                                                                                                               |
| `deleteVersion`                        | `deleteObject`                                                                                                                                                                                |
| `disableRequesterPays`                 | `putBucketRequestPayment`                                                                                                                                                                     |
| `doesBucketExist`                      | `headBucket`                                                                                                                                                                                  |
| `doesBucketExistV2`                    | `headBucket`                                                                                                                                                                                  |
| `doesObjectExist`                      | `headObject`                                                                                                                                                                                  |
| `enableRequesterPays`                  | `putBucketRequestPayment`                                                                                                                                                                     |
| `generatePresignedUrl`                 | [S3Presigner](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html)                                                                                       |
| `getBucketAccelerateConfiguration`     | `getBucketAccelerateConfiguration`                                                                                                                                                            |
| `getBucketAcl`                         | `getBucketAcl`                                                                                                                                                                                |
| `getBucketAnalyticsConfiguration`      | `getBucketAnalyticsConfiguration`                                                                                                                                                             |
| `getBucketCrossOriginConfiguration`    | `getBucketCors`                                                                                                                                                                               |
| `getBucketEncryption`                  | `getBucketEncryption`                                                                                                                                                                         |
| `getBucketInventoryConfiguration`      | `getBucketInventoryConfiguration`                                                                                                                                                             |
| `getBucketLifecycleConfiguration`      | `getBucketLifecycle` or `getBucketLifecycleConfiguration`                                                                                                                                     |
| `getBucketLocation`                    | `getBucketLocation`                                                                                                                                                                           |
| `getBucketLoggingConfiguration`        | `getBucketLogging`                                                                                                                                                                            |
| `getBucketMetricsConfiguration`        | `getBucketMetricsConfiguration`                                                                                                                                                               |
| `getBucketNotificationConfiguration`   | `getBucketNotification` or `getBucketNotificationConfiguration`                                                                                                                               |
| `getBucketPolicy`                      | `getBucketPolicy`                                                                                                                                                                             |
| `getBucketReplicationConfiguration`    | `getBucketReplication`                                                                                                                                                                        |
| `getBucketTaggingConfiguration`        | `getBucketTagging`                                                                                                                                                                            |
| `getBucketVersioningConfiguration`     | `getBucketVersioning`                                                                                                                                                                         |
| `getBucketWebsiteConfiguration`        | `getBucketWebsite`                                                                                                                                                                            |
| `getObject`                            | `getObject`                                                                                                                                                                                   |
| `getObjectAcl`                         | `getObjectAcl`                                                                                                                                                                                |
| `getObjectAsString`                    | `getObjectAsBytes().asUtf8String`                                                                                                                                                             |
| `getObjectMetadata`                    | `headObject`                                                                                                                                                                                  |
| `getObjectTagging`                     | `getObjectTagging`                                                                                                                                                                            |
| `getResourceUrl`                       | [S3Utilities#getUrl](https://github.com/aws/aws-sdk-java-v2/blob/7428f629753c603f96dd700ca686a7b169fc4cd4/services/s3/src/main/java/software/amazon/awssdk/services/s3/S3Utilities.java#L140) |
| `getS3AccountOwner`                    | `listBuckets`                                                                                                                                                                                 |
| `getUrl`                               | [S3Utilities#getUrl](https://github.com/aws/aws-sdk-java-v2/blob/7428f629753c603f96dd700ca686a7b169fc4cd4/services/s3/src/main/java/software/amazon/awssdk/services/s3/S3Utilities.java#L140) |
| `headBucket`                           | `headBucket`                                                                                                                                                                                  |
| `initiateMultipartUpload`              | `createMultipartUpload`                                                                                                                                                                       |
| `isRequesterPaysEnabled`               | `getBucketRequestPayment`                                                                                                                                                                     |
| `listBucketAnalyticsConfigurations`    | `listBucketAnalyticsConfigurations`                                                                                                                                                           |
| `listBucketInventoryConfigurations`    | `listBucketInventoryConfigurations`                                                                                                                                                           |
| `listBucketMetricsConfigurations`      | `listBucketMetricsConfigurations`                                                                                                                                                             |
| `listBuckets`                          | `listBuckets`                                                                                                                                                                                 |
| `listMultipartUploads`                 | `listMultipartUploads`                                                                                                                                                                        |
| `listNextBatchOfObjects`               | `listObjectsV2Paginator`                                                                                                                                                                      |
| `listNextBatchOfVersions`              | `listObjectVersionsPaginator`                                                                                                                                                                 |
| `listObjects`                          | `listObjects`                                                                                                                                                                                 |
| `listObjectsV2`                        | `listObjectsV2`                                                                                                                                                                               |
| `listParts`                            | `listParts`                                                                                                                                                                                   |
| `listVersions`                         | `listObjectVersions`                                                                                                                                                                          |
| `putObject`                            | `putObject`                                                                                                                                                                                   |
| `restoreObject`                        | `restoreObject`                                                                                                                                                                               |
| `restoreObjectV2`                      | `restoreObject`                                                                                                                                                                               |
| `selectObjectContent`                  | `selectObjectContent`                                                                                                                                                                         |
| `setBucketAccelerateConfiguration`     | `putBucketAccelerateConfiguration`                                                                                                                                                            |
| `setBucketAcl`                         | `putBucketAcl`                                                                                                                                                                                |
| `setBucketAnalyticsConfiguration`      | `putBucketAnalyticsConfiguration`                                                                                                                                                             |
| `setBucketCrossOriginConfiguration`    | `putBucketCors`                                                                                                                                                                               |
| `setBucketEncryption`                  | `putBucketEncryption`                                                                                                                                                                         |
| `setBucketInventoryConfiguration`      | `putBucketInventoryConfiguration`                                                                                                                                                             |
| `setBucketLifecycleConfiguration`      | `putBucketLifecycle` or `putBucketLifecycleConfiguration`                                                                                                                                     |
| `setBucketLoggingConfiguration`        | `putBucketLogging`                                                                                                                                                                            |
| `setBucketMetricsConfiguration`        | `putBucketMetricsConfiguration`                                                                                                                                                               |
| `setBucketNotificationConfiguration`   | `putBucketNotification` or `putBucketNotificationConfiguration`                                                                                                                               |
| `setBucketPolicy`                      | `putBucketPolicy`                                                                                                                                                                             |
| `setBucketReplicationConfiguration`    | `putBucketReplication`                                                                                                                                                                        |
| `setBucketTaggingConfiguration`        | `putBucketTagging`                                                                                                                                                                            |
| `setBucketVersioningConfiguration`     | `putBucketVersioning`                                                                                                                                                                         |
| `setBucketWebsiteConfiguration`        | `putBucketWebsite`                                                                                                                                                                            |
| `setObjectAcl`                         | `putObjectAcl`                                                                                                                                                                                |
| `setObjectRedirectLocation`            | `copyObject`                                                                                                                                                                                  |
| `setObjectTagging`                     | `putObjectTagging`                                                                                                                                                                            |
| `uploadPart`                           | `uploadPart`                                                                                                                                                                                  |

## 4.2. SNS 变更

1. SNS 客户端不再能访问与客户端所配置区域不同的 SNS 主题。

## 4.3. SQS 变更

1. SQS 客户端不再能访问与客户端所配置区域不同的 SQS 队列。

## 4.4. RDS 变更

1. 类 `RdsIamAuthTokenGenerator` 已由 `RdsUtilities#generateAuthenticationToken` 替代。

# 5. 配置文件（Profile）变更

对 `~/.aws/config` 与 `~/.aws/credentials` 的解析已变更，以更贴近 AWS CLI 所采用的行为。

1. 路径以 `~/` 或 `~` 开头且后跟文件系统默认路径分隔符时，会按顺序解析为：`$HOME`、`$USERPROFILE`（仅 Windows）、`$HOMEDRIVE$HOMEPATH`（仅 Windows），然后是 `user.home` 系统属性。
2. 环境变量 `AWS_CREDENTIAL_PROFILES_FILE` 现为 `AWS_SHARED_CREDENTIALS_FILE`。
3. 配置文件中不带 `profile` 前缀的 profile 定义会被静默丢弃。
4. 不由字母数字、下划线或短横线组成的 profile 名称会被静默丢弃（对配置文件会先去掉 `profile` 前缀再判断）。
5. 同一文件内重复的 profile 会合并其属性。
6. 在配置与凭证文件中均出现的重复 profile 会合并其属性。
7. 若同一文件中同时存在 `[profile foo]` 与 `[foo]`，二者的属性**不会**合并。
8. 若在配置文件中同时存在 `[profile foo]` 与 `[foo]`，将使用 `[profile foo]` 的属性。
9. 同一文件、同一 profile 内重复的属性以文件中较后者为准。
10. 注释可使用 `;` 与 `#`。
11. 在 profile 定义中，即使紧邻右方括号，`;` 与 `#` 也视为注释起始。
12. 在属性值中，仅当 `;` 与 `#` 前有空白时，才视为注释。
13. 在属性值中，若 `;` 与 `#` 前没有空白，则 `;`、`#` 及其后的全部内容均包含在属性值中。
14. 基于角色的凭证优先级最高；若用户指定了 `role_arn` 属性，则始终使用角色凭证。
15. 基于会话的凭证次之；若未使用角色凭证且用户指定了 `aws_access_key_id` 与 `aws_session_token`，则始终使用会话凭证。
16. 若未使用角色与会话凭证且用户指定了 `aws_access_key_id`，则使用基本凭证。

# 6. 对照表

## 6.1. 环境变量与系统属性

| 1.x 环境变量 | 1.x 系统属性 | 2.0 环境变量 | 2.0 系统属性 |
|-----------------------------------------------|----------------------------------------------------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| `AWS_ACCESS_KEY_ID`<br />`AWS_ACCESS_KEY`     | `aws.accessKeyId`                                        | `AWS_ACCESS_KEY_ID`                                                                  | `aws.accessKeyId`                                                                    |
| `AWS_SECRET_KEY`<br />`AWS_SECRET_ACCESS_KEY` | `aws.secretKey`                                          | `AWS_SECRET_ACCESS_KEY`                                                              | `aws.secretAccessKey`                                                                |
| `AWS_SESSION_TOKEN`                           | `aws.sessionToken`                                       | `AWS_SESSION_TOKEN`                                                                  | `aws.sessionToken`                                                                   |
| `AWS_REGION`                                  | `aws.region`                                             | `AWS_REGION`                                                                         | `aws.region`                                                                         |
| `AWS_CONFIG_FILE`                             |                                                          | `AWS_CONFIG_FILE`                                                                    | `aws.configFile`                                                                     |
| `AWS_CREDENTIAL_PROFILES_FILE`                |                                                          | `AWS_SHARED_CREDENTIALS_FILE`                                                        | `aws.sharedCredentialsFile`                                                          |
| `AWS_PROFILE`                                 | `aws.profile`                                            | `AWS_PROFILE`                                                                        | `aws.profile`                                                                        |
| `AWS_EC2_METADATA_DISABLED`                   | `com.amazonaws.sdk.disableEc2Metadata`                   | `AWS_EC2_METADATA_DISABLED`                                                          | `aws.disableEc2Metadata`                                                             |
|                                               | `com.amazonaws.sdk.ec2MetadataServiceEndpointOverride`   | `AWS_EC2_METADATA_SERVICE_ENDPOINT`                                                  | `aws.ec2MetadataServiceEndpoint`                                                     |
| `AWS_CONTAINER_CREDENTIALS_RELATIVE_URI`      |                                                          | `AWS_CONTAINER_CREDENTIALS_RELATIVE_URI`                                             | `aws.containerCredentialsPath`                                                       |
| `AWS_CONTAINER_CREDENTIALS_FULL_URI`          |                                                          | `AWS_CONTAINER_CREDENTIALS_FULL_URI`                                                 | `aws.containerCredentialsFullUri`                                                    |
| `AWS_CONTAINER_AUTHORIZATION_TOKEN`           |                                                          | `AWS_CONTAINER_AUTHORIZATION_TOKEN`                                                  | `aws.containerAuthorizationToken`                                                    |
| `AWS_CBOR_DISABLED`                           | `com.amazonaws.sdk.disableCbor`                          | `CBOR_ENABLED`                                                                       | `aws.cborEnabled`                                                                    |
| `AWS_ION_BINARY_DISABLE`                      | `com.amazonaws.sdk.disableIonBinary`                     | `BINARY_ION_ENABLED`                                                                 | `aws.binaryIonEnabled`                                                               |
| `AWS_EXECUTION_ENV`                           |                                                          | `AWS_EXECUTION_ENV`                                                                  | `aws.executionEnvironment`                                                           |
|                                               | `com.amazonaws.sdk.disableCertChecking`                  | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
|                                               | `com.amazonaws.sdk.enableDefaultMetrics`                 | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/23)                    | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/23)                    |
|                                               | `com.amazonaws.sdk.enableThrottledRetry`                 | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/645)                   | [不支持](https://github.com/aws/aws-sdk-java-v2/issues/645)                   |
|                                               | `com.amazonaws.regions.RegionUtils.fileOverride`         | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
|                                               | `com.amazonaws.regions.RegionUtils.disableRemote`        | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
|                                               | `com.amazonaws.services.s3.disableImplicitGlobalClients` | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
|                                               | `com.amazonaws.sdk.enableInRegionOptimizedMode`          | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |

## 6.2. 凭证提供程序

| 1.x 凭证提供程序 | 2.0 凭证提供程序 |
|----------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `com.amazonaws.auth.AWSCredentialsProvider`                          | `software.amazon.awssdk.auth.credentials.AwsCredentialsProvider`                                                                                        |
| `com.amazonaws.auth.DefaultAWSCredentialsProviderChain`              | `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider`                                                                                    |
| `com.amazonaws.auth.AWSStaticCredentialsProvider`                    | `software.amazon.awssdk.auth.credentials.StaticCredentialsProvider`                                                                                     |
| `com.amazonaws.auth.EnvironmentVariableCredentialsProvider`          | `software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider`                                                                        |
| `com.amazonaws.auth.SystemPropertiesCredentialsProvider`             | `software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider`                                                                             |
| `com.amazonaws.auth.profile.ProfileCredentialsProvider`              | `software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider`                                                                                    |
| `com.amazonaws.auth.ContainerCredentialsProvider`                    | `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider`                                                                                  |
| `com.amazonaws.auth.InstanceProfileCredentialsProvider`              | `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider`                                                                            |
| `com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider`         | `software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider`                                                                             |
| `com.amazonaws.auth.STSSessionCredentialsProvider`                   | `software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider`                                                                        |
| `com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider` | `software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider`                                                              |
| `com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper`          | `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider` and `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider` |
| `com.amazonaws.services.s3.S3CredentialsProviderChain`               | `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider` and `software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider`         |
| `com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider`      | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new))                                                                    |
| `com.amazonaws.auth.PropertiesFileCredentialsProvider`               | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new))                                                                    |

## 6.3. 客户端名称

| 1.x 客户端 | 2.0 客户端 |
|----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `amazonaws.services.acmpca.AWSACMPCAAsyncClient`                                                   | `software.amazon.awssdk.services.acm.AcmAsyncClient`                                                         |
| `amazonaws.services.acmpca.AWSACMPCAClient`                                                        | `software.amazon.awssdk.services.acm.AcmClient`                                                              |
| `amazonaws.services.alexaforbusiness.AmazonAlexaForBusinessAsyncClient`                            | `software.amazon.awssdk.services.alexaforbusiness.AlexaForBusinessAsyncClient`                               |
| `amazonaws.services.alexaforbusiness.AmazonAlexaForBusinessClient`                                 | `software.amazon.awssdk.services.alexaforbusiness.AlexaForBusinessClient`                                    |
| `amazonaws.services.apigateway.AmazonApiGatewayAsyncClient`                                        | `software.amazon.awssdk.services.apigateway.ApiGatewayAsyncClient`                                           |
| `amazonaws.services.apigateway.AmazonApiGatewayClient`                                             | `software.amazon.awssdk.services.apigateway.ApiGatewayClient`                                                |
| `amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingAsyncClient`                   | `software.amazon.awssdk.services.applicationautoscaling.ApplicationAutoScalingAsyncClient`                   |
| `amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClient`                        | `software.amazon.awssdk.services.applicationautoscaling.ApplicationAutoScalingClient`                        |
| `amazonaws.services.applicationdiscovery.AWSApplicationDiscoveryAsyncClient`                       | `software.amazon.awssdk.services.applicationdiscovery.ApplicationDiscoveryAsyncClient`                       |
| `amazonaws.services.applicationdiscovery.AWSApplicationDiscoveryClient`                            | `software.amazon.awssdk.services.applicationdiscovery.ApplicationDiscoveryClient`                            |
| `amazonaws.services.appstream.AmazonAppStreamAsyncClient`                                          | `software.amazon.awssdk.services.appstream.AppStreamAsyncClient`                                             |
| `amazonaws.services.appstream.AmazonAppStreamClient`                                               | `software.amazon.awssdk.services.appstream.AppStreamClient`                                                  |
| `amazonaws.services.appsync.AWSAppSyncAsyncClient`                                                 | `software.amazon.awssdk.services.appsync.AppSyncAsyncClient`                                                 |
| `amazonaws.services.appsync.AWSAppSyncClient`                                                      | `software.amazon.awssdk.services.appsync.AppSyncClient`                                                      |
| `amazonaws.services.athena.AmazonAthenaAsyncClient`                                                | `software.amazon.awssdk.services.athena.AthenaAsyncClient`                                                   |
| `amazonaws.services.athena.AmazonAthenaClient`                                                     | `software.amazon.awssdk.services.athena.AthenaClient`                                                        |
| `amazonaws.services.autoscaling.AmazonAutoScalingAsyncClient`                                      | `software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient`                                         |
| `amazonaws.services.autoscaling.AmazonAutoScalingClient`                                           | `software.amazon.awssdk.services.autoscaling.AutoScalingClient`                                              |
| `amazonaws.services.autoscalingplans.AWSAutoScalingPlansAsyncClient`                               | `software.amazon.awssdk.services.autoscalingplans.AutoScalingPlansAsyncClient`                               |
| `amazonaws.services.autoscalingplans.AWSAutoScalingPlansClient`                                    | `software.amazon.awssdk.services.autoscalingplans.AutoScalingPlansClient`                                    |
| `amazonaws.services.batch.AWSBatchAsyncClient`                                                     | `software.amazon.awssdk.services.batch.BatchAsyncClient`                                                     |
| `amazonaws.services.batch.AWSBatchClient`                                                          | `software.amazon.awssdk.services.batch.BatchClient`                                                          |
| `amazonaws.services.budgets.AWSBudgetsAsyncClient`                                                 | `software.amazon.awssdk.services.budgets.BudgetsAsyncClient`                                                 |
| `amazonaws.services.budgets.AWSBudgetsClient`                                                      | `software.amazon.awssdk.services.budgets.BudgetsClient`                                                      |
| `amazonaws.services.certificatemanager.AWSCertificateManagerAsyncClient`                           | `software.amazon.awssdk.services.acm.AcmAsyncClient`                                                         |
| `amazonaws.services.certificatemanager.AWSCertificateManagerClient`                                | `software.amazon.awssdk.services.acm.AcmClient`                                                              |
| `amazonaws.services.cloud9.AWSCloud9AsyncClient`                                                   | `software.amazon.awssdk.services.cloud9.Cloud9AsyncClient`                                                   |
| `amazonaws.services.cloud9.AWSCloud9Client`                                                        | `software.amazon.awssdk.services.cloud9.Cloud9Client`                                                        |
| `amazonaws.services.clouddirectory.AmazonCloudDirectoryAsyncClient`                                | `software.amazon.awssdk.services.clouddirectory.CloudDirectoryAsyncClient`                                   |
| `amazonaws.services.clouddirectory.AmazonCloudDirectoryClient`                                     | `software.amazon.awssdk.services.clouddirectory.CloudDirectoryClient`                                        |
| `amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient`                                | `software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient`                                   |
| `amazonaws.services.cloudformation.AmazonCloudFormationClient`                                     | `software.amazon.awssdk.services.cloudformation.CloudFormationClient`                                        |
| `amazonaws.services.cloudfront.AmazonCloudFrontAsyncClient`                                        | `software.amazon.awssdk.services.cloudfront.CloudFrontAsyncClient`                                           |
| `amazonaws.services.cloudfront.AmazonCloudFrontClient`                                             | `software.amazon.awssdk.services.cloudfront.CloudFrontClient`                                                |
| `amazonaws.services.cloudhsm.AWSCloudHSMAsyncClient`                                               | `software.amazon.awssdk.services.cloudhsm.CloudHsmAsyncClient`                                               |
| `amazonaws.services.cloudhsm.AWSCloudHSMClient`                                                    | `software.amazon.awssdk.services.cloudhsm.CloudHsmClient`                                                    |
| `amazonaws.services.cloudhsmv2.AWSCloudHSMV2AsyncClient`                                           | `software.amazon.awssdk.services.cloudhsmv2.CloudHsmV2AsyncClient`                                           |
| `amazonaws.services.cloudhsmv2.AWSCloudHSMV2Client`                                                | `software.amazon.awssdk.services.cloudhsmv2.CloudHsmV2Client`                                                |
| `amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainAsyncClient`                          | `software.amazon.awssdk.services.cloudsearchdomain.CloudSearchDomainAsyncClient`                             |
| `amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient`                               | `software.amazon.awssdk.services.cloudsearchdomain.CloudSearchDomainClient`                                  |
| `amazonaws.services.cloudsearchv2.AmazonCloudSearchAsyncClient`                                    | `software.amazon.awssdk.services.cloudsearch.CloudSearchAsyncClient`                                         |
| `amazonaws.services.cloudsearchv2.AmazonCloudSearchClient`                                         | `software.amazon.awssdk.services.cloudsearch.CloudSearchClient`                                              |
| `amazonaws.services.cloudtrail.AWSCloudTrailAsyncClient`                                           | `software.amazon.awssdk.services.cloudtrail.CloudTrailAsyncClient`                                           |
| `amazonaws.services.cloudtrail.AWSCloudTrailClient`                                                | `software.amazon.awssdk.services.cloudtrail.CloudTrailClient`                                                |
| `amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient`                                        | `software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient`                                           |
| `amazonaws.services.cloudwatch.AmazonCloudWatchClient`                                             | `software.amazon.awssdk.services.cloudwatch.CloudWatchClient`                                                |
| `amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsAsyncClient`                            | `software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsAsyncClient`                               |
| `amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient`                                 | `software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient`                                    |
| `amazonaws.services.codebuild.AWSCodeBuildAsyncClient`                                             | `software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient`                                             |
| `amazonaws.services.codebuild.AWSCodeBuildClient`                                                  | `software.amazon.awssdk.services.codebuild.CodeBuildClient`                                                  |
| `amazonaws.services.codecommit.AWSCodeCommitAsyncClient`                                           | `software.amazon.awssdk.services.codecommit.CodeCommitAsyncClient`                                           |
| `amazonaws.services.codecommit.AWSCodeCommitClient`                                                | `software.amazon.awssdk.services.codecommit.CodeCommitClient`                                                |
| `amazonaws.services.codedeploy.AmazonCodeDeployAsyncClient`                                        | `software.amazon.awssdk.services.codedeploy.CodeDeployAsyncClient`                                           |
| `amazonaws.services.codedeploy.AmazonCodeDeployClient`                                             | `software.amazon.awssdk.services.codedeploy.CodeDeployClient`                                                |
| `amazonaws.services.codepipeline.AWSCodePipelineAsyncClient`                                       | `software.amazon.awssdk.services.codepipeline.CodePipelineAsyncClient`                                       |
| `amazonaws.services.codepipeline.AWSCodePipelineClient`                                            | `software.amazon.awssdk.services.codepipeline.CodePipelineClient`                                            |
| `amazonaws.services.codestar.AWSCodeStarAsyncClient`                                               | `software.amazon.awssdk.services.codestar.CodeStarAsyncClient`                                               |
| `amazonaws.services.codestar.AWSCodeStarClient`                                                    | `software.amazon.awssdk.services.codestar.CodeStarClient`                                                    |
| `amazonaws.services.cognitoidentity.AmazonCognitoIdentityAsyncClient`                              | `software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient`                                 |
| `amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient`                                   | `software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient`                                      |
| `amazonaws.services.cognitoidp.AWSCognitoIdentityProviderAsyncClient`                              | `software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient`                 |
| `amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient`                                   | `software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient`                      |
| `amazonaws.services.cognitosync.AmazonCognitoSyncAsyncClient`                                      | `software.amazon.awssdk.services.cognitosync.CognitoSyncAsyncClient`                                         |
| `amazonaws.services.cognitosync.AmazonCognitoSyncClient`                                           | `software.amazon.awssdk.services.cognitosync.CognitoSyncClient`                                              |
| `amazonaws.services.comprehend.AmazonComprehendAsyncClient`                                        | `software.amazon.awssdk.services.comprehend.ComprehendAsyncClient`                                           |
| `amazonaws.services.comprehend.AmazonComprehendClient`                                             | `software.amazon.awssdk.services.comprehend.ComprehendClient`                                                |
| `amazonaws.services.config.AmazonConfigAsyncClient`                                                | `software.amazon.awssdk.services.config.ConfigAsyncClient`                                                   |
| `amazonaws.services.config.AmazonConfigClient`                                                     | `software.amazon.awssdk.services.config.ConfigClient`                                                        |
| `amazonaws.services.connect.AmazonConnectAsyncClient`                                              | `software.amazon.awssdk.services.connect.ConnectAsyncClient`                                                 |
| `amazonaws.services.connect.AmazonConnectClient`                                                   | `software.amazon.awssdk.services.connect.ConnectClient`                                                      |
| `amazonaws.services.costandusagereport.AWSCostAndUsageReportAsyncClient`                           | `software.amazon.awssdk.services.costandusagereport.CostAndUsageReportAsyncClient`                           |
| `amazonaws.services.costandusagereport.AWSCostAndUsageReportClient`                                | `software.amazon.awssdk.services.costandusagereport.CostAndUsageReportClient`                                |
| `amazonaws.services.costexplorer.AWSCostExplorerAsyncClient`                                       | `software.amazon.awssdk.services.costexplorer.CostExplorerAsyncClient`                                       |
| `amazonaws.services.costexplorer.AWSCostExplorerClient`                                            | `software.amazon.awssdk.services.costexplorer.CostExplorerClient`                                            |
| `amazonaws.services.databasemigrationservice.AWSDatabaseMigrationServiceAsyncClient`               | `software.amazon.awssdk.services.databasemigration.DatabaseMigrationAsyncClient`                             |
| `amazonaws.services.databasemigrationservice.AWSDatabaseMigrationServiceClient`                    | `software.amazon.awssdk.services.databasemigration.DatabaseMigrationClient`                                  |
| `amazonaws.services.datapipeline.DataPipelineAsyncClient`                                          | `software.amazon.awssdk.services.datapipeline.DataPipelineAsyncClient`                                       |
| `amazonaws.services.datapipeline.DataPipelineClient`                                               | `software.amazon.awssdk.services.datapipeline.DataPipelineAsyncClient`                                       |
| `amazonaws.services.dax.AmazonDaxAsyncClient`                                                      | `software.amazon.awssdk.services.dax.DaxAsyncClient`                                                         |
| `amazonaws.services.dax.AmazonDaxClient`                                                           | `software.amazon.awssdk.services.dax.DaxClient`                                                              |
| `amazonaws.services.devicefarm.AWSDeviceFarmAsyncClient`                                           | `software.amazon.awssdk.services.devicefarm.DeviceFarmAsyncClient`                                           |
| `amazonaws.services.devicefarm.AWSDeviceFarmClient`                                                | `software.amazon.awssdk.services.devicefarm.DeviceFarmClient`                                                |
| `amazonaws.services.directconnect.AmazonDirectConnectAsyncClient`                                  | `software.amazon.awssdk.services.directconnect.DirectConnectAsyncClient`                                     |
| `amazonaws.services.directconnect.AmazonDirectConnectClient`                                       | `software.amazon.awssdk.services.directconnect.DirectConnectClient`                                          |
| `amazonaws.services.directory.AWSDirectoryServiceAsyncClient`                                      | `software.amazon.awssdk.services.directory.DirectoryAsyncClient`                                             |
| `amazonaws.services.directory.AWSDirectoryServiceClient`                                           | `software.amazon.awssdk.services.directory.DirectoryClient`                                                  |
| `amazonaws.services.dlm.AmazonDLMAsyncClient`                                                      | `software.amazon.awssdk.services.dlm.DlmAsyncClient`                                                         |
| `amazonaws.services.dlm.AmazonDLMClient`                                                           | `software.amazon.awssdk.services.dlm.DlmClient`                                                              |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient`                                          | `software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient`                                               |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBClient`                                               | `software.amazon.awssdk.services.dynamodb.DynamoDbClient`                                                    |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsAsyncClient`                                   | `software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClient`                                |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClient`                                        | `software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient`                                     |
| `amazonaws.services.ec2.AmazonEC2AsyncClient`                                                      | `software.amazon.awssdk.services.ec2.Ec2AsyncClient`                                                         |
| `amazonaws.services.ec2.AmazonEC2Client`                                                           | `software.amazon.awssdk.services.ec2.Ec2Client`                                                              |
| `amazonaws.services.ecr.AmazonECRAsyncClient`                                                      | `software.amazon.awssdk.services.ecr.EcrAsyncClient`                                                         |
| `amazonaws.services.ecr.AmazonECRClient`                                                           | `software.amazon.awssdk.services.ecr.EcrClient`                                                              |
| `amazonaws.services.ecs.AmazonECSAsyncClient`                                                      | `software.amazon.awssdk.services.ecs.EcsAsyncClient`                                                         |
| `amazonaws.services.ecs.AmazonECSClient`                                                           | `software.amazon.awssdk.services.ecs.EcsClient`                                                              |
| `amazonaws.services.eks.AmazonEKSAsyncClient`                                                      | `software.amazon.awssdk.services.eks.EksAsyncClient`                                                         |
| `amazonaws.services.eks.AmazonEKSClient`                                                           | `software.amazon.awssdk.services.eks.EksClient`                                                              |
| `amazonaws.services.elasticache.AmazonElastiCacheAsyncClient`                                      | `software.amazon.awssdk.services.elasticache.ElastiCacheAsyncClient`                                         |
| `amazonaws.services.elasticache.AmazonElastiCacheClient`                                           | `software.amazon.awssdk.services.elasticache.ElastiCacheClient`                                              |
| `amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkAsyncClient`                               | `software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkAsyncClient`                               |
| `amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient`                                    | `software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient`                                    |
| `amazonaws.services.elasticfilesystem.AmazonElasticFileSystemAsyncClient`                          | `software.amazon.awssdk.services.efs.EfsAsyncClient`                                                         |
| `amazonaws.services.elasticfilesystem.AmazonElasticFileSystemClient`                               | `software.amazon.awssdk.services.efs.EfsClient`                                                              |
| `amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsyncClient`                    | `software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingAsyncClient`                       |
| `amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient`                         | `software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient`                            |
| `amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingAsyncClient`                  | `software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2AsyncClient`                   |
| `amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient`                       | `software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client`                        |
| `amazonaws.services.elasticmapreduce.AmazonElasticMapReduceAsyncClient`                            | `software.amazon.awssdk.services.emr.EmrAsyncClient`                                                         |
| `amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient`                                 | `software.amazon.awssdk.services.emr.EmrClient`                                                              |
| `amazonaws.services.elasticsearch.AWSElasticsearchAsyncClient`                                     | `software.amazon.awssdk.services.elasticsearch.ElasticsearchAsyncClient`                                     |
| `amazonaws.services.elasticsearch.AWSElasticsearchClient`                                          | `software.amazon.awssdk.services.elasticsearch.ElasticsearchClient`                                          |
| `amazonaws.services.elastictranscoder.AmazonElasticTranscoderAsyncClient`                          | `software.amazon.awssdk.services.elastictranscoder.ElasticTranscoderAsyncClient`                             |
| `amazonaws.services.elastictranscoder.AmazonElasticTranscoderClient`                               | `software.amazon.awssdk.services.elastictranscoder.ElasticTranscoderClient`                                  |
| `amazonaws.services.fms.AWSFMSAsyncClient`                                                         | `software.amazon.awssdk.services.fms.FmsAsyncClient`                                                         |
| `amazonaws.services.fms.AWSFMSClient`                                                              | `software.amazon.awssdk.services.fms.FmsClient`                                                              |
| `amazonaws.services.gamelift.AmazonGameLiftAsyncClient`                                            | `software.amazon.awssdk.services.gamelift.GameLiftAsyncClient`                                               |
| `amazonaws.services.gamelift.AmazonGameLiftClient`                                                 | `software.amazon.awssdk.services.gamelift.GameLiftClient`                                                    |
| `amazonaws.services.glacier.AmazonGlacierAsyncClient`                                              | `software.amazon.awssdk.services.glacier.GlacierAsyncClient`                                                 |
| `amazonaws.services.glacier.AmazonGlacierClient`                                                   | `software.amazon.awssdk.services.glacier.GlacierClient`                                                      |
| `amazonaws.services.glue.AWSGlueAsyncClient`                                                       | `software.amazon.awssdk.services.glue.GlueAsyncClient`                                                       |
| `amazonaws.services.glue.AWSGlueClient`                                                            | `software.amazon.awssdk.services.glue.GlueClient`                                                            |
| `amazonaws.services.greengrass.AWSGreengrassAsyncClient`                                           | `software.amazon.awssdk.services.greengrass.GreengrassAsyncClient`                                           |
| `amazonaws.services.greengrass.AWSGreengrassClient`                                                | `software.amazon.awssdk.services.greengrass.GreengrassClient`                                                |
| `amazonaws.services.guardduty.AmazonGuardDutyAsyncClient`                                          | `software.amazon.awssdk.services.guardduty.GuardDutyAsyncClient`                                             |
| `amazonaws.services.guardduty.AmazonGuardDutyClient`                                               | `software.amazon.awssdk.services.guardduty.GuardDutyClient`                                                  |
| `amazonaws.services.health.AWSHealthAsyncClient`                                                   | `software.amazon.awssdk.services.health.HealthAsyncClient`                                                   |
| `amazonaws.services.health.AWSHealthClient`                                                        | `software.amazon.awssdk.services.health.HealthClient`                                                        |
| `amazonaws.services.identitymanagement.AmazonIdentityManagementAsyncClient`                        | `software.amazon.awssdk.services.iam.IamAsyncClient`                                                         |
| `amazonaws.services.identitymanagement.AmazonIdentityManagementClient`                             | `software.amazon.awssdk.services.iam.IamClient`                                                              |
| `amazonaws.services.importexport.AmazonImportExportAsyncClient`                                    | `software.amazon.awssdk.services.importexport.ImportExportAsyncClient`                                       |
| `amazonaws.services.importexport.AmazonImportExportClient`                                         | `software.amazon.awssdk.services.importexport.ImportExportClient`                                            |
| `amazonaws.services.inspector.AmazonInspectorAsyncClient`                                          | `software.amazon.awssdk.services.inspector.InspectorAsyncClient`                                             |
| `amazonaws.services.inspector.AmazonInspectorClient`                                               | `software.amazon.awssdk.services.inspector.InspectorClient`                                                  |
| `amazonaws.services.iot.AWSIotAsyncClient`                                                         | `software.amazon.awssdk.services.iot.IotAsyncClient`                                                         |
| `amazonaws.services.iot.AWSIotClient`                                                              | `software.amazon.awssdk.services.iot.IotClient`                                                              |
| `amazonaws.services.iot1clickdevices.AWSIoT1ClickDevicesAsyncClient`                               | `software.amazon.awssdk.services.iot1clickdevices.Iot1ClickDevicesAsyncClient`                               |
| `amazonaws.services.iot1clickdevices.AWSIoT1ClickDevicesClient`                                    | `software.amazon.awssdk.services.iot1clickdevices.Iot1ClickDevicesClient`                                    |
| `amazonaws.services.iot1clickprojects.AWSIoT1ClickProjectsAsyncClient`                             | `software.amazon.awssdk.services.iot1clickprojects.Iot1ClickProjectsAsyncClient`                             |
| `amazonaws.services.iot1clickprojects.AWSIoT1ClickProjectsClient`                                  | `software.amazon.awssdk.services.iot1clickprojects.Iot1ClickProjectsClient`                                  |
| `amazonaws.services.iotanalytics.AWSIoTAnalyticsAsyncClient`                                       | `software.amazon.awssdk.services.iotanalytics.IotAnalyticsAsyncClient`                                       |
| `amazonaws.services.iotanalytics.AWSIoTAnalyticsClient`                                            | `software.amazon.awssdk.services.iotanalytics.IotAnalyticsClient`                                            |
| `amazonaws.services.iotdata.AWSIotDataAsyncClient`                                                 | `software.amazon.awssdk.services.iotdata.IotDataAsyncClient`                                                 |
| `amazonaws.services.iotdata.AWSIotDataClient`                                                      | `software.amazon.awssdk.services.iotdata.IotDataClient`                                                      |
| `amazonaws.services.iotjobsdataplane.AWSIoTJobsDataPlaneAsyncClient`                               | `software.amazon.awssdk.services.iotdataplane.IotDataPlaneAsyncClient`                                       |
| `amazonaws.services.iotjobsdataplane.AWSIoTJobsDataPlaneClient`                                    | `software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient`                                            |
| `amazonaws.services.kinesis.AmazonKinesisAsyncClient`                                              | `software.amazon.awssdk.services.kinesis.KinesisAsyncClient`                                                 |
| `amazonaws.services.kinesis.AmazonKinesisClient`                                                   | `software.amazon.awssdk.services.kinesis.KinesisClient`                                                      |
| `amazonaws.services.kinesisanalytics.AmazonKinesisAnalyticsAsyncClient`                            | `software.amazon.awssdk.services.kinesisanalytics.KinesisAnalyticsAsyncClient`                               |
| `amazonaws.services.kinesisanalytics.AmazonKinesisAnalyticsClient`                                 | `software.amazon.awssdk.services.kinesisanalytics.KinesisAnalyticsClient`                                    |
| `amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsyncClient`                              | `software.amazon.awssdk.services.firehose.FirehoseAsyncClient`                                               |
| `amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient`                                   | `software.amazon.awssdk.services.firehose.FirehoseClient`                                                    |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMediaAsyncClient`                       | `software.amazon.awssdk.services.kinesisvideoarchivedmedia.KinesisVideoArchivedMediaAsyncClient`             |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMediaClient`                            | `software.amazon.awssdk.services.kinesisvideoarchivedmedia.KinesisVideoArchivedMediaClient`                  |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoAsyncClient`                                    | `software.amazon.awssdk.services.kinesisvideo.KinesisVideoAsyncClient`                                       |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoClient`                                         | `software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient`                                            |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoMediaAsyncClient`                               | `software.amazon.awssdk.services.kinesisvideomedia.KinesisVideoMediaAsyncClient`                             |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoMediaClient`                                    | `software.amazon.awssdk.services.kinesisvideomedia.KinesisVideoMediaClient`                                  |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMediaClient`                                 | 不支持 ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new))                         |
| `amazonaws.services.kms.AWSKMSAsyncClient`                                                         | `software.amazon.awssdk.services.kms.KmsAsyncClient`                                                         |
| `amazonaws.services.kms.AWSKMSClient`                                                              | `software.amazon.awssdk.services.kms.KmsClient`                                                              |
| `amazonaws.services.lambda.AWSLambdaAsyncClient`                                                   | `software.amazon.awssdk.services.lambda.LambdaAsyncClient`                                                   |
| `amazonaws.services.lambda.AWSLambdaClient`                                                        | `software.amazon.awssdk.services.lambda.LambdaClient`                                                        |
| `amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingAsyncClient`                            | `software.amazon.awssdk.services.lexmodelbuilding.LexModelBuildingAsyncClient`                               |
| `amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClient`                                 | `software.amazon.awssdk.services.lexmodelbuilding.LexModelBuildingClient`                                    |
| `amazonaws.services.lexruntime.AmazonLexRuntimeAsyncClient`                                        | `software.amazon.awssdk.services.lexruntime.LexRuntimeAsyncClient`                                           |
| `amazonaws.services.lexruntime.AmazonLexRuntimeClient`                                             | `software.amazon.awssdk.services.lexruntime.LexRuntimeClient`                                                |
| `amazonaws.services.lightsail.AmazonLightsailAsyncClient`                                          | `software.amazon.awssdk.services.lightsail.LightsailAsyncClient`                                             |
| `amazonaws.services.lightsail.AmazonLightsailClient`                                               | `software.amazon.awssdk.services.lightsail.LightsailClient`                                                  |
| `amazonaws.services.logs.AWSLogsAsyncClient`                                                       | `software.amazon.awssdk.services.logs.LogsAsyncClient`                                                       |
| `amazonaws.services.logs.AWSLogsClient`                                                            | `software.amazon.awssdk.services.logs.LogsClient`                                                            |
| `amazonaws.services.machinelearning.AmazonMachineLearningAsyncClient`                              | `software.amazon.awssdk.services.machinelearning.MachineLearningAsyncClient`                                 |
| `amazonaws.services.machinelearning.AmazonMachineLearningClient`                                   | `software.amazon.awssdk.services.machinelearning.MachineLearningClient`                                      |
| `amazonaws.services.macie.AmazonMacieAsyncClient`                                                  | `software.amazon.awssdk.services.macie.MacieAsyncClient`                                                     |
| `amazonaws.services.macie.AmazonMacieClient`                                                       | `software.amazon.awssdk.services.macie.MacieClient`                                                          |
| `amazonaws.services.marketplacecommerceanalytics.AWSMarketplaceCommerceAnalyticsAsyncClient`       | `software.amazon.awssdk.services.marketplacecommerceanalytics.MarketplaceCommerceAnalyticsAsyncClient`       |
| `amazonaws.services.marketplacecommerceanalytics.AWSMarketplaceCommerceAnalyticsClient`            | `software.amazon.awssdk.services.marketplacecommerceanalytics.MarketplaceCommerceAnalyticsClient`            |
| `amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlementAsyncClient`                   | `software.amazon.awssdk.services.marketplaceentitlement.MarketplaceEntitlementAsyncClient`                   |
| `amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlementClient`                        | `software.amazon.awssdk.services.marketplaceentitlement.MarketplaceEntitlementClient`                        |
| `amazonaws.services.marketplacemetering.AWSMarketplaceMeteringAsyncClient`                         | `software.amazon.awssdk.services.marketplacemetering.MarketplaceMeteringAsyncClient`                         |
| `amazonaws.services.marketplacemetering.AWSMarketplaceMeteringClient`                              | `software.amazon.awssdk.services.marketplacemetering.MarketplaceMeteringClient`                              |
| `amazonaws.services.mediaconvert.AWSMediaConvertAsyncClient`                                       | `software.amazon.awssdk.services.mediaconvert.MediaConvertAsyncClient`                                       |
| `amazonaws.services.mediaconvert.AWSMediaConvertClient`                                            | `software.amazon.awssdk.services.mediaconvert.MediaConvertClient`                                            |
| `amazonaws.services.medialive.AWSMediaLiveAsyncClient`                                             | `software.amazon.awssdk.services.medialive.MediaLiveAsyncClient`                                             |
| `amazonaws.services.medialive.AWSMediaLiveClient`                                                  | `software.amazon.awssdk.services.medialive.MediaLiveClient`                                                  |
| `amazonaws.services.mediapackage.AWSMediaPackageAsyncClient`                                       | `software.amazon.awssdk.services.mediapackage.MediaPackageAsyncClient`                                       |
| `amazonaws.services.mediapackage.AWSMediaPackageClient`                                            | `software.amazon.awssdk.services.mediapackage.MediaPackageClient`                                            |
| `amazonaws.services.mediastore.AWSMediaStoreAsyncClient`                                           | `software.amazon.awssdk.services.mediastore.MediaStoreAsyncClient`                                           |
| `amazonaws.services.mediastore.AWSMediaStoreClient`                                                | `software.amazon.awssdk.services.mediastore.MediaStoreClient`                                                |
| `amazonaws.services.mediastoredata.AWSMediaStoreDataAsyncClient`                                   | `software.amazon.awssdk.services.mediastoredata.MediaStoreDataAsyncClient`                                   |
| `amazonaws.services.mediastoredata.AWSMediaStoreDataClient`                                        | `software.amazon.awssdk.services.mediastoredata.MediaStoreDataClient`                                        |
| `amazonaws.services.mediatailor.AWSMediaTailorAsyncClient`                                         | `software.amazon.awssdk.services.mediatailor.MediaTailorAsyncClient`                                         |
| `amazonaws.services.mediatailor.AWSMediaTailorClient`                                              | `software.amazon.awssdk.services.mediatailor.MediaTailorClient`                                              |
| `amazonaws.services.migrationhub.AWSMigrationHubAsyncClient`                                       | `software.amazon.awssdk.services.migrationhub.MigrationHubAsyncClient`                                       |
| `amazonaws.services.migrationhub.AWSMigrationHubClient`                                            | `software.amazon.awssdk.services.migrationhub.MigrationHubClient`                                            |
| `amazonaws.services.mobile.AWSMobileAsyncClient`                                                   | `software.amazon.awssdk.services.mobile.MobileAsyncClient`                                                   |
| `amazonaws.services.mobile.AWSMobileClient`                                                        | `software.amazon.awssdk.services.mobile.MobileClient`                                                        |
| `amazonaws.services.mq.AmazonMQAsyncClient`                                                        | `software.amazon.awssdk.services.mq.MqAsyncClient`                                                           |
| `amazonaws.services.mq.AmazonMQClient`                                                             | `software.amazon.awssdk.services.mq.MqClient`                                                                |
| `amazonaws.services.mturk.AmazonMTurkAsyncClient`                                                  | `software.amazon.awssdk.services.mturk.MTurkAsyncClient`                                                     |
| `amazonaws.services.mturk.AmazonMTurkClient`                                                       | `software.amazon.awssdk.services.mturk.MTurkClient`                                                          |
| `amazonaws.services.neptune.AmazonNeptuneAsyncClient`                                              | `software.amazon.awssdk.services.neptune.NeptuneAsyncClient`                                                 |
| `amazonaws.services.neptune.AmazonNeptuneClient`                                                   | `software.amazon.awssdk.services.neptune.NeptuneClient`                                                      |
| `amazonaws.services.opsworks.AWSOpsWorksAsyncClient`                                               | `software.amazon.awssdk.services.opsworks.OpsWorksAsyncClient`                                               |
| `amazonaws.services.opsworks.AWSOpsWorksClient`                                                    | `software.amazon.awssdk.services.opsworks.OpsWorksClient`                                                    |
| `amazonaws.services.opsworkscm.AWSOpsWorksCMAsyncClient`                                           | `software.amazon.awssdk.services.opsworkscm.OpsWorksCmAsyncClient`                                           |
| `amazonaws.services.opsworkscm.AWSOpsWorksCMClient`                                                | `software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient`                                                |
| `amazonaws.services.organizations.AWSOrganizationsAsyncClient`                                     | `software.amazon.awssdk.services.organizations.OrganizationsAsyncClient`                                     |
| `amazonaws.services.organizations.AWSOrganizationsClient`                                          | `software.amazon.awssdk.services.organizations.OrganizationsClient`                                          |
| `amazonaws.services.pi.AWSPIAsyncClient`                                                           | `software.amazon.awssdk.services.pi.PiAsyncClient`                                                           |
| `amazonaws.services.pi.AWSPIClient`                                                                | `software.amazon.awssdk.services.pi.PiClient`                                                                |
| `amazonaws.services.pinpoint.AmazonPinpointAsyncClient`                                            | `software.amazon.awssdk.services.pinpoint.PinpointAsyncClient`                                               |
| `amazonaws.services.pinpoint.AmazonPinpointClient`                                                 | `software.amazon.awssdk.services.pinpoint.PinpointClient`                                                    |
| `amazonaws.services.polly.AmazonPollyAsyncClient`                                                  | `software.amazon.awssdk.services.polly.PollyAsyncClient`                                                     |
| `amazonaws.services.polly.AmazonPollyClient`                                                       | `software.amazon.awssdk.services.polly.PollyClient`                                                          |
| `amazonaws.services.pricing.AWSPricingAsyncClient`                                                 | `software.amazon.awssdk.services.pricing.PricingAsyncClient`                                                 |
| `amazonaws.services.pricing.AWSPricingClient`                                                      | `software.amazon.awssdk.services.pricing.PricingClient`                                                      |
| `amazonaws.services.rds.AmazonRDSAsyncClient`                                                      | `software.amazon.awssdk.services.rds.RdsAsyncClient`                                                         |
| `amazonaws.services.rds.AmazonRDSClient`                                                           | `software.amazon.awssdk.services.rds.RdsClient`                                                              |
| `amazonaws.services.redshift.AmazonRedshiftAsyncClient`                                            | `software.amazon.awssdk.services.redshift.RedshiftAsyncClient`                                               |
| `amazonaws.services.redshift.AmazonRedshiftClient`                                                 | `software.amazon.awssdk.services.redshift.RedshiftClient`                                                    |
| `amazonaws.services.rekognition.AmazonRekognitionAsyncClient`                                      | `software.amazon.awssdk.services.rekognition.RekognitionAsyncClient`                                         |
| `amazonaws.services.rekognition.AmazonRekognitionClient`                                           | `software.amazon.awssdk.services.rekognition.RekognitionClient`                                              |
| `amazonaws.services.resourcegroups.AWSResourceGroupsAsyncClient`                                   | `software.amazon.awssdk.services.resourcegroups.ResourceGroupsAsyncClient`                                   |
| `amazonaws.services.resourcegroups.AWSResourceGroupsClient`                                        | `software.amazon.awssdk.services.resourcegroups.ResourceGroupsClient`                                        |
| `amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIAsyncClient`               | `software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiAsyncClient`               |
| `amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClient`                    | `software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient`                    |
| `amazonaws.services.route53.AmazonRoute53AsyncClient`                                              | `software.amazon.awssdk.services.route53.Route53AsyncClient`                                                 |
| `amazonaws.services.route53.AmazonRoute53Client`                                                   | `software.amazon.awssdk.services.route53.Route53Client`                                                      |
| `amazonaws.services.route53domains.AmazonRoute53DomainsAsyncClient`                                | `software.amazon.awssdk.services.route53domains.Route53DomainsAsyncClient`                                   |
| `amazonaws.services.route53domains.AmazonRoute53DomainsClient`                                     | `software.amazon.awssdk.services.route53domains.Route53DomainsClient`                                        |
| `amazonaws.services.s3.AmazonS3Client`                                                             | `software.amazon.awssdk.services.s3.S3Client`                                                                |
| `amazonaws.services.sagemaker.AmazonSageMakerAsyncClient`                                          | `software.amazon.awssdk.services.sagemaker.SageMakerAsyncClient`                                             |
| `amazonaws.services.sagemaker.AmazonSageMakerClient`                                               | `software.amazon.awssdk.services.sagemaker.SageMakerClient`                                                  |
| `amazonaws.services.sagemakerruntime.AmazonSageMakerRuntimeAsyncClient`                            | `software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeAsyncClient`                               |
| `amazonaws.services.sagemakerruntime.AmazonSageMakerRuntimeClient`                                 | `software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient`                                    |
| `amazonaws.services.secretsmanager.AWSSecretsManagerAsyncClient`                                   | `software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient`                                   |
| `amazonaws.services.secretsmanager.AWSSecretsManagerClient`                                        | `software.amazon.awssdk.services.secretsmanager.SecretsManagerClient`                                        |
| `amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClient`                              | `software.amazon.awssdk.services.sts.StsAsyncClient`                                                         |
| `amazonaws.services.securitytoken.AWSSecurityTokenServiceClient`                                   | `software.amazon.awssdk.services.sts.StsClient`                                                              |
| `amazonaws.services.serverlessapplicationrepository.AWSServerlessApplicationRepositoryAsyncClient` | `software.amazon.awssdk.services.serverlessapplicationrepository.ServerlessApplicationRepositoryAsyncClient` |
| `amazonaws.services.serverlessapplicationrepository.AWSServerlessApplicationRepositoryClient`      | `software.amazon.awssdk.services.serverlessapplicationrepository.ServerlessApplicationRepositoryClient`      |
| `amazonaws.services.servermigration.AWSServerMigrationAsyncClient`                                 | `software.amazon.awssdk.services.sms.SmsAsyncClient`                                                         |
| `amazonaws.services.servermigration.AWSServerMigrationClient`                                      | `software.amazon.awssdk.services.sms.SmsClient`                                                              |
| `amazonaws.services.servicecatalog.AWSServiceCatalogAsyncClient`                                   | `software.amazon.awssdk.services.servicecatalog.ServiceCatalogAsyncClient`                                   |
| `amazonaws.services.servicecatalog.AWSServiceCatalogClient`                                        | `software.amazon.awssdk.services.servicecatalog.ServiceCatalogClient`                                        |
| `amazonaws.services.servicediscovery.AWSServiceDiscoveryAsyncClient`                               | `software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryAsyncClient`                               |
| `amazonaws.services.servicediscovery.AWSServiceDiscoveryClient`                                    | `software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient`                                    |
| `amazonaws.services.shield.AWSShieldAsyncClient`                                                   | `software.amazon.awssdk.services.shield.ShieldAsyncClient`                                                   |
| `amazonaws.services.shield.AWSShieldClient`                                                        | `software.amazon.awssdk.services.shield.ShieldClient`                                                        |
| `amazonaws.services.simpledb.AmazonSimpleDBAsyncClient`                                            | `software.amazon.awssdk.services.simpledb.SimpleDbAsyncClient`                                               |
| `amazonaws.services.simpledb.AmazonSimpleDBClient`                                                 | `software.amazon.awssdk.services.simpledb.SimpleDbClient`                                                    |
| `amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient`                               | `software.amazon.awssdk.services.ses.SesAsyncClient`                                                         |
| `amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient`                                    | `software.amazon.awssdk.services.ses.SesClient`                                                              |
| `amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementAsyncClient`                 | `software.amazon.awssdk.services.ssm.SsmAsyncClient`                                                         |
| `amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient`                      | `software.amazon.awssdk.services.ssm.SsmClient`                                                              |
| `amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient`                                | `software.amazon.awssdk.services.swf.SwfAsyncClient`                                                         |
| `amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient`                                     | `software.amazon.awssdk.services.swf.SwfClient`                                                              |
| `amazonaws.services.snowball.AmazonSnowballAsyncClient`                                            | `software.amazon.awssdk.services.snowball.SnowballAsyncClient`                                               |
| `amazonaws.services.snowball.AmazonSnowballClient`                                                 | `software.amazon.awssdk.services.snowball.SnowballClient`                                                    |
| `amazonaws.services.sns.AmazonSNSAsyncClient`                                                      | `software.amazon.awssdk.services.sns.SnsAsyncClient`                                                         |
| `amazonaws.services.sns.AmazonSNSClient`                                                           | `software.amazon.awssdk.services.sns.SnsClient`                                                              |
| `amazonaws.services.sqs.AmazonSQSAsyncClient`                                                      | `software.amazon.awssdk.services.sqs.SqsAsyncClient`                                                         |
| `amazonaws.services.sqs.AmazonSQSClient`                                                           | `software.amazon.awssdk.services.sqs.SqsClient`                                                              |
| `amazonaws.services.stepfunctions.AWSStepFunctionsAsyncClient`                                     | `software.amazon.awssdk.services.sfn.SfnAsyncClient`                                                         |
| `amazonaws.services.stepfunctions.AWSStepFunctionsClient`                                          | `software.amazon.awssdk.services.sfn.SfnClient`                                                              |
| `amazonaws.services.storagegateway.AWSStorageGatewayAsyncClient`                                   | `software.amazon.awssdk.services.storagegateway.StorageGatewayAsyncClient`                                   |
| `amazonaws.services.storagegateway.AWSStorageGatewayClient`                                        | `software.amazon.awssdk.services.storagegateway.StorageGatewayClient`                                        |
| `amazonaws.services.support.AWSSupportAsyncClient`                                                 | `software.amazon.awssdk.services.support.SupportAsyncClient`                                                 |
| `amazonaws.services.support.AWSSupportClient`                                                      | `software.amazon.awssdk.services.support.SupportClient`                                                      |
| `amazonaws.services.transcribe.AmazonTranscribeAsyncClient`                                        | `software.amazon.awssdk.services.transcribe.TranscribeAsyncClient`                                           |
| `amazonaws.services.transcribe.AmazonTranscribeClient`                                             | `software.amazon.awssdk.services.transcribe.TranscribeClient`                                                |
| `amazonaws.services.translate.AmazonTranslateAsyncClient`                                          | `software.amazon.awssdk.services.translate.TranslateAsyncClient`                                             |
| `amazonaws.services.translate.AmazonTranslateClient`                                               | `software.amazon.awssdk.services.translate.TranslateClient`                                                  |
| `amazonaws.services.waf.AWSWAFAsyncClient`                                                         | `software.amazon.awssdk.services.waf.WafAsyncClient`                                                         |
| `amazonaws.services.waf.AWSWAFClient`                                                              | `software.amazon.awssdk.services.waf.WafClient`                                                              |
| `amazonaws.services.waf.AWSWAFRegionalAsyncClient`                                                 | `software.amazon.awssdk.services.waf.regional.WafRegionalAsyncClient`                                        |
| `amazonaws.services.waf.AWSWAFRegionalClient`                                                      | `software.amazon.awssdk.services.waf.regional.WafRegionalClient`                                             |
| `amazonaws.services.workdocs.AmazonWorkDocsAsyncClient`                                            | `software.amazon.awssdk.services.workdocs.WorkDocsAsyncClient`                                               |
| `amazonaws.services.workdocs.AmazonWorkDocsClient`                                                 | `software.amazon.awssdk.services.workdocs.WorkDocsClient`                                                    |
| `amazonaws.services.workmail.AmazonWorkMailAsyncClient`                                            | `software.amazon.awssdk.services.workmail.WorkMailAsyncClient`                                               |
| `amazonaws.services.workmail.AmazonWorkMailClient`                                                 | `software.amazon.awssdk.services.workmail.WorkMailClient`                                                    |
| `amazonaws.services.workspaces.AmazonWorkspacesAsyncClient`                                        | `software.amazon.awssdk.services.workspaces.WorkSpacesAsyncClient`                                           |
| `amazonaws.services.workspaces.AmazonWorkspacesClient`                                             | `software.amazon.awssdk.services.workspaces.WorkSpacesClient`                                                |
| `amazonaws.services.xray.AWSXRayAsyncClient`                                                       | `software.amazon.awssdk.services.xray.XRayAsyncClient`                                                       |
| `amazonaws.services.xray.AWSXRayClient`                                                            | `software.amazon.awssdk.services.xray.XRayClient`                                                            |

# 7. 高级库

所有高级库将按 2.0 编程模型重新设计，并陆续重新提供。
下列库在 2.0 中已可用：

| 1.x | 2.0 | 起始版本 |
|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| DynamoDBMapper                              | [DynamoDbEnhancedClient](https://github.com/aws/aws-sdk-java-v2/blob/464be97535cc82a4adb2e5b9fdadd9d4ac739ef8/services-custom/dynamodb-enhanced/README.md) | 2.12.0        |
| Waiters                                     | [Waiters](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/waiters.html)                                                                    | 2.15.0        |
| CloudFrontUrlSigner, CloudFrontCookieSigner | [CloudFrontUtilities](https://aws.amazon.com/blogs/developer/amazon-cloudfront-signed-urls-and-cookies-are-now-supported-in-aws-sdk-for-java-2-x/)         | 2.18.33       |
| TransferManager                             | [S3TransferManager](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/transfer-manager.html)                                                 | 2.19.0        |
| IAM Policy Builder                          | [IAM Policy Builder](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/feature-iam-policy-builder.html)                                      | 2.20.126      |
