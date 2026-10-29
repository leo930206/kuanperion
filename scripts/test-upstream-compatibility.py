#!/usr/bin/env python3
"""Provisional-source scope guard; controller behavior is covered by real compiled models/native fixtures."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion'
c=(m/'BazaarPurchaseController.java').read_text();v=(m/'automation/BazaarPurchaseView.java').read_text()
for forbidden in ['sendCommand(', 'handleContainerInput(', 'closeContainer(', 'new Hypixel', 'Thread.sleep(', 'session.begin(', 'session.finish(', 'coordinator.clear(', 'setEnabled(', 'setScreen(']:
 assert forbidden not in c+v,forbidden
for required in ['AutomationMenuActions.purchaseClick(', 'p.request().usesBudget(p.budget())', 'p.plan().stillMatches(', 'coordinator.holdsIdentity(', 'items.after(', 'cursors.emptyAfterOpening(', 'game.menuIdentity()!=menuIdentity', 'game.level()!=level', 'attempt.send(', 'attempt.poll(']:assert required in c,required
assert 'sendNative(' not in c and 'System::nanoTime' in c
assert not [p for p in m.rglob('*.java') if p.name!='BazaarPurchaseController.java' and 'new BazaarPurchaseController(' in p.read_text()], 'Parent wiring is still pending, do not silently expose a partial purchase workflow'
assert 'src/test/java/dev/kuan/kuanperion/automation/BazaarPurchaseViewTest.java' in (r/'scripts/ui-smoke/prepare.py').read_text(), 'Isolated full-smoke compile must include its explicit test-only Bazaar data fixture'
registry=(r/'docs/upstream-compatibility-2.1.0.md').read_text();spec=(r/'docs/spec-2.1.0.md').read_text()
for key in ['U-BZ-BUY','U-BZ-PRICE','U-GEORGE','U-BZ-SELL','U-BAIT','U-STRATEGY','U-EXCHANGE-SPRAY','U-COMPOSTER','U-SUPERCRAFT']:assert key in registry,key
assert '904a32484806b4d19a605d9a21503b9188324a80cafcbbf4f82b8d39c3313868' in registry
assert 'implement first, batch validation last' in spec and 'provisional' in registry
assert 'additional provisional adapter assumption' in registry
print('UpstreamCompatibility: PASS (user-approved implement-first/source-marked deferred batch evidence; guarded native terminal buyer remains unwired, no author/runtime/screen/config/root effects)')
