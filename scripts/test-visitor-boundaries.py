#!/usr/bin/env python3
"""Read-only/protocol scope: no unfinished Visitor caller or author-side service imported."""
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion'
view=(main/'VisitorGameView.java').read_text()
protocol='\n'.join(p.read_text() for p in (main/'automation/visitor').glob('*.java'))
strip=lambda text:re.sub(r'/\*.*?\*/|//[^\n]*','',text,flags=re.S)
for forbidden in ['sendCommand(','.handleContainerInput(','.setDown(','.setSelectedSlot(','.setPos(','.resolveProfile(','HttpClient','URLConnection','Thread.sleep','CompletableFuture','FabricLoader']:
    assert forbidden not in strip(view+protocol),forbidden
assert 'mc.isSameThread()' in view and 'getContainerSize()!=43' in view and 'getNonEquipmentItems().size()!=36' in view
assert 'i<9||i>=36' in view
assert 'inspected>4096' in view and 'facts.size()>=256' in view and 'ChunkStatus.FULL,false' in view
assert 'getListedOnlinePlayers()' in view and 'VisitorTargets.select(queue,facts)' in view
feature=(main/'automation/AutomationFeature.java').read_text().split('public boolean implemented()',1)[1]
assert 'this==VISITOR' not in feature,'No executable Visitor driver yet; do not expose placeholder auto toggle'
for p in main.rglob('*.java'):
    if p.name!='VisitorGameView.java':assert 'VisitorGameView.offer(' not in p.read_text(),p
assert 'AutomationVisitorViewTest.java' in (root/'scripts/test-native-observations.py').read_text()
print('VisitorBoundariesTest: PASS (bounded read-only43-slot native view, pure offer/value/submission contracts, no hidden action/network/startup caller or unfinished auto toggle)')
