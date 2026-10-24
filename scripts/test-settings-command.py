#!/usr/bin/env python3
"""Run actual command interception/deferred open against fake screen boundary."""
from pathlib import Path
import subprocess,sys,tempfile,re
root=Path(__file__).resolve().parent.parent
source=(root/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
def method(name):
 a=re.search(r'    private static [^\n]* '+re.escape(name),source).start();b=source.index('{',a)+1;d=1
 while d:
  d+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
methods='\n'.join(method(n) for n in ['allowOutgoingCommand(', 'tickSettingsCommand('])
assert 'ClientSendMessageEvents.ALLOW_COMMAND.register(KuanperionClient::allowOutgoingCommand)' in source
assert 'tickSettingsCommand(mc);' in source
java='''public class SettingsCommandTest {
 static boolean settingsCommandPending;static int opens;
 static class Minecraft {}
 static void toggleSettingsScreen(Minecraft mc){opens++;}
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 METHODS
 public static void main(String[] args){
  Minecraft mc=new Minecraft();
  for(String command:new String[]{"kuanperion","Kuanperion","KUANPERION","kUaNpErIoN","  Kuanperion  "}){
   int before=opens;check(!allowOutgoingCommand(command),"local command must not reach server");
   check(opens==before&&settingsCommandPending,"defer until chat finishes closing");
   tickSettingsCommand(mc);check(opens==before+1&&!settingsCommandPending,"open on following tick");
   tickSettingsCommand(mc);check(opens==before+1,"only once");
  }
  for(String command:new String[]{"home","barn","sethome","kuanperion other","kuanperionx","say kuanperion",""}){
   check(allowOutgoingCommand(command)&&!settingsCommandPending,"unrelated server commands unchanged");
  }
  System.out.println("SettingsCommandTest: PASS (mixed case / local cancellation / deferred once / exact command isolation)");
 }
}'''.replace('METHODS',methods)
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'SettingsCommandTest.java';p.write_text(java);jdk=Path(sys.argv[1])/'bin'
 subprocess.run([str(jdk/'javac'),'--release','21','-d',tmp,str(p)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'SettingsCommandTest'],check=True)
