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

## Dispatcher 與單次手動意圖優先

Bridge 自動輪詢順序對齊既有範圍：Pest → Greenhouse → Composter → Supercraft → Visitor，之後才是 Book／NPC Sell／Junk 等獨立 Trade 工作。未到期／未啟用／未設定的服務仍自行拒絕，不建立強制佇列或修改偏好。一次仍僅有一個可撤銷 gameplay root；每個成功接續都重新驗證當下世界、原 Farming 世代、UI／控制權與該任務條件，不重開 Farming。

八個 gameplay 服務的關閉選單 gate 共用 idle 手動意圖仲裁：有尚未執行的單次 UI 請求時，其他自動計時工作先讓路，避免較早的 Pest timer 搶走玩家剛按的 Book／Trade／Visitor。已有 root 不因新的／競爭意圖被仲裁器搶走；原本每個服務的30秒／世界／Farming／設定撤銷條件仍有效，競爭意圖不被保存或重播。多個同時意圖採上述穩定服務順序。

Stash 不新增 gameplay root 或排程 UI；當有單次 gameplay 意圖等待時，暫不 tick 它，避免其合法 outgoing-command event 先清掉尚未派送的 Trade 意圖。取得既有 root 後，原 coordinator 的 COMMAND 互斥仍有效。

`test-dispatch-priority.py` 執行真正 Bridge tick/readiness bodies 配真實 AutomationSession 與可控工作 actors。原749c90d tick／對應舊 screen gate 可重現「自動 Pest 先開始，手動 Trade 消失」；新實作驗證手動優先、Stash 隔離、due 順序、已有 root 不被替換、失效 Farming 不續跑。Actor 測試不是完整原生多任務或 Hypixel 連服證明；既有各 task 的伺服器／回程／停止守門仍是必要條件，完整 live handoff gate 保持未勾選。

Visitor／Supercraft 亦在其他 root 忙碌時先讓路並重置 runtime poll 標記，不做 native preflight／資料要求，也不吃掉到期工作的1秒輪詢窗口。`DispatchBusyRootTest` 使用真正兩個 service／task／session 與既有 ports，驗證忙碌零預檢、相同 timestamp 釋放後派送、原 Farming 世代、UI／Farm-off 重新守門及設定 byte 不變；仍非整條原生任務鏈驗收。
