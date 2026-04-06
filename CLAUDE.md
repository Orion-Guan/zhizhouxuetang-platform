# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指导。

## 项目概述

Tianji (天机学堂) 是一个基于微服务的在线学习平台，使用 Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 构建。它使用 Nacos 进行服务发现/配置管理，RabbitMQ 进行消息传递，Redis/Redisson 进行缓存/分布式锁，Elasticsearch 进行搜索，MySQL 配合 MyBatis Plus 进行持久化。

## 构建和运行命令

### 构建
```bash
# 构建整个项目
mvn clean package

# 构建特定模块
mvn clean package -pl tj-learning
```

### 运行服务
每个服务都是一个 Spring Boot 应用程序，可以直接运行：
```bash
# 示例：运行学习服务
cd tj-learning && mvn spring-boot:run

# 或从 JAR 运行
java -jar tj-learning/target/tj-learning.jar
```

### Docker 部署
使用提供的 `startup.sh` 脚本，该脚本处理构建 Docker 镜像和创建容器：
```bash
# 用法: ./startup.sh -c <container-name> -n <jar-name> -d <module-path> -p <port>
./startup.sh -c learning-service -n tj-learning -d tj-learning -p 8083
```

### 配置
服务使用 `bootstrap.yml` 配合 Nacos 进行配置管理。从 Nacos 加载共享配置：
- `shared-spring.yaml`
- `shared-redis.yaml`
- `shared-mybatis.yaml`
- `shared-logs.yaml`
- `shared-feign.yaml`

通过 bootstrap 文件中的 `spring.profiles.active` 激活环境（例如：`dev`、`local`）。

## 架构

### 模块结构
- **tj-common**: 共享工具、自动配置、领域模型
- **tj-auth**: 认证/授权，包含网关和资源 SDK
- **tj-api**: Feign 客户端和 DTO，用于服务间通信
- **tj-gateway**: Spring Cloud Gateway 用于路由
- **tj-user**: 用户管理
- **tj-course**: 课程目录和内容管理
- **tj-learning**: 学习进度和记录
- **tj-search**: 基于 Elasticsearch 的搜索
- **tj-trade**: 订单处理
- **tj-pay**: 支付集成
- **tj-exam**: 考试管理
- **tj-media**: 媒体处理（视频存储）
- **tj-message**: 通知中心（多模块：domain、api、service）
- **tj-data**: 数据分析

### 服务通信模式

**Feign 客户端**: 服务通过 `tj-api/src/main/java/com/tianji/api/client/` 中的接口进行通信：
- 客户端接口使用 `@FeignClient(value = "service-name", fallbackFactory = ...)` 注解
- Fallback 工厂在服务不可用时提供容错能力

**消息队列**: 通过 RabbitMQ 进行异步通信，交换机定义在 `MqConstants` 中：
- `COURSE_EXCHANGE`: 课程事件（新建、上架、下架、过期、删除）
- `ORDER_EXCHANGE`: 订单事件（支付、退款）
- `LEARNING_EXCHANGE`: 学习事件
- `SMS_EXCHANGE`: 短信通知
- `PAY_EXCHANGE`: 支付事件
- `ERROR_EXCHANGE`: 错误处理

使用 `RabbitMqHelper` 发送消息：
```java
rabbitMqHelper.send(exchange, routingKey, data);
rabbitMqHelper.sendDelayMessage(exchange, routingKey, data, Duration.ofMillis(delay));
rabbitMqHelper.sendAsync(exchange, routingKey, data); // 异步发送
```

### 数据层模式

**实体类**: 位于 `domain.po` 包中，使用 MyBatis Plus 注解：
```java
@TableId(value = "id", type = IdType.ASSIGN_ID)  // 雪花 ID
@TableField("user_id")  // 列映射
```

**Mapper**: 继承 MyBatis Plus 的 `BaseMapper<T>` - 基本 CRUD 操作无需编写 SQL。

**Service**: 继承 `ServiceImpl<Mapper, Entity>`：
```java
@Service
public class MyServiceImpl extends ServiceImpl<MyMapper, MyEntity> implements IMyService {
    // 使用基础方法: list(), getOne(), save(), update() 等
}
```

**自动填充**: `BaseMetaObjectHandler` 在插入/更新时自动从 `UserContext.getUser()` 设置 `creater` 和 `updater` 字段。

### 常见模式

**用户上下文**: 当前用户 ID 的 ThreadLocal 存储：
```java
UserContext.getUser()  // 获取当前用户 ID
UserContext.setUser(userId)  // 设置用户 ID（通常在认证拦截器中）
UserContext.removeUser()  // 清理（在过滤器中）
```

**响应包装**: 所有 API 响应使用 `R<T>`：
```java
return R.ok(data);  // 成功并返回数据
return R.ok();      // 成功但不返回数据
return R.error(msg); // 错误
```

**分页**: 请求使用 `PageQuery`，响应使用 `PageDTO<T>`。

**分布式锁**: 使用 `@Lock` 注解配合 Redisson：
```java
@Lock(name = "lock:learning:#{userId}", lockType = LockType.RE_ENTRANT_LOCK,
      leaseTime = 30, autoUnlock = true)
public void myMethod(Long userId) { ... }
```

**验证**: 自定义 `@ParamChecker` 注解用于参数验证，通过 `CheckerAspect` 处理。

### 异常处理
`tj-common` 中的自定义异常：
- `BizIllegalException`: 业务逻辑错误
- `BadRequestException`: 无效请求
- `DbException`: 数据库错误
- `ForbiddenException`: 拒绝访问
- `UnauthorizedException`: 未认证

由 `CommonExceptionAdvice` 处理，自动包装为 `R` 响应。

### 服务特定说明

**学习服务**: 处理学习记录、进度跟踪和互动问答。主要功能：
- 带有每周频率的学习计划
- 互动问题和回复（支持点赞）
- 通过 Feign 客户端与课程、交易和用户服务集成

**课程服务**: 管理课程目录、内容和草稿/工作流状态。在发布前使用草稿表进行编辑。

**认证服务**: 提供基于 JWT 的认证，带有 JWK 公钥端点供其他服务验证令牌。网关使用 `tj-auth-gateway-sdk`，资源服务器使用 `tj-auth-resource-sdk`。

### 测试
`src/test/java` 中的测试文件使用标准的 JUnit 模式，并带有 Spring Boot 测试支持。
