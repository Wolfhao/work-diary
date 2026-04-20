# Work Diary Server

> 个人工作台小程序 — 后端服务

## 项目简介

Work Diary Server 是**个人工作台小程序**的后端服务，基于 **Spring Boot 3.2.2** 构建，服务于配套的微信小程序客户端。

### 产品定位

这是一个面向个人（博主/自由职业者）的**多功能工作台小程序**，已实现模块与规划如下：

| 模块 | 状态 | 说明 |
|------|------|------|
| 商单管理 | ✅ 已上线 | 商单全生命周期管理、垫付/收入资金追踪、数据看板统计 |
| 旅游攻略 | ✅ 已上线 | 纯小程序端实现，静态攻略内容，无需后端接口 |
| 记账本 | 🚧 规划中 | 聊天式 AI 记账、资产管理、账单报表 |

后端当前承载**商单管理**模块的全部接口，后续将随记账本模块的实现持续扩展。通过 Sa-Token 实现用户数据完全隔离。

---

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 / 框架 | Java + Spring Boot | 17 / 3.2.2 |
| 权限认证 | Sa-Token + Redis 持久化 | 1.37.0 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 5.7 |
| 缓存 | Redis | 7 |
| 接口文档 | Knife4j (OpenAPI 3) | 4.4.0 |
| 微信SDK | weixin-java-miniapp (WxJava) | 4.6.0 |
| 工具库 | Hutool / Lombok / MapStruct | 5.8.25 / latest / 1.5.5.Final |
| 文件存储 | 本地 / 腾讯云 COS / MinIO / OSS（可配置切换） | — |
| 构建工具 | Maven | 3.6+ |

---

## 项目目录结构

采用**按业务模块分包**结构，`src/main/java/com/workdiary/` 下各包职责如下：

```
com/workdiary/
├── common/           # 公共基础：统一响应体 Result、错误码、全局异常处理
├── config/           # 全局配置：MyBatis-Plus、Sa-Token 拦截、Knife4j、微信 SDK、文件存储属性
├── infrastructure/   # 跨模块基础设施：文件存储策略（Strategy Pattern）本地 / COS / MinIO / OSS
├── shared/           # 跨模块共享实体：User（被 auth、workorder 等多模块共用）
└── module/           # 业务模块根包（每个子目录为独立模块，含完整的 controller/service/mapper/entity/dto/vo 分层）
    ├── auth/         # 认证登录模块
    ├── file/         # 文件上传下载模块
    ├── workorder/    # 商单管理模块（含看板统计）
    └── ledger/       # 记账本模块（占位，待实现）
```

**约定**：新业务模块统一在 `module/` 下新建子目录，模块内沿用 `controller / service / mapper / entity / dto / vo` 分层；跨模块共享的实体放 `shared/`，跨模块共享的技术能力放 `infrastructure/`。

资源文件位于 `src/main/resources/`，按环境拆分配置：`application.yml`（公共）、`application-dev.yml`、`application-prod.yml`（环境变量驱动）。

---

## 数据库设计

### user 表（微信小程序用户表）

```sql
CREATE TABLE `user` (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `open_id`     varchar(64)  NOT NULL UNIQUE COMMENT '微信 OpenID',
  `union_id`    varchar(64)           COMMENT '微信 UnionID (预留)',
  `nickname`    varchar(64)           COMMENT '昵称',
  `avatar_url`  varchar(255)          COMMENT '头像 URL',
  `phone`       varchar(32)           COMMENT '绑定手机号',
  `status`      tinyint(2)   DEFAULT 1  COMMENT '1:正常, 0:禁用',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  tinyint(2)   NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) COMMENT='微信小程序用户表';
```

**关键索引**：`uk_open_id (open_id)` — 登录认证唯一约束

---

### work_order 表（商单记录表）

```sql
CREATE TABLE `work_order` (
  `id`                    bigint(20)     NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id`               bigint(20)     NOT NULL COMMENT '所属用户 ID',
  `title`                 varchar(128)   NOT NULL COMMENT '商单名称',
  `description`           text                    COMMENT '商单描述/备注',
  `platform`              varchar(64)             COMMENT '发布平台(小红书/抖音/B站/微博)',
  `image_urls`            json                    COMMENT '截图 URL 集合(JSON 数组)',

  `advance_amount`        decimal(10,2)  DEFAULT 0.00 COMMENT '垫付金额',
  `is_advance_recovered`  tinyint(2)     DEFAULT 0    COMMENT '垫付收回标识 0/1',
  `advance_recover_time`  datetime                    COMMENT '垫付收回时间',

  `income_amount`         decimal(10,2)  DEFAULT 0.00 COMMENT '酬金/收入',
  `is_income_received`    tinyint(2)     DEFAULT 0    COMMENT '收入到账标识 0/1',
  `income_receive_time`   datetime                    COMMENT '收入到账时间',

  `status`                tinyint(2)     DEFAULT 10   COMMENT '10:待开工 20:制作中 30:待结款 40:已完成 90:已取消',

  `create_time`           datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`           datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`            tinyint(2)     NOT NULL DEFAULT 0,

  KEY `idx_user_id` (`user_id`),
  KEY `idx_status`  (`status`)
) COMMENT='商单记录表';
```

**设计说明**：
- 金额字段统一使用 `decimal(10,2)`，精确到分，避免浮点误差
- 图片集合使用 JSON 格式，支持多图灵活扩展
- `advance_recover_time` / `income_receive_time` 在状态首次变更时自动记录时间戳

**商单状态机**：
```
待开工(10) → 制作中(20) → 待结款(30) → 已完成(40)
                                    ↘
                                    已取消(90)

[自动流转] 当状态更新为 30(待结款) 时：
  如果垫付已收回（或无垫付）AND 收入已到账（或无收入）
  → 系统自动升级状态为 40(已完成)
```

---

## API 接口清单

> 所有需鉴权接口须在 Header 中携带：`workDiaryAuthorization: <tokenValue>`

### 1. 认证模块 `/wx`

| Method | Path | 说明 | 鉴权 |
|--------|------|------|------|
| POST | `/wx/login` | 微信一键登录（自动注册） | 否 |
| POST | `/wx/logout` | 登出，注销 Token | 是 |

**POST `/wx/login` 请求体**
```json
{ "code": "wx.login() 返回的 code" }
```

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "tokenName": "workDiaryAuthorization",
    "tokenValue": "e7a3c...-uuid-...",
    "tokenTimeout": 2592000
  }
}
```

---

### 2. 商单管理模块 `/work-order`

| Method | Path | 说明 | 鉴权 |
|--------|------|------|------|
| POST | `/work-order` | 新增商单 | 是 |
| PUT | `/work-order` | 修改商单（状态/资金等） | 是 |
| DELETE | `/work-order/{id}` | 逻辑删除商单 | 是 |
| GET | `/work-order/{id}` | 查询商单详情 | 是 |
| POST | `/work-order/page` | 分页查询当前用户商单列表 | 是 |

**POST `/work-order` — 新增商单**
```json
{
  "title": "小红书美食探店",         // required
  "description": "发布三张图文",
  "platform": "小红书",
  "imageUrls": ["https://..."],
  "advanceAmount": 200.00,
  "incomeAmount": 1500.00
}
```

**PUT `/work-order` — 修改商单**
```json
{
  "id": 123,                         // required
  "title": "...",
  "status": 30,
  "isAdvanceRecovered": 1,
  "isIncomeReceived": 0
}
```

**POST `/work-order/page` — 分页查询**
```json
{
  "current": 1,
  "size": 10,
  "title": "小红书",                 // 模糊搜索
  "status": 20,                      // 单状态过滤（与 statuses 互斥）
  "statuses": [10, 20],              // 多状态过滤（逻辑 OR）
  "isAdvanceRecovered": 0,
  "isIncomeReceived": 0
}
```

**分页响应结构**
```json
{
  "code": 200,
  "data": {
    "records": [{ "id": 1, "title": "...", "status": 20, ... }],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

---

### 3. 数据看板模块 `/dashboard`

| Method | Path | 说明 | 鉴权 |
|--------|------|------|------|
| GET | `/dashboard/stats` | 获取资金与接单聚合统计 | 是 |

**响应结构 (DashboardVO)**
```json
{
  "code": 200,
  "data": {
    "totalAdvanceAmount":     5000.00,   // 总垫付金额
    "recoveredAdvanceAmount": 3000.00,   // 已收回垫付
    "pendingAdvanceAmount":   2000.00,   // 待收回垫付
    "totalIncomeAmount":      50000.00,  // 总预计收入
    "receivedIncomeAmount":   45000.00,  // 已到账收入
    "pendingIncomeAmount":    5000.00,   // 待到账收入
    "totalOrderCount":        20,        // 历史总接单数
    "completedOrderCount":    15,        // 已完成单数
    "inProgressOrderCount":   5          // 进行中单数 (status 10/20/30)
  }
}
```

统计基于单条自定义 SQL（CASE WHEN 聚合），无 N+1 问题。

---

### 4. 文件模块 `/file`

| Method | Path | 说明 | 鉴权 |
|--------|------|------|------|
| POST | `/file/upload` | 上传文件，返回可访问 URL | 是 |
| GET | `/file/download?key=xxx` | 代理下载文件（私有桶场景） | 否 |

**上传成功响应**
```json
{
  "code": 200,
  "data": "http://localhost:8080/files/20240120/uuid.jpg"
}
```

---

## 核心业务逻辑

### 微信登录流程

```
前端 wx.login()
    ↓ code
POST /wx/login
    ↓ 调用微信 jscode2session
    ↓ 获得 openId
    ↓ 查询 user 表 — 不存在则自动注册 (status=1, nickname="微信用户")
    ↓ StpUtil.login(userId) — Sa-Token 生成 UUID token 存入 Redis
    ↓ 返回 SaTokenInfo
前端保存 token
```

---

### 商单状态自动流转逻辑

在 `WorkOrderServiceImpl.updateWorkOrder()` 中：

1. 权限校验：当前登录用户 ID 必须等于 `work_order.user_id`
2. 时间戳自动记录：`isAdvanceRecovered` 或 `isIncomeReceived` 从 0 变为 1 时，自动写入对应时间
3. 自动升级为完成：当目标状态为 30(待结款)，且垫付/收入均已结清，自动升级为 40(已完成)

---

### 文件存储策略切换

通过配置 `work-diary.storage.type` 无缝切换存储后端，上层接口完全透明：

```
FileController
    ↓
FileStorageFactory.getStrategy()
    ├─ "local" → LocalFileStorageStrategy   (本地文件系统 + Web 资源映射)
    ├─ "cos"   → CosFileStorageStrategy     (腾讯云私有桶 + 代理下载)
    ├─ "minio" → MinioFileStorageStrategy   (占位，待集成)
    └─ "oss"   → OssFileStorageStrategy     (占位，待集成)
```

文件路径规则：`{日期yyyyMMdd}/{UUID}.{扩展名}`

---

## 认证与安全

### Sa-Token 配置

| 参数 | 值 | 说明 |
|------|----|------|
| `token-name` | `workDiaryAuthorization` | Header/Cookie 键名 |
| `timeout` | `2592000`（30天） | Token 有效期 |
| `active-timeout` | `86400`（24h） | 无操作自动下线 |
| `is-concurrent` | `true` | 允许多端同时登录 |
| `is-share` | `false` | 每次登录生成新 Token |
| `token-style` | `uuid` | Token 格式 |

### 路由白名单（无需鉴权）

```
/wx/login        微信登录
/file/download   文件代理下载
/swagger-ui/**   Swagger UI
/v3/api-docs/**  OpenAPI JSON
/doc.html        Knife4j 文档
/webjars/**      静态资源
```

### 安全设计要点

- **多租户隔离**：所有商单查询强制绑定当前 `userId`，防止越权
- **逻辑删除**：软删除保留审计日志，`@TableLogic` 自动过滤
- **参数校验**：所有 DTO 使用 `@Validated` + JSR303 注解
- **异常隐藏**：全局异常处理器屏蔽内部堆栈，仅返回统一错误信息
- **私有桶代理**：COS 密钥不暴露给前端，服务端代理文件下载

---

## 异常处理体系

`GlobalExceptionHandler` 统一捕获并格式化以下异常类型：

| 异常类型 | HTTP 状态码 | 说明 |
|----------|------------|------|
| `ApiException` | 500 | 自定义业务异常 |
| `MethodArgumentNotValidException` | 404 | @Valid 参数验证失败 |
| `BindException` | 404 | 参数绑定失败 |
| `NotLoginException` | 401 | Sa-Token 未登录 |
| `NotPermissionException` | 403 | Sa-Token 权限不足 |
| `Exception` | 500 | 其他未捕获异常（记录日志） |

---

## MyBatis-Plus 配置摘要

- **主键策略**：`id-type: auto`（数据库自增）
- **逻辑删除**：`isDeleted`，删除值 1，正常值 0
- **自动填充**：INSERT 时填充 `createTime + updateTime`；UPDATE 时填充 `updateTime`
- **分页插件**：单页最大 100 条，防止数据量过大
- **下划线转驼峰**：`map-underscore-to-camel-case: true`
- **SQL 日志**：`StdOutImpl`（开发环境输出到控制台）

---

## 快速启动（本地开发）

### 前置条件

- JDK 17+
- Maven 3.6+
- MySQL 5.7+
- Redis 7+
- 微信小程序 AppID & AppSecret

### 步骤

**1. 初始化数据库**
```bash
mysql -u root -p work_diary < doc/sql/init.sql
```

**2. 配置 `application-dev.yml`**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/work_diary?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_db_user
    password: your_db_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_pass

wx:
  miniapp:
    appid: your_appid
    secret: your_secret

work-diary:
  storage:
    type: local           # 或 cos / minio / oss
    local:
      path: /data/upload/
      domain: http://localhost:8080
      prefix: /files
```

**3. 启动服务**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**4. 验证**
- API 文档（Knife4j）：http://localhost:8080/doc.html
- 健康检查：`GET /dashboard/stats`（需携带 Token）

---

## 开发规范

1. **接口规范** — 统一响应结构 `{ code, message, data }`，遵循 RESTful 风格
2. **分层规范** — 严格遵循 `Controller → Service → Mapper`，禁止越级调用
3. **校验规范** — 入参使用 `@Validated` + JSR303 注解
4. **文档规范** — 所有 Controller 方法配置 `@Operation` + `@Tag` 注解
5. **Git 提交** — `feat / fix / docs / refactor / chore: 描述`

---

## 扩展说明

| 扩展点 | 当前状态 | 说明 |
|--------|----------|------|
| MinIO 存储 | 占位实现 | 集成 `minio` SDK 后即可启用 |
| OSS 存储 | 占位实现 | 集成 `aliyun-sdk-oss` 后即可启用 |
| UnionID | 字段预留 | `user.union_id`，适配跨小程序用户体系 |
| 手机号绑定 | 字段预留 | `user.phone`，适配微信手机号授权 |