#!/usr/bin/env python3
"""Only server packet TAIL observations may produce automation position evidence; no movement effects."""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion'
view=(main/'AutomationPositionView.java').read_text()
bridge=(main/'AutomationClientBridge.java').read_text()
client=(main/'KuanperionClient.java').read_text()
mixin=(main/'mixin/AutomationReceiptMixin.java').read_text()
code=re.sub(r'/\*.*?\*/|//[^\n]*','',view,flags=re.S)
for forbidden in ['player.position(','.getX(','.setPos(','.setDeltaMovement(','.sendCommand(','HttpClient','resolveProfile']:
    assert forbidden not in code,forbidden
assert 'packet.change().position()' in view
assert all('Relative.'+axis in view for axis in ['X','Y','Z'])
assert '@Inject(method="handleMovePlayer",at=@At("TAIL"))' in mixin
assert 'kuanperion$position' in mixin and 'observeAutomationPosition(packet)' in mixin
assert 'AutomationPositionView.absolute(packet)' in bridge and 'positions.observe(currentWorldId(),packet.id(),point,point!=null,System.nanoTime())' in bridge
for name in ['worldChanged','shutdown']:
    match=re.search(r' public void '+name+r'\(\)\{',bridge);assert match,name
    end=match.end();depth=1
    while depth:
        depth+=(bridge[end]=='{')-(bridge[end]=='}');end+=1
    cleanup=bridge[match.start():end]
    assert 'AutomationStops.run(' in cleanup,name
    assert cleanup.index('()->session.invalidate()')<cleanup.index('()->receipts.clear()')<cleanup.index('()->positions.clear()'),name
method=client.split('public static void observeAutomationPosition(',1)[1].split('public static void observeAutomationSlot(',1)[0]
assert 'mc.isSameThread()' in method and 'mc.player==null' in method and 'mc.level==null' in method
for path in main.rglob('*.java'):
    if path.name!='AutomationClientBridge.java':assert 'positions.observe(' not in path.read_text(),path
assert 'AutomationLocation.inspect(lines,flags.island())' in bridge
assert 'flags.island()!=null||' not in bridge and 'contains("garden")' not in bridge
print('PositionBoundariesTest: PASS (server packet TAIL only, no predicted-position receipts/effects/network, native-world proof, world clear)')
