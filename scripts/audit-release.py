#!/usr/bin/env python3
"""Bounded static release regression gate, not a network sandbox or a complete information-flow/security proof."""
from pathlib import Path
import argparse,hashlib,io,json,re,struct,zipfile
BUNDLED={
 'lwjgl-nanovg-3.4.1-natives-linux.jar':'9e24ae4154ffffba9836218f17931889c81458bc121714773af590f36d2472c3',
 'lwjgl-nanovg-3.4.1-natives-macos-arm64.jar':'fe0b4f55ab4a58bdeb2977f7d557fdc30a8d2177b2e755f5f9aec478ff0f8c55',
 'lwjgl-nanovg-3.4.1-natives-macos.jar':'a426042decd326df1e6a08d078609f13f1bc25de2199cba21821413a9793f231',
 'lwjgl-nanovg-3.4.1-natives-windows.jar':'c872328a9eabbff9a47947638c84c22f0a826bd6e5add4d3210a9aa83849b079',
 'lwjgl-nanovg-3.4.1.jar':'d8b83f2790ea3d1e199d7817e53be4e622f23bd3f0cab765b2ba1e2028e9c07b',
}
BASE='dev/kuan/kuanperion/'
PUBLIC={BASE+'automation/HypixelItemCatalogClient':'https://api.hypixel.net/v2/resources/skyblock/items',BASE+'automation/HypixelMarketClient':'https://api.hypixel.net/v2/skyblock/bazaar'}
HTTP_CALLERS=set(PUBLIC)|{BASE+'KuanperionClient'}|{p+'$NativeTransport' for p in PUBLIC}
URLS=set(PUBLIC.values())|{'http://apache.org/xml/features/disallow-doctype-decl'}
NETWORK=re.compile(r'\b(?:java/net/(?:Socket|ServerSocket|DatagramSocket|URL|URLConnection|HttpURLConnection|Authenticator|CookieHandler)\b|java/nio/channels/(?:SocketChannel|ServerSocketChannel)\b|javax/net/|java/net/http/WebSocket\b|okhttp3/|org/apache/http/|org/java_websocket/|io/netty/)')
def require(b,message):
 if not b:raise ValueError(message)
def without_comments(s):
 token=r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/'
 return re.sub(token,lambda m:'' if m[0].startswith(('//','/*')) else m[0],s)
def audit_runtime_text(name,text):
 require(not re.search(r'(?i)(?:dev[/.]aether[/.]|aetherclient|aetherclient\.com)',text),'Upstream runtime reference: '+name)
 normalized=text.replace('.','/')
 require(not NETWORK.search(normalized),'Unapproved native networking type: '+name)
 if re.search(r'\bjava/net/http/(?:HttpClient|HttpRequest)\b',normalized):require(name in HTTP_CALLERS,'Unapproved HTTP consumer: '+name)
 for url in re.findall(r'(?:https?|wss?|ftp)://[^\s"<>\)]+',text):require(url in URLS,'Unapproved runtime URL in '+name)
def audit_sources(sources):
 require(bool(sources),'No production sources')
 for path,raw in sources.items():
  require(path.startswith(BASE) and path.endswith('.java'),'Unknown source package: '+path)
  audit_runtime_text(path[:-5],without_comments(raw))
 for name,url in PUBLIC.items():
  raw=without_comments(sources[name+'.java']);compact=re.sub(r'\s+','',raw)
  require('URI.create("'+url+'")' in compact,'Lookup URL is not a fixed literal: '+name)
  require('.GET().build()' in compact and '.POST(' not in compact and '.PUT(' not in compact,'Public lookup is not read-only: '+name)
  require(re.findall(r'\.header\("([^"]+)","([^"]+)"\)',compact)==[('Accept','application/json')],'Lookup has unexpected/auth headers: '+name)
  require('client.sendAsync(request(),' in compact and compact.count('.sendAsync(')==1,'Lookup send boundary changed: '+name)
  require(not any(v in compact for v in ['.authenticator(','.cookieHandler(','.proxy(','.setHeader(']),'Lookup acquired identity/proxy state: '+name)
  require(not any(v in raw for v in ['Minecraft','AutomationSession','AutomationClientBridge','ProcessBuilder','Runtime.getRuntime','Class.forName','.sendCommand(','.handleContainerInput(','.setDown(']),'Public data client acquired gameplay/process authority: '+name)
 webhook=without_comments(sources[BASE+'KuanperionClient.java']);require(webhook.count('.sendAsync(')==1 and 'HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())' in webhook,'User webhook must remain outbound-only/discard response')
 require('URI.create(pestWebhookUrl.trim())' in webhook,'Webhook no longer uses explicit user configuration')
 callback=re.search(r'HTTP_CLIENT\.sendAsync[\s\S]*?\.whenComplete\(\(response, error\) -> \{(.*?)\n\s*\}\);',webhook,re.S)
 expected_callback='if (error != null) LOGGER.warn("[Kuanperion] Discord notification request failed"); else if (response.statusCode() < 200 || response.statusCode() >= 300) LOGGER.warn("[Kuanperion] Discord notification HTTP status={}", response.statusCode());'
 require(callback is not None and re.sub(r'\s+','',callback.group(1))==re.sub(r'\s+','',expected_callback),'Webhook response callback must remain status-only logging, not remote actions')
def class_constants(data):
 require(len(data)>=10 and data[:4]==b'\xca\xfe\xba\xbe','Invalid class header')
 require(struct.unpack_from('>H',data,6)[0]==69,'Project bytecode is not pinned Java25')
 count=struct.unpack_from('>H',data,8)[0];require(1<=count<=65535,'Invalid constant pool');p=10;i=1;values=[]
 try:
  while i<count:
   tag=data[p];p+=1
   if tag==1:
    length=struct.unpack_from('>H',data,p)[0];p+=2;require(p+length<=len(data),'Truncated class UTF8');values.append(data[p:p+length].decode('utf-8','replace'));p+=length
   elif tag in (3,4,9,10,11,12,17,18):p+=4
   elif tag in (5,6):p+=8;i+=1
   elif tag in (7,8,16,19,20):p+=2
   elif tag==15:p+=3
   else:raise ValueError('Unknown constant pool tag')
   require(p<=len(data),'Truncated constant pool');i+=1
 except (IndexError,struct.error) as e:raise ValueError('Truncated constant pool') from e
 require(i==count,'Invalid wide constant-pool entry')
 return values
def audit_archive(data,root):
 require(0<len(data)<=64*1024*1024,'Archive size out of bounds')
 try:
  with zipfile.ZipFile(io.BytesIO(data)) as z:
   infos=z.infolist();names=[e.filename for e in infos]
   require(len(names)==len(set(names)) and len(names)<=5000,'Duplicate/excess archive entries')
   require(sum(e.file_size for e in infos)<=64*1024*1024 and all(e.file_size<=16*1024*1024 and not e.flag_bits&1 for e in infos),'Oversized/encrypted archive content')
   files={}
   for entry in infos:
    name=entry.filename
    require(not name.startswith('/') and '\\' not in name and '..' not in name.split('/') and name!='','Unsafe archive path')
    if entry.is_dir():continue
    allowed=name in {'META-INF/MANIFEST.MF','fabric.mod.json','kuanperion.mixins.json','META-INF/licenses/Kuanperion-GPL-3.0.txt','META-INF/licenses/THIRD_PARTY_NOTICES.md'} or name in {'META-INF/jars/'+n for n in BUNDLED} or name.startswith(BASE) and name.endswith('.class') or name.startswith('assets/kuanperion/') and Path(name).suffix in {'.svg','.png','.otf','.txt','.json'}
    require(allowed,'Unapproved archive payload: '+name)
    require(not re.search(r'(?i)(?:^|/)(?:options|accounts?|credentials?|tokens?|secrets?|sessions?)(?:[./_-]|$)',name),'Possible private runtime file: '+name)
    files[name]=z.read(entry)
    require(name.endswith('.jar') or files[name][:4] not in {b'PK\x03\x04',b'PK\x05\x06',b'PK\x07\x08'},'Archive disguised as non-dependency asset: '+name)
   require(any(n.startswith(BASE) and n.endswith('.class') for n in files),'No project classes')
   for name,payload in files.items():
    if name.endswith('.class'):
     require(not re.search(r'(?:Test|Tests|UiSmoke)(?:\$[^/]*)?\.class$',name),'Test class in production payload')
     for value in class_constants(payload):audit_runtime_text(name[:-6],value)
   expected={'META-INF/jars/'+n for n in BUNDLED}
   require({n for n in files if n.endswith('.jar')}==expected,'Unexpected/missing nested dependencies')
   for name,sha in BUNDLED.items():require(hashlib.sha256(files['META-INF/jars/'+name]).hexdigest()==sha,'Unapproved bundled dependency bytes: '+name)
   for target,source in [('fabric.mod.json','src/main/resources/fabric.mod.json'),('kuanperion.mixins.json','src/main/resources/kuanperion.mixins.json'),('META-INF/licenses/Kuanperion-GPL-3.0.txt','LICENSE'),('META-INF/licenses/THIRD_PARTY_NOTICES.md','THIRD_PARTY_NOTICES.md')]:require(files.get(target)==(root/source).read_bytes(),'Missing/changed source metadata or license: '+target)
   meta=json.loads(files['fabric.mod.json'])
   require(meta.get('id')=='kuanperion' and meta.get('version')=='2.1.0' and meta.get('environment')=='client' and meta.get('license')=='GPL-3.0-only','Unexpected mod identity/license')
   require(meta.get('entrypoints')=={'client':['dev.kuan.kuanperion.KuanperionClient']} and meta['depends'].get('minecraft')=='26.2','Unexpected entrypoint/game target')
   require({e['file'] for e in meta.get('jars',[])}==expected and len(meta['jars'])==len(expected),'Nested dependencies not exactly declared')
   manifest=files.get('META-INF/MANIFEST.MF',b'').decode('utf-8')
   require('Manifest-Version: 1.0' in manifest and not re.search(r'(?im)^(?:Premain-Class|Agent-Class|Launcher-Agent-Class|Main-Class|Class-Path):',manifest),'Unexpected executable/agent manifest')
   return len(files)
 except (zipfile.BadZipFile,KeyError,UnicodeError,json.JSONDecodeError) as e:raise ValueError('Invalid release archive/metadata') from e
def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--jar',type=Path,help='Optional built development archive; read-only, never executed');args=parser.parse_args();root=Path(__file__).resolve().parent.parent
 sources={str(p.relative_to(root/'src/main/java')):p.read_text() for p in (root/'src/main/java').rglob('*.java')};audit_sources(sources)
 print('ReleaseSourceAudit: PASS (fixed anonymous resource GETs + explicit user-configured outbound webhook only; no new networking type/upstream-runtime reference)')
 if args.jar:
  count=audit_archive(args.jar.read_bytes(),root);print(f'ReleaseArchiveAudit: PASS ({count} files; pinned five rendering dependencies, class constants/endpoints, metadata/licenses and no known-private-path/test/upstream/agent payload)')
if __name__=='__main__':main()
