#!/usr/bin/env python3
"""Pure planning boundary; expand with native proof tests before exposing strategy actions."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent/'src/main/java'
plan=root/'dev/kuan/kuanperion/automation/pest/PestStrategyPlan.java';source=plan.read_text()
for forbidden in ['java.net','HttpClient','URLConnection','Thread','Executor','Minecraft','sendCommand(','setEnabled(','setPolicy(','System.nanoTime(','https://']:
 assert forbidden not in source,forbidden
for p in root.rglob('*.java'):
 if p!=plan:assert 'PestStrategyPlan' not in p.read_text(),f'unfinished planning cannot acquire a runtime caller without new boundary proof: {p.name}'
assert 'Mode.OFF,List.of(),List.of(),Map.of(),Optional.empty()' in source
print('PestStrategyBoundary: PASS (off/empty pure targets only, no fetch/client/clock/UI/native/state-write caller; no current menu or mapping claim)')
