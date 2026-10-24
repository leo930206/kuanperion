# 2.1.0 開發檢查點驗證（未發佈）

日期：2026-10-24。這份紀錄不是完整大升級驗收；未完成項目見 `tasks/todo-2.1.0.md`。正式 main、v2.0.16 與安裝中的 2.0.16 JAR 不隨開發檢查點更新。

## 已執行

- `./scripts/test.sh`：通過所有原有功能測試，另含自動化 runtime／Stash service／真實 adapter 方法測試。
- `./scripts/build.sh`：成功建立開發版 `kuanperion-2.1.0.jar`；未安裝到正式遊戲。
- `./scripts/test-ui.sh`：UiModelTest、ChatStyleTest 通過。
- `./scripts/test-ui-migration.sh`：通過舊設定／快捷鍵／風格遷移回歸。
- `scripts/ui-smoke/prepare.py`：使用本機已安裝的 26.2 依賴建構獨立測試客戶端，虛構帳號、獨立 game 目錄、不加入伺服器。

- 獨立 Minecraft 26.2 實跑：`UI_SMOKE_PASS`，36 個狀態／截圖；包含新的第五側欄路由、Trade／Stash 頁面、真實設定保存、預設關閉、離線手動拒絕，及既有 resize／置中／頂層懸停／快捷鍵／配色／文字樣式檢查。人工檢視 Setting 展開側欄與 Stash 詳情截圖，未見新增元件重疊。
- 初輪 smoke 在配色頁切換檢查失敗；保留初輪 log，加入立即路由斷言後，以乾淨 game 目錄重跑 36 狀態通過。未為此修改配色邏輯；原因尚未確認，不把首次失敗隱藏為通過。

TDD：先新增 service 測試確認缺少實作時失敗；再實作測試通過。Adapter 測試先暴露「其他伺服器 + 過期 island API」仍可通過的問題，修正為必須同時符合已知 Hypixel 網域及 SkyBlock 證據，再確認綠燈。另以現有 sidebarLines 的實際小寫輸出新增紅燈測試，修正 Garden fallback 大小寫後確認通過。

## 實作與覆蓋

- 設定：啟動不寫檔、預設全關、重啟讀取、未知欄位保留、原 properties 不變、損壞／較新格式不覆寫、外部修改衝突保留。
- 權限：單一 owner、世界 session、過期／舊 token、硬阻擋、焦點／GUI／背包滿、Mining／回程／選取任務、Garden 權限、奈秒時鐘回捲。
- Stash：型別限定 `/pickupstash`、重複觸發不重設上限、時間間隔、嘗試上限、逾時、失焦／GUI 期間不送、背包滿停止、世界改變與手動指令優先、發送失敗不當成功、關閉設定即使寫檔失敗也停止本次與本 session 的自動領取。
- 通道：只採 server GAME、不採 signed player CHAT；拒絕玩家格式／本機前綴訊息；新來源碼無 HTTP／Socket／帳號／遙測或 Aether 後端入口。
- Adapter：擷取實際 observe/send 方法，注入可控遊戲邊界；測試 known-server regex、偽網域／偽 sidebar／過期 API 拒絕、安全 context 的唯一 typed command、自己的設定 GUI 與其他選單的差異。

## 限制／未驗證

- 尚未進 Hypixel 伺服器驗證 Stash 的當前訊息格式及實際領取結果。只接受已知格式；不識別時不自動排程。手動流程同樣有停止及次數／逾時上限。
- 離線測試證明程式守門／狀態／UI／保存，不代表所有即時伺服器互動已驗收；安全停下也不等於成功領取。
- Pest、Visitor、Greenhouse、Composter、其餘 Trade、Supercraft、移動／GUI 工作流程接管 Farming 等仍未完成，不提供無效開關假裝可用。
- 無新版正式標籤、無 main 合併、無正式 JAR 替換。原 2.0.16 可由 `backup/pre-2.1.0` 還原。

## 安全執行底層／Trade 圖示檢查點

- Trade 改為雙向交換箭頭，與 Farming／Mining 同為 24px viewBox、2px 圓角描邊、無半透明外圈；新增 XML／側欄引用回歸檢查。
- 新增精確物品 ID 白名單／保護規則、保留與提交花費預算、完整選單 identity/revision/slot ticket、26.2 唯讀物品與選單資料、受權限限制的點擊 adapter。通用 GUI 點擊不能修改玩家背包；背包動作須另附保護政策，THROW 只允許 Junk owner。
- 新增伺服器封包 TAIL 觀察的 InventoryReceipts；本機預測、別的 slot、過期或不同 world 的資料不能冒充交易成功。僅有 gameplay session 時記錄，不送資料、不寫設定、不額外發封包。
- Farming hold 實際接進既有 tick／輸入鎖／停滯偵測；手動停止或新 session 會失效，成功只能釋放原本的 hold，不能把 Farming 開關重新打開。錯誤／逾時安全停下，原有異常回程有優先權。未知選單或換成另一個同名 container 會使 session 失效。
- AutomationSafetyTest：白名單／工具／UUID 保護、預算溢位／重播、選單變更、伺服器回條、Farming hold 與新舊 session／世界／未知 GUI。
- 26.2 原版 Item 的元件在未進伺服器時尚未綁定，初次實體測試因此失敗；測試改用明確 Holder.direct 元件 fixtures，不改寫原版 registry，也不把 fixtures 當作 live server 證據。獨立客戶端確認 NBT／lore／metadata fingerprint／工具與綁定物保護，以及 ClientPacketListener 的回條 Mixin 確實套用。
- 完整 36-state UI smoke PASS，另對後續 menu ownership 補強版本重跑初始化檢查；Trade 圖示人工檢視通過。完整紀錄保留於獨立 tmp game，不進正式遊戲。

這仍是開發底層，不表示 Auto Sell／Junk 等工作流程已完成；下一階段才串接實際服務。

## /warp garden 快捷鍵修正 — 2026-10-24

原 /home 顯示名稱、實際 wire command 與本機聊天室提示已統一為 /warp garden；保留 key.kuanperion.home／hotkey.home.modifiers 儲存 ID、Shift+H 預設及既有自訂／清空設定。先確認新名稱／指令測試在舊碼失敗，再修正並通過 focused 快捷鍵測試、完整 test/build/UI/migration 及獨立離線初始化 smoke。未連線發送伺服器指令，未安裝／發佈開發版。

## NPC Auto Sell 開發切片（仍未發佈）

- Trade 新增實際 Auto Sell／Stash 入口。NPC 透過唯一 typed `/boostercookie` 開啟選單，嚴格比對 `Booster Cookie` 標題與合法 chest shape；未實作的 Bazaar 出售不提供假選項。
- 預設關閉、空 ID 白名單；工具／快捷列／綁定／鎖定／收藏／museum／指定 UUID 優先保護。UUID 比對忽略大小寫與連字號，支援 NBT UUID int[4]；無法辨識的 UUID 表示法一律保護。
- 每次只 shift-click 一個已授權玩家背包 slot；新的伺服器移出回條前不操作下一個物品，不把本機預測或別的 slot 當作成功。回饋只稱「物品移出確認」，不宣稱已驗證實收 coins。
- 有 stack/item/delay/deadline/cooldown/threshold 上限、自動只在無其他 GUI 時開始；手動操作／指令／世界變更取消自動 ownership，不吞使用者輸入、不擅自關閉已讓出的 GUI。預期外選單、逾時或每日 NPC cap 安全停止；成功只釋放同一 Farming session 的 hold。
- AutoSellTaskTest／AutoSellServiceTest：預設拒絕、明確授權、server-only receipt、未知選單、逾時、取消、GUI/user 優先與獨立持久化。另有 typed command／protected action／manual-input Mixin 實際接線測試。
- 完整 test、build、UI model、設定遷移測試通過；獨立 Minecraft 實跑結果另於本切片完成後補記。第一次最終測試與 UUID 紅燈測試寫入重疊而失敗，修正後凍結來源重新完整跑通，不隱藏該失敗。
- 尚未連線 Hypixel 驗證當前 NPC menu/receipt。Bazaar、其餘 Trade 與 Pest／maintenance／Visitor／Supercraft 仍未完成，不能當作完整 2.1.0。

最終獨立 26.2 客戶端 37-state UI_SMOKE_PASS；包含實際 Auto Sell 設定綁定、預設關閉／空授權、持久化、離線指令拒絕、UUID int-array fixtures 與兩個 inventory Mixins 套用。人工檢視 Trade／NPC 詳情無重疊。這不是 live 伺服器出售驗收。

## Bazaar 共用底層（尚無完整購買／出售 GUI driver）

- 公開查價依 Hypixel 官方 API 的 top-30 order-book schema：<https://api.hypixel.net/#tag/SkyBlock/paths/~1v2~1skyblock~1bazaar/get>。精確 product ID／amount／price、雙向訂單深度、保守取整、過期／未來時間、UTF-8／JSON strictness／深度／大小上限皆有測試；深度不足不外推價格。
- HypixelMarketClient 僅按需查固定匿名 HTTPS GET endpoint，不追蹤 redirects、不帶帳號／UUID／座標／API key／Cookie；8 MiB stream 上限、8/10 秒期限、共用進行中的查詢、20 秒快取與失敗 60 秒退避。建構與啟動不抓資料；目前尚未接任何功能呼叫，不會因安裝開發版自動查價。測試使用 fake transport，不實際打 API。
- 精確選單 transition 只能由原 container 進到已宣告的下一份 contract，轉場空隙有期限，未知 GUI／另一個同名 container／stale owner 不接管。
- 購買 ledger 要同時有 server GAME 購買收據與 window0 背包增量回條才能提交實付預算；單靠聊天、畫面預測、錯誤物品／數量／world 都不行。已點擊、取消、逾時或 native outcome 不明時，保留預算 reservation，不能自動當作沒付款而重買。
- native purchase action 使用三態 REJECTED／SENT／UNCERTAIN，與單純 guard 拒絕分開；固定要求背包空位。既有 NPC boolean adapter 維持原有成功／停止語意。
- TDD 覆蓋先紅後綠：缺失 parser/client/transition/ledger、JSON unquoted key、sub-coin 收據、同名 contract 的 ownership 逃逸與 native outcome 誤判。完整舊功能與新契約回歸、build/UI/migration 已通過；最終獨立 UI smoke 另補記。
- 未宣稱 Bazaar 買賣流程已完成。選單 product/quantity/cost 驗證、實際 driver／呼叫者及 Visitor valuation 等仍待串接。官方 Wiki 舊 Bazaar 網址現回到論壇公告，不能拿來臆測當前 GUI；已向使用者索取可選的當前截圖，不需實際付款。

UI smoke 首輪在 Mining capacity phase33 出現非預期轉到 Trade 的路由／null 錯誤，未視為通過；保留 failure screenshot/log。比對先前 NPC 通過版與此版的 92 個 UI class payload 完全相同。加入僅測試用路由追蹤後，乾淨目錄 37-state 通過，未重現異常；不能據此宣稱原因已修復。首輪診斷 Mixin 的 package 選得過廣造成測試啟動失敗，縮到專用 package 後才取得診斷紀錄；這些追蹤碼不進正式 JAR。最終 smoke 加入 fixture 前後及 phase32/33 的明確路由斷言，若重現會顯示實際 group，而非 null exception；不會偷偷重開畫面掩蓋錯誤。

最終獨立 26.2 客戶端 37-state UI_SMOKE_PASS，確認共用底層變更沒有破壞舊 UI／快捷鍵／通知／Trade 設定與 inventory Mixins。實際執行未呼叫公開查價，未登入／連線伺服器。專案重建 payload 與隔離測試 JAR 逐 entry 相等。

## Book Combine 精確 preflight（不是可用的合書流程）

- 新增純 BookCombinePlanner：只接收明確授權的 enchantment ID／最高產出等級、同一附魔與同級的兩本單附魔書、完整非附魔 metadata 相容、非工具／綁定／鎖定／保護 UUID／快捷列。空授權、跨級、重複 slot、多附魔、改名、缺失 metadata 都拒絕；不從 lore 第一行或顯示名稱猜附魔。
- 26.2 AutomationGameView.book 讀取原生 custom-data／ExtraAttributes enchantments，僅接受整數 NBT level；浮點數不得截斷冒充等級。對副本建立非附魔 metadata 投影，UUID 留在 ItemSnapshot 做保護比對，不修改原 ItemStack；未知屬性不任意刪除。
- 預期結果 proof 要求等級恰好 +1 且其餘 metadata 保留，供之後鐵砧確認前檢查。這不是目前就已操作鐵砧的宣稱。
- TDD：缺少 planner／native adapter 先編譯失敗；改名書保護測試先紅後綠。完整 pure runtime（含 BookCombinePlannerTest）通過；獨立 26.2 初始化 smoke 的 BookGameViewTest 與既有 fixtures／Mixin／設定／快捷鍵／樣式檢查通過。來源不新增 UI、開關、指令、購買、經驗消耗或自動合書動作。
- 比對其餘 UI class payload 與已通過完整 37-state 的 Bazaar 檢查點一致；actual project 最終 test/build/UI/migration 與 payload 驗證另補記。
- 尚待實作／驗證鐵砧選單、兩次輸入轉移的 server-only 回條、預覽與 XP/coins 上限、確認與結果回收、取消與輸入物回收、服務／持久化／Trade UI。Book Combine 在完整工作流程完成前不提供無效入口。

Book preflight 專案端完整 test/build/UI/migration 均通過；重建 JAR 與暫存及獨立初始化測試 JAR 逐 entry 相等，92 個 production UI class 與先前 37-state 通過版相同。未包含測試 class；正式安裝中的 2.0.16 SHA-256 未變。這仍非 Book Combine 或完整大升級驗收。

## 2.0.17 hotfix 同步 — 2026-10-24

- 兩條線分開：穩定版 2.0.17 從 2.0.16 建立，僅帶 Garden 快捷鍵與 Farming 啟動工具選取；不混入 Trade 或自動化開發底層。2.1.0 同步相同 helper 與快捷鍵預設，版本仍為 2.1.0。
- /warp garden 新建／重設預設為 Shift+G；保持持久化 ID，既有自訂、已儲存舊預設或清空都不強制改寫。實際 wire command 與聊天室回報維持 /warp garden。
- 啟用工具偵測時，開始辨識作物後先保留正確手持工具，否則只搜尋快捷列 0..8；找到即選用，找不到才原有錯誤／停止。執行中手動換錯工具仍暫停等待換回。
- TDD 先紅後綠：缺少 selector、舊 KEY_H；focused 生產方法測試涵蓋全部 13 種 block ID × 9 格、正確手持、主背包不取用、無匹配不切換、南瓜／西瓜、修飾名稱與 runtime pause 接線。第一次 focused shortcut 執行漏傳 JDK 參數屬測試呼叫錯誤，補參數後通過。
- 暫存版完整 test/build/UI/migration PASS；獨立 26.2 初始化 smoke PASS（含新預設／重設／衝突／自訂保存與既有原生 fixtures）。不冒稱 live 伺服器收成已驗證，完整大升級仍未完成。

## Junk 指定 Garden 位置丟棄流程（開發版，未安裝）

- 預設關閉、空 ID 白名單、無丟棄位置。只在使用者記錄的 Garden 座標半徑內工作，不自行導航／傳送；重啟或換世界後不從舊位置與啟用旗標重播丟棄，需重新記錄位置、在該位置明確啟用，或手動確認。世界授權不保存。
- 只開啟玩家原生 InventoryScreen / inventoryMenu / container0，辨識完整 46 slots（含 offhand45）；只授權主背包 slots9..35，排除快捷列、裝備、副手、工具、綁定／鎖定／收藏及保護 UUID。Native THROW 仍使用既有 protected inventory action gate 與手動輸入優先 Mixin。
- 一次一疊並等待新的 server-only window0 同 slot 移出回條；本機預測／別的 slot 不計成功。不拆堆、不超過堆疊／數量／時間上限；不確定 native outcome、拒絕或逾時停下並持久停用自動 Junk。手動接管後不關使用者選單，不重新開啟 Farming。
- 原有原子外部設定新增獨立 junk 區塊，保留深層未知 anchor 欄位；無效數字／座標、數值字串與限制不默默擴大丟棄授權。Trade 入口／獨立 enable、白名單、保護 UUID、位置、半徑、門檻／間隔／上下限、手動執行／停止／狀態均實際接線。
- TDD：缺少 Policy/Task/Service/native wiring 先紅後綠；新增「舊 enabled 在新世界自動丟棄」濫用案例曾確實失敗，加入不持久化的世界授權後通過。位置／Garden／dimension、保護、46-slot offhand、滿背包、焦點、手動接管、堆疊／物品 cap、server-only 回條、外部寫入衝突、未知 anchor 欄位、失敗停用皆有回歸測試。
- 第一輪 runtime smoke 因測試仍預期 Trade 只有 Auto Sell/Stash 而在 assertGroup 失敗，沒有視為通過；修正明確預期為 Auto Sell/Junk/Stash 並增加實際分組診斷。早期 pure inventory fixture 也已由45修為 pinned26.2 的46，包含副手；沒有把錯誤形狀發佈或安裝。
- 完整 test/build/UI/migration 與最終獨立 runtime 結果，待實際執行完成後補記。沒有登入 Hypixel、沒有在正式遊戲丟棄任何物品；offline 證據不冒稱已驗收 live drop。

Junk 新世界授權補強後，完整 pure/UI/model/migration 均通過。最終 runtime 首次進入 Farming 詳情後又出現預期外回到 Trade 概覽（phase1），與先前 phase33 的未明路由異常類似；保留 screenshots/failure.png 與 log，不重開目標頁掩蓋。正在用僅測試 JAR 的路由／輸入 trace 定位，未修改正式 UI 行為、未將此輪當作通過。

後續合書查核：2026-10-24 讀取 https://wiki.hypixel.net/Anvil 已導到官方 Wiki 結束公告，不能作為當前 GUI 欄位證據。官方 2020 Enchanting 公告 https://hypixel.net/threads/skyblock-enchanting-mega-overhaul.3604521/ 描述書與書合併免經驗，與把書套用到裝備的費用不同；此歷史規則不能取代目前伺服器 preview/cost 驗證。舊上游 BookCombineManager 以名稱包含 anvil、固定 slot22 連點，缺少 preview／cost／server receipt，不原樣採用。

Junk 最終診斷客戶端完整 38-state UI_SMOKE_PASS，含 UI_SMOKE_JUNK_PASS（實際 callbacks／獨立保存／離線拒絕／原生 slot 常數）與既有 fixtures／Mixin。人工檢視 35-trade、38-junk，入口與控制列無重疊。測試附加 trace 僅在獨立 ui-smoke.jar，不在 production JAR；這次沒有重現 phase1／舊 phase33 異常，不能宣稱已修復根因。Trace 中另觀察到兩次非 UiSmoke 呼叫的 KeyboardHandler 事件，但沒有證據能把前次失敗歸咎於它們。完整 pure/build/UI/migration PASS；專案端重建與 payload 比對後另補記。

Junk 專案端 test/build/UI/migration 全數 PASS；重建 production JAR 與已通過 38-state 診斷客戶端的 production JAR 逐 entry 相等，未包含測試／trace class。正式安裝的 2.0.17 SHA-256 未變。這是 Junk 開發切片驗證，不是完整大升級或 live 伺服器驗收。
