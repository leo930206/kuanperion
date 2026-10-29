#!/usr/bin/env python3
"""Pet metadata eligibility and native projections are read-only, not a disposal driver."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion'
pure=(m/'automation/pets/PetSalePreflight.java').read_text();view=(m/'PetGameView.java').read_text();attempt=(m/'automation/pets/PetSaleAttempt.java').read_text();feature=(m/'automation/AutomationFeature.java').read_text().split('public boolean implemented()',1)[1]
for forbidden in ['HttpClient','URLConnection','Socket(','Minecraft.getInstance','sendCommand(','.handleContainerInput(','.resolveProfile(','.setCarried(','.setDown(','.setPos(','.setSelectedSlot(','UUID.randomUUID','Thread.sleep','Files.']:
 assert forbidden not in pure+view+attempt,forbidden
assert 'this==PET_SELL' in feature,'Connected native task/service/Trade route must exist before exposure'
for needed in ['inv.size()!=43','p.slot()>=9&&p.slot()<36','!p.item().protectedItem()','!p.metadata().protectedMetadata()','rules.protectedUuids().contains','!ids.add(p.metadata().uuid())','private Plan(','reader.setStrictness(Strictness.STRICT)','o.has(k)','petSoulbound','noMove','heldItemUuid','uniqueId']:
 assert needed in pure,needed
for needed in ['custom.copyTag()','a.get("petInfo") instanceof StringTag','allowed.containsAll(a.keySet())','PetSalePreflight.metadata(a.getStringOr("petInfo",""),item.uuid())']:
 assert needed in view,needed
for forbidden in ['.put(','.putString(','.set(','.remove(']:assert forbidden not in view,forbidden
for file in m.rglob('*.java'):
 if file.name not in {'PetSaleAttempt.java','PetSaleCustody.java','GeorgeSaleView.java','PetGeorgeTask.java','PetGeorgeController.java'}:assert 'PetSaleAttempt' not in file.read_text(), 'Unexpected direct pet transaction caller: '+str(file.relative_to(m))
for forbidden in ['session.finish(', 'session.cancel(', 'session.begin(', 'coordinator.release(', 'closeOwnedMenu(']:assert forbidden not in attempt,forbidden
native=(m/'PetGeorgeController.java').read_text();bridge=(m/'AutomationClientBridge.java').read_text();task=(m/'automation/pets/PetGeorgeTask.java').read_text()
assert 'exactPet(pet,' in native and 'nativeMenu' in native and 'pet.slot()>=36' in native
assert 'new PetSaleService(' in bridge and 'this::sendPetGeorge' in bridge and 'pets.onSystemMessage(' in bridge
assert 'new PetSaleCustody(' in task and 'sale.startCustody(' in task and 'sale.continueCustody(' in task and 'returnItems' in task
print('PetBoundariesTest: PASS (strict read-only typed metadata/raw native projection, complete protected43, no guessing/fetch/profile in projections; guarded native George task/service/Trade route, not live acceptance)')
