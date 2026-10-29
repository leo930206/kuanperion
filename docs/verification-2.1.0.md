# 2.1.0 開發檢查點驗證（未發佈）

日期：2026-10-24。這份紀錄不是完整大升級驗收；未完成項目見 `tasks/todo-2.1.0.md`。正式 main、v2.0.17 與安裝中的 2.0.17 JAR 不隨開發檢查點更新。

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


## Book Combine 有界互動協定（沒有 runtime caller）

- BookAnvilView／BookCombineTask 使用呼叫端明確宣告的選單角色，不內建當前 live layout。測試中的 20/24/13/22 slots 僅為 synthetic fixtures，絕不是伺服器實際欄位的聲稱。原生 `/av` 指令、選單 adapter、service／設定／Trade UI 尚未接入，Book Combine 的 implemented 仍為 false。
- 一次只處理明確授權且 metadata 相符的一對書；第一次輸入需同時有 source 移除與 destination 原書回條，才可進行第二次。第二次之後，預覽與明確 0 XP 費用按鈕也都要有新伺服器回條，不信本機預測或沿用舊零費用。正費用／未知費用不確認。
- 合併需雙輸入消耗回條，結果需伺服器同 metadata +1 等級且背包增量恰好 1；只計入有 fresh slot 回條的增減，不容許用另一疊未變書的回條替本機預測背書。支援已存在結果疊的權威 +1 增量。
- 確定尚未合併時可在仍擁有的選單逐本取回原書，亦需來源與背包目的地證明。native outcome 不明、手動接管、換世界、未知容器或逾時不盲點、不重試／關掉使用者 GUI、不重啟 Farming；可能留書時標記需人工處理。原選單未能關閉亦不得回報成功／恢復 Farming。
- TDD 真正紅燈案例：無效 canonical inventory key、無關未變書回條誤認結果、關閉失敗仍報成功、玩家背包 raw index 與 physical slot 錯配。均先重現再修正；另覆蓋 fresh preview/cost、同名 shadow container、錯世界、重複費用、取消／不確定／超時路徑。
- 暫存版 focused runtime 與完整 test/build/UI/migration PASS。接著執行專案端重建與 payload 比對；不耗書、不連線、不發 `/av`，不更動已安裝 2.0.17。

Book 協定專案端完整 test/build/UI/migration PASS；staging 與專案重建 JAR 逐 entry 相等，沒有測試／trace classes；92 個 UI class 與已通過完整 Junk38-state 的版本完全相同，因此本純協定、無 caller 的切片沒有宣稱額外實跑 live 或重跑完整 GUI。第一次 artifact 比對只在 THIRD_PARTY_NOTICES.md 不同（文件補註晚於 staging build），重新建置後比對通過，未隱藏該初次失敗。正式 2.0.17 checksum 未變。


## Pest 唯讀觀察切片（尚非可用的 Pest 工作流程）

- PestTabObservation 僅在 Pests 區塊解析 Alive／Plots／Cooldown／Bonus；裸 Alive、其他分區、衝突／超長／溢位／不合法數字皆不轉成有效動作。未知不當作 0／READY／active；MAX PESTS 不是虛構秒數。Plot 僅 1～24、無去字元猜編號、衝突未知。舊 Pest／Visitor 通知解析與狀態不變。
- PestIdentity 只比對完整已知的 Minecraft texture URL path，限定 textures.minecraft.net 無 userinfo／port／query／fragment；不是在任意 base64 解碼內容找 hash 子字串。嚴格 UTF-8、JSON 深度／欄位／大小上限及重複 key 拒絕。只讀既有 profile partial property，不呼叫 resolveProfile、不下載／解析網域、不連 Aether。自訂名稱／多個 texture properties 不當作 Pest marker。
- 檢查 Hypixel 官方公開 catalog <https://api.hypixel.net/v2/resources/skyblock/items> 的 category VACUUM 五個精確 ID，最小事實快照在 docs/data/pest-vacuum-catalog.json。TURBOMAX_VACUUM 沒有該 category，不因名字有 vacuum 就選用。API 沒提供吸取範圍，因此範圍仍須工具明確的 Vacuum ability lore；無範圍／未知／重複／改名能力不依 rarity 或工具名字猜。只觀察 hotbar0..8，不切換 slot、不使用工具。此查詢僅開發查核，不新增任何 runtime API caller。
- 目標必須有已知頭像且隱形的 ArmorStand marker；普通 Bat／Silverfish、未知頭像旁的 mob 都不單獨授權。只配對 marker 兩格內最近的 mob，等距不猜；原 marker 作為觀察候選，不推定捕捉。UUID／entity ID 去重，原生掃描上限4096、候選上限256，不在 tick 無界掃描。
- PestGameView 在 client thread、Garden／世界有效時按需讀取，快照含 world 與兩秒 read-time freshness；這不是 server kill receipt，也不能以實體消失或清單空白推定殺蟲。沒有 gameplay runtime caller、移動、飛行、攻擊、鍵盤、熱鍵／通知偏好更動，Pest implemented 仍為 false。
- TDD 缺失 classes／native adapter 先紅；真實重現並修正「Ability: Vacuum Cleaner 前綴誤認」與候選上限未生效。Focused pure suite PASS。初次離線26.2初始化取得 PestGameViewTest PASS，含原生物品 NBT／lore、profile local property、不改原工具、錯誤／自訂／多重頭像與 read-time/world freshness；上限補強後將重建並再驗一次。
- 原生實體／飛行／捕捉尚未連線驗證，不能拿離線 profile fixtures 當作當前 Pest 模型或完整 Pest 功能驗收。完整 test/build/UI/migration、最終 runtime、專案 payload 比對待補記。

後續26.2 native code review 找到 skinPatch 會使 partial profile 不足以斷定頭像；先以真實 Static profile + 非空 Patch fixture 重現誤認紅燈，再拒絕非空 patch。第一次測試嘗試 subclass ResolvableProfile 因它是 sealed 而編譯失敗，改為僅測試使用 pinned private constructor 建立原生 fixture，沒有修改原版 registry。初次編譯未產生 launch-args，接續啟動因缺檔失敗，沒有把它視為執行或通過。

新 `python3 scripts/test-native-observations.py` 在 build 後可用 pinned26.2 vanilla Bootstrap + Holder.direct fixtures 無視窗重跑完整 NBT／Book／Pest 資料投影，無 client／帳號／連線／GPU／遊戲設定／資源下載。此路徑不聲稱 Mixin 套用；原有獨立 client smoke 仍保留兩項原生 Mixin 斷言。skinPatch 案例 headless 真紅後綠，native fixtures、focused pure suite PASS。舊 GUI 初始化仍為前述已通過版本，最終 client/payload 證據另行標示，不混用。

Pest 切片專案端完整 test/build/UI/migration 與新增 headless native fixtures 全數 PASS。專案重建與 frozen staging production JAR 逐 entry 相等，92 個 UI class 與 Junk38-state 通過版相同，沒有測試／trace 類別。最終 skinPatch 補強以無視窗真實26.2 fixtures 驗證，沒有把先前 client PASS 冒稱最後修改後已重新實跑完整 GUI；live Pest 捕捉及 phase7 runtime release matrix 仍待完成。正式2.0.17 checksum 未變。


## NPC Auto Sell 清理／例外與失焦補強

- Review 後以測試實際重現「close 未關閉仍報 COMPLETE／恢復 Farming」、「native command 例外逃出 tick 且 hold 未釋放」、「同標題／container 但 malformed shape 仍被關閉」、「失焦仍完成／關選單」紅燈。
- 完成前重新驗證 world／container／精確 Cookie contract 及 chest+36 slot shape；只有仍擁有的原選單可關閉。未能關閉或 native close 例外均 FAILED，finally 釋放 lease／hold 並安全停止原 Farming；session.finish 失敗不能假報完成。未知／手動接管的 GUI 不關掉。
- native tick 例外封閉成 FAILED，不冒充無風險成功或繼續點擊。FOCUS 短暫缺失時延後動作、完成與恢復；期限仍有界，不無限持有 Farming。未新增指令／出售權限／UI／持久化旗標，不修改穩定2.0.17。
- 新測試亦覆蓋 close 丟例外、同 container 未知 title 及 focus 恢復；focused pure 與 native wiring guard PASS。舊 source shape assertion 因安全條件移到 owns 判斷而確實失敗，改為更嚴格檢查 world／contract／container／close result／finally，不藉此移除保護。完整專案回歸與 artifact 比對待補記。

Auto Sell 清理切片專案端完整 test/build/UI/migration/headless native fixtures PASS；最終 staging 重建與專案 JAR 逐 entry 相等。與前一 Pest 檢查點比對，production payload 唯一變更為 AutoSellTask.class；92 個 UI classes／資源／其他舊功能 bytecode 不變，無測試／trace classes。正式2.0.17 checksum 未變，main／已安裝設定未修改。未登入 Hypixel、未宣稱當前 NPC live 出售已驗收，整個2.1.0仍未完成。

## Pest 已載入目標核心（開發中，未安裝）

- Pest 入口取代 Pest 通知，但舊通知主開關／門檻／通道保留為獨立設定；舊通知啟用不會啟用自動除蟲。新核心預設關閉，門檻／時間／數量／半徑／冷卻保存於外部 automation.json，錯誤限制不默默退回可執行預設。
- 真正串接 service → Farming hold → 原生工具／飛行／尋路／轉向／Vacuum → 回原位／原工具／視角／飛行狀態 → 原 session 才可續耕；只用伺服器已授予的 mayfly 與標準按鍵／useItem，不寫速度、傳送位置、noPhysics 或飛行權限。每個實際步驟重新驗證 lease、世界、Garden、焦點、手動輸入、工具與已載入的完整 swept-body 碰撞空間。
- A* 每 tick 有 collision-query 預算，節點／半徑／五秒搜尋上限，六軸相鄰與精確小數錨點。未知 chunks 不視為空氣、不強制載入；路徑執行每一段再查碰撞。轉向限速，使用按鍵煞停觀察到的慣性，沒有直接改速度。
- 只有實際嘗試使用 Vacuum 後，指定目標／marker 離場且伺服器 Tab 數量下降才記一次「確認數量減少」。單純實體消失、Tab 空白、只有數字下降均不能宣稱完成；尚有未載入目標時不宣稱清空 Garden。
- 手動移動／視角／換工具、指令／背包接管、世界變化／失焦／權限撤銷／逾時，停止本任務並清除自己持有的輸入；不恢復使用者已接管的視角／工具、不重新開啟 Farming。失敗自動停用，避免無限重試。只在原錨點且有安全支撐時恢復原本的非飛行狀態，不在失敗空中把玩家摔下。
- TDD 已重現並修正：Vacuum 容量 lore 更新被誤當換工具、idle lifecycle cancel 使下一次 Farming 永遠不派工，以及單純本機 disappearance 誤判等守門案例；UUID／精確 ID／能力距離仍須相同，不能用另一把同名工具替代。測試涵蓋 native 例外／終止清理、lease/world/focus/manual/hotbar、capturing timeout、還原拒絕不能回報完成。詳細最終命令與 runtime 結果待完成後續寫，未通過項目不先宣稱 PASS。
- 目前這條線只處理已載入且在半徑內的候選，尚缺未載入地塊搜尋、陷阱／補餌／兌換／噴灑／策略／維護工作接續。沒有登入 Hypixel，離線 fixture／Mixin／GUI 不是 live 飛行或吸捕驗收；完整大升級仍待辦。

本輪 staging 完整 `test.sh`（含新核心／service／尋路／轉向與原有回歸）、build、UI model／聊天室樣式、migration、headless native fixtures 全部 PASS。獨立26.2客戶端 `kuanperion-ui-smoke-pest-core` 於22:08:43 `UI_SMOKE_PASS`，含新的 Pest 真實設定 callbacks、獨立通知／自動化旗標、外部保存、離線拒絕，並驗證 receipt、inventory input 及 camera 三個 Mixin 實際套用。此輪為 chatOnly 初始化／callbacks 檢查，不冒稱重跑完整38-state動態 resize／hover。其 production JAR 與最終 staging 逐 entry 相等（327個檔案），無 Test／UiSmoke／Trace 類別；正式2.0.17 checksum 未變。

測試過程保留真實失敗：一次廣泛文字替換把 movement precheck 放到舊 recovery method，導致原測試編譯失敗，已縮回唯一 START movement hook，原 recovery 行為恢復通過；慣性 fixture 最初錯把水平阻尼同用於垂直，查 pinned26.2 `Player.travel` 的飛行 Y 阻尼為0.6後修正 fixture，12種水平參數的定性收斂檢查通過，不把這個簡化數學模型當作實際飛行測試。專案同步／重建證據待後續補記。

Pest 核心同步後，專案端完整 test/build/UI/migration/headless native 全數 PASS；專案重建與已通過獨立客戶端的 frozen production JAR 327 entries 相等。`06d7179` 已推送 feature/2.1.0，正式2.0.17與 main/tag 未變。這是中途備份，不作為停止點；繼續下一個實際搜尋切片。

## Pest 有界範圍搜尋（獨立選項）

- 新增明確 opt-in 的四點搜尋，缺省／舊檔均為 false；通知或既有 Pest 主開關不默默授權更廣的搜尋。設定布林採嚴格驗證，型別錯誤不當成 true。仍只在 Garden 既有飛行權限內使用同一原生控制器，不加傳送或位置／速度寫入。
- 同一三維 FlightBounds 同時守住 A* 提案與實際每段完整 body-collision recheck；目標點在半徑內，未知 chunks 不當空氣。每點抵達後只短暫等待載入資訊，找到目標立即釋放舊搜尋輸入並交回吸捕流程；人工接管仍優先。
- 四點耗盡時先返回、還原工具／視角及安全飛行狀態，再以 INCOMPLETE 停止 Farming／停用自動 Pest、等待操作，不宣稱清空。Tab 0 與仍存活目標矛盾時有界等待／失敗，不能偷放行 Farming。
- TDD 缺失 policy／state／FlightBounds 先紅；實際重現「發現目標後仍留一 tick 舊搜尋鍵」與「搜尋中 Tab 0／存活目標矛盾仍進返回成功」紅燈後修正。四點／返回次數、發現中斷、人工取消、設定獨立保存／舊檔預設、三維 geofence 均有測試。
- Staging 完整 test/build/UI/migration/headless native PASS；獨立26.2 `kuanperion-ui-smoke-pest-sweep` 22:33:24 UI_SMOKE_PASS，實際測試新增搜尋開關保存與原有通知／自動化獨立、原生 Mixin 與其他初始化 callbacks。仍為 chatOnly 模式，沒有 live 捕捉、沒有跨未載入地塊的傳送；完整38-state／live驗收仍在 release gate，未把此切片當作整個七階段完成。

Pest 四點搜尋專案端完整 test/build/UI/migration/native PASS，production payload 與獨立客戶端／frozen JAR 相等（328 entries），`3cac22e` 已推送開發分支。正式2.0.17保持原 checksum，未安裝2.1.0。

## 新世界位置佐證／伺服器絕對位置回條

日期：2026-10-25。接續跨地塊導航前，先補齊資料證據與位置守門，尚未新增任何傳送 driver 或 UI 開關。

- 原 observe 方法確實可因切世界後殘留的 API GARDEN，在原生 Lobby／Hub 仍授權 SkyBlock／Garden。以實際 production-method harness 重現紅燈；改成必須有目前原生 SKYBLOCK header，Garden 另需精確地區／有效 Plot 行，且原生其他地區與已知 API 矛盾會否決。只提到 Garden 的任務文字不算，未知資料不退回舊 API。這只收緊新自動化 context，不修改原 Farming／Mining 位置恢復流程。
- 原生資料有大小／行數限制，格式處理使用預編譯 regex，避免每個權限檢查重建 patterns。官方伺服器網域限制保留。
- ClientPacketListener.handleMovePlayer 的 TAIL 只投影伺服器封包中絕對 XYZ；relative XYZ 不依本機預測合成，relative rotation／delta flag 不影響絕對座標的判定。只存一筆最新回條與32個 opaque ID，world／epoch／sequence／收到時間守門，不假設 ID 正數或遞增，重播／未知資料會清除可用證據。
- 不讀本機 player.position() 製造回條，不送指令／傳送／速度／能力封包，不持久化位置、不加遙測。資料回條不是執行權限，未有 runtime travel caller，後續仍須 lease／目的地／現在玩家位置／時效一同驗證。
- 純類別／native projection／wiring 缺失先紅；pinned26.2 真實封包 fixtures 驗證 absolute／relative 各旗標、NaN／缺資料與不可變性。Staging 完整 test/build/UI/migration/native PASS；獨立26.2 position-proof 客戶端 01:04:53 UI_SMOKE_PASS，新增位置 Mixin handler 實際套用斷言通過，同跑舊 Pest／Trade／設定初始化 callbacks。非 live 伺服器移動驗收，亦非完整38-state resize/hover rerun。

## 2026-10-25 — opt-in Pest plot travel / confirmed return

- Added curated 25-cell Garden geometry as strict typed destinations (24 plots + barn). Geometry alone never grants authority. `travelPlots` defaults to 0, including existing enabled Pest configurations; strict persisted integer 0..24 and all other policy edits retain the explicit limit.
- Real Pest service/controller/command boundary now share the server-position receipt cache. Only current strict Tab plot IDs may initiate outward travel; one command per visited plot, at most the configured number, no retries. Owned flight keys are released before dispatch. Outgoing local command-event observation proves dispatch only, not arrival.
- Movement/re-anchoring waits for a NEW absolute XYZ server packet, expected plot, same world/session/Garden, current position within 0.5 block and a 15-second deadline. Relative/corrected/wrong-plot/stale/local-only positions do not unlock movement. Manual slot/input and world/focus/lease changes cancel before re-preparation.
- The original Farming anchor is retained separately from each regional flight fence. Return uses its typed original plot (barn only for original plot 0), then bounded <=65%-radius flight legs to the exact fractional original position, view/tool/support restoration before Farming release. No `/sethome`, coordinate writes, velocity changes or server movement-authority writes were introduced.
- TDD missing-API RED followed by pure travel/task/settings and actual-production command-method harness GREEN. Existing local-sweep regression exposed an unnecessary extra return leg; local-only return remains direct, while distant post-teleport return is segmented. Far-corner, incomplete, duplicate destination, manual hotbar, failed command and missing receipt fixtures pass.
- Staging test/build/UI/migration/native commands pass. Isolated pinned Minecraft26.2 chat-only fixture `kuanperion-ui-smoke-pest-travel` passed at 01:26:01, exercising the real new slider, independent flags and external persistence without offline actions; production JAR frozen separately. This is not a live Hypixel teleport/capture validation or a claim the full upgrade is complete.

## 2026-10-25 — loaded full-trap release/capture vertical slice

- Independent `clearTraps` opt-in (default false even for previously enabled Pest); existing notifications remain untouched. A known loaded full trap can trigger a Pest run with zero flying pests. Original root lease/Farming hold spans flight approach, trap interaction/menu, released-pest capture and exact return.
- Bounded native Tab `Full Traps`/`No Bait` parsing distinguishes unknown, conflicting and explicit `None`; exact native `Pest Trap`/`Mouse Trap`/`Vermin Trap` markers use entity ID + UUID and reject duplicate trap IDs. Families match a recorded audited official item-catalog subset. These formats are supported contracts, not a claim current live layouts were validated.
- Standard legal-reach native entity interaction only after bounded rotation, swept-body/loaded/line-of-sight checks. No guessed slot numbers: exact owned title/chest shape/world/container and ONE external green `Release All Pests` role, no priced role and no player-inventory lookalike. Full-trap status is revalidated immediately before click.
- Confirmation requires the clicked trap no longer full plus a server Tab alive increase OR a newly observed exact Pest UUID absent just before the click; pre-existing targets and disappearing text alone do not count. The source's pre-release entity-snapshot concept avoids falsely requiring counters to rise when a server already includes trapped pests.
- Server-menu closure is expected after release; the root retains its original lease and only the declared trap menu can bypass MENU cancellation. Manual cancellation never closes the now-user-owned menu or moves carried items. A client-predicted button cursor is observed while waiting, never clicked away: progress/owned close additionally requires an empty cursor, with an eight-second response deadline.
- TDD catches: missing protocol/root APIs; manual takeover incorrectly closing the menu (fixed); stale full status before click (fixed); predicted cursor incorrectly completing/closing (fixed). Pure old/new tests, pinned26.2 native marker fixtures and build passed. No bait purchase/insertion, unloaded-trap travel or live release is claimed by this slice.
- First isolated UI run failed because a test-only row-index edit also changed the unrelated Auto Sell smoke method; production Auto Sell was unchanged. Restored that method exactly; failed fixture/log retained at `kuanperion-ui-smoke-pest-trap-failed-first`. Corrected fixture validation is recorded below once complete.

- Corrected isolated Minecraft26.2 chat-only UI fixture passed at 09:32:10, including real trap-toggle callback/external persistence, Stash/NPC/Junk regressions, all real packet/input/camera Mixin probes and old chat/key/Mining preferences. No Hypixel login or trap interaction was attempted. Final frozen production payload was built after the cursor/status fixes.

## 2026-10-25 — server-applied cursor receipts for upcoming item transfers

- Added a bounded latest-only cursor ledger: exact world/container scope, invalidation epoch, new sequence after a request, finite age, null/invalid/zero-count non-air rejection. It is evidence, never permission to move/repair an item or bypass protected-item policy.
- Actual26.2 `handleSetCursorItem` and container-content TAIL hooks feed typed wrappers only on the client thread with a player/world and active owned gameplay session. Container-content receipts additionally require the current container ID. Raw packet contents must exactly match the server-applied current carried item; a predicted/mismatched cursor cannot create a receipt. World changes clear it.
- Pure missing-class RED/GREEN and real packet/native projection fixtures pass. Headless fixture initially used an unbound vanilla Item holder (test-only NPE); corrected to the repository's explicit Holder.direct fixture; production code unchanged by that fixture correction. Real client smoke also verifies the new cursor Mixin handler is applied.
- Full staging tests/UI/migration/build, native fixture rerun and isolated Minecraft26.2 chat-only fixture passed (client 10:03:48). No cursor mutation, purchases, bait insertion, new toggle, stored preference, network query or current-server transfer verification is claimed in this infrastructure slice. This enables the next guarded bait/input workflow instead of trusting client prediction.

## 2026-10-25 — Greenhouse read-only discovery and harvest receipt groundwork

- Added the three supplied mature-crop texture identities (Ashwreath/Turtellini/Glasscorn), disjoint from Pest. Extracted the existing strict offline texture decoder into one shared helper without widening the Pest whitelist; all old identity tests pass. No profile resolution or texture request is performed.
- Added explicit entity-ID/UUID or block-position targets and bounded discovery for configured plot/type/range: 64 candidate reads per tick, <=8192 candidates/1024 results, ten-second deadline, duplicate/unknown/unloaded/world change rejection. Native source snapshots bounded loaded chunk block-entity references instead of scanning 51*21*51 world positions; all getChunk calls are no-load. Discovery is not attack authority and is revalidation input only.
- One watched harvest target records only fresh same-world, exact-target server removal, HEAD-equipment, block, skull-data or section-update TAIL events. Section projection stops at the target or 4096-entry bound. Raw ignored updates cannot borrow local prediction: equipment/raw block state must match applied native state, and skull-data requires an actually present skull block entity/state. A later task must additionally prove its mature target changed after an issued attack; neither a packet signal nor predicted air alone means harvest success.
- Native26.2 API compile checks caught moved BlockEntityTypes.SKULL and ChunkPos record accessors; corrected against the pinned JAR. Missing-API RED/GREEN, all prior pure tests, bounded scanner/receipt tests, real head/profile and removal/equipment/block packet fixtures pass. Native section visitor budget is tested with real BlockPos/BlockState values; this is not a claim of live server harvest testing.
- Isolated Minecraft26.2 chat-only fixture passed 10:45:28 with all five new harvest Mixin handlers actually present, old packet/input/camera probes and existing UI/settings/key/Mining regressions. Frozen production JAR contains no fixture classes. No Greenhouse UI/driver, attack, movement, configuration change or automatic task caller is enabled by this observation slice; executable harvest is the next increment.

## 2026-10-25 — shared legal-flight primitive, Pest facade preserved

- Extracted the existing key/path/collision/pose restoration primitive into GardenFlightCore with a fixed constructor owner restricted to Pest or Greenhouse. Resource leases still decide each operation; sharing flight does not grant Greenhouse Pest item-use or screen capabilities. No new executable Greenhouse task or attack permission is introduced in this refactor.
- Pest retains its exact Vacuum/range/identity, capture cadence, trap/menu proof and feedback behavior; the facade delegates only movement/release/restore and converts every anchor field and motion explicitly. Stale-handle cleanup does not clear a different current owner's capture cache.
- Compared eight movement/key/collision method bodies before/after normalization: move, clear, loaded, physical, manualInput, keys, set and release are unchanged. Guard tests execute the actual new method with explicit game boundaries (feature/world/session/thread/flight/Garden/focus/menu/physical/resource cases). Real native fixtures check facade anchor/motion mappings, side-effect-free construction and rejection of other feature owners.
- All existing pure/runtime/UI/migration/native fixtures passed on staging; client fixture results are recorded after completion. An initial isolated draft compile used an unordered classpath containing older Minecraft versions; corrected to the pinned26.2 JAR first (no production API/code workaround). Actual repository build already pins that JAR first. This is not live flight/physics validation.

- Isolated pinned26.2 chat-only fixture passed 11:04:18 with the actual Pest facade/construction fixtures, all server/input/camera probes and existing real UI callbacks/preferences/shortcuts; no server or flight action was executed. Production payload frozen separately.

## 2026-10-25 — Greenhouse executable harvest and return

Greenhouse is now wired to the Farming page with explicit plots/crops/exact tool and independent external preferences, default off. Pure task tests require both a fresh targeted server-applied harvest event and a changed mature identity after one actual attempt. Typed plot travel/return requires new absolute server position + expected plot + matching current pose; original tool/view/flight/position and the same revocable Farming token are restored only on verified return. Cap/time-limited maintenance reports PARTIAL honestly rather than claiming every plot cleared. See greenhouse-driver-2.1.0.md.

Full actual-project test/build/UI/migration/native gates passed before e81b6eed7fa0239f1ac989a07e6ecae6db6cb9ba was pushed. Production payload390 entries matched the frozen isolated-client-tested JAR. The pinned26.2 chat-only fixture passed 11:47:53 including UI_SMOKE_GREENHOUSE_PASS, old Pest/native/Mixin/preference/shortcut callbacks and overall UI_SMOKE_PASS. This is not a live Hypixel travel/physics/harvest result; no stable installation/main/tag change.

## 2026-10-25 — Composter inventory-source slice verification (in progress)

Explicit configured Barn machine/material/units, bounded protected full stacks, source/resource/cursor server proof, exact return and independent Farming module are implemented in the development staging source. Sacks/purchases/current live formats remain pending; see composter-driver-2.1.0.md.

TDD caught missing anchor coordinates defaulting to zero, a fractional percentage rounding to a falsely complete empty target, and delayed initial menu slots incorrectly causing immediate failure. These have explicit regression cases. An actual isolated UI callback test also caught Composter writing Greenhouse's feature bit; fixed to COMPOSTER with persistent enable/cancel isolation tests. The failed run/payload are retained separately. Final corrected isolated-client and actual-project gates must pass before this slice is treated as backed up; no live or release claim.

The corrected pinned26.2 isolated chat-only run passed 15:01:49 with UI_SMOKE_COMPOSTER_PASS, UI_SMOKE_GREENHOUSE_PASS, UI_SMOKE_PEST_PASS, real applied Mixin/native fixtures and overall UI_SMOKE_PASS. Full staging test/build/UI/migration/native gates passed with the new enable/cancel feature-isolation regression. The production payload is frozen separately for actual-project equality checks. Neither this offline result nor any fixture conversion value is a current server menu/material catalog claim.

## 2026-10-25 — Composter checkpoint backed up; explicit trap destination staged

Composter inventory-source checkpoint 0d22a23cfd44c9f51e9cae9b7d67d4fb7b120a09 passed all five actual-project gates; production412-entry content matched the corrected isolated-client frozen JAR. No stable/main/tag/settings change. Sacks, purchases and live validation remain pending.

The following staged Pest increment adds an independently configured trap plot, never an inferred trap-ID mapping. See pest-trap-travel-2.1.0.md. Tests cover full original plot24 -> Barn0 trap7 -> release/capture -> confirmed original segmented return, legacy defaults without new authority, and malformed independent preferences. Isolated current-client callbacks and actual-project gates are still required before that increment is backed up.


## 2026-10-25 — Jerry automatic /sethome preserves the maintenance hold

The existing Jerry automatic record-only command now has a typed, client-thread-only bridge sender. It sends only literal sethome through the normal connection and observes Fabric dispatch without classifying that single trusted command as manual takeover. Nested command-origin flags are restored even on refusal or exception; manual shortcuts and later manual commands retain priority. No lease, preference or additional command authority is granted.

Actual legacy Jerry handler + sender + command observer bodies are composed with real AutomationSession/FarmingHandoff in tests for Pest, Greenhouse and Composter, both Jerry stop states and both chat-notification states. The prior manual-origin route is mutation-tested and rejected. Existing messages/beam ordering remain covered. This does not implement the still-pending global Jerry stop latch for standalone/idle jobs.

Staging full test/build/UI/migration/native gates passed. Pinned26.2 isolated offline client passed 16:35:44, including UI_SMOKE_JERRY_HOME_GUARD_PASS, Pest/Greenhouse/Composter callbacks, applied Mixin probes, legacy settings and UI_SMOKE_PASS. It exercised no server command, gameplay or real account. The no-player native bridge refuses without changing preferences or acquiring a lease. Production payload is frozen for actual-project verification; no release or installation is claimed.


## 2026-10-25 — Visitor exact offer/value/submission boundaries

Added bounded selected-title/unique external-role offer parsing, explicit material-name mapping (never scanner-button ID), typed integer amounts/Copper, conservative full material/deficit estimates and an inventory-only one-attempt submission protocol. Unknown prices, ambiguous roles/materials, protected same-ID stacks anywhere, prediction, stale receipts or disappearance cannot authorize or confirm delivery. Protocol success needs exact server GAME acceptance, every fresh required applied item loss and physical/server-empty cursor. See visitor-offers-2.1.0.md.

Missing-API RED -> pure GREEN covers original ID fallback hazard, quantity/section ambiguity, independent limits, protected inventory, exact acceptance, single-attempt/no-retry, manual/world/deadline and dual receipt cases. Native API inspection showed43 inventory slots in26.2 (body41/saddle42 in addition to offhand40); protocol/native projection explicitly cover all43, with36 main slots for price deficits. An initial fixture referenced unavailable terracotta item constants; fixed fixture-only materials to existing native Wheat/Coal, preserving semantic role/ID assertions.

Staging five gates passed. Isolated pinned26.2 offline client passed18:42:09 including AutomationVisitorViewTest and all existing UI callbacks/preferences/Mixin proofs. No live server, offer interaction, purchase or submission was attempted. No executable Visitor toggle, acquisition, navigation, scheduler or return is claimed; these remain unchecked release gates. Production JAR is frozen for actual-project payload verification, not release/installation.


## 2026-10-25 — Jerry stops standalone and idle automatic jobs

All six currently executable new services now support explicit safety suspension even while idle, distinct from ordinary lifecycle cancellation. Jerry stop-on invokes it independently of legacy Farming/Mining flags, after the unchanged legacy stop/beam preparation. Standalone/manual actors and separately leased Stash stop; later timers/thresholds/signals cannot auto-restart. Idle suspension preserves the latest external preferences byte-for-byte; explicit per-feature enable acknowledges it. No automatic Farming rearm.

Actual service regressions cover five threshold/interval/disposal services plus active/idle Stash without Farming. Composed actual legacy+bridge bodies cover six standalone actors, idle timers, three held-Farm owners and independent stop/chat states. Negative mutations reject both old manual-origin sethome and legacy-Farm-only stopping. A source guard requires future executable features to extend this matrix. Existing cancellation cleanup is reused, without new cursor repair or user-GUI close.

Staging five gates passed. One UI-model invocation raced build/classes deletion because build and UI compilation were launched concurrently; its failed transcript is retained as jerry-global-ui-build-race.log. Corrected sequencing after build passed without an application-code workaround. Isolated pinned26.2 offline client passed with UI_SMOKE_JERRY_GLOBAL_STOP_PASS and all old native/UI proofs; no server gameplay or real account was used. See jerry-global-stop-2.1.0.md.


## 2026-10-25 — anonymous on-demand official item catalog

Added a strict bounded offline official-resource projection and a fixed anonymous demand-only client. Only unambiguous exact display names map to validated IDs; duplicates, malformed/oversized/deep JSON, invalid UTF8/schema/timestamps deny data. No scanner-button ID, fuzzy name service, account/backend or startup fetch fallback. Shared existing redirect-denying HTTP factory/stream bound,8s HTTP/10s future limits, coalescing, receipt-time24h cache,60s failure backoff, clock-reversal invalidation and real close cancellation are covered by fake transport tests. No network request was performed by these tests.

A previously cached official5680-item/4.8MB source was parsed locally into5364 unambiguous names (SHA recorded in item-catalog-2.1.0.md); it is not bundled. No native or legacy consumer constructs/fetches the dependency yet. Future callers still require world/session/allowlist, independent GUI amount/cost proof and lifecycle shutdown.

Staging five gates passed, including all original Jerry/global-stop and Visitor contracts. Isolated pinned26.2 offline client passed18:59:14 with existing real UI callbacks/native projections/Mixin proofs and no server gameplay. Frozen production payload is retained for actual-project equality verification, not release/installation.


## 2026-10-25 — bounded Visitor queue and native dual-identity discovery

Added complete unique bounded Tab Visitors sections and metadata; unknown/missing/extra/conflicting counts/names are UNKNOWN rather than empty. Local candidates require an exact unique queued invisible hologram plus a sole nearby living non-listed character, retaining both entity IDs/UUIDs/positions; no substring, first/nearest, player or duplicate-character association. Native projection visits <=4096 loaded entities, records <=256 within64 blocks, never forces chunks or interacts. Read-only data is not future action authority.

Missing-API RED -> pure GREEN covers malformed queues, ambiguity, duplicates/players/positions and immutable outputs. Staging full test/build/UI/migration/native gates passed; isolated pinned26.2 offline client passed19:09:12 including native no-world queue/target refusal and all old UI/Mixin/settings proofs. No server Tab/entity format, navigation, interaction, buying, submission or return was tested/enabled. Visitor runtime/service/UI and the full upgrade remain unfinished.


## 2026-10-25 — delayed server-owned trap close preserves the original hold

A true behavioral RED reproduced a release menu remaining visible for an extra poll, then an applied server close cancelling the original root before task processing. Added a narrow opaque one-use pending-release ticket: exact owned-menu/ID/current-root/world/deadline snapshot at native HEAD, matching applied vanilla inventory-menu/no-screen at TAIL. Only that scoped event clears the old expected-menu claim; local/manual/ignored/stale/foreign close cannot borrow it. Independent release facts and empty cursor still gate success. See pest-trap-close-2.1.0.md.

Actual-session regressions cover the delayed poll ordering, one-use acknowledgement, manual/local close, wrong/ignored IDs, rejected-TAIL replay and revoked roots. Existing capture/trap/travel/return, Jerry and legacy checks pass. Native source guard requires client-thread checks before cache mutation and no native command, cursor or screen mutation.

Staging five gates passed. Isolated pinned26.2 offline client passed19:23:12 including both newly applied handleContainerClose HEAD/TAIL probes, the actual getContainerId packet API/no-player bridge refusal and all prior UI/native/settings/Mixin checks. No live server or player account was used, and this is not a claim of live trap release validation. Frozen production payload is retained for actual-project equality verification.


## 2026-10-28 — Book current UI and acquisition-boundary checkpoint

- Source on feature/2.1.0 after8e8de59 includes current book+book split zero-Exp parsing/direct-cursor result, manual-priority dispatcher, Supercraft world/Farm intent and shared acquisition planning/delivery boundaries. Stable2.0.17/main/tag/settings remain untouched.
- Full reviewed regression suite, pinned26.2 build/source+archive audits, UI model/chat style, migration and seven actual Java25 market/compatibility models passed for the acquisition boundary. All10source hashes and542JAR entry hashes matched rebuilt actual project output. This is not a native buying GUI or parent acquisition caller.
- Isolated offline Minecraft26.2 current smoke passed39states/screenshots with a dummy identity and independent game/config directory, no server join. Added Book Trade route/default-off/explicit ceilings/protected UUID/external preferences callbacks, refused invalid-world manual request/lazy native task, and Jerry runtime-only suspension assertions. Supercraft offline manual request now correctly expects invalid-world refusal, not a delayed request that could replay in a future world.
- The first runtime RED was a stale test route expecting Trade without the new Book Combine card; that expectation was updated, not the production sidebar. Another run failed at phase16 with `gear did not open interface settings`; the earlier phase0 gear check passed. Evidence was retained. A subsequent diagnostic run added immediate sidebar click assertions and coordinates/state logs and passed all39states unchanged production code, including the same phase16 gear click (before0/after4 and owned coordinates). The intermittent failure is **not explained or claimed fixed**; retain diagnostics and keep final release/UI acceptance gates open rather than hide a rerun.
- Manual image review then found the long Book group introduction does not wrap inside its46px header. [Screenshot before correction](evidence/book-ui-description-overflow-2026-10-28.png). The runtime callbacks passed; that does not imply every pixel/layout is correct. A shared measured group-header height/render/interaction/scroll regression correction is next, without rewriting descriptions.
- Smoke helpers/tests are outside the production JAR. The GUI-tested production JAR is entry-identical to the acquisition-boundary artifact; these test/documentation changes add no runtime dependency or game behavior. This is not actual Anvil, buy, George or maintenance-chain server acceptance.


## 2026-10-28 — measured module introduction layout

- Fixed the Book screenshot's horizontal description overflow without changing any description text. Module group headers now measure the existing localized help at the available width, reserve the existing checkbox strip when present, wrap/scissor the text and grow beyond46px only when needed. Title/font scale/configured gap, culling, total scroll height, actual module row click offset and module search anchoring use the same geometry. Existing one-line defaults and legacy flat-page labels stay unchanged.
- TDD: new GroupHeaderLayoutTest failed against the old compiled API before the implementation. Headless geometry checks cover narrow/wide text, default46, font1/1.4 and configurable gaps; a source wiring guard protects shared painting/click/search geometry. Native glyph correctness is not inferred from the headless estimate.
- Pinned26.2 build/source+archive audit and focused UiModelTest/GroupHeaderLayoutTest/ChatStyleTest/migration pass. The new real phase38 check measured every Book help line and the final line's complete vertical extent, then clicked the actual checkbox twice below the grown introduction. Full39-state isolated runtime reports UI_SMOKE_GROUP_HEADER_PASS and UI_SMOKE_PASS; [reviewed corrected image](evidence/book-ui-wrapped-description-2026-10-28.png) shows all help lines inside the card. No account/server/transaction was used. Final full regression/actual project payload equality is recorded at the sync checkpoint, not inferred from this UI run.
- The earlier intermittent Setting click failure was not reproduced in the diagnostic nor wrapping run; production navigation was not modified and no root-cause fix is claimed. All final native-menu/live acceptance/release gates remain open.

Final sync checkpoint: 完整 test.sh（含 shared-header wiring）、實際專案重建／來源與封裝稽核通過。15個同步檔案與542個JAR entries 全等；實際專案產物亦與通過39狀態的獨立客戶端JAR逐entry全等。四個nested helper只有debug行號改動，javap指令全等；其他gameplay／交易／網路classes未改。正式2.0.17 checksum未變，未安裝2.1.0。


## 2026-10-28 — Pest read-only crop/event strategy checkpoint

- Added pure Feast/Jacob/Jacob-then-Feast explicit target selection with default Off, independent exact priorities/profiles, bounded fresh same-world facts and fallback only after positive inactive evidence. Unknown or unlisted active crops never become first-crop guesses. No current profile mapping, event parser/feed, runtime caller, native apply task or unfinished toggle was added; complete strategy scope stays unchecked.
- Genuine missing-API RED precedes implementation. Focused pure models, complete test.sh including a no-runtime-caller/no-network boundary, pinned26.2 build/source+archive audits, UI/GroupHeader/Chat models, migration and eight actual Java25 Pest/compatibility models passed. All542previous JAR entries are byte-identical; only11new leaf-policy classes were added. The earlier39-state UI result applies to unchanged UI code; no new GUI run or live strategy application is claimed for this pure-policy addition.
- Official Public API reference was checked read-only for a public event source; no documented global Feast/Jacob feed was established. No undocumented URL, historical third-party feed, account/profile lookup or new runtime endpoint was introduced. Details/remaining data and native-effect gates are in pest-strategy-2.1.0.md and live-evidence-needed-2.1.0.md.

## 2026-10-28 — workflow observation, due Pest and fault-tolerant Jerry stop

- Current overall delivery estimate is conservatively about65%, not checkbox/test count. Actual native Bazaar acquisition/sale, George Pet Sell, Pest bait/exchange/spray/full strategy, broader source/quantity handling and server acceptance remain substantive unfinished work. No2.1.0 publication/installation or stable2.0.17/settings change.
- Genuine RED: Book/Supercraft service unknown-screen and null/throwing Book factory cases escaped without safe service termination; Supercraft begin/catalog/tick/cancel swallowed original session-stop errors. New actual service/task/session fault models preceded narrow read handling and stop-error propagation. Healthy automatic recovery, known-owned menus, original hold revocation, preference preservation, once-only cleanup and no replay/recovery/close are checked. Source-only Supercraft intent wiring initially failed its old direct-boolean spelling assertion; updated guard retains intent binding and now also requires the unknown/pending/active fail-closed routes. This was a stale source assertion, not an excuse to drop the behavioral tests.
- Genuine RED: Pest service inspected a foreign active root's temporary context/tool/targets and consumed the poll throttle; explicit manual requests also did preflight. Actual Pest service/task/session fixtures now prove zero busy native/UI reads, no manual acceptance, same-timestamp fresh dispatch after successful release, current UI/Farm/threshold refusal, unchanged saved consent and only original Farming generation. Borrowed Supercraft root is not a claim of an actual native multi-job chain.
- Genuine behavioral RED: the previous actual Bridge Jerry-stop body failed17 of19 dispatcher scenarios because a throwing early service skipped later services. Controlled actors with the real AutomationSession/Farming hold now pass all19, including each callback position, all-nine errors and each of eight gameplay owners' real session-stop failures. First exception identity/later suppressed failures survive; helper tests cover repeated first-error identity, once-only order, empty sequence and uncaught arbitrary Error. An initial generated-fixture forward-reference compile error was corrected before the meaningful behavioral RED; it is not counted as production failure evidence. Other stop entry points and each native service's cleanup remain open gates.
- Final integrated staged `test.sh`, pinned26.2 Java25 build with source/archive audits, UiModelTest/GroupHeaderLayoutTest/ChatStyleTest, legacy migration and native-observation fixtures all passed. No new GUI client was started for these service/dispatcher-only changes, and the older intermittent gear-click failure remains unresolved rather than being called fixed. No current native Bazaar/George menu or live transaction was inferred from synthetic actors/models. See workflow-observation-2.1.0.md for scope and remaining boundaries.

Actual-project sync checkpoint:18 changed/new paths and all526 source/doc/test hashes match the tested stage. Actual pinned26.2 rebuild/source+archive audits pass; all554 ZIP entries match the frozen staged artifact. Relative to the prior553-entry build, only five existing implementation classes change and one AutomationStops class is added; two synthetic switch helpers differ only in debug metadata, with full `javap -c -p` output identical. All other546 entries are byte-identical, including legacy gameplay/settings/UI/native networking classes except the explicitly changed Bridge dispatcher. Thirteen focused model entry points compile against and run the actual Java25 production classes; actual-project UI/GroupHeader/Chat models and migration also pass. main/origin-main/v2.0.17 remain a126ea5, and installed2.0.17 SHA256 remains bebb7c16ed1fcefc95ebdb03257f9268fde5a7d4f1d44212446353921a963a94. No2.1.0 install/tag/publication or live result is claimed.

The first actual-project UI-model invocation passed its assertions but its file appender could not write the sandbox-restricted project logs/latest.log. An approved unsandboxed rerun of the unchanged test command then passed all three entry points without that logging error. The first log is retained; this permission diagnostic is not represented as a production UI regression or silently ignored.

## 2026-10-29 — lifecycle dispatch and original Stash request

- Seven actual Bridge stop bodies now attempt all explicitly listed callbacks after RuntimeExceptions, retaining first/later failure identity. Shutdown covers all implemented services, both close tickets and four ledgers; client stopping attempts NanoVG destruction even when automation shutdown fails. Original takeover revocation precedes Trade cleanup. Genuine RED131/151 preceded the change; final209 real-method/real-session actor scenarios and four renderer-actor cases pass. Actors alone do not prove native GL or every service's cleanup.
- Actual Stash service/task/coordinator now refuse unavailable idle reads, revoke the active original command lease and runtime-suspend on unknown observations without changing saved consent, and bind observations/sender returns to the original request. Fresh requests reset first-send timing; nested typed sends restore the prior trusted scope. Eleven named actual-model RED invocations plus a separate nested-sender RED preceded implementation. Full new models, existing runtime/safety/suspension models and foreign-root preservation cases pass. No new command, menu format, endpoint or settings schema is added.
- The first two staged full runs stopped at obsolete adjacent-token/first-line source guards after the lifecycle bodies became multiline. Guards were corrected to inspect balanced actual methods, retain native restrictions and add shutdown coverage; behavioral tests were not removed. The final combined test.sh, pinned26.2 Java25 build/source+archive audits, UI/GroupHeader/Chat models, migration and native-observation fixtures all pass. Earlier failed logs remain retained.
- The smoke launcher previously accepted a UI marker despite a nonzero child exit or absent shutdown evidence. Five harmless child-process fixtures produce genuine old-launcher RED and corrected GREEN; the verifier now requires successful UI, explicit shutdown proof, no failure marker and exit0. A new isolated-only dependent stop observer checks actual renderer teardown and idle service/root state after the production stop callback. Real GUI results and actual-project synchronization will be recorded below only after completion.
- Overall delivery estimate remains about65%; substantive acquisition, George, Pest follow-ups and live acceptance gates remain open. Installed2.0.17 and actual game settings are not part of these development tests.

Full-GUI result (not passed): the new verifier rejected exit0 because the full run stopped at phase33 with `Mining route changed at phase33: main=0 filter=3 module=none`, after phase32's Mining capacity check passed and the next fixture's immediate route checks did not fail. The retained failure image shows Trade selected, not Mining. No native-input call-site trace was captured, so external OS input, fixture timing and production navigation remain hypotheses, not diagnosed causes. The phase16 gear check passed in this run; the older intermittent failure is still not explained. Do not call this a39-state pass or rerun away the failure. The tested production UI classes are unchanged from the preceding checkpoint; this does not by itself clear the runtime gate. A separate phase0-only invocation targets the newly changed normal client-shutdown path; it cannot substitute for full navigation acceptance.

Focused shutdown result: the separate phase0-only dummy-identity26.2 client completed its real registered UI/settings/service checks, printed UI_SMOKE_PASS, then after production client-stopping printed UI_SMOKE_SHUTDOWN_PASS and exited0. The native NanoVG renderer had been observed initialized before stopping; afterward its renderer/context/drawing state was destroyed, with idle services/session/hold. This proves the normal initialized-renderer shutdown path, not an injected native shutdown fault or successful cancellation of every active native workflow. Expected vanilla dummy-token user-properties/Realms authentication errors are retained; no real credentials/server join were used. The failed full navigation run remains failed.

Actual-project sync: all24 changed/new paths and531 complete source/doc/test files are hash-bound to the final tested stage; the actual pinned26.2 rebuild and source/archive audits pass. All554 JAR entries match the stage and independent GUI-tested JARs. Relative to4af773e, only Bridge, client stop callback, StashService and StashTask implementations change; StashService$1 differs only in debug metadata (full javap -c -p identical), and all other549 entries are byte-identical. Sixteen actual-production Java25 model entry points pass, as do actual UI/GroupHeader/Chat models, five launcher-exit fixtures and legacy migration. Main/origin-main/v2.0.17 and installed2.0.17 SHA remain unchanged; no stable game preferences were edited, and no2.1.0 installation/release/live transaction is claimed. Full-GUI route drift and earlier gear intermittence remain explicit release blockers.

## 2026-10-29 — opt-in navigation call-site diagnosis

- Added only a smoke-mod diagnostic Mixin and explicit prepare-time opt-in (`KUANPERION_SMOKE_NAV_TRACE=1`). It records actual mouse/sidebar/navigation-setter call sites and current local route; it does not cancel/inject native events, modify production navigation, fetch/upload data or change the production JAR. The first diagnostic configuration incorrectly reserved the ordinary production UI namespace and failed loading before any UI run. The error log/crash report is retained; corrected configuration uses a dedicated smokemixin package. This was a new test-configuration fault, not a production startup regression.
- The actual opt-in metadata block now has default/zero/enabled-mode regression checks, normal entrypoint preservation and a dedicated-package/smoke-source check. Restoring the original broad package produces a genuine failing assertion; corrected metadata passes. A temporary zsh read-only-variable error interrupted the local RED wrapper after recording the intended assertion; the source configuration was immediately restored and the unchanged focused GREEN completed. No actual project/game data was involved.
- With corrected diagnostic configuration, a fresh isolated full client passes all39 states/39 screenshots, the actual Book header/checkbox checks, UI_SMOKE_PASS, UI_SMOKE_SHUTDOWN_PASS and process exit0. The applied diagnostic recorded73 direct fixture clicks and no other/native mouse click in this run. Its production JAR is entry-identical to all554 frozen lifecycle entries. The production UI code was not changed. This establishes this instrumented run only: earlier phase33 drift and gear intermittence are **not reproduced or diagnosed**, and external input remains an unproven hypothesis for those older untraced runs. Both failed records and release gates stay open, not erased by this pass.
- See [diagnostic procedure](ui-smoke-diagnostics-2.1.0.md). Normal smoke remains trace-disabled; neither diagnostic Mixin nor metadata is packaged in the production artifact. No server transaction, Bazaar/George compatibility or complete2.1.0 acceptance is claimed.

Diagnostic sync checkpoint: eight changed/new paths and534 full source/doc/test hashes verified. Actual-project UI/GroupHeader/Chat/launcher-exit/trace-metadata models and migration pass. Recompiling the complete smoke mod from actual-project classes produces the same entry-by-entry smoke artifact as the instrumented GUI run; all554 production JAR entries remain byte-identical to the earlier rebuilt lifecycle artifact. No production rebuild is implied for this subsequent test/documentation-only change. Stable refs/installed2.0.17 are unchanged.

## 2026-10-29 — Pet Sell single-attempt result proof (not a native seller)

- Added a Minecraft-independent parent-owned proof component for one exact pet from the private preflight plan, an explicit positive minimum/native-declared quote and unchanged complete43 inventory. A supplied typed guarded-result boundary is called once; the component has no own menu operation, item transfer, command, networking or persistence implementation. Confirmation requires only the selected pet to disappear, both fresh applied main/menu-alias receipts, physical/server empty cursor, no selected UUID left in menu and an exact declared server-sale notice. That typed tuple is **not** a current George string/parser, native price/custody/payout source or evidence that a real server reports a UUID; those adapters and the feature itself remain unavailable.
- A missing-API RED preceded implementation. Three subsequent actual behavioral REDs exposed cancel-during-preflight, accepted nested-attempt overwrite/re-send and cancel-during-poll confirmation; post-read state checks fix them. Three further lease-revocation REDs showed session-handle identity alone did not prove the coordinator's original lease. The new passive exact identity query plus before/after-read/after-send checks deny old ownership without validating or invalidating a replacement foreign-world Stash lease. Source guards retain no runtime caller/unfinished toggle. No shared-session acquire/finish/close/retry/Farming enable authority was added to this leaf.
- The passive coordinator query is identity-only, not permission or expiration validation. All seven previous coordinator method bodies are compiled-semantically identical after normalizing constant-pool reference numbers in javap -c -p output; only the new query is added. Existing isOwner/permits semantics remain unchanged.
- The pre-lease-review staged full suite/build/UI/migration/native run passed its then-current tests, but was superseded by the additional counterexamples and corrected final reviewed stage. Final reviewed test.sh, pinned26.2 Java25 build/source+archive audits, UI/GroupHeader/Chat/launcher-exit/trace-metadata models, legacy migration and native-observation fixtures pass. New proof/preflight/Bazaar and four shared-session observation models also pass. One earlier focused wrapper accidentally named nonexistent AutomationSessionTest after three passing entry points; that wrapper failed and the corrected existing named models were rerun successfully, not counted as a production regression or an all-pass run.
- No fresh GUI client or live transaction was started for this unused leaf addition. All previous554 production entries remain byte-identical except AutomationCoordinator.class, whose only semantic addition is the passive query; four new PetSaleAttempt leaf/record/enum classes make558 entries. Existing UI classes are unchanged, so the earlier instrumented39-state result still applies only to those unchanged paths; prior phase33/gear causes remain unresolved and are not claimed fixed. Full native George transfer/recovery/payout/service/settings/UI and the wider2.1.0 gates remain open; overall delivery estimate is not raised by counting pure tests.

Actual sync checkpoint: ten changed/new paths and537 complete source/doc/test hashes match the final reviewed stage (ignored Finder .DS_Store files are not source inputs). Actual pinned26.2 rebuild/source+archive audits pass; all558 ZIP entries match the tested artifact. Twenty-four actual-production Java25 model entry points and actual UI/GroupHeader/Chat/exit/trace-metadata/migration tests pass. Stable main/origin-main/v2.0.17 and installed2.0.17 checksum remain unchanged. No actual game configuration, stable JAR, main/tag or2.1.0 publication/installation was changed.

## 2026-10-29 — Bazaar whole-commodity planning (not a native seller)

- Added read-only explicit whole-product sale planning. Each raw ID requires an approved native exemplar with matching component fingerprint/name/lore/native ID, no UUID/protection and a positive bounded unit-net minimum. Full43 snapshots and same-ID hotbar/equipment/protected/variant/UUID vetoes prevent silently selling other stacks. Product/stack/item caps reject the whole plan rather than truncate an unknown partial-quantity operation. Unrelated items remain in the immutable baseline, not a Sell Inventory authorization.
- Existing fresh bounded public-depth sell estimates are gross references only. Exact configured actual-net bounds are separate; no fee rate, current native payout format, live price or executable quote is inferred. Defaults approve no product; no runtime caller, endpoint, startup fetch, native menu action, backend switch, service/UI or persisted setting is introduced. Existing NPC Auto Sell is untouched. The [official public API documentation](https://api.hypixel.net/) was checked read-only during development for documented summary depth/weighted aggregates and example ordering; no account/profile/auth query or current native-GUI acceptance is claimed.
- Missing-API RED preceded implementation. Focused actual planner/market/acquisition/NPC task/service models pass, including whole-product caps, all variant/protection cases, exact minimum-vs-gross distinction, stale/insufficient data, multi-product stable order/total, immutable approval/slot/baseline and unrelated/duplicate UUID cases. A new source guard initially expected the wrong immutable-baseline parameter spelling; it was corrected without changing the passing behavioral immutability tests.
- Final staged test.sh, pinned26.2 Java25 build/source+archive audits, UI/GroupHeader/Chat/exit/trace-metadata models, migration and native-observation fixtures pass. All558 previous JAR entries are byte-identical; only five new leaf planner/record classes are added, making563 entries. No new GUI client or server sale was run for this unwired addition. The previous instrumented39-state run applies only to unchanged UI/old runtime paths; unresolved gear/phase33 causes and wider native/live release gates remain open.
- Current product-specific sale navigation/quantity/fee/net quote, irreversible confirmation/receipt/custody/return, policy persistence and actual Trade backend integration remain substantive unfinished work. Minimum native evidence is recorded in live-evidence-needed-2.1.0.md; a successful pure plan or an API number must not be promoted into sale authority or complete2.1.0 status.

Actual sync checkpoint: ten changed/new paths and541 complete source/doc/test hashes match the final tested stage. Actual pinned26.2 rebuild/source+archive audits pass; all563 ZIP entries match, with every previous558 entry unchanged and only five unused planner classes added. Twenty-nine actual-production Java25 model entry points and actual UI/GroupHeader/Chat/exit/trace-metadata/migration checks pass. Main/origin-main/v2.0.17 and installed2.0.17 checksum remain unchanged; no actual game configuration, stable JAR,2.1.0 install/publication or live trade was changed/claimed.

## 2026-10-29 — incumbent-first same-pass dispatch

A new actual-Bridge/real-Session actor regression first fails against the old dispatcher: when Greenhouse completes, earlier already-due Pest cannot acquire before tick returns. The corrected dispatcher snapshots eight gameplay active flags, ticks incumbents first and every other service once in approved idle priority order. Eight incumbent cases plus manual priority/Stash isolation, no-due release, callback stop/generation invalidation, settings takeover and visible callback failure are covered. It neither forces non-due followups nor rearms Farming. Actors do not prove native tool/input cleanup or a complete server maintenance chain; existing service/native/full release gates remain necessary.

Staged verification: test.sh, pinned26.2/Java25 build and source/archive audits, UI/GroupHeader/Chat/exit/trace-metadata models, byte-preserving legacy migration and native-observation/controller fixtures all pass. The production payload has no new classes/resources; only AutomationClientBridge.class changes relative to the prior563-entry artifact. No fresh GUI or live server session is claimed for this checkpoint; earlier retained UI intermittence and incomplete adapters remain release blockers.

Actual-project verification: the pinned rebuild/audits,29 models against actual built production classes, UI/Chat/GroupHeader/launcher-fixture models and migration all pass. All541 source/doc/test hashes and563 archive entries are bound to the tested stage; only Bridge.class differs from the previous artifact, and javap instruction comparison confirms its other148 method bodies unchanged (constant-pool indices normalized). Stable main/origin-main/v2.0.17 and installed2.0.17 checksum are unchanged. No game preference, installed JAR, native server action or2.1.0 release was changed/claimed.

## 2026-10-29 — user-approved legacy-first Bazaar terminal adapter

The user explicitly changed sequencing: continue implementation using source-marked supplied Aether behavior, then request one consolidated current-server evidence set after implementation. Record provisional assumptions in upstream-compatibility-2.1.0.md; earlier screenshot-first implementation instructions are superseded, not falsely marked verified. No upstream runtime/client is executed; the supplied artifact SHA is checked and BazaarBuySession/BazaarUtils/GeorgeManager inspected statically.

New BazaarPurchaseView and BazaarPurchaseController connect original legacy quantity/custom/confirm concepts to local native purchaseClick and the real parent/budget/receipt attempt. Total Price numeric/coins grammar is a separately labeled local provisional assumption. Current navigation/sign/balance/parent/settings/UI/live work remains unfinished. New pure/native-controller missing-API REDs precede implementation; the first pure fixture mistakenly used nonexistent catalog/book constructors and was corrected before the API RED, not counted as a production failure. Adversarial actual-controller tests expose a genuine own-reservation RED at an exact128-coin budget: re-parsing dynamic available balance after reserving128 falsely rejects the same quote. The final gate now uses exact immutable whole-menu equality for the already-authorized quote, while re-reading native inventory/menu/cursor/root/server facts; no budget bypass/refund/replay is added. Changed-price-after-reservation still refuses before native send.

The first complete chain passes full test.sh, build/audits, UI models and migration, but its new native component fixture fails before assertions: a normal ItemStack constructor requests registry-holder components not bound in this headless bootstrap. Existing native fixtures already use Holder.direct with explicit DataComponentMap; the new fixture is corrected to that same pinned26.2 construction. The retained failure is a fixture initialization error, not a production buyer failure; no assertions/production guards are removed and no GUI/live success is inferred. Native fixtures are rerun after this test-only correction.

Final staged results: full test.sh, pinned26.2/Java25 build/source+archive audits, UI/GroupHeader/Chat/launcher-exit/trace-metadata models and legacy migration pass. The corrected native-observations run also passes, including the actual terminal-controller/session/budget/receipt model and real26.2 native name/lore/component projection fixtures. No fresh graphical client, current menu compatibility, actual coins/pets or server purchase is claimed. Compared to the prior563-entry archive, only the three acquisition-plan class/debug payloads and packaged provenance notice change, with eight new terminal-adapter/view classes (571 entries). All15 old acquisition-plan/Plan/Request method instruction bodies remain identical with constant-pool indices normalized; one passive original-budget identity query is added. All other559 old entries, including legacy/UI/service behavior, are byte-identical. Native opener/search/sign/balance/parent/UI wiring and remaining whole-upgrade scope stay incomplete; no screenshot-first pause is required for their implementation.

An additional actual isolated-smoke prepare invocation fails compilation because its all-smoke source graph did not include the newly shared pure Bazaar test fixture (the separate native runner already included it). The prepare list now explicitly includes that test-only source, and the boundary guard retains this dependency. No main payload or client behavior changes; the original failed preparation is retained. This is compilation/packaging verification only, not a new GUI run.

Actual sync:20 changed/new paths,547 complete source/doc/test hashes and571 archive entries match the final tested stage. Actual pinned rebuild/audits,29 production-class model entry points, native terminal/controller/component fixtures, UI/Chat/GroupHeader/launcher-verifier models, migration and upstream-scope guard pass. Corrected isolated all-smoke preparation/compilation passes against both stage and actual project; optional existing missing native-path diagnostics are retained, no GUI process launched. Stable main/origin/main/v2.0.17 refs and installed2.0.17 checksum are unchanged; actual game settings were not edited, and2.1.0 was not installed/tagged/released.
