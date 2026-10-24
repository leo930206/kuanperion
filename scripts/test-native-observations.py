#!/usr/bin/env python3
"""Real pinned26.2 classes, no GUI/launcher/server/account/settings and no dependency download.
Run after scripts/build.sh. Full client smoke separately checks applied Mixins/UI.
"""
import json
from pathlib import Path
import subprocess
import tempfile

root=Path(__file__).resolve().parent.parent
prism=Path.home()/'Library/Application Support/PrismLauncher'
java=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin'
cp=[str(root/'build/classes')]
for component,version in [('net.minecraft','26.2'),('org.lwjgl3','3.4.1'),('net.fabricmc.fabric-loader','0.19.5')]:
    metadata=json.loads((prism/'meta'/component/(version+'.json')).read_text())
    for lib in metadata['libraries']:
        rules=lib.get('rules',[])
        allowed=not rules
        for rule in rules:
            os=rule.get('os',{})
            if os.get('name') and os['name'] not in ('osx','osx-arm64'):continue
            if os.get('arch') and os['arch'] not in ('arm64','aarch64'):continue
            if rule.get('features'):continue
            allowed=rule['action']=='allow'
        if not allowed:continue
        group,artifact,ver,*classifier=lib['name'].split(':')
        name=artifact+'-'+ver+('-'+classifier[0] if classifier else '')+'.jar'
        path=prism/'libraries'/group.replace('.','/')/artifact/ver/name
        if path.is_file():cp.append(str(path))
cp.append(str(prism/'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'))
assert (root/'build/classes/dev/kuan/kuanperion/PestGameView.class').is_file(),'Run scripts/build.sh first'
classpath=':'.join(dict.fromkeys(cp))
with tempfile.TemporaryDirectory(prefix='kuanperion-native-') as directory:
    tmp=Path(directory)
    sources=[root/'src/smoke/java/dev/kuan/kuanperion'/name for name in ['AutomationGameViewTest.java','AutomationPositionViewTest.java','NativeObservationTest.java']]
    subprocess.run([str(java/'javac'),'--release','25','-encoding','UTF-8','-cp',classpath,'-d',str(tmp),*map(str,sources)],check=True,timeout=90)
    subprocess.run([str(java/'java'),'-Xmx1G','-Djava.awt.headless=true','-cp',str(tmp)+':'+classpath,'dev.kuan.kuanperion.NativeObservationTest'],cwd=tmp,check=True,timeout=90)
