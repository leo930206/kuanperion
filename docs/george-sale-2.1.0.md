# George Pet Sell — 2.1.0 開發線

此為已接線但尚未連服驗收的流程，不是整個大升級完成宣告。完整離線回歸、原生編譯／fixtures、UI、遷移與隔離 prepare 編譯已通過；來源假設見 upstream-compatibility-2.1.0.md 的 U-GEORGE。

## 使用與權限

Trade → Pet Sell。預設關閉、沒有授權種類。種類與稀有度必須明確填寫，例如 `SLUG:EPIC`；這只是格式範例，不是自動授權。另設定最高經驗、正整數最低實收與間隔，每次處理一隻；自動預設只在原手動 Farming 執行中。所有偏好保存於遊戲外部 `config/kuanperion/automation.json`，不在 JAR 內。更新不重置偏好，也不重播暫存手動請求。

UUID／原生 PET ID 與完整 petInfo 先預檢。綁定、啟用中、收藏／鎖定、裝備／快捷列、糖果、附件、皮膚、保護 UUID、未知資料與重複身份優先拒絕；不能因名稱像 Rat/Slug 或等級文字是1就出售。沒有作者帳號、profile 查詢、上游連線或遙測。

## 原生流程

1. 延後到關閉設定畫面，取得單一 PET_SELL root／原 Farming hold。唯一 typed opener 是 `/call george`，發送事件須被觀察；錯誤不公告成功或默默重試。
2. 原版 George/Sell Pets/Pet Collector/Offer Pets 角色為候選。完整43格、選取的原 pet、已套用的選單與空游標回條一致，才從唯一核准主背包格 QUICK_MOVE。
3. 原始槽與當前 alias 的成對新回條、NPC13 中同一原生物品／UUID、其他43格不變及新空游標，才能鑄造私有 custody token。從 George 轉入新的 Offer Pets container 也要重新驗證實際 destination 回條。
4. 核對 NPC 中原 pet 與最低價，唯一 Sell Pet/Accept/Confirm/Offer 候選須有明確單一正整數報價。只允許一次同寵物、同報價的額外 Confirm Sale/Are You Sure 確認，不接受新的售價或重播 token。
5. 正常完成需要原 NPC 物品清空、完整43格維持入庫後基準、新來源／游標回條及私有 pending pet 對應的伺服器 GAME 成交文字；不是用 API 估價或本機預測當成功。GAME 文字沒有 UUID 時，只能關聯到這個已證明的唯一 pending custody，不能由名字造出新身份。
6. 若由伺服器關閉選單，必須先有原 private sale 的 Close HEAD ticket，再在 vanilla TAIL 確認同世界／ID 的真正 inventory-menu 關閉。仍另需售出後全43格 canonical 回條、新 window0 空游標及精確成交文字。沒有 ticket 的本機 GUI 消失不可走這個成功分支。
7. 沒有可接受的報價時，可在同一 root 下只取回原 custody pet。QUICK_MOVE 實際目的地可為另一空主背包格，但要確認該格 canonical＋alias、NPC13 清空、新空游標、其他所有格不變，才可關閉自己的選單。回復後停用自動賣寵，避免循環重新報價。

手動輸入、換世界、Jerry、停止／重開 Farming、未知 GUI、選單 identity 替換、錯誤或不確定成交會停止；不擅自修復使用者游標、關閉已讓出的視窗或重啟 Farming。取消後若物品可能還在 NPC，狀態明確提示手動確認。原生 controller 與 task 結束時清除世界／選單 ownership，即使清理失敗也不能釋放成功 hold。

## 明確尚待一次批次驗收

原版提供候選流程與角色，不提供本地嚴格語法的完整事實。以下是 **額外本地 provisional 假設**：精確大小寫／標點的 menu titles、`You will receive/Sell Price/Price/Offer: N Coins`、`You sold <原 pet 名稱> to George for N coins!`、NPC13 的完整原物品保留，以及額外確認與自動關閉時的回條組合。這些要在整體實作後一次確認，不逐項等截圖；未知資料安全停止，不宣稱已在現行 Hypixel 成功售出。
