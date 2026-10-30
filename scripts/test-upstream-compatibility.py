#!/usr/bin/env python3
"""Provisional-source scope guard; controller behavior is covered by real compiled models/native fixtures."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion'
c=(m/'BazaarPurchaseController.java').read_text();v=(m/'automation/BazaarPurchaseView.java').read_text()
for forbidden in ['sendCommand(', 'handleContainerInput(', 'closeContainer(', 'new Hypixel', 'Thread.sleep(', 'session.begin(', 'session.finish(', 'coordinator.clear(', 'setEnabled(', 'setScreen(']:
 assert forbidden not in c+v,forbidden
for required in ['AutomationMenuActions.purchaseClick(', 'p.request().usesBudget(p.budget())', 'p.plan().stillMatches(', 'coordinator.holdsIdentity(', 'items.after(', 'cursors.emptyAfterOpening(', 'game.menuIdentity()!=menuIdentity', 'game.level()!=level', 'attempt.send(', 'attempt.poll(']:assert required in c,required
assert 'sendNative(' not in c and 'System::nanoTime' in c
for required in ['attempt.confirm(', 'attempt.canConfirm()', 'items.after(afterFirstSend,', 'BazaarPurchaseView.confirmation(', 'confirmedMenu()']:assert required in c,required
assert 'budget.reserve(' not in c and 'new BazaarPurchaseLedger(' not in c

assert [p.name for p in m.rglob('*.java') if p.name!='BazaarPurchaseController.java' and 'new BazaarPurchaseController(' in p.read_text()]==['BazaarAcquisitionController.java'], 'Only the guarded original-parent navigation workflow may invoke terminal purchases'
f=(m/'BazaarAcquisitionController.java').read_text();visitor=(m/'VisitorFlightController.java').read_text();bridge=(m/'AutomationClientBridge.java').read_text()
for required in ['plan.stillMatches(', 'request.usesBudget(', 'applied(menu,now)', 'session.expectScreenSequence(', 'terminal.onSystemMessage(', 'terminal.poll(', 'game.purse()', 'game.amount(signIdentity,request.quantity())', 'game.menuIdentity()==originIdentity']:
 assert required in f,required
for forbidden in ['session.begin(', 'session.finish(', 'coordinator.clear(', 'setEnabled(', 'Thread.sleep(', 'new Hypixel', '.sendCommand(', '.handleContainerInput(']:assert forbidden not in f,forbidden
assert 'new BazaarAcquisitionController(' in visitor and 'acquisitionBudget' in visitor and 'acquisitionPolicy.buyingRules()' in visitor
assert 'this::sendBazaarAcquisition' in bridge and 'BazaarPurse.read(sidebar.apply(' in bridge
composter=(m/'ComposterFlightController.java').read_text()
assert 'new BazaarAcquisitionController(' in composter and 'acquisitionPolicy.buying().rules(' in composter
assert 'composter.onSystemMessage(currentWorldId(),text)' in bridge
callers={p.name for p in m.rglob('*.java') if p.name!='BazaarAcquisitionController.java' and 'new BazaarAcquisitionController(' in p.read_text()}
assert callers=={'VisitorFlightController.java','ComposterFlightController.java','PestFlightController.java','PestSprayController.java'},'Only implemented guarded original-parent workflows may navigate purchases'
pest=(m/'PestFlightController.java').read_text()
for required in ['baitAcquisitionCurrent(h)','budget.limit()!=policy.buying().maximumCoins()','baitBuyingPolicy.buying().rules(','baitBuyingBudget','baitAcquisition.onSystemMessage(world,text)']:assert required in pest,required
assert 'pest.onSystemMessage(currentWorldId(),text)' in bridge


assert 'InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER' in (m/'AutomationMenuActions.java').read_text()
assert 'AutomationSignInputMixin' in (r/'src/main/resources/kuanperion.mixins.json').read_text()

assert 'src/test/java/dev/kuan/kuanperion/automation/BazaarPurchaseViewTest.java' in (r/'scripts/ui-smoke/prepare.py').read_text(), 'Isolated full-smoke compile must include its explicit test-only Bazaar data fixture'
registry=(r/'docs/upstream-compatibility-2.1.0.md').read_text();spec=(r/'docs/spec-2.1.0.md').read_text()
for key in ['U-BZ-BUY','U-BZ-PRICE','U-GEORGE','U-BZ-SELL','U-BAIT','U-STRATEGY','U-EXCHANGE-SPRAY','U-COMPOSTER','U-SUPERCRAFT']:assert key in registry,key
assert '904a32484806b4d19a605d9a21503b9188324a80cafcbbf4f82b8d39c3313868' in registry
assert 'implement first, batch validation last' in spec and 'provisional' in registry
assert 'additional provisional adapter assumption' in registry
print('UpstreamCompatibility: PASS (user-approved implement-first/source-marked deferred batch evidence; source-marked guarded Visitor native acquisition route; no author services, default-off explicit budgets/materials, bounded original-root screen ownership)')

spray=(m/'PestSprayController.java').read_text()
for required in ['sprayAcquisitionCurrent(h,now)','budget.limit()!=purchase.maximumCoins()','buying.rules(buyPolicy)','buyBudget','acquisition.onSystemMessage(world,text)']:assert required in spray,required
