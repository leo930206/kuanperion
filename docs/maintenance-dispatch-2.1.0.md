# Greenhouse／Composter 忙碌讓路與到期派送

這是自動派送的局部接續檢查點，不是完整 Pest → Greenhouse → Composter 原生／連服驗收。

## 問題與修正

原本維護工作已到期時，即使共享 session 被 Pest 或其他工作占用，service 仍可能讀取自己的 context／frame 做預檢，之後才因拿不到 root 而退出；同時消耗一秒輪詢節流。前一項工作剛結束時，已到期維護因此還可能被該節流擋住。

兩個 service 現在在其他 root 活躍時直接讓路：

- 不做該工作的原生預檢、不碰工具／位置／選單、不取得 root。
- 保留原本 `lastScheduled`／interval；不因忙碌延後到期時間，也不提早執行未到期工作。
- 清除「忙碌時已輪詢」的節流狀態，原 owner 釋放後可在同一個 timestamp 重新接受派送檢查，而不是額外等一秒。
- 重新讀取當前 GUI、Farm 啟用意圖、Garden／工具／世界等既有 task 守門條件；不是保存舊 frame／anchor 後自動重播。
- 下一項工作取得自己的新固定 feature lease，只能暫停同一個原手動 Farming generation；不沿用失效的前一個 handle，也不把 Farm 開關重新打開。

沒有新增排程佇列、持久化待辦、強制啟動、延後重播或新設定。讓路／派送開始不寫偏好；既有取消／失敗停用該功能的規則不變。單次手動請求的既有語意不改。

## 驗證範圍

`GreenhouseBusyRootTest`、`ComposterBusyRootTest` 使用真實 service／task／session，借用一個 Pest lease 隔離所有權情境：

- 舊版本確實出現忙碌預檢及釋放後同 timestamp 無法派送的四個紅燈。
- 新版本不讀取忙碌期間原生預檢；到期時間保留、未到期不提早。
- 前 owner 釋放後能立即取新 lease，持有原手動 Farm generation，不寫偏好、不呼叫安全停止。
- 期間若使用者已關閉 Farm，不能拿舊意圖重播；設定畫面仍優先，關閉後重新守門。
- 原有 service、單次 UI 請求與取消回歸一起執行。

這些測試沒有執行真正 Pest 飛行／返回，也沒有證明全服務優先順序、跨世界完整事件矩陣、所有工作間永無 legacy tick 空檔或任何伺服器成功。這些仍在進度清單的完整 handoff／release gate，不能以本局部修正代替。
