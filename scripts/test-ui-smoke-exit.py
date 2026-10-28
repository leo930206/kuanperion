#!/usr/bin/env python3
"""Run the actual launch verifier with harmless child-output fixtures, never Minecraft/GUI/network."""
from pathlib import Path
import json,os,subprocess,sys,tempfile
root=Path(__file__).resolve().parent.parent
launcher=Path(sys.argv[1]) if len(sys.argv)>1 else root/'scripts/ui-smoke/launch.py'
cases=[('missing-shutdown',['UI_SMOKE_PASS'],0,False),('nonzero-exit',['UI_SMOKE_PASS','UI_SMOKE_SHUTDOWN_PASS'],1,False),('reported-failure',['UI_SMOKE_PASS','UI_SMOKE_SHUTDOWN_PASS','UI_SMOKE_FAIL'],0,False),('missing-ui',['UI_SMOKE_SHUTDOWN_PASS'],0,False),('complete',['UI_SMOKE_PASS','UI_SMOKE_SHUTDOWN_PASS'],0,True)]
failed=[]
with tempfile.TemporaryDirectory(prefix='kuanperion-launch-verifier-') as tmp:
 for name,lines,code,expected in cases:
  folder=Path(tmp)/name;(folder/'game').mkdir(parents=True)
  child='import sys;'+''.join('print('+repr(line)+');' for line in lines)+'sys.exit('+str(code)+')'
  (folder/'launch-args.json').write_text(json.dumps([sys.executable,'-c',child]))
  env=os.environ.copy();env['KUANPERION_SMOKE_DIR']=str(folder)
  result=subprocess.run([sys.executable,str(launcher)],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
  if (result.returncode==0)!=expected:failed.append(name)
if failed:raise AssertionError('UI launch verifier accepted/rejected invalid evidence: '+', '.join(failed))
print('UiSmokeExitTest: PASS (actual verifier, five harmless child fixtures; require successful UI, shutdown proof and process exit; no GUI)')
