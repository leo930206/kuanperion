# 2.1.0 交接總覽（先讀這份）

更新日期：2026-10-29。此為未發佈開發線，不是完整升級驗收，也不是安裝說明。

> 最新指示（2026-10-29）：先依 Aether 原流程完成實作，把所有未確認的相容性假設記入 [原版相容性清單](upstream-compatibility-2.1.0.md)。**不要逐項等待／重問截圖**；全數實作完成後再一次核對。保留物品／金額／手動停止保護，實作完成不等於連服驗收。

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
| Visitor | 明確對象／接單估價、背包材料提交、返回、service／UI | 已新增明確預算／材料原生 ID 的 custom-amount 採購及重新確認接單；單次額外確認已接線；當前連服格式／結果仍待驗收 |
| Trade | NPC Auto Sell／Junk／Stash 已接線；Book 已接預設關閉的設定／service／Trade UI、固定 `/anvil` 與原生操作／指定格取回；Pet 有唯讀資格預檢與原root單次成交證明元件（無原生caller） | Bazaar 實際採購／出售、Book 實際連服的開啟權限／費用與結果回條驗收、George 估價／交易／取回接線、各項連服驗收 |
| Supercraft | 明確選定且符合當前預覽批量的 inventory-only 任務、原生 adapter／service／Other UI | 當前來源／費用格式、必要時實際選量、較廣配方與連服驗收 |

進度勾選代表該檢查點完成，不代表整列已在 Hypixel 成功運行。目前對使用者保守提供約 65% 的整體交付粗估，不可用測試數或打勾比例冒充驗收率；採購、Pet Sell、Pest 後續及連服驗證仍是實質缺項，不是只剩測試。

Greenhouse／Composter 的已到期工作已補上「忙碌 root 讓路、釋放後重新守門派送」，見 [維護派送檢查點](maintenance-dispatch-2.1.0.md)。Bridge 另有單次 UI 意圖優先、Stash 隔離及 Supercraft 在 Visitor 前的 due 順序；不代表整條原生 Pest 後續任務鏈已驗收。

## 4. 快速找到程式

- `KuanperionClient.java`：舊 Farming／Mining／快捷鍵及新任務橋接入口。
- `AutomationClientBridge.java`：原生觀察、固定 typed commands、service 裝配與全域停止／手動優先路徑。
- `automation/AutomationCoordinator.java`、`AutomationSession.java`、`FarmingHandoff.java`：root、資源、原 Farming 接續；安全停止回呼錯誤不可被吞掉。
- `automation/AutomationSettingsStore.java`：`config/kuanperion/automation.json`。舊 properties／theme／Minecraft `options.txt` 不改位置；損壞／未來格式不可覆寫。
- `mixin/AutomationReceiptMixin.java`：在真正 native inventory write **之後**記錄，而非整個 handler TAIL；被忽略的封包不能成為證據。
- `automation/{pest,greenhouse,composter,visitor,supercraft,pets}/`：各自的純邏輯；根 package 的 `*GameView`／flight controller 為 Minecraft 邊界。
- Bazaar：`automation/BazaarPurchaseView` → `BazaarPurchaseController` → 既有 `BazaarPurchaseAttempt`／ledger，為 provisional 終端購買 adapter；BazaarAcquisitionController 的 Custom Amount 導航／sign／sidebar 餘額已由 VisitorFlightController 接到原 VisitorTask，其他 parent 尚缺；單次額外確認已接線，未知警告仍拒絕。
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
- `scripts/ui-smoke/`：獨立測試 game 目錄／虛構身份的 GUI／Mixin 驗證，不能改用使用者正式 instance 或把 dummy token 啟動說成 Hypixel 實測。最近完整既有 UI 檢查點為39狀態；不表示其後每一個 JAR 都重新啟動過 GUI。
- 本機預測、唯讀配方、估價、未知選單、單一兄弟 slot 回條或別的 root／world／container 的證據，不能當成本次成功。

## 6. 接下來怎麼做

1. 先核對最新 Book 任務、指定格取回與原生操作的測試；新 `Port.progressReturn` 是明確推進點，禁止把原生操作藏到 getter／tick 以外的背景回呼。
2. Book 的外部設定、單次／自動 service、Trade UI、全域停止與固定 `/anvil` 已接線。先讀 [目前 Anvil 證據](anvil-current-evidence-2.1.0.md)：Kuan 的截圖提供29/33/13/22角色候選及書＋書分行明確零費用格式；亦支援有新伺服器回條的合成直接游標結果。不假設按下後處置或所有帳號權限，維持當次費用及回條守門；不能改回 `/av`＋定時點22，不能把缺費用當免費。
3. 依2026-10-29使用者修訂：先參照原 Aether 流程繼續採購、Pet／Pest 與串接，將原版角色／格位／格式假設記入 upstream-compatibility-2.1.0.md；不要逐項等截圖。保留預算、物品、手動停止等守門，不憑空編造來源沒有的事實。全部實作完成後再一次核對現行伺服器。
4. 每次小步驟先 RED→GREEN，再跑相應完整回歸／原生編譯與稽核，檢查未完成模組未公開、設定未變、原模組未回歸。
5. Git commit／push 至 `feature/2.1.0`。**整體完成且驗收後**才考慮 main／2.1.0 tag／正式安裝；不要把開發 JAR 複製到使用者 `mods/` 來冒充已完成。

交接不需要提供帳號、token、Webhook、正式設定或完整個人日誌；只提供此分支／文件／測試與必要且去識別的選單證據。

Supercraft 的單次 UI 意圖已統一使用 ManualTaskRequest(false)，詳見 [Supercraft 邊界](supercraft-protocol-notes.md#single-ui-intent-lifecycle)；原世界／Farm／外來root失效不得因關選單而重播。這不等於已支援尚未確認的 quantity-setting 選單。

共用購料的缺量計畫／原root單次購買驗收見 [Bazaar 購料邊界](bazaar-acquisition-2.1.0.md)。估價不能傳成 GUI 證明；下一步是當前原生選單／數量／費用解析與 parent wiring，不是再新增購料分頁或開放未完成開關。

尚缺的原生選單證據集中在 [當前伺服器證據清單](live-evidence-needed-2.1.0.md)。依最新使用者修訂延到全部實作完成後一次收集；現在先沿用有來源註記的原版流程，仍不能以盲點擊或原作者服務替代保護／驗證。已提供的 Anvil 書＋書圖不必重問。

長模組介紹不再固定46px：MainGUITextLayout.groupHeaderHeight 同時供繪製、點擊與搜尋位移使用。勿只改畫面高度卻保留舊65px列偏移；GroupHeaderLayoutTest／shared-wiring guard／實際Book勾選smoke一起守住。原Setting偶發實跑失敗仍留在verification／待辦，未以重跑通過冒充根因修復。

Pest 策略的純目標選擇見 [策略規劃邊界](pest-strategy-2.1.0.md)：未知活動資料不可當未舉辦，未授權作物不可選第一個硬套；沒有複製舊第三方活動 feed／預設映射或提供未完成開關，實際套用仍待原生接線。

Book／Supercraft 狀態讀取錯誤及 Pest 忙碌派送的最新檢查點見 [工作流程觀察邊界](workflow-observation-2.1.0.md)。未知視窗不能冒充自己的正常選單；停止回呼錯誤不可被廣泛 catch 吞掉；Pest 不應消耗其他 root 期間的預檢或 poll。這些離線回歸不代表完整九服務／原生接續或交易操作已完成。

Jerry 全域停止現在也會在單一服務回呼失敗後繼續通知其餘八個服務，最後保留原始與後續錯誤；見同份文件的 dispatcher fault matrix。其後的 [手動／換世界／shutdown 檢查點](lifecycle-stop-2.1.0.md) 已擴大 dispatcher 覆蓋，另保護 client-stopping 的 renderer 清理。實際 Bridge 方法＋可控服務 actors／真 session 驗證仍不代表每個服務原生清理、真實 GL teardown 或整個連服矩陣都已成功。

Stash 的 [讀取錯誤／run 世代檢查點](stash-observation-2.1.0.md) 補上獨立 command lease 的未知狀態取消、原請求限定與首發計時重置。不要把 Shared AutomationSession 的守門誤認為已保護這條獨立 Stash 路徑；也不要讓舊讀取／舊 sender 結果覆寫新的手動領取任務。此為實際 service/task/coordinator 離線證據，沒有更動原生指令或宣稱連服領取成功。

2026-10-29 首次完整 GUI 在 phase33 由 Mining 跳到 Trade，失敗記錄已保留。後續加入僅 smoke 啟用的[切頁呼叫來源診斷](ui-smoke-diagnostics-2.1.0.md)，新的完整39狀態／39截圖及原生關閉驗證通過；該次記錄73個 fixture click，沒有其他 native click。這不是舊偶發問題的根因修復：phase33 與較早 gear 問題均未重現／定位，輸入干擾仍只是未證實假設，驗收項目不能因此打勾。詳見 verification／todo。

Pet 的[單次成交驗證底層](pet-sale-attempt-2.1.0.md)只接受明確原pet／最低價及 declared quote/ack，再驗證成對伺服器物品／游標回條；不是 George 原生估價／賣出功能，不能將 typed notice 當成現有伺服器格式。

Bazaar 的[整批商品出售規劃](bazaar-sale-plan-2.1.0.md)區分 API gross estimate 與明確最低實收設定，固定經批准的原生商品 exemplar／完整43格與整批上限。它仍無 native caller，不可用估價授權 Sell Inventory，也未切換原 NPC Auto Sell backend。

Bridge 目前先 tick 原 active 工作、再派送其他 due 工作，每個 service 同次只 tick 一次，消除「後序工作完成卻要跨下一個 legacy tick 才能接到前序 due 工作」的派送排序空檔。詳見 [維護派送檢查點](maintenance-dispatch-2.1.0.md)；不能把這項離線 dispatcher 修正說成全部 Pest 後續任務已完成。

2026-10-29 Visitor 採購已接上原 task／共享預算／外部設定／UI，見 Bazaar 文件最新段落。仍不是全模組完成；下一步補額外確認與其他 parent、Pet/Pest 等實質缺項，不能再將「沒有 caller」的舊段落當作最新狀態。
