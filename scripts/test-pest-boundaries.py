#!/usr/bin/env python3
"""Guard read-only observations and the single leased native Pest driver."""
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
    if path.name not in {'PestGameView.java','PestFlightController.java'}:assert 'PestGameView.read(' not in path.read_text(),path
catalog=json.loads((root/'docs/data/pest-vacuum-catalog.json').read_text())
assert catalog['source']=='https://api.hypixel.net/v2/resources/skyblock/items'
assert all(item['category']=='VACUUM' for item in catalog['items'])
authorized=set(re.findall(r'"([A-Z_]+)"',identity.split('Set<String> VACUUMS=Set.of(',1)[1].split(');',1)[0]))
assert authorized=={item['id'] for item in catalog['items']}
driver=(main/'PestFlightController.java').read_text()
core=(main/'GardenFlightCore.java').read_text()
movement=driver+'\n'+core
bridge=(main/'AutomationClientBridge.java').read_text()
client=(main/'KuanperionClient.java').read_text()
for forbidden in ['.setPos(','.setDeltaMovement(','.noPhysics','setFlyingSpeed','mayfly=','mayfly =','HttpClient','sendCommand(']:
    assert forbidden not in strip(movement),forbidden
assert 'session.isCurrent(h)' in driver and 'coordinator.permits(' in core and 'manualInput()' in driver
assert 'getAbilities().mayfly' in core and 'hasChunkAt(' in core and 'noCollision(p,swept)' in core
assert 'mc.gameMode.useItem(p,InteractionHand.MAIN_HAND)' in driver and '250_000_000L' in driver
assert 'bounds=new PestFlightBounds(anchor.position(),radius)' in core and '!bounds.permits(from,to)' in core
assert 'public void dispose(' in driver and 'physical(key,false)' in core
assert 'pest.tick(now)' in bridge and 'pest.cancel(System.nanoTime())' in bridge
assert 'beforeMovementTick()' in client and 'PestModuleSettings.tab(' in client
assert '啟用 Pest 通知' in client and '()->pestNotificationEnabled' in client
assert 'onManualAutomationLook' in (main/'mixin/EntityTurnMixin.java').read_text()
print('PestBoundariesTest: PASS (read-only bounded observations, one leased legal-flight controller, no movement packets/velocity/teleport/network, physical priority, independent notification flag)')

trap=(main/'automation/pest/PestTrapRelease.java').read_text()
policy=(main/'automation/AutomationSettingsStore.java').read_text()
session=(main/'automation/AutomationSession.java').read_text()
assert 'mc.gameMode.interact(p,stand,new EntityHitResult(stand,aim),InteractionHand.MAIN_HAND)' in driver
assert 'mc.player.containerMenu.getCarried().isEmpty()' in driver and 'PestTrapTarget.unique(traps)' in view
assert 'GuardedMenuAction.Space.REQUIRE_FREE_SLOT,this::context,this::menu,now' in driver
assert 'buttons.size()!=1' in trap and 'buttons.getFirst().index()' in trap and '!slot.playerInventory()' in trap
assert 'facts.alive()>baseline' in trap and '!facts.traps().full().contains(target.trapId())' in trap
assert 'terminal!=State.CANCELLED' in trap and 'session.clearExpectedMenu(handle)' in trap
assert 'public void clearExpectedMenu(Handle handle){if(!isCurrent(handle))return;' in session
assert '!context.canIssueCommand()&&!pest.ownsMenu()' in bridge and 'var context=observe();' in bridge
assert 'strictBoolean(s,"clearTraps",false)' in policy
catalog=json.loads((root/'docs/data/pest-trap-catalog.json').read_text())
assert catalog['source']=='https://api.hypixel.net/v2/resources/skyblock/items'
assert {i['id'] for i in catalog['items']}=={'PEST_TRAP','MOUSE_PEST_TRAP','VERMIN_PEST_TRAP'}
assert {i['name'] for i in catalog['items']}=={'Pest Trap','Mouse Trap','Vermin Trap'}
print('PestTrapBoundariesTest: PASS (read-only catalog/Tab/marker facts; guarded native interaction, unique free role, owned MENU exception, release/count proof, manual takeover no close, opt-in persistence)')
