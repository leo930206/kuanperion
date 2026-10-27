# 2.1.0 交接總覽（先讀這份）

更新日期：2026-10-27。此為未發佈開發線，不是完整升級驗收，也不是安裝說明。

## 1. 哪份才是目前成果

- Repository：<https://github.com/leo930206/kuanperion>，開發分支 `feature/2.1.0`。先讀 `git status`、`git log -5 --oneline`，不要假設 HEAD 就是穩定版。
- 正式穩定線：`main`／`v2.0.17`，commit `a126ea51f1f916efd69d4c2374f58f2373512363`。這次開發不可覆蓋使用者正在用的 JAR 或設定。
- 原生合書操作基底：`8469514`；指定主背包格取回與本交接入口：`e7055e5`；官方 Wiki 證據註記：`25b58aa`。後續最新 revision 以 Git 為準，無需仰賴聊天記憶。
- 來源與可重跑測試都在 repository。`/private/tmp` 僅為分段工作／測試場，不是交接必需的唯一來源；個人 agent memory 不是權威文件。
- 開發產物：`build/libs/kuanperion-2.1.0.jar`。建出 JAR 不等於可發佈、已安裝或已連服驗收；build 會重建 `build/`。

閱讀順序：本檔 → [進度清單](../tasks/todo-2.1.0.md) → [規格](spec-2.1.0.md) → 對應模組文件／測試。較早的驗證段落是歷史檢查點，不應覆蓋進度清單的最新狀態。

## 2. 範圍與不可變條件

- Farming 頁：Farming、Pest（含原通知）、Greenhouse、Visitor（含原通知）、Composter。
- Trade 側欄位於 Mining／Other 之間：NPC／Bazaar 自動出售、Book Combine、Pet Sell、Junk、Stash。
- Other 保留 Jerry／Webhook；Supercraft 為獨立模塊。Freecam 已取消。
- 不帶入 Aether 作者服務、帳號、IRC、遙測、Discord 遠控、自動更新。只允許必要的匿名官方遊戲／價格資料與使用者明確設定的單向 Discord 通知。
- 新自動化預設關閉；採購須有明確物品／數量／預算／伺服器成交證據。不能新增無實作開關假裝完成。
- 保留原 Farming／Mining、選取十種方塊、配色／收藏／快捷鍵、Jerry、靜音／停滯保護與更新沿用設定。Shift+G `/warp garden`、Farming 啟動搜尋九格工具在兩條線都已保留。
- 新任務只能持有原先手動開啟的 Farming；成功亦不能把停止／重開過的 Farming 自動打開。手動操作、Mining、Jerry、換世界／斷線／死亡／異常回程有優先權。

## 3. 已接線與尚未完成

| 區塊 | 目前開發線 | 尚未完成／驗收 |
| --- | --- | --- |
| 共用底層 | 單一可撤銷 root、原 Farming hold、世界／選單／物品／游標回條、外部設定及停用保護 | 後續新模組全生命週期與跨任務接續矩陣、連服驗收 |
| Pest | 已載入目標、可選地塊搜尋、陷阱釋放／捕捉／返回及原通知 | 補餌原生角色／容量／導航／採購接線、兌換／噴灑、完整活動／作物策略 |
| Greenhouse | 設定地塊／工具／作物、收割回條、返回、service／UI | 即時伺服器驗收 |
| Composter | 背包既有材料補充、回條、返回、service／UI | Sacks／採購、當前選單與連服驗收 |
| Visitor | 明確對象／接單估價、背包材料提交、返回、service／UI | 預算控管採購與當前連服格式／結果驗收 |
| Trade | NPC Auto Sell／Junk／Stash 已接線；Book 已接預設關閉的設定／service／Trade UI、固定 `/anvil` 與原生操作／指定格取回；Pet 有唯讀資格預檢 | Bazaar 實際採購／出售、Book 實際連服的開啟權限／費用與結果回條驗收、George 估價／交易／取回接線、各項連服驗收 |
| Supercraft | 明確選定且符合當前預覽批量的 inventory-only 任務、原生 adapter／service／Other UI | 當前來源／費用格式、必要時實際選量、較廣配方與連服驗收 |

進度勾選代表該檢查點完成，不代表整列已在 Hypixel 成功運行。先前提供的約 60% 是整體交付粗估，不可用測試數或打勾比例冒充驗收率。

Greenhouse／Composter 的已到期工作已補上「忙碌 root 讓路、釋放後重新守門派送」，見 [維護派送檢查點](maintenance-dispatch-2.1.0.md)。Bridge 另有單次 UI 意圖優先、Stash 隔離及 Supercraft 在 Visitor 前的 due 順序；不代表整條原生 Pest 後續任務鏈已驗收。

## 4. 快速找到程式

- `KuanperionClient.java`：舊 Farming／Mining／快捷鍵及新任務橋接入口。
- `AutomationClientBridge.java`：原生觀察、固定 typed commands、service 裝配與全域停止／手動優先路徑。
- `automation/AutomationCoordinator.java`、`AutomationSession.java`、`FarmingHandoff.java`：root、資源、原 Farming 接續；安全停止回呼錯誤不可被吞掉。
- `automation/AutomationSettingsStore.java`：`config/kuanperion/automation.json`。舊 properties／theme／Minecraft `options.txt` 不改位置；損壞／未來格式不可覆寫。
- `mixin/AutomationReceiptMixin.java`：在真正 native inventory write **之後**記錄，而非整個 handler TAIL；被忽略的封包不能成為證據。
- `automation/{pest,greenhouse,composter,visitor,supercraft,pets}/`：各自的純邏輯；根 package 的 `*GameView`／flight controller 為 Minecraft 邊界。
- Book：`BookGameView` → `automation/BookInventorySnapshot`／`BookCombinePlanner` → `BookCombineTask` → `BookMenuController`；服務為 `BookService`、設定介面為 `ui/BookModuleSettings`；指定格取回由 `BookReturnTransfer` 逐步驗證，原生 carried deposit 僅允許自己的完整一本書放到明確空主背包格。

## 5. 怎麼驗證、證明到哪一層

現有 scripts 使用本機 PrismLauncher 的固定 Minecraft26.2、Java25、Fabric 依賴（包含 Gson）。換電腦先準備對應本機依賴／檢查 scripts 路徑；不要宣稱單純 clone 即可在任意環境直接運行。

```bash
git status --short
./scripts/test.sh
./scripts/build.sh
python3 scripts/test-native-observations.py
./scripts/test-ui.sh
./scripts/test-ui-migration.sh
git diff --check
```

- `test.sh`：舊功能、純任務／service、擷取實際方法的可控邊界、原生源碼／bytecode 及負向變異回歸。Book carried-native 測試執行實際方法本體，但遊戲狀態是 fixtures。
- `build.sh`：真實 pinned26.2 API 編譯與來源／JAR 網路端點、依賴、私密路徑／測試 payload、授權稽核。
- `test-native-observations.py`：真實26.2 ItemStack／NBT／Slot 等無視窗 fixtures，以及真實 controller／task 配可控環境；不是伺服器交易或完整 native GUI 實跑。
- `scripts/ui-smoke/`：獨立測試 game 目錄／虛構身份的 GUI／Mixin 驗證，不能改用使用者正式 instance 或把 dummy token 啟動說成 Hypixel 實測。最近完整既有 UI 檢查點為38狀態；不表示其後每一個 JAR 都重新啟動過 GUI。
- 本機預測、唯讀配方、估價、未知選單、單一兄弟 slot 回條或別的 root／world／container 的證據，不能當成本次成功。

## 6. 接下來怎麼做

1. 先核對最新 Book 任務、指定格取回與原生操作的測試；新 `Port.progressReturn` 是明確推進點，禁止把原生操作藏到 getter／tick 以外的背景回呼。
2. Book 的外部設定、單次／自動 service、Trade UI、全域停止與固定 `/anvil` 已接線。先讀 [目前 Anvil 證據](anvil-current-evidence-2.1.0.md)：Kuan 的截圖提供29/33/13/22角色候選及書＋書分行明確零費用格式；亦支援有新伺服器回條的合成直接游標結果。不假設按下後處置或所有帳號權限，維持當次費用及回條守門；不能改回 `/av`＋定時點22，不能把缺費用當免費。
3. 依進度清單完成其他採購、Pet／Pest 及串接；必要的當前選單證據不足時，保持拒絕，不猜材料／金額／容量／按鈕角色。
4. 每次小步驟先 RED→GREEN，再跑相應完整回歸／原生編譯與稽核，檢查未完成模組未公開、設定未變、原模組未回歸。
5. Git commit／push 至 `feature/2.1.0`。**整體完成且驗收後**才考慮 main／2.1.0 tag／正式安裝；不要把開發 JAR 複製到使用者 `mods/` 來冒充已完成。

交接不需要提供帳號、token、Webhook、正式設定或完整個人日誌；只提供此分支／文件／測試與必要且去識別的選單證據。

Supercraft 的單次 UI 意圖已統一使用 ManualTaskRequest(false)，詳見 [Supercraft 邊界](supercraft-protocol-notes.md#single-ui-intent-lifecycle)；原世界／Farm／外來root失效不得因關選單而重播。這不等於已支援尚未確認的 quantity-setting 選單。
