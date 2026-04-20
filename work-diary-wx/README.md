# Work Diary WX（微信小程序端）

> 个人工作台小程序 — 微信小程序

## 项目简介

Work Diary WX 是一个**个人工作台微信小程序**，基于 **TDesign MiniProgram** 组件库构建，聚合多个实用功能模块，为个人（博主/自由职业者）提供一站式的工作效率工具。

### 产品定位

| 模块 | 状态 | 实现方式 | 说明 |
|------|------|----------|------|
| 商单管理 | ✅ 已上线 | 前后端全栈 | 商单全生命周期管理、垫付/收入资金追踪、数据看板统计 |
| 旅游攻略 | ✅ 已上线 | 纯小程序端 | 静态攻略内容，无需后端接口 |
| 记账本 | 🚧 规划中 | 前后端全栈 | 聊天式 AI 记账、资产管理、账单报表 |

---

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 小程序框架 | 微信原生 MINA | — |
| UI 组件库 | TDesign MiniProgram | ^1.11.2 |
| 样式 | LESS | — |
| 代码规范 | ESLint (airbnb-base) + Prettier | — |
| Git Hooks | Husky + lint-staged | — |

---

## 项目目录结构

采用**主包 + 分包**结构，按功能模块划分目录：

```
work-diary-wx/
├── app.js / app.json / app.less  # 应用入口、路由配置、全局样式
├── variable.less                  # 全局 LESS 变量（颜色 / 字号 Token）
├── config.js                      # 环境配置（Mock 开关、API 地址）
├── api/                           # HTTP 请求封装（Token 注入、统一错误处理）
├── pages/                         # 主包页面（仅保留首页，保证快速启动）
├── modules/                       # 功能分包（按模块拆分，按需加载）
│   ├── workorder/                 # 商单管理模块
│   └── travel/                    # 旅游攻略模块
├── components/                    # 可复用自定义组件
├── custom-tab-bar/                # 自定义底部 TabBar
├── behaviors/                     # 可复用 Behavior（如 Toast）
├── utils/                         # 工具函数（EventBus、时间格式化等）
├── mock/                          # Mock 数据（离线开发调试）
└── miniprogram_npm/               # npm 构建产物（TDesign 等）
```

**约定**：每个页面/组件目录下统一包含 `index.js`、`index.json`、`index.wxml`、`index.less` 四个文件；新功能模块统一放在 `modules/` 下作为独立分包。

---

## 路由与分包配置

```
主包
  └── pages/home/index              # 看板首页（App 启动入口）

分包 workorder（root: modules/workorder）
  ├── index                         # 商单管理容器页
  ├── dashboard/index               # 数据看板
  └── release/index                 # 新建 / 编辑 / 查看商单

分包 travel（root: modules/travel）
  ├── index                         # 攻略列表
  └── pingyao/index                 # 平遥古城详情

TabBar（custom-tab-bar）
  ├── 看板     /pages/home/index
  ├── 商单     /modules/workorder/index
  └── 我的     /pages/my/index
```

**分包优势**：初始加载仅需主包，进入对应功能时动态加载分包，提升首屏速度。

---

## 全局应用入口（app.js）

### 生命周期

```
onLaunch()
  ├─ 1. 检查小程序版本更新 → 提示重启
  ├─ 2. handleLogin() — 检查本地 Token，无则执行微信授权登录
  ├─ 3. fetchUnreadNum() — 获取未读消息数
  └─ 4. connectSocket() — 建立 WebSocket 连接
```

### 全局数据（globalData）

| 字段 | 类型 | 说明 |
|------|------|------|
| `userInfo` | Object | 当前用户信息 |
| `unreadNum` | Number | 未读消息数量（驱动 Badge 显示） |
| `socket` | SocketTask | WebSocket 连接实例 |

### 全局事件

通过 `app.eventBus` 实现跨页面通信：

```javascript
// 发送
getApp().eventBus.emit('unread-num-change', count);

// 接收
getApp().eventBus.on('unread-num-change', (count) => {
  this.setData({ unreadNum: count });
});
```

---

## API 请求封装（api/request.js）

### 核心特性

- Promise 封装 `wx.request()`
- 自动从 Storage 读取 `Authorization` Token 并注入 Header
- 响应自动解包（返回 `data.data`，而非完整响应体）
- 401 自动触发重新登录，无需业务层处理

### 错误处理三层

```
HTTP 网络异常     → Toast: 网络请求失败
业务 code ≠ 200  → Toast: data.message
code === 401      → 清除 Token → 重新调用 doLogin()
```

### 导出方法

| 方法 | 说明 |
|------|------|
| `request(options)` | 基础请求，返回 Promise |
| `get(url, data)` | GET 快捷方法 |
| `post(url, data)` | POST 快捷方法 |
| `put(url, data)` | PUT 快捷方法 |
| `del(url)` | DELETE 快捷方法 |
| `doLogin()` | 微信授权登录（内部使用 Promise 防止并发重复调用） |
| `uploadFile(path)` | 上传文件，返回服务端存储路径 |
| `getImageUrl(key)` | 将服务端相对路径转换为完整 URL |

**Base URL**：`https://www.suntool.online`

**本地存储 Key**：`Authorization`（存储 Sa-Token 返回的 tokenValue）

---

## 页面分析

### 首页（pages/home/index）

**功能**：品牌展示 + 功能入口网格 + 待处理商单 Badge

**数据流**：
```
onShow()
  └─ GET /dashboard/stats
      └─ pendingCount = inProgressOrderCount
          └─ 显示红色 Badge
```

**功能卡片入口**：

| 卡片 | 跳转路径 |
|------|---------|
| 商单管理 | `/modules/workorder/index` |
| 旅游攻略 | `/modules/travel/index` |
| 记账本 | （待实现） |

**主题**：深色科技感（背景 `#0d1117`，品牌区紫色渐变）

---

### 商单管理容器页（modules/workorder/index）

**功能**：两个 Tab 的容器页 + 新建 FAB 按钮

```
┌─────────────────────────────────┐
│  自定义导航栏                    │
├─────────────────────────────────┤
│  [数据看板]    [商单列表]         │
├─────────────────────────────────┤
│                                 │
│   <workorder-dashboard />       │
│   或                            │
│   <workorder-order />           │
│                                 │
│                          [+]   │  ← FAB 新建
└─────────────────────────────────┘
```

**Tab 切换逻辑**：切换时通过 `selectComponent` 调用子组件的刷新方法。

---

### 新建/编辑/查看商单（modules/workorder/release/index）

**三种模式**（通过页面参数 `mode` 区分）：

| 模式 | 参数 | 说明 |
|------|------|------|
| `create` | 无 | 空表单，新建商单 |
| `edit` | `id` | 加载详情并回显，可修改 |
| `view` | `id` | 只读展示，含编辑跳转按钮 |

**表单数据结构**：

```javascript
formData: {
  id: null,                  // 编辑时有值
  title: '',                 // 商单名称 (required)
  platform: '',              // 合作平台（小红书/抖音等）
  advanceAmount: '',         // 垫付金额
  isAdvanceRecovered: 0,     // 垫付是否已收回 0/1
  incomeAmount: '',          // 预计收入/酬金
  isIncomeReceived: 0,       // 收入是否已到账 0/1
  description: '',           // 备注描述
  status: 10,                // 商单状态
  imageUrls: []              // 已上传图片的服务端路径数组
}
```

**图片处理流程**：
```
用户选择图片
  └─ image-uploader 逐张调用 uploadFile(tempFilePath)
      └─ POST /file/upload → 返回服务端代理路径
          └─ 文件对象中记录 _storePath
              └─ 提交时收集所有 status=done 的 _storePath
```

**API 调用**：

| 操作 | 接口 |
|------|------|
| 加载详情 | `GET /work-order/{id}` |
| 新建商单 | `POST /work-order` |
| 更新商单 | `PUT /work-order` |
| 删除商单 | `DELETE /work-order/{id}` |

---

### 旅游攻略（modules/travel/）

- **列表页**：读取 `config.js` 中的静态攻略数据，展示卡片列表
- **详情页**（平遥古城）：完整 2 天 1 夜行程规划，含交通/餐饮/景点/住宿信息
- 纯静态内容，无后端接口调用

---

## 自定义组件

### workorder-dashboard（数据看板）

**数据来源**：`GET /dashboard/stats`

**展示内容**（从上到下）：

```
欢迎卡片（含当前日期）
  ↓
垫付情况：总垫付 + 未收回垫付
  ↓
商单概况：进行中 + 已完成
  ↓
总收入 + 总商单数
  ↓
净利润 = 总收入 - 未收回垫付
```

---

### workorder-order（商单列表容器）

**功能**：Tab 切换 + 搜索 + 分页加载

**Tab 配置**：

| Tab | 过滤条件 | 说明 |
|-----|---------|------|
| 全部 | 无 | 所有商单 |
| 制作中 | `statuses=[10, 20]` | 待开工 + 制作中 |
| 待结款 | `status=30` | 待结款 |

**分页参数**：每页 10 条，下拉刷新重置，上拉触底加载更多

**缓存策略**：三个 Tab 各自缓存列表数据，操作后调用 `clearAllTabsCache()` 清空重新加载

**业务操作按钮**（卡片 Actions）：

| 操作 | 触发接口 |
|------|---------|
| 交付完毕 | `PUT /work-order { status: 30 }` |
| 核销垫付 | `PUT /work-order { isAdvanceRecovered: 1 }` |
| 确认收款 | `PUT /work-order { isIncomeReceived: 1 }` |

---

### order-list（商单卡片组件）

**Props**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `orderList` | Array | 商单数据列表 |
| `loading` | Boolean | 加载中状态 |
| `hasMore` | Boolean | 是否还有更多数据 |

**卡片布局**：

```
┌──────────────────────────────────────┐
│  [平台首字母头像]  商单标题    [状态标签] │
├──────────────────────────────────────┤
│  进度条：接单 ──── 制作 ──── 交付 ──── 结款 │
├──────────────────────────────────────┤
│  [垫付状态 Chip]   [收款状态 Chip]       │
└──────────────────────────────────────┘
```

**状态标签颜色**：

| 状态码 | 标签 | 颜色 |
|--------|------|------|
| 10/20 | 进行中 | 紫色 (primary) |
| 30 | 待结款 | 黄色 (warning) |
| 40 | 已完成 | 绿色 (success) |
| 90 | 已取消 | 灰色 (cancel) |

---

### image-uploader（图片上传组件）

**Props**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `fileList` | Array | `[]` | TDesign Upload 格式的文件列表 |
| `max` | Number | 5 | 最多上传张数 |
| `disabled` | Boolean | `false` | 是否禁用（查看模式） |

**事件**：

| 事件名 | payload | 说明 |
|--------|---------|------|
| `change` | `{ fileList }` | 文件列表变化时触发 |

**上传流程**：
```
选择图片 → 添加 loading 占位 → 并发上传多张
  → 各自完成后 status=done，记录 _storePath
  → 每次变化 emit change 事件
```

**防抖**：使用 `_emitting` 标志防止 observer 和 emit 相互循环触发。

---

### image-uploader 使用示例

```wxml
<image-uploader
  fileList="{{fileList}}"
  max="5"
  disabled="{{isViewMode}}"
  bind:change="onFileListChange"
/>
```

```javascript
onFileListChange(e) {
  this.setData({ fileList: e.detail.fileList });
},

// 提交时收集已上传文件路径
getValidImagePaths() {
  return this.data.fileList
    .filter(item => item.status === 'done')
    .map(item => item._storePath)
    .filter(Boolean);
}
```

---

## 样式体系

### 全局变量（variable.less）

```less
// 布局
@navbar-padding-top: 20px;
@nav-bar-height: 60px;
@tab-bar-height: 112rpx;

// 字体
@font-size-default: 16px;
@font-size-small:   14px;
@font-size-mini:    12px;

// 颜色
@bg-color:        #f3f3f3;
@brand7-normal:   #0052d9;
@gy1:             #000000e6;   // 主文字
@gy2:             #00000099;   // 次要文字
@gy3:             #00000066;   // 辅助文字
```

### 页面主题一览

| 页面/模块 | 背景 | 主色 |
|----------|------|------|
| 首页 Home | `#0d1117` 深灰蓝 | 紫色渐变 `#6366f1 → #8b5cf6` |
| 商单 release | `#fff5f8 → #f0eeff → #e8f8ff` 渐变 | 深紫 `#3a2f5f` |
| 商单 workorder | 同 release | 紫色渐变 `#c3a8f0 → #845ec2` |

---

## 全局状态管理

| 机制 | 用途 | 位置 |
|------|------|------|
| `App.globalData` | userInfo、unreadNum、socket | app.js |
| `wx.getStorageSync('Authorization')` | API Token | 本地存储 |
| `EventBus` | 跨页面事件通信 | utils/eventBus.js |

**EventBus API**：

```javascript
const app = getApp();
app.eventBus.on('event-name', callback);   // 监听
app.eventBus.emit('event-name', data);    // 触发
app.eventBus.off('event-name', callback); // 取消
```

---

## Mock 数据系统

**启用方式**：修改 `config.js` 中 `isMock: true`

**拦截原理**（mock/WxMock.js）：重写 `wx.request()`，匹配已注册 URL 则返回本地数据，未匹配则透传到真实服务器。

**WebSocket 模拟**（mock/chat.js）：
- 模拟延迟 500ms 建立连接
- 收到消息后 3 秒自动回复

---

## 完整商单工作流

```
1. 首页
   GET /dashboard/stats → 显示 Badge（待处理商单数）

2. 进入商单管理
   ├─ [数据看板] GET /dashboard/stats → 展示统计指标
   └─ [商单列表] POST /work-order/page → 分页列表

3. 列表操作
   ├─ 点击卡片 → 跳转 release 页（view 模式）
   ├─ 交付完毕 → PUT /work-order { status: 30 }
   ├─ 核销垫付 → PUT /work-order { isAdvanceRecovered: 1 }
   └─ 确认收款 → PUT /work-order { isIncomeReceived: 1 }

4. 新建商单
   ├─ 跳转 release 页（create 模式）
   ├─ 选择图片 → POST /file/upload（逐张上传）
   └─ 提交 → POST /work-order

5. 编辑商单
   ├─ GET /work-order/{id} → 回显数据
   └─ 保存 → PUT /work-order
```

---

## 快速启动（本地开发）

### 前置条件

- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html) 最新版
- Node.js 16+
- 已申请微信小程序 AppID

### 步骤

**1. 安装依赖**
```bash
cd work-diary-wx
npm install
```

**2. 构建 npm 依赖（TDesign）**

在微信开发者工具：`工具` → `构建 npm`

**3. 配置后端地址**

修改 `api/request.js` 中的 `BASE_URL`，或使用 Mock：
```javascript
// config.js
const config = {
  isMock: false,   // true = 离线 Mock，false = 连接真实后端
};
```

**4. 导入项目**

微信开发者工具 → 导入项目 → 选择 `work-diary-wx/` → 填入 AppID

**5. 本地开发技巧**
- HTTP 后端：`详情` → `本地设置` → 勾选「不校验合法域名」
- 开启 Mock：`isMock: true` 可完全离线调试

---

## 如何新增 API 调用

```javascript
import { get, post, put, del } from '../../api/request';

// GET 示例
get('/dashboard/stats').then(data => {
  this.setData({ stats: data });
});

// POST 示例（错误已自动 Toast，无需 catch）
post('/work-order', formData).then(data => {
  wx.showToast({ title: '创建成功' });
  wx.navigateBack();
});
```

---

## 如何新增页面

1. 在 `pages/` 或 `modules/<分包>/` 下创建目录（含 `.js/.json/.wxml/.less`）
2. 在 `app.json` 中注册：
   - 主包：加入 `pages` 数组
   - 分包：加入对应 `subpackages[].pages` 数组
3. 使用 `wx.navigateTo({ url: '/modules/xxx/index' })` 跳转

---

## 代码规范

```bash
npm run lint        # ESLint 检查
npm run lint:fix    # 自动修复格式问题
```

- **ESLint**：airbnb-base，适配微信小程序环境（允许大写函数无 new、关闭循环导入检查）
- **Prettier**：统一代码格式（`.prettierrc.yml`）
- **Husky**：提交前自动执行 lint-staged，保障入库代码质量