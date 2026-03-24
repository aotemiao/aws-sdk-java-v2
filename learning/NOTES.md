# 速查笔记（一页式）

> 用途：复习时用。条目尽量短；细节回仓库用 IDE / 原文档。

---

## 1. 心智模型（调用链）

```
你的代码
  → XxxClient（高层 API，常含 Builder）
  → 内部执行管线（拦截器 / 端点 / 签名 / 重试…）
  → SdkHttpClient（SPI：只关心「怎么发 HTTP」）
  → AWS 服务端点
```

**记住一句**：`core` 管「语义与策略」，`http-client-*` 管「怎么发字节」。

---

## 2. 设计词汇与 SDK 对应

| 概念 | 在 SDK 中的痕迹 | 启发（你可用在业务代码里） |
|------|-----------------|----------------------------|
| SPI（服务提供接口） | `http-client-spi` | 核心域依赖抽象，第三方/实现可换 |
| Builder | `*Client.builder()`、各类 `*Request.builder()` | 可选参数多时用 Builder，避免 telescoping constructor |
| 不可变配置 | `ClientConfiguration` 指南 | 配置对象建好后不再变；修改 = 新实例 |
| 防御性拷贝 | 指南里对集合字段的要求 | 对外暴露的集合考虑 `unmodifiable` 或拷贝 |
| 静态工厂 | `FavorStaticFactoryMethods.md` | 表达语义（`of`、`from`）比裸构造更清晰 |
| Optional | `UseOfOptional.md` | 返回值可用；参数 / 字段慎用 |
| 同步 / 异步双轨 | `async` client + 指南 | 异步 API 要约定线程与异常传播（CompletionStage） |

---

## 3. 异步：三条铁律（来自仓库精神，非逐字引用）

1. **不要在回调里阻塞**（尤其可能运行在 IO 线程池上的路径）。
2. **异常要进 CompletableFuture**，不要既吞掉又返回成功。
3. **取消 / 超时**要考虑底层资源是否释放（连接、流）。

---

## 4. 测试分层（速记）

| 层级 | 典型手段 | 防什么 |
|------|----------|--------|
| 单元测试 | Mock SPI、假实现 | 单类逻辑 |
| 协议 / HTTP 测试 | `protocol-tests`、契约类用例 | 序列化、头、URL 结构被改坏 |
| 集成测试 | 真实或 LocalStack（项目外常见） | 端到端行为 |

---

## 5. 阅读代码的「入口快捷键」

- 从 **`XxxClient` 的某个 operation** 往里追，比从 `internal` 包往外看更省力。
- 优先看 **`test/`** 里同名或同包的测试：往往是最短「可运行说明书」。
- **`docs/design/*/DecisionLog.md`**：看「为什么没选 B 方案」比只看最终代码更省脑。

---

## 6. 本仓库 `learning/examples` 迷你对照表

| 范例类 | 对应上面哪条 |
|--------|----------------|
| `BuilderAndImmutableConfigDemo` | Builder、不可变、防御性拷贝 |
| `HttpClientSpiDemo` | SPI、依赖倒置 |
| `AsyncPipelineMiniDemo` | CompletableFuture、组合 |

---

## 7. 延伸阅读（官方）

- [Developer Guide](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html)
- 仓库 `docs/README.md` 中的 Migration / Changelog 链接
