#!/usr/bin/env python3
"""Declared bait protocol is not a live GUI/capacity proof or permission to bypass the native empty-cursor gate."""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion';package=main/'automation/pest'
protocol=(package/'PestBaitTransfer.java').read_text();task=(package/'PestBaitTask.java').read_text();code=re.sub(r'/\*.*?\*/|//[^\n]*','','\n'.join([protocol,task]),flags=re.S)
for forbidden in ['Minecraft','HttpClient','URLConnection','sendCommand(','.handleContainerInput(','.setDown(','.setPos(','.setSelectedSlot(','Thread.sleep','QUICK_MOVE','Click.THROW','session.begin(','session.finish(','safeStopFarm','restoreCursor']:
 assert forbidden not in code,forbidden
assert 'private Plan(' in protocol and 'Map.copyOf(inv)' in protocol and 'inv.size()!=43' in protocol
assert 'rules.units()==Math.min(item.count(),layout.remainingCapacity())' in protocol
assert 'i=9;i<36' in protocol and 'item.uuid().isEmpty()' in protocol and 'protection.permits(item)' in protocol
assert 'sourceReceipt(0)' in protocol and 'cursors.after(cursorStamp,now,WINDOW)' in protocol and 'cursors.emptyAfter(cursorStamp,now,WINDOW)' in protocol
assert 'items.stamp(plan.menu.worldId())' in protocol and 'state!=State.ARMED' in protocol
assert 'traps.observedAt()>depositAt' in protocol and 'confirmedStillMatches' in task
ready=task.split('if(proof.state()==PestBaitTransfer.State.READY){',1)[1]
assert ready.index('port.layout(target,menu).filter(plan.layout()::equals)')<ready.index('proof.armNext(')<ready.index('port.transfer(')
assert task.count('port.transfer(')==1 and task.count('port.closeOwnedMenu(')==1
for file in main.rglob('*.java'):
 if file.name not in {'PestBaitTask.java','PestBaitTransfer.java'}:
  source=file.read_text();assert 'new PestBaitTask(' not in source and 'PestBaitTransfer.' not in source,file
native=(main/'AutomationMenuActions.java').read_text();assert '!mc.player.containerMenu.getCarried().isEmpty()' in native
assert 'PestBait' not in native,'Do not widen generic native actions with an unproven carried-item/material/capacity path'
print('PestBaitBoundariesTest: PASS (declared direct-item/capacity data only, explicit protected43/server receipt protocol and original-root subtask; no native action/cursor bypass/live layout or unfinished toggle)')
