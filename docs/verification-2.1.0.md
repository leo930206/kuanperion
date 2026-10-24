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
