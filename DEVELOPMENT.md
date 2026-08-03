# OneStep4.0 二次开发指南

本文基于 `main` 的 `1336a4d`（OneStep4.0 1.0.3）源码整理，面向需要继续维护多窗口桌面、Root 虚拟显示、系统 Hook、媒体与导航组件的开发者。它描述的是当前实现，不替代真机测试结论。

## 1. 项目定位

OneStep4.0 是一个注册为 Android `HOME` 的多应用桌面容器。它维护一个主窗口和多个侧窗口，通过以下两种宿主承载真实第三方应用：

1. 优先路径：Root 进程创建可信 `VirtualDisplay`，应用侧用 `SurfaceView` 显示画面，并把输入转发到目标显示。
2. 后备路径：在具有系统任务嵌入权限的环境中，通过反射使用隐藏 `ActivityView`。

项目同时提供顶部应用快捷区、媒体播放器、高德导航、计时/录音通知卡片、内置桌面选择、自定义布局、背景和会话日志。

这不是普通 APK 能完整运行的应用。虚拟显示、任务管理、系统栏控制等核心能力依赖 Root、priv-app、平台签名或等效系统权限。`FLAG_SECURE`、状态栏 overlay 和虚拟显示 HOME 增强还依赖 LSPosed 或 Zygisk Hook 后端。

## 2. 当前构建基线

| 项目 | 当前值 |
| --- | --- |
| 工程名 | `OneStep4.0` |
| 应用 ID / namespace | `com.sangluo.onestep` |
| 版本 | `1.0.3`，versionCode `11` |
| Android Gradle Plugin | `9.2.1` |
| Gradle Wrapper | `9.4.1` |
| Gradle daemon JVM | Java 21（由 `gradle/gradle-daemon-jvm.properties` 指定） |
| compileSdk | Android 36，minor API 1 |
| minSdk / targetSdk / maxSdk | 29 / 36 / 37 |
| Java 源码级别 | Java 11 |
| Zygisk C++ 标准 | C++17 |
| Zygisk ABI | `arm64-v8a`、`armeabi-v7a` |
| Zygisk 最低平台 | Android 29 |

开发环境至少需要：

- 完整 JDK 21（必须包含 `jlink`）。项目 daemon 条件固定为 Java 21；命令行 Launcher 可以由 JDK 17 启动，Gradle 会按 Foojay 配置查找或下载 Java 21 工具链。源码本身按 Java 11 编译。
- Android SDK 及对应的 API 36.1 平台、Build Tools。
- Android Studio 可选；命令行构建以仓库内 Gradle Wrapper 为准。
- 打包 Hook 模块时需要 Bash、`zip`、`unzip`、SHA-256 工具、Android SDK `aapt2`。
- 构建 Zygisk 时需要 Android NDK r29；脚本也会从 `ANDROID_NDK_HOME`、`ANDROID_NDK_ROOT`、`ANDROID_SDK_ROOT` 或 `local.properties` 查找 `ndk-build`。
- 真机联调需要 ADB，以及已 Root 的测试设备。修改 `system_server` Hook 前必须准备可禁用模块或进入安全模式的恢复手段。

注意：当前 README 徽章仍写 Android 7/API 24，但 `app/build.gradle.kts` 的实际 `minSdk` 是 29。安装范围、CI 和二次开发兼容基线应以 Gradle 配置为准。

## 3. 模块与目录

```text
OneStepForAndroid-main/
├─ app/                         Android 应用、Java 源码、资源和测试
├─ xposed-api-stubs/            Xposed API 编译期桩，仅 compileOnly
├─ zygisk/                      system_server Zygisk 原生入口
├─ packaging/
│  ├─ magisk/                   Magisk 模块模板与启动脚本
│  ├─ ksu/                      KernelSU 模块模板与启动脚本
│  ├─ root/                     priv-app 权限、公共启动/操作脚本
│  └─ statusbar-overlay/        状态栏高度资源 overlay
├─ scripts/                     构建、打包和 Root ADB 安装脚本
├─ assets/readme/               README 展示图片
├─ gradle/                      版本目录与 Wrapper
└─ README.md                    用户侧简介和安装说明
```

Gradle 包含两个模块：

- `:app`：主应用。Aliuhook 和 Xposed API 都是编译期依赖；Aliuhook runtime 通过独立配置提取给 Zygisk 包使用。
- `:xposed-api-stubs`：最小 Xposed 接口桩，避免把框架 API 打进 APK。

## 4. 总体架构

```text
Android HOME / Launcher 入口
          |
          v
     MainActivity
   状态与流程编排层
     /     |      \
    v      v       v
窗口/UI   功能服务   系统能力
    |      |       |
    |      |       +-- SystemUiController
    |      |       +-- PersistentRootShell
    |      |       +-- Root Binder clients
    |      |
    |      +-- MediaSessionCoordinator
    |      +-- MediaNotificationListenerService
    |      +-- AmapNavigationClient
    |      +-- SessionLogRecorder
    |
    +-- OneStepWindowView[]
    +-- EmbeddedAppHost[]
            |
            +-- RootVirtualDisplayHost
            |      +-- RootVirtualDisplayBridge
            |      +-- RootInputBridge
            |
            +-- HiddenActivityViewHost

Hook 目标进程
    +-- android / system_server
    |    +-- OneStepSecureWindowHook
    |    +-- OneStepStatusBarOverlayHook
    |    +-- OneStepPrimaryHomeHook
    |    `-- OneStepRootVirtualDisplayCompatHook
    +-- com.android.settings: HyperOsThirdPartyHomeBypassHook
    +-- com.miui.home: HyperOsGestureNavigationBypassHook
    `-- com.android.systemui: HyperOsSystemUiGestureNavigationBypassHook

后端
    +-- LSPosed: OneStepLsposedEntry
    `-- Zygisk: onestep_zygisk.cpp + Aliuhook/LSPlant
```

### 4.1 编排层

[`MainActivity.java`](app/src/main/java/com/sangluo/onestep/MainActivity.java) 是核心编排类。它维护三组按槽位对应的数组：

- `LauncherApp[] windowApps`：每个槽当前代表的应用。
- `OneStepWindowView[] windowViews`：窗口外框、占位内容和交互。
- `EmbeddedAppHost[] embeddedHosts`：该槽实际使用的应用宿主。

槽位总数为“最大侧窗数 + 1”。`activeMainSlot` 指向主窗口，`sideSlotOrder` 保存侧窗显示顺序。不要把数组下标永久等同于主/侧角色，窗口交换时角色会变化。

`MainActivity` 还统一管理窗口切换期间的延迟任务、多个单线程 executor、PiP、系统栏、旋转、跨应用路由和资源释放。新增功能应优先放到现有 `feature/`、`system/` 或 `ui/` 类中，再通过 callback 接入，避免继续扩大编排类。

### 4.2 UI 层

界面基本由 Java 动态创建，没有传统 XML layout：

- [`OneStepWindowView.java`](app/src/main/java/com/sangluo/onestep/ui/window/OneStepWindowView.java)：单个窗口容器、桌面占位和侧窗滑动关闭。
- [`WindowLayoutCalculator.java`](app/src/main/java/com/sangluo/onestep/ui/window/WindowLayoutCalculator.java)：纯函数计算横向/纵向布局 Rect。
- [`WindowLayoutModePolicy.java`](app/src/main/java/com/sangluo/onestep/ui/window/WindowLayoutModePolicy.java)：按最小宽度和宽高比选择单主窗或双主窗。
- [`MainPaneFullscreenPolicy.java`](app/src/main/java/com/sangluo/onestep/ui/window/MainPaneFullscreenPolicy.java)：双主窗中选择靠外缘的主槽进入全屏。
- [`WindowAnimationController.java`](app/src/main/java/com/sangluo/onestep/ui/window/WindowAnimationController.java)：主侧窗口交换和替换动画。
- [`SideWindowInputShieldController.java`](app/src/main/java/com/sangluo/onestep/ui/window/SideWindowInputShieldController.java)：动画或层级特殊阶段的侧窗输入保护。
- [`TopPanelController.java`](app/src/main/java/com/sangluo/onestep/ui/topbar/TopPanelController.java)：顶部组件、媒体区和应用快捷区。
- [`SettingsPanelController.java`](app/src/main/java/com/sangluo/onestep/ui/settings/SettingsPanelController.java)：设置页、Root/Hook 状态、法律声明和内置桌面选择。
- [`BlurredBackgroundView.java`](app/src/main/java/com/sangluo/onestep/ui/background/BlurredBackgroundView.java)：工作区背景和模糊效果。
- [`FixedViewportFrameLayout.java`](app/src/main/java/com/sangluo/onestep/ui/widget/FixedViewportFrameLayout.java)：固定内置桌面的逻辑尺寸，再按容器缩放或裁切。

Surface 层级、窗口 Rect、虚拟显示规格和输入坐标相互依赖。修改布局时至少同时检查：手机单主窗、大屏双主窗、主窗全屏、折叠态变化、各侧窗、横/纵布局、进入/退出多窗口动画、触摸坐标变换、IME、旋转、PiP 和隐藏槽位。

### 4.3 数据与设置层

- [`LauncherAppRepository.java`](app/src/main/java/com/sangluo/onestep/data/apps/LauncherAppRepository.java)：查询 Launcher/Home activity，处理多用户应用和主题图标。
- [`SystemThemedIconLoader.java`](app/src/main/java/com/sangluo/onestep/data/apps/SystemThemedIconLoader.java)：加载 ROM 主题化图标。
- [`OneStepSettings.java`](app/src/main/java/com/sangluo/onestep/data/settings/OneStepSettings.java)：设置值、范围和 sanitize 规则。
- [`OneStepSettingsStore.java`](app/src/main/java/com/sangluo/onestep/data/settings/OneStepSettingsStore.java)：`SharedPreferences` 持久化与旧键迁移。
- [`TopAppListPolicy.java`](app/src/main/java/com/sangluo/onestep/data/settings/TopAppListPolicy.java)：合并顶部应用的已保存顺序、选择状态和当前可启动应用。
- [`RunningTaskAppResolver.java`](app/src/main/java/com/sangluo/onestep/feature/tasks/RunningTaskAppResolver.java)：按用户与任务组件解析正在运行的应用实例，驱动状态角标。

设置文件名为 `onestep_settings`。当前设置覆盖桌面行列、顶部应用选择/顺序、顶部图标/间距、顶部组件显示、状态栏留白、横纵布局、侧窗数、触发区域、手势灵敏度、日志记录和内置桌面组件。首次配置顶部应用时默认全选；已有配置会剔除失效项，并把新安装应用追加且默认选中。

Store 中保留了多个旧版本键的读取与迁移。新增或重命名设置时应遵循：

1. 在 `OneStepSettings` 定义默认值、边界和 sanitize 方法。
2. 在 Store 中增加新键，读取时兼容旧键，写入新值后移除旧键。
3. 在 `SettingsPanelController.Callbacks` 与 `MainActivity` 接通保存及即时应用。
4. 为边界、无效值和迁移路径补充 JVM 单元测试。

## 5. 启动与生命周期

Manifest 中有三个关键组件：

- `MainActivity`：`HOME + DEFAULT`，`singleTask`，实际桌面入口。
- `HomeRedirectActivity`：`LAUNCHER` 可见入口，负责回到 HOME。
- `MediaNotificationListenerService`：通知监听服务，提供媒体和顶部状态卡片数据。

`MainActivity.onCreate()` 的主要顺序如下：

1. 判断是否为 system app，并选择透明导航栏主题。
2. 如果 Activity 被启动在非默认显示上，转发 HOME 到默认显示后返回。
3. 设置不透明宿主窗口，避免 HOME 过渡泄露壁纸。
4. 读取设置，按需启动日志记录器。
5. 创建设置、顶部组件、动画控制器和嵌入桥状态。
6. 查询 Launcher/Home 应用，动态创建整个桌面视图树。
7. 创建每个槽位的嵌入宿主，预热 Root 输入桥。
8. 渲染窗口，请求内置桌面进入主槽。
9. 启动媒体和高德导航监控。

`onResume()` 会恢复系统栏、旋转、媒体、PiP 和输入保护；`onPause()/onStop()` 会抑制不合时机的嵌入启动。`onDestroy()` 必须释放：

- BroadcastReceiver、返回回调和 Handler callbacks；
- Notification/媒体/导航监听；
- 所有 `EmbeddedAppHost`；
- Root shell 和各 executor；
- 输入保护、Surface 动画与系统栏状态。

新增异步任务时必须有明确的 Activity 销毁检查、取消路径和 executor 关闭位置。

## 6. 窗口与应用启动流程

[`AppLaunchPlacement.java`](app/src/main/java/com/sangluo/onestep/ui/window/AppLaunchPlacement.java) 集中定义新应用的放置策略：

| 条件 | 动作 |
| --- | --- |
| 主窗口为空 | 在主槽启动 |
| 主窗口已有应用、存在空侧槽 | 在空侧槽启动并提升为主窗口 |
| 所有槽位已满 | 替换当前主窗口 |
| 内置桌面已占用槽位 | 优先替换桌面槽；必要时提升 |

典型启动链路：

```text
用户点击快捷项
  -> MainActivity 计算 AppLaunchPlacement
  -> 准备目标 OneStepWindowView 与动画
  -> EmbeddedAppHost.attach/start
  -> Root/ActivityView 宿主启动目标 activity
  -> 解析并确认目标 task/display
  -> Surface 可见后结束占位和切换动画
  -> 同步输入、IME、旋转、跨应用路由来源
```

代码使用 generation、epoch、延迟重试和“任务已确认”状态防止旧异步结果覆盖新窗口。不要用固定延时替代这些保护，也不要在未确认目标 task 消失时直接把应用视为已退出。

### 6.1 手机、大屏与折叠屏布局

[`WindowLayoutModePolicy.java`](app/src/main/java/com/sangluo/onestep/ui/window/WindowLayoutModePolicy.java) 将 `smallestScreenWidthDp >= 600` 视为大屏；大屏短长边比达到 `0.72` 时启用双主窗，否则保持单主窗。双主窗切换到全屏时，[`MainPaneFullscreenPolicy.java`](app/src/main/java/com/sangluo/onestep/ui/window/MainPaneFullscreenPolicy.java) 选择更靠工作区外缘的主槽，避免折叠屏和平板展开/收起后的角色跳变。

每个 1+n 槽位代表同一逻辑屏幕，不能直接用当前缩放后 View 的零散像素反复重建虚拟显示。相关边界分布在：

- [`VirtualDisplayViewportPolicy.java`](app/src/main/java/com/sangluo/onestep/feature/embedding/VirtualDisplayViewportPolicy.java)：决定何时沿用工作区规格、何时因大屏或双主窗转换重算容器尺寸。
- [`VirtualDisplayDensityPolicy.java`](app/src/main/java/com/sangluo/onestep/VirtualDisplayDensityPolicy.java)：手机保持约 393dp 逻辑宽度；大屏按宿主 density 和质量缩放，并在平板模式控制逻辑短边。
- [`FixedViewportFrameLayout.java`](app/src/main/java/com/sangluo/onestep/ui/widget/FixedViewportFrameLayout.java)：让内置 OneStep 桌面保持稳定逻辑 viewport，再缩放到主窗。

改动这组策略时必须把“布局 Rect”“虚拟显示 width/height/density”“Surface fixed size”和“触摸坐标变换”作为一个契约验证，否则容易出现闪屏、应用重启、内容拥挤或误触。

跨应用启动由 [`CrossAppLaunchRoutingPolicy.java`](app/src/main/java/com/sangluo/onestep/CrossAppLaunchRoutingPolicy.java) 和 Root bridge 协同处理。以下契约必须保留原始 Android 行为，不应强制路由回 OneStep：

- Storage Access Framework 和文件选择；
- 拍照、选择图片等结果型 Intent；
- `FLAG_ACTIVITY_FORWARD_RESULT`；
- 需要返回结果的显式启动。

## 7. 两种嵌入后端

### 7.1 统一接口

[`EmbeddedAppHost.java`](app/src/main/java/com/sangluo/onestep/feature/embedding/EmbeddedAppHost.java) 定义宿主能力：可用性、View 挂载、应用启动、返回、触摸分发和释放。窗口层只应依赖该接口或明确检查增强能力，避免把 Root 实现细节扩散到 UI。

### 7.2 Root VirtualDisplay 后端

[`RootVirtualDisplayHost.java`](app/src/main/java/com/sangluo/onestep/RootVirtualDisplayHost.java) 是应用进程侧控制器，职责包括：

- 创建和维护高分辨率 `SurfaceView`；
- 启动 Root bridge，并连接按 UID 命名的 Binder 服务；
- 创建、调整、挂接和释放可信虚拟显示；
- 在指定显示启动应用并解析实际 task；
- 转换、排队并注入触摸/按键；
- 管理显示焦点、IME local policy、方向和传感器 UID 覆盖；
- 监听 task 变化、返回退出、跨应用启动和 PiP。
- 在大屏/折叠态变化时按 viewport/density 策略稳定调整显示，避免按槽位舍入值反复重建。

应用侧 Binder client 位于：

- [`RootVirtualDisplayBridgeClient.java`](app/src/main/java/com/sangluo/onestep/system/display/RootVirtualDisplayBridgeClient.java)
- [`RootInputBridgeClient.java`](app/src/main/java/com/sangluo/onestep/system/input/RootInputBridgeClient.java)

Root 端入口位于：

- [`RootVirtualDisplayBridge.java`](app/src/main/java/com/sangluo/onestep/RootVirtualDisplayBridge.java)
- [`RootInputBridge.java`](app/src/main/java/com/sangluo/onestep/RootInputBridge.java)
- [`RootTaskStackObserver.java`](app/src/main/java/com/sangluo/onestep/RootTaskStackObserver.java)
- [`RootCrossAppLaunchController.java`](app/src/main/java/com/sangluo/onestep/RootCrossAppLaunchController.java)

Bridge 同时校验调用 UID 和随机 token。`SystemServiceFailurePolicy` 会识别 `DeadObjectException`、`DeadSystemException` 等系统进程重启后的旧服务失败，client 可据此断开并重新连接；不要把所有 `RemoteException` 都当作可盲目重试。修改 Binder transaction 时必须同步更新 client/server 的 transaction code、Parcel 字段顺序、异常返回和兼容逻辑，并增加至少一层协议测试。

系统版本还有显式兼容分支：Android 11 跳过运行时 SELinux policy 重载并禁止向已移除显示注入 HOME；Android 17 及以上不再用 `Presentation` 抢占/降低宿主输入焦点；Android 10-13 可由 Root 显示兼容 Hook 绕过 root UID 的包名校验。合并上游时不要把这些看似特殊的判断当作冗余代码删除。

### 7.3 Hidden ActivityView 后端

[`HiddenActivityViewHost.java`](app/src/main/java/com/sangluo/onestep/feature/embedding/HiddenActivityViewHost.java) 通过反射访问隐藏 `ActivityView`。它适用于系统权限可用但 Root VirtualDisplay 后端不可用的环境。该路径仍可能因 ROM 删除/改名隐藏 API 而失败，因此 `isAvailable()` 和 `getUnavailableReason()` 是正常降级的一部分。

## 8. Hook 后端

### 8.1 LSPosed

入口为 [`OneStepLsposedEntry.java`](app/src/main/java/com/sangluo/onestep/hook/OneStepLsposedEntry.java)，`assets/xposed_init` 指向该类。当前 `xposed_scope.xml` 包含 `android`、`com.android.settings`、`com.android.systemui` 和 `com.miui.home`：system_server 安装显示/HOME Hook，另外三个进程只安装对应的 HyperOS 默认桌面或手势兼容 Hook。修改 scope 时必须同步核对入口中的 package/process 双重过滤。

### 8.2 Zygisk

[`onestep_zygisk.cpp`](zygisk/jni/onestep_zygisk.cpp) 在 `system_server` specialize 阶段加载 Aliuhook/LSPlant runtime 和 OneStep APK 中的系统 Hook 类，并在 HyperOS 的 `com.miui.home`、`com.android.systemui` 进程加载手势兼容 Hook。若检测到活动的 LSPosed/Vector 模块或选择了 LSPosed backend，它会跳过独立后端，避免重复 Hook。

当前增强点：

- [`OneStepSecureWindowHook.java`](app/src/main/java/com/sangluo/onestep/hook/OneStepSecureWindowHook.java)：让 OneStep 虚拟显示正确承载 `FLAG_SECURE` 窗口。
- [`OneStepStatusBarOverlayHook.java`](app/src/main/java/com/sangluo/onestep/hook/OneStepStatusBarOverlayHook.java)：控制状态栏资源 overlay，并带有快速崩溃保护。
- [`OneStepPrimaryHomeHook.java`](app/src/main/java/com/sangluo/onestep/hook/OneStepPrimaryHomeHook.java)：增强虚拟显示上的主 HOME 行为。
- [`OneStepRootVirtualDisplayCompatHook.java`](app/src/main/java/com/sangluo/onestep/hook/OneStepRootVirtualDisplayCompatHook.java)：兼容 Android 10-13 的 Root 虚拟显示包名校验。
- [`HyperOsThirdPartyHomeBypassHook.java`](app/src/main/java/com/sangluo/onestep/hook/HyperOsThirdPartyHomeBypassHook.java)：在 Settings 进程解除 HyperOS 第三方 HOME 限制。
- [`HyperOsGestureNavigationBypassHook.java`](app/src/main/java/com/sangluo/onestep/hook/HyperOsGestureNavigationBypassHook.java)：在 MIUI Home 保持 OneStep 虚拟显示上的 HOME/Overview 生命周期。
- [`HyperOsSystemUiGestureNavigationBypassHook.java`](app/src/main/java/com/sangluo/onestep/hook/HyperOsSystemUiGestureNavigationBypassHook.java)：在 SystemUI 放开第三方 HOME 的手势导航限制。

对应禁用 marker：

```text
hook-config/disable-secure-window
hook-config/disable-status-bar-overlay
hook-config/disable-primary-home-enhancement
hook-config/enable-hyperos-third-party-gesture
```

前三个 marker 是禁用开关；`enable-hyperos-third-party-gesture` 是用户在设置页确认后写入的启用标记，同时还会设置 `force_fsg_nav_bar=1`，不能按禁用 marker 处理。

新增 Hook 时必须提供独立开关、启动日志、ROM/版本探测、反射失败降级和无需进入系统即可恢复的禁用方式。不要把“Hook 类加载成功”等同于功能已在真机生效。

## 9. 功能模块

### 9.1 媒体

[`MediaSessionCoordinator.java`](app/src/main/java/com/sangluo/onestep/feature/media/MediaSessionCoordinator.java) 查询 active media sessions，选择当前或用户指定的媒体源，并监听 controller 状态。`MediaPlaybackPanel` 展示封面、元数据、进度、收藏、播放控制和队列。

[`MediaNotificationListenerService.java`](app/src/main/java/com/sangluo/onestep/MediaNotificationListenerService.java) 提供通知快照作为补充，并识别计时器、秒表和录音通知及其 actions。通知文案和 RemoteViews 在不同 ROM 上差异很大，分类规则必须允许“未知”，不能用单一文案硬判所有设备。

### 9.2 高德导航

[`AmapNavigationClient.java`](app/src/main/java/com/sangluo/onestep/AmapNavigationClient.java) 绑定：

```text
com.autonavi.minimap.service.NAVIGATION_SERVICE
```

它解析回调中的 type `4` 导航 JSON，发布道路、距离、剩余时间和转向信息，并在数据过期时清空状态。相关展示格式位于 [`NavigationDisplayFormatter.java`](app/src/main/java/com/sangluo/onestep/feature/navigation/NavigationDisplayFormatter.java)。修改协议前应先保存真实高德回调样本，并保持无高德、鉴权失败、字段缺失、无导航四种状态可用。

### 9.3 日志

[`SessionLogRecorder.java`](app/src/main/java/com/sangluo/onestep/feature/logging/SessionLogRecorder.java) 将筛选后的 logcat 写入 `cache/session_logs`。用户导出时：

- Android 10 及以上通过 MediaStore 写入 Downloads；
- Android 9 及以下使用传统外部存储并请求写权限。

日志导出成功只证明文件写出，不证明 Root、虚拟显示或 Hook 工作正常。分析问题时应同时记录版本、ROM、Root 后端、显示 ID、任务 ID 和复现步骤。

## 10. 构建与测试

### 10.1 Windows 基础构建

```powershell
.\gradlew.bat :app:assembleDebug
```

如果 IDE 附带的 Java 21 只是缺少 `jlink` 的精简 JRE，可关闭本机 Java 自动探测，让项目下载完整工具链：

```powershell
.\gradlew.bat --% -Porg.gradle.java.installations.auto-detect=false -Porg.gradle.java.installations.auto-download=true --no-daemon :app:assembleDebug
```

本工作区在 Windows 上实际验证时还需要临时绕过用户级 Gradle 代理，并显式提供 SDK：

```powershell
$env:ANDROID_SDK_ROOT='D:\software\androidSDK'
.\gradlew.bat --% -Dhttp.proxyHost= -Dhttps.proxyHost= -Porg.gradle.java.installations.auto-detect=false -Porg.gradle.java.installations.auto-download=true --no-configuration-cache --no-daemon --rerun-tasks :app:testDebugUnitTest :app:assembleDebug --stacktrace
```

`--%` 是 PowerShell 的停止解析标记；去掉后，空 proxy 参数可能被错误拆成 Gradle task。SDK 路径只适用于当前机器，其他环境应使用自己的 `local.properties` 或 `ANDROID_SDK_ROOT`。

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 10.2 JVM 单元测试

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

测试报告通常位于：

```text
app/build/reports/tests/testDebugUnitTest/index.html
```

当前单元测试重点覆盖纯策略和解析器：窗口放置、单/双主窗与全屏、虚拟显示 density/viewport/flags、跨应用与 HOME 路由、任务解析和运行实例匹配、返回/输入焦点/Surface 复用、设置边界与顶部应用合并、方向映射、Root bridge 失效识别、Android 10-17 兼容分支、HyperOS HOME 条件、Hook 配置和状态栏 crash guard。新增可纯化的规则时，优先提取为无 Android 状态的类并在 JVM 测试中覆盖。

### 10.3 仪器测试

连接测试设备后执行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

仪器测试不等于 Root/Hook 验收。普通测试 APK 环境通常无法覆盖 signature 权限、system_server、可信显示和开机脚本。

## 11. 打包与安装

### 11.1 Magisk 模块

在具备 Bash、Android SDK、NDK r29、zip/unzip 的环境执行：

```bash
./scripts/package-magisk-privapp.sh
```

### 11.2 KernelSU 模块

```bash
./scripts/package-ksu-privapp.sh
```

两个脚本都会：

1. 清理旧 app 构建产物；
2. 禁用构建缓存并强制重建 APK；
3. 提取 Aliuhook runtime；
4. 构建双 ABI Zygisk so；
5. 构建状态栏资源 overlay；
6. 组装模块 ZIP；
7. 校验 ZIP 内 APK SHA-256 与本次 APK 一致，并检查所需启动脚本/Hook payload 完整；Magisk 包含独立的 `post-fs-data.sh`。

产物位于 `dist/`。最终成功应以脚本零退出码、打印的 ZIP 路径和 SHA-256 为准。

### 11.3 Root ADB 直装

```bash
./scripts/install-root-privapp-adb.sh [device-serial]
```

该脚本会修改 `/system/priv-app`、安装 privapp 权限 XML、调整 hidden API policy 并重启设备。它只适合可写 system 的专用测试设备，不应用于用户数据不可恢复的设备，也不包含完整 Zygisk/LSPosed 模块验收。

## 12. 推荐的二次开发路径

### 12.1 新增一个设置项

1. 在 `OneStepSettings` 增加默认值、范围和 sanitize。
2. 在 `OneStepSettingsStore` 增加持久化；如果替换旧项，保留迁移读取。
3. 扩展 `SettingsPanelController.Callbacks`，在设置 UI 中增加控件。
4. 在 `MainActivity` 保存并即时应用，必要时重排窗口或重建顶部区域。
5. 增加设置边界、默认值和迁移测试。

### 12.2 新增顶部组件

1. 明确数据源和生命周期，不要直接把系统监听堆进 View 创建代码。
2. 数据/解析放入 `feature/`，展示和交互放入 `ui/topbar/`。
3. 在 `TopComponentPage`/adapter 中加入稳定 page ID，保留翻页位置。
4. 接入 `resume/pause/close`，移除 Handler ticker 和监听器。
5. 验证顶部组件隐藏、横纵布局、窗口动画和低高度屏幕。

### 12.3 修改窗口策略

1. 先修改 `AppLaunchPlacement` 或新增纯策略类。
2. 为每个状态组合写 JVM 测试。
3. 再调整 MainActivity 的动画和状态提交顺序。
4. 验证桌面占槽、空主槽、空侧槽、满槽、快速连点、动画中启动、侧窗关闭和主侧交换。

### 12.4 修改大屏或虚拟显示尺寸

1. 先在 `WindowLayoutModePolicy`、`WindowLayoutCalculator`、`MainPaneFullscreenPolicy` 明确物理窗口布局。
2. 再核对 `VirtualDisplayViewportPolicy`、`VirtualDisplayDensityPolicy` 和 `RootVirtualDisplayHost.makeVirtualDisplaySpecForRect()` 的逻辑屏规格。
3. 保持 `FixedViewportFrameLayout`、Surface fixed size、IME local policy 与触摸坐标使用同一参考尺寸。
4. 为边界宽高比、600dp 阈值、折叠态进入/退出、单/双主窗转换增加 JVM 测试。
5. 真机覆盖手机、平板、展开/折叠、横竖屏、主窗全屏和主侧交换，记录 display size/density 与 task 是否重启。

### 12.5 扩展 Root Binder 协议

1. 定义 transaction code 与参数/返回结构。
2. 同步修改 client 和 bridge server。
3. 保留 UID + token 校验、超时和 RemoteException 处理。
4. 确保旧 bridge 连接失败时能重新启动或明确降级。
5. 使用错误 token、错误 UID、旧服务、进程死亡和超时场景测试。

### 12.6 新增 system_server 或 ROM Hook

1. 先确认现有公开/隐藏 API 无法完成需求。
2. 将条件判断提取成可单测 policy，Hook 只负责取值和写回。
3. 明确目标 package/process，并同步 `xposed_scope.xml`、`OneStepLsposedEntry` 和需要的 Zygisk specialize 分支。
4. 同时接入适用的 LSPosed 与 Zygisk bootstrap，记录后端差异；不是所有 Settings 进程 Hook 都能由当前 Zygisk 路径覆盖。
5. 增加独立 enable/disable marker 和模块更新时的保留逻辑，名称必须表达真实极性。
6. 在至少两个 Android 大版本验证，并准备安全模式/移除模块恢复流程。

## 13. 调试建议

常用标签和证据来源：

```powershell
adb logcat -s OneStep40 OneStepZygisk OneStepLsposed OneStepPrimaryHome OneStepHyperOsGesture OneStepHyperOsSystemUi
adb shell dumpsys activity activities
adb shell dumpsys display
adb shell dumpsys media_session
adb shell dumpsys notification
```

Zygisk 状态栏 Hook 还会写入：

```text
/data/system/onestep-status-hook.log
```

排查虚拟显示黑屏时按层确认：

1. APK 是否为当前版本，是否以 priv-app/模块方式生效。
2. 应用是否获得 Root，以及 bridge 进程/Binder 服务是否启动。
3. VirtualDisplay 是否创建，display ID、尺寸、density、flags 是否合理。
4. 目标 task 是否真的位于该 display。
5. Surface 是否已 attach、可见且 alpha 非零。
6. `FLAG_SECURE` 黑屏是否仅发生在 Hook 未启用时。
7. 输入问题与画面问题分开验证，输入走独立 bridge。

大屏或折叠屏闪屏还要对比布局切换前后的 workspace Rect、双主窗状态、虚拟显示 width/height/density、Surface frame 和 hosted task ID。只有 Rect 变化而虚拟显示反复销毁重建，通常说明 viewport 或宽高比判断失去稳定性。

排查窗口错位或误触时，记录物理屏旋转、虚拟显示旋转、窗口 Rect、Surface 尺寸、触摸 view/display 坐标和当前 main slot。只看屏幕截图不足以定位输入变换问题。

## 14. 验证矩阵

| 阶段 | 能证明什么 | 不能证明什么 |
| --- | --- | --- |
| `testDebugUnitTest` | 纯策略、解析和边界规则 | Android 系统权限、Surface、Root、Hook |
| `assembleDebug` | Java/资源/Manifest 可编译并产出 APK | APK 已安装或系统特权生效 |
| 模块打包脚本成功 | APK、Hook、overlay 已组装且哈希一致 | 模块已刷入、设备已重启 |
| ADB/管理器显示安装 | 设备上存在对应包/模块 | system_server Hook 和业务行为正常 |
| 重启后日志与 dumpsys | 启动脚本、进程、显示和任务的运行证据 | 用户工作流全部正确 |
| 真机交互回归 | 指定 ROM/Root 后端上的实际行为 | 其他 ROM 和 Android 版本兼容性 |

每次涉及 Root、虚拟显示或 Hook 的改动，交付时应明确列出已经完成到哪一层，不要用“构建通过”替代“真机可用”。

## 15. 已知维护边界

- `MainActivity` 与 `RootVirtualDisplayHost` 状态量大，局部改动也可能影响窗口生命周期；优先提取纯策略和小控制器。
- Android 10-17 存在显式的 Root 显示、HOME、SELinux policy、IME 和输入焦点兼容分支；删除分支前必须在对应版本获得运行证据。
- 手机、平板和折叠屏共享一套槽位状态，但布局 Rect、逻辑 viewport、density 和像素缩放不是同一概念，不能只改其中一层。
- 隐藏 API、反射字段、shell 文本和 ROM 私有广播都可能随系统升级变化；失败必须可降级，解析必须拒绝含糊结果。
- `system_server` 和 SystemUI Hook 的错误可能导致重启循环；任何新路径都需要禁用开关和 crash guard。
- 通知分类、高德服务、主题图标属于外部协议，修改前应保存真实样本并覆盖缺失/变更情况。
- 设备上的 Root 授权、模块刷入、重启、默认 HOME 切换均是有状态操作，应与静态分析和构建分开记录。
- 仓库当前 `.gitignore` 忽略 `docs/`，新增需要纳入版本控制的文档应放在根目录，或先明确调整 ignore 规则。
- README 当前仍标注 Android 7/API 24，而 Gradle `minSdk` 已是 29；发布前应统一用户文案，开发和测试始终以构建配置为准。

## 16. 关键源码索引

| 主题 | 文件 |
| --- | --- |
| 应用入口与总编排 | `app/src/main/java/com/sangluo/onestep/MainActivity.java` |
| Manifest/权限 | `app/src/main/AndroidManifest.xml` |
| 应用构建 | `app/build.gradle.kts` |
| 窗口视图 | `ui/window/OneStepWindowView.java` |
| 布局计算 | `ui/window/WindowLayoutCalculator.java` |
| 单/双主窗与全屏策略 | `ui/window/WindowLayoutModePolicy.java`、`MainPaneFullscreenPolicy.java` |
| 虚拟显示 density/viewport | `VirtualDisplayDensityPolicy.java`、`feature/embedding/VirtualDisplayViewportPolicy.java` |
| 启动放置策略 | `ui/window/AppLaunchPlacement.java` |
| 宿主接口 | `feature/embedding/EmbeddedAppHost.java` |
| Root 虚拟显示宿主 | `RootVirtualDisplayHost.java` |
| Root 显示 bridge | `RootVirtualDisplayBridge.java` |
| Root 输入 bridge | `RootInputBridge.java` |
| 设置模型与存储 | `data/settings/OneStepSettings.java`、`OneStepSettingsStore.java` |
| 顶部应用合并与运行状态 | `data/settings/TopAppListPolicy.java`、`feature/tasks/RunningTaskAppResolver.java` |
| 顶部组件 | `ui/topbar/TopPanelController.java` |
| 媒体 | `feature/media/MediaSessionCoordinator.java`、`ui/media/MediaPlaybackPanel.java` |
| 高德导航 | `AmapNavigationClient.java` |
| 日志 | `feature/logging/SessionLogRecorder.java` |
| LSPosed 入口 | `hook/OneStepLsposedEntry.java` |
| 系统/HyperOS Hook | `hook/OneStepRootVirtualDisplayCompatHook.java`、`hook/HyperOs*Hook.java` |
| Zygisk 入口 | `zygisk/jni/onestep_zygisk.cpp` |
| Magisk/KSU 打包 | `scripts/package-magisk-privapp.sh`、`scripts/package-ksu-privapp.sh` |

后续开发前，先从目标功能对应的控制器和测试开始阅读，再进入 `MainActivity` 的接线代码。涉及系统服务时，同时检查应用侧 client、Root bridge、Hook 后端、模块脚本和真机恢复路径。

## 17. 本次基线验证

2026-08-03 对 `1336a4d` 强制重新执行 JVM 测试和 Debug 构建，结果如下：

| 项目 | 结果 |
| --- | --- |
| Gradle | 9.4.1，43 个 task 全部执行，构建成功 |
| JVM 测试 | 38 个 suite、148 个 test，0 failures/errors/skipped |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| APK 元数据 | `com.sangluo.onestep`，1.0.3，versionCode 11 |
| APK 大小 | 15,139,941 bytes |
| APK SHA-256 | `28040412D751B076EF680B7FF903AC9D2A7E45A0350DA4A5FD1A97129BB5F1E1` |
| APK 签名 | `apksigner` 校验通过，v2，Android Debug 单一签名者 |

本次没有安装 APK、刷入 Magisk/KSU 模块、重启设备或验证真实 VirtualDisplay、输入、IME、HyperOS 手势与 system_server Hook。Gradle 还报告了未来与 Gradle 10 不兼容的 deprecated features；升级 Wrapper 前应另用 `--warning-mode all` 定位并清理。
