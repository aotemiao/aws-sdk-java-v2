# AWS SDK for Java v2 — 启发式自学材料

本目录是**本地补充**：把仓库里分散的文档与代码，收成一张**地图**、一页**笔记**、一组**可改可跑的小范例**。官方权威内容仍以仓库根目录 `README.md`、`docs/` 与 [开发者指南](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html) 为准。

## 包含什么

| 文件 / 目录 | 用途 |
|-------------|------|
| [LEARNING_MAP.md](./LEARNING_MAP.md) | 学习地图 + 分阶段路线（读什么、顺序、验收问题） |
| [NOTES.md](./NOTES.md) | 一页式速查：核心概念与「和 SDK 的对应关系」 |
| [examples/](./examples/) | 独立 Maven 小项目：自己改代码体会 Builder / SPI / 异步 |

## 建议用法（启发式）

1. 打开 `LEARNING_MAP.md`，选一个**当前阶段**，只做该阶段的「验收问题」。
2. 对照 `NOTES.md` 标出你不熟的 1～2 个词，回到仓库里 **Jump to Definition** 追一次调用链。
3. 在 `examples/` 里运行范例，**故意改坏一处**（例如去掉 `join()`、换实现类），观察行为差异。

## 运行范例

```bash
cd learning/examples
mvn -q compile exec:java -Dexec.mainClass="software.amazon.awssdk.learning.examples.RunAllDemos"
```

（需本机 JDK 8+、能访问 Maven Central。范例中的 AWS 调用为**可选**，默认演示不访问真实 AWS。）
