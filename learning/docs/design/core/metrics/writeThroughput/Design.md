# JAVA-8699：写入吞吐量指标 — 设计文档

## 概述

新增 `WRITE_THROUGHPUT` 指标，用于衡量请求体上传速度（字节/秒），与现有 `READ_THROUGHPUT` 互补。回应客户从 SDK v1 迁移的需求（[GitHub #3704](https://github.com/aws/aws-sdk-java-v2/issues/3704)）。

## 公式

```
WRITE_THROUGHPUT = RequestBytesWritten / (LastReadTime - WriteStartTime)
```

- **WriteStartTime**：从请求体流读取首字节的时间戳
- **LastReadTime**：从请求体流读取末字节的时间戳

## 请求时间线

```
AttemptStart    WriteStart      LastReadTime     TTFB                TTLB
     |              |                |             |                    |
     v              v                v             v                    v
     [-- Setup --][-- Sending Body --][-- Server --][-- Receive Response --]
```

## 实现

### 同步客户端

在 `BytesWrittenTrackingInputStream` 中记录时间戳：

```java
public final class BytesWrittenTrackingInputStream extends SdkFilterInputStream {
    private final AtomicLong bytesWritten;
    private final AtomicLong writeStartTime;
    private final AtomicLong lastReadTime;

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        writeStartTime.compareAndSet(0, System.nanoTime());  // First read only
        int read = super.read(b, off, len);
        if (read > 0) {
            bytesWritten.addAndGet(read);
            lastReadTime.set(System.nanoTime());  // Every read
        }
        return read;
    }
}
```

### 异步客户端

采用类似模式，使用 `BytesWrittenTrackingPublisher` 包装请求体 publisher。

### 指标上报

在 `HandleResponseStage` 中计算并上报：

```java
double writeThroughput = bytesWritten / ((lastReadTime - writeStartTime) / 1_000_000_000.0);
attemptMetricCollector.reportMetric(CoreMetric.WRITE_THROUGHPUT, writeThroughput);
```

## 理由

1. **准确性** — 在快网络（例如与 S3 同区域的 EC2）上，服务端处理时间（约 60–110ms）可占上传总时长相当比例。若以 TTFB 作为写入结束会严重低估吞吐量（最高约 96%）。
2. **开销可忽略** — 性能基准显示记录 `lastReadTime` 对实际上传开销 &lt;0.01%。
3. **一致性** — 不依赖 HTTP 客户端如何读取流。
4. **同步/异步对等** — 两种客户端可采用相同思路。

---

## 附录 A：曾考虑的替代方案

### 替代 1：在 EOF/onComplete 打点
在流结束时刻捕获时间戳（同步：`read()` 返回 `-1`；异步：`onComplete()`）。

**优点：**
- 精确对应流耗尽时刻
- 语义清晰

**缺点：**
- HTTP 客户端在达到 content-length 后未必再调用
- 同步：`read()` 返回 `-1` 无保证
- 异步：`onComplete()` 无保证 — 客户端可能取消订阅

### 替代 2：使用 TTFB
以首字节时间代理写入结束。

**优点：**
- 简单，数据现成
- 无需额外跟踪

**缺点：**
- 包含服务端处理时间
- 在快网络上显著低估吞吐量（见附录 B）

### 替代 3：在 HTTP 客户端层实现
在各 HTTP 客户端（Apache、Netty、CRT）中跟踪实际写入套接字的字节数。

**优点：**
- 最贴近网络写入
- 考虑客户端缓冲

**缺点：**
- 需在多个客户端实现
- 工作量与维护成本高
- 与在 SDK core 跟踪的 `READ_THROUGHPUT` 不一致

---

## 附录 B：准确性对比

使用真实 S3 上传对比替代 2（TTFB）与推荐方案（lastReadTime）：

**本机（较慢网络）：**

| 对象大小 | 替代 2 (TTFB) | 推荐 (lastRead) | 差异 | 服务端处理 |
|-------------|----------------------|------------------------|------|-------------------|
| 10MB | 19.92 MB/s | 24.80 MB/s | 24.5% | 103 ms |
| 100MB | 33.66 MB/s | 34.94 MB/s | 3.8% | 113 ms |

**EC2（快网络，与 S3 同区域）：**

| 对象大小 | 替代 2 (TTFB) | 推荐 (lastRead) | 差异 | 服务端处理 |
|-------------|----------------------|------------------------|------|-------------------|
| 10MB | 34.22 MB/s | 43.09 MB/s | 25.9% | 63 ms |
| 10MB | 75.61 MB/s | 147.87 MB/s | 95.6% | 67 ms |

**结论：** 服务端处理时间相对恒定（约 60–110ms），但在快网络上占上传总时长比例显著。替代 2 可能将吞吐量低估多达约 96%。

---

## 附录 C：性能基准

### 同步客户端：BytesWrittenTrackingInputStream

JMH 对比 `BytesWrittenTrackingInputStream` 实现：

| 数据量 | 缓冲区 | 基线 | 替代 2 | 推荐 | Alt2 - 基线 | Rec - 基线 |
|-----------|--------|----------|---------------|-------------|-----------------|----------------|
| 10MB | 16KB | 420 µs | 468 µs | 480 µs | +48 µs | +60 µs |
| 10MB | 64KB | 460 µs | 470 µs | 457 µs | +10 µs | -3 µs (noise) |
| 10MB | 128KB | 461 µs | 467 µs | 443 µs | +6 µs | -18 µs (noise) |
| 100MB | 16KB | 7,359 µs | 8,127 µs | 8,346 µs | +768 µs | +987 µs |
| 100MB | 64KB | 8,118 µs | 8,057 µs | 8,004 µs | -61 µs (noise) | -114 µs (noise) |
| 100MB | 128KB | 7,925 µs | 7,769 µs | 7,990 µs | -156 µs (noise) | +65 µs (noise) |

- 16KB 缓冲区由 `FileAsyncRequestBody` 使用
- 64KB/128KB 缓冲区由同步 HTTP 客户端使用

**结论：** 推荐方案相对基线增加约 60–987 µs。相对真实网络 I/O（例如 100MB 约 33 MB/s 时约 3 秒），约 **&lt;0.03%**，可忽略。

### 异步客户端：BytesWrittenTrackingPublisher

JMH 对比 `BytesWrittenTrackingPublisher` 开销：

| 数据量 | 缓冲区 | 基线 | 带跟踪 | 开销 |
|-----------|--------|----------|---------------|----------|
| 10MB | 16KB | 1.2 µs | 45.6 µs | +44.4 µs |
| 10MB | 64KB | 0.2 µs | 11.3 µs | +11.1 µs |
| 10MB | 128KB | 0.1 µs | 5.7 µs | +5.6 µs |
| 100MB | 16KB | 13.6 µs | 450.4 µs | +436.8 µs |
| 100MB | 64KB | 1.9 µs | 111.8 µs | +109.9 µs |
| 100MB | 128KB | 0.9 µs | 56.4 µs | +55.5 µs |

**说明：** 异步基线远快于同步，因响应式路径仅传递 `ByteBuffer` 引用而未模拟真实 I/O 读取。绝对开销（100MB 约 450 µs）实际低于同步（约 987 µs）。

**结论：** 100MB 上传约 450 µs 开销，相对真实网络 I/O（约 3 秒 @ 33 MB/s）约 **&lt;0.015%**，可忽略。

### 原始 JMH 输出

**同步（BytesWrittenTrackingInputStream）：**
```
Benchmark                                                    (bufferSize)  (dataSize)  Mode  Cnt     Score     Error  Units
BytesWrittenTrackingBenchmark.baseline                              16384    10485760  avgt    5   420.387 ±   1.665  us/op
BytesWrittenTrackingBenchmark.baseline                              16384   104857600  avgt    5  7359.279 ±  95.049  us/op
BytesWrittenTrackingBenchmark.baseline                              65536    10485760  avgt    5   460.210 ±   6.793  us/op
BytesWrittenTrackingBenchmark.baseline                              65536   104857600  avgt    5  8117.617 ± 984.170  us/op
BytesWrittenTrackingBenchmark.baseline                             131072    10485760  avgt    5   460.559 ±   2.650  us/op
BytesWrittenTrackingBenchmark.baseline                             131072   104857600  avgt    5  7924.954 ± 232.629  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            16384    10485760  avgt    5   467.664 ±   6.503  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            16384   104857600  avgt    5  8127.128 ± 362.653  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            65536    10485760  avgt    5   470.295 ±   1.482  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            65536   104857600  avgt    5  8057.417 ± 368.637  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly           131072    10485760  avgt    5   466.987 ±   2.365  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly           131072   104857600  avgt    5  7768.661 ± 296.360  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         16384    10485760  avgt    5   480.264 ±   7.447  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         16384   104857600  avgt    5  8345.863 ± 356.510  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         65536    10485760  avgt    5   457.162 ±   2.356  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         65536   104857600  avgt    5  8004.015 ± 247.785  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead        131072    10485760  avgt    5   442.916 ±   3.713  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead        131072   104857600  avgt    5  7989.967 ± 437.852  us/op
```

**异步（BytesWrittenTrackingPublisher）：**
```
Benchmark                                            (bufferSize)  (dataSize)  Mode  Cnt    Score    Error  Units
BytesWrittenTrackingPublisherBenchmark.baseline             16384    10485760  avgt   10    1.150 ±  0.003  us/op
BytesWrittenTrackingPublisherBenchmark.baseline             16384   104857600  avgt   10   13.620 ±  0.273  us/op
BytesWrittenTrackingPublisherBenchmark.baseline             65536    10485760  avgt   10    0.186 ±  0.002  us/op
BytesWrittenTrackingPublisherBenchmark.baseline             65536   104857600  avgt   10    1.862 ±  0.018  us/op
BytesWrittenTrackingPublisherBenchmark.baseline            131072    10485760  avgt   10    0.100 ±  0.002  us/op
BytesWrittenTrackingPublisherBenchmark.baseline            131072   104857600  avgt   10    0.928 ±  0.020  us/op
BytesWrittenTrackingPublisherBenchmark.withTracking         16384    10485760  avgt   10   45.559 ±  0.468  us/op
BytesWrittenTrackingPublisherBenchmark.withTracking         16384   104857600  avgt   10  450.381 ± 17.005  us/op
BytesWrittenTrackingPublisherBenchmark.withTracking         65536    10485760  avgt   10   11.341 ±  0.192  us/op
BytesWrittenTrackingPublisherBenchmark.withTracking         65536   104857600  avgt   10  111.815 ±  2.430  us/op
BytesWrittenTrackingPublisherBenchmark.withTracking        131072    10485760  avgt   10    5.677 ±  0.076  us/op
BytesWrittenTrackingPublisherBenchmark.withTracking        131072   104857600  avgt   10   56.413 ±  0.084  us/op
```

---

## 附录 D：SDK v1 做法

SDK v1 使用 `ByteThroughputHelper`，围绕每次 I/O 操作统计时间。写入吞吐量在 Apache HTTP 客户端层的 `MetricInputStreamEntity` 中实现。

**V1：**
- 时间：各次 I/O 调用耗时之和
- 上报：周期性（每累计 10s I/O 时间）
- 范围：跨多个请求聚合
- 实现：HTTP 客户端层（Apache entity）

**V2：**
- 时间：墙钟时间（首次读到末次读）
- 上报：每次 API 调用尝试结束时
- 范围：按请求粒度
- 实现：SDK core 层
