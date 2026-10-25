#!/usr/bin/env python3
"""Fixed inventory-backed consumer; native gameplay and anonymous on-demand data remain separate."""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion'
strip=lambda text:re.sub(r'/\*.*?\*/|//[^\n]*','',text,flags=re.S)
view=(main/'VisitorGameView.java').read_text();protocol='\n'.join(p.read_text() for p in (main/'automation/visitor').glob('*.java'))
for forbidden in ['sendCommand(','.handleContainerInput(','.setDown(','.setSelectedSlot(','.setPos(','.resolveProfile(','HttpClient','URLConnection','Thread.sleep','CompletableFuture','FabricLoader']:
 assert forbidden not in strip(view+protocol),forbidden
assert 'mc.isSameThread()' in view and 'getContainerSize()!=43' in view and 'getNonEquipmentItems().size()!=36' in view and 'i<9||i>=36' in view
assert 'inspected>4096' in view and 'facts.size()>=256' in view and 'ChunkStatus.FULL,false' in view and 'getListedOnlinePlayers()' in view and 'VisitorTargets.select(queue,facts)' in view
feature=(main/'automation/AutomationFeature.java').read_text().split('public boolean implemented()',1)[1];assert 'this==VISITOR' in feature
controller=(main/'VisitorFlightController.java').read_text();bridge=(main/'AutomationClientBridge.java').read_text();client=(main/'KuanperionClient.java').read_text()
for required in ['new GardenFlightCore(coordinator,session,observe,AutomationFeature.VISITOR)','targets(queue())','target::equals','entity.getUUID().equals(target.uuid())','flight.loaded(','ClipContext.Block.COLLIDER','mc.gameMode.interact(','VisitorOffer.inspect(','offer.get().acceptSlot()!=ticket.slot()','AutomationMenuActions.purchaseClick(']:assert required in controller,required
for forbidden in ['.useItem(','.attack(','.startDestroyBlock(','.sendCommand(','.setPos(','.setDeltaMovement(','.handleContainerInput(','.resolveProfile(','Thread.sleep','whenComplete(','.thenAccept(']:assert forbidden not in strip(controller),forbidden
assert 'public VisitorService visitor()' in bridge and 'visitor.tick(now)' in bridge and 'visitor.onSystemMessage(currentWorldId(),text)' in bridge
for name in ['invalidateFarmSession','onOutgoingCommand','onManualInventoryInput','worldChanged','onManualLook']:
 body=bridge.split('public void '+name+'(',1)[1].split('\n ',1)[0];assert 'visitor.cancel(' in body,name
for phase in ['before','after']:
 body=bridge.split('private void '+phase+'VisitorServerClose(',1)[1].split('\n }',1)[0]
 assert '!mc.isSameThread()' in body
 assert phase+'VisitorServerClose(packet)' in bridge.split('public void '+phase+'ServerClose(',1)[1].split('\n }',1)[0]
 for forbidden in ['.sendCommand(','.closeContainer(','.setScreen(','.finish(','clearExpectedMenu(']:assert forbidden not in body
assert 'visitorCloseWorld!=currentWorldId()' in bridge and 'visitorCloseId!=packet.getContainerId()' in bridge
assert 'visitorFlight.shutdownData()' in bridge.split('public void shutdown()',1)[1]
assert 'automation.shutdown()' in client and 'VisitorModuleSettings.tab(automation.visitor(),visitorNotifications)' in client
assert 'new ToggleSetting("啟用 Visitor 通知"' in client
assert 'AutomationVisitorViewTest.java' in (root/'scripts/test-native-observations.py').read_text()
print('VisitorBoundariesTest: PASS (bounded43-slot/dual identity, fixed inventory-only task/native/UI, independent notification/stop/lifecycle/scoped-close guards; no buy/guess/retry/telemetry)')
