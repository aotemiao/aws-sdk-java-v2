# AWS SDK for Java v2 — 启发式自学材料

本目录是**本地补充**：把仓库里分散的文档与代码，收成一张**地图**、一页**笔记**、一组**可改可跑的小范例**。官方权威内容仍以仓库根目录 `README.md`、英文 `docs/` 与 [开发者指南](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html) 为准；根目录 `docs` 下 Markdown 的**简体中文镜像**见本目录 [`docs/`](./docs/README.md)。

## 最短入门（推荐先走这条）

1. 先读 `NOTES.md` 的前两节，只记住一件事：`core` 管语义与策略，`http-client-*` 管“怎么发 HTTP”。
2. 按顺序运行 3 个 demo：`BuilderAndImmutableConfigDemo` → `HttpClientSpiDemo` → `AsyncPipelineMiniDemo`。
3. 每跑完一个 demo，就去 `LEARNING_MAP.md` 对照对应的**真仓库锚点**，只看那 1～2 个文件，不要散读。

## 包含什么

| 文件 / 目录 | 用途 |
|-------------|------|
| [LEARNING_MAP.md](./LEARNING_MAP.md) | 学习地图 + 分阶段路线（读什么、顺序、验收问题） |
| [NOTES.md](./NOTES.md) | 一页式速查：核心概念与「和 SDK 的对应关系」 |
| [docs/](./docs/) | 根目录 `docs/` 的简体中文镜像（入口见其中 `README.md`） |
| [examples/](./examples/) | 独立 Maven 小项目：自己改代码体会 Builder / SPI / 异步 |

## 建议用法（启发式）

1. 先按 `LEARNING_MAP.md` 里的「最短入门顺序」跑完第一轮，不要一开始就读完整条扩展路线。
2. 对照 `NOTES.md` 标出你不熟的 1～2 个词，回到仓库里 **Jump to Definition** 追一次调用链。
3. 在 `examples/` 里运行范例，**故意改坏一处**（例如删掉拷贝、换实现类、让 Future 异常完成），观察行为差异。

## 运行范例

```bash
cd learning/examples
mvn -q compile exec:java
```

`pom.xml` 已预设 `RunAllDemos` 为默认入口；这样写在 PowerShell 下也更稳。

（需本机 JDK 8+、能访问 Maven Central。范例中的 AWS 调用为**可选**，默认演示不访问真实 AWS。）
