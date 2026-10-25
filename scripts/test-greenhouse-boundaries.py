#!/usr/bin/env python3
"""Read-only Greenhouse observations; no feature action is granted by a texture or a packet signal."""
from pathlib import Path
import re,json
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion';strip=lambda s:re.sub(r'/\*.*?\*/|//[^\n]*','',s,flags=re.S)
view=(m/'GreenhouseGameView.java').read_text();packet=(m/'GreenhousePacketView.java').read_text();scan=(m/'automation/greenhouse/GreenhouseScan.java').read_text();bridge=(m/'AutomationClientBridge.java').read_text();mix=(m/'mixin/AutomationReceiptMixin.java').read_text()
for bad in ['resolveProfile(','.setSelectedSlot(','.setDown(','.setYRot(','.startDestroyBlock(','.attack(','.sendCommand(','HttpClient','URLConnection','.getBlockEntity(']:assert bad not in strip(view)+strip(packet),bad
assert view.count('.getChunk(')==view.count('ChunkStatus.FULL,false') and 'getBlockEntities()' in view
assert 'inspected>4096' in view and 'inspected>16384' in view and 'nodes.size()>8192' in view
assert 'budget<64' in scan and 'queries>8192' in scan and 'targets.size()>=1024' in scan and '!source.loaded()' in scan
assert 'count[0]>4096' in packet and 'heads==1' in packet
for handler in ['handleRemoveEntities','handleSetEquipment','handleBlockUpdate','handleBlockEntityData','handleChunkBlocksUpdate']:assert '@Inject(method="'+handler+'",at=@At("TAIL"))' in mix
assert 'harvests.clear()' in bridge and 'GreenhouseGameView.applied(mc,watched.get(),packet)' in bridge
for path in m.rglob('*.java'):
 code=strip(path.read_text())
 if path.name!='AutomationReceiptMixin.java':assert 'KuanperionClient.observeAutomationHarvest(' not in code,path
 if path.name!='AutomationClientBridge.java':assert 'harvests.entityChanged(' not in code and 'harvests.blockChanged(' not in code,path
 if path.name!='AutomationClientBridge.java':assert 'GreenhouseGameView.applied(' not in code,path
 if path.name!='GreenhouseGameView.java':assert 'GreenhouseGameView.scan(' not in code,'Discovery has no executable task caller until its module is wired'
identity=(m/'automation/greenhouse/GreenhouseIdentity.java').read_text();pest=(m/'automation/pest/PestIdentity.java').read_text()
hashes=set(re.findall(r'"([0-9a-f]{64})"',identity));catalog=json.loads((r/'docs/data/greenhouse-textures.json').read_text())
assert hashes==set(catalog['textures'].values()) and len(hashes)==3
assert not hashes.intersection(re.findall(r'"([0-9a-f]{32,64})"',pest)),'Greenhouse/Pest identity sets must not overlap'
assert 'TextureIdentity.hash(encoded).filter(TEXTURES::contains)' in pest
print('GreenhouseBoundariesTest: PASS (strict disjoint identities, no-load bounded discovery, exact server TAIL/watch signals only, no attack/movement/network/prediction/unfinished UI caller)')
