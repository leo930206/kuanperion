#!/usr/bin/env python3
"""Audit pinned vanilla branch paths and the actual applied Mixin bytecode; no game connection."""
from pathlib import Path
import re, subprocess, sys

root = Path(__file__).resolve().parent.parent
jdk = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / 'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home'
jar = Path.home() / 'Library/Application Support/PrismLauncher/libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'
source_path = Path(sys.argv[sys.argv.index('--source') + 1]) if '--source' in sys.argv else root / 'src/main/java/dev/kuan/kuanperion/mixin/AutomationReceiptMixin.java'
source = source_path.read_text()
owner = 'net/minecraft/world/inventory/'
expected = {
    'handleContainerSetSlot': ('slot', {f'L{owner}{cls};setItem(IILnet/minecraft/world/item/ItemStack;)V' for cls in ['InventoryMenu', 'AbstractContainerMenu']}),
    'handleContainerContent': ('content', {f'L{owner}{cls};initializeContents(ILjava/util/List;Lnet/minecraft/world/item/ItemStack;)V' for cls in ['InventoryMenu', 'AbstractContainerMenu']}),
}

def source_gate(code):
    for method, (_, targets) in expected.items():
        start = code.index('@Inject(method="' + method + '",')
        annotation = code[start:code.index('private void', start)]
        actual = re.findall(r'@At\(value="INVOKE",target="([^"]+)",shift=At\.Shift\.AFTER\)', annotation)
        assert set(actual) == targets and len(actual) == 2, method + ': only AFTER the two actual native writes may observe a receipt, never whole-handler TAIL'
        assert 'require=2' in annotation and 'allow=2' in annotation, method + ': pinned injection count must fail startup on drift'
        assert 'TAIL' not in annotation and 'cancellable' not in annotation, 'never cancel or fabricate a native application'

def method_code(disassembly, name):
    start = disassembly.index(' public void ' + name + '(')
    end = disassembly.find('\n  public ', start + 1)
    section = disassembly[start:end if end >= 0 else None]
    return {int(m[1]): (m[2], m[3]) for m in re.finditer(r'^[ \t]+(\d+):[ \t]+(\w+)[ \t]*(.*)$', section, re.M)}

def target_of(operand):
    match = re.search(r'// (?:InterfaceMethod|Method) (\S+)', operand)
    if not match or '.' not in match[1]: return None
    cls, tail = match[1].rsplit('.', 1)
    name, desc = tail.split(':', 1)
    return 'L' + cls + ';' + name + desc

def audit_paths(disassembly, transformed=False):
    for method, (hook, targets) in expected.items():
        code = method_code(disassembly, method)
        offsets = sorted(code)
        following = dict(zip(offsets, offsets[1:]))
        writes = [pc for pc, (op, arg) in code.items() if op.startswith('invoke') and target_of(arg) in targets]
        assert len(writes) == 2, method + ': pinned vanilla write sites changed'
        callbacks = [pc for pc, (op, arg) in code.items() if op.startswith('invoke') and re.search(r'\$kuanperion\$' + hook + r':', arg)]
        assert len(callbacks) == (2 if transformed else 0), method + ': actual injected callback count changed'
        # Exhaust all branch paths in the actual pinned/transformed method, not a retyped Java handler.
        work = [(offsets[0], 0, 0)]
        visited = set()
        results = set()
        while work:
            pc, applied, observed = work.pop()
            if (pc, applied, observed) in visited: continue
            visited.add((pc, applied, observed))
            op, arg = code[pc]
            if pc in writes: applied += 1
            if pc in callbacks:
                assert applied == 1 and observed == 0, method + ': receipt can precede an actual write or be duplicated'
                observed += 1
            assert applied <= 1 and observed <= 1, method + ': unexpected multiple applications'
            if op == 'return':
                if transformed: assert applied == observed, method + ': ignored native branch created evidence or applied branch lost it'
                results.add((applied, observed)); continue
            assert op not in ['tableswitch', 'lookupswitch', 'athrow'], method + ': review new native control flow'
            edges = [int(arg.split()[0])] if op.startswith('goto') else [following[pc]]
            if op.startswith('if'): edges.append(int(arg.split()[0]))
            work.extend((edge, applied, observed) for edge in edges)
        assert (0, 0) in results and any(a == 1 for a, _ in results), method + ': must retain both ignored and applied native branches'
        print(method + ': PASS (actual bytecode has ignored return; ' + ('each applied branch has exactly one post-write receipt, ignored branches zero' if transformed else 'whole-handler TAIL is not application proof') + ')')

native = subprocess.check_output([str(jdk / 'bin/javap'), '-c', '-p', '-classpath', str(jar), 'net.minecraft.client.multiplayer.ClientPacketListener'], text=True)
audit_paths(native)
source_gate(source)
for mutation in [source.replace('shift=At.Shift.AFTER', 'shift=At.Shift.BEFORE'), source.replace('require=2', 'require=1'), source.replace('initializeContents(', 'wrongInitialize(')]:
    try: source_gate(mutation)
    except (AssertionError, ValueError): pass
    else: raise AssertionError('unsafe hook mutation accepted')
if '--transformed' in sys.argv:
    path = Path(sys.argv[sys.argv.index('--transformed') + 1])
    assert path.is_file(), 'No actual Mixin-exported listener class'
    actual = subprocess.check_output([str(jdk / 'bin/javap'), '-c', '-p', str(path)], text=True)
    audit_paths(actual, transformed=True)
print('AppliedInventoryHooksTest: PASS (pinned native branch graph, strict post-write source hooks and negative mutations' + (', actual Mixin-transformed branch graph' if '--transformed' in sys.argv else '') + '; no server interaction)')
