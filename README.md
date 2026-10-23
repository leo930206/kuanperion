# Kuanperion

Minecraft **26.2** 的 Fabric 客戶端模組，提供 Mining、Farming 與通知功能。

**版本：`2.0.12`** · Java **25** · Fabric Loader **0.19.5+** · Fabric API

勾選框、單行輸入與數字控制依完整設定卡片高度垂直置中，無論是否有說明；螢幕通知時間輸入與開關維持獨立。輸入框預設高 22 logical px、內文字 12 px。

## 安裝與更新

1. 使用 Minecraft 26.2／Java 25 的 Fabric instance，安裝 Fabric API。
2. 放入 `kuanperion-2.0.12.jar`，移除本模組的舊版 JAR，保留其他模組，完整重啟遊戲。
3. NanoVG 3.4.1 與 Linux／macOS Intel／macOS Apple Silicon／Windows native 已包在 JAR，不必另外安裝繪圖插件。
4. 首次啟動若尚無 `config/kuanperion.properties`，會複製既有版本設定；不覆寫新設定、不刪原檔。舊自訂快捷鍵亦在新鍵尚未保存時沿用。

原始碼中的舊識別僅用於設定／按鍵相容遷移；現行 mod ID、入口、資源、通知前綴與 JAR 名稱均為 Kuanperion。專案維護位置為 `/Users/kuan/projects/kuanperion`，remote 為 `https://github.com/leo930206/kuanperion.git`。

## 導覽

左側功能分成 **Farming**（Farming、Pest 通知、Visitor 通知）、**Mining**（Mining）、**Other**（Jerry 偵測、Discord Webhook）。每組只顯示所屬功能；詳情返回與搜尋跳轉維持正確分組。

單一齒輪 **Setting** 頁籤順序為「設定檔／介面／顏色／快捷鍵」。顏色、設定檔不再有第二排分類；保留內容小標題，顏色頁依序為「配色收藏 → Colour Presets → 選單顏色」。內建六款白底柔和配色為 Sky Blossom、Ocean Mist、Mint Garden、Peach Cream、Lavender Haze、Rose Quartz，按鈕提供色彩預覽，小視窗自動換行，不含第七個預設按鈕。五組配色與既有設定均保留。

左上保留大頭貼與名稱；底部無 K、無 MC 版本與 Setting 下分隔線。版本文字在收合時為 `2.0.12`，展開時為 `Mod Version 2.0.12`（無冒號），依側欄寬度水平置中，版本讀取 metadata。Setting 與版本列間距縮短；兩種狀態均可點版本開啟「關於」。

## 介面與操作

- 面板稍微縮小，仍動態適應視窗；縮小／放大與 DPI 改變後重新置中，不再拖動離開中央。
- Search 輸入即時更新、不重播切頁動畫；空白聚焦亦有輸入游標。
- 最上層取得滑鼠懸停／點擊／滾輪：展開側欄不穿透下方內容，選色器／下拉選單也阻擋下層懸停。
- Mining 目標方塊單行顯示 `順位 N: 方塊名稱`，文字垂直置中，保留圖示與排序／刪除按鈕；移除介面頁最底下重複的版本資訊列，側欄版本保留。
- 全介面取消懸停說明框；側欄維持原有圖示／文字亮度，功能入口固定位置只畫一圈天空藍邊框，設定列保留描邊回饋。
- 設定列共用 12px 左邊距與標題樣式；開啟功能卡片僅顯示勾選，不改背景深淺。
- 通知時間與 Jerry 光柱時間使用 66px 置中數字框，與通知門檻一致；Webhook 輸入目標寬度 390px（小視窗會縮限與換行）。
- Jerry 四色在同一列，個別開關；全部關閉即不偵測。光柱時間可設 0，關閉光柱但不影響其他 Jerry 處理。
- Jerry 新增「出現 Jerry 後自動 sethome」，預設關閉；只在勾選顏色的出現訊號觸發，與通知／停止動作獨立。
- `/sethome` 與 `/home` 快捷鍵都預設未綁定，支援組合鍵；只在遊戲世界、無選單且視窗聚焦時觸發，一次按下只發一次。
- 啟用異常狀況處理並已設定 Webhook 時，自動回到 Garden 後通知原本被傳送到的地點；只發一次，Farming 仍保持關閉等待操作。
- 所有本模組本機聊天訊息統一 `[Kuanperion]` 天空藍→白→粉紅漸層，不使用粗體。

專案目前位於 `/Users/kuan/projects/kuanperion`；GitHub：[leo930206/kuanperion](https://github.com/leo930206/kuanperion)。大型自動化移植尚未開始。

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

自 2.0.8 起，更新不再自動替換舊版預設配色或已儲存的輸入框高度。已有設定優先；想套用新預設請手動重設或選擇配色。

### 更新與設定保留
關閉遊戲後只替換 `mods/` 中的本模組 JAR；同一個遊戲 instance 會沿用以下檔案，不需要重新設定：
- `config/kuanperion.properties`：Farming、Mining、通知、Webhook、快捷鍵修飾鍵等。
- `options.txt`：Minecraft 快捷鍵主鍵。
- `config/kuanperion-theme.json`：目前配色與介面偏好。
- `config/kuanperion/`：設定檔、主題、五組配色槽。

換電腦／新 instance 時需另外複製這些設定；備份含 Webhook，請勿公開。新選項才使用預設值；無效數值仍會做安全範圍檢查。Farming／Mining 的「正在執行」狀態不會在重開後自動恢復，這不是設定遺失。舊名稱遷移只在新檔不存在時複製，保留原檔。


### 配色收藏（最多五組）

新安裝預設空清單，按「新增配色」擷取目前配色；已有收藏會保留，不會清空。點色票區立即載入；右側「儲存」更新為目前配色，覆寫須於 5 秒內再確認；「刪除」移除該收藏但不改目前配色。

每列不顯示編號／已儲存等文字，改以完整已存色票預覽。色塊依下方「選單顏色」由上到下的順序排列，從左向右、空間不足換行，保持固定大小與間距，不擠成細線。五組上限延續原本設定；刪除後可再新增。

仍使用 `config/kuanperion/color-presets/slot-1.json`～`slot-5.json`，與舊版相容；未使用的位置不顯示。儲存覆寫前保留 `.bak`，刪除時保留 `.deleted.bak`；備份不會當成有效收藏重新出現。只儲存選單顏色、彩虹與漸層，不影響功能設定、快捷鍵、Webhook、HUD、比例與動畫。

左上角使用模組現有的 Minecraft 大頭貼 icon，保留原色、像素縮放並本機快取；不連線抓取帳號或 skin。底部無 K，點版本文字可進入「關於」。設定說明只在左側預留文字欄換行，避免與右側輸入框及按鈕重疊。

### 開選單時的執行行為

Farming／Mining 已啟動時，開啟 Kuanperion 的 `'` 選單會繼續移動、轉向與採收／挖掘；關閉自身選單也不新增智慧暫停緩衝。GUI 的打字與滑鼠操作不會穿透到遊戲，功能快捷鍵仍不在 GUI 編輯中觸發。目標方塊右鍵選取須回到世界畫面。

背包、聊天室、其他模組選單仍受原「智慧暫停」控制，關閉後保留 0.5 秒緩衝；從其他 GUI 轉入自身設定，也保留這段既有緩衝。工具偵測、Jerry 停止與異常回復等保護不取消。只有自身設定介面被排除，不是全域關閉暫停保護。

## 新版設定介面

- 可展開側欄、三欄功能卡片、分類與設定詳情、滑順捲動、搜尋、hover 提示。
- 原生向量繪圖、卡片／頁面／勾選動畫、色彩選擇器、文字編輯、滑桿與數值輸入。
- 三個功能入口只包含現有 Farming、Mining、Pest 通知、Visitor 通知、Jerry 偵測、共用 Discord 通知設定；沒有自動除蟲或其他新巨集。
- 說明距離統一為標題下方 6 logical px，群組、設定列、搜尋與 hover 使用同一偏好；在「介面設定」可改「說明間距」（6～12）與「輸入框高度」（14～24，預設 22）。數字／單行時間框內文字預設 12 px，快捷鍵按鈕不縮小。
- Farming／Mining 的工具偵測、智慧暫停、異常處理、範圍／轉速／延遲等設定，直接在列內顯示用途說明，另保留 hover 提示。
- 點**卡片文字／主體**進設定，點勾選框控制啟停。停止時也能調整偏好，不會因查看設定自動啟動。
- 「顏色」即時預覽；「設定檔」保存／載入本機功能與主題 Profiles，另有 晴空藍粉、Slate、Sage、Matcha、Dusk、Ember 預設色彩。
- 「介面設定」調整 UI／文字比例與動畫時間。較小視窗會暫時縮放至可容納尺寸，不覆蓋偏好。
- Ctrl 或 macOS Command 支援複製／貼上／全選；Esc 優先結束編輯或返回，再關閉畫面。
- 未接管 Minecraft 主選單或更換遊戲 HUD，未引入第三方帳號、遙測、IRC、遠控、更新器。

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
- Farming 非預期停滯滿 5 秒，開啟異常處理且有共用 Webhook 時通知 Discord；正常等待／其他 GUI／拿錯工具不算停滯；Kuanperion 自身設定選單內則繼續監控。
- 開啟 Kuanperion 自身設定不會暫停 Farming／Mining；其他畫面仍遵守各自「智慧暫停」。

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

`test-ui.sh` 需要先 build，使用實際 26.2 classpath 做 UI 模型／文字／動畫／布局測試。遊戲畫面／輸入 smoke test 使用 `scripts/ui-smoke/prepare.py`、`scripts/ui-smoke/launch.py` 與 `src/smoke`；需先 build，只啟動隔離視窗，不隨正式 JAR 提供測試入口。測試使用虛構帳號、不進伺服器，原版自身帳號／Realms 請求出現 401 屬預期；不使用 Prism 的真實登入資料。驗證範圍與限制見 [docs/verification-2.0.12.md](docs/verification-2.0.12.md)。

必要來源與授權見 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 與 [LICENSE](LICENSE)，也包進 JAR。
