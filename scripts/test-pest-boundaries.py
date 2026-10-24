#!/usr/bin/env python3
"""Guard this observation-only slice; future drivers must deliberately replace these gates."""
import json
from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion'
view=(main/'PestGameView.java').read_text()
identity=(main/'automation/pest/PestIdentity.java').read_text()
strip=lambda text:re.sub(r'/\*.*?\*/|//[^\n]*','',text,flags=re.S)
code=strip(view)+'\n'+strip(identity)
for forbidden in ['.resolveProfile(','.sendCommand(','.setSelectedSlot(','.setDown(','.setPos(','.setDeltaMovement(','.handleContainerInput(','HttpClient','URLConnection']:
    assert forbidden not in code,forbidden
assert 'profile.skinPatch()' in view and 'textures.size()==1' in view
assert 'facts.size()>=256' in view and 'inspected>4096' in view and '!mc.isSameThread()' in view
for path in main.rglob('*.java'):
    if path.name!='PestGameView.java':assert 'PestGameView.read(' not in path.read_text(),path
catalog=json.loads((root/'docs/data/pest-vacuum-catalog.json').read_text())
assert catalog['source']=='https://api.hypixel.net/v2/resources/skyblock/items'
assert all(item['category']=='VACUUM' for item in catalog['items'])
authorized=set(re.findall(r'"([A-Z_]+)"',identity.split('Set<String> VACUUMS=Set.of(',1)[1].split(');',1)[0]))
assert authorized=={item['id'] for item in catalog['items']}
print('PestBoundariesTest: PASS (on-demand read-only/no runtime caller, bounded native scan, no profile/network/actions, exact catalog IDs)')
