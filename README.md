# Kuanperion

Minecraft **26.2** 的 Fabric 客戶端模組，提供 Mining、Farming 與通知功能。

**版本：`2.0.4`** · Java **25** · Fabric Loader **0.19.5+** · Fabric API

勾選框、單行輸入與數字控制依完整設定卡片高度垂直置中，無論是否有說明；螢幕通知時間輸入與開關維持獨立。輸入框預設高 22 logical px、內文字 12 px。

## 安裝與更新

1. 使用 Minecraft 26.2／Java 25 的 Fabric instance，安裝 Fabric API。
2. 放入 `kuanperion-2.0.4.jar`，移除本模組的舊版 JAR，保留其他模組，完整重啟遊戲。
3. NanoVG 3.4.1 與 Linux／macOS Intel／macOS Apple Silicon／Windows native 已包在 JAR，不必另外安裝繪圖插件。
4. 首次啟動若尚無 `config/kuanperion.properties`，會複製既有版本設定；不覆寫新設定、不刪原檔。舊自訂快捷鍵亦在新鍵尚未保存時沿用。

原始碼中的舊識別僅用於設定／按鍵相容遷移；現行 mod ID、入口、資源、通知前綴與 JAR 名稱均為 Kuanperion。Git 倉庫路徑／既有 remote 暫沿用維護位置，不是遊戲內模組名稱。

## 快捷鍵

| 按鍵 | 功能 |
| --- | --- |
| `'` | 開啟設定；在設定畫面再按可關閉（文字編輯／綁鍵時優先輸入） |
| `Shift + M` | 開啟／關閉 Mining |
| `Shift + F` | 開啟／關閉 Farming |

表內為預設值，**Shift 不再強制**。在模組「快捷鍵」點擊綁定框，可直接按 `F`，或先按住 Ctrl／Shift／Alt／Command 再按主鍵，例如 `Ctrl + F`、`Ctrl + Shift + F`、`Ctrl + Alt + Shift + F`。左右修飾鍵皆可，完整組合會顯示並保存；重設回預設，Esc 取消，未搭配修飾鍵的 Delete／Backspace 清除。

字母主鍵固定顯示 A～Z，不受系統注音鍵名影響；預設仍為 **Shift + M／Shift + F**，不因展示多鍵捕捉測試而改動你的設定。

同一完整組合不能重複分配給兩個功能；不同組合可共用主鍵。Mining／Farming 僅在聚焦的遊戲畫面觸發，聊天、背包、設定與綁鍵期間不觸發；長按不會反覆切換，關閉畫面後仍需重新按鍵。兩者互斥、重啟不自動開啟。Minecraft 原生控制頁僅編輯主鍵，完整修飾鍵組合以本模組設定頁為準；請避開原版或其他模組的衝突綁定。

## 預設主題：晴空藍粉

以白色為主，天空藍用於選取、勾選、數值與互動重點；左下淡藍、右上淡粉柔和漸層，中間保留白色閱讀區。主標題、設定標籤與數值改用淺天空藍（不再深藍），說明用中性灰；滑桿白色把手加天空藍外圈。

「顏色」可開關藍粉漸層、調整兩端色彩、範圍與濃度；預設縮小範圍（0.48）、降低濃度（0.60），讓白色留白更多。「設定檔 → 主題」可選「晴空藍粉」或「預設顏色」，原有深色主題仍保留。

2.0.1 完整未改色的舊晴空預設亦會升級為淺天空藍與較淡／較小漸層，先備份至 `config/kuanperion-theme.before-light-sky.bak`。自訂色不覆蓋；UI 比例、動畫、HUD、快捷鍵保持。2.0.0 未調整的原預設色會自動升級，先備份至 `config/kuanperion-theme.before-sky-blossom.bak`，保留比例、動畫與 HUD 偏好。曾改色／開啟彩虹的自訂主題不自動覆蓋；要切新外觀請手動選「晴空藍粉」。只影響設定介面，不改玩法、遊戲 HUD 或 Webhook。

### 配色收藏（最多五組）

到「顏色 → 配色收藏」：每列右側「儲存目前配色」保存當前顏色，之後點擊該列即可載入。已存槽要改存時，點「覆寫目前配色」，再於 5 秒內點「確認覆寫」；未確認不改檔。

五組保存在 `config/kuanperion/color-presets/slot-1.json`～`slot-5.json`，重啟後保留；覆寫前保留上一份 `.bak`。只保存選單色票、彩虹選取與漸層設定，不影響 Webhook、Mining／Farming、快捷鍵、HUD、比例、動畫、間距與輸入高度。原本「設定檔 → 主題」功能仍獨立存在。

## 新版設定介面

- 可展開側欄、三欄功能卡片、分類與設定詳情、滑順捲動、搜尋、hover 提示。
- 原生向量繪圖、卡片／頁面／勾選動畫、色彩選擇器、文字編輯、滑桿與數值輸入。
- 「模組」只包含現有 Farming、Mining、Pest 通知、Visitor 通知、Jerry 偵測、共用 Discord 通知設定；沒有自動除蟲或其他新巨集。
- 說明距離統一為標題下方 6 logical px，群組、設定列、搜尋與 hover 使用同一偏好；在「介面設定」可改「說明間距」（6～12）與「輸入框高度」（14～24，預設 22）。數字／單行時間框內文字預設 12 px，快捷鍵按鈕不縮小。
- Farming／Mining 的工具偵測、智慧暫停、異常處理、範圍／轉速／延遲等設定，直接在列內顯示用途說明，另保留 hover 提示。
- 點**卡片文字／主體**進設定，點勾選框控制啟停。停止時也能調整偏好，不會因查看設定自動啟動。
- 「顏色」即時預覽；「設定檔」保存／載入本機功能與主題 Profiles，另有 晴空藍粉、Slate、Sage、Matcha、Dusk、Ember 預設色彩。
- 「介面設定」調整 UI／文字比例與動畫時間。較小視窗會暫時縮放至可容納尺寸，不覆蓋偏好。
- Ctrl 或 macOS Command 支援複製／貼上／全選；Esc 優先結束編輯或返回，再關閉畫面。
- 本輪只換設定介面；未接管 Minecraft 主選單或更換遊戲 HUD，未引入第三方帳號、遙測、IRC、遠控、更新器。

## Mining

1. 點 Mining 卡片，進「目標方塊」，設定種類數量（1～5），點「開始選擇目標方塊」。
2. 回到遊戲後對準方塊按右鍵，選滿指定種類；種類不可重複。
3. 數量輸入與開始選擇按鈕在同一行。列表只顯示已選取的項目，不預建五個空順位；每項附目前資源包的方塊材質圖示與名稱，可用 ↑／↓ 調優先序或 × 刪除。
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
- 螢幕通知的時間框與勾選框在同一行，時間框在勾選框左邊；輸入正小數秒數。留空顯示「顯示時間」，實際沿用預設 3 秒，空白狀態會保存。編輯時間不切換通道，勾選通道不改時間。Jerry 的光柱期限同樣支援正小數、預設 300 秒、不設使用者秒數上限。
- Jerry 沒有新增總開關；Green／Blue／Purple／Golden 各自篩選，另決定是否停止 Mining／Farming。
- 保留 Farming 停機光柱、10 格內手動重啟提前清除、離線時間計入期限等行為。

## 設定與隱私

- 主設定：`config/kuanperion.properties`；UI 主題：`config/kuanperion-theme.json`。
- Profiles：`config/kuanperion/profiles`／`themes`，均為本機資料。
- 功能 Profile／剪貼簿匯出排除 Webhook、光柱座標／世界與 runtime 啟停值及本機修飾鍵設定；載入 Profile 不會自動開啟巨集，也不覆寫本機 Webhook。
- 勿公開主 properties 檔或填有 Webhook 的畫面；它們可能含秘密。沒有把帳號與設定上傳作者的機制。

## 開發與驗證

```bash
./scripts/test.sh
./scripts/build.sh
./scripts/test-ui-migration.sh
./scripts/test-ui.sh
```

`test-ui.sh` 需要先 build，使用實際 26.2 classpath 做 UI 模型／文字／動畫／布局測試。遊戲畫面／輸入 smoke test 使用 `scripts/ui-smoke/prepare.py`、`scripts/ui-smoke/launch.py` 與 `src/smoke`；需先 build，只啟動隔離視窗，不隨正式 JAR 提供測試入口。測試使用虛構帳號、不進伺服器，原版自身帳號／Realms 請求出現 401 屬預期；不使用 Prism 的真實登入資料。驗證範圍與限制見 [docs/verification-2.0.4.md](docs/verification-2.0.4.md)。

必要來源與授權見 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 與 [LICENSE](LICENSE)，也包進 JAR。
