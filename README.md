# 知舟学堂 (Zhizhouxuetang)
一个基于 Spring Cloud 微服务架构的在线学习平台，提供完整的在线课程学习解决方案。

## 项目简介

知舟学堂是一个功能完善的在线教育平台，采用微服务架构设计，涵盖了课程学习、订单支付、考试测评、互动评论、数据分析等完整功能模块。

## 核心功能

- **认证授权** - JWT + 基于 RBAC 的权限管理系统
- **课程管理** - 课程分类、章节、目录管理，支持多种课程结构
- **学习系统** - 学习进度跟踪、课程记录、积分系统、签到功能
- **全文搜索** - 基于 Elasticsearch 的课程搜索
- **支付交易** - 支付宝支付集成，订单管理
- **促销优惠** - 优惠券系统，折扣活动
- **互动评论** - 课程评论、问答、点赞功能
- **数据分析** - 数据统计与报表分析
- **考试测评** - 试题管理、在线考试
- **媒体处理** - 视频存储与播放支持
- **消息通知** - 短信、站内消息通知
- **用户管理** - 用户信息、权限管理

## 技术架构

### 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.2 | 应用框架 |
| Spring Cloud | 2021.0.3 | 微服务框架 |
| Spring Cloud Alibaba | 2021.0.1.0 | 阿里微服务组件 |
| Nacos | 2.x | 服务发现与配置中心 |
| MySQL | 8.0.23 | 关系型数据库 |
| MyBatis Plus | 3.5.2 | ORM 框架 |
| Redis | - | 缓存与分布式锁 |
| Redisson | 3.13.6 | Redis 客户端 |
| RabbitMQ | - | 消息队列 |
| Elasticsearch | 7.12.1 | 全文搜索引擎 |
| Seata | 1.5.1 | 分布式事务 |
| XXL-Job | 2.3.1 | 分布式任务调度 |
| Knife4j/Swagger | 3.0.3 | API 文档 |
| OpenFeign | - | 服务间调用 |
| Spring Cloud Gateway | - | API 网关 |

### 项目结构

```
zhizhouxuetang-platform/
├── tj-common               # 公共模块
│   ├── autoconfigure/      # Spring Boot 自动配置
│   ├── domain/             # 基础领域模型
│   ├── enums/              # 枚举定义
│   ├── utils/              # 通用工具类
│   ├── exceptions/         # 异常定义
│   ├── filters/            # 过滤器
│   ├── validate/           # 校验相关
│   └── constants/          # 常量定义
├── tj-auth                 # 认证授权服务
│   ├── tj-auth-common/     # 认证公共模块
│   ├── tj-auth-gateway-sdk/# 网关认证 SDK
│   ├── tj-auth-resource-sdk/# 资源服务认证 SDK
│   └── tj-auth-service/    # 认证服务实现
├── tj-api                  # Feign 客户端 API 接口
│   └── src/main/java/com/tianji/api/
│       ├── annotations/    # 注解定义
│       ├── cache/          # 缓存接口
│       ├── client/         # Feign 客户端定义
│       ├── config/         # 配置类
│       ├── constants/      # 常量定义
│       └── dto/            # 数据传输对象
├── tj-gateway              # API 网关服务
├── tj-user                 # 用户管理服务
├── tj-course               # 课程管理服务
├── tj-learning             # 学习记录服务
├── tj-search               # 搜索服务
├── tj-trade                # 订单交易服务
├── tj-pay                  # 支付服务
│   ├── tj-pay-api/         # 支付 API
│   ├── tj-pay-domain/      # 支付领域模型
│   └── tj-pay-service/     # 支付服务实现
├── tj-exam                 # 考试管理服务
├── tj-media                # 媒体处理服务
├── tj-message              # 消息通知服务
│   ├── tj-message-api/
│   ├── tj-message-domain/
│   └── tj-message-service/
├── tj-data                 # 数据分析服务
├── tj-remark               # 评论点赞服务
└── tj-promotion            # 促销优惠服务
```

### 微服务模块说明

| 模块 | 服务名 | 端口 | 功能说明 |
|------|--------|------|----------|
| tj-gateway | gateway-service | 10010 | API 网关，路由分发、权限校验 |
| tj-auth | auth-service | 8081 | 认证授权，JWT 签发，权限管理 |
| tj-user | user-service | 8082 | 用户信息管理 |
| tj-search | search-service | 8083 | 课程全文搜索 |
| tj-media | media-service | 8084 | 视频存储与处理 |
| tj-message | message-service | 8085 | 消息通知 |
| tj-course | course-service | 8086 | 课程分类、目录管理 |
| tj-pay | pay-service | 8087 | 支付处理 |
| tj-trade | trade-service | 8088 | 订单管理 |
| tj-exam | exam-service | 8089 | 考试管理 |
| tj-learning | learning-service | 8090 | 学习进度、积分、签到 |
| tj-remark | remark-service | 8091 | 评论点赞管理 |
| tj-promotion | promotion-service | 8091 | 优惠券促销 |
| tj-data | data-service | 8093 | 数据分析统计 |

## 快速开始

### 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x
- RabbitMQ 3.8+
- Elasticsearch 7.12.x

### 1. 克隆项目

```bash
git clone https://github.com/yourusername/zhizhouxuetang-platform.git
cd zhizhouxuetang-platform
```

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p < database/init.sql
# 导入基础数据
mysql -u root -p zhizhouxuetang < database/data.sql
```

### 3. 配置 Nacos

在 Nacos 中添加配置文件，参考 Nacos 配置文档

需要配置以下共享配置：

- `shared-spring.yaml` - Spring 基础配置
- `shared-redis.yaml` - Redis 配置
- `shared-mybatis.yaml` - MyBatis 配置
- `shared-logs.yaml` - 日志配置
- `shared-feign.yaml` - Feign 配置
- `shared-mq.yaml` - RabbitMQ 配置

### 4. 修改配置

修改每个服务的 `bootstrap-dev.yml` 中的配置：

- Nacos 地址
- 数据库连接信息
- 第三方服务密钥（阿里云 OSS/腾讯云 COS 等）

### 5. 构建项目

```bash
# 编译整个项目（跳过测试）
mvn clean package -DskipTests

# 编译单个模块
mvn clean package -pl tj-auth -am
```

### 6. 启动服务

按顺序启动以下服务：

```bash
# 1. 启动认证服务
java -jar tj-auth/tj-auth-service/target/tj-auth-service.jar --spring.profiles.active=dev

# 2. 启动网关
java -jar tj-gateway/target/tj-gateway.jar --spring.profiles.active=dev

# 3. 依次启动其他服务
java -jar tj-user/target/tj-user.jar --spring.profiles.active=dev
java -jar tj-course/target/tj-course.jar --spring.profiles.active=dev
```

或者使用 Maven 启动：

```bash
cd tj-auth/tj-auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 7. 访问服务

- API 网关: http://localhost:10010
- Swagger/Knife4j 文档: http://localhost:10010/doc.html
- Nacos 控制台: http://localhost:8848/nacos

## Docker 部署

项目提供了 Dockerfile 和一键部署脚本：

```bash
# 构建镜像并启动容器
./startup.sh -c gateway -n tj-gateway -d tj-gateway -p 10010
./startup.sh -c auth -n tj-auth -d tj-auth/tj-auth-service -p 8081
```

Dockerfile 基于 `openjdk:11.0-jre-buster` 镜像。

## 开发指南

### 代码结构约定

每个微服务遵循标准分层架构：

```
src/main/java/com/tianji/{module}/
├── {ModuleName}Application.java      # 启动类
├── config/                           # 配置类
├── controller/                       # HTTP 接口
├── service/                          # 业务层
│   ├── I{ServiceName}Service.java
│   └── impl/
│       └── {ServiceName}ServiceImpl.java
├── mapper/                           # 数据访问层
├── domain/
│   ├── po/          # 持久化对象
│   ├── dto/         # 数据传输对象
│   ├── query/       # 查询参数
│   └── vo/          # 视图对象
├── mq/             # 消息队列消费者
└── utils/          # 工具类
```

### 领域对象约定

| 类型 | 包路径 | 说明 |
|------|--------|------|
| PO (Persistent Object) | `domain.po` | 数据库表映射，使用 MyBatis Plus 注解 |
| DTO (Data Transfer Object) | `domain.dto` | 服务间传输（Feign 调用、MQ 消息） |
| VO (View Object) | `domain.vo` | 返回给前端的视图模型 |
| Query | `domain.query` | 分页查询参数 |

### 服务间通信

- **Feign 声明式调用**: 接口定义在 `tj-api` 模块，使用 `@FeignClient` 注解
- **RabbitMQ 异步消息**: 通过 `RabbitMqHelper` 工具类发送消息
- **分布式事务**: 使用 Seata 保证数据一致性

### 参与开发

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目基于 MIT 许可证开源 - 查看 [LICENSE](LICENSE) 文件了解详情

## 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [MyBatis Plus](https://mp.baomidou.com/)
- [Nacos](https://nacos.io/)
- [Elasticsearch](https://www.elastic.co/)