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
