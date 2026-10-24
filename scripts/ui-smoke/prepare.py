from pathlib import Path
import json,subprocess,shutil,os
P=Path.home()/'Library/Application Support/PrismLauncher';W=Path(__file__).resolve().parents[2];R=Path(os.environ.get('KUANPERION_SMOKE_DIR','/private/tmp/kuanperion-ui-smoke-2.0.16'))
(R/'game/mods').mkdir(parents=True,exist_ok=True)
(R/'classes').mkdir(parents=True,exist_ok=True)
def path_for(lib):
 parts=lib['name'].split(':');group,artifact,version=parts[:3];filename=artifact+'-'+version+('-'+parts[3] if len(parts)>3 else '')+'.jar'
 return P/'libraries'/group.replace('.','/')/artifact/version/filename
def allowed(lib):
 rules=lib.get('rules',[])
 if not rules:return True
 allow=False
 for r in rules:
  osr=r.get('os',{}); name=osr.get('name');arch=osr.get('arch')
  if name and name not in ['osx','osx-arm64']:continue
  if arch and arch not in ['arm64','aarch64']:continue
  if r.get('features'):continue
  allow=r['action']=='allow'
 return allow
cp=[]
for uid,v in [('net.minecraft','26.2'),('org.lwjgl3','3.4.1'),('net.fabricmc.fabric-loader','0.19.5')]:
 d=json.loads((P/'meta'/uid/(v+'.json')).read_text())
 for lib in d['libraries']:
  if not allowed(lib):continue
  p=path_for(lib)
  if p.exists():cp.append(str(p))
  else:print('MISSING',p)
cp.append(str(P/'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'))
cp=list(dict.fromkeys(cp));(R/'classpath.txt').write_text(':'.join(cp))
compilecp=cp+[str(W/'build/classes')]+[str(p) for p in (W/'build/tmp/fabric-api').rglob('*.jar')]+[str(p) for p in (W/'src/main/resources/META-INF/jars').glob('*.jar')]
subprocess.run([str(P/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin/javac'),'--release','25','-encoding','UTF-8','-cp',':'.join(compilecp),'-d',str(R/'classes'),str(W/'src/smoke/java/dev/kuan/kuanperion/ui/UiSmoke.java')],check=True)
(R/'classes/fabric.mod.json').write_text(json.dumps({'schemaVersion':1,'id':'kuanperion_ui_smoke','version':'1.0','name':'Isolated UI Smoke Test','environment':'client','entrypoints':{'client':['dev.kuan.kuanperion.ui.UiSmoke']},'depends':{'kuanperion':'2.0.16','fabric-api':'*'}}))
subprocess.run([str(P/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin/jar'),'--create','--file',str(R/'game/mods/ui-smoke.jar'),'-C',str(R/'classes'),'.'],check=True)
shutil.copy2(W/'build/libs/kuanperion-2.0.16.jar',R/'game/mods/kuanperion-2.0.16.jar')
shutil.copy2(P/'instances/Hypixel SkyBlock/minecraft/mods/fabric-api-0.161.0+26.2.jar',R/'game/mods/fabric-api.jar')
(R/'game/options.txt').write_text('maxFps:30\nguiScale:2\nfullscreen:false\nskipMultiplayerWarning:true\n')
args=[str(P/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin/java'),'-XstartOnFirstThread','-Xmx2G','-Dfabric.log.level=info','-Dfabric.gameVersion=26.2','-cp',':'.join(cp),'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','KuanperionUITest','--version','26.2','--gameDir',str(R/'game'),'--assetsDir',str(P/'assets'),'--assetIndex','32','--uuid','00000000000040008000000000000001','--accessToken','offline-ui-test','--userType','legacy','--versionType','release','--width','1100','--height','760']
(R/'launch-args.json').write_text(json.dumps(args));print('Prepared isolated client,',len(cp),'runtime dependencies; no account or server arguments')
