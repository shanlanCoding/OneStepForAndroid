# Progress

## 2026-08-01
- 已完整读取 `planning-with-files` 技能说明。
- 已确认没有既有规划文件和相关历史记忆。
- 已确认当前目录不是 Git 工作树。
- 已创建本次任务的规划、发现和进度记录。
- 已盘点 Gradle 配置、Manifest、README、源码/测试/资源数量和运行形态。
- 已识别主编排层、两种嵌入后端、窗口分配/布局策略和设置兼容逻辑。
- 已梳理 Root Binder 双通道、虚拟显示状态、Zygisk/LSPosed 后端选择和生命周期入口。
- 已完成构建、打包、部署脚本和测试覆盖盘点；Phase 1 完成。
- 已梳理顶部组件、媒体、导航、通知、日志和设置数据流；Phase 2 完成。
- 已创建根目录 `DEVELOPMENT.md`；Phase 3 完成。

## 2026-08-03
- 已配置 `origin`、`upstream` 和 `D:/Key/id_ecdsa`，拉取及 push dry-run 通过。
- 已证明本地受跟踪源码精确匹配旧提交 `5537a19`，用户没有代码改动。
- 已开始升级到最新 `origin/main` 并修订开发文档。
- 已恢复并复核既有计划上下文；Git 状态仍与旧源码快照挂在最新 HEAD 上的诊断一致。
- 已确认 `task_plan.md`、`progress.md`、`findings.md` 均为严格 UTF-8、LF、无 BOM。
- 已盘点 `origin/main` 的 484 个文件、Git 二进制分类、`.gitattributes` 和 23 个待同步提交。
- 已严格校验远端 229 个文本文件：全部为 UTF-8，无 BOM、U+FFFD 或常见乱码标记；240 个二进制文件未按文本解码。
- 已建立恢复分支 `snapshot/old-source-20260801` 指向 `5537a19`。
- 已将受跟踪文件更新到 `origin/main` 的 `1336a4d`；索引和受跟踪工作树均干净，四个本地文档保留。
- Phase 5 完成，开始基于最新版源码重新分析和修订文档。
- 已完成最新版文件规模、85 文件差异、构建基线、Manifest、README 和新增策略/Hook 类的初步盘点。
- 已深读大屏/密度/viewport、宿主生命周期、任务解析、Root bridge 失败恢复、Android 10-17 分支及 HyperOS/LSPosed scope 实现。
- 已复核最新版 min/target/max SDK、RecyclerView 依赖、顶部应用列表合并策略和模块打包脚本变化。
- 已核对 `createEmbeddedHost()` 的真实 Root 优先/ActivityView 降级顺序、Root 可用性与错误状态，以及内置桌面固定 viewport 接入。
- 已局部修订 `DEVELOPMENT.md`：更新 1.0.3/SDK 29-37 基线，补充大屏/折叠屏、虚拟显示规格、运行任务角标、Root 失效恢复、Android 10-17 与 HyperOS Hook、打包和扩展路径。
- `DEVELOPMENT.md` 已通过严格 UTF-8/LF、无乱码标记、无失效本地链接和过时版本结论检查；Phase 6 完成。

## Files Created
- `DEVELOPMENT.md`
- `task_plan.md`
- `findings.md`
- `progress.md`

## Verification
- 最新 `DEVELOPMENT.md` 已通过严格 UTF-8 解码：无 BOM、LF 547、CRLF 0、无 U+FFFD/常见乱码标记，47 个本地 Markdown 链接全部存在。
- 文档中的 AGP、Gradle、SDK、版本、Java、ABI、Hook scope 和兼容策略已与 1.0.3 源码复核一致。
- 使用完整 Java 21 自动工具链并加 `--rerun-tasks` 后，`:app:testDebugUnitTest :app:assembleDebug` 成功，43 个 task 全部执行。
- 最新单元测试共 38 个 suite、148 个 test，0 failures、0 errors、0 skipped。
- Debug APK 已生成：`app/build/outputs/apk/debug/app-debug.apk`（15,139,941 bytes，1.0.3/versionCode 11，SHA-256 `28040412D751B076EF680B7FF903AC9D2A7E45A0350DA4A5FD1A97129BB5F1E1`）。
- 受跟踪工作树与索引均无差异；只保留四个本地文档为未跟踪文件。
- `apksigner` 校验通过：APK 使用 v2 Android Debug 签名，单一签名者。
- 最终门禁通过：四个文档严格 UTF-8/LF、无乱码、文档链接完整；实时 origin/upstream main、本地三项引用均为 `1336a4d`，恢复分支为 `5537a19`。
- Phase 7 完成；未安装、未刷模块、未进行真机 Root/Hook 验证。
- 已开始“小窗口全屏后预留状态栏高度”任务，追加 Phase 8-10。
- 已确认开始时源码工作树干净，仅有用户未跟踪文件 `开发笔记.md`，不会改动。
- 已确认三个规划文件均为严格 UTF-8、LF、无 BOM。
- 首次编码检查命令因 PowerShell 空管道语法错误在读取阶段停止；改用显式 `$rows` 数组后成功。
- 已确认设备 `58a9fb4b` 在线并拉取最新 1.3 MB 日志到系统临时目录，日志严格 UTF-8、无 BOM。
- 已用日志和实时 `dumpsys window` 确认顶部状态栏高度为 127 px，OneStep 全屏主槽仍从 y=0 承载虚拟显示内容。
- 已定位 `completeExitOneStepMode()` 的状态栏显示与 `applyWindowLayout(true)` 调用链，继续追踪窗口矩形计算入口。
- Phase 8 完成：根因是 `WindowLayoutCalculator` 的非多窗口分支把主槽顶部硬编码为 0。
- 已实现 `WindowContentTopInsetPolicy`，全屏使用状态栏安全 inset，多窗口保留原顶部组件高度；已补 3 个纯 Java 策略测试。
- 首次源码补丁因 import 上下文顺序不匹配被整体拒绝；读取准确上下文后拆分并成功应用。
- 新增 `WindowContentTopInsetPolicyTest` 的 3 个测试全部通过，主源码和测试源码编译成功；Phase 9 完成。
- Gradle 首次因 SDK 环境变量缺失停止，随后两次因旧 daemon 仍选中 Trae 精简 JRE 21 而缺少 `jlink.exe`；最终用完整 Adoptium JDK 21 启动单次 Gradle 成功。
- 使用 `--rerun-tasks` 完整执行 43 个 Gradle task：39 个 suite、151 个 JVM test 全通过，Debug APK 构建成功。
- APK 路径为 `app/build/outputs/apk/debug/app-debug.apk`，15,140,385 bytes，SHA-256 `F613DA01AE90D52B2ED11A01955EB1C8B3E0BCA7A95E0978C3AEED9E3B560A15`。
- `adb install -r` 被签名门禁拒绝；现装系统 APK 与新 Debug APK 证书不同，未卸载、未清数据、未写系统分区。
- Phase 10 完成于安装门禁：静态、测试和构建验证完成，真机运行证明等待匹配签名密钥或另行授权的可恢复系统替换方案。
- 用户确认既有构建产物是 Magisk 模块 ZIP；已追加 Phase 11，重新评估完整模块构建与设备更新方式。
- 已确认设备模块 ID 为 `onestep40_privapp`，设备挂载 APK 与用户提供的旧模块 APK 哈希完全一致。
- 已确认完整模块除 APK 外还包含权限、overlay、SELinux、Zygisk 和启动脚本；后续交付改为构建 Magisk ZIP。
- 新旧 APK 签名不一致，刷入新模块存在重启后系统包扫描失败风险；在缺少原私钥时不直接刷入设备。
- 已下载 Google 官方 NDK r29 并按 repository2-3.xml 的 SHA-1 校验通过，解压到 SDK 独立目录；下载压缩包已删除。
- 已用当前源码强制重编 APK、Zygisk runtime、双 ABI native payload 和状态栏 overlay，Magisk 打包脚本零退出。
- 已生成 `dist/OneStep4-1.0.3-magisk-20260803-222302.zip`（7,710,273 bytes，SHA-256 `FF164AE316CA96A2FD22031CA4C1F5EA53AEEDF1F0D2E21557D04DAA387D8489`）。
- ZIP 最终结构门禁通过：37 个条目、0 重复、12 个严格 UTF-8 文本、必需项齐全、无静态顶层 `zygisk/`，脚本 0755/普通文件 0644。
- 打包 clean 后重新运行完整 JVM 测试：39 suite、151 tests，0 failure/error/skipped。
- 临时 NDK/zip/aapt2 转发入口均已删除；设备模块 APK 哈希、hook marker、版本和 lastUpdateTime 均未变化。
- Phase 11 完成于签名门禁：完整模块已交付但未刷入，需原签名私钥或明确的数据/包身份迁移授权才能继续真机验证。
- 用户授权由 Codex 决定并生成长期个人签名密钥；已追加 Phase 12-14。
- 已确认仓库没有 signingConfigs，Magisk 打包固定使用 `assembleDebug`；需要同时接入 Debug 和 Release 签名。
- 已确认计划中的密钥、属性文件和备份路径均不存在，尚未覆盖任何现有秘密。
- 已生成 RSA 4096/SHA256withRSA、PKCS12、有效期 9125 天的 `onestep-personal` 密钥；Phase 12 完成。
- 主密钥和配置位于 `%USERPROFILE%/.android/`，恢复副本与说明位于 `D:/Key/OneStep/`；密钥和配置的主/备 SHA-256 分别一致。
- 证书 SHA-256 为 `0E:45:65:39:0C:6A:82:67:F7:B1:D9:C0:39:95:CB:50:1C:16:76:76:5D:34:92:3F:C1:DC:A4:35:A8:1D:BC:02`；密码未写入日志或聊天。
- 已确认五个密钥/恢复文件均关闭 ACL 继承，仅当前用户和 SYSTEM 可完全访问；外部文本严格 UTF-8、无 BOM。
- 已在 `app/build.gradle.kts` 接入仓库外个人签名：配置存在时 Debug/Release 共用同一密钥，缺字段或缺密钥时失败；配置完全不存在时保持开源默认构建。
- `:app:signingReport` 成功：Debug、Release、debugAndroidTest 均使用 `oneStepPersonal/onestep-personal`，证书指纹完全一致；Phase 13 完成。
- 首次个人签名组合构建完成 Debug/Release 编译和签名校验，但在 Release lint 解析依赖时因失效的本机 Gradle 代理停止；未生成错误签名产物，待临时清空代理重跑。
- 清空失效代理后，个人签名 Debug/Release APK 与全量 JVM 测试构建成功；两个 APK 的证书均为个人证书。
- 首次个人签名 Magisk 打包因外层 5 秒执行窗口断开，子进程随后结束且未生成新 ZIP；已确认没有残留构建进程，改用可续接长运行会话。
- 个人签名 Magisk 模块已零退出生成；首次 ZIP Unix 权限修正遇到 PowerShell 高位十六进制有符号转换错误，结果不采信，待用字符串解析后覆盖全部条目。
- 已生成个人签名模块 `dist/OneStep4-1.0.3-magisk-20260803-224546.zip`，最终 SHA-256 为 `B80542B07AA4B4CCF0A4BDED10A2F50ABDB36872C743ABCDFFC2D6DCE1C8B230`。
- ZIP 门禁通过：37 个条目、0 重复、0 缺项、12 个文本严格 UTF-8、0 权限错误，无静态顶层 `zygisk/`。
- ZIP 内 APK 与当前 Debug APK 的 SHA-256 均为 `BC6BABF5954E4DD976C6E99C09126842EC9EACF327154360B38C758F41EDEDA6`；`apksigner` 证明单一 v2 签名者为个人 RSA 4096 证书。
- clean 后最终重跑 71 个 Gradle task 成功；39 suite、151 tests、0 failure/error/skipped，Release APK 重新生成并使用同一个人证书。
- Release APK 最终 SHA-256 为 `5B6C385299822A995E73F1E992F6DDDF1B6EB848C7EC4006992AA0C3494A0D2A`。
- 三个临时 Windows 打包 shim 和临时解包 APK 均已删除并确认不存在。
- 设备只读复核：模块 APK 哈希仍为旧值 `EBBE0BBAFD44B540C222252350F932E0503F65CD7041B0B420BB73185CE67CAC`，模块/包仍为 1.0.3/versionCode 11，未刷入个人签名模块。
- Phase 14 完成；首次安装个人签名版本仍需明确的数据与包身份迁移方案。

## 2026-09-22（小米15 Ultra 适配，Phase 15）
- 已恢复上下文；确认目标设备 58a9fb4b 不在线（IPv6/局域网均连不上），仅小米6 `e5a2f9b5` 在线；本轮不修改设备。
- 已基于源码定位开发笔记 Bug 2 根因：触发区域 OnTouchListener 全量消费事件；Bug 3 链路：`focusHostedDisplay` → IME 策略确认 → ATMS `focusTopTask` 反射。
- 已发现上游 `upstream/main` 前进 35 个提交（1.0.7/versionCode 72），`8b568f9` 与 `98e0bec` 正是 Bug 2/Bug 3 的官方修复，`9aac416` 提供屏幕内状态栏/导航条的 HyperOS 适配。
- 已将本地未提交修改（含状态栏留白实验、计划文件、开发笔记）归档到分支 `local/mi15u-base-20260922`（e5f67e6）。
- 已将 main 重置快进到上游 `51bbc00`（main 原有文档提交 `6b460c4` 导致无法 ff；DEVELOPMENT.md 已恢复为未跟踪文件）。
- 已在新 build.gradle.kts 重放个人签名配置；`:app:signingReport` 确认 Debug/Release/debugAndroidTest 均为个人证书（`0E:45:65...BC:02`）。
- 配置缓存残留旧 daemon Trae JRE 路径导致构建失败；删除 `.gradle/configuration-cache` 并以完整 Adoptium JDK 21 + `--no-daemon` 重跑成功。
- 全量 JVM 测试 48 suite、189 tests、0 failure/error/skipped；Debug APK 15,774,751 bytes，SHA-256 `6F77764B66C8B4E2D939093DC48BEBAB41C9FD2906FDA3554BA9DDC3CA38D592`。
- `apksigner` 确认 APK 单一 v2 签名者为个人证书；代理 `127.0.0.1:7890` 本轮实测可用。
- 已建临时 SDK 视图（`$TEMP/onestep-sdk-view`：ndk-build/aapt2 shim + android.jar）与工具 shim（strings/zip），`local.properties`（已忽略）隔离 Gradle SDK 路径。
- Zygisk 双 ABI 重编成功，`OneStepNativeStatusBarHook` 符号已确认编入 so。
- 完整打包零退出：`dist/OneStep4-1.0.7-magisk-20260922-055751.zip`，ZIP 门禁通过（39 条目、0 重复、0 权限错误、无静态顶层 `zygisk/`），ZIP 内 APK SHA-256 `6F77764B66C8B4E2D939093DC48BEBAB41C9FD2906FDA3554BA9DDC3CA38D592` 且签名为个人证书。
- 最终状态：main = `51bbc00`（上游 1.0.7）+ 未跟踪本地文档（DEVELOPMENT.md、三个计划文件、开发笔记）+ 已修改的 `app/build.gradle.kts`（个人签名）与 zygisk 重编产物；本轮未修改任何设备。
- 用户决策：①只交付不刷入（设备迁移由用户手动执行）；②fork 同步选 force push。
- 执行时发现 fork main 已出现 `6d52f74`（网页端将上游 merge 进 fork），force push 前提失效且会丢文档提交历史；改为本地 `--ff-only` 快进到 `6d52f74`，备份后恢复最新版计划文件。本地与远端完全对齐，无需任何推送。
- 当前工作区形态：三个计划文件为已跟踪+已修改（本地最新 Phase 15 版本，将来推送需先 commit，属普通快进）；`app/build.gradle.kts` 个人签名与 zygisk 重编产物保持本地不推送；`开发笔记.md` 保持未跟踪。
- 设备上线（`192.168.123.81:5557`，xuanyuan / 25019PNF3C，Android 16 SDK 36，在支持范围内）；按用户指令仅传输不刷入：模块 ZIP 已推至设备 `/sdcard/Download/oneStep/OneStep4-1.0.7-magisk-20260922-055751.zip`（18,419,634 bytes），设备端与本地 SHA-256 一致（`0AF83DDA254652A3231F849E2B576EC7DF767BDC95E83C89821E5FA660E889FD`）。设备 Download 下另存有旧 1.0.3/1.0.4 模块 ZIP 与日志，未改动。
- **首次安装失败与修复**：Magisk 日志显示 `customize.sh` line 2 `\r: not found`——同步上游时 `git reset --hard` 在全局 `core.autocrlf=true` 下把工作区全部文本重检出为 CRLF，ZIP 内脚本带 `\r` 导致 Magisk shell 解析崩溃；打包门禁只查哈希/条目/权限，未覆盖行尾，故漏网。
- 修复：仓库级 `git config core.autocrlf input`（.bat 仍由 .gitattributes 保持 CRLF），备份后 `git rm --cached -r . && git reset --hard` 全量重检出为 LF，恢复本地签名配置与计划文件；重打 `dist/OneStep4-1.0.7-magisk-20260922-062919.zip`，ZIP 内全部文本零 CRLF（仅二进制 APK/so 含巧合字节序列），`customize.sh` 纯 LF 确认。
- 已删除设备上失败的旧 ZIP，推送新 ZIP 至 `/sdcard/Download/oneStep/`，双侧 SHA-256 一致（`B4ACCB1E71551A09E298BD828F7172EB73B77C7D5F175779248D19F455DE2B31`）。教训：Windows 环境下每次 git 全量重检出后，打包前必须验证 shell 脚本行尾。
- **迁移完成（用户自行刷入）**：06:31 用户用 Magisk 刷入修复版并重启；06:32 模块服务启动正常（service.log：可信虚拟显示角色授予、OneStep 应用恢复完成、无 /data/app 旧更新包残留）。`pm` 确认 com.sangluo.onestep 为 1.0.7/versionCode 72；设备挂载 APK SHA-256 `4D8E4904...` 与本地个人签名构建一致；dumpsys 显示 `past signatures` 为空（旧作者签名包被干净移除后以个人签名新装），签名一致性门禁通过。用户确认无需保留旧数据，旧设置（含 HyperOS 手势 marker、状态栏 Hook 开关）均未迁移，需时在应用内重新开启。

## 2026-09-22（Phase 16：配置备份/恢复功能）
- 新增用户需求：一键备份/恢复配置，覆盖重装模块或停用后配置丢失（上游无此功能）。
- 新增 `data/settings/SettingsBackupCodec.java`：纯 JSON 编解码（schema v1），含全部 13 项布局设置 + 顶部应用列表（order/selected）+ 背景图 URI；`decode` 对每个数值走 `OneStepSettings.sanitizeXxx`，损坏/异构文件抛 `IllegalArgumentException` 拒绝。5 个 JVM 测试（往返一致性、越界钳制、非法载荷拒绝、缺省容忍、未配置项跳过）。
- `app/build.gradle.kts` 增加 `testImplementation("org.json:json:20231013")`，使 JVM 单测使用真实 JSON 实现（android.jar stub 会抛 not mocked）。
- `SettingsPanelController`：Callbacks 新增 `exportSettingsBackup()/importSettingsBackup()`；设置列表在"开源许可"前插入"备份配置（导出）"与"恢复配置（导入）"两行。
- `MainActivity`：导出走 MediaStore 写 `Download/OneStep4-config-yyyyMMdd-HHmmss.json`（Android 10+ 免存储权限）；导入走 SAF `ACTION_OPEN_DOCUMENT`（JSON/text/plain/octet-stream 兜底）→ 读文件 → decode → `applySettingsBackup` 逐项写 Store + `loadOneStepSettings()` 同步内存态 + `reconcileTopAppListConfiguration()` + 状态栏/顶栏/背景/布局统一刷新。
- 测试 49 套件 194 tests 全过；个人签名 Magisk 模块 `dist/OneStep4-1.0.7-magisk-20260922-065404.zip`（versionCode 73，高于设备上的 72 以保证升级识别），文本零 CRLF，已推送设备 `Download/oneStep/`，双侧 SHA-256 `7E38E48C...2D754` 一致。

## 2026-09-22（Phase 17：侧窗滑动关闭距离自适应）
- 用户需求：多窗口模式（尤其 6 窗）下侧窗横向滑动关闭距离过远，窗宽不足导致无法滑动关闭。
- 原实现：`SIDE_DISMISS_DISTANCE_DP = 48` 固定阈值（`movedPastSideDismissThreshold` 判定 + `OneStepWindowView` 视觉进度共用）。
- 新增 `ui/window/SideWindowDismissDistancePolicy.java`：`resolvePx(fixed, smallestWindowEdge, fraction, min)` —— 阈值 = min(48dp, 最窄可见侧窗边长 × 0.6)，下限 20dp 防误触，窗尺寸未知回退固定值。
- `MainActivity`：新增 `sideWindowDismissDistancePx()` 遍历非主槽可见窗取最小宽（纵向布局取高），判定与视觉进度共用同一值；`getSideDismissDistancePx` callback 与 `movedPastSideDismissThreshold` 均改走新方法。
- 5 个策略 JVM 测试（宽窗保持固定、窄窗按比例、极窄贴下限、未知回退、上下限截断）；全量 50 套件 199 tests 全过。
- 模块 `dist/OneStep4-1.0.7-magisk-20260922-070133.zip`（versionCode 74）推送设备并清理历史 ZIP，仅保留最新版，双侧 SHA-256 `1F383F0D...17C04` 一致。
- 教训：adb push 通配 `dist/*.zip` 会把全部历史包推上设备；推送前先 `rm` 设备旧包、推送用精确文件名。

## 2026-09-22（Phase 18：上滑回桌面后触摸失效修复）
- 用户报告：上滑回桌面后屏幕全部点击无反应，长按可弹菜单但无法二次点击，语音助手杀掉最近任务后才恢复。
- 日志证据（小米15 Ultra `current-session`，3.2MB）：310 条 `E InputDispatcher`，关键行为 `W InputDispatcher: Focused display #0 does not have a focused window.` + `displayId=10, name='com.miui.home/...'`——上滑 HOME 后系统桌面 miui.home 被 OneStep 内置桌面流程放入虚拟显示（display 10）并获得焦点窗口，物理屏（display 0）作为焦点显示却无焦点窗口，InputDispatcher 丢弃全部物理屏触摸。杀掉虚拟显示 task 后焦点回落故恢复。当前 session 的 07:42:56-59 可见三次"Restore OneStep after system HOME"恢复循环。
- 修复：新增 root 输入桥 `focusDefaultDisplay` 命令（服务端 `RootInputBridge` 调 ATMS `focusTopTask(0)`），客户端 `RootInputBridgeClient.focusDefaultDisplay()`；`RootVirtualDisplayHost` 新增 `focusDefaultDisplayAsync(reason)` 与 `hasLiveHostedDisplay()`；`MainActivity` 在 `restoreOneStepHomeNow` 成功后经 `scheduleDefaultDisplayFocusAfterRestore()` 延迟 500ms 把焦点切回 display 0，保证物理屏触摸可达 MainActivity。
- 顺带修复：`focusActiveHostedDisplay` 增加 `hasLiveHostedDisplay()` guard，消除日志中 `Focus promoted display failed: slot=1, display=-1` 的无效焦点请求告警。
- 全量 50 套件 199 tests 通过；模块 `dist/OneStep4-1.0.7-magisk-20260922-075856.zip`（versionCode 75，累积含备份功能/侧窗滑动优化/本修复）推送设备，双侧 SHA-256 `13F1E5F5...82077` 一致，设备仅保留此最新包。注意：用户设备此前仍在运行 versionCode 72（72-74 均未刷入）。

## 2026-09-22（个人版命名规范启用）
- 用户要求：个人版包不再沿用作者版本名。即日起启用个人版命名：`versionName = 上游版本-pN`（p=personal，如 `1.0.7-p1`），`versionCode` 独立递增（当前 76），Magisk 模块文件名与 module.prop 的 version 均随之显示为个人版。
- 已重打包 `dist/OneStep4-1.0.7-p1-magisk-20260922-080434.zip`（versionCode 76，内容=75 + 命名变更，累积备份功能/侧窗滑动/焦点修复三轮改动），替换设备上的 075856 包，设备仅保留此包，双侧 SHA-256 `1ECF3675...660C0` 一致。

## Errors
- 打包脚本首跑在 `strings` 校验步骤失败（Git Bash 无 binutils）：已用 `grep -aoE '[[:print:]]{4,}'` shim 替代，并确认 arm64 so 含 `OneStepNativeStatusBarHook`。
- 环境无 `zip` 命令且 MSYS chmod 在 NTFS 为 no-op：zip shim 以 Python zipfile 按打包意图设置权限（.sh 与 update-binary 0755、目录 0755、其余 0644）；`customize.sh` 安装时 `set_perm` 统一重设，ZIP 权限位仅为对齐。
- 打包脚本首跑经管道 `| tail` 运行导致真实失败退出码被吞：重跑时以 `PIPESTATUS[0]` 复核。
- PowerShell 编码检查首次使用 `foreach (...) { ... } | Format-Table` 触发空管道语法错误；已改用显式结果数组并成功完成根目录文本检查。
- 严格编码复核时再次误用了相同管道形式；随后拆分命令并以 `$rows` 数组完成关键文件严格 UTF-8/LF 检查。
- `:app:testDebugUnitTest :app:assembleDebug` 首次调用超过工具短超时；结果待通过进程和产物检查确认。
- 第二次 Gradle 调用明确失败：`com.android.application:9.2.1` 未能从 Google、Maven Central 或 Plugin Portal 解析；测试和编译任务未开始。
- 已确认 Google Maven 两个 9.2.1 POM 均返回 HTTP 200；用户 Gradle 代理 `127.0.0.1:7890` 的 TLS 握手失败。
- 首次代理覆盖参数被 PowerShell 错误拆分为任务 `.proxyHost=`；改用 `--%` 原样传参。
- 当前 PATH 没有 Bash，规划完成检查改用技能自带 PowerShell 脚本。
- 恢复会话后的首个编码检查命令再次触发 PowerShell 空管道语法错误；已改用显式结果数组，未修改任何文件。
- 远端 UTF-8 校验首次在创建临时目录前失败：`New-Item` 不支持 `-LiteralPath`；未生成临时文件，待改用受边界校验的绝对 `-Path`。
- 深读 Hook 时 Windows 不接受 `rg` 的 `hook/*.java` 路径写法；已改用目录加 `-g`，未修改源码。
- 最新强制重跑构建成功，但 Gradle 报告存在未来与 Gradle 10 不兼容的 deprecated features；不影响本次 Gradle 9.4.1 构建结果。
- 最终门禁首次因压缩 PowerShell 的 `foreach` 语法错误在执行前停止；未修改文件，已改用清晰语法重跑。
- 计划完成检查与编码复核的组合命令因空管道语法错误在执行前停止；未修改文件，已改为显式数组。
