# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Tianji (天机学堂) 是一个基于微服务的在线学习平台，使用 Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 构建。它使用 Nacos 进行服务发现/配置管理，RabbitMQ 进行消息传递，Redis/Redisson 进行缓存/分布式锁，Elasticsearch 进行搜索，MySQL 配合 MyBatis Plus 进行持久化。

## 构建和运行命令

### 根项目构建
```bash
# 构建整个项目（跳过测试）
mvn clean package -DskipTests

# 构建整个项目（包含测试）
mvn clean package

# 仅编译不打包
mvn clean compile

# 强制更新依赖
mvn clean install -U
```

### 单模块构建
```bash
# 构建特定模块
mvn clean package -pl tj-learning -am

# 运行模块的测试
mvn test -pl tj-learning

# 运行单个测试类
mvn test -pl tj-learning -Dtest=LearningRecordServiceImplTest

# 运行特定测试方法
mvn test -pl tj-learning -Dtest=LearningRecordServiceImplTest#testMethodName
```

### 本地运行服务
```bash
# 直接运行 Spring Boot 应用（通过 Maven）
cd tj-learning && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 通过 java -jar 运行
java -jar tj-learning/target/tj-learning.jar --spring.profiles.active=dev

# 指定端口
java -jar tj-learning/target/tj-learning.jar --server.port=8083
```

### Docker 部署
使用提供的 `startup.sh` 脚本构建 Docker 镜像并创建容器：
```bash
# 语法: ./startup.sh -c <container-name> -n <jar-name> -d <module-path> -p <port> [-o <java-opts>] [-a <debug-port>]
./startup.sh -c learning-service -n tj-learning -d tj-learning -p 8083

# 启用远程调试
./startup.sh -c learning-service -n tj-learning -d tj-learning -p 8083 -a 5005

# 自定义 JVM 参数
./startup.sh -c learning-service -n tj-learning -d tj-learning -p 8083 -o "-Xms512m -Xmx512m"
```

Dockerfile 位于项目根目录，基于 openjdk:11.0-jre-buster 镜像。

### 配置管理
服务使用 `bootstrap.yml` 配合 Nacos 进行配置管理。从 Nacos 加载共享配置：
- `shared-spring.yaml` - 通用 Spring 配置
- `shared-redis.yaml` - Redis/Redisson 配置
- `shared-mybatis.yaml` - MyBatis Plus 配置
- `shared-logs.yaml` - 日志配置
- `shared-feign.yaml` - Feign 客户端配置
- `shared-mq.yaml` - RabbitMQ 配置

通过 bootstrap 文件中的 `spring.profiles.active` 激活环境（`dev`、`local`、`test`、`prod`）。

## 项目结构

### 模块划分
- **tj-common** - 共享工具类、自动配置、领域模型、异常定义
  - `autoconfigure/` - Spring Boot 自动配置（MQ、MyBatis、Redisson、MVC、Swagger、XxlJob）
  - `utils/` - 通用工具类（BeanUtils、JsonUtils、AssertUtils 等）
  - `domain/` - 基础 DTO、查询对象、枚举
  - `exceptions/` - 自定义异常体系
  - `constants/` - 常量定义
  - `validate/` - 参数校验相关
- **tj-auth** - 认证/授权服务
  - `tj-auth-common` - 认证公共模块
  - `tj-auth-gateway-sdk` - 网关认证 SDK
  - `tj-auth-resource-sdk` - 资源服务认证 SDK
  - `tj-auth-service` - 认证服务实现
- **tj-api** - Feign 客户端接口和 DTO，定义服务间通信契约
- **tj-gateway** - Spring Cloud Gateway 路由网关
- **tj-user** - 用户管理服务
- **tj-course** - 课程目录和内容管理
- **tj-learning** - 学习进度、记录、积分、签到、互动问答
- **tj-search** - Elasticsearch 搜索服务
- **tj-trade** - 订单处理
- **tj-pay** - 支付集成
  - `tj-pay-api` - 支付 Feign API
  - `tj-pay-domain` - 支付领域模型
  - `tj-pay-service` - 支付服务实现
- **tj-exam** - 考试管理
- **tj-media** - 媒体处理（视频存储）
- **tj-message** - 通知中心
  - `tj-message-api` - 消息 Feign API
  - `tj-message-domain` - 消息领域模型
  - `tj-message-service` - 消息服务实现
- **tj-data** - 数据分析
- **tj-remark** - 点赞/评论互动管理
- **tj-promotion** - 促销模块（优惠券）

### 多模块项目特点
- 父 POM (`pom.xml`) 管理依赖版本和模块列表
- 每个模块是独立的 Spring Boot 应用
- 模块间通过 Feign 客户端通信
- 公共代码放在 `tj-common` 中

## 代码架构与模式

### 分层架构（标准模块结构）
```
src/main/java/com/tianji/{module}/
├── {ModuleName}Application.java          # Spring Boot 启动类
├── config/                                # 模块级配置类
├── controller/                            # Web 层 - HTTP 接口
├── service/                               # 业务层
│   ├── I{ServiceName}Service.java         # 业务接口
│   └── impl/                              # 业务实现
│       └── {ServiceName}ServiceImpl.java
├── mapper/                                # 数据访问层
│   └── {EntityName}Mapper.java            # MyBatis Plus Mapper
├── domain/                                # 领域模型
│   ├── po/                                # 持久化对象（数据库表映射）
│   ├── dto/                               # 数据传输对象
│   ├── query/                             # 查询参数对象
│   └── vo/                                # 视图对象
├── mq/                                    # 消息队列消费者
└── utils/                                 # 模块级工具类
```

### 领域对象约定
- **PO (Persistent Object)**: 位于 `domain.po` 包，数据库表映射，使用 MyBatis Plus 注解
  - `@TableId(value = "id", type = IdType.ASSIGN_ID)` - 雪花 ID
  - `@TableField("column_name")` - 列映射
- **DTO (Data Transfer Object)**: 位于 `domain.dto`，服务间传输（Feign 调用、MQ 消息）
- **VO (View Object)**: 位于 `domain.vo`，返回给前端的视图模型
- **Query**: 位于 `domain.query`，分页查询参数，继承 `PageQuery`

### 数据层模式

**MyBatis Plus 集成**
- Mapper 继承 `BaseMapper<T>`，提供基础 CRUD 操作
- Service 继承 `ServiceImpl<Mapper, Entity>`，提供基础业务方法
- 自定义 SQL 使用 `@Select`、`@Update` 等注解或 XML
- 自动填充：`BaseMetaObjectHandler` 自动设置 `creater` 和 `updater` 字段

**常用 MyBatis Plus 方法**
```java
// 查询
list()                    // 查询所有
getById(id)              // 按 ID 查询
getOne(queryWrapper)     // 查询单个
list(queryWrapper)       // 条件查询
page(page, queryWrapper) // 分页查询

// 写入
save(entity)             // 插入
saveBatch(list)          // 批量插入
saveOrUpdate(entity)     // 插入或更新

// 更新
updateById(entity)       // 按 ID 更新
update(entity, wrapper)  // 条件更新

// 删除
removeById(id)           // 按 ID 删除
remove(queryWrapper)     // 条件删除
```

### 服务通信模式

**Feign 客户端**
- 接口定义在 `tj-api/src/main/java/com/tianji/api/client/`
- 使用 `@FeignClient(value = "service-name", fallbackFactory = ...)` 注解
- Fallback 工厂在服务不可用时提供容错

**消息队列 (RabbitMQ)**
- 交换机定义在 `tj-common/constants/MqConstants.java`
- 常用交换机：
  - `COURSE_EXCHANGE`: 课程事件（新建、上架、下架、过期、删除）
  - `ORDER_EXCHANGE`: 订单事件（支付、退款）
  - `LEARNING_EXCHANGE`: 学习事件
  - `LIKE_RECORD_EXCHANGE`: 点赞记录同步
- 使用 `RabbitMqHelper` 发送消息：
  ```java
  rabbitMqHelper.send(exchange, routingKey, data);
  rabbitMqHelper.sendDelayMessage(exchange, routingKey, data, Duration.ofMillis(delay));
  rabbitMqHelper.sendAsync(exchange, routingKey, data);  // 异步发送
  ```

### 常见模式

**用户上下文**
- ThreadLocal 存储当前用户 ID
- `UserContext.getUser()` - 获取当前用户 ID
- `UserContext.setUser(userId)` - 设置用户 ID（认证拦截器中）
- `UserContext.removeUser()` - 清理（在过滤器中）

**响应包装**
- 所有 API 响应使用 `R<T>`（`tj-common/domain/R.java`）
- `R.ok(data)` - 成功并返回数据
- `R.ok()` - 成功但不返回数据
- `R.error(msg)` / `R.error(code, msg)` - 错误

**分页**
- 请求使用 `PageQuery`（`tj-common/domain/query/PageQuery.java`）
- 响应使用 `PageDTO<T>`（`tj-common/domain/dto/PageDTO.java`）

**分布式锁**
- 使用 `@Lock` 注解配合 Redisson
```java
@Lock(name = "lock:learning:#{userId}", 
      lockType = LockType.RE_ENTRANT_LOCK,
      leaseTime = 30, 
      autoUnlock = true)
public void myMethod(Long userId) { ... }
```

**参数校验**
- 自定义 `@ParamChecker` 注解
- 通过 `CheckerAspect` 处理
- 使用 `AssertUtils` 进行参数检查

**枚举处理**
- 枚举实现 `BaseEnum` 接口
- 使用 `EnumUtils` 进行枚举值和描述的转换
- 支持 `@EnumValid` 注解进行枚举值校验

### 异常处理
`tj-common` 中的自定义异常（`tj-common/exceptions/`）：
- `BadRequestException` (400) - 无效请求
- `BizIllegalException` (400) - 业务逻辑错误
- `UnauthorizedException` (401) - 未认证
- `ForbiddenException` (403) - 拒绝访问
- `RequestTimeoutException` (404) - 请求超时/资源不存在
- `DbException` (500) - 数据库错误

由 `CommonExceptionAdvice` 统一处理，自动包装为 `R` 响应。

### 认证与授权
- 基于 JWT 的认证机制
- 网关使用 `tj-auth-gateway-sdk` 验证令牌
- 资源服务使用 `tj-auth-resource-sdk` 启用资源认证
- 在 `bootstrap.yml` 中配置 `tj.auth.resource.enable: true`
- 认证服务提供 JWK 公钥端点供其他服务验证令牌

### 网关服务
- 使用 Spring Cloud Gateway 进行路由
- 服务映射规则：短前缀 → 服务名（如 `/ls/**` → `learning-service`）
- 配置 `StripPrefix=1` 去除路由前缀
- 全局 CORS 配置允许所有来源
- 公开端点（如 JWK）需配置为白名单

## 通用工具类

`tj-common/utils/` 提供大量实用工具：
- `BeanUtils` - 对象属性拷贝
- `JsonUtils` - JSON 序列化/反序列化
- `AssertUtils` - 参数断言
- `CollUtils` / `ArrayUtils` / `ObjectUtils` - 集合/数组/对象操作
- `StringUtils` - 字符串操作
- `RequestUtils` / `WebUtils` - HTTP 请求处理
- `UserContext` - 用户上下文管理
- `RequestIdUtil` - 请求 ID 追踪
- `SPELUtils` - Spring EL 表达式解析
- `SqlWrapperUtils` - MyBatis Plus 条件构造器工具

## 测试

### 测试结构
- 测试文件位于 `src/test/java/`，与主代码包结构一致
- 使用 JUnit 5 + Spring Boot Test
- 命名约定：`{ClassName}Test.java`

### 运行测试
```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl tj-learning

# 运行特定测试类
mvn test -pl tj-learning -Dtest=LearningRecordServiceImplTest

# 运行特定测试方法
mvn test -pl tj-learning -Dtest=LearningRecordServiceImplTest#testMethodName

# 跳过测试打包
mvn clean package -DskipTests

# 重新运行失败的测试
mvn test -Dtest=FailedTest#testMethod -Dsurefire.rerunFailingTestsCount=2
```

### 测试约定
- 测试类使用 `@Slf4j` 记录日志
- 测试方法使用 `@Test` 注解
- 异常测试使用 `assertThrows`
- 集成测试使用 `@SpringBootTest`
- 单元测试尽量使用 `@ExtendWith(MockitoExtension.class)`

## 开发工作流

### 创建新模块
1. 在根 `pom.xml` 的 `<modules>` 列表中添加新模块
2. 创建模块目录结构和 `pom.xml`
3. 继承父 POM 的依赖管理
4. 添加必要的依赖（Spring Boot Starter、MyBatis Plus 等）
5. 创建启动类、配置文件
6. 在需要通信的服务中添加 Feign 客户端

### 添加新功能（以添加实体为例）
1. **领域模型**: `domain.po.{EntityName}` - 数据库表映射
2. **Mapper**: `mapper.{EntityName}Mapper` - 继承 `BaseMapper`
3. **Service 接口**: `service.I{EntityName}Service` - 定义业务方法
4. **Service 实现**: `service.impl.{EntityName}ServiceImpl` - 实现业务逻辑
5. **Controller**: `controller.{EntityName}Controller` - HTTP 接口
6. **DTO/VO**: `domain.dto` / `domain.vo` - 传输对象
7. **测试**: `src/test/...` - 单元测试和集成测试

### 跨模块调用
1. 在 `tj-api` 中定义 Feign 客户端接口和 DTO
2. 在服务提供方实现业务逻辑
3. 在服务消费方注入 Feign 客户端
4. 处理熔断和降级（通过 fallbackFactory）

## 日志与监控
- 日志框架：SLF4J + Logback
- 日志配置：`shared-logs.yaml`
- Knife4j (Swagger) 用于 API 文档
- XXL-JOB 用于定时任务

## 性能优化提示
- 批量操作使用 `saveBatch()` 而非循环 `save()`
- 分页查询使用 `PageQuery` 和 `PageDTO`
- 热点数据使用 Redisson 分布式缓存
- 耗时操作异步化：使用 `RabbitMqHelper.sendAsync()`
- 延迟任务使用 `RabbitMqHelper.sendDelayMessage()`
- 避免 N+1 查询问题，合理使用 MyBatis Plus 的关联查询

## 常见问题

**Q: 如何查看某个模块的依赖？**
A: 查看模块的 `pom.xml`，注意继承的父 POM 依赖管理

**Q: 如何添加新的 Feign 客户端？**
A: 1. 在 `tj-api` 中定义接口；2. 在消费方使用 `@FeignClient` 引用；3. 确保服务提供方暴露相应接口

**Q: 如何配置本地开发环境？**
A: 使用 `spring.profiles.active=local`，配置 `bootstrap-local.yml`，连接本地 Nacos、MySQL、Redis 等

**Q: 数据库 ID 策略是什么？**
A: 使用 MyBatis Plus 的 `IdType.ASSIGN_ID`（雪花算法）

**Q: 如何处理事务？**
A: 使用 `@Transactional` 注解，注意跨服务事务需使用分布式事务方案（如 Seata）

## 参考资料
- Spring Boot 2.7.2
- Spring Cloud 2021.0.3
- Spring Cloud Alibaba 2021.0.1.0
- MyBatis Plus 3.5.2
- Java 11
- Maven 3.x