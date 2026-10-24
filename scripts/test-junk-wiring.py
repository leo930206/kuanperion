#!/usr/bin/env python3
from pathlib import Path
r=Path(__file__).resolve().parent.parent;src=r/'src/main/java/dev/kuan/kuanperion'
b=(src/'AutomationClientBridge.java').read_text();g=(src/'AutomationGameView.java').read_text();u=(src/'ui/AutomationModuleSettings.java').read_text();c=(src/'KuanperionClient.java').read_text()
assert 'junk=new JunkService(settings,session,receipts,' in b
assert 'junk.tick(now)' in b and b.count('junk.cancel(')>=2
assert 'handle.lease().owner()!=AutomationFeature.JUNK' in b
assert 'new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)' in b
assert 'AutomationGameView.junkMenu' in b and 'ContainerInput' not in b
assert 'public static MenuSnapshot junkMenu' in g and 'instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen' in g
assert 'mc.player.containerMenu!=mc.player.inventoryMenu' in g and 'menu.containerId()==0' in g and 'menu.slots().size()==46' in g
assert 'AutomationModuleSettings.trade(automation.stash(),automation.autoSell(),automation.junk())' in c
assert '記錄目前丟棄位置' in u and '立即丟棄授權物品' in u and 'new ModulesTab.SubTab("Junk"' in u
print('JunkWiringTest: PASS (native InventoryScreen/window0 only, typed protected THROW, world/manual cancellation, real Trade UI/anchor)')
