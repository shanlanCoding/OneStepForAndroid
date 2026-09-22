# Findings

## 初始状态
- 工作目录：`E:\Sources\Android\OneStepForAndroid-main`
- 根目录未发现 `task_plan.md`、`findings.md`、`progress.md`，已为本次任务新建。
- 初次检查时 Git 尚未识别工作树；后续确认 `.git` 已存在，但全部项目文件均未跟踪，仍无可用提交基线。
- 记忆索引中没有 OneStepForAndroid 相关记录，本次结论将完全来自当前工作区。

## 待确认
- 项目文件与模块结构
- Gradle/Android 构建配置
- 应用入口与核心业务流程
- 外部依赖、权限、系统要求和扩展点

## 旧快照构建与模块（5537a19）
- Gradle 工程名为 `OneStep4.0`，包含 `:app` 和 `:xposed-api-stubs` 两个模块。
- Android Gradle Plugin 9.2.1，Java 11，`compileSdk 36.1`，`minSdk 24`，`targetSdk 36`。
- 应用 ID/namespace 均为 `com.sangluo.onestep`，旧快照构建配置版本为 `1.0.1`（versionCode 8）。
- `:app` 依赖 AppCompat、Core KTX、Material、ViewPager2；Aliuhook 与本地 Xposed API stubs 只用于编译，Aliuhook runtime 另行解包供 Zygisk 使用。
- 主源码约 67 个 Java 文件，单元/仪器测试 24 个，资源文件约 272 个。

## 运行形态
- `MainActivity` 注册为 HOME/DEFAULT，是主桌面入口；`HomeRedirectActivity` 是 LAUNCHER 跳转入口。
- Manifest 同时声明 Xposed 模块元数据及 NotificationListenerService。
- 核心权限多为 signature/privileged 级别，包括任务管理、可信显示、媒体控制、状态栏和日志读取；普通 APK 安装无法完整运行。
- 仓库带有 Magisk、KSU、状态栏 overlay、Zygisk 和 root priv-app 的构建/部署脚本。

## 工作区状态
- `.git` 已存在，但所有项目文件均显示为未跟踪，当前没有可依赖的已提交基线。
- 根目录现有文本以 UTF-8 可解码；项目文本主要为 LF，仅 `gradlew.bat` 使用 CRLF。
- README、Gradle 配置、Manifest、MainActivity、RootVirtualDisplayHost 和主要打包脚本已通过严格 UTF-8 解码检查。

## 应用架构
- `MainActivity`（约 5684 行）是主要编排层，直接维护 `windowApps`、`windowViews`、`embeddedHosts` 三组固定槽位数组，以及主槽、侧槽顺序、多窗口模式、动画和异步任务状态。
- `EmbeddedAppHost` 定义 `attach/start/release/dispatchBack/dispatchTouch` 等宿主协议。
- `createEmbeddedHost()` 优先创建 `RootVirtualDisplayHost`；若不具备相应路径，再反射创建 `HiddenActivityViewHost` 作为系统特权环境后备。
- `RootVirtualDisplayHost` 是应用进程侧控制器，通过 root shell 启动桥接进程，并经 Binder 客户端管理虚拟显示、任务、Surface、输入和旋转。
- `AppLaunchPlacement` 是纯策略：主窗口为空则进主窗口；有空侧槽则进入侧槽并提升；侧槽满时替换主窗口；内置桌面占槽时优先替换桌面槽。
- `WindowLayoutCalculator` 以纯函数计算横向/纵向主侧窗口 Rect；`WindowAnimationController` 独立负责切换动画。

## 设置与兼容
- 设置保存在 `onestep_settings` SharedPreferences，由 `OneStepSettingsStore` 集中读写。
- 设置模型包括桌面网格、顶部应用图标/间距、顶部组件、状态栏留白、横纵窗口布局、侧窗数量、触发区域/灵敏度、日志开关等。
- Store 保留旧键迁移逻辑，例如旧媒体可见、图标 dp、导航高度和角落触发尺寸；二次开发不能直接删除这些兼容读取。

## Root、Binder 与 Hook
- `RootVirtualDisplayHost` 通过单线程 root executor 管理桥接启动/连接，持有 `SurfaceView`、虚拟显示 ID、宿主 task ID、输入队列、旋转与 IME 策略等状态。
- `RootVirtualDisplayBridge` 发布 Binder 服务，服务名按应用 UID 区分；调用同时校验 caller UID 和随机 token，并管理虚拟显示、Surface、任务观察和跨应用启动路由。
- `RootInputBridge`/`RootInputBridgeClient` 负责向指定虚拟显示注入 MotionEvent/按键等输入；输入与显示管理是两个独立通道。
- Root 虚拟显示包含 trusted、own-focus、touch、rotate-with-content 等隐藏 flag，并处理不同 Android 版本的隐藏 API 差异。
- Zygisk 原生模块仅构建 `arm64-v8a` 与 `armeabi-v7a`，在 `system_server` 加载 APK 内 Hook 类。
- 三类 Hook 分别处理安全窗口承载、状态栏 overlay、主 HOME/虚拟显示行为；均可通过模块目录 marker 禁用。
- Zygisk 检测到有效 LSPosed 模块或明确选择 LSPosed backend 时跳过独立 Aliuhook/LSPlant 注入，避免双后端。
- 生命周期入口为 `onCreate`，`onResume` 负责回到前台后的同步，`onPause/onStop` 收敛状态，`onDestroy` 释放宿主、线程、监听和系统 UI。

## 构建、打包与部署
- Gradle Wrapper 为 9.4.1；Android 构建主要使用 `:app:assembleDebug`，单元测试使用 `:app:testDebugUnitTest`。
- Magisk/KSU 打包脚本会先 `:app:clean`，再以 `--no-build-cache --rerun-tasks` 构建 APK 与 Zygisk runtime，随后构建双 ABI Zygisk so 和状态栏 overlay。
- 打包产物写入 `dist/OneStep4-<version>-<magisk|ksu>-<timestamp>.zip`，并验证 ZIP 内 APK SHA-256 与本次 APK 完全一致。
- Root ADB 安装脚本会直接改写 `/system/priv-app/OneStep4` 与 `/system/etc/permissions`，调整 hidden API policy 并重启，只适用于可写 system 的测试设备。
- `build-statusbar-overlay.sh` 依赖 Bash、Android SDK、可执行的 `aapt2` 和 platform `android.jar`。
- `.gitignore` 忽略 `docs/`，正式二次开发文档应置于根目录，避免被默认排除。

## 测试覆盖
- 单元测试覆盖窗口分配、跨应用路由保留契约、HOME Hook 条件、状态栏 overlay 崩溃保护/路径策略、虚拟显示 flag/owner、设置边界与旧值迁移、任务解析、返回退出策略、方向映射、导航/时长格式和 Zygisk 配置解析。
- 仪器测试覆盖基础 Android 环境与 MotionEvent 编解码；真实 Root、system_server Hook、虚拟显示 Surface、输入注入仍需设备验证。

## UI 与功能数据流
- `TopPanelController` 管理顶部导航、计时器、秒表、录音和媒体分页卡片，生命周期由 MainActivity 的 resume/pause 转发。
- `MediaPlaybackPanel` 使用 `MediaSessionCoordinator` 查询 active sessions，同时以 `MediaNotificationListenerService` 的通知快照补足媒体信息；可切换媒体源、控制播放和队列。
- NotificationListener 还从系统通知识别计时器、秒表、录音状态及对应 action；这依赖系统/ROM 的通知格式，修改分类规则需保留降级逻辑。
- `AmapNavigationClient` 绑定高德 `com.autonavi.minimap.service.NAVIGATION_SERVICE`，解析 type=4 的 JSON 导航状态并设过期清理；需要高德包、查询声明和专用权限。
- `SessionLogRecorder` 将筛选后的 logcat 写入应用 cache 的 `session_logs`，导出时 Android 10+ 使用 MediaStore 写 Downloads，旧版走传统存储路径。
- `SettingsPanelController` 是代码构建 UI，除了外观/布局设置，还包含 Root 授权、Hook 开关、内置桌面选择、系统 HOME 设置和法律声明。
- 主要界面没有 XML layout，视图树基本由 Java 动态构造；新增 UI 时需要同时关注 dp 计算、窗口 Rect、Surface z-order、动画期间延迟任务和释放逻辑。

## 维护风险
- `MainActivity` 和 `RootVirtualDisplayHost` 体积很大、状态耦合高；新增功能应优先放入现有 feature/system/ui 边界，通过 callback 接入。
- 多处依赖隐藏 API、反射、shell 输出和 ROM 私有行为；必须保留失败降级、超时、代际 token 和异步线程边界。
- 跨应用启动策略明确保留 SAF、拍照/选择器及 `FLAG_ACTIVITY_FORWARD_RESULT` 契约，不得将所有 intent 无差别重路由。
- Root/system_server 改动存在开机循环与 SystemUI 崩溃风险；状态栏 Hook 已有 crash guard，新增 Hook 也应具备禁用 marker 和恢复路径。

## 2026-08-03 上游同步诊断
- 当前受跟踪工作区与上游提交 `5537a190b418825ae38076fb4f9946f441b358e9` 完全一致，排除用户源码修改。
- 旧提交日期为 2026-08-01 01:41:11，主题为“增加状态栏显示开关”，版本为 1.0.1。
- `origin/main` 与 `upstream/main` 当前均为 `1336a4d889e8f4231affc73b37e6c835d8b58ca1`，比旧快照前进 23 个提交，版本为 1.0.3。
- 85 个工作区差异是旧快照与最新远端之间的版本差，不是用户改动；更新后需重新审查文档。
- 上游 23 个提交主要覆盖 Android 7-17、HyperOS、折叠屏/平板、虚拟显示尺寸、输入法、返回逻辑及窗口闪烁兼容。
- `origin/main` 共 484 个文件；Git 二进制检测识别出 PNG/WebP/JPG、Gradle Wrapper JAR、Zygisk SO/O 等二进制，其他源码、资源 XML、脚本和配置将按严格 UTF-8 校验。
- 上游 `.gitattributes` 仅强制 `*.bat` 使用 CRLF，其余文本应保留仓库 blob 的换行形式。
- 远端解包校验实际得到 469 个文件，其中 229 个 Git 文本文件严格 UTF-8 解码全部通过，240 个为二进制；文本中无 BOM、U+FFFD 或常见错误转码标记。
- 更新后 `git diff` 与 `git diff --cached` 均为空，仅 `DEVELOPMENT.md`、`task_plan.md`、`findings.md`、`progress.md` 保持未跟踪。
- 恢复分支 `snapshot/old-source-20260801` 固定旧源码提交 `5537a19`；当前 `main`/`origin/main` 为 `1336a4d`。

## 2026-08-03 最新版初步分析
- 相对旧快照，最新版涉及 85 个文件，新增 5,374 行、删除 446 行；主源码 87 个文件、JVM 测试 38 个文件、仪器测试 2 个文件、资源 273 个文件。
- `README.md` 与 Gradle 均显示版本 `1.0.3`、versionCode `11`；Android/Gradle/JDK/ABI 基线未发生根本变化。
- 最新 `app/build.gradle.kts` 的 `minSdk` 已提升到 29，和 README 仍写 Android 7/API 24 的用户文案存在不一致；开发文档必须以构建配置为准并明确此差异。
- 新增多组可独立测试的兼容策略：虚拟显示 density/viewport、窗口布局模式与主窗全屏、宿主输入/返回/Surface 复用、默认 HOME 路由、运行任务解析、Root bridge 兼容/失败识别。
- 新增 HyperOS 默认桌面与手势导航绕过 Hook，以及 Root 虚拟显示兼容 Hook；`xposed_scope.xml` 范围不再只有 `android`，文档必须按实际 scope 与入口更新。
- 现有 `DEVELOPMENT.md` 仍基于 1.0.1，且部分宿主优先级、Hook 种类和测试覆盖描述已过时，不能继续作为当前版本文档交付。
- `WindowLayoutModePolicy` 以 `smallestScreenWidthDp >= 600` 判断大屏，并在归一化宽高比至少 0.72 时启用双主窗；`MainPaneFullscreenPolicy` 选择更靠工作区外缘的主槽进入全屏。
- `VirtualDisplayDensityPolicy` 将手机虚拟屏稳定在约 393dp 逻辑宽度；大屏按宿主 density/质量缩放，可用 600dp 最小短边与 800dp 目标短边控制平板密度/像素放大。
- `VirtualDisplayViewportPolicy` 在非双主窗时使用稳定工作区规格，并在大屏、进入或离开双主窗时触发容器尺寸重算；内置桌面由 `FixedViewportFrameLayout` 固定逻辑 viewport 后缩放。
- Android 11 禁止向已移除虚拟显示注入 HOME，并跳过运行时 SELinux policy 重载；Android 17 及以上不再用 Presentation 降低宿主焦点，避免 hosted task 停止。
- 新的宿主策略分别管理 Back 分发、输入焦点、Surface 复用验证和系统手势 inset 保留；这些纯策略均有 JVM 测试。
- `RunningTaskAppResolver` 按 userId 和 launch/original/base/top component 解析运行实例，精确组件优先，随后按包名回退，支持多用户状态角标。
- `SystemServiceFailurePolicy` 识别 DeadObject/DeadSystem 失败并允许 bridge 重新连接；`RootVirtualDisplayCompatPolicy` 仅对 Android 10-13 的 root UID 绕过显示包名校验。
- LSPosed scope 现包含 `android`、`com.android.settings`、`com.android.systemui`、`com.miui.home`；除原三类 Hook 外还会安装 Root 虚拟显示兼容及 HyperOS 默认桌面/手势导航 Hook。
- 最新构建基线为 `minSdk 29`、`targetSdk 36`、`maxSdk 37`、versionCode 11/versionName 1.0.3，并新增 RecyclerView 1.3.1；README 的 Android 7/API 24 徽章与 Gradle 实际安装范围不一致。
- `TopAppListPolicy` 首次配置时默认选择全部可启动应用；已有配置保留已知顺序/选择，删除失效项，并把新安装应用追加且默认选中。
- Magisk 打包现将 `post-fs-data.sh` 纳入模块 ZIP 和必需项校验；Magisk/KSU 脚本末尾明确打印中文 APK SHA-256 校验值与模块输出路径。
- `MainActivity.createEmbeddedHost()` 的实际顺序仍是先构造并选择可用的 `RootVirtualDisplayHost`，失败后才尝试 `HiddenActivityViewHost`；README 所称“系统宿主优先”与代码不一致，开发文档以源码控制流为准。
- `RootVirtualDisplayHost` 只有在检测到 su 或 system/priv-app 权限时可用；虚拟显示创建、trusted flag、显示启动权限或桥接失败均会写入 `unavailableReason` 并进入降级路径。
- 内置 OneStep 桌面现包在 `FixedViewportFrameLayout` 中并 crop-to-fill；顶部快捷条为每个应用显示运行状态角标，应用列表刷新时同步合并顶部选择配置。
- 文档应把物理 `WindowLayoutCalculator`、逻辑 `VirtualDisplayViewportPolicy`、density `VirtualDisplayDensityPolicy`、Surface fixed size 与触摸坐标描述为同一跨层契约；只改一层会导致闪屏、重建、拥挤或误触。

## 2026-08-03 最新版构建验证
- 使用代理清空、关闭本机 Java 自动探测、允许下载完整 Java 21 工具链并加 `--rerun-tasks` 后，Gradle 9.4.1 在 2 分 37 秒内成功执行 43 个 task。
- JVM 测试报告包含 38 个 suite、148 个 test，0 failures、0 errors、0 skipped。
- Debug APK 元数据为 `com.sangluo.onestep`、1.0.3、versionCode 11、minSdkForDexing 29；文件大小 15,139,941 bytes，SHA-256 为 `28040412D751B076EF680B7FF903AC9D2A7E45A0350DA4A5FD1A97129BB5F1E1`。
- Gradle 报告使用了未来与 Gradle 10 不兼容的 deprecated features；本次 Gradle 9.4.1 构建成功，但升级 Wrapper 前应使用 `--warning-mode all` 单独清理。

## 2026-08-03 全屏状态栏重叠任务
- 目标设备序列号为 `58a9fb4b`，设备日志位于 `/sdcard/Download/`，文件名以 `OneStep4-log-` 开头。
- 现象限定为“小窗口全屏之后”：顶部状态栏覆盖应用内容，目标是为状态栏预留高度。
- 当前源码与 `origin/main` 对齐；开始任务时仅有未跟踪的 `开发笔记.md`，必须保持不动。
- 既有设置模型包含“状态栏留白”，同时最新版有 `MainPaneFullscreenPolicy` 和大屏/双主窗布局策略；实现前需确认全屏路径是否绕过或重复使用该留白。
- 本轮规划文件均经严格 UTF-8 解码确认，无 BOM，换行风格为 LF。
- 设备在线，最新日志为 `OneStep4-log-20260803-214923-492.txt`，1,306,940 bytes；拉取到系统临时目录后严格 UTF-8 解码通过、无 BOM。
- 设备日志在 OneStep task 上报告 `excludeMiuiStatusBar = true`，全屏切换附近多次出现顶部 Insets 高度 `127`；实时 `dumpsys window` 也显示普通应用 `mAppBounds=Rect(0, 127 - 1080, ...)`。
- OneStep 托管虚拟显示当前尺寸为 `1080x2266`，其 Window/Surface 内容坐标从 `(0,0)` 开始；物理状态栏显示后若宿主主槽仍从 `y=0` 布局，顶部 `127 px` 会直接覆盖托管应用画面。
- `completeExitOneStepMode()` 先将 `multiWindowMode=false` 并显示状态栏，再调用 `applyWindowLayout(true)`；状态栏显示与窗口矩形更新是两个独立步骤。
- `updateCornerTriggerBounds()` 已在非多窗口模式显式使用 `getStatusBarHeight()` 作为 topMargin，说明全屏交互层已经承认顶部系统栏占位；主窗口矩形尚需同样契约。
- 窗口计算器此前在所有非多窗口分支硬编码活动主槽为 `(0,0)-(workspaceWidth,workspaceHeight)`；这是状态栏显示后仍覆盖内容的直接代码原因。
- 修复采用模式化顶部 inset：多窗口继续使用现有顶部组件高度，全屏使用状态栏与刘海的安全高度；计算器统一裁剪顶部边界并保持底边不变。
- 完整 JVM 测试共 39 个 suite、151 个 test，0 failure/error/skipped；Debug APK 构建成功，SHA-256 为 `F613DA01AE90D52B2ED11A01955EB1C8B3E0BCA7A95E0978C3AEED9E3B560A15`。
- 设备现装包位于 `/system/priv-app/OneStep4/OneStep4.apk`，证书 SHA-256 为 `b4302c...dfc3c`；新 APK 证书为 `40a61a...22b8e`，`adb install -r` 因 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 拒绝。
- 安装失败后没有卸载、清数据或写入系统分区；设备现装版本仍为 1.0.3/versionCode 11。缺少匹配私钥时不能完成可信的真机运行验证。
- 用户补充原交付物为 `E:/下载/OneStep4-1.0.3-magisk-20260802-205321.zip`，因此现装 `/system/priv-app` 很可能来自 Magisk 模块挂载；需要按完整模块而非单 APK 路径继续核验。
- 旧 ZIP 的模块 ID 为 `onestep40_privapp`，包含 APK、privapp 权限 XML、状态栏 overlay、SELinux 规则、Zygisk 双 ABI payload/runtime 及 Magisk 生命周期脚本，不能用单 APK 等价替代。
- 设备 `/data/adb/modules/onestep40_privapp` 与旧 ZIP 元数据一致；模块 APK、当前 `/system/priv-app/OneStep4/OneStep4.apk` 和旧 ZIP 内 APK 的 SHA-256 均为 `EBBE0BBAFD44B540C222252350F932E0503F65CD7041B0B420BB73185CE67CAC`。
- 设备 hook 配置保留 `enable-hyperos-third-party-gesture` marker；重新安装模块时 `customize.sh` 会迁移该 marker。
- 完整模块更新仍受 APK 签名约束：旧模块证书为 `b4302c...dfc3c`，本机新构建为 `40a61a...22b8e`。Magisk 挂载绕过普通安装步骤，但不能让 PackageManager 接受系统包签名变化。
- Google 官方 NDK r29 Windows 包大小 833,850,862 bytes，SHA-1 与 repository2-3.xml 公布值 `ab3bb30fbb9e6903666d60c55d11e78b04e07472` 一致；已解压为 `D:/software/androidSDK/ndk/android-ndk-r29`，下载压缩包已删除。
- 完整模块 `dist/OneStep4-1.0.3-magisk-20260803-222302.zip` 已从当前源码重编 APK、Zygisk runtime、双 ABI payload 和状态栏 overlay；ZIP 为 7,710,273 bytes，SHA-256 `FF164AE316CA96A2FD22031CA4C1F5EA53AEEDF1F0D2E21557D04DAA387D8489`。
- ZIP 内 APK SHA-256 为 `F613DA01AE90D52B2ED11A01955EB1C8B3E0BCA7A95E0978C3AEED9E3B560A15`；37 个条目无重复，12 个文本严格 UTF-8，必需 payload 均存在，无静态顶层 `zygisk/`，Unix 权限与旧模块一致。
- 全量 NDK 构建更新了仓库内已跟踪的 `zygisk/build` 二进制/对象产物；这些是当前 NDK r29 构建结果，不是状态栏业务源码改动。

## 2026-09-22 小米15 Ultra 适配任务

### 设备与任务状态
- 目标设备 58a9fb4b（开发笔记中的小米15 Ultra）当前不在线：IPv6 link-local 与局域网 `192.168.123.137:5555` 均连接失败；当前 USB 仅连接小米6（`e5a2f9b5`，sagit）。
- `开发笔记.md` 记录三个 bug：①小窗口全屏后状态栏覆盖内容（本地 Phase 9 已修）；②一步左右上角触发区域拦截点击，导致全屏应用角落无法点击；③微信在小窗口运行时输入框经常无法获取焦点、键盘不弹。
- 2026-08-03 拉取的设备日志（含 `OneStep4-log-20260803-225115-935.txt`）已不在系统临时目录，本轮无法取得运行时 IME 证据。

### 触发区域拦截点击的根因（Bug 2，源码证据）
- `MainActivity.createCornerTrigger()` 给透明触发 View 设置 `setOnTouchListener`，所有 action 一律返回 `true`：DOWN 被消费后整条手势流归触发区域，纯点击永远无法到达下层主槽 `SurfaceView`（`RootVirtualDisplayHost.onTouch` → `injectMotionDirect` 注入虚拟显示）。
- 正确机制：`Activity.dispatchTouchEvent()` 是唯一能"先观察后放行"的层级；上游 `8b568f9` 即采用此方案——dispatch 层用 `CornerTriggerGesturePolicy.matches` 检测拖拽，匹配时向子视图补发 `ACTION_CANCEL` 再 `enterOneStepMode`，点击则自然穿透到宿主 SurfaceView，`findCornerTrigger` 用 `getLocationOnScreen` 命中判定。

### 微信输入焦点的机制链路（Bug 3，源码证据）
- 触摸 DOWN 时 `touchFocusRequestGeneration = ++focusRequestGeneration`，焦点请求与 DOWN 在同一串行注入队列执行（`drainPendingMotionEvents`），先 `focusHostedDisplay` 再注入。
- root 输入桥服务端 `focusHostedDisplay`（`RootInputBridge.java:463`）：先确认虚拟显示 IME 策略为 0（IME 回落物理主屏），再反射调用 ATMS 公开方法 `focusTopTask(displayId)`（AOSP `ActivityTaskManagerService` 存在此方法，需 `enforceTaskPermission`，system uid 可通过）。
- Android 17 以下走 `depriveHostedInputFocus()` 的 1x1 Presentation 焦点守护；小米15 Ultra（Android 15/16 HyperOS）在该路径内，主槽切换时新旧槽分别 deprive/restore。
- 上游 `98e0bec` 新增 `VirtualDisplayImePolicyReadinessPolicy` 修复 IME 策略"假失败"；`9795bf4` 撤回了另一版输入法修改，最终保留的是就绪判定方案。

### 上游同步（v1.0.4 → v1.0.7）
- `upstream/main` = `51bbc00`，比本地旧基线 `1336a4d` 前进 35 个提交；`app/build.gradle.kts` 为 versionName 1.0.7 / versionCode 72，README 徽章仍写 1.0.5（上游自身不同步）。
- 关键修复/功能：`8b568f9` 触发区域点击、`98e0bec`/`4ee6ef9`/`9795bf4` 输入法、`9aac416` 屏幕内状态栏/导航条（新增 `OneStepNativeStatusBarHook` 让 SystemUI 的 `MultiDisplayStatusBarStarter` 为 `OneStepSlot-*` 虚拟显示启动原生状态栏，全屏时隐藏物理状态栏）、`ed54c8e` 全屏闪屏、`f8adb50` 折叠屏黑边、`3d0a142` 上滑退出、`e64239c`/`08e53ad` 拉伸/底部黑屏、`a4058be` 比例差距全屏刷新、拖拽分享系列（图片/视频/QQ/微信）、`58e8c5c` 拖拽分享开关。
- 本地 main 曾有自有提交 `6b460c4`（DEVELOPMENT.md + 计划文件）导致无法 ff；已 reset 到 `51bbc00`，文档恢复为未跟踪文件。
- 上游 `9aac416` 方案与本地 Phase 9 状态栏留白（`WindowContentTopInsetPolicy`）冲突：虚拟显示内已渲染状态栏时宿主主槽不应再留白，否则双重 inset；本地实验已归档至 `local/mi15u-base-20260922`（e5f67e6），未在任何设备安装过。

### 环境与构建
- 用户级 Gradle 代理 `127.0.0.1:7890` 本轮实测可用（HTTP 200），无需清空。
- 历史问题再现：项目 `.gradle/configuration-cache` 残留旧 daemon 的 Trae JRE jlink 路径导致 `JdkImageTransform` 失败；删除 configuration-cache 目录并以完整 Adoptium JDK 21（`~/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`）+ `--no-daemon` 重跑成功。
- `package-magisk-privapp.sh` 子脚本按名字找 `ndk-build`/`aapt2`，Windows SDK 只有 `.cmd`/`.exe`；本轮方案：创建被忽略的 `local.properties`（sdk.dir 指向真 SDK，隔离 Gradle）+ `$TEMP/onestep-sdk-view` 临时 SDK 视图（ndk-build/aapt2 sh shim + android.jar 复制），通过 `ANDROID_SDK_ROOT`/`ANDROID_NDK_HOME` 指向视图。
- Magisk 打包脚本固定取 `app/build/outputs/apk/debug/app-debug.apk`，因此个人签名必须接入 Debug build type，不能只配置 Release。
- 仓库 `.gitignore` 已忽略 `*.p12`、`*.jks`、`keystore.properties` 等秘密文件；仍应把实际私钥和密码放在仓库外。
- 选定主路径为 `%USERPROFILE%/.android/onestep-release.p12` 与 `onestep-signing.properties`，恢复副本为 `D:/Key/OneStep/`，均限制 ACL。
- 已生成 alias `onestep-personal`，算法 RSA 4096/SHA256withRSA，PKCS12，有效期 9125 天；证书 SHA-256 为 `0E:45:65:39:0C:6A:82:67:F7:B1:D9:C0:39:95:CB:50:1C:16:76:76:5D:34:92:3F:C1:DC:A4:35:A8:1D:BC:02`。
- 主密钥/属性文件与 `D:/Key/OneStep` 恢复副本逐文件哈希一致；密码只存在于受 ACL 限制的外部 properties 文件中。
- Gradle `signingReport` 证明 Debug、Release 和 debugAndroidTest 均使用 `oneStepPersonal` 配置与 `onestep-personal` alias，证书有效期至 2051-07-28。
- 个人签名 Magisk 模块为 `dist/OneStep4-1.0.3-magisk-20260803-224546.zip`，SHA-256 `B80542B07AA4B4CCF0A4BDED10A2F50ABDB36872C743ABCDFFC2D6DCE1C8B230`。
- ZIP 内 Debug APK SHA-256 为 `BC6BABF5954E4DD976C6E99C09126842EC9EACF327154360B38C758F41EDEDA6`；签名证书 SHA-256 为 `0E:45:65:39:0C:6A:82:67:F7:B1:D9:C0:39:95:CB:50:1C:16:76:76:5D:34:92:3F:C1:DC:A4:35:A8:1D:BC:02`。
- 模块最终结构为 37 个条目，无重复/缺项，12 个文本严格 UTF-8，脚本 0755、普通文件 0644、目录 0755，无静态顶层 `zygisk/`。
- 最终全量 JVM 测试为 39 suite、151 tests、0 failure/error/skipped；Release APK SHA-256 为 `5B6C385299822A995E73F1E992F6DDDF1B6EB848C7EC4006992AA0C3494A0D2A`，使用同一个人证书。
- 设备模块 APK 仍是旧作者签名产物，SHA-256 `EBBE0BBAFD44B540C222252350F932E0503F65CD7041B0B420BB73185CE67CAC`；当前模块属性与包信息仍为 1.0.3/versionCode 11，未刷入新模块。
