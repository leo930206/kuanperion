#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
sidebar=(root/'src/main/java/dev/kuan/kuanperion/ui/MainGUISidebar.java').read_text()
assert 'new Entry("Trade", "trade"' in sidebar,'Trade must have its own consistent-stroke icon'
svg=ET.parse(root/'src/main/resources/assets/kuanperion/icons/trade.svg').getroot()
assert svg.attrib['viewBox']=='0 0 24 24'
paths=[e for e in svg.iter() if e.tag.endswith('path')]
assert len(paths)==1
p=paths[0].attrib
assert p['stroke-width']=='2' and p['stroke']=='white' and p['fill']=='none'
assert p['stroke-linecap']=='round' and p['stroke-linejoin']=='round'
assert all('opacity' not in e.attrib for e in svg.iter()),'All lines must have equal opacity/weight'
print('TradeIconTest: PASS (uniform 2px round strokes, matching Farming/Mining at 24px)')
