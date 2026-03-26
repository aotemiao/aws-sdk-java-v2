# SDK 指标系统
## 概念
### 指标（Metric）
* 对 SDK 某方面的度量。例如请求延迟、池化连接数、执行的重试次数等。

* 指标关联到类别。部分指标类别为 `Default`、`HttpClient` 与 `Streaming`。这样客户可仅启用感兴趣的类别。

SDK 采集的标准指标完整列表见 [指标列表](./MetricsList.md)。

### 指标收集器（Metric Collector）

* `MetricCollector` 是指标的类型安全聚合器。其他 SDK 组件主要通过 `reportMetric(SdkMetric,Object)` 向其上报所发射的指标。

* `MetricCollector` 支持嵌套，从而可在其他指标事件的上下文中收集指标。例如，单次 API 调用若发生重试，可能有多次请求尝试；每次尝试的指标事件可存放在各自的 `MetricCollector` 中，而这些收集器均为表示整次 API 调用的父收集器的子节点。

  调用父收集器的 `childCollector(String)` 可创建子收集器。

* `collect()` 方法返回 `MetricCollection`。该类本质上返回由收集器及其子节点形成的树的不可变视图，子节点同样用 `MetricCollection` 表示。

  注意：调用 `collect()` 意味着子收集器也会被收集。

* 每个收集器有名称，常用于描述所收集指标类别；例如 `"ApiCall"` 与 `"ApiCallAttempt"`。

* [接口原型](prototype/MetricCollector.java)

### 指标发布器（MetricPublisher）

* `MetricPublisher` 将已收集指标发布到 SDK 外部的系统。它接收 `MetricCollection`，可能将数据转换为更丰富的指标，并转换为目标接收方期望的格式。

* 默认情况下，SDK 将提供向 [Amazon CloudWatch](https://aws.amazon.com/cloudwatch/) 与 [Client Side Monitoring](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/sdk-metrics.html)（亦称 AWS SDK Metrics for Enterprise Support）发布指标的实现。

* 指标发布器在 SDK 内可插拔，客户可提供自定义实现。

* 不同发布器在发布指标列表、发布频率、所需配置等方面可有不同行为。

* [接口原型](prototype/MetricPublisher.java)

## 启用指标

指标功能默认关闭。可通过以下方式启用并配置：

### 选项 1：在请求上配置 MetricPublisher

可直接在 `RequestOverrideConfiguration` 上配置发布器：

```java
MetricPublisher metricPublisher = CloudWatchMetricPublisher.create();
DynamoDbClient dynamoDb = DynamoDbClient.create();
dynamoDb.listTables(ListTablesRequest.builder()
                                     .overrideConfiguration(c -> c.addMetricPublisher(metricPublisher))
                                     .build());
```

设置 metric publisher 的方法遵循 `ExecutionInterceptor` 已确立的模式：

```java
class RequestOverrideConfiguration {
    // ...
    class Builder {
        // ...
        Builder metricPublishers(List<MetricPublisher> metricsPublishers);
        Builder addMetricPublisher(MetricPublisher metricsPublisher);
    }
}
```

### 选项 2：在客户端上配置 MetricPublisher

可直接在 `ClientOverrideConfiguration` 上配置发布器。此方式优先级**低于**上文**选项 1**。

```java
MetricPublisher metricPublisher = CloudWatchMetricPublisher.create();
DynamoDbClient dynamoDb = DynamoDbClient.builder()
                                        .overrideConfiguration(c -> c.addMetricPublisher(metricPublisher))
                                        .build();
```

设置 metric publisher 的方法同样遵循 `ExecutionInterceptor` 的模式：

```java
class ClientOverrideConfiguration {
    // ...
    class Builder {
        // ...
        Builder metricPublishers(List<MetricPublisher> metricsPublishers);
        Builder addMetricPublisher(MetricPublisher metricsPublisher);
    }
}
```

**注意：** 与 `httpClient` 设置类似，对 `DynamoDbClient` 调用 `close()` *不会*关闭已配置的 `metricPublishers`。用完后需自行关闭 `metricPublishers`。

### 选项 3：通过系统属性或环境变量配置 MetricPublisher

该选项允许客户默认启用指标发布，而无需通过上文**选项 1**或**选项 2**在代码中启用。即无需修改运行时代码即可打开指标。

通过环境变量或系统属性启用。若两者均设置，以系统属性为准。若已通过**选项 2**在客户端级别启用指标，则忽略本选项。在请求级通过**选项 1**覆盖发布器时，将覆盖任何全局启用的发布器。

**系统属性：** `aws.metricPublishingEnabled=true` 

**环境变量：** `AWS_METRIC_PUBLISHING_ENABLED=true`

取值须为 `"true"` 或 `"false"`。其他字符串将按 `"false"` 处理，且每次创建 SDK 客户端时记录警告。

为 `"false"` 时，客户端不会发布指标。

为 `"true"` 时，每个客户端会向一组「全局 metric publisher」发布指标。该组通过当前用于发现 HTTP 客户端的相同机制自动加载。因此只要包含 `cloudwatch-metric-publisher` 模块并启用上述系统属性或环境变量，即可为所有 AWS 客户端启用向 CloudWatch 发布指标。

「全局 Metric Publisher」集合是静态的，在**选项 3**启用期间用于应用内*所有* AWS SDK 客户端实例。将注册 JVM shutdown hook，对每个 publisher 调用 `MetricPublisher.close()`（以免 publisher 使用非守护线程阻塞 JVM 退出）。

#### 将 MetricPublisher 更新为可作为全局 metric publisher

上文**选项 3**提到「全局 Metric Publisher」，由 SDK 自动发现。本节说明其发现与创建方式。

每个在**选项 3**启用时可被加载的 `MetricPublisher` 必须：
1. 提供 `SdkMetricPublisherService` 实现。该类须有无参构造函数，用于实例化特定 `MetricPublisher`（例如作为 `CloudWatchMetricPublisher` 工厂的 `CloudWatchMetricPublisherService`）。
2. 提供资源文件：`META-INF/services/software.amazon.awssdk.metrics.SdkMetricPublisherService`，内容为 `SdkMetricPublisherService` 实现类的全限定类名列表。

所有全局 metric publisher 候选必须实现的 `software.amazon.awssdk.metrics.SdkMetricPublisherService` 接口定义为：

```java
public interface SdkMetricPublisherService {
    MetricPublisher createMetricPublisher();
}
```

**`SdkMetricPublisherService` 示例**

通过实现 `SdkMetricPublisherService` 可将 `CloudWatchMetricPublisher` 作为全局 metric publisher 启用：

```java
package software.amazon.awssdk.metrics.publishers.cloudwatch;

public final class CloudWatchSdkMetricPublisherService implements SdkMetricPublisherService {
    @Override
    public MetricPublisher createMetricPublisher() {
        return CloudWatchMetricPublisher.create();
    }
}
```

并在 `cloudwatch-metric-publisher` 模块中创建 `META-INF/services/software.amazon.awssdk.metrics.SdkMetricPublisherService`，内容为：

```
software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchSdkMetricPublisherService
```

#### 选项 3 实现细节与边界情况

**选项 3 启用时 SDK 如何加载 `MetricPublisher`**

当客户端在**选项 3**启用且**选项 2**为「未指定」时创建，客户端通过静态「全局 metric publisher 列表」单例获取要使用的全局发布器。该单例按以下流程仅初始化一次：
1. 单例使用 `java.util.ServiceLoader` 定位按上文配置的全部 `SdkMetricPublisherService`。所用 classloader 与 HTTP 客户端 service loader（`software.amazon.awssdk.core.internal.http.loader.SdkServiceLoader`）一致，即按顺序取：(1) 加载 SDK 的 classloader；(2) 当前线程 classloader；(3) 系统 classloader。
2. 单例为每个以此方式找到的 `SdkMetricPublisherService` 创建一个实例。
3. 单例通过这些服务为每个 `MetricPublisher` 创建实例。

**当选项 2「未指定」时，选项 3 与选项 1 如何协作**

当**选项 2**「未指定」时，SDK 将**选项 3**视为默认的客户端级 metric publisher 集合。因此若客户：(1) 用**选项 3**启用全局发布；(2) 未用**选项 2**指定客户端级发布器；(3) 用**选项 1**在请求级指定发布器；则全局 metric publisher 仍会被*实例化*但不会使用。这样可避免每次请求都查询全局指标配置。

**为考虑选项 3，选项 2 何时算「未指定」**

仅当**选项 2**「未指定」时才考虑使用全局 metric publisher（**选项 3**）。

「未指定」指客户：(1) 未调用 `ClientOverrideConfiguration.Builder.addMetricPublisher()` / `ClientOverrideConfiguration.Builder.metricPublishers()`；或 (2) 在客户端覆盖配置构建器上，最后一次修改 `metricPublisher` 的动作为 `ClientOverrideConfiguration.Builder.metricPublishers(null)`。

该定义有意不包含 `ClientOverrideConfiguration.Builder.metricPublishers(emptyList())`。将 `metricPublishers` 设为空列表等价于设为 `NoOpMetricPublisher`。

**依赖 AWS 客户端的 `SdkMetricPublisherService` 实现**

凡可通过 `SdkMetricPublisherService` 创建且依赖 AWS 服务客户端的 `MetricPublisher`，在通过 `SdkMetricPublisherService` 创建这些 AWS 客户端时**必须**使用**选项 2**关闭其指标发布。以免全局 metric publisher 单例初始化过程反过来依赖尚未完成初始化的全局单例。

## 模块
新增模块以支持指标功能。

### metrics-spi
* 包含指标接口与不依赖其他模块的默认实现
* 为 `core` 下的子模块
* `sdk-core` 依赖 `metrics-spi`，客户会自动传递依赖该模块

### metrics-publishers
* 新模块，包含 SDK 支持的全部 publisher 实现
* 其下为每个 publisher 建子模块（`cloudwatch-publisher`、`csm-publisher`）
* 客户需**显式添加依赖**才能使用 SDK 提供的 publisher

## 性能
指标的主要信条之一是「启用默认指标应对应用性能影响极小」。设计上做如下取舍：

* 若指标关闭，收集时使用 No-op metric collector，其方法均为空操作并立即返回。

* Metric publisher 实现可能涉及网络调用，若同步执行会影响延迟。因此所有 SDK publisher 实现将异步处理指标，避免阻塞请求线程。

* 每次发布将编写并运行性能测试，确保在启用、收集并发布指标时 SDK 仍有良好表现。
