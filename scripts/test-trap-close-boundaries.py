#!/usr/bin/env python3
"""Native close observation must never turn local GUI absence into a general ownership exemption."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent/'src/main/java/dev/kuan/kuanperion'
b=(r/'AutomationClientBridge.java').read_text()
head=b.split('public void beforeServerClose(',1)[1].split('\n }',1)[0]
tail=b.split('public void afterServerClose(',1)[1].split('\n }',1)[0]
assert head.index('!mc.isSameThread()')<head.index('trapClose=null')
assert tail.index('!mc.isSameThread()')<tail.index('trapClose=null')
assert 'menu.containerId()!=packet.getContainerId()' in head and '!pest.ownsMenu()' in head
for guard in ['trapCloseWorld!=currentWorldId()','trapCloseId!=packet.getContainerId()','mc.gui.screen()!=null','mc.player.containerMenu!=mc.player.inventoryMenu']:
 assert guard in tail,guard
assert 'pest.afterServerClose(ticket,System.nanoTime())' in tail
for forbidden in ['.sendCommand(','.closeContainer(','.setScreen(','.handleContainerInput(','.finish(','clearExpectedMenu(']:assert forbidden not in head+tail,forbidden
m=(r/'mixin/AutomationReceiptMixin.java').read_text()
assert '@Inject(method="handleContainerClose",at=@At("HEAD"))' in m and '@Inject(method="handleContainerClose",at=@At("TAIL"))' in m
assert 'observeAutomationMenuClose(packet,true)' in m and 'observeAutomationMenuClose(packet,false)' in m
print('PestTrapCloseBoundariesTest: PASS (matching server HEAD + applied TAIL, client-thread/world/id/inventory identity, no local-close exemption, command/cursor/UI mutation)')
