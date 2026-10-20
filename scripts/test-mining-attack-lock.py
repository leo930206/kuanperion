"""Source-contract regression checks for the vanilla attack interception."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "src/main/java/dev/kuan/skyblockassistant"
main = (java / "KuanSkyBlockAssistantClient.java").read_text()
assert "public static boolean shouldLockManualAttack()" in main
assert "return miningEnabled;" in main.split("public static boolean shouldLockManualAttack()", 1)[1].split("}", 1)[0]
mixin = (java / "mixin/MinecraftAttackMixin.java").read_text()
assert 'method = "startAttack"' in mixin and "cir.setReturnValue(false)" in mixin
assert 'method = "continueAttack"' in mixin and "ci.cancel()" in mixin
assert mixin.count("KuanSkyBlockAssistantClient.shouldLockManualAttack()") == 2
assert mixin.count("cancellable = true") == 2
config = json.loads((root / "src/main/resources/kuan-hypixel-skyblock-assistant.mixins.json").read_text())
assert "MinecraftAttackMixin" in config["client"]
# Automatic mining must retain direct game-mode calls, outside vanilla input handlers.
mining = main.split("private static boolean mineActiveTarget", 1)[1].split("private static void clearActiveTarget", 1)[0]
assert "mc.gameMode.startDestroyBlock(target, face)" in mining
assert "mc.gameMode.continueDestroyBlock(target, face)" in mining
assert "player.swing(InteractionHand.MAIN_HAND)" in mining
print("Mining attack lock source-contract checks passed")
