#!/usr/bin/env python3
"""Recipe preflight/receipts are not a native driver, cost proof or authority to click."""
from pathlib import Path
import re
r=Path(__file__).resolve().parent.parent/'src/main/java/dev/kuan/kuanperion'
source='\n'.join(p.read_text() for p in (r/'automation/supercraft').glob('*.java'))
code=re.sub(r'/\*.*?\*/|//[^\n]*','',source,flags=re.S)
for forbidden in ['Minecraft','HttpClient','URLConnection','sendCommand(','.handleContainerInput(','.setDown(','.setPos(','.setSelectedSlot(','.resolveProfile(','Thread.sleep','CompletableFuture','FabricLoader','Supplier<GuardedMenuAction','QUICK_MOVE']:
 assert forbidden not in code,forbidden
assert 'private Plan(' in code and 'static boolean complete(' in code and 'inventory.size()!=43' in code
assert 'outputCount%recipe.outputPerCraft()!=0' in code and 'slot<9||slot>=36' in code
assert 'SupercraftPlan.stillMatches(plan,current)' in code and 'state!=State.ARMED' in code
assert 'stamp=items.stamp(menu.worldId());cursorStamp=cursors.stamp(menu.worldId(),menu.containerId(),now)' in code
feature=(r/'automation/AutomationFeature.java').read_text().split('public boolean implemented()',1)[1]
assert 'this==SUPERCRAFT' not in feature,'No actual recipe/quantity/cost/native driver yet: never expose a placeholder toggle'
for p in r.rglob('*.java'):
 if 'supercraft' not in p.parts:assert 'new SupercraftReceipts(' not in p.read_text(),p
print('SupercraftBoundariesTest: PASS (read-only exact quantity/protected inventory/one-attempt receipt contracts, no GUI/cost assumption, native caller, action/network or unfinished toggle)')
