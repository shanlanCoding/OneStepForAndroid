# OneStepForAndroid 二次开发文档任务

## Goal
阅读并理解当前项目，基于源码证据生成可供后续二次开发使用的中文文档，并完成必要验证。

## Phases

### Phase 1: 仓库与构建系统盘点
**Status:** complete
- 确认目录、文本编码、换行风格、模块和构建入口。

### Phase 2: 架构与业务流程分析
**Status:** complete
- 追踪应用入口、核心模块、数据流、系统交互和配置点。

### Phase 3: 二次开发文档编写
**Status:** complete
- 生成基于源码证据的中文开发指南。

### Phase 4: 验证与交付
**Status:** complete
- 校验文档编码、链接、源码引用和可用的构建命令，发送 Windows 通知。

### Phase 5: 安全更新到最新上游
**Status:** complete
- 为旧快照 `5537a19` 建立恢复引用，验证远端文本编码后更新受跟踪文件。

### Phase 6: 重新分析并修订开发文档
**Status:** complete
- 分析 23 个上游提交带来的架构和工作流变化，更新 `DEVELOPMENT.md`。

### Phase 7: 最新源码验证与交付
**Status:** complete
- 运行单元测试和 Debug 构建，复核 Git 状态、文档编码和输出产物。

### Phase 8: 全屏状态栏重叠诊断
**Status:** complete
- 获取设备 `58a9fb4b` 的 OneStep4 日志，追踪小窗口全屏后的布局与 Insets 处理。

### Phase 9: 状态栏高度预留实现
**Status:** complete
- 在最小影响范围内修正全屏窗口工作区，保留现有非全屏和大屏布局行为。

### Phase 10: 构建与真机安装门禁
**Status:** complete
- 运行相关测试和 Debug 构建；尝试更新目标设备，并在签名不匹配时保持设备不变、明确运行时验证边界。

### Phase 11: Magisk 模块交付与设备更新门禁
**Status:** complete
- 对比既有 Magisk ZIP、仓库打包脚本和设备模块目录，构建完整模块；签名不匹配时保持设备不变并报告迁移条件。

### Phase 12: 个人签名密钥生成与备份
**Status:** complete
- 在仓库外生成长期固定的个人 APK 私钥和受 ACL 保护的恢复副本，不在日志或聊天中暴露密码。

### Phase 13: Gradle 固定签名接入
**Status:** complete
- 让 Debug/Release 在本机签名配置存在时统一使用个人密钥，同时保持开源仓库无私钥也可构建。

### Phase 14: 签名构建与模块验证
**Status:** complete
- 重建 APK/Magisk 模块，核对证书指纹、测试、ZIP 内容和设备未变更状态。

### Phase 15: 小米15 Ultra 适配与 bug 修复（上游同步）
**Status:** in_progress
- 恢复上下文并确认目标设备 58a9fb4b（小米15 Ultra）不在线，仅小米6 在线。
- 确认开发笔记中三个 bug：状态栏重叠（本地已修）、触发区域拦截点击、微信小窗口输入焦点。
- 发现上游 `upstream/main`（51bbc00，v1.0.7/versionCode 72）已包含 35 个新提交：`8b568f9` 修复全屏应用左右上角无法点击（Bug 2）、`98e0bec` 修复输入法策略"假失败"（Bug 3）、`9aac416` 屏幕内状态栏/导航条（HyperOS 适配）等。
- 归档本地未提交修改到 `local/mi15u-base-20260922`，main 快进到上游最新。
- 重放个人签名配置，测试构建后打包个人签名 Magisk 模块。

## Next Step
完成 Phase 15 的构建验证与模块打包；设备在线后需真机迁移（旧设备仍是作者签名 1.0.3，新装个人签名包需按 Magisk 模块刷入路径处理包身份）。

## Decisions Made
| Decision | Rationale |
|---|---|
| 使用独立中文开发文档 | 便于后续开发者按主题检索，避免改写未知用途的现有说明 |
| 所有结论必须能追溯到当前源码 | 当前 Git 工作树没有已跟踪基线，不能依赖提交历史推断 |
| 以最新 `origin/main` 作为后续适配基线 | 用户未改源码，本地受跟踪文件精确匹配旧提交 `5537a19`，上游已前进 23 个提交 |
| 更新前保留旧快照引用 | 即使更新工作区，也能随时恢复或对比旧版行为 |
| 先以设备日志和当前源码确定重叠路径 | 状态栏留白已有设置与多窗口/全屏策略，必须避免重复叠加 inset |
| 完整模块只构建不刷入 | 新旧 APK 签名不同，Magisk 挂载不能绕过 PackageManager 的系统包签名一致性校验 |
| 使用仓库外的固定个人签名配置 | 避免泄露私钥，同时确保 Debug/Magisk 和 Release 后续更新始终具有同一包身份 |
| 放弃本地状态栏留白实验、整体同步上游 | 上游 `9aac416` 用虚拟显示内 SystemUI 原生状态栏方案解决同一问题，与本地布局留白方案冲突（双重留白），作者方案更完整且经真机验证 |
| 本地两个 bug 不再手写修复 | 上游 `8b568f9`（dispatchTouchEvent 触发手势检测+点击自然穿透）与 `98e0bec`（IME 策略就绪判定）正是这两个 bug 的官方修复 |

## Errors Encountered
| Error | Attempt | Resolution |
|---|---|---|
| 首次并行命令因无匹配检索返回非零 | 1 | 将检索拆分并显式处理 rg 的无匹配状态 |
| git status 报当前目录不是 Git 工作树 | 1 | 记录限制，继续基于工作区文件做静态分析 |
| PowerShell 编码检查脚本出现空管道语法错误 | 1 | 改用结果数组收集后再输出，检查成功 |
| 严格编码复核重复使用了错误管道形式 | 2 | 停止该写法，拆开命令并使用显式 `$rows` 数组后成功 |
| Gradle 测试与构建命令超过工具短超时 | 1 | 先检查存活进程与产物状态，避免重复启动并发构建 |
| Gradle 无法解析 Android application plugin 9.2.1 | 2 | 构建在配置阶段失败；只读检查 Google Maven URL 后记录验证边界，不擅自改版本 |
| PowerShell 拆分空 proxy host 的 `-D` 参数 | 3 | 改用 `--%` 停止解析，确保参数原样传给 Gradle Wrapper |
| Gradle 选中缺少 jlink 的 Trae JRE 21 | 4 | 关闭本机 Java 自动探测，按项目 Foojay 条件获取完整 JDK 21；测试与构建成功 |
| 当前 PATH 中没有 Bash，无法运行 check-complete.sh | 1 | 改用技能目录内的 check-complete.ps1 |
| 恢复会话时编码检查再次出现 PowerShell 空管道语法错误 | 1 | 立即停止该写法，改用显式 `$rows` 数组并完成严格 UTF-8/LF 检查 |
| 远端 UTF-8 校验脚本用 `New-Item -LiteralPath` 创建临时目录失败 | 1 | 当前 PowerShell 该 cmdlet 无此参数；确认目录未创建后改用已校验绝对路径的 `-Path` |
| Windows 下向 `rg` 传入 `hook/*.java` 通配路径失败 | 1 | 改为传入目录并使用 `-g '*.java'` 文件过滤器 |
| 最终门禁的压缩 PowerShell `foreach` 缺少必要空格 | 1 | 脚本在执行前停止；改用清晰的多行变量与循环语法后重跑全部门禁 |
| 计划完成检查与编码复核组合命令再次出现空管道语法错误 | 1 | 两项检查尚未执行；改为显式 `$rows` 数组并分开运行 |
| 本轮编码检查再次使用 `foreach { } | Format-Table` 触发空管道语法错误 | 1 | 命令在读取阶段停止；立即改用显式 `$rows` 数组并成功确认 UTF-8/LF |
| 首次源码补丁因 import 顺序上下文不匹配被拒绝 | 1 | 补丁整体未写入；读取准确上下文后拆成小补丁并成功应用 |
| 首次 Gradle 测试缺少 Android SDK 路径 | 1 | 配置阶段停止；从本机目录确认 SDK 为 `D:/software/androidSDK`，改用临时环境变量 |
| Gradle 两次选中缺少 jlink 的 Trae Java 21 | 2 | installations 参数受旧 daemon 探测影响；改为让本次 Gradle 直接由完整 Adoptium JDK 21 启动并禁用 daemon，测试成功 |
| `adb install -r` 返回 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 1 | 现装系统 APK 与 Debug APK 签名不同；停止安装，不卸载、不清数据、不替换 `/system/priv-app` |
| 广域候选密钥递归搜索两次超时 | 2 | 改为核对仓库签名配置、显式密钥目录与已发现候选；未找到匹配证书，停止扩大搜索 |
| 设备模块/挂载查询的远端 shell 引号错误 | 1 | 两条只读命令失败且未改设备；改用固定模块 ID 和单一 `su -mm -c` 字符串后成功 |
| 初次模块构建仍选中 Trae 精简 JRE | 1 | clean 后在配置阶段停止；通过 `GRADLE_OPTS` 固定完整 JDK 21、禁用 daemon/cache/自动探测 |
| Magisk 打包脚本在 Windows 未识别 `ndk-build.cmd` | 1 | APK/runtime 已成功；安装并校验官方 NDK r29，使用已忽略的临时转发入口完成双 ABI 重编 |
| 状态栏 overlay 脚本在 Windows 未识别 `aapt2.exe` | 1 | native 已成功；使用临时无扩展转发入口完成 overlay 构建，交付后已删除入口 |
| 首次 ZIP Unix 权限修正发生有符号整数转换错误 | 1 | 内容校验通过但权限未变；改用十六进制字符串解析，复核脚本 0755、普通文件 0644 |
| 首次密钥 ACL 复核使用 `foreach { } | Format-List` 触发空管道语法错误 | 1 | 命令在读取前停止；改用显式数组后确认 ACL 和 UTF-8 均符合要求 |
| 个人签名组合构建在 `lintVitalAnalyzeRelease` 下载依赖时失败 | 1 | Debug/Release 编译和签名校验已过；用户级 `127.0.0.1:7890` 代理未监听，改用本次命令临时清空代理重跑 |
| 首次个人签名 Magisk 打包的外层执行窗口超时 | 1 | 5 秒执行限制断开日志会话，Java 子进程结束且未生成 ZIP；改用可续接的长运行会话重跑 |
| 个人签名 ZIP 首次 Unix 权限修正发生有符号整数转换错误 | 1 | ZIP 内容未变但权限结果不可采信；改用十六进制字符串解析为 UInt32 后覆盖全部条目 |
| 首次设备复合 root 只读查询的远端引号边界错误 | 1 | APK 哈希成功但后续查询落回 shell 权限；将完整 `su -mm -c` 内容作为一个远端字符串后成功 |
