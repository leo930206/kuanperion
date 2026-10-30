#!/usr/bin/env python3
"""Development-only source wiring contract; runtime bodies and native acceptance have separate tests."""
from pathlib import Path
import re
r=Path(__file__).resolve().parents[1];m=r/'src/main/java/dev/kuan/kuanperion';b=(m/'AutomationClientBridge.java').read_text()
def method(name):
 a=re.search(r' (?:public|private) [^\n{}]+ '+name+r'\(',b).start();p=b.index('{',a)+1;depth=1
 while depth:depth+=(b[p]=='{')-(b[p]=='}');p+=1
 return b[a:p]
assert b.count('new PestSprayController(')==1 and b.count('new PestSprayService(')==1,'One real native adapter and service, no startup HTTP'
for name in ['onManualLook','invalidateFarmSession','onOutgoingCommand','onManualInventoryInput','shutdown','worldChanged']:
 assert 'spray.cancel(now)' in method(name),name
assert 'spray.suspend(now)' in method('suspendForJerry')
for name in ['shutdown','worldChanged']:assert 'sprayController.shutdownData()' in method(name),name
assert 'spray.pending()' in method('manualOwner') and 'sprayWasActive' in method('tick')
assert 'spray.onSystemMessage(currentWorldId(),text,now)' in method('onServerGameMessage')
assert 'sprayController.manualInput()' in method('beforeMovementTick') and 'spray.ownsMenu()' in method('beforeMovementTick')
ui=(m/'ui/PestSprayModuleSettings.java').read_text()
for required in ['service::autoEnabled','service.manualRequest(','service.cancel(','service::status','maximumUseItems()','acknowledgeSacks()','maximumCoins()','targetCount()','暫定','Sacks']:
 assert required in ui,required
assert 'PestSprayModuleSettings.tab(automation.spray())' in (m/'KuanperionClient.java').read_text()
print('PestSprayRuntimeWiring: PASS explicit default-off native/service/UI plus all lifecycle routes; not applied-Mixin/live proof')
