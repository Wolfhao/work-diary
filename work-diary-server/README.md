# Work Diary Server

> 宠物博主商务接单管理平台 — 后端服务

## 项目简介

Work Diary Server 是专为宠物博主打造的商务接单管理工具后端服务，基于 **Spring Boot 3.2.2** 构建。  
提供微信小程序快捷登录、商单全生命周期管理、资金（垫付/收入）追踪统计等核心功能，通过 Sa-Token 实现用户数据完全隔离。

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
| 工具库 | Hutool / Lombok / MapStruct | 5.8.25 / latest |
| 文件存储 | 本地 / 阿里云 OSS / MinIO（可配置切换） | — |

---

## 核心模块与 API

### 1. 微信认证模块 `POST /wx/login`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/wx/login` | 微信快捷登录，code 换 Sa-Token |
| POST | `/wx/logout` | 退出登录，注销 Token |

**登录流程：**
```
wx.login() → code → /wx/login → openId → 自动注册/查询用户 → 返回 tokenValue
```

---

### 2. 商单管理模块 `CRUD /work-order`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/work-order` | 新增商单 |
| PUT | `/work-order` | 修改商单（状态/资金回款等） |
| DELETE | `/work-order/{id}` | 逻辑删除商单 |
| GET | `/work-order/{id}` | 查询商单详情 |
| POST | `/work-order/page` | 分页查询当前用户商单列表 |

**商单状态流转：**
```
待开工(10) → 制作中(20) → 待结款(30) → 已完成(40)
                                      ↘ 已取消(90)
```

**核心字段说明：**
- `advanceAmount` — 垫付金额
- `isAdvanceRecovered` — 垫付是否已收回（0/1）
- `incomeAmount` — 商单酬金/收入
- `isIncomeReceived` — 收入是否已到账（0/1）
- `imageUrls` — 商单截图（JSON 数组，存储为字符串）

---

### 3. 数据看板模块 `GET /dashboard`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard/stats` | 获取当前用户资金与接单统计聚合数据 |

返回包含：总垫付 / 待收回垫付 / 总收入 / 已到账收入 / 商单数量等核心统计。

---

### 4. 文件上传模块 `POST /file`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/file/upload` | 上传商单相关截图，返回可访问文件 URL |

支持三种后端存储策略，通过配置自动切换，对上层接口完全透明。

---

## 项目目录结构

```
work-diary-server/
├── src/main/java/com/workdiary/
│   ├── WorkDiaryApplication.java       # 启动入口
│   ├── common/                         # 公共组件（统一返回 Result、异常处理、常量）
│   ├── config/                         # 配置类（MyBatisPlus、Redis、Knife4j、WxJava、Sa-Token）
│   ├── controller/                     # API 控制层
│   │   ├── WxAuthController.java       # 微信认证
│   │   ├── WorkOrderController.java    # 商单 CRUD
│   │   ├── DashboardController.java    # 数据看板
│   │   └── FileController.java        # 文件上传
│   ├── dto/                            # 入参 DTO（Add/Update/Query/Login）
│   ├── vo/                             # 出参 VO（WorkOrderVO、DashboardVO）
│   ├── entity/                         # 数据库实体（User、WorkOrder）
│   ├── mapper/                         # MyBatis Mapper 接口
│   └── service/                        # 业务逻辑层
│       ├── storage/                    # 文件存储策略（Strategy Pattern）
│       │   ├── FileStorageStrategy.java    # 策略接口
│       │   ├── FileStorageFactory.java     # 工厂
│       │   └── impl/                       # Local / OSS / MinIO 实现
│       └── impl/                       # Service 实现
└── src/main/resources/
    ├── application.yml                 # 核心配置
    ├── application-dev.yml             # 开发环境（含数据库/Redis/微信 AppId）
    ├── application-prod.yml            # 生产环境
    └── mapper/                         # MyBatis XML
```

---

## 快速启动（本地开发）

### 前置条件

- JDK 17+
- Maven 3.6+
- MySQL 5.7+
- Redis 7+
- 微信小程序 AppID & AppSecret

### 步骤

**1. 克隆并导入**
```bash
git clone <repo-url>
# 在 IntelliJ IDEA 中打开 work-diary-server/
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
    appid: ${WX_APPID:your_appid}
    secret: ${WX_SECRET:your_secret}

work-diary:
  storage:
    type: local   # 或 minio / oss
    local:
      path: /data/upload/
      domain: http://localhost:8080
```

**3. 初始化数据库**
```bash
mysql -u root -p work_diary < doc/sql/init.sql
```

**4. 启动服务**
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
# 或在 IDEA 中直接运行 WorkDiaryApplication.java
```

**5. 验证**
- API 文档：http://localhost:8080/doc.html
- 健康检查：`GET http://localhost:8080/dashboard/stats`（需携带 Token）

---

## 身份验证

所有需要登录的接口请在请求 Header 中携带 Sa-Token：

```
satoken: <登录后返回的 tokenValue>
```

---

## 开发规范

1. **接口规范** — 统一响应结构 `{ code, msg, data }`，遵循 RESTful 风格。
2. **分层规范** — 严格遵循 `Controller → Service → Mapper`，禁止越级调用。
3. **校验规范** — 入参使用 `@Validated` + JSR303 注解进行约束。
4. **文档规范** — 所有 Controller 方法必须配置 `@Operation` + `@Tag` 注解。
5. **Git 提交** — `feat / fix / docs / refactor / chore: 描述`
