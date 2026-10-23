# Kuanperion

Minecraft **26.2** 的 Fabric 客戶端模組，提供 Mining、Farming 與通知功能。

**版本：`2.0.0`** · Java **25** · Fabric Loader **0.19.5+** · Fabric API

## 安裝與更新

1. 使用 Minecraft 26.2／Java 25 的 Fabric instance，安裝 Fabric API。
2. 放入 `kuanperion-2.0.0.jar`，移除本模組的舊版 JAR，保留其他模組，完整重啟遊戲。
3. NanoVG 3.4.1 與 Linux／macOS Intel／macOS Apple Silicon／Windows native 已包在 JAR，不必另外安裝繪圖插件。
4. 首次啟動若尚無 `config/kuanperion.properties`，會複製既有版本設定；不覆寫新設定、不刪原檔。舊自訂快捷鍵亦在新鍵尚未保存時沿用。

原始碼中的舊識別僅用於設定／按鍵相容遷移；現行 mod ID、入口、資源、通知前綴與 JAR 名稱均為 Kuanperion。Git 倉庫路徑／既有 remote 暫沿用維護位置，不是遊戲內模組名稱。

## 快捷鍵

| 按鍵 | 功能 |
| --- | --- |
| `'` | 開啟設定；在設定畫面再按可關閉（文字編輯／綁鍵時優先輸入） |
| `Shift + M` | 開啟／關閉 Mining |
| `Shift + F` | 開啟／關閉 Farming |

可在設定的「快捷鍵」或 Minecraft 控制選項調整；Mining／Farming 切換仍搭配 Shift。兩者互斥，重新啟動後不自動開啟。

## 新版設定介面

- 可展開側欄、三欄功能卡片、分類與設定詳情、滑順捲動、搜尋、hover 提示。
- 原生向量繪圖、卡片／頁面／勾選動畫、色彩選擇器、文字編輯、滑桿與數值輸入。
- 「模組」只包含現有 Farming、Mining、Pest 通知、Visitor 通知、Jerry 偵測、共用 Discord 通知設定；沒有自動除蟲或其他新巨集。
- 點**卡片文字／主體**進設定，點勾選框控制啟停。停止時也能調整偏好，不會因查看設定自動啟動。
- 「顏色」即時預覽；「設定檔」保存／載入本機功能與主題 Profiles，另有 Slate、Sage、Matcha、Dusk、Ember 預設色彩。
- 「介面設定」調整 UI／文字比例與動畫時間。較小視窗會暫時縮放至可容納尺寸，不覆蓋偏好。
- Ctrl 或 macOS Command 支援複製／貼上／全選；Esc 優先結束編輯或返回，再關閉畫面。
- 本輪只換設定介面；未接管 Minecraft 主選單或更換遊戲 HUD，未引入第三方帳號、遙測、IRC、遠控、更新器。

## Mining

1. 點 Mining 卡片，進「目標方塊」，設定種類數量（1～5），點「開始選擇目標方塊」。
2. 回到遊戲後對準方塊按右鍵，選滿指定種類；種類不可重複。
3. 用 ↑／↓ 調優先序，或刪除指定順位。
4. 調整半徑（1～8）、轉動速度（1～40°）、挖礦延遲（1～20 ticks），再啟用。

執行時保留原本移動／視角／左鍵鎖定與目標優先序，不因更換 UI 改變演算法。

## Farming

使用相容農場，站在起點、準星對準作物並持對應工具，按 Shift＋F。

- 黃色染色玻璃：換列；淺藍色：起點；粉紅色：終點。
- 保留各作物、南瓜／西瓜啟動方向、農場列標記與返回流程。
- 工具偵測、智慧暫停、異常狀況處理仍獨立開關。
- Farming 非預期停滯滿 5 秒，開啟異常處理且有共用 Webhook 時通知 Discord；正常等待／GUI／拿錯工具不算停滯。
- 開設定不會強制關掉巨集，但仍遵守各自「智慧暫停」的既有規則。

## 通知與 Jerry

- Pest／Visitor 卡片勾選框是各自總開關；點主體可在關閉時調門檻與通道。
- Pest 門檻 1～8；Visitor 1～5。Discord、聊天、螢幕通知獨立。
- Discord URL 在「Discord 通知」共用設定；沒有 URL 時不允許新啟用 Discord 通道。
- 螢幕通知時間輸入正小數。Jerry 的光柱期限同樣支援正小數、預設 300 秒、不設使用者秒數上限。
- Jerry 沒有新增總開關；Green／Blue／Purple／Golden 各自篩選，另決定是否停止 Mining／Farming。
- 保留 Farming 停機光柱、10 格內手動重啟提前清除、離線時間計入期限等行為。

## 設定與隱私

- 主設定：`config/kuanperion.properties`；UI 主題：`config/kuanperion-theme.json`。
- Profiles：`config/kuanperion/profiles`／`themes`，均為本機資料。
- 功能 Profile／剪貼簿匯出排除 Webhook、光柱座標／世界與 runtime 啟停值；載入 Profile 不會自動開啟巨集，也不覆寫本機 Webhook。
- 勿公開主 properties 檔或填有 Webhook 的畫面；它們可能含秘密。沒有把帳號與設定上傳作者的機制。

## 開發與驗證

```bash
./scripts/test.sh
./scripts/build.sh
./scripts/test-ui-migration.sh
./scripts/test-ui.sh
```

`test-ui.sh` 需要先 build，使用實際 26.2 classpath 做 UI 模型／文字／動畫／布局測試。遊戲畫面／輸入 smoke test 使用 `scripts/ui-smoke/prepare.py`、`scripts/ui-smoke/launch.py` 與 `src/smoke`；需先 build，只啟動隔離視窗，不隨正式 JAR 提供測試入口。測試使用虛構帳號、不進伺服器，原版自身帳號／Realms 請求出現 401 屬預期；不使用 Prism 的真實登入資料。驗證範圍與限制見 [docs/verification-2.0.0.md](docs/verification-2.0.0.md)。

必要來源與授權見 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 與 [LICENSE](LICENSE)，也包進 JAR。
