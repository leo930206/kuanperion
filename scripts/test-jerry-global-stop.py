#!/usr/bin/env python3
"""Wiring complement to actual service + composed legacy/bridge behavior tests."""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion'
bridge=(main/'AutomationClientBridge.java').read_text()
body=bridge.split('public void suspendForJerry(){',1)[1].split('}',1)[0]
expected={'PEST':'pest','GREENHOUSE':'greenhouse','COMPOSTER':'composter','VISITOR':'visitor','AUTO_SELL':'autoSell','JUNK':'junk','STASH':'stash','SUPERCRAFT':'supercraft','BOOK_COMBINE':'book'}
implemented=set(re.findall(r'this==([A-Z_]+)',(main/'automation/AutomationFeature.java').read_text().split('public boolean implemented()',1)[1]))
assert implemented==set(expected),'Every new executable service must join the Jerry global-stop matrix'
assert set(re.findall(r'(\w+)\.suspend\(',body))==set(expected.values())
for file in ['pest/PestService.java','greenhouse/GreenhouseService.java','composter/ComposterService.java','visitor/VisitorService.java','AutoSellService.java','JunkService.java','StashService.java','supercraft/SupercraftService.java','BookService.java']:
    service=(main/'automation'/file).read_text()
    suspend=service.split('public void suspend(',1)[1].split('}',1)[0]
    assert '=true;' in suspend and 'cancel(' in suspend and 'settings.' not in suspend,file
client=(main/'KuanperionClient.java').read_text().split('private static boolean handleJerryMessage(',1)[1].split('private static void tickTabNotifications(',1)[0]
assert 'if(jerryStopActionsEnabled&&automation!=null)automation.suspendForJerry();' in client
assert client.index('if(jerryStopActionsEnabled&&automation')>client.index('if (stoppedFarming) markJerryFarmingPause(mc);')
print('JerryGlobalStopWiringTest: PASS (all nine implemented services, idle runtime suspension, legacy beam ordering, future executable features require extending matrix)')
