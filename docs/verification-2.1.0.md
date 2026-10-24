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
