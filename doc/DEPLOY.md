# Work Diary 完整部署方案

> 📦 **推荐使用 Docker 部署**（见第一章），无需关心宿主机 Java 版本，开箱即用。  
> 📋 传统手动部署见第二章。

## 部署架构总览

```
用户微信小程序
     │
     │ HTTPS (443)
     ▼
┌───────────────────────────────────┐
│           云服务器 / ECS           │
│                                   │
│   Nginx (反向代理 + HTTPS 终止)    │
│      ↓ 转发到                     │
│   Spring Boot (8080)              │
│   [work-diary-server.jar]         │
│                                   │
│   MySQL 5.7   Redis 7             │
│   (可同机或独立 RDS / Redis)        │
└───────────────────────────────────┘
```

---

## 一、Docker 部署（推荐 ⭐）

> 适用于：宿主机 Java 版本不是 17、或希望快速一键部署的场景。  
> 宿主机只需安装 Docker，项目在容器内使用 JDK 17，不影响系统已有环境。

### 1.1 安装 Docker & Docker Compose

```bash
# CentOS 7/8
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

sudo systemctl enable docker
sudo systemctl start docker

# 验证
docker --version        # Docker version 24.x.x
docker compose version  # Docker Compose version v2.x.x
```

### 1.2 拉取项目代码

```bash
# 服务器上
git clone <your-repo-url> /opt/work-diary
cd /opt/work-diary
```

### 1.3 配置环境变量

```bash
cp .env.example .env
vim .env   # 填写数据库密码、Redis 密码、微信 AppID/Secret、COS 密钥
```

`.env` 关键配置说明：

| 变量 | 说明 | 示例 |
|------|------|------|
| `DB_PASS` | MySQL root 密码 | `MyStr0ng_Pass!` |
| `REDIS_PASS` | Redis 密码 | `redis_secret` |
| `WX_APPID` | 微信小程序 AppID | `wxxxxxxxxxxx` |
| `WX_SECRET` | 微信小程序 Secret | `xxxxx` |
| `COS_SECRET_ID` | 腾讯云 SecretId | `AKIDxxxxxxxx` |
| `COS_SECRET_KEY` | 腾讯云 SecretKey | `xxxxxxxx` |
| `COS_BUCKET` | COS 存储桶（含 APPID）| `work-diary-1234567890` |

> ⚠️ `.env` 已加入 `.gitignore`，**不会提交到 Git**，请妥善保管。

### 1.4 配置 Nginx SSL 证书

```bash
mkdir -p doc/nginx/ssl

# 将你的证书文件放到 doc/nginx/ssl/ 目录
# 腾讯云/阿里云申请的证书，下载 Nginx 格式，通常有两个文件：
cp /your/cert/fullchain.pem  doc/nginx/ssl/fullchain.pem
cp /your/cert/privkey.pem    doc/nginx/ssl/privkey.pem

# 修改 doc/nginx/nginx.conf 中的域名
sed -i 's/your-api-domain.com/你的真实域名/g' doc/nginx/nginx.conf
```

### 1.5 一键启动所有服务

```bash
cd /opt/work-diary

# 首次启动（会自动构建镜像，约 3~5 分钟）
docker compose up -d --build

# 查看启动状态
docker compose ps
```

正常输出示例：
```
NAME                    STATUS          PORTS
work-diary-nginx        Up              0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
work-diary-server       Up              0.0.0.0:8080->8080/tcp
work-diary-mysql        Up (healthy)    3306/tcp
work-diary-redis        Up (healthy)    6379/tcp
```

### 1.6 验证部署

```bash
# 检查后端日志
docker compose logs -f work-diary-server

# 测试接口（将域名替换成你的）
curl https://your-api-domain.com/doc.html
```

### 1.7 常用运维命令

```bash
# 更新代码后重新部署
git pull
docker compose up -d --build work-diary-server

# 查看实时日志
docker compose logs -f work-diary-server

# 重启某个服务
docker compose restart work-diary-server

# 进入 MySQL 容器
docker compose exec mysql mysql -u root -p

# 停止所有服务（数据不丢失）
docker compose down

# 停止并清空所有数据（慎用！）
docker compose down -v
```

### 1.8 使用云数据库 / 云 Redis（可选）

若已有腾讯云 / 阿里云数据库，可跳过自建 MySQL 和 Redis：

1. 在 `.env` 中修改：
   ```bash
   DB_HOST=rm-xxxxx.mysql.rds.aliyuncs.com  # 云数据库地址
   REDIS_HOST=r-xxxxx.redis.rds.aliyuncs.com # 云 Redis 地址
   ```

2. 在 `docker-compose.yml` 中删除 `mysql` 和 `redis` service 块，以及 `depends_on` 中对应的条件。

3. 重新启动：
   ```bash
   docker compose up -d --build
   ```

---



## 一、后端服务部署

### 1.1 环境准备

```bash
# 安装 JDK 17（以 Ubuntu 22.04 为例）
sudo apt update
sudo apt install -y openjdk-17-jdk

# 验证
java -version  # openjdk 17.x.x

# 安装 MySQL（或使用云数据库）
sudo apt install -y mysql-server

# 安装 Redis
sudo apt install -y redis-server
```

---

### 1.2 数据库初始化

```bash
mysql -u root -p
```

```sql
CREATE DATABASE work_diary
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE work_diary;

-- 用户表
CREATE TABLE `user` (
  `id`          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `open_id`     VARCHAR(64) NOT NULL UNIQUE COMMENT '微信OpenId',
  `nickname`    VARCHAR(64) DEFAULT '微信用户' COMMENT '昵称',
  `avatar_url`  VARCHAR(512) COMMENT '头像URL',
  `status`      TINYINT DEFAULT 1 COMMENT '状态(1:正常 0:禁用)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商单表
CREATE TABLE `work_order` (
  `id`                    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商单ID',
  `user_id`               BIGINT NOT NULL COMMENT '所属用户ID',
  `title`                 VARCHAR(128) NOT NULL COMMENT '商单名称',
  `description`           TEXT COMMENT '描述/备注',
  `platform`              VARCHAR(32) COMMENT '发布平台(小红书/抖音/B站等)',
  `image_urls`            JSON COMMENT '截图URL数组',
  `advance_amount`        DECIMAL(10,2) DEFAULT 0 COMMENT '垫付金额',
  `is_advance_recovered`  TINYINT DEFAULT 0 COMMENT '垫付是否收回(0否 1是)',
  `advance_recover_time`  DATETIME COMMENT '垫付收回时间',
  `income_amount`         DECIMAL(10,2) DEFAULT 0 COMMENT '酬金/收入',
  `is_income_received`    TINYINT DEFAULT 0 COMMENT '收入是否到账(0否 1是)',
  `income_receive_time`   DATETIME COMMENT '收入到账时间',
  `status`                INT DEFAULT 10 COMMENT '状态(10待开工 20制作中 30待结款 40已完成 90已取消)',
  `create_time`           DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time`           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`            TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_work_order_user_id ON work_order(user_id);
```

---

### 1.3 生产配置文件

创建 `application-prod.yml`（不提交到 Git，在服务器上维护）：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://<DB_HOST>:3306/work_diary?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=true
    username: ${DB_USER}
    password: ${DB_PASS}

  data:
    redis:
      host: <REDIS_HOST>
      port: 6379
      password: ${REDIS_PASS}
      database: 0
      lettuce:
        pool:
          max-active: 16
          max-wait: 2000ms

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB

server:
  port: 8080

work-diary:
  storage:
    type: cos    # 生产环境推荐使用腾讯云 COS
    cos:
      region-id: ${COS_REGION}
      secret-id: ${COS_SECRET_ID}
      secret-key: ${COS_SECRET_KEY}
      bucket-name: ${COS_BUCKET}        # 格式: BucketName-APPID
      domain: ${COS_DOMAIN:}            # 可选：自定义CDN域名

wx:
  miniapp:
    appid: ${WX_APPID}
    secret: ${WX_SECRET}
    msgDataFormat: JSON

sa-token:
  token-name: satoken
  timeout: 2592000   # 30天（秒）
  is-concurrent: false
  is-share: false
  token-style: uuid
```

---

### 1.4 构建与启动

**本地打包：**
```bash
cd work-diary-server
mvn clean package -DskipTests -Pprod
# 产物：target/work-diary-server-1.0.0.jar
```

**上传到服务器：**
```bash
scp target/work-diary-server-1.0.0.jar user@<SERVER_IP>:/opt/work-diary/
scp src/main/resources/application-prod.yml user@<SERVER_IP>:/opt/work-diary/config/
```

**启动服务（推荐 systemd 管理）：**

创建 `/etc/systemd/system/work-diary.service`：
```ini
[Unit]
Description=Work Diary Backend Service
After=network.target mysql.service redis.service

[Service]
User=www-data
WorkingDirectory=/opt/work-diary
ExecStart=/usr/bin/java \
  -Xms256m -Xmx512m \
  -Dspring.config.additional-location=file:./config/ \
  -Dspring.profiles.active=prod \
  -DOST_UPLOAD_PATH=/data/upload/ \
  -DWX_APPID=your_appid \
  -DWX_SECRET=your_secret \
  -DDB_USER=root \
  -DDB_PASS=your_db_pass \
  -DREDIS_PASS=your_redis_pass \
  -jar work-diary-server-1.0.0.jar
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable work-diary
sudo systemctl start work-diary

# 查看日志
sudo journalctl -u work-diary -f
```

---

### 1.5 Nginx 反向代理配置

```nginx
server {
    listen 80;
    server_name your-api-domain.com;
    # HTTP 强制跳转 HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name your-api-domain.com;

    ssl_certificate     /etc/ssl/your-api-domain.com.pem;
    ssl_certificate_key /etc/ssl/your-api-domain.com.key;
    ssl_protocols       TLSv1.2 TLSv1.3;

    # 文件上传限制
    client_max_body_size 50M;

    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    # 本地文件存储时，静态资源直接由 Nginx 伺服
    location /files/ {
        alias /data/upload/;
        expires 30d;
        add_header Cache-Control "public";
    }
}
```

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 二、微信小程序发布

### 2.1 修改后端地址

在 `config.js` 中更新为线上域名：
```javascript
const config = {
  isMock: false,
  baseUrl: 'https://your-api-domain.com',
}
export default config;
```

### 2.2 配置合法域名

在[微信公众平台](https://mp.weixin.qq.com) → `开发` → `开发管理` → `开发设置` → `服务器域名` 中添加：

| 类型 | 域名 |
|------|------|
| request 合法域名 | `https://your-api-domain.com` |
| uploadFile 合法域名 | `https://your-api-domain.com` |

> ⚠️ 必须是 HTTPS 域名，且已备案（中国大陆服务器必须备案）。

### 2.3 构建并上传

```bash
cd work-diary-wx

# 安装依赖（首次）
npm install

# 在微信开发者工具中：
# 1. 工具 → 构建 npm
# 2. 版本管理 → 上传代码（填写版本号和描述）
```

### 2.4 发布审核

在[微信公众平台](https://mp.weixin.qq.com) → `版本管理` → 提交线上版本审核。

---

## 三、配置项汇总

### 后端核心环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `WX_APPID` | 微信小程序 AppID | `wx1234567890abcdef` |
| `WX_SECRET` | 微信小程序 Secret | `xxxxxxxxxxxxxxxx` |
| `DB_USER` | 数据库用户名 | `root` |
| `DB_PASS` | 数据库密码 | `your_password` |
| `REDIS_PASS` | Redis 密码 | `your_redis_pass` |
| `COS_REGION` | COS 存储桶地域 | `ap-guangzhou` |
| `COS_SECRET_ID` | 腾讯云 API SecretId | — |
| `COS_SECRET_KEY` | 腾讯云 API SecretKey | — |
| `COS_BUCKET` | COS 存储桶名称（含 APPID） | `work-diary-1234567890` |
| `COS_DOMAIN` | COS 自定义 CDN 域名（可选） | `https://cdn.example.com` |

### 文件存储切换

修改 `work-diary.storage.type`：

| 值 | 说明 |
|-----|------|
| `local` | 本地磁盘存储（开发/测试） |
| `oss` | 阿里云 OSS |
| `minio` | 自建 MinIO 私有存储 |
| `cos` | 腾讯云 COS（推荐生产） |

---

## 四、部署检查清单

### 服务器环境
- [ ] JDK 17 已安装
- [ ] MySQL 已启动，`work_diary` 库已初始化
- [ ] Redis 已启动并设置密码
- [ ] Nginx 已安装并配置反向代理
- [ ] SSL 证书已配置（HTTPS）
- [ ] 防火墙已开放 80 / 443 端口，关闭 8080 外网访问

### 后端服务
- [ ] `application-prod.yml` 已正确填写所有配置
- [ ] JAR 包已上传并使用 systemd 托管启动
- [ ] `GET /dashboard/stats` API 调用可正常响应（需携带 Token）
- [ ] Knife4j 文档 `https://your-api-domain.com/doc.html` 可访问

### 微信小程序
- [ ] `config.js` 中的 `baseUrl` 已更新为线上域名
- [ ] 微信公众平台已配置 request 合法域名
- [ ] 已在开发者工具中「构建 npm」
- [ ] 已上传代码并提交审核

---

## 五、常见问题排查

| 现象 | 排查方向 |
|------|---------|
| 小程序提示「登录过期」 | 检查后端 Redis 是否正常连接；检查 Token Header 名称是否为 `satoken` |
| 文件上传失败 | 检查 `client_max_body_size` Nginx 配置；检查存储策略配置是否正确 |
| 接口 HTTPS 报错 | 检查 SSL 证书有效期；微信公众平台合法域名是否已添加 |
| 数据库连接超时 | 检查安全组 3306 端口访问限制；MySQL 用户是否允许远程连接 |
| 看板数据为空 | 检查 Sa-Token 登录状态；确认 `user_id` 数据隔离逻辑正常工作 |
