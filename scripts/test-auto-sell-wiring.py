#!/usr/bin/env python3
from pathlib import Path
import json
r=Path(__file__).resolve().parent.parent;src=r/'src/main/java/dev/kuan/kuanperion'
b=(src/'AutomationClientBridge.java').read_text();c=(src/'KuanperionClient.java').read_text();a=(src/'AutomationMenuActions.java').read_text();m=(src/'mixin/AutomationInventoryInputMixin.java').read_text()
assert 'autoSell=new AutoSellService(settings,session,receipts,' in b
assert 'autoSell.tick(now)' in b and 'autoSell.onSystemMessage(text,now)' in b
assert 'if(ownGameplayCommand){observedGameplayCommand=true;return;}' in b
assert 'command!=AutomationCommand.BOOSTER_COOKIE' in b and 'handle.lease().owner()!=AutomationFeature.AUTO_SELL' in b
assert 'AutomationModuleSettings.trade(automation.stash(),automation.autoSell(),automation.junk())' in c
assert 'rules.permits(ticket.observed().item(ticket.slot()))' in a and 'finally{ownedInput=false;}' in a
assert 'if(!AutomationMenuActions.ownsInventoryInput())' in m and 'at=@At("HEAD")' in m and 'cancellable' not in m
assert 'AutomationInventoryInputMixin' in json.loads((r/'src/main/resources/kuanperion.mixins.json').read_text())['client']
assert 'session.isCurrent(handle))port.closeOwnedMenu' in (src/'automation/AutoSellTask.java').read_text()
print('AutoSellWiringTest: PASS (real service/menu command/typed protected actions, manual-input priority without swallowing user input, no stale GUI close)')
