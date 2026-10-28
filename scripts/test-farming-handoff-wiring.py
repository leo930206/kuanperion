#!/usr/bin/env python3
from pathlib import Path
import re
r=Path(__file__).resolve().parent.parent
s=(r/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
b=(r/'src/main/java/dev/kuan/kuanperion/AutomationClientBridge.java').read_text()
assert 'automation.beforeLegacyTick()' in s
assert 'if (!isAutomationHoldingFarming()) {' in s
assert 'if (isAutomationHoldingFarming()) return;' in s
stop=s.split('private static void stopFarming(',1)[1].split('private static void resetFarmingRuntime',1)[0]
assert 'invalidateAutomationFarmSession();' in stop
start=s.split('private static void toggleFarming(',1)[1].split('private static void stopFarming',1)[0]
assert 'invalidateAutomationFarmSession();' in start
def body(name):
    match=re.search(r' public void '+name+r'\([^)]*\)\{',b);assert match,name
    end=match.end();depth=1
    while depth:
        depth+=(b[end]=='{')-(b[end]=='}');end+=1
    return b[match.start():end]
for name in ['onManualInventoryInput','onManualLook','onOutgoingCommand']:
    code=body(name)
    assert 'AutomationStops.run(' in code and '()->session.cancel(now)' in code,name
    assert code.index('()->session.cancel(now)')<code.index('()->autoSell.cancel(now)')<code.index('()->junk.cancel(now)'), 'Manual takeover must revoke before Trade cleanup: '+name
for name in ['invalidateFarmSession','worldChanged','shutdown']:
    assert 'AutomationStops.run(' in body(name) and '()->session.invalidate()' in body(name),name
assert 'new AutomationSession(' in b
assert 'farmingEnabled = true' not in b and 'toggleFarming' not in b,'Task may not arm Farming'
print('FarmingHandoffWiringTest: PASS (hold bypasses legacy inputs/stall; stop/new session/world invalidates; no automatic enable)')
