#!/usr/bin/env python3
"""Exercise the actual opt-in metadata block without launching a game or contacting a service."""
import ast,json,os,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
prepare=root/'scripts/ui-smoke/prepare.py'
if not prepare.exists():
    raise AssertionError('Run from a complete project root')
nodes=ast.parse(prepare.read_text()).body
blocks=[node for node in nodes if isinstance(node,ast.If) and 'KUANPERION_SMOKE_NAV_TRACE' in ast.unparse(node.test)]
assert len(blocks)==1,'One explicit opt-in only'
block=compile(ast.Module(body=blocks,type_ignores=[]),str(prepare),'exec')
original=os.environ.get('KUANPERION_SMOKE_NAV_TRACE')
try:
    for mode in [None,'0','1']:
        with tempfile.TemporaryDirectory(prefix='kuanperion-trace-metadata-') as folder:
            target=Path(folder);(target/'classes').mkdir()
            mod={'entrypoints':{'client':['dev.kuan.kuanperion.ui.UiSmoke']}}
            path=target/'classes/fabric.mod.json';path.write_text(json.dumps(mod))
            if mode is None:os.environ.pop('KUANPERION_SMOKE_NAV_TRACE',None)
            else:os.environ['KUANPERION_SMOKE_NAV_TRACE']=mode
            exec(block,{'os':os,'json':json,'R':target})
            result=json.loads(path.read_text())
            if mode!='1':
                assert result==mod and not (target/'classes/ui-navigation-trace.mixins.json').exists(),'Default smoke must not activate trace'
                continue
            assert result['entrypoints']==mod['entrypoints'] and result['mixins']==['ui-navigation-trace.mixins.json']
            config=json.loads((target/'classes/ui-navigation-trace.mixins.json').read_text())
            assert config['package']=='dev.kuan.kuanperion.smokemixin','Do not reserve the production UI/entrypoint namespace as a mixin package'
            assert config['required'] and config['injectors']['defaultRequire']==1 and config['client']==['UiNavigationTraceMixin']
            source=root/'src/smoke/java'/Path(config['package'].replace('.','/'))/'UiNavigationTraceMixin.java'
            assert source.exists(),'Diagnostic source must stay exclusively in smoke'
finally:
    if original is None:os.environ.pop('KUANPERION_SMOKE_NAV_TRACE',None)
    else:os.environ['KUANPERION_SMOKE_NAV_TRACE']=original
print('UiSmokeTraceTest: PASS (actual opt-in metadata, disabled default, dedicated mixin namespace, smoke-only source; no GUI)')
