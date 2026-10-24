#!/usr/bin/env python3
"""Source wiring guard; behavior is tested separately by the pure runtime suite."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent/'src/main/java/dev/kuan/kuanperion'
a=(r/'AutomationMenuActions.java').read_text()
purchase=a.split('public static GuardedMenuAction.Result purchaseClick(',1)[1].split('\n }',1)[0]
assert 'GuardedMenuAction.perform(' in purchase and 'GuardedMenuAction.Space.REQUIRE_FREE_SLOT' in purchase
assert 'menuButton(ticket)' in purchase and 'AutomationMenuActions::sendNative' in purchase
native=a.split('private static boolean sendNative(',1)[1]
assert 'handleContainerInput(' in native and 'finally{ownedInput=false;}' in native
client=(r/'automation/HypixelMarketClient.java').read_text()
assert client.count('https://')==1 and 'https://api.hypixel.net/v2/skyblock/bazaar' in client
assert 'Redirect.NEVER' in client and '.header("Accept","application/json")' in client
assert 'http.cancel(true)' in client and 'orTimeout(10,TimeUnit.SECONDS)' in client
# No startup/old gameplay path may quietly start the public data client before an approved consumer exists.
for name in ['KuanperionClient.java','AutomationClientBridge.java']:
 assert 'new HypixelMarketClient' not in (r/name).read_text()
print('MarketBoundariesTest: PASS (typed native purchase result/free-space guard, bounded anonymous endpoint, no startup fetching)')
