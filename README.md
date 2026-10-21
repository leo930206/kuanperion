# Kuan Hypixel SkyBlock Assistant

為 **Minecraft 26.2／Hypixel SkyBlock** 開發的 Client-side Fabric mod，提供 Mining、Farming，以及 Visitor、Pest、Jerry 通知功能。

**目前版本：`1.0.123`**

| 項目 | 說明 |
| --- | --- |
| 遊戲版本 | Minecraft `26.2` |
| 執行環境 | Fabric Client；需安裝 Fabric API |
| Fabric Loader | `0.19.0` 以上 |
| 設定分區 | Farming → Mining → Other |
| 更新紀錄 | [CHANGELOG.md](CHANGELOG.md) |

> Farming 的移動路線依特定農場布局設計，不是任意農場都能直接使用。第一次啟動前，請先確認作物、工具、站位與標記方塊，並在可監看遊戲的情況下測試。

## 目錄

- [快速開始](#快速開始)
- [Mining 挖礦輔助](#mining-挖礦輔助)
- [Farming 農耕輔助](#farming-農耕輔助)
- [Other 與通知](#other-與通知)
- [設定與紀錄](#設定與紀錄)
- [建置與測試](#建置與測試)
- [開發與維護](#開發與維護)

## 快速開始

### 安裝

1. 準備 Minecraft `26.2` 的 Fabric instance，並安裝相容的 Fabric API。
2. 依[建置與測試](#建置與測試)產生 jar。
3. 將 `build/libs/kuan-hypixel-skyblock-assistant-1.0.123.jar` 放入該 instance 的 `minecraft/mods` 資料夾。
4. 移除舊版的 **本模組 jar**，只留最新版；不要刪除其他 mod。
5. 重新啟動 Minecraft。

目前開發使用的 PrismLauncher instance 為 `Hypixel SkyBlock`，macOS 安裝位置：

```text
~/Library/Application Support/PrismLauncher/instances/Hypixel SkyBlock/minecraft/mods
```

### 快捷鍵

| 按鍵 | 功能 |
| --- | --- |
| `'` | 開啟設定頁面 |
| `Shift + M` | 開啟／關閉 Mining |
| `Shift + F` | 開啟／關閉 Farming |

- Mining 與 Farming 互斥，開啟其中一個會停止另一個。
- 重新啟動遊戲時，Mining／Farming 不會依上次狀態自動啟動。
- 設定按鈕以綠色「開啟」、紅色「關閉」顯示狀態。

### 智慧暫停與操作鎖定

Mining、Farming 各有自己的「智慧暫停」開關。

| 情況 | 開啟智慧暫停時的行為 |
| --- | --- |
| 開啟背包、聊天室、Esc、Bazaar、Auction House 或模組設定等 GUI | 暫停該功能的自動操作，但不關閉功能 |
| GUI 關閉 | 持續 **0.5 秒**沒有任何 GUI，才恢復 |
| 緩衝期間又開啟 GUI | 重新計算完整的 0.5 秒 |

關閉智慧暫停後，功能可在 GUI 開啟時繼續執行，請自行留意。

自動化啟用時會鎖定手動移動與滑鼠視角；Farming 只套用作物流程要求的操作。Mining 也會鎖定手動左鍵攻擊／挖掘，但不阻擋 GUI 點擊或模組自己的自動挖礦。手動關閉自動化後恢復一般操作；異常返回期間則依返回流程管理鎖定。

## Mining 挖礦輔助

### 選擇目標方塊

1. 按 `'`，點選 Mining 區的「選擇目標方塊」。
2. 輸入要選取的種類數量，範圍 **1～5**；留空代表 1 種。
3. 確認後會清除舊目標清單，進入右鍵選取模式。
4. 將準星對準方塊並按右鍵，直到選滿指定數量。
5. 回到設定頁面，用 `↑`、`↓` 調整優先順序，或用 `×` 刪除目標。

重複的方塊種類只計算一次；Bedrock 不能設為目標。目標清單會顯示方塊圖示與 ID。

### 目標選擇規則

- 只掃描玩家周圍、範圍內可讀取且符合條件的方塊。
- **先挖完當前這顆，再重新從最高優先級選下一顆**，不會因高順位方塊出現就中途切換。
- 清單順位優先於距離；只有同順位目標，才比較哪一顆較近。
- 例如正在挖二號時出現一號，挖完二號並經過挖礦延遲後，下一顆會優先找可挖的一號。
- 自動轉向固定啟用；只有準星確認指向鎖定的那一顆，才進行挖掘。
- 暫時跳過失敗或無法對準的目標，避免在相同方塊間反覆切換。
- 已挖位置的耗盡紀錄不會擋住較高順位的新方塊；較低順位的耗盡外觀仍會等待原方塊重生。

### 可調整項目

| 設定 | 範圍 |
| --- | --- |
| 偵測半徑 | 1～8 |
| 轉動速度 | 1～40 |
| 挖礦延遲 | 1～20 client ticks |

Mining 開啟期間，即使正在智慧暫停，也會阻擋手動左鍵點擊／長按造成的攻擊、揮拳或挖掘。自動挖礦與介面按鈕仍可正常運作。

## Farming 農耕輔助

### 啟動流程

1. 站在對應農場的起始位置，將準星對準作物。
2. 使用正確工具，按 `Shift + F` 啟動。
3. 模組先辨識準星作物，不會為了辨識先把作物打掉；辨識後會儲存作物類型並套用固定視角及移動流程。
4. 若持續無法辨識作物，會關閉 Farming，請重新對準後再啟動。

Pumpkin 與 `carved_pumpkin` 都能辨識為南瓜；Pumpkin／Melon 雖共用移動流程，工具驗證仍分開處理。紅、棕 Mushroom 也會區分起始方向。

農耕流程由舊 `bobscript` Forge mod 移植，仍須依 Garden 布局實際測試。

### 農場標記

換列、起點、終點偵測固定啟用，不能在設定介面修改。

| 用途 | 方塊 | ID |
| --- | --- | --- |
| 換列 | 黃色染色玻璃 | `minecraft:yellow_stained_glass` |
| 起點 | 淺藍色染色玻璃 | `minecraft:light_blue_stained_glass` |
| 終點 | 粉紅色染色玻璃 | `minecraft:pink_stained_glass` |

不同作物對標記的反應與回程方式不同，不能只換作物卻沿用不相容的布局。

### 支援作物與工具偵測

| 作物 | 工具名稱 |
| --- | --- |
| 小麥 Wheat | `Euclid's Wheat Sickle` |
| 馬鈴薯 Potato | `Pythagorean Potato Shovel` |
| 胡蘿蔔 Carrot | `Gauss Carrot Shovel` |
| 地獄疙瘩 Nether Wart | `Newton Nether Wart Cutter` |
| 甘蔗 Sugar Cane | `Turing Sugar Cane Cutter` |
| 仙人掌 Cactus | `Cactus Knife` |
| 可可豆 Cocoa Bean | `Cocoa Chopper` |
| 蘑菇 Mushroom | `Fungi Cutter` |
| 南瓜 Pumpkin | `Pumpkin Dicer` |
| 西瓜 Melon | `Melon Dicer` |
| Sunflower／Moonflower | `Eclipse Sickle` |
| Wild Rose | `Wild Rose Cutter` |

開啟「工具偵測」後：

- 啟動時工具不符，會直接拒絕開始。
- 啟動後每個 tick 都檢查工具，GUI 開啟時也不例外。
- 換錯工具或空手時，無限期暫停移動、攻擊與作物流程，但不關閉 Farming。
- 換回正確工具後，從原狀態繼續；不會因等待過久自動忽略工具檢查。
- 每次從正確變成錯誤時，只警告一次，不會持續洗版。
- 名稱比對接受 `Mk. I`、`Mk. II`、`Mk. III` 等後綴。
- 關閉工具偵測可解除這項暫停；異常返回與智慧暫停仍各自生效。

<details>
<summary>各作物的移動與換列規則</summary>

### Wheat／Potato／Carrot／Nether Wart

- Wheat／Potato 使用 yaw `90`，Carrot／Nether Wart 使用 yaw `-90`；pitch 均為 `5.5`。
- 開始先前進一格，不攻擊，再進入橫向收割。
- 換列時向原方向多移動一格、跳躍三秒，第 4 秒同時按住跳躍與反向橫移；這段定時換列持續攻擊。
- 尚未遇到終點時忽略起點標記。遇到終點後停止攻擊，沿當前橫向方向至少走一格並找到起點，等待一秒後重啟循環。

### Sugar Cane

- 固定 yaw `45`、pitch `0`。
- 向左收割到換列標記 → 前進五格 → 右移一格 → 向後收割到下一標記 → 右移七格，重複循環。
- 只在向後收割時檢查終點；回到起點及額外後退兩格的過程也維持攻擊。

### Cocoa Bean

- 固定 yaw `0`、pitch `-45`，開始先左移一格，不攻擊。
- 向前收割 → 右移一格 → 向後收割 → 右移兩格 → 前進一格 → 左移一格，重複循環；初始調整後的正常循環維持攻擊。
- 只在向後收割時檢查終點。觸發後停止攻擊，忽略換列標記並向左找起點，再左移一格，恢復向前收割。

### Cactus

- 固定 yaw `90`、pitch `0`，先前進一格，不攻擊，再交替左右收割。
- 換列時停止攻擊，沿當前橫向方向多走一格、前進兩格、原地等待一秒，再反向收割。
- 只在向右收割時檢查終點；觸發後停止攻擊，向後找起點，再左移一格、前進一格，重新向左收割。

### Pumpkin & Melon

- 介面名稱為 `Pumpkim & Melon`；固定 yaw `90`、pitch `-57`，採雙標記折返流程。
- 遇到第一個標記後，沿當前橫向方向多走一格，再向前找第二個標記；收割與正常換列期間維持攻擊。
- 終點回程停止攻擊並忽略換列標記；完成回程後，後退一格、左移一格、前進一格，再重新向左收割。
- 此流程的一格步進以實際位移計算，不以固定 tick 數代替；連續十個 tick 沒有移動時，才使用受阻處理結束該步。

### Sunflower／Moonflower／Wild Rose

- 固定 yaw `-45`、pitch `0`，先後退一格，再向右收割。
- 換列時前進一秒，最後半秒同時左移；向後收割後，再左移一秒，最後半秒同時前進，重複循環。
- 全程維持攻擊；只在向後收割時檢查終點。觸發後保持後退與攻擊，忽略換列標記直到起點，再後退一格，重新向右收割。

### Mushroom

依紅、棕 Mushroom 選擇起始方向，使用自己的往返流程。沒有自動偵測卡住後跳躍的行為。

</details>

### 異常狀況處理

「異常狀況處理」涵蓋 Island、Hub、Lobby、Limbo，以及 `You fell into the void` 訊息。

前四種返回流程，必須同時符合：

1. Farming 與異常狀況處理正在啟用。
2. 啟用期間記錄到 Fabric 的 client-world-load 事件。
3. 確認目前位於對應地點。

僅地點符合、僅發生 world load，或只比較 dimension ID，都不會直接觸發返回；不要求先辨識到 Garden。事件與地點可先後更新，但返回開始後的 world load 不會再次啟動另一輪。

| 所在地點 | 返回流程 |
| --- | --- |
| Island／Hub | 確認所在地 → 等待三秒 → `/warp garden` |
| Lobby | 等待三秒 → `/play sb` → 確認進入 SkyBlock → 等待三秒 → `/warp garden` |
| Limbo | 等待三秒 → `/lobby` → 確認進入 Lobby → 等待三秒 → `/play sb` → 確認進入 SkyBlock → 等待三秒 → `/warp garden` |
| 掉入虛空訊息 | 只關閉 Farming，不執行返回指令；不需要 world-load 事件 |

每段等待都是在確認指令所需地點後，重新計算 **60 client ticks，約三秒**。載入中、地點未知、玩家／世界資料缺失或到達錯誤地點時，不會消耗待執行步驟，並重置該段等待。

返回開始時會停止操作並清除錯誤工具等待，但不關閉已儲存的工具偵測設定。確認抵達 Garden 後，立即取消剩餘指令並釋放移動、視角鎖定；不會自動 `/home` 或重啟 Farming。

地點判斷優先使用可選的 SkyBlock API `LocationAPI`；沒有硬性依賴，無法使用時會採原生 sidebar／Lobby／Limbo 判斷。Sidebar 會處理 team 前後綴、顯示名稱與文字格式；`Private Island`、`Hub` 等訊號有各自判斷，不能只靠 `Your Island` 一詞認定地點。

> 這裡的返回指令等待，與下方可自行設定的「螢幕通知秒數」是兩件事。

## Other 與通知

### 各功能獨立設定

沒有共用的「通知設定」按鈕。請直接點選各功能：

| 按鈕 | 設定內容 |
| --- | --- |
| Farming 區的「Visitor 通知」 | 外層總開關；內層四行：「出現多少 Visitor 要通知：」數量（1～5）、三種通知管道 |
| Farming 區的「Pest 通知」 | 外層總開關；內層四行：「出現多少 Pest 要通知：」數量（1～8）、三種通知管道 |
| Other 區的「Jerry 偵測」 | 第一行為「出現 Jerry 是否停下動作」，接著三種通知管道；**沒有 Jerry 總開關** |

每個功能都有下列三個開關，可以全開、全關或任意搭配，互不影響：

| 通知管道 | 行為 |
| --- | --- |
| Discord 訊息通知 | 透過共用 Webhook 傳送訊息 |
| 遊戲內聊天室通知 | 顯示本機 `[Kuan HSBA]` 提示，不會送到伺服器聊天頻道 |
| 遊戲內螢幕通知 | 在畫面中央顯示放大的黃色文字 |

Visitor／Pest 的總開關只放在 Farming 主設定頁面，不在彈出視窗重複顯示：

- 關閉時點選外層按鈕：輸入數量門檻，確認後啟用並開啟四行設定視窗；取消／Esc 不啟用。
- 開啟時點選外層按鈕：直接關閉該功能，保留原門檻、通知管道與秒數。
- 設定視窗第一行為「出現多少 Visitor 要通知：數量」或「出現多少 Pest 要通知：數量」，接著是 Discord 訊息通知、遊戲內聊天室通知、遊戲內螢幕通知，另有「完成」按鈕。點選第一行可修改數量，不會改變總開關。
- 要重新開啟設定，可關閉再開啟外層按鈕。設定變更只重置該功能需要重置的通知紀錄，不會影響另一種通知。

關閉「遊戲內聊天室通知」只會停止模組額外的提示，不會隱藏原本的伺服器訊息。

### 共用 Discord Webhook

- **三個功能共用同一個 Webhook URL，但 Discord 開關各自獨立。**
- 開啟任一功能的 Discord 通知時，會跳出輸入視窗，預填最新的共用 URL。
- 確認後更新共用 URL，並只開啟當前功能的 Discord 開關；其他功能的開關不變，但後續會使用新 URL。
- 取消／Esc 不變更 URL，也不開啟當前功能的 Discord 通知。
- 關閉 Discord 通知不會刪除 URL；要修改時，可關閉再開啟任一功能的 Discord 通知。
- 主設定頁面不再提供左下角的 Webhook 輸入欄。
- URL 需為有效的絕對 HTTP(S) 網址。請將 Webhook 視為憑證，避免貼進公開截圖、Git 或其他公開內容。

Webhook 請求非同步送出，逾時為十秒；失敗或 HTTP 狀態碼只記錄不含 token 的資訊，不會對已通知的事件自動重試。

### 自訂螢幕通知時間

1. 在某個功能內，開啟「遊戲內螢幕通知」。
2. 在彈出視窗輸入大於 0 的秒數，可含**最多一個小數點**，例如 `0.5`、`1.25`、`3`。
3. 確認後啟用，開關上會顯示設定秒數；取消／Esc 則保持關閉且不修改原時間。
4. 想改時間時，先關閉再開啟，視窗會預填上次儲存的值。

三個功能的秒數獨立保存；舊設定缺少秒數或內容無效時，預設為三秒。

- 不接受空白、零、負數、多個小數點、科學記號或超出計時範圍的值。
- 小數會精確換算成奈秒，不透過浮點數近似；支援的時間範圍為 `0.000000001`～`2147483647` 秒，需可精確表示為整數奈秒。
- 多則通知會排隊，各自使用送入佇列時的秒數，不會互相蓋掉或修改伺服器 Title。
- 文字最多放大至一般字體的四倍，並依畫面寬度縮放；F1 隱藏 HUD 時不繪製。
- 關閉某功能的螢幕通知／總開關，只移除該功能的螢幕提示；離開或切換世界則清除全部待顯示提示。

### Visitor 與 Pest 通知規則

| 功能 | 資料來源 | 門檻範圍 |
| --- | --- | --- |
| Visitor | Tab 的 `Visitors: (number)` | 1～5 |
| Pest | Tab 的 `Pests` → `Alive` | 1～8 |

直接讀取伺服器提供的快取 Tab 資料，約每 20 client ticks 檢查一次；不必按住 Tab，也不必開啟 Farming／Mining。Pest 通知不使用出生聊天訊息或 sidebar Pest 數。

通知方式是：**每個達到門檻的實際觀察數量，各通知一次**。

- Visitor 設為 `3`：觀察到 `3`、`4`、`5` 位時，各通知一次。
- Pest 設為 `4`：觀察到 `4`、`5`、`6`、`7`、`8` 隻時，各通知一次；更高的有效數量同樣依門檻處理。
- 維持相同數量，或 `3 → 4 → 3` 這種回到已通知數量的情況，不會重複通知。
- 有效數量降到門檻以下、切換該通知總開關，或實際更改門檻時，會重置該功能的紀錄。
- 從 `2` 直接跳到 `5`，只通知實際讀到的 `5`，不補發未觀察到的 `3`、`4`。
- Tab／世界資料暫時缺失不視為零，不會清除紀錄；Visitor 超出 0～5 的數量也不會重置紀錄。
- 某功能沒有可用管道時，不會消耗該功能的通知數量紀錄；另一個功能仍能獨立通知。
- 更換通知管道不會重播已通知過的數量，也不會自動清除 Pest 或接待 Visitor。

### Jerry 偵測

點選 Other 的「Jerry 偵測」即可設定，不需要先開啟任何 Jerry 總開關。

模組會先去除聊天室訊息的顏色碼，再比對下列 **7 種訊息**。忽略大小寫、額外空白、前後附加文字；`[type]` 可為任意顏色名稱，不使用顏色白名單。

```text
You found a [type] Jerry!
You discovered a [type] Jerry!
Some [type] Jerry was hiding, but you found it!
wild [type] Jerry spawned!
[type] Jerry appeared!
There is a [type] Jerry!
You located a hidden [type] Jerry!
```

讀取的是聊天／系統訊息，不包含 action bar，也不掃描實體。單純提到 Jerry、其他物品、`Jerryfish` 或不完整的躲藏／生成訊息，不會當作有效提示。

- 偵測後，依 Jerry 自己的三個通知開關送出通知；每則符合訊息各算一次，不額外設冷卻。
- 「出現 Jerry 是否停下動作」獨立運作：啟用後立即關閉 Mining／Farming、釋放攻擊與移動／視角鎖定，並取消待執行的 Farming 返回指令，不會自動恢復。
- 即使全部通知管道關閉或 Webhook 無效，停下動作仍可生效。
- 若原本沒有執行自動化，通知不會釋放玩家手動操作的按鍵。

## 設定與紀錄

設定會儲存在所用 Minecraft instance 的：

```text
minecraft/config/kuan-hypixel-skyblock-assistant.properties
```

| 資料 | 行為 |
| --- | --- |
| Mining／Farming 執行狀態 | 不會在重新啟動後自動恢復 |
| 方塊清單、農耕選項、門檻、各功能管道與秒數 | 個別保存 |
| Discord Webhook | 全功能共用；請勿公開設定檔內容 |
| 舊版共用通知設定 | 轉移為個別功能設定；舊 Jerry 總開關關閉時，不會意外啟用通知或停下動作 |
| 首次安裝 | 通知管道與 Jerry 停下動作預設關閉 |

另保留 `farmingAutoSetHome` 設定，可在啟動與部分換列流程送出 `/sethome`；目前主介面未提供這項開關。

診斷資訊寫入 instance 的 `logs/latest.log`，包含地點來源變化、返回指令階段及 Webhook 的非機密失敗資訊。一般目標可用狀態、農耕換列與成功工具檢查等例行訊息盡量保持安靜。

## 建置與測試

本專案**不使用 Gradle**，透過腳本直接使用 PrismLauncher 已安裝的 Minecraft、Fabric Loader、Fabric API 與 Java runtime。

### 環境需求

- Python 3：執行部分回歸測試。
- PrismLauncher 的 Java runtime，需包含 `java`、`javac`、`jar`。
- 建置腳本目前指向 Minecraft `26.2`、Fabric Loader `0.19.5`、Fabric API `0.161.0+26.2`。
- 腳本含開發環境的 PrismLauncher 路徑；若環境或套件版本不同，請先檢查 [scripts/build.sh](scripts/build.sh) 與 [scripts/test.sh](scripts/test.sh)。

### 執行指令

在專案根目錄執行：

```bash
# 執行回歸測試
./scripts/test.sh

# 編譯並封裝 jar
./scripts/build.sh
```

建置產物：

```text
build/libs/kuan-hypixel-skyblock-assistant-1.0.123.jar
```

測試涵蓋工具偵測、智慧暫停、地點與異常返回、Mining 順位／左鍵鎖定、Tab 解析與去重、七種 Jerry 訊息、個別通知管道、共用 Webhook、秒數視窗及小數時間邊界。

測試使用遊戲與網路邊界的替身，不會真的呼叫 Webhook；通過回歸測試不代表已完成 Hypixel 遊戲內實測。

## 開發與維護

修改程式前，先閱讀入口：

[src/main/java/dev/kuan/skyblockassistant/KuanSkyBlockAssistantClient.java](src/main/java/dev/kuan/skyblockassistant/KuanSkyBlockAssistantClient.java)

| 檔案 | 用途 |
| --- | --- |
| `KuanSkyBlockAssistantClient.java` | 啟動、遊戲事件、Mining／Farming、設定與通知整合 |
| `NotificationSettingsScreen.java` | 各功能通知設定介面 |
| `NotificationChannels.java`／`NotificationType.java` | 個別管道與顯示時間資料 |
| `ScreenDurationScreen.java`／`ScreenNotificationQueue.java` | 秒數輸入與螢幕通知佇列 |
| `GardenTabReader.java`／`GardenTabNotifications.java` | Tab 資料讀取、數量解析與通知紀錄 |
| `JerryMessageMatcher.java` | 七種 Jerry 訊息比對 |
| `GuiPauseGuard.java`／`FarmingToolGuard.java` | GUI 與工具暫停狀態 |
| `SkyBlockLocationBridge.java` | 可選的 SkyBlock API 地點橋接 |
| `mixin/` | 手動視角與攻擊輸入攔截 |

### 每次更新的維護流程

1. 同步提升 `src/main/resources/fabric.mod.json` 與 `scripts/build.sh` 的版本號。
2. 更新 README 與 [CHANGELOG.md](CHANGELOG.md)。
3. 執行測試，建置新 jar。
4. 安裝到 PrismLauncher 的 `Hypixel SkyBlock` instance，只保留最新版助手 jar，保留其他 mod。
5. 完成 Git commit、版本 tag，並將 commit／tag 推送到 GitHub。

功能歷史與修正細節請見 [CHANGELOG.md](CHANGELOG.md)。
