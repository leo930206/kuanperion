from pathlib import Path
import json,subprocess,os
root=Path(os.environ.get('KUANPERION_SMOKE_DIR','/private/tmp/kuanperion-ui-smoke-2.0.6'))
args=json.loads((root/'launch-args.json').read_text())
with (root/'client.log').open('w') as log:
 p=subprocess.Popen(args,cwd=root/'game',stdout=log,stderr=subprocess.STDOUT)
 print('Isolated UI test PID',p.pid,flush=True)
 try:p.wait(timeout=240)
 except subprocess.TimeoutExpired:
  p.terminate();p.wait(timeout=15);print('UI smoke timed out')
 print('Client exit',p.returncode)

log=(root/"client.log").read_text()
if "UI_SMOKE_PASS" not in log or "UI_SMOKE_FAIL" in log:
 raise SystemExit("UI smoke failed; inspect "+str(root/"client.log"))
print("UI_SMOKE_PASS verified")
