#!/usr/bin/env python3
"""Source wiring guard; behavior is tested separately by the pure runtime suite."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent/'src/main/java/dev/kuan/kuanperion'
a=(r/'AutomationMenuActions.java').read_text()
purchase=a.split('public static GuardedMenuAction.Result purchaseClick(',1)[1].split('\n }',1)[0]
assert 'GuardedMenuAction.perform(' in purchase and 'GuardedMenuAction.Space.REQUIRE_FREE_SLOT' in purchase
assert 'menuButton(ticket)' in purchase and 'AutomationMenuActions::sendNative' in purchase
import re
def native_body(source):
 match=re.search(r'^ static boolean sendNative\(MenuContract.Ticket request\)\{',source,re.M)
 assert match,'Native sender must remain package-private, never public/protected'
 return source[match.end():]
native=native_body(a)
for visibility in ('public ','protected '):
 try:native_body(a.replace(' static boolean sendNative(', ' '+visibility+'static boolean sendNative('));raise RuntimeError('Visibility mutation passed')
 except AssertionError:pass
assert 'handleContainerInput(' in native and 'finally{ownedInput=false;}' in native
assert 'getCarried().isEmpty()' in native
# Package-level sender references are confined to independently guarded Book, Pet and Sacks native gates; never public raw input.
callers={p.name for p in r.rglob('*.java') if p.name!='AutomationMenuActions.java' and ('AutomationMenuActions.sendNative(' in p.read_text() or 'AutomationMenuActions::sendNative' in p.read_text())}
assert callers=={'BookMenuController.java','PetGeorgeController.java','ComposterFlightController.java'},callers
pet=(r/'PetGeorgeController.java').read_text();composter=(r/'ComposterFlightController.java').read_text()
for requirement in ['GuardedMenuAction.perform(', 'exactPet(pet,', 'mc().player.containerMenu==nativeMenu', 'pet.slot()>=36', 'owns(h)']:assert requirement in pet,requirement
assert 'new ComposterSacksFlow(' in composter and 'GuardedMenuAction.perform(' in composter
book=(r/'BookMenuController.java').read_text()
assert 'GuardedMenuAction.perform(' in book and 'session.isCurrent(h)' in book and 'lastInventory.equals(game.inventory())' in book
client=(r/'automation/HypixelMarketClient.java').read_text()
assert client.count('https://')==1 and 'https://api.hypixel.net/v2/skyblock/bazaar' in client
assert 'Redirect.NEVER' in client and '.header("Accept","application/json")' in client
assert 'http.cancel(true)' in client and 'orTimeout(10,TimeUnit.SECONDS)' in client
# No startup/old gameplay path may quietly start the public data client before an approved consumer exists.
for name in ['KuanperionClient.java','AutomationClientBridge.java']:
 assert 'new HypixelMarketClient' not in (r/name).read_text()
print('MarketBoundariesTest: PASS (typed native purchase result/free-space guard, bounded anonymous endpoint, no startup fetching)')

items=(r/'automation/HypixelItemCatalogClient.java').read_text()
assert items.count('https://')==1 and 'https://api.hypixel.net/v2/resources/skyblock/items' in items
assert 'HypixelMarketClient.newHttpClient()' in items and '.header("Accept","application/json")' in items
assert 'new HypixelMarketClient.LimitedBodySubscriber(SkyblockItemCatalog.MAX_BYTES)' in items
assert 'orTimeout(10,TimeUnit.SECONDS)' in items and 'http.cancel(true)' in items
for name in ['KuanperionClient.java','AutomationClientBridge.java']:
 assert 'new HypixelItemCatalogClient' not in (r/name).read_text()
print('ItemCatalogBoundariesTest: PASS (fixed anonymous official endpoint, bounded shared transport, no player/account input or startup caller)')
