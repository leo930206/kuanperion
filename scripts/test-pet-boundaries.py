#!/usr/bin/env python3
"""Pet metadata eligibility and native projections are read-only, not a disposal driver."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion'
pure=(m/'automation/pets/PetSalePreflight.java').read_text();view=(m/'PetGameView.java').read_text();feature=(m/'automation/AutomationFeature.java').read_text().split('public boolean implemented()',1)[1]
for forbidden in ['HttpClient','URLConnection','Socket(','Minecraft.getInstance','sendCommand(','.handleContainerInput(','.resolveProfile(','.setCarried(','.setDown(','.setPos(','.setSelectedSlot(','UUID.randomUUID','Thread.sleep','Files.']:
 assert forbidden not in pure+view,forbidden
assert 'this==PET_SELL' not in feature,'Eligibility without menu/price/transfer/confirmation proof must not expose a no-op or unsafe toggle'
for needed in ['inv.size()!=43','p.slot()>=9&&p.slot()<36','!p.item().protectedItem()','!p.metadata().protectedMetadata()','rules.protectedUuids().contains','!ids.add(p.metadata().uuid())','private Plan(','reader.setStrictness(Strictness.STRICT)','o.has(k)','petSoulbound','noMove','heldItemUuid','uniqueId']:
 assert needed in pure,needed
for needed in ['custom.copyTag()','a.get("petInfo") instanceof StringTag','allowed.containsAll(a.keySet())','PetSalePreflight.metadata(a.getStringOr("petInfo",""),item.uuid())']:
 assert needed in view,needed
for forbidden in ['.put(','.putString(','.set(','.remove(']:assert forbidden not in view,forbidden
print('PetBoundariesTest: PASS (strict read-only typed metadata/raw native projection, complete protected43, no guessing/fetch/profile/action or unfinished Pet Sell toggle)')
