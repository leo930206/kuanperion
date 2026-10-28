#!/usr/bin/env python3
"""A cursor snapshot is not a receipt without the native server packet boundary."""
from pathlib import Path
import re
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion'
strip=lambda t:re.sub(r'/\*.*?\*/|//[^\n]*','',t,flags=re.S)
mixin=(m/'mixin/AutomationReceiptMixin.java').read_text();bridge=(m/'AutomationClientBridge.java').read_text();client=(m/'KuanperionClient.java').read_text();view=(m/'AutomationCursorView.java').read_text();ledger=(m/'automation/CursorReceipts.java').read_text()
assert '@Inject(method="handleSetCursorItem",at=@At("TAIL"))' in mixin
assert 'observeAutomationCursor(packet)' in mixin and 'observeAutomationContainerCursor(packet)' in mixin
assert 'packet.containerId()!=mc.player.containerMenu.containerId' in bridge
assert 'AutomationCursorView.applied(server,menu.getCarried())' in bridge
assert 'cursors.observe(currentWorldId(),menu.containerId,item,System.nanoTime())' in bridge
for name in ['worldChanged','shutdown']:
 match=re.search(r' public void '+name+r'\(\)\{',bridge);assert match,name
 end=match.end();depth=1
 while depth:
  depth+=(bridge[end]=='{')-(bridge[end]=='}');end+=1
 cleanup=bridge[match.start():end]
 assert 'AutomationStops.run(' in cleanup,name
 assert cleanup.index('()->session.invalidate()')<cleanup.index('()->positions.clear()')<cleanup.index('()->cursors.clear()'),name
for path in m.rglob('*.java'):
 code=strip(path.read_text())
 if path.name!='AutomationClientBridge.java':assert 'cursors.observe(' not in code,path
 if path.name!='AutomationReceiptMixin.java':
  assert 'KuanperionClient.observeAutomationCursor(' not in code and 'KuanperionClient.observeAutomationContainerCursor(' not in code,path
 if path.name!='AutomationClientBridge.java':assert 'AutomationCursorView.applied(' not in code,path
for forbidden in ['setCarried(','.setCount(','.handleContainerInput(','.sendCommand(','HttpClient','Files.']:
 assert forbidden not in strip(view)+strip(ledger),forbidden
for name in ['observeAutomationCursor','observeAutomationContainerCursor']:
 method=client.split('public static void '+name+'(',1)[1].split('\n    }',1)[0]
 assert '!mc.isSameThread()' in method and 'mc.player==null' in method and 'mc.level==null' in method and 'packet==null' in method
assert 'fromServer.equals(current)?fromServer:null' in view
print('CursorBoundariesTest: PASS (server packet TAIL only, current-container/applied-state match, world clear, no local-prediction receipt or gameplay/persistence/network effects)')
