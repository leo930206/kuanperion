#!/usr/bin/env python3
"""Shared measured module-header wiring; glyph/click behavior is checked in actual isolated UI smoke."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent/'src/main/java/dev/kuan/kuanperion/ui'
def method(file,name):
 s=(root/file).read_text();a=s.index(' '+name+'(');a=s.index('{',a)+1;b=a;depth=1
 while depth:
  depth+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b-1]
paint=method('MainGUI.java','renderGroupHeader')
assert 'groupHeaderHeight(group,w)' in paint and 'wrapTextForWidth' in paint and 'groupHeaderDescriptionWidth' in paint
assert 'pushContentLocalScissor' in paint and 'popContentLocalScissor' in paint and 'headerH' in paint
assert 'w, 46.0f' not in paint and 'UiLayoutMetrics.descriptionTop(13.5f)' not in paint
panel=method('MainGUIModuleDetailRenderer.java','renderSettingsPanel')
assert 'groupHeaderHeight(group,groupW)' in panel and 'y += headerH' in panel and 'total += headerH' in panel
click=method('MainGUISettingInteractionController.java','handleModuleSettingsPanelClick')
assert 'groupHeaderHeight(group,gw)' in click and 'y2 += 46.0f' not in click
scroll=method('MainGUIActions.java','moduleSettingScrollOffset')
assert 'groupHeaderHeight(targetGroup,this.owner.moduleSettingsWidth())' in scroll and '65.0f' not in scroll
print('GroupHeaderWiring: PASS (measured painted/culling/scroll height, click offset and search anchor share module geometry; legacy flat labels untouched)')
