# 启发式范例（独立 Maven 项目）

与仓库主工程**解耦**：不加入根 `pom.xml` 的 `<modules>`，避免影响官方构建。仅使用 JDK 8+，**不拉取 AWS SDK 依赖**——用最小代码模仿 SDK 里反复出现的结构（Builder、SPI、异步组合），便于你改坏再对照。

## 运行

```bash
cd learning/examples
mvn -q compile exec:java
```

`pom.xml` 已经把 `RunAllDemos` 配成默认入口，不需要额外传 `-Dexec.mainClass=...`。

## 建议运行顺序

1. `BuilderAndImmutableConfigDemo`
2. `HttpClientSpiDemo`
3. `AsyncPipelineMiniDemo`

不要先跑 `RunAllDemos` 再结束。更好的做法是单独看每个 demo 的输出，然后立刻回主仓库找对应锚点。

## 范例一览

| 类 | 模仿什么 | 建议你做的实验 |
|----|----------|----------------|
| `BuilderAndImmutableConfigDemo` | Client/Request 的 Builder + 不可变配置 | build 后修改传入的 `ArrayList`，看内部是否被改 |
| `HttpClientSpiDemo` | `http-client-spi` 思路：换 HTTP 实现 | 新增第三种 `FakeHttpClient`，返回固定 body |
| `AsyncPipelineMiniDemo` | Async 管线中的 `thenCompose` 链 | 在某步 `completeExceptionally`，观察后续是否不执行 |

## 与真 SDK 的对照

读完每个类的类内简短注释后，在仓库中搜索：

- `BuilderAndImmutableConfigDemo` → `core/sdk-core/.../ClientOverrideConfiguration.java` 和对应测试
- `HttpClientSpiDemo` → `http-client-spi/.../SdkHttpClient.java`
- `AsyncPipelineMiniDemo` → `docs/guidelines/async-programming-guidelines.md` 与 `CompletableFutureUtils` 的使用点
