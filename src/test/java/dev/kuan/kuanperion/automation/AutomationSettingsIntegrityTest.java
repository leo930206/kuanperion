package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;import java.io.IOException;
/** Ambiguous consent must never pick a last-wins value or overwrite the original. */
public final class AutomationSettingsIntegrityTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{var dir=Files.createTempDirectory("automation-integrity-");try{
  var file=dir.resolve("automation.json");var cases=new ArrayList<byte[]>();
  for(String raw:List.of("{\"schemaVersion\":1,\"features\":{\"supercraft\":false,\"supercraft\":true}}","{\"schemaVersion\":1,\"supercraft\":{\"maxConsumed\":0,\"maxConsumed\":65536}}","{\"schemaVersion\":1,\"schemaVersion\":1}","{\"schemaVersion\":1,\"future\":{\"value\":0,\"value\":1}}","{\"schemaVersion\":1} {}","{/*comment*/\"schemaVersion\":1}","{\"schemaVersion\":1,\"future\":"+"[".repeat(40)+"0"+"]".repeat(40)+"}"))cases.add(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  cases.add(("{\"schemaVersion\":1,\"future\":["+"0,".repeat(33000)+"0]}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  byte[] invalid="{\"schemaVersion\":1,\"future\":\"x\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);invalid[invalid.length-3]=(byte)0xc0;cases.add(invalid);
  for(byte[] bytes:cases){Files.write(file,bytes);var store=AutomationSettingsStore.load(file);check(!store.writable(),"ambiguous/malformed consent must be unavailable");check(!store.enabled(AutomationFeature.SUPERCRAFT),"invalid root never arms a feature");try{store.setEnabled(AutomationFeature.STASH,true);throw new AssertionError("invalid file write");}catch(IOException expected){}check(Arrays.equals(bytes,Files.readAllBytes(file)),"invalid original retained byte for byte");}
  String valid="{\"schemaVersion\":1,\"future\":{\"文字\":\"甲乙🙂\",\"arr\":[true,null,{\"x\":1.25}]},\"features\":{\"stash\":false}}";Files.writeString(file,valid);var s=AutomationSettingsStore.load(file);check(s.writable(),"valid unknown fields preserved");s.setEnabled(AutomationFeature.STASH,true);String saved=Files.readString(file);check(saved.contains("甲乙🙂")&&saved.contains("1.25")&&saved.contains("null"),"unknown nested strings/numbers/arrays survive explicit save");
  var next=AutomationSettingsStore.load(file);check(next.enabled(AutomationFeature.STASH)&&!next.enabled(AutomationFeature.SUPERCRAFT),"strict read remains restart/upgrade compatible");
  System.out.println("AutomationSettingsIntegrityTest: PASS (duplicate keys at all levels/strict JSON/UTF8/depth fail closed, original bytes retained, unknown valid nested data and restart preserved)");
 }finally{try(var walk=Files.walk(dir)){for(var p:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}}
}
