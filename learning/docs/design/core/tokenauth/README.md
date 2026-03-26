**设计：** 新功能，**状态：** [已发布](../../../README.md)

# Bearer Token 授权与 Token 提供者

**什么是 Bearer Token 授权？**

Bearer Token 授权是一种使用 [Bearer Token](https://oauth.net/2/bearer-tokens/)（而非传统 AWS 凭证）对服务请求进行认证的方法。做法是在 Authorization 头中填入 bearer token。

**用户体验是怎样的？**

首先，用户可在共享的 SDK 配置文件中配置一个配置文件（profile），以便通过 AWS CLI 或 AWS IDE 插件完成初次登录。例如该 profile 可能如下：

 ```
 [profile sono]
 sso_start_url = https://sono.aws
 sso_region = us-east-1
 ```

在此配置下，用户可通过 AWS CLI 等支持工具登录：

 ```
 $ aws sso login --profile sono
 ```

登录完成后，`~/.aws/sso/cache` 下会有缓存 token，供 SDK 进行 bearer token 授权。

服务将在服务级别建模为使用 `bearer` 签名版本。该客户端会知道如何根据 profile 配置加载缓存 token，并用其填充 bearer 授权头。这与 SDK 解析 AWS 凭证并执行 SigV4 签名的方式类似。

*示例*

例如，若 token 值为 `"mF_9.B5f-4.1JqM"`，则 `Authorization` 头的值为：`"Bearer mF_9.B5f-4.1JqM"`。完整 HTTP 请求可能如下：

 ```
 GET /resource HTTP/1.1
 Host: server.example.com
 Authorization: Bearer mF_9.B5f-4.1JqM
 ```

## 拟议 API

当 SDK 2.x 客户端遇到建模或配置为使用 `bearer` 签名版本的服务或操作时，将查询 token 提供者链以获取 token，用于生成并附加请求的授权信息。

### Token / Token 提供者及链的 API 设计

尽管 Token/TokenProvider 与 AWS Credentials、AwsCredentialsProvider、AwsCredentialsProviderChain 相似，我们仍需提供新接口，原因如下：
 1. 凭证最终会解析为 accessKeyId 与 secretAccessKey，与 AWS 账户登录相关。
 2. SDK 应支持客户端配置多种凭证类型（例如 token 与 AWS 凭证）。

#### Token 

SDK 将支持包含额外元数据的 token 表示，包括 token 字符串与过期时间字段。
```java
@SdkPublicApi
public interface SdkToken {
    String token();
    Optional<Instant> expirationTime();
}

```

#### Token Provider

为生成 token 对象，SDK 将实现 token 提供者与提供者链支持。token 提供者及链接口将镜像现有 AWS 凭证提供者接口，但返回 Token 对象。

```java
@FunctionalInterface
@SdkPublicApi
public interface SdkTokenProvider {

    SdkToken resolveToken();
}
```
`SsoTokenProvider` 是 `SdkTokenProvider` 的实现，可从 `~/.aws/sso/cache` 加载并存储 SSO token，并可通过 SSO-OIDC 服务刷新缓存 token。


```java
        SsoTokenProvider ssoTokenProvider =  SsoTokenProvider.builder()
                               .startUrl("https://d-abc123.awsapps.com/start")
                               .build();
```

`StaticTokenProvider` 是最简单的 token 提供者实现，仅返回静态 token 字符串，并可选择是否有过期时间。

```java
 StaticTokenProvider provider = StaticTokenProvider.create(bearerToken);
```

#### Token Provider Chains

`SdkTokenProviderChain` 是 `SdkTokenProvider` 的实现，将多个 token 提供者链接起来。该类的公开方法与 [AwsCredentialsProviderChain](https://github.com/aws/aws-sdk-java-v2/blob/master/core/auth/src/main/java/software/amazon/awssdk/auth/credentials/AwsCredentialsProviderChain.java) 相同，但解析的是 Token 提供者。

#### ClientBuilder 中设置 Token Provider 的 API

将在 [AwsClientBuilder](https://github.com/aws/aws-sdk-java-v2/blob/master/core/aws-core/src/main/java/software/amazon/awssdk/awscore/client/builder/AwsClientBuilder.java) 中新增 API，用于为客户端设置 `tokenProvider`。

```java

   SdkTokenProviderChain TOKEN_PROVIDER_CHAIN =  DefaultSdkTokenProvderChain.create(); 
   ServiceClient.builder()
                 .region(REGION)
                 .tokenProvider(TOKEN_PROVIDER_CHAIN)
                 .build();
```


### Bearer Token 签名器

Bearer Token 授权由 `BearerTokenSigner` 完成，其会用从 Token 提供者解析出的 token 更新 Authorization 头。

```java
@SdkPublicApi
public final class BearerTokenSigner implements Signer {

    @Override
    public CredentialType credentialType() {
        return CredentialType.TOKEN;
    }
    
    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
       /**
        * doSign will do following
        * 1. Resolve token from Token Provider
        * 2. Set the Authorization header by filling in the token to the following format string: "Bearer {token}".
        */
        return doSign(request, executionAttributes);
    }
    public static BearerTokenSigner create() {
        return new BearerTokenSigner();
    }
}

```
#### ClientBuilder 中设置 Bearer Token Signer 的 API

将新增 AdvancedOption 设置 `BEARER_SIGNER`，用于添加 bearer 签名器。

```java

   ServiceClient.builder()
                   .region(REGION)
                   .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                                   .putAdvancedOption(BEARER_SIGNER, DefaultBearerTokenSigner.create())
                                                   .build())
                    .build();
```

也可通过现有的 `SIGNER` advancedOption 为 ClientBuilder 配置 Bearer Token Signer。
```java
   serviceClient.builder()
                   .region(REGION)
                   .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                                   .putAdvancedOption(SIGNER, DefaultBearerTokenSigner.create())
                                                   .build())
```
#### OperationRequest Builder 中设置 Bearer Token Signer 的 API
可在请求级别通过覆盖操作请求的配置来设置 Bearer Token Signer。将在 [RequestOverrideConfiguration](https://github.com/aws/aws-sdk-java-v2/blob/master/core/aws-core/src/main/java/software/amazon/awssdk/awscore/AwsRequestOverrideConfiguration.java) 中新增 API `bearerTokenSigner(BearerTokenSigner bearerTokenSigner)`。

```java
   serviceClient.operation(OperationRequest.builder()
                                           .overrideConfiguration(o -> o.bearerTokenSigner(DefaultBearerTokenSigner.create()))
                                           .build());
```

也可通过在 `RequestOverrideConfiguration` 上设置 `signer` 来更新 BearerTokenSigner。

```java
   serviceClient.operation(OperationRequest.builder()
                                        .overrideConfiguration(o -> o.signer(DefaultBearerTokenSigner.create()))
                                        .build());
```
