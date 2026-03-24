# 启发式范例（独立 Maven 项目）

与仓库主工程**解耦**：不加入根 `pom.xml` 的 `<modules>`，避免影响官方构建。仅使用 JDK 8+，**不拉取 AWS SDK 依赖**——用最小代码模仿 SDK 里反复出现的结构（Builder、SPI、异步组合），便于你改坏再对照。

## 运行

```bash
cd learning/examples
mvn -q compile exec:java -Dexec.mainClass="software.amazon.awssdk.learning.examples.RunAllDemos"
```

## 范例一览

| 类 | 模仿什么 | 建议你做的实验 |
|----|----------|----------------|
| `BuilderAndImmutableConfigDemo` | Client/Request 的 Builder + 不可变配置 | build 后修改传入的 `ArrayList`，看内部是否被改 |
| `HttpClientSpiDemo` | `http-client-spi` 思路：换 HTTP 实现 | 新增第三种 `FakeHttpClient`，返回固定 body |
| `AsyncPipelineMiniDemo` | Async 管线中的 `thenCompose` 链 | 在某步 `completeExceptionally`，观察后续是否不执行 |

## 与真 SDK 的对照

读完每个类的类内简短注释后，在仓库中搜索：

- `Builder` → 任意 `*Client.builder()`
- `SdkHttpClient` → `http-client-spi` 包
- `CompletableFuture` → `async` 客户端返回类型
