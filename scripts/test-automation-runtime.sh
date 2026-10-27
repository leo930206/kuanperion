#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDK="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
GSON="$HOME/Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar"
TMP="$(mktemp -d)";trap 'rm -rf "$TMP"' EXIT
"$JDK/bin/javac" --release 21 -encoding UTF-8 -cp "$GSON" -d "$TMP" $(find "$ROOT/src/main/java/dev/kuan/kuanperion/automation" -name '*.java') $(find "$ROOT/src/test/java/dev/kuan/kuanperion/automation" -name "*.java")
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationRuntimeTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationSafetyTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutoSellTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutoSellServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutoSellManualQueueTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BazaarMarketTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.HypixelMarketClientTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationMenuTransitionTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BazaarPurchaseLedgerTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookCombinePlannerTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.JunkTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.JunkServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.JunkManualQueueTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookCombineTaskTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookConfirmedAnvilTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestObservationTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestFlightPathTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestSteeringTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestManualQueueTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.PlayerPositionReceiptsTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationLocationTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestPlotTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestPlotTravelTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTaskTravelTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTravelSettingsTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTrapObservationTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTrapTargetTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTrapReleaseTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTaskTrapTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.CursorReceiptsTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhouseIdentityTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.HarvestReceiptsTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhouseScanTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.flight.FlightAnchorTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhousePolicyTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhouseTaskTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhouseSettingsTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhouseServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhouseManualQueueTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.composter.ComposterObservationTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.composter.ComposterPolicyTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.composter.ComposterTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.composter.ComposterSettingsTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.composter.ComposterServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.composter.ComposterManualQueueTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTrapTravelTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTrapTravelSettingsTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorOfferTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorValuationTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorSubmissionTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.StashSuspendTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.SkyblockItemCatalogTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.HypixelItemCatalogClientTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorQueueTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorTargetsTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestTrapDelayedCloseTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.visitor.VisitorManualQueueTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.supercraft.SupercraftPlanTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.supercraft.SupercraftReceiptsTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.supercraft.SupercraftPreviewTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.supercraft.SupercraftTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationCompletionTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pets.PetSalePreflightTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.supercraft.SupercraftServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationSettingsIntegrityTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestBaitTransferTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.pest.PestBaitTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationSessionObservationTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationSessionReentrantFinishTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationSessionPauseCallbackTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.TradePreflightObservationTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationSessionBeginObservationTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.JunkTaskObservationTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.TradeServiceObservationTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookInventorySnapshotTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookTaskInventoryGuardsTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookTaskCursorTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.CursorOpeningReceiptsTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookReturnTransferTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.greenhouse.GreenhouseBusyRootTest
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.composter.ComposterBusyRootTest
