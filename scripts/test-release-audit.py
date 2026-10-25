#!/usr/bin/env python3
"""In-memory negative fixtures for the bounded source/archive release audit. Never executes candidate classes/native libraries."""
from pathlib import Path
import importlib.util,io,json,struct,zipfile,sys
sys.dont_write_bytecode=True
root=Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('release_audit',root/'scripts/audit-release.py');audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)
def check(b,m):
 if not b:raise AssertionError(m)
def denied(call,label):
 try:call()
 except (ValueError,AssertionError):return
 raise AssertionError('audit missed '+label)
sources={str(p.relative_to(root/'src/main/java')):p.read_text() for p in (root/'src/main/java').rglob('*.java')}
audit.audit_sources(sources)
legal=dict(sources);legal['dev/kuan/kuanperion/Attribution.java']='// Adapted from Aether; see https://author.invalid/source (notice only)\nclass Attribution {}';audit.audit_sources(legal)
for path,text,label in [
 ('dev/kuan/kuanperion/Unexpected.java','class Unexpected {java.net.http.HttpClient client;}','new network consumer'),
 ('dev/kuan/kuanperion/Unexpected.java','class Unexpected {String x="https://author.invalid/metrics";}','unapproved runtime endpoint'),
 ('dev/kuan/kuanperion/Unexpected.java','class Unexpected {String x="dev/aether/bootstrap/Client";}','upstream runtime reference'),
 ('dev/kuan/kuanperion/Unexpected.java','class Unexpected {java.net.Socket s;}','raw socket'),
]:
 changed=dict(sources);changed[path]=text;denied(lambda:audit.audit_sources(changed),label)
changed=dict(sources);key='dev/kuan/kuanperion/automation/HypixelItemCatalogClient.java';changed[key]=changed[key].replace('.header("Accept","application/json")','.header("Authorization","Bearer hidden")');denied(lambda:audit.audit_sources(changed),'credential added to anonymous lookup')
changed=dict(sources);changed[key]=changed[key].replace('.GET().build()','.POST(HttpRequest.BodyPublishers.ofString("events")).build()');denied(lambda:audit.audit_sources(changed),'lookup becomes upload')
changed=dict(sources);key='dev/kuan/kuanperion/KuanperionClient.java';changed[key]=changed[key].replace('// Never log the webhook URL or exception message, which can contain its secret token.','// response must not execute gameplay\n                remote.run();');denied(lambda:audit.audit_sources(changed),'response callback becomes remote control')
# Java constant-pool fixtures only: not loadable/verified gameplay classes.
def constants(*values):
 data=struct.pack('>IHHH',0xcafebabe,0,69,len(values)+1)
 for v in values:
  b=v.encode();data+=b'\x01'+struct.pack('>H',len(b))+b
 return data
entries={'META-INF/MANIFEST.MF':b'Manifest-Version: 1.0\n','fabric.mod.json':(root/'src/main/resources/fabric.mod.json').read_bytes(),'kuanperion.mixins.json':(root/'src/main/resources/kuanperion.mixins.json').read_bytes(),'META-INF/licenses/Kuanperion-GPL-3.0.txt':(root/'LICENSE').read_bytes(),'META-INF/licenses/THIRD_PARTY_NOTICES.md':(root/'THIRD_PARTY_NOTICES.md').read_bytes(),'dev/kuan/kuanperion/LocalOnly.class':constants('dev/kuan/kuanperion/LocalOnly')}
for name in audit.BUNDLED:entries['META-INF/jars/'+name]=(root/'src/main/resources/META-INF/jars'/name).read_bytes()
def archive(e,duplicate=None):
 out=io.BytesIO()
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for k,v in e.items():z.writestr(k,v)
  if duplicate:
   import warnings
   with warnings.catch_warnings():warnings.simplefilter('ignore');z.writestr(duplicate,e[duplicate])
 return out.getvalue()
audit.audit_archive(archive(entries),root)
for path,value,label in [
 ('../escape.txt',b'no','path traversal'),('/absolute.txt',b'no','absolute path'),('config/settings.properties',b'webhook=private','user settings'),
 ('META-INF/jars/aether-original.jar',b'not loaded','original/unknown dependency'),('assets/kuanperion/disguised.png',archive({'old/runtime.class':b'not executed'}),'archive disguised as an asset'),('dev/aether/Client.class',constants('dev/aether/Client'),'upstream class'),
 ('dev/kuan/kuanperion/SmokeTest.class',constants('test'),'test payload'),('dev/kuan/kuanperion/Unexpected.class',constants('https://author.invalid/metrics'),'compiled endpoint'),
 ('dev/kuan/kuanperion/Unexpected.class',constants('java/net/Socket'),'compiled raw socket'),('dev/kuan/kuanperion/Unexpected.class',constants('java/net/http/HttpClient'),'compiled unapproved client'),
 ('META-INF/MANIFEST.MF',b'Manifest-Version: 1.0\nPremain-Class: hidden.Agent\n','agent injection'),
 ('META-INF/licenses/Kuanperion-GPL-3.0.txt',b'license removed','lost GPL notice'),
]:
 changed=dict(entries);changed[path]=value;denied(lambda:audit.audit_archive(archive(changed),root),label)
changed=dict(entries);name='META-INF/jars/'+next(iter(audit.BUNDLED));changed[name]=changed[name][:-1]+bytes([changed[name][-1]^1]);denied(lambda:audit.audit_archive(archive(changed),root),'changed native dependency bytes')
denied(lambda:audit.audit_archive(archive(entries,'fabric.mod.json'),root),'duplicate metadata')
denied(lambda:audit.class_constants(b'not a class'),'malformed class header')
print('ReleaseAuditTest: PASS (actual source plus in-memory endpoint/client/auth/upload/upstream/agent/path/secret-file/test/dependency/license/duplicate mutations rejected; no network or candidate execution)')
