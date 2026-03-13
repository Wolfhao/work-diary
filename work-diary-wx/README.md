# Work Diary WX（微信小程序端）

> 宠物博主商务接单管理平台 — 微信小程序

## 项目简介

Work Diary WX 是面向宠物博主的轻量级商务账本小程序，基于 **TDesign MiniProgram** 组件库构建，提供商单录入、状态追踪、资金（垫付/收入）看板等完整功能，专为移动端一手掌控接单全流程而设计。

---

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 小程序框架 | 微信原生 MINA | — |
| UI 组件库 | TDesign MiniProgram | ^1.11.2 |
| 样式 | LESS | — |
| 代码规范 | ESLint (Airbnb) + Prettier | — |
| Git Hooks | Husky + lint-staged | — |

---

## 功能页面

### 主包页面（TabBar）

| 页面 | 路径 | 说明 |
|------|------|------|
| 看板（首页） | `pages/home/index` | 展示垫付总额、待收回垫付、总收入、已到账收入等核心数据统计卡片 |
| 商单列表 | `pages/order/index` | 分页浏览所有商单，支持按状态/平台筛选 |
| 我的 | `pages/my/index` | 用户信息、个人设置 |

### 分包页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录 | `pages/login/login` | 微信快捷登录，调用 `wx.login()` 换取系统 Token |
| 发布商单 | `pages/release/index` | 录入新商单表单（标题、平台、垫付金、酬金、截图上传等） |

---

## 自定义组件

| 组件名 | 路径 | 说明 |
|--------|------|------|
| `card` | `components/card` | 数据统计展示卡片，用于首页看板各项指标 |
| `nav` | `components/nav` | 自定义底部 TabBar 导航栏 |
| `order-list` | `components/order-list` | 商单列表条目，含状态标签、金额、平台信息 |

---

## 项目目录结构

```
work-diary-wx/
├── app.js                    # 小程序入口（全局事件总线、更新管理）
├── app.json                  # 页面路由 & TabBar 配置
├── app.less                  # 全局样式
├── variable.less             # 全局 LESS 变量（颜色 / 字号 Token）
├── config.js                 # 全局配置（isMock 开关、API Base URL）
├── api/                      # HTTP 请求封装
│   └── index.js              # 统一请求方法，自动注入 satoken Header
├── behaviors/                # 可复用 Behavior（分页加载等）
├── components/               # 自定义组件
│   ├── card/                 # 看板卡片组件
│   ├── nav/                  # 自定义 TabBar
│   └── order-list/           # 商单列表条目组件
├── custom-tab-bar/           # 自定义 TabBar 实现
├── mock/                     # Mock 数据（开发阶段离线调试）
├── pages/                    # 页面（主包）
│   ├── home/                 # 看板首页
│   ├── order/                # 商单列表页
│   └── my/                   # 我的页面
├── pages/login/              # 分包：登录
├── pages/release/            # 分包：发布商单
├── utils/                    # 工具函数
│   └── eventBus.js           # 全局事件总线
├── static/                   # 静态资源（图标等）
└── miniprogram_npm/          # npm 构建产物（TDesign 等）
```

---

## 快速启动（本地开发）

### 前置条件

- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html) 最新版
- Node.js 16+（用于 lint/prettier）
- 已申请微信小程序 AppID

### 步骤

**1. 克隆并安装依赖**
```bash
git clone <repo-url>
cd work-diary-wx
npm install
```

**2. 构建 npm 依赖（TDesign）**

在微信开发者工具中点击：  
`工具` → `构建 npm`  
（构建后 `miniprogram_npm/` 目录会生成 TDesign 组件文件）

**3. 配置后端接口地址**

修改 `config.js`：
```javascript
const config = {
  isMock: false,          // false = 连接真实后端
  baseUrl: 'https://your-api-domain.com'
}
export default config;
```

**4. 导入项目**

在微信开发者工具：
- 点击 `+` → `导入项目`
- 目录选择 `work-diary-wx/`
- 填入你的 AppID

**5. 开启调试**
- 开发阶段可将 `isMock: true` 使用内置 Mock 数据离线调试
- 如后端为 HTTP，在开发者工具 `详情` → `本地设置` 中勾选「不校验合法域名」

---

## 接口联调说明

所有 API 请求通过 `api/index.js` 统一封装。登录成功后，Token 会自动写入本地存储，后续请求自动携带：

```
Header: satoken: <tokenValue>
```

### 接口参考

| 功能 | 请求方式 | 路径 |
|------|---------|------|
| 微信登录 | POST | `/wx/login` |
| 获取看板统计 | GET | `/dashboard/stats` |
| 商单分页查询 | POST | `/work-order/page` |
| 新增商单 | POST | `/work-order` |
| 修改商单 | PUT | `/work-order` |
| 删除商单 | DELETE | `/work-order/{id}` |
| 文件上传 | POST | `/file/upload` |

---

## 代码规范

```bash
# 检查 lint
npm run lint

# 自动修复格式
npm run lint:fix
```

- **ESLint**：基于 Airbnb 规则，针对微信小程序环境做了适配
- **Prettier**：统一代码格式化风格（`.prettierrc.yml` 定义）
- **Husky**：提交前自动执行 lint-staged，保障入库代码质量
