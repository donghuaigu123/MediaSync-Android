# 媒体同步 · MediaSync Android

<img src="docs/app-icon.png" width="96" alt="媒体同步应用图标" />

一款**原生 Android 客户端**，把「媒体同步」的多平台媒体解析、批量下载、订阅同步、直播录制与文件管理装进口袋。

- **应用名**：媒体同步
- **包名**：`cn.mediasync.app`
- **版本**：v1.0.0（versionCode 1）
- **要求**：Android 7.0（API 24）及以上
- **成品**：[`releases/媒体同步.apk`](releases/媒体同步.apk)（已签名，可直接安装）
- **开源协议**：[MIT](LICENSE)

---

## 重要声明

本项目是**独立实现**的 Android 客户端，不是 `lincolnpark2000/MediaSync` 的官方版本，也与其作者无关联。

在立项前核查发现：

| 项目 | 实际情况 |
| --- | --- |
| 原仓库内容 | 仅 `README.md`、`docker-compose.mediasync.yml`、`guides/` 文档、`image/` 截图 |
| 源代码 | **未公开**（无 `frontend/`、`backend/`，GitHub API 返回 404） |
| 开源许可证 | **无**（`license: null`，即保留所有权利） |
| 官方分发形式 | Docker 镜像 `lincolnpark2000/mediasync` 与 Windows 桌面安装包 |
| Android / APK | 官方从未提供，也不存在可改造的 Android 工程 |

因此本项目**没有复制、改造或包含原项目的任何代码、图片与素材**，而是依据其 README 中公开描述的功能设计，用 Java + Android Framework 从零编写。原项目名称仅用于说明功能参考来源。本项目以 MIT 许可证开源。

同时，本客户端设计了**连接自托管服务**的工作模式：当你自己部署了 MediaSync 服务（Docker / Windows 版）后，在 App 内填入服务地址，即可通过内嵌 WebView 使用其完整 Web 界面；平台登录态、高清画质与合集解析等能力仍由原服务提供，本 App 不绕过也不破解任何平台限制。

---

## 功能

| 模块 | 本地模式（无需服务） | 连接自托管服务后 |
| --- | --- | --- |
| 媒体解析 | 平台识别；通用媒体直链 / M3U8 / 图片 / 音频直链解析 | 转发服务端解析接口，读取标题、封面与多档画质 |
| 批量下载 | 多行链接批量入队 | 由服务端解析后下载 |
| 下载任务 | 并发队列、断点续传、进度、暂停 / 继续 / 取消 / 重试、通知栏进度 | 同左 |
| 画质与格式选择 | 直链原始画质 | 服务端返回的格式列表，逐条或全部加入队列 |
| 订阅同步 | 订阅管理、定时扫描调度、本地记录 | 调用服务端增量扫描接口获取新增内容 |
| 直播监控录制 | 房间管理、监控状态 | 服务端实际执行录制；Web 界面集中管理 |
| 文件浏览 | 打开 / 播放 / 分享 / 删除本地文件 | Web 界面内置文件浏览器 |
| 关注收藏 / 聚合搜索 | — | 跳转 Web 界面对应页面 |
| 通知 | 下载完成、失败、订阅扫描通知；Telegram / 企业微信配置保存 | 由服务端发送远程命令通知 |
| 设置 | 服务地址、解析接口、并发、仅 WiFi、代理、默认画质、深色模式 | 同左 |
| 备份 | 配置 / 订阅 / 直播间 / 历史 JSON 导出导入、剪贴板传输 | 同左 |

底部导航覆盖 **解析 / 任务 / 订阅 / 直播 / 文件** 五个模块，顶部提供「完整版」（内嵌 Web UI）与「设置」入口；支持从系统分享面板直接把链接送入 App 解析。

---

## 安装与使用

1. 下载 [`releases/媒体同步.apk`](releases/媒体同步.apk)，在 Android 设备上安装（需允许「安装未知来源应用」）。
2. **本地模式**：打开 App →「解析」→ 粘贴一个通用媒体直链（例如 `https://media.w3.org/2010/05/sintel/trailer.mp4`，也可点「示例直链」自动填入）→「解析链接」→「下载」。到「任务」页查看进度，到「文件」页打开或分享。
3. **完整模式**：在「设置 → 服务连接」填入自托管地址（如 `http://192.168.1.10:4399`）→ 保存 → 点顶部「完整版」。App 会加载 MediaSync 的完整 Web 界面；Web 内触发的下载也会交给本地下载引擎，享受断点续传与任务管理。
4. **订阅 / 直播**：在对应页面添加链接，可设置扫描间隔、手动扫描、暂停 / 启用。自动下载与自动录制需要连接服务端。

> 说明：B 站、抖音、YouTube 等平台的登录态、反爬策略与高清格式解析都在服务端完成，纯客户端无法独立实现。本 App 对这类链接会明确提示需要连接服务，而不是伪装成"已解析"。

---

## 从源码构建

### 方式 A：一键脚本（推荐，无需 Gradle）

环境要求：JDK 11+、Android SDK `platforms;android-35` 与 `build-tools;35.0.0`、`zip`。

```bash
export ANDROID_SDK_ROOT=/opt/android-sdk   # 按你的实际路径修改
chmod +x build.sh
./build.sh
```

脚本依次完成：资源编译（`aapt2`）→ 生成 `R.java` → `javac` 编译 → `d8` 转 dex → 合并进 APK → `zipalign` → 生成自签名证书并 `apksigner` 签名 → 校验。产物为 `releases/媒体同步.apk`。

> 首次构建会在 `build/` 下生成调试用的 `mediasync-release.jks`（已被 `.gitignore` 忽略）。若要正式分发，请替换为自己的密钥库。

### 方式 B：Android Studio / Gradle

项目同时提供了标准的 Gradle 工程结构（`settings.gradle`、`build.gradle`、`app/build.gradle`，AGP 8.6.1）。用 Android Studio 打开根目录即可自动补齐 Gradle Wrapper 并构建。

---

## 目录结构

```
MediaSync-Android/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/cn/mediasync/app/
│   │   ├── App.java / MainActivity.java       # 应用入口与主容器
│   │   ├── core/                              # 配置、解析、下载引擎、通知、定时扫描、UI 工具
│   │   ├── db/Db.java                         # SQLite（任务 / 订阅 / 直播间 / 历史 / 备份）
│   │   └── ui/                                # 7 个页面：解析、任务、订阅、直播、文件、设置、完整版
│   └── res/                                   # 布局、主题、矢量图标、多密度启动图标
├── build.sh                                   # 无 Gradle 的一键打包脚本
├── docs/app-icon.png                          # 应用图标
├── releases/媒体同步.apk                       # 已签名成品
├── README.md / LICENSE
```

**技术栈**：Java 11、Android Framework（无 AndroidX、无第三方依赖）、SQLite、`HttpURLConnection` + `RandomAccessFile` 断点续传、前台 Service + `AlarmManager`、WebView。全部代码与资源均在此仓库中，可离线构建。

---

## 与自托管服务的接口约定

客户端只依赖两个可选的 HTTP GET 接口，路径均可在「设置」中修改；接口不可用时会降级为本地模式并给出明确提示，不会导致 App 崩溃。

| 用途 | 默认路径 | 期望返回 |
| --- | --- | --- |
| 解析链接 | `{server}/api/parse?url=...` | JSON，含 `title`、`thumbnail`，以及 `formats[]`（每项 `url` / `format` / `mime`），或直接返回 `url` |
| 订阅扫描 | `{server}/api/subscriptions/scan?sync=1` | JSON，含 `new_count`（也兼容 `new` / `added`） |

若你的服务端路径不同，在「设置 → 解析接口路径」中调整即可；也可以完全不填服务地址，只使用本地直链下载。

---

## 合规与免责

- 请仅下载和保存自己创作、获得授权或依法可使用的内容，并遵守来源平台的使用规则。媒体版权归原作者或权利人所有。
- 本项目不提供、不鼓励绕过 DRM、付费内容限制或平台访问控制。
- 本项目按「现状」提供，不附带任何明示或暗示的担保。

## 许可证

[MIT License](LICENSE) © 2026 donghuaigu123

功能设计参考自 [lincolnpark2000/MediaSync](https://github.com/lincolnpark2000/MediaSync) 的公开介绍；该仓库未附带开源许可证，且本项目未使用其任何代码或素材。
