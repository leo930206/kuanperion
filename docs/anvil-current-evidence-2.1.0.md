# Anvil 當前規格證據狀態

核對日期：2026-10-27。

- 讀取 `https://wiki.hypixel.net/Anvil`，實際 HTTP 回應為302，轉往 [官方 Wiki 關閉公告](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/)；公告日期為2026-07-21。
- 因此該網址目前不是 Anvil 規格；HTTP 最後回傳200，亦不表示取得選單內容。不能用它確認 `/av`、權限、格位、費用或收取動作。
- 過往來源／歷史截圖與目前離線 fixtures 不能替代當前選單證據。Book 的原生操作／指定格取回已獨立實作測試，但目前 layout／opener、service／設定／UI／連服驗收仍是未完成項目。
- 本次只查 Wiki 網頁；不據此推論官方遊戲／價格 API 可用或失效。不改模組端點、不新增任何作者服務／遙測。

後續取得必要、去識別的當前選單證據後，先驗證明確 title／角色／物品／費用與開啟方式，再接 runtime caller。不得把上述302公告或純測試佈局當作點擊權限。
