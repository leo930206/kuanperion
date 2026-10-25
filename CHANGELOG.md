# Changelog

## 2.1.0 — 開發中（未發佈）
- 快速點擊攻擊／使用物品也會先交還控制權給玩家，不等下一次按鍵採樣；不吞掉玩家操作。
- 整理共用合法飛行流程，保留既有 Pest 行為並隔離不同模組的控制權，供 Greenhouse 使用。
- Farming 新增 Greenhouse 模塊：明確設定地塊、作物及工具後，分批巡查／收割／返回原位；自動執行預設關閉。每次收割需新伺服器回條及目標變化，不以本機預測判定成功；尚未連服驗收。
- 補上伺服器游標回條及過期／選單切換驗證，供後續補餌與物品轉移使用；不自動移動游標物品。
- Pest 新增可選的已載入滿載陷阱清理（預設關閉），確認放出後接續捕捉及返回；不猜按鈕位置，手動接管不誤關選單。
- Pest 可明確設定地塊傳送上限（預設停用），只前往 Tab 已確認地塊；抵達與返回都需伺服器位置回條，保留原始續耕位置，未清完不假報成功。

- 新自動化位置守門不再信任切世界後殘留的 Garden API 狀態；要求目前原生側欄佐證。加入有界伺服器絕對位置回條供後續跨地塊導航驗證，尚未加入傳送行為。

- Pest 可另行啟用範圍內四點搜尋；找到目標立即切回吸捕，掃描完仍有 Pest 則返回原位並停用，不誤報清空。搜尋預設關閉；舊設定不會默默增加搜尋權限。

- NPC Auto Sell 補強完成／失敗清理：關閉原選單失敗不得恢復 Farming，未知同號／錯誤形狀 GUI 不關閉，native 例外不殘留 hold；短暫失焦延後動作。

- Pest 新增已載入目標的核心工作流程：依門檻觸發、合法飛行權限、有界避障尋路／轉向、Vacuum 使用、數量與目標離場雙重確認、返回原位後才釋放 Farming；手動接管／切世界／失焦／異常會停止。
- Pest 與舊通知合併入口但啟用開關獨立；自動除蟲預設關閉、設定保存於外部 automation.json。尚未完成未載入地塊搜尋、陷阱／兌換／噴灑等擴充，也未驗收實際伺服器捕捉。

- 合書新增可測試的宣告式選單協定：輸入、預覽、零費用、結果及取消取回均須新的伺服器回條；尚無原生鐵砧 driver／設定入口，不當作可用功能。

- Trade 新增指定 Garden 位置的 Junk 工作流程：精確白名單與保護、46-slot 原生背包驗證、單疊 server-only 回條與上限、世界重新授權、失敗停用；不自行導航，不把 offline 測試當作 live 驗收。

- 原 `/home` 快捷鍵名稱、實際發送與聊天室回報統一改為 `/warp garden`；預設 Shift+G，保留內部鍵 ID 與既有自訂／清空綁定。

- 新增隔離的自動化設定、安全執行權限、世界／原有任務守門與有界 Stash 工作流程。
- 補上選單 identity／revision 守門、伺服器物品回條、精確白名單／預算，以及不重開 Farming 的暫停接續 token；實際交易工作流程仍在開發。
- Trade 側欄改為與 Farming／Mining 相同 2px 圓角描邊的交換箭頭圖示。
- Trade 側欄位於 Mining 和 Other 之間，目前暴露已接線的 Auto Sell（NPC）、Junk 與 Stash；其餘完成接線後才提供入口。
- 不更動原有通知、Farming／Mining 操作或既有設定位置；新自動化預設關閉。
- 完整 Pest／Visitor／Greenhouse／Composter／其餘 Trade／Supercraft 尚未完成；不標記發佈、不安裝開發 JAR，正式遊戲使用獨立穩定版 2.0.17。

## 2.0.17 — 2026-10-24

- 原 `/home` 快捷鍵名稱、實際指令與聊天室回報統一為 `/warp garden`，預設改為 Shift+G；保留既有自訂／清空綁定，重設可套用新預設。
- 啟用工具偵測時，Farming 開始辨識作物後若手持工具不符，先搜尋快捷列 9 格並切換到對應工具；找不到才通知工具錯誤並停止。執行中手動換錯工具仍維持原有暫停機制。
- 不包含 2.1.0 未完成的大升級功能，不更動設定檔位置或內容。

## 2.0.16 — 2026-10-24

- 聊天室一般文字改為 #00D8F5，功能名稱、狀態、警告、方塊／作物／地點等重點統一 #0088D1。
- 前綴及所有文字片段明確停用粗體（含繼承的粗體），原前綴漸層與灰色括號保留。
- 通知文字、觸發條件、保存設定、介面、Discord 與螢幕通知均不更動。

## 2.0.15 — 2026-10-24

- Mining 開始選取不再清空原清單，新種類逐一插在最前；上限改為 10 種並完整保存／載入。
- 新增數量動態限制為剩餘名額，選滿後停用輸入及開始按鈕；重複／無效種類不計數，重啟選取保留已新增內容。
- Jerry 自動 sethome 成功的聊天室回報依開關固定為合併一行，或與手動快捷鍵完全相同的一行；Discord／螢幕通知不變，失敗時不誤報成功。

## 2.0.14 — 2026-10-24

- 聊天室 `/kuanperion` 不分大小寫，在本機開啟設定，不發送到伺服器；延後至聊天視窗關閉後顯示。
- 新增 `/barn` 快捷鍵（預設 Shift+B）；`/home`、`/sethome` 預設改 Shift+H、Shift+S。既有自訂／清空綁定保留，按重設可套用新預設。
- `/barn` 沿用一次按下送一次、焦點／選單／世界檢查、聊天室發送回報、清空／重設與自動保存。
- 聊天前綴的藍紅加深、縮窄中央白色範圍；灰色括號與不加粗維持不變。

## 2.0.13 — 2026-10-24

- 聊天前綴括號改灰色，名稱漸層提高飽和度；維持不加粗。
- Farming／Mining 各自新增執行中暫時靜音開關，不覆寫原遊戲音量偏好；關閉動作／斷線恢復原音量。
- `/sethome`、`/home` 快捷鍵新增發送回報；Jerry 自動 `/sethome` 與已開啟的 Jerry 聊天室通知合併同一行，否則獨立回報。
- 移除手動設定檔頁；各項設定依舊自動保存在遊戲資料夾，換 JAR 沿用，舊快照檔不刪除。
- 所有快捷鍵新增「清空」按鈕，保存 Not Bound 並清除修飾鍵，重設仍恢復預設。
- Farming 停滯改為獨立開關與可設定秒數，到時先停止，再送 DC 通知；缺少 Webhook 仍停止，正常等待不誤觸。
- 配色收藏最多顯示單行色塊，依寬度隱藏放不下的預覽；完整配色不刪除，儲存／載入仍包含全部顏色。
- 既有說明文案先不修改，大型功能移植仍暫停。

## 2.0.12 — 2026-10-23

- 展開側欄依實際動畫寬度獨佔滑鼠互動，不再懸停穿透下方功能卡；選色器／下拉框也遮蔽下層 hover。
- Mining 目標方塊改為置中的單行「順位 N: 方塊名稱」，保留圖示、排序與刪除。
- 刪除介面頁最底部 Kuanperion／Minecraft 重複資訊列，不影響側欄版本。
- 內建配色移至顏色頁的配色收藏與選單顏色之間，改為六款英文命名的白底柔和配色，附色彩預覽與響應式換行；既有使用者主題與收藏不重設。
- 其餘介紹文案保留，等待使用者逐項確認；大型移植持續暫緩。

## 2.0.11 — 2026-10-23

- 側欄恢復原有文字／圖示 hover，移除額外外框；功能入口移除浮起／陰影與第二層描邊，改成單圈天空藍 hover 框。
- 配色收藏改為空清單自行新增，保留舊配色；以依選單順序排列的色塊取代編號／狀態文字，空間不足換行，右側提供儲存／刪除。
- 維持最多五組與載入／覆寫確認，刪除保留本機備份且不改目前主題；空位置不再顯示。
- 新增預設未綁定的 /home 組合快捷鍵；沿用 /sethome 的一次按下、焦點與選單保護，既有設定不重設。

## 2.0.10 — 2026-10-23

- 僅 UI／既有流程調整；暫緩大型移植。面板略縮、resize 永遠置中，Search 不重播轉場並顯示空白聚焦游標。
- 全域移除懸停文字框，改為非位移的描邊／亮度回饋；設定列左邊距統一，啟用卡片不加深底色。
- Setting 改為「設定檔、介面、顏色、快捷鍵」，移除顏色／設定檔第二排分類；配色收藏置於選單顏色前。
- 時間數字框寬 66 並置中；Webhook 目標寬 390，說明簡化；Pest／Visitor 各自顯示門檻說明。
- Jerry 四色同列、各自開關；光柱支援 0 秒；新增預設關閉的 Jerry 自動 /sethome。
- 新增預設未綁定的 /sethome 快捷鍵；異常回到 Garden 後發送原地點 Discord 通知；聊天前綴三色漸層且全訊息不粗體。
- 專案資料夾更名為 kuanperion，remote 更新至 leo930206/kuanperion。保留既有設定與授權來源檔。

## 2.0.9 — 2026-10-23

- Setting 齒輪與底部版本列距離縮短 8 logical px；保留原底部位置。
- 收合時顯示實際版本號，展開時顯示 `Mod Version 版本號`，無冒號；兩種狀態與動畫過程均依當前側欄寬度置中，完整文字放不下時保留短版。
- 版本文字不再隨收合消失；收合／展開均可點擊開啟「關於」。不修改或重設設定。

## 2.0.8 — 2026-10-23

- 保留已存配色與輸入框高度，不再因舊 schema 自動換成新版預設；補上更新與設定備份說明、設定載入保留測試。

- 移除側欄左下 K 圖示與 Setting 下方的分隔線；只保留單行模組版本，不顯示 Minecraft 版本，貼近側欄底部，保留 Setting 上方的區域分隔。
- 「關於」改由展開側欄的版本文字開啟；點擊範圍與文字列一致，收合後不殘留隱形 K 按鈕。
- 保留左上頭像／名稱、三組功能與 Setting 分頁；不變更功能、配色或既有設定。新增單行文字／底部位置／點擊邊界與實際渲染寬度驗收。

## 2.0.7 — 2026-10-23

- 側欄改為 Farming（Farming、Pest、Visitor）、Mining（Mining）、Other（Jerry、Discord Webhook）三個功能入口；搜尋跳轉／詳情返回保持正確分組，不改功能行為。
- 顏色、設定檔、快捷鍵、介面設定整合至單一齒輪「Setting」，內部頁籤保留原配色收藏、主題／功能設定檔與所有調整項目。
- 左上大頭貼與名稱保留；左下移除重複名稱，改為兩行 `Mod Version: 實際版本`、`Minecraft 26.2`。點左下大 K 開啟獨立「關於」，不再重複導向介面設定。
- 補上側欄路由／版本資訊與實際滑鼠點擊、搜尋跨組、Setting 子頁、關於及展開／收合驗收。

## 2.0.6 — 2026-10-23

- Farming／Mining 的智慧暫停排除 Kuanperion 自身設定選單；開啟／關閉 `'` 選單不新增暫停或 0.5 秒緩衝。其他 GUI 仍沿用原有保護及關閉緩衝。
- Farming 在自身選單中以原版 held-block attack 延續採收；狹義 Mixin 存取26.2的 GUI missTime blocker，僅在 active Farming／自身選單時清除，關閉時清除一次殘留。保留原版物品使用保護，不開放 GUI 操作穿透；停用／離開到其他 GUI 會收尾舊採收，不干擾新 Mining 動作。
- 移動、轉向、Mining 目標／冷卻及通知照常；Farming 停滯監控不再排除自身選單。死亡、真正暫停、載入 overlay、錯工具、計畫等待、異常回復、Jerry 停止等保護保留。
- 目標方塊右鍵選取只在世界畫面處理；GUI 內點擊或按住右鍵關閉 GUI 不會誤選背後方塊。
- 更新智慧暫停說明；側欄版本資訊改讀取模組 metadata，避免舊版字串。補上自身／其他 GUI 轉換、原版 miss blocker／採收續行與關閉清理、停滯監控及 Mixin 套用回歸。

## 2.0.5 — 2026-10-23

- 左上角 K 改為現有模組 icon（Minecraft 大頭貼），沿用相同 PNG 原色，不染成主題色、不連網抓 skin；底部 K 保持不變。
- 頭像採 nearest 像素縮放，NanoVG 紋理只載入一次並隨渲染器釋放。
- 延續 2.0.4 左側說明換行／控制項預留欄，補上說明裁切防護及 Mining 目標數量列的多寬度回歸，避免文字侵入輸入框與按鈕。
- 補上 Fabric icon 資源一致性／PNG 驗證、實際頭像載入與紋理重用測試；不更動功能設定與配色收藏。

## 2.0.4 — 2026-10-23

- 單行文字與數字輸入框預設由 16 加高至 22 logical px，內文字由 10 放大至 12 px；輸入、數字滑桿與目標選擇操作以完整卡片高度垂直置中，繪製／點擊／游標同步；說明維持原間距，僅在左側文字欄換行，不與置中控制項重疊。
- 舊 schema 的預設高度 16 會備份後升級；其他自訂高度保留，新 schema 明確指定的 16 亦保留。仍可自行調整輸入框高度。
- 「顏色 → 配色收藏」新增五個固定儲存槽；儲存目前配色、點擊已存列立即載入，覆寫需 5 秒內再次確認。
- 只保存選單色票、彩虹選取與漸層開關／範圍／濃度，不含遊戲功能、Webhook、快捷鍵、HUD 或介面比例／動畫／版面尺寸。重啟後保留，與原本一般主題設定檔分開。
- 配色槽使用驗證／大小限制／暫存原子替換及覆寫前備份，損毀與寫入失敗不會顯示成功；新增儲存槽及實際滑鼠操作回歸測試。

## 2.0.3 — 2026-10-23

- 一般勾選列與螢幕通知合併列的勾選框，均以完整卡片高度垂直置中，不因說明有無／換行而偏上；只排除列間留白。
- 點擊區同步涵蓋完整卡片高度；時間輸入與開關仍獨立，保留原文字／說明位置、配色及快捷鍵。
- 補上無說明／單行／多行說明置中幾何與勾選框下緣點擊回歸。

## 2.0.2 — 2026-10-23

- 主色改為淺天空藍 `#58B9EA`；標題／標籤／數值改為淺天空藍 `#50AFE1`，不再深藍；說明用中性灰。
- 藍粉漸層預設範圍縮至 0.48、濃度降至 0.60，增加白色留白；「顏色」新增範圍／濃度設定，仍可自訂所有色票。
- 通知門檻、數字與單行時間框縮為 16 logical px 高、較小字，繪製／點擊／游標同步；介面設定可調高度。
- 說明統一靠近標題，預設間隔 6 logical px（較原群組 4 px 稍大），設定列／群組／模組／搜尋／hover 共用偏好；螢幕通知也顯示列內說明。
- 快捷鍵預設仍為 Shift + M／Shift + F；A～Z 固定顯示英文字母，避免注音鍵名。截圖測試在捕捉多鍵後恢復預設再拍照，不再混入測試值；不重設使用者自訂綁定。
- 僅對完整未改色的 2.0.1 舊預設做備份後升級，保留自訂、漸層開關、比例、動畫、HUD 與快捷鍵。補上配色／升級／緊湊尺寸／統一間距／IME 顯示回歸測試。

## 2.0.1 — 2026-10-23

- 快捷鍵改用可保存的單鍵／Ctrl／Shift／Alt／Command 多修飾鍵組合，移除強制 Shift 與硬編碼 M／F，完整顯示／重設／取消／清除；舊主鍵與預設習慣保留。
- 避免修飾鍵單獨被誤當主鍵，拒絕重複完整組合；遊戲失焦／GUI 輸入期間不觸發，長按與關閉畫面不重複啟停。
- 螢幕通知時間與開關合併同列；留空顯示「顯示時間」、預設 3 秒，保存空白偏好，兩個控制不互相誤觸。
- Farming／Mining 與通知設定新增列內用途說明，修正中文長說明的全形換行高度。
- 目標數量與開始選擇合併同列，數量會保存；只顯示實際選取項目，加入資源包方塊材質圖示、名稱及列內排序／刪除，移除空白五順位。未進世界不清除目標或進入選取。

- 預設主題改為「晴空藍粉」：白色主面板、天空藍重點色、左下淡藍／右上淡粉柔和漸層，深藍灰文字。
- 新增漸層開關與兩端顏色，支援本機主題保存／匯出／重設；保留原有深色 presets。
- 原 2.0.0 完整未改色預設安全遷移並先備份；自訂色／彩虹、比例、動畫及 HUD 偏好保留。
- 修正淺色介面的白色細邊／滑桿把手可見性，陰影隨明暗主題調整。
- 新增配色對比、遷移保護與實際客戶端主題回歸驗證；未修改 Mining／Farming 行為。

## 2.0.0 — 2026-10-23

- 全面更名 Kuanperion：mod ID、入口／package、資源與 mixin 名稱、快捷鍵分類、遊戲／Discord 通知前綴、JAR 均更新。
- 保留相容遷移：首次新設定不存在時複製舊設定，不覆寫／刪原檔；新快捷鍵沒有保存時沿用舊綁定。
- 移植設定介面核心／元件／動畫／配色，適配 Minecraft 26.2 deferred GUI、獨立 stencil framebuffer 與 OpenGL 狀態復原；加入 NanoVG 3.4.1 必要平台依賴。
- 介面只綁現有 Mining／Farming／通知／Jerry／Webhook；不導入上游玩法、帳號、遙測、IRC、遠端控制或更新器。
- 新增搜尋、卡片／分類詳情、色彩選擇、UI／文字縮放、動畫速度、主題／本機設定檔與快捷鍵頁；runtime 開關與偏好編輯分開。
- 修復移植過程的反編譯還原問題、中文字長詞換行、macOS Command 編輯、側欄長品牌名稱及非有限滑桿輸入。
- Profiles 排除 Webhook／光柱世界座標／runtime 啟停；載入不自動啟用功能、不覆寫本機 Webhook。
- 保留必要 GPL、OFL、LWJGL／NanoVG 授權與來源；UI 顯示品牌僅 Kuanperion。
- 新增遷移、設定綁定、文字、動畫／布局測試；沿用所有既有 gameplay 回歸測試。

## [1.0.132] - 2026-10-22

### 新增

- Jerry 設定最下方新增「光柱最久存在時間」，點擊輸入正整數或小數秒數，預設 300 秒，不設秒數上限。
- 光柱從建立時起計時，到期自動消失；仍可在 10 格內手動成功啟動 Farming 提前清除，Mining 不建立或手動清除光柱。
- 保存光柱建立時間，切換世界、開啟選單或離線不會重設期限；新增到期邊界、超大秒數、重啟保存與設定視窗回歸測試。

## [1.0.131] - 2026-10-22

### 新增

- Farming 因選定顏色的 Jerry 而停止時，在停止位置留下淺綠色光柱，方便遠處辨識。
- 只有在光柱半徑 10 格內手動成功啟動 Farming 才會清除；單純走近、開選單或啟動 Mining 不會清除。
- Mining 不建立光柱；保留停止位置，切換世界或重啟後仍可返回，並新增範圍、保存與光柱渲染測試。

## [1.0.130] - 2026-10-22

### 新增

- 南瓜／西瓜依啟動位置自動選擇正向或反向收割，兩種作物分開判斷，支援所在列的小數座標。
- 保留原本起步動作，之後接入對應的完整左／右收割流程；不反轉整條路線，換列、終點與回程不變。
- 未設定的位置維持原本向左起步，其他作物不受影響；新增座標邊界、左右換列與完整回程測試。

## [1.0.129] - 2026-10-21

### 新增

- Farming 開啟「異常狀況處理」時，非預期停滯滿 5 秒會透過共用 Webhook 通知 Discord。
- 排除選單、工具不符、正常農耕等待、手動關閉與異常返回；同一段停滯只提醒一次，恢復移動後可再次提醒。
- 僅發送提醒，不自行關閉或重新啟動 Farming；補上計時、暫停排除與通知的回歸測試。

## [1.0.128] - 2026-10-21

### 文件

- README 精簡為繁體中文使用教學，只保留安裝、快捷鍵、功能操作與通知設定。
- 移除程式架構、內部運作細節、開發流程與本機環境資訊；本次不變更遊戲功能。

## [1.0.127] - 2026-10-21

### 變更

- Visitor／Pest 從外層按鈕啟用後直接進入通知設定，沿用上次門檻，不再先開啟單獨的門檻輸入視窗。
- 需要調整時才點選設定第一行；確認修改後保存，取消／Esc 沿用原數量，且不改變已啟用狀態。
- 保留關閉功能、個別計數紀錄與螢幕通知清理，以及通知管道、秒數、共用 Webhook 與 Jerry 色彩設定；新增直接進入與門檻保留的回歸測試。

## [1.0.126] - 2026-10-21

### 變更

- Jerry 設定交換最上方兩行：四色開關列在上，「出現 Jerry 是否停下動作」在下，其後通知管道順序不變。
- 保留原尺寸、顏色、開關狀態與通知設定，更新順序、位置及縮放後的回歸測試。

## [1.0.125] - 2026-10-21

### 變更

- Jerry 的 Discord、遊戲內聊天室與螢幕通知加上實際顏色名稱，例如「發現 Golden Jerry！」。
- 直接使用符合提示且已啟用的 Jerry 顏色，不以無關前後文字或較早出現但已關閉的顏色作為通知名稱。
- 延伸七種格式、四色與全部 16 種開關組合的測試，驗證訊息及螢幕文字顏色一致，保留原過濾與停下動作行為。

## [1.0.124] - 2026-10-21

### 新增

- Jerry 設定在 Discord 訊息通知上方新增 Green／Blue／Purple／Golden 四等寬開關，名稱分別顯示 Minecraft 綠色、藍色、深紫色、金色，並各自顯示開啟／關閉。
- 四色預設全開並獨立保存，舊設定自動沿用全開；不影響通知管道、顯示秒數、共用 Webhook 或停下動作偏好。

### 變更

- 七種 Jerry 提示均需包含四種顏色之一，只對已啟用的顏色發送通知與執行停下動作；四色全關時忽略所有 Jerry。仍忽略顏色碼、大小寫與前後附加文字。
- 新增全部 16 種顏色組合、七種格式、前後無關顏色、多事件訊息、無效顏色、四等寬位置、名稱顏色、全關／重新啟用與偏好保存測試。

## [1.0.123] - 2026-10-21

### 變更

- Jerry 設定將「出現 Jerry 是否停下動作」移至第一行，其後依序為 Discord、遊戲內聊天室與遊戲內螢幕通知。
- Visitor／Pest 設定第一行分別改為「出現多少 Visitor 要通知：數量」及「出現多少 Pest 要通知：數量」，保留原數量與編輯功能。
- 維持每個設定視窗四行、既有通知偏好與秒數；更新順序、完整門檻文字與儲存值的回歸測試。

## [1.0.122] - 2026-10-21

### 變更

- 通知管道統一改稱「遊戲內聊天室通知」與「遊戲內螢幕通知」，目前介面、提示與 README 統一使用 Pest 名稱。
- Visitor／Pest 設定視窗移除重複總開關，只保留數量門檻與三種通知管道共四行；Jerry 仍維持三種管道與停下動作設定。
- Visitor／Pest 總開關由外層 Farming 頁面操作：關閉時輸入門檻後啟用並開啟設定，取消不啟用；開啟時點選直接關閉，保留通知偏好與秒數。
- 新增四行視窗、外層開關、門檻確認／取消、名稱、通知佇列與計數紀錄互不影響的回歸測試。

## [1.0.121] - 2026-10-21

### 文件

- 全面重寫 README 為繁體中文，保留必要的 Minecraft、Hypixel SkyBlock、Fabric、工具與訊息等英文名詞。
- 重新整理快速開始、Mining、Farming、個別通知、共用 Webhook、小數秒顯示時間、建置測試與維護說明。
- 將農耕路線細節收進可展開區塊，以表格整理工具、標記與異常返回流程；補充設定保存與診斷資訊。
- 本次為文件整理與版本同步，不變更遊戲功能。

## [1.0.120] - 2026-10-21

### Changed

- Accept positive decimal seconds with at most one decimal point for each feature's screen duration, including `0.5` and `1.25`; reject multiple dots, zero/negative values, malformed input, and unrepresentable/out-of-range timing values.
- Preserve decimal durations exactly with BigDecimal-to-nanosecond conversion, per-event queue timing, independent feature settings, and plain-decimal persistence/display. Existing integer settings and the three-second default remain compatible.
- Extend parser, queue, and production-dialog/config tests for fractional values, sub-second boundaries, invalid decimal syntax, independent saved durations, and exact timing without floating-point drift.

## [1.0.119] - 2026-10-21

### Added

- Open a seconds-entry dialog when enabling each feature's game-screen notifications. Confirm a positive integer to enable; reject blank, zero, negative, decimal, malformed, and overflowing input. Cancel/Esc preserves the disabled state and prior duration; resize preserves the draft.
- Save Visitor, pest, and Jerry screen durations independently, prefill the last value, and show seconds on the enabled switch. Legacy/missing/invalid saved values default to 3 seconds without altering channel switches or shared Webhook settings.

### Changed

- Replace the fixed three-second screen timeout with each event's selected duration. Capture duration on enqueue so mixed notification types display for their own chosen times; retain feature-isolated cancellation, centered/scaled rendering, and monotonic timing.
- Add parser/queue and production-dialog/routing/configuration regression coverage for input validation, independent durations, confirmation/cancellation, restart persistence, exact boundaries, large-duration arithmetic, and unchanged notification-channel behavior.

## [1.0.118] - 2026-10-21

### Changed

- Remove the global notification settings button. Visitor, pest, and Jerry buttons each open their own independent Discord/chat/screen channel settings; Visitor/pest keep internal enable and threshold controls.
- Keep one live shared Webhook URL: editing from any feature changes every Discord destination without changing other features' Discord switches. Cancel/Esc does not alter the URL; old modal instances also read the latest shared URL.
- Make Jerry a plain settings button with no detection master or generic notification toggle. Its three notification channels and stop-action option independently control behavior; preserve all seven message patterns.
- Safely migrate legacy global notification preferences. An old disabled Jerry setup stays inert, including its previously hidden default-on stop flag. Persist the new per-feature flags without obsolete master/global flags.
- Isolate per-feature Tab-count availability/history and screen queue cleanup. Disabling one feature's screen alerts or master switch does not remove other features' queued alerts; preserve centered three-second rendering and asynchronous Discord delivery.
- Update regression coverage for independent channel combinations, mixed routing, shared live URL edits, threshold validation, masterless Jerry actions, safe migration, feature-specific pending counts, and overlay isolation.

## [1.0.117] - 2026-10-21

### Changed

- Replace the prior Jerry matcher with the seven supplied message patterns: found, discovered, hiding/found-it, wild/spawned, appeared, there-is, and located-hidden Jerry messages.
- Find signals anywhere within received color-stripped chat, allowing arbitrary prefixes/suffixes, any/custom/missing color names, case variations, and extra whitespace. Precompile the patterns in a dedicated matcher while rejecting incomplete signals and non-Jerry items.
- Keep all shared notification channels, Jerry stop-action preferences, and cleanup behavior unchanged. Extend production event/action regression coverage across every pattern, surrounding text (including adjacent/multiline text), color formatting, custom types, and all notification/stop combinations.

## [1.0.116] - 2026-10-21

### Added

- Add Other → Notification Settings on the left and move Jerry Detection to the right. Offer independent Discord, local game-chat, and centered screen notification switches; Visitor, pest, and Jerry alerts all share the selected channels.
- Move Webhook configuration out of the main-screen footer into a confirmation dialog opened when enabling Discord. Preserve existing URLs, validate absolute HTTP(S) input, and leave saved settings unchanged on Cancel/Esc.
- Show large centered screen text for three monotonic-clock seconds per event, queue simultaneous events, fit labels to the viewport, respect F1, and clear stale alerts when disabled/disconnected/changing worlds without modifying server titles.

### Changed

- Rename Jerry's Discord-specific option to a generic notification option, migrating existing preferences. Stop actions remain independent and no longer force chat status messages outside the selected channels.
- Keep local notifications usable without a Webhook and avoid consuming Tab notification counts when every channel is unavailable. Preserve existing pest/Visitor thresholds, per-count deduplication, and asynchronous token-safe Discord delivery.
- Persist channel settings and retain Discord by default for installations with an existing Webhook; fresh installations start with all channels off. Add regression coverage for all eight channel combinations, configuration migration, Webhook input/confirmation/cancellation, local-only routing, centered/scaled rendering, and exact three-second queue boundaries.

## [1.0.115] - 2026-10-21

### Changed

- Extend Jerry detection to `Jerry appeared!`, `You located a hidden [color] Jerry!`, and `You discovered a [color] Jerry!` after stripping Minecraft color codes. Match case-insensitively with flexible whitespace; ignore color names rather than whitelisting known colors.
- Preserve independent Discord/stop-action settings, immediate automation/recovery cleanup, and existing notification content. Avoid matching non-Jerry discoveries, Jerryfish, or a separate sentence mentioning Jerry.
- Extend production chat/action regression tests across all three forms, multiple/custom/missing colors, embedded formatting codes, case/spacing variations, negative examples, and all notification/stop combinations.

## [1.0.114] - 2026-10-21

### Changed

- Replace the fixed five-Visitor notification with a configurable 1–5 threshold dialog, matching the pest notification enable/confirm/Cancel/Esc flow and showing the saved threshold on the enabled toggle.
- Notify once per distinct observed Visitor count at or above the threshold: threshold 3 sends separately at 3, 4, and 5. Sustained or revisited counts do not repeat until a valid below-threshold count or explicit Visitor reset; skipped-over counts are not invented.
- Persist the threshold, defaulting missing/invalid saved values to 5 for existing configurations. Keep missing/invalid Tab data from clearing Visitor history, and keep pest and Visitor notification histories independent. Farming, Mining, Jerry detection, and asynchronous Webhook delivery are unchanged.
- Extend unit and production polling/UI regression tests for every threshold, per-count deduplication/reset, jump behavior, unknown data, strict 1–5 validation, confirmation, cancellation, and independent pest behavior.

## [1.0.113] - 2026-10-21

### Added

- Add the centered dashed Other section beneath Mining and its target list, with the same-sized `Jerry 偵測` toggle.
- Add a confirmation dialog with independent Discord notification and stop-action options. Persist all three settings; master detection defaults off, both draft actions default on. Cancel/Esc leaves saved settings unchanged.
- Detect `Jerry appeared` in received chat/system text after stripping color codes, case-insensitively and with flexible whitespace. Ignore action-bar overlays and non-appearance mentions.
- Notify through the existing shared asynchronous Discord Webhook. Independently stop Mining/Farming, clear targets and input locks, reset pause guards, and cancel queued Farming recovery without resuming. Stopping does not depend on a configured/working Webhook, and idle manual controls are untouched.
- Add production-method and dialog callback regression tests covering the formatted Green Jerry example, color variants, negative matches, all action combinations, inactive detection, missing Webhook, input/recovery cleanup, confirmation, cancellation, and resize drafts.

## [1.0.112] - 2026-10-20

### Fixed

- Preserve finish-current-block behavior and rescan from the highest configured priority for every new target, after the existing mining delay; do not preempt an unfinished block.
- Fix depletion history hiding a higher-priority replacement at a previously mined position while waiting for the old block type to respawn. Permit higher-priority replacements and discard obsolete history if the old type is no longer configured; retain lower-priority depleted-form suppression.
- Add regression tests executing production target selection with a fake world: finish before switching, higher-priority versus distance, replacement/respawn eligibility, visibility, temporary skips, and cooldown.

## [1.0.111] - 2026-10-20

### Changed

- Lock vanilla single-click and held attack handlers whenever Mining is enabled, including GUI smart pause, without blocking GUI clicks or direct automatic mining calls. Disabling Mining restores vanilla handling; Farming is unchanged.
- Add source-contract regression checks for both attack injections, Mining-only gating, registration, and preserved automatic mining calls.

## [1.0.110] - 2026-10-20

### Fixed

- Fix Limbo → Lobby recovery consuming `/play sb` during a slow transfer, then sending `/warp garden` in Lobby where the command is unknown. The old queue advanced only on elapsed ticks without confirming each command's destination context.
- Keep each pending command until its required location is ready, then count a fresh 60 client ticks (about three seconds): `/lobby` in Limbo, `/play sb` in Lobby, and `/warp garden` only after SkyBlock is confirmed. Reset the full countdown if readiness is lost; missing player/world data and transfer time cannot consume queued steps.
- Accept other SkyBlock islands through the optional location API or native SKYBLOCK title, while explicitly rejecting Lobby/Limbo for Garden warps. Preserve immediate completion if `/play sb` lands directly in Garden, Farming remaining off, input cleanup, initial world-load gating, and the separate void stop.
- Log command/location phase changes and dispatch attempts once per transition. Extend tests to execute the actual queue and command objects through slow Limbo/Lobby/SkyBlock loads, transient location loss, missing player/world data, all existing routes, and direct Garden arrival.

## [1.0.109] - 2026-10-20

### Changed

- Shorten the GUI-close smart-pause buffer for both Farming and Mining from 1 second to 0.5 seconds (500ms of continuous GUI-free time, measured with the monotonic clock).
- Keep pausing throughout open GUIs, restart the buffer if any GUI reopens, and preserve independent module buffers and immediate bypass when smart pause is disabled. Other farming waits, recovery command delays, and notification polling are unchanged.
- Update regression coverage for the exact 500ms boundary, repeated queries, Bazaar transitions, toggles, session resets, and independent Farming/Mining resume times.

## [1.0.108] - 2026-10-20

### Changed

- Notify Discord once per distinct observed pest count at or above the configured threshold, rather than once for the entire above-threshold episode. With threshold 4, observed counts 4, 5, 6, 7, and 8 each notify once.
- Keep a set of notified counts so sustained values and revisits such as 4 → 5 → 4 do not resend. Clear it only after a valid below-threshold count or explicit pest-notification reset; unknown/loading Tab data preserves it. Jumped-over counts do not generate invented alerts.
- Leave Visitor notifications unchanged: exactly 5 notifies once until the count leaves 5 or its toggle rearms it. Preserve independent toggles, direct cached Tab reading, the 1–8 threshold dialog, and all Farming/Mining behavior.
- Extend unit and production-polling integration tests for per-count notifications, revisits, missing-data retention, reset behavior, jumps, and unchanged Visitor deduplication.

## [1.0.107] - 2026-10-20

### Changed

- Replace pest spawn-chat notifications with direct cached Tab data: read `Pests` → `Alive` and notify Discord when the count reaches or exceeds a configurable threshold.
- Enabling `害蟲通知` now opens a Mining-style dialog accepting only a single digit from 1–8. Invalid/blank input cannot confirm; Cancel/Esc leaves notifications off. Persist the threshold and display it on the enabled toggle (existing configs default to 1).
- Implement `Visitor 通知` using Tab `Visitors: (number)`, notifying when the value is exactly 5. Reuse the existing local Discord Webhook for both features.
- Poll independently of Farming/Mining once per 20 client ticks, without requiring the Tab overlay to be open. Match vanilla Tab ordering and read formatted display names rather than scoreboard text or screenshots.
- Notify once per qualifying episode, separately for pests and visitors. Rearm after a valid nonqualifying count or an explicit toggle; absent/loading Tab data does not reset an episode. Keep existing world-load recovery, tool detection, and GUI smart pause unchanged.
- Add pure parsing/threshold/episode tests and production-code integration coverage for ordering, polling, toggles, missing data, and threshold-screen validation/cancellation. Keep HTTP asynchronous with a ten-second timeout and token-safe failure logging.

## [1.0.106] - 2026-10-19

### Changed

- Require a recorded client-world-load event in addition to the existing Island, Hub, Lobby, or Limbo condition before starting abnormal recovery. Use Fabric `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE`, not a dimension-ID comparison.
- Record events only during active Farming with abnormal handling enabled. Allow the event and location update to arrive in either order; consume the event when recovery starts and reset it for new/stopped Farming sessions or abnormal-setting changes. Ignore loads caused by an ongoing recovery.
- Preserve all existing follow-up actions, three-second command waits, wrong-tool cleanup, and immediate Garden-arrival completion. Void-message stopping remains independent of world loads.
- Include the pending world-change state in location diagnostics and add regression coverage for both signal orders, all four gated locations, Limbo chat, disabled/inactive safeguards, and unchanged return/void behavior.

## [1.0.105] - 2026-10-19

### Changed

- Standardized every Island, Hub, Lobby, and Limbo return-command wait to 60 client ticks (about three seconds), including the first command and every following step.
- Kept each route's commands unchanged and preserved immediate recovery completion when Garden is confirmed.
- Paused the proposed dimension-ID gate pending discussion of world-load events; neither dimension-ID nor world-load gating is included in this release.
- Updated command-sequence regression tests to verify all waits are three seconds using the production delay constant.

## [1.0.104] - 2026-10-19

### Fixed

- Finish abnormal recovery as soon as Garden is confirmed, checking at client-tick start before enforcing movement locks and again before sending any queued command.
- Do not wait for the return-command queue to become empty. Cancel leftover commands and fully stop the recovery session, clearing tool/GUI pause state and releasing movement and manual mouse-look locks immediately upon detection.
- Keep Farming disabled after return and emit the completion message once only; normal Garden farming is not affected.
- Added regression coverage for arrival with pending commands, early completion, mouse-look unlock, one-time completion messages, and safeguards against stopping normal farming or finishing at unknown locations.

## [1.0.103] - 2026-10-19

### Fixed

- Prefer the optional SkyBlock API `LocationAPI` island enum, the same island source used by the installed CustomScoreboard. Its generated Private Island/Hub labels need not exist in the native scoreboard, so native text matching alone was insufficient.
- Detect Private Island, Hub, and Garden directly from API island identity. Keep native sidebar and existing Limbo/Lobby checks as fallbacks when the API is absent or not ready; do not misclassify other known API islands using stale native text.
- Resolve optional API access lazily and cache reflection methods without adding a required mod dependency or retaining stale island identity outside SkyBlock.

### Added

- Transition-only diagnostic logging of detected location, API island, and native sidebar in `logs/latest.log` for in-game verification without chat spam.
- Regression coverage for API island transitions, non-SkyBlock state, missing/failing API, API-first detection without native sidebar labels, native fallback, and Garden return confirmation.

## [1.0.102] - 2026-10-19

### Fixed

- Read complete sidebar score names using their team prefixes/suffixes and owner display names instead of only raw score owners or display overrides, fixing missed Island, Hub, and Garden keywords stored in team text.
- Match sidebar keywords case-insensitively while ignoring color codes, icons, punctuation, spacing, and invisible characters.
- Identify Island from `Private Island` alone; prioritize `Hub` over Island and the held Game Menu Lobby signal. Do not identify Island from `Your Island` alone, since that line can also appear in Hub.
- Added regression fixtures based on the supplied Island and Hub screenshots, including split team text, common `Your Island` lines, decorated/non-breaking-space keywords, display overrides, hidden entries, Garden, and empty-sidebar Limbo detection.

## [1.0.101] - 2026-10-19

### Fixed

- Starting Farming abnormal recovery now clears the pending wrong-tool pause and previous crop-tool requirement before closing Farming and executing return commands. This includes server-changed items in Lobby and empty hands in Limbo.
- Preserve the saved `工具偵測` toggle so tool validation works normally on the next manual Farming start. Normal wrong-tool waiting remains unchanged outside abnormal recovery.
- Added regression coverage for clearing tool waits in all four return paths and chat-triggered Limbo recovery while preserving the detection setting and command sequence.

## [1.0.100] - 2026-10-19

### Changed

- Removed the prior-Garden requirement from all five Farming abnormal-handling triggers. With Farming active and `異常狀況處理` enabled, detected Island, Hub, Lobby, Limbo, or a void-fall message is handled without needing to recognize Garden first.
- Removed obsolete Garden-history state and changed recovery messages to report the detected location rather than assuming departure from Garden.
- Preserved existing location detection, return commands and delays, disabled-setting/session safeguards, and the rule that Farming stays off after returning to Garden.

### Added

- Dependency-free regression tests exercising the actual client recovery/message methods with fake Minecraft boundaries, covering all five triggers without prior Garden recognition, command sequences, disabled safeguards, and no automatic restart.

## [1.0.99] - 2026-10-19

### Fixed

- Added a one-second GUI-close buffer to both Farming and Mining `智慧暫停` to prevent premature automation during short menu transitions such as Bazaar searches.
- A new GUI cancels the previous countdown; the full second starts again when that GUI closes. Farming movement, attack, and crop-state progression and Mining rotation/breaking stay paused throughout the buffer.
- Turning smart pause off bypasses the buffer. Module starts/stops clear stale pause history; Mining and Farming have independent buffers.

### Added

- Deterministic regression tests for the one-second boundary, Bazaar-style GUI transitions, repeated checks within a tick, toggle changes, session reset, and module independence.

## [1.0.98] - 2026-10-19

### Changed

- Kept the original immediate tool rejection at Farming startup when `工具偵測` is enabled.
- Added continuous held-tool checks against the initially detected crop. A wrong tool or empty hand pauses movement, attack, and crop-state progression indefinitely; returning to the correct tool resumes the same state without restarting Farming.
- Send one in-game warning per switch to an incorrect tool, suppress repeated warnings while waiting, and allow a new warning after switching back and away again.
- Tool checks continue through open GUIs; abnormal location recovery remains active while waiting. Disabling tool detection removes the tool-related pause.

### Added

- Dependency-free regression tests for tool matching, indefinite waiting, automatic resume, notification transitions, detection toggling, and session resets (`./scripts/test.sh`).

## [1.0.97] - 2026-10-19

### Changed

- Blocked manual mouse-look rotation while Mining or Farming is enabled, including Farming abnormal recovery.
- Module-controlled Mining target rotation and Farming fixed crop angles continue to work normally.

## [1.0.96] - 2026-10-18

### Changed

- Mining target selection now prioritizes the configured list order before distance. All eligible targets of a higher-priority block type within the scan radius are exhausted before lower-priority types are selected.
- Distance now selects the nearest block only among candidates of the same priority.

## [1.0.95] - 2026-10-18

### Added

- Added an independent saved Mining `智慧暫停` toggle beside `選擇目標方塊`.
- When enabled, Mining pauses target rotation and block breaking while any GUI is open, then resumes after it closes without turning Mining off.

## [1.0.94] - 2026-10-17

### Changed

- Renamed the Farming `選單關閉` toggle to `智慧暫停` without changing its behavior.

## [1.0.93] - 2026-10-17

### Added

- Added the saved `選單關閉` toggle in the third Farming notification-row position.
- When enabled, any open GUI pauses Farming until closed; when disabled, Farming continues while GUIs remain open.

## [1.0.92] - 2026-10-17

### Changed

- Locked manual forward, backward, left, right, jump, sneak, and sprint input while Mining or Farming is enabled.
- Mining now remains horizontally stationary, while Farming accepts movement only from its active crop state machine.

## [1.0.91] - 2026-10-17

### Changed

- Shifted the Mining target reorder/delete controls to the right edge of the settings grid and replaced the delete label with `×`.

## [1.0.90] - 2026-10-17

### Changed

- Made Mining automatic rotation permanently enabled, removed its toggle, and moved `選擇目標方塊` into the middle position.
- Swapped the Farming notification controls so `Visitor 通知` appears before `害蟲通知`.

### Removed

- Removed the unused `？？？通知` placeholder button and configuration value.

## [1.0.89] - 2026-10-17

### Changed

- Hardcoded row, start, and end detection as permanently enabled with yellow stained glass, light blue stained glass, and pink stained glass.

### Removed

- Removed all six Farming detection and block-selection buttons, their three block-name/icon labels, right-click selection mode, and saved marker configuration.

## [1.0.88] - 2026-10-17

### Changed

- Shortened both flower row transitions from two seconds to one second: the first half-second uses one direction, and the final half-second adds the diagonal direction.

## [1.0.87] - 2026-10-17

### Added

- Added native block icons to the left of the Farming row, start, and end block names.

## [1.0.86] - 2026-10-17

### Removed

- Removed Mushroom stuck detection and its automatic 12-tick jump response; Mushroom farming no longer presses jump.

### Fixed

- Moved the Mining target rows below the mining-delay controls so block icons and names no longer overlap the buttons.

## [1.0.85] - 2026-10-17

### Added

- Added saved `Visitor 通知` and `？？？通知` placeholder toggles for future notification behavior.
- Added native block icons beside Mining targets.

### Changed

- Merged Island, Hub, Lobby, Limbo, and void-fall handling under one `異常狀況處理` toggle.
- Island/Hub/Lobby/Limbo recovery now closes Farming and ends after Garden is confirmed, without `/home` or automatically restarting Farming; a void fall only closes Farming.
- Changed the shared Farming marker defaults to yellow stained glass, light blue stained glass, and pink stained glass.
- Mining target selection now replaces the old list, accepts 1–5 requested unique block types, ignores duplicate selections, and ends after the requested count.
- Reordered the Farming and Mining controls and removed the Mining target-list header.

## [1.0.84] - 2026-10-17

### Changed

- Removed the `Auto` word from Mining and Farming open, close, and unavailable messages.
- Removed Mining target waiting/resume notices and Farming movement, row-marker, start/end/return, mushroom-direction, and successful tool-confirmation messages.
- Farming tool mismatch messages now show only the detected crop and required tool, without the currently held item.

## [1.0.83] - 2026-10-17

### Added

- Added automatic Sunflower/Moonflower and Wild Rose farming at fixed yaw `-45` and pitch `0`.
- Added the shared flower right/back harvesting cycle, timed two-second row transitions, and the final-half-second diagonal inputs.
- Added flower end-to-start recovery while keeping attack held, plus `Eclipse Sickle` and `Wild Rose Cutter` validation.

## [1.0.82] - 2026-10-17

### Removed

- Removed the Farming Speed list, item icons, editable fields, and their saved configuration values from the settings screen.

## [1.0.81] - 2026-10-17

### Fixed

- Hub return now prioritizes the sidebar's `Hub` location over the held Game Menu Lobby signal.
- Starting Farming while already in Garden now arms teleport recovery immediately instead of waiting for the next periodic location scan.

## [1.0.80] - 2026-10-16

### Fixed

- Nether Wart, Wheat, Carrot, and Potato row changes now hold the jump key instead of the forward key: jump alone for three seconds, then jump plus the opposite sideways direction for the fourth second.

## [1.0.79] - 2026-10-16

### Changed

- Sugar Cane farming now keeps attack held during every active state, including the end-to-start return and the additional two backward blocks.

## [1.0.78] - 2026-10-16

### Changed

- Replaced the two-second stationary row-change wait for Nether Wart, Wheat, Carrot, and Potato with four seconds of forward input for water movement.
- The first three seconds hold forward only; the fourth second holds forward and the opposite sideways direction simultaneously, with attack held throughout.

## [1.0.77] - 2026-10-16

### Changed

- Nether Wart, Wheat, Carrot, and Potato row changes no longer move forward two blocks after the extra sideways block.
- They now wait two seconds without movement while keeping attack held, then perform the final one-block forward/sideways diagonal movement.

## [1.0.76] - 2026-10-16

### Changed

- Cactus farming now waits one second without moving or attacking after each two-block forward row transition, before starting the opposite harvesting direction.

## [1.0.75] - 2026-10-16

### Changed

- Cocoa Bean farming now continues left for one measured block without attacking after detecting the start marker, then resumes forward harvesting with attack held.

## [1.0.74] - 2026-10-16

### Changed

- Nether Wart, Wheat, Carrot, and Potato farming now move forward one measured block without attacking immediately after startup crop/tool detection, then begin their normal harvest direction.

## [1.0.73] - 2026-10-16

### Changed

- Cocoa Bean farming now moves left one measured block without attacking immediately after startup crop/tool detection, then begins forward harvesting with attack held.

## [1.0.72] - 2026-10-16

### Fixed

- Fixed Farming Speed input text becoming invisible immediately after editing by using opaque ARGB text colors.

## [1.0.71] - 2026-10-16

### Changed

- Changed the fixed Sugar Cane farming yaw from `60` to `45`; pitch remains `0`.

## [1.0.70] - 2026-10-16

### Added

- Added Cactus automatic farming at fixed yaw `90` and pitch `0`.
- Added its initial forward-one step and alternating left/right harvest loop with attack-free one-block sideways and two-block forward row transitions.
- Cactus start markers remain inactive until an end marker is detected while harvesting right.
- After the end marker, Cactus releases attack, backs up while ignoring row markers until the start marker, moves left one block, moves forward one block, and restarts leftward harvesting.

## [1.0.69] - 2026-10-16

### Added

- Added Cocoa Bean automatic farming at fixed yaw `0` and pitch `-45`.
- Added its forward harvest → right one → backward harvest → right two → forward one → left one loop while holding attack.
- Cocoa Bean start markers remain inactive until an end marker is detected during backward harvesting.
- After the end marker, Cocoa Bean releases attack, moves left while ignoring row markers until the start marker, and immediately restarts forward harvesting.

## [1.0.68] - 2026-10-16

### Added

- Added Sugar Cane automatic farming at fixed yaw `60` and pitch `0`.
- Added its left harvest → forward five → right one → backward harvest → right seven loop while holding attack.
- Sugar Cane start markers remain inactive until an end marker is detected during backward harvesting.
- After the end marker, Sugar Cane releases attack, backs up to the start marker, continues backward two measured blocks, and restarts leftward harvesting.

## [1.0.67] - 2026-10-16

### Added

- Added a shared automatic farming state machine for Nether Wart/Carrot using fixed yaw `-90` and pitch `5.5`.
- Added its mirrored state machine for Wheat/Potato using fixed yaw `+90` and pitch `5.5`.
- Added measured sideways, forward, and diagonal row transitions, end-to-start return handling without attack, and a one-second restart wait.
- Start markers for these four crops remain inactive until the end marker has triggered the return phase.

## [1.0.66] - 2026-10-16

### Changed

- Farming now pauses movement and attack whenever any GUI is open, including the Esc menu and settings screens, then resumes when the GUI closes.

## [1.0.65] - 2026-10-16

### Changed

- Pumpkin/Melon now continues backward for two measured blocks after detecting the start block, before moving left and forward to resume the cycle.

## [1.0.64] - 2026-10-16

### Changed

- Converted all 13 Farming Speed reference values into editable numeric fields.
- Every Farming Speed field now defaults to `200` and persists independently in the local configuration.

## [1.0.63] - 2026-10-16

### Changed

- Pumpkin/Melon now moves right by one block without attacking after detecting the end block, then begins moving backward toward the start block.

## [1.0.62] - 2026-10-16

### Fixed

- Pumpkin/Melon one-block movement now replaces the old movement key immediately when a measured step completes, preventing an extra forward/backward movement tick.

## [1.0.61] - 2026-10-16

### Added

- Added a read-only `Farming Speed：` reference list with native item icons on the right side of the settings screen.
- Listed all 13 crops separately; Pumpkin/Melon use `400`, Mushroom uses `233`, and unspecified speeds show `?`.

## [1.0.60] - 2026-10-16

### Fixed

- Changed every Pumpkim & Melon “one block” movement stage from fixed-time movement to an actual `1.0`-block displacement measurement, preventing high Speed from intentionally moving multiple blocks.
- Blocked one-block actions now finish only after ten consecutive ticks without position progress, preserving the earlier blocked-movement fallback without shortening normal slow movement.

## [1.0.59] - 2026-10-16

### Fixed

- Added `minecraft:carved_pumpkin` as a Pumpkin crop representation for automatic crosshair detection.
- Carved Pumpkin now selects the shared Pumpkin/Melon movement profile, reports `Pumpkin`, and validates against `Pumpkin Dicer`.

## [1.0.58] - 2026-10-16

### Changed

- All block names in Kuanperion chat messages now hide the `minecraft:` namespace, for example `minecraft:red_mushroom` is displayed as `red_mushroom`.
- Confirmed all module-generated status and detection messages continue to use the standard `[Kuanperion]` prefix; long messages may visually wrap onto a second Minecraft chat line.

## [1.0.57] - 2026-10-16

### Changed

- Simplified Farming crop detection to inspect the block directly under the crosshair without attacking or breaking it.
- Pumpkin and Melon now report their exact detected type and use separate tool validation while continuing to share the `Pumpkim & Melon` movement profile.
- Red and Brown Mushroom are identified directly from the aimed block and still select their respective initial Farming directions.

## [1.0.56] - 2026-10-16

### Fixed

- Fixed crop auto-detection doing nothing when the aimed block was not already in the known crop-ID list. Farming now captures any aimed non-air block first and reports its ID.
- Crop probing now combines a continuously held attack key with Minecraft's direct start/continue-destroy actions for one second, ensuring the initial crop is actually hit and broken before identification.

## [1.0.55] - 2026-10-16

### Added

- Added tool validation mappings for Wheat (`Euclid's Wheat Sickle`), Carrot (`Gauss Carrot Shovel`), Potato (`Pythagorean Potato Shovel`), Sugar Cane (`Turing Sugar Cane Cutter`), Cactus (`Cactus Knife`), Cocoa Beans (`Cocoa Chopper`), and Nether Wart (`Newton Nether Wart Cutter`).
- Reserved the supplied future flower mappings in code: Sunflower and Moonflower use `Eclipse Sickle`; Wild Rose uses `Wild Rose Cutter`. These become active when the flower block IDs and movement profiles are added.

### Fixed

- Crop auto-detection now genuinely holds the attack key continuously for at least 20 ticks (one second) without resetting it every tick, then identifies the broken crop.

## [1.0.54] - 2026-10-16

### Added

- Added a saved `工具偵測` toggle in place of the obsolete crop-selection button.
- After automatic crop identification, enabled tool detection now verifies the held item name before Farming continues. Mushroom requires `Fungi Cutter`, Melon requires `Melon Dicer`, and Pumpkin requires `Pumpkin Dicer`; Mk. suffixes and other name decorations do not affect the base-name match.
- Tool mismatches now close Farming and report the detected crop, required tool, and currently held item in the standard `[Kuanperion]` chat format.

### Removed

- Removed the manual crop-selection popup because Farming now always detects the crop automatically.

## [1.0.53] - 2026-10-16

### Added

- Farming now attacks the crop under the crosshair on every start, identifies the broken crop block, and automatically switches to the detected crop profile regardless of the previous menu selection.
- Pumpkim & Melon now reports every row-change and return-state action in chat with the standard `[Kuanperion]` prefix.

### Changed

- Removed harvest-block detection and its settings controls. Row, start, and end blocks are now one shared configuration used by every crop, while crop-specific reactions remain separate.
- Pumpkim & Melon now moves one additional block in its existing lateral direction immediately after the first row-marker detection, then continues the forward/second-marker sequence.
- Pumpkim & Melon forward marker transitions use a minimum held-input duration and no longer appear idle while waiting to clear the first marker.

## [1.0.52] - 2026-10-16

### Changed

- `Pumpkim & Melon` startup now holds the forward input for 10 ticks (about half a second, approximately one normal walking block) before beginning continuous leftward harvesting, regardless of whether collision allows actual movement.

## [1.0.51] - 2026-10-16

### Changed

- `Pumpkim & Melon` startup now performs the forward input once and immediately begins continuous leftward harvesting; it no longer waits for actual forward displacement when the player is blocked.

## [1.0.50] - 2026-10-16

### Fixed

- Prevented `Pumpkim & Melon` one-block movement stages from getting stuck forever when collision or server position correction prevents reaching the exact distance threshold; each stage now falls back after one second so startup proceeds to continuous leftward harvesting.

## [1.0.49] - 2026-10-15

### Added

- Added a unified semantic chat-message color system: gradient/bold `[Kuanperion]`, matching Farming/Mining feature colors, green success, red failure, orange warnings, yellow blocks, aqua locations, gold commands, and yellow-green crop names.
- Mining now refuses to start with an empty target list and reports the reason.
- Mining now reports once when waiting for an available target and once when a target returns.

### Changed

- Renamed every chat prefix from `[Kuan SBA]` to `[Kuanperion]`.
- Standardized all Farming and Mining status, selection, detection, recovery, success, warning, and error messages.
- Mushroom startup now reports `Auto Farming 開啟，方向確認：Red Mushroom` or `Brown Mushroom`; all crop names share the same yellow-green style.

## [1.0.48] - 2026-10-15

### Added

- Added the dedicated `Pumpkim & Melon` Farming state machine with fixed yaw `90.0` and pitch `-57.0`.
- Added distance-based one-block forward, backward, left, and right movement steps.
- Added two-stage row-marker handling that requires leaving the first marker before the second marker can trigger.

### Changed

- Merged the separate Pumpkin and Melon crop choices into `Pumpkim & Melon`, using the pumpkin icon and migrating legacy Pumpkin/Melon block settings.
- Pumpkim & Melon now holds attack during all normal harvesting and row-change steps, releases attack only between end-block and start-block detection, and ignores row markers during that return run.
- After reaching the start block, the return sequence is back one block, left one block, forward one block, then continuous leftward harvesting.

## [1.0.47] - 2026-10-15

### Changed

- Removed the `Shift+B` Mining block-selection shortcut and its keybinding entry.
- Mining block selection can now only be started from the settings screen. It selects one right-clicked block and then ends automatically, matching the Farming block-selection flow.

## [1.0.46] - 2026-10-15

### Changed

- Moved the Farming settings section above Mining.
- Centered the Farming and Mining section headings horizontally.
- Added a blank-line gap between settings sections for clearer separation.

## [1.0.45] - 2026-10-15

### Changed

- Replaced the expanding crop list with a centered crop-selection popup arranged in a two-column grid.
- Added native Minecraft item icons to every crop choice.
- Renamed the webhook label to `Discord Webhook`, shortened the input field, and removed its gray suggestion text.

## [1.0.44] - 2026-10-15

### Added

- Added a fourth per-crop Farming block setting: harvest-block detection. Farming pauses when the player is not standing on the configured block and automatically turns off after 3 seconds.
- Mushroom Farming now breaks and confirms one starting mushroom before moving; red uses the original direction and brown uses the post-row-switch direction.

### Changed

- Row, start, end, and harvest detection toggles and block choices are now stored independently for every crop profile. Existing legacy settings migrate to Mushroom.
- Farming block controls now use four equal-width columns without increasing the original total width.

## [1.0.43] - 2026-10-15

### Changed

- Moved the Discord webhook input from the center of the Farming controls to the bottom-left corner of the settings screen.

## [1.0.42] - 2026-10-15

### Changed

- Lobby recovery now triggers from the held `Game Menu` item alone; the `Evacuating to Hub...` chat message is no longer required.
- All recovery flows still require Farming to have been active and confirmed in Garden first.

## [1.0.41] - 2026-10-14

### Added

- Farming now pauses movement and attack while the player inventory or chat screen is open, then resumes automatically after closing the screen.

## [1.0.40] - 2026-10-14

### Added

- Added Garden void-fall recovery: when Farming is active in Garden and chat says `You fell into the void`, Farming closes, runs `/home`, then reopens.

## [1.0.39] - 2026-10-14

### Changed

- Reworked Farming auto-recovery to require a confirmed Garden state before Island/Hub/Lobby/Limbo recovery can trigger.
- Recovery now closes Farming before sending return commands and reopens Farming after returning to Garden and running `/home`.
- Added separate settings toggles for Island, Hub, Lobby, and Limbo return flows.

## [1.0.38] - 2026-10-13

### Added

- Farming auto-recovery now also triggers from the Hypixel chat message `Evacuating to Hub...`.

## [1.0.37] - 2026-10-13

### Fixed

- Prevented stale saved Farming enabled state from triggering auto-recovery immediately after joining a server.
- Improved Farming auto-recovery location detection for Hub/Lobby/Limbo and checks Limbo before generic lobby text.

## [1.0.36] - 2026-10-13

### Changed

- Removed the hard-coded Discord pest webhook URL from code and added an in-game settings field saved to the local config file.

## [1.0.35] - 2026-10-13

### Changed

- Updated all settings toggle buttons to show 開啟/關閉, with green enabled text and red disabled text while keeping labels white.

## [1.0.34] - 2026-10-13

### Added

- Added a Farming crop dropdown selector and a Discord pest notification toggle for Hypixel pest chat messages.

### Changed

- Replaced the previous Farming crop previous/next buttons plus crop label with one crop selector button.

## [1.0.33] - 2026-10-13

### Changed

- Reverted Farming harvesting to simple held attack-key behavior, removing forced vanilla attack reflection used for GUI/background harvesting attempts.

## [1.0.32] - 2026-10-13

### Fixed

- Farming now refreshes Minecraft's native crosshair target before continuing the vanilla held-left-click attack flow, improving harvesting while inventory or other screens are open.

## [1.0.31] - 2026-10-13

### Added

- Added Farming auto-recovery for Hub, Lobby, Limbo, and Island displacement using Hypixel sidebar detection.
- Auto-recovery now stops inputs, runs `/lobby` then `/play sb` from Limbo, returns to Garden, runs `/home`, and resumes Farming.

## [1.0.30] - 2026-10-13

### Removed

- Removed the Mining HUD and all HUD position controls from settings.

## [1.0.29] - 2026-10-13

### Fixed

- Farming now starts the vanilla attack once and then only continues it each tick, matching held left-click behavior instead of repeated clicking.

## [1.0.28] - 2026-10-13

### Fixed

- Farming now uses Minecraft's native left-click attack flow (`startAttack` / `continueAttack`) instead of custom reach-based raycasting, matching manual harvest distance and hit behavior more closely.

## [1.0.27] - 2026-10-13

### Fixed

- Reduced Farming direct block-breaking reach so automated harvesting matches manual left-click reach more closely and avoids hitting farther rows.

## [1.0.26] - 2026-10-13

### Added

- Added separate Farming start and end detection blocks with independent toggles.
- Added one-shot block-pick buttons for row, start, and end Farming detection blocks.
- End detection now backs up until the start block is detected, shifts right once, then resumes normal forward harvesting.

### Changed

- Reworked the Farming settings marker UI into two rows of three controls and removed the long crop help line.

## [1.0.25] - 2026-10-13

### Fixed

- Farming now directly sends block-breaking actions every tick, improving harvesting while inventory, settings, or other GUI screens are open.

## [1.0.24] - 2026-10-13

### Changed

- Farming now directly applies player movement input every tick so harvesting keeps running through focus changes and open GUI/screens as much as the client/server allow.

## [1.0.23] - 2026-10-13

### Changed

- Mushroom now shifts left before reversing from both forward and backward row marker detections.
- Mushroom keeps holding attack while performing the left-shift row change.

## [1.0.22] - 2026-10-13

### Changed

- Replaced separate Red/Brown Mushroom crop profiles with a single Mushroom profile.
- Mushroom now shifts left briefly before reversing when a row marker is detected while moving forward.

## [1.0.21] - 2026-10-13

### Fixed

- Fixed Farming repeatedly re-triggering the same marker before moving away, which caused it to idle at row-switch blocks.

## [1.0.20] - 2026-10-13

### Added

- Added configurable Farming detection blocks from the settings screen.
- Added chat notifications when Farming detects a row-switch marker block.

### Changed

- Farming uses custom detection blocks when configured, otherwise it falls back to crop defaults.

## [1.0.19] - 2026-10-13

### Changed

- Changed automation hotkeys to `Shift+M` for Mining, `Shift+F` for Farming, and `Shift+B` for Mining block selection mode.

## [1.0.18] - 2026-10-13

### Added

- Added the first Farming Assistant port inspired by the old `bobscript` Forge mod.
- Added crop profiles for Wheat, Potato, Carrot, Nether Wart, Cactus, Sugar Cane, Melon, Pumpkin, Cocoa Bean, Red Mushroom, and Brown Mushroom.
- Added Farming settings for crop selection, marker-based row switching, and optional `/sethome`.
- Added `N` as the default Farming toggle key.

### Changed

- Mining and Farming now turn each other off so only one automation mode runs at a time.

## [1.0.17] - 2026-10-13

### Added

- Added a Mining HUD showing recent average blocks mined per second, with position controls in settings.

### Changed

- Mining now always selects the nearest currently visible selected block that is not bedrock; target list order only breaks distance ties.
- Mining locks the selected block position, rotates to its center, and mines only if the crosshair confirms that exact block.
- Failed/unreachable candidates are skipped longer to prevent immediate back-and-forth jitter.
- Mining disables pause-on-lost-focus while enabled and continues running through in-game menus/inventories where the server accepts mining packets.

## [1.0.16] - 2026-10-13

### Fixed

- After a mined target changes to bedrock or another block, that position is treated as depleted and ignored until the original target block respawns.
- Removed the old last-target retry behavior that could cause the miner to keep revisiting depleted bedrock positions.

## [1.0.15] - 2026-10-13

### Fixed

- Bedrock is now hard-blocked from target selection, saved target lists, and mining scans.
- Existing saved `minecraft:bedrock` targets are automatically ignored when config loads.

## [1.0.14] - 2026-10-13

### Fixed

- Targets that cannot be mined after rotating are temporarily skipped, preventing repeated back-and-forth jitter between unreachable blocks.

## [1.0.13] - 2026-10-13

### Changed

- Simplified mining validation: after rotating, the mod only mines if the crosshair is on the selected candidate block and that block type is in the target list.
- Removed extra line-of-sight and edge-center rejection logic that caused valid targets to be skipped.

## [1.0.12] - 2026-10-13

### Fixed

- Fixed Mining idling after line-of-sight checks by aiming slightly inside the visible block face instead of outside it.
- Slightly relaxed the face-center hit margin so valid targets are less likely to be rejected.

## [1.0.11] - 2026-10-13

### Changed

- Split the settings screen into visibly separated Mining and Farming sections.
- Removed the bottom Clear Targets button and centered the Done button.

## [1.0.10] - 2026-10-13

### Changed

- Reworked the Mining settings layout into four rows.
- Added configurable rotation speed controls in the settings screen.

## [1.0.9] - 2026-10-13

### Changed

- Increased smooth rotation speed from `7°/tick` to `13°/tick`.
- Mining now aims at the visible face center of the target block.

### Fixed

- Mining will not start if the crosshair hits the target block too close to an edge, reducing accidental mining of adjacent blocks such as bedrock.

## [1.0.8] - 2026-10-13

### Fixed

- Mining now only selects blocks with a clear line of sight.
- Before mining, the crosshair must be pointing at the exact target block; obstructed targets are skipped.

## [1.0.7] - 2026-10-13

### Changed

- Block selection mode no longer clears existing targets when enabled.
- Re-selecting an existing block moves it to the newest position without creating duplicates.
- Reordered the scan radius controls to show minus, plus, then current radius.

## [1.0.6] - 2026-10-13

### Fixed

- Mining now holds the attack key while working on the active target so block breaking behaves like holding left click.

## [1.0.5] - 2026-10-13

### Fixed

- Mining completion is now detected when the target block changes, including server-side replacement with bedrock.

## [1.0.4] - 2026-10-13

### Changed

- Mining Assistant now freezes player movement while enabled.
- Target rotation is now smoothed over multiple ticks instead of snapping instantly.

## [1.0.3] - 2026-10-13

### Fixed

- Mining now locks onto the current target until the block is mined, changes, or goes out of range instead of jumping between blocks mid-break.

## [1.0.2] - 2026-10-13

### Changed

- Renamed the keybind category to "Kuanperion".
- Renamed keybind actions to "Select Blocks", "Open Setting", and "Toggle Auto Mining".
- Changed the default settings key from `Right Shift` to `'`.

## [1.0.1] - 2026-10-13

### Changed

- Updated the mod description to: "Hypixel SkyBlock assistant for mining and farming".

## [1.0.0] - 2026-10-13

### Added

- Initial Fabric client mod for Minecraft `26.2`.
- Mining Assistant with nearby block scanning, selectable target block types, target priorities, auto-rotation, and auto-mining.
- In-game settings screen opened with `Right Shift`.
- Block selection mode toggled with `B`.
- Mining toggle with `M`.
- KuanCapoo mod icon.
