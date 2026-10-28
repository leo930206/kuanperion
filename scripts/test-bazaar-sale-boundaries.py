#!/usr/bin/env python3
"""Unwired sale planning is data/configuration, not a native backend or permission."""
from pathlib import Path
r=Path(__file__).resolve().parents[1];main=r/'src/main/java/dev/kuan/kuanperion'
source=main/'automation/BazaarSalePlan.java';text=source.read_text()
for path in main.rglob('*.java'):
 if path!=source:assert 'BazaarSalePlan' not in path.read_text(), 'No runtime caller before native sale/parser/receipts: '+str(path.relative_to(main))
for forbidden in ['HttpClient','URLConnection','Socket(','Minecraft.getInstance','sendCommand(','.handleContainerInput(','.setCarried(','.setDown(','.setPos(','.setSelectedSlot(','Thread.sleep','Files.','session.begin(','session.finish(','new HypixelMarketClient','taxRate']:
 assert forbidden not in text,forbidden
for required in ['market.estimateSell(','private Sale(','private Plan(','withinConfiguredBounds(','Map.copyOf(baseline)','!sameIdentity(item,material.exemplar())','slot<9||slot>=36','maximumProducts()','minimumUnitNet()']:
 assert required in text,required
print('BazaarSaleBoundaries: PASS (unwired exact approved commodity plan, fresh gross-only data/configured net bounds; no fetch, tax assumption, native action or backend switch)')
