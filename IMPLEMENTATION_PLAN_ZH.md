# Croc Droid 功能增强实施计划

## 概述

计划实现七个功能领域，涵盖状态持久化、密钥生成、设置优化、接收确认、多文件支持、下载管理以及带通知的后台传输。

---

## 需要用户确认

> [!IMPORTANT]
> **功能 7 (前台服务 + 通知)** 是最复杂且风险最高的更改。它需要 `FOREGROUND_SERVICE` 权限和新的 `TransferService`。这将显著增加代码复杂度。请确认是否在第一阶段就需要此功能。

> [!WARNING]
> **功能 6 (接收历史)** 需要本地数据库。使用 Room 是标准做法，但会增加新的依赖。替代方案：在 SharedPreferences 中使用简单的 JSON 文件。你更倾向于哪种？

> [!IMPORTANT]
> **功能 4 (接收确认对话框)** — 目前 bridge.go 使用 `NoPrompt: true`，会自动接受。要实现接受/拒绝，我们需要在 Go 桥接层中新增一个回调 (`OnFileOffer`)，该回调会暂停直到 Kotlin 确认。这需要修改 `bridge.go` 并重新构建 `.aar`。修改 Go 桥接层是否可以接受？

---

## 拟议变更

### 第一阶段：状态持久化 + 密钥生成 + 设置 (功能 1, 2, 3)

这些功能紧密相关，可以一起交付。

---

#### 功能 1：跨页面切换的输入状态持久化

**问题**：SendScreen 中的 `customCode` 和 ReceiveScreen 中的 `code` 使用 `remember { mutableStateOf("") }`，在导航时会重置。

**解决方案**：将所有临时 UI 状态移至 ViewModel 中，使用 `MutableStateFlow` 管理。

##### [修改] [SendViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/send/SendViewModel.kt)
- 添加 `private val _customCode = MutableStateFlow("")` + 公开的 `customCode: StateFlow<String>`
- 添加 `fun updateCustomCode(code: String)`
- 添加 `private val _selectedFileUris = MutableStateFlow<List<Uri>>(emptyList())` + 公开访问器
- 添加 `fun addFiles(uris: List<Uri>)`, `fun removeFile(index: Int)`, `fun clearFiles()`

##### [修改] [SendScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/send/SendScreen.kt)
- 将 `var customCode by remember { mutableStateOf("") }` 替换为从 ViewModel 收集状态
- 文件选择状态也改为从 ViewModel 获取

##### [修改] [ReceiveViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveViewModel.kt)
- 添加 `private val _receiveCode = MutableStateFlow("")` + 公开的 `receiveCode: StateFlow<String>`
- 添加 `fun updateReceiveCode(code: String)`

##### [修改] [ReceiveScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveScreen.kt)
- 将 `var code by remember { mutableStateOf("") }` 替换为从 ViewModel 收集状态

---

#### 功能 2：通过 croc 内部算法生成规范密钥

**问题**：当前代码使用 `"temp-code-${System.currentTimeMillis() % 1000}"`，密钥强度弱且不规范。

**解决方案**：从 Go 桥接层暴露 `utils.GetRandomName()` → 从 Kotlin 调用。

##### [修改] [bridge.go](file:///workspaces/croc-droid/go/crocbridge/bridge.go)
- 添加新的导出函数：
```go
// GenerateCode 生成 croc 风格的助记词密钥 (例如 "1234-apple-banana-cherry")
func GenerateCode() string {
    return utils.GetRandomName()
}
```
- 添加对 `"github.com/schollz/croc/v10/src/utils"` 的导入

##### [修改] [CrocEngine.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/data/croc/CrocEngine.kt)
- 添加：`fun generateCode(): String = Crocbridge.generateCode()`

##### [修改] [SendViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/send/SendViewModel.kt)
- 在 `sendFile()` 中，当未指定自定义密钥时：
  - 修改前：`"temp-code-${System.currentTimeMillis() % 1000}"`
  - 修改后：`crocEngine.generateCode()`

---

#### 功能 3：设置中的固定密钥 (发送与接收分开)

**解决方案**：在 `CrocSettings` 中添加 `fixedSendCode` 和 `fixedReceiveCode` 字段。

##### [修改] [CrocSettings.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/domain/model/CrocSettings.kt)
- 添加 `val fixedSendCode: String = ""`
- 添加 `val fixedReceiveCode: String = ""`

##### [修改] [SettingsScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/settings/SettingsScreen.kt)
- 添加两个新的 `OutlinedTextField` 用于输入固定的发送/接收密钥
- 添加辅助文本说明当这些字段不为空时将覆盖自动生成的密钥

##### [修改] [SendViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/send/SendViewModel.kt)
- 密钥解析优先级：UI 手动输入 → 设置中的 `fixedSendCode` → `crocEngine.generateCode()`

##### [修改] [ReceiveViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveViewModel.kt)
- 如果 `fixedReceiveCode` 不为空，则预填接收密钥输入框

---

### 第二阶段：接收确认对话框 (功能 4)

需要修改 Go 桥接层。

##### [修改] [bridge.go](file:///workspaces/croc-droid/go/crocbridge/bridge.go)
- 添加 `CrocCallback` 方法：`OnFileOffer(fileName string, fileSize int64, fileCount int) bool`
  - Kotlin 返回 `true` 表示接受，`false` 表示拒绝
  - 这将阻塞 Go 协程直到 Kotlin 响应 (gomobile 会处理 JNI 阻塞)
- 将 `NoPrompt` 设置为 `false` 并覆盖 `client.Receive()` 中的提示机制以使用该回调
  - **实际上**，croc 在库级别并不直接支持使用回调覆盖提示。方案应为：
  - 保持 `NoPrompt: true` 但增加一个中间步骤：当 `OnReady` 触发时，返回文件元数据
  - 添加新的桥接函数 `AcceptReceive(id string)` 和 `RejectReceive(id string)`
  - 使用 channel 暂停 `Receive()` 直到接收到接受/拒绝指令

##### [修改] [bridge.go 中的 CrocCallback 接口](file:///workspaces/croc-droid/go/crocbridge/bridge.go)
```go
type CrocCallback interface {
    OnReady(code string)
    OnFileOffer(fileName string, fileSize int64, fileCount int) bool  // 新增
    OnProgress(sent int64, total int64)
    OnSuccess()
    OnError(errStr string)
}
```

##### [修改] [TransferState.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/domain/model/TransferState.kt)
- 添加：`data class FileOffer(val fileName: String, val fileSize: Long, val fileCount: Int) : TransferState`

##### [修改] [ReceiveScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveScreen.kt)
- 当状态为 `TransferState.FileOffer` 时，显示带有文件信息及接受/拒绝按钮的 `AlertDialog`

##### [修改] [ReceiveViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveViewModel.kt)
- 添加调用 `CrocEngine` 的 `acceptTransfer()` 和 `rejectTransfer()` 函数

##### [修改] [CrocEngine.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/data/croc/CrocEngine.kt)
- 处理 `OnFileOffer` 回调 → 更新状态为 `FileOffer`
- 添加接受/拒绝的相关逻辑

---

### 第三阶段：多文件及文件夹支持 (功能 5)

##### [修改] [SendScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/send/SendScreen.kt)
- 使用 `ActivityResultContracts.OpenMultipleDocuments()` 替换 `OpenDocument()`
- 添加 `ActivityResultContracts.OpenDocumentTree()` 用于文件夹选择
- 在 `LazyColumn` 中显示已选文件，并提供删除按钮
- 添加“添加更多文件”和“选择文件夹”按钮

##### [修改] [SendViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/send/SendViewModel.kt)
- 重构 `sendFile()` → `sendFiles()`：接收 `List<Uri>`
- 将所有文件复制到临时目录，然后调用桥接层处理该目录或文件列表

##### [修改] [bridge.go](file:///workspaces/croc-droid/go/crocbridge/bridge.go)
- 修改 `SendFile` 以接收多个文件路径 (逗号分隔或 JSON 数组)
- 或者添加新的 `SendFiles(id string, filePathsJSON string, code string, configJSON string, cb CrocCallback)` 函数

---

### 第四阶段：下载路径 + 历史记录 (功能 6)

##### [修改] [CrocSettings.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/domain/model/CrocSettings.kt)
- 添加 `val downloadPath: String = ""` (为空则默认为 `filesDir/downloads`)

##### [新增] [TransferHistory.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/domain/model/TransferHistory.kt)
- `data class TransferRecord(val id: String, val fileName: String, val fileSize: Long, val timestamp: Long, val savePath: String, val direction: Direction)`
- `enum class Direction { SEND, RECEIVE }`

##### [新增] [HistoryRepository.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/data/history/HistoryRepository.kt)
- 在 SharedPreferences 中以 JSON 列表形式存储历史记录 (简单方案)
- 函数：`addRecord()`, `getHistory()`, `deleteRecord()`, `clearHistory()`

##### [修改] [SettingsScreen.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/settings/SettingsScreen.kt)
- 添加下载路径选择器 (使用 `ActivityResultContracts.OpenDocumentTree()`)
- 添加“查看历史”章节，包含列表显示及删除操作

##### [修改] [ReceiveViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveViewModel.kt)
- 使用 `settings.downloadPath` 替代硬编码路径
- 传输成功后保存记录到 `HistoryRepository`

---

### 第五阶段：后台传输 + 通知 (功能 7)

##### [新增] [TransferService.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/service/TransferService.kt)
- Android `ForegroundService` (`TYPE_DATA_SYNC`)
- 创建带有进度的持久化通知
- 持有 `CrocEngine` 引用，确保应用在后台时传输仍处于激活状态
- 在进度更新/成功/失败时更新通知

##### [新增] [NotificationHelper.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/service/NotificationHelper.kt)
- 应用启动时创建通知渠道
- 构建进度通知 (带有进度条的持续显示通知)
- 构建完成通知 (成功/错误)

##### [修改] [AndroidManifest.xml](file:///workspaces/croc-droid/app/src/main/AndroidManifest.xml)
- 添加 `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
- 添加 `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />`
- 注册 `<service android:name=".service.TransferService" android:foregroundServiceType="dataSync" />`

##### [修改] [CrocDroidApp.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/CrocDroidApp.kt)
- 在 `onCreate()` 中创建通知渠道

##### [修改] [SendViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/send/SendViewModel.kt) + [ReceiveViewModel.kt](file:///workspaces/croc-droid/app/src/main/kotlin/com/henkenlink/crocdroid/ui/receive/ReceiveViewModel.kt)
- 开始传输时启动 `TransferService`
- 绑定到服务以观察状态

---

## 待确认问题

> [!IMPORTANT]
> 1. **功能 4 (接收确认)**：croc 的 `client.Receive()` 原生并不支持异步的接受/拒绝回调。有两种选择：
>    - **方案 A**：将 `OnFileOffer` 作为 **阻塞式** 回调 — Go 会一直阻塞到 Kotlin 返回布尔值。简单但会占用一个协程。
>    - **方案 B**：使用 channel — 添加 `AcceptReceive(id)`/`RejectReceive(id)` 桥接函数和 Go channel。更复杂但更优雅。
>    - 你倾向于哪种？方案 A 是否可以接受？

> [!IMPORTANT]
> 2. **功能 6 (历史记录存储)**：SharedPreferences+JSON 很简单，但在处理非常大量的历史记录时扩展性较差。Room 数据库更健壮但会增加依赖。你的偏好是？

> [!IMPORTANT]
> 3. **功能 5 (文件夹发送)**：在 Android 上，`ActivityResultContracts.OpenDocumentTree()` 返回一个 `Uri`，但 croc 需要文件系统路径。这意味着我们需要递归地将文件夹内容复制到缓存目录，对于大文件夹来说这可能会比较慢。这个取舍是否可以接受？

> [!IMPORTANT]
> 4. **实施顺序**：我建议按照上述阶段顺序执行 (1→2→3→4→5)。每个阶段结束都会重新构建 `.aar` 处于可测试状态。你是希望完成所有阶段还是先从其中一部分开始？

---

## 验证计划

### 自动化
- 修改 bridge.go 后重新构建 `.aar`：`./build_project.sh`
- 验证 Kotlin 编译：`./gradlew assembleDebug`

### 手动
- 在设备/模拟器上安装 APK
- 测试：在发送/接收标签页之间切换 → 验证输入内容是否保留
- 测试：在不输入自定义密钥的情况下发送 → 验证助记词密钥生成 (例如 `1234-word-word-word`)
- 测试：在设置中设定固定密钥 → 验证在未输入自定义密钥时是否使用了该设定
- 测试：接收流程 → 验证确认对话框是否弹出
- 测试：多文件选择 → 验证文件列表管理
- 测试：后台传输 → 验证通知进度显示
