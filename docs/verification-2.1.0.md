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
