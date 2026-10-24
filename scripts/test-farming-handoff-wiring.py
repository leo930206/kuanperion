#!/usr/bin/env python3
from pathlib import Path
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
assert 'session.cancel(System.nanoTime())' in b,'Manual commands must cancel active gameplay task'
assert 'new AutomationSession(' in b and 'session.invalidate();' in b
assert 'farmingEnabled = true' not in b and 'toggleFarming' not in b,'Task may not arm Farming'
print('FarmingHandoffWiringTest: PASS (hold bypasses legacy inputs/stall; stop/new session/world invalidates; no automatic enable)')
