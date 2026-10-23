package dev.kuan.kuanperion.ui.theme;
import com.google.gson.*;
import java.nio.file.*;
public final class ColorPresetStoreTest {
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("colors-five-");
  try{
   var source=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
   source.addProperty("pestWebhookUrl","secret-never-store");source.addProperty("uiScale",3);source.addProperty("inputHeight",16);source.addProperty("animSpeed",999);
   var store=new ColorPresetStore(dir);
   for(int slot=1;slot<=5;slot++){
    require(!store.occupied(slot),"starts empty");
    require(store.read(slot)==null,"empty load returns no snapshot");
    source.addProperty("Accent",String.format("FF01020%d",slot));
    store.save(slot,source,false);
   }
   var reopened=new ColorPresetStore(dir);
   for(int slot=1;slot<=5;slot++)require(reopened.read(slot).get("Accent").getAsString().endsWith(""+slot),"five independent persisted slots");
   String saved=Files.readString(dir.resolve("slot-1.json"));
   require(!saved.contains("secret-never-store")&&!saved.contains("uiScale")&&!saved.contains("inputHeight")&&!saved.contains("animSpeed"),"colors only allowlist");
   var detached=reopened.read(1);detached.addProperty("Accent","FF000000");require(!reopened.read(1).get("Accent").getAsString().equals("FF000000"),"read copies immutable snapshot");
   expectFailure(()->reopened.save(1,source,false));require(Files.readString(dir.resolve("slot-1.json")).equals(saved),"overwrite requires confirmation");
   reopened.save(1,source,true);require(Files.readString(dir.resolve("slot-1.json.bak")).equals(saved),"previous snapshot backup");
   expectFailure(()->reopened.save(0,source,true));expectFailure(()->reopened.read(6));
   var bad=source.deepCopy();bad.addProperty("Accent","oops");final var badHex=bad;expectFailure(()->reopened.save(2,badHex,true));
   bad=source.deepCopy();bad.addProperty("gradientCoverage",Float.NaN);final var badRange=bad;expectFailure(()->reopened.save(2,badRange,true));
   require(reopened.read(2).get("Accent").getAsString().equals("FF010202"),"failed save preserves slot");
   Files.writeString(dir.resolve("slot-3.json"),"{broken");
   Files.writeString(dir.resolve("slot-4.json")," ".repeat(65537));
   var corrupt=new ColorPresetStore(dir);require(corrupt.occupied(3)&&corrupt.occupied(4),"damaged files not silently treated as empty");
   expectFailure(()->corrupt.read(3));expectFailure(()->corrupt.read(4));require(corrupt.read(5)!=null,"bad slot isolated");
   corrupt.save(3,source,true);require(new ColorPresetStore(dir).read(3)!=null,"explicit repair by overwrite");
   try(var files=Files.list(dir)){require(files.noneMatch(p->p.toString().endsWith(".tmp")),"no temp file leftovers");}
   require(corrupt.firstEmptySlot()==0,"full collection cannot add a sixth");
   String deleted=Files.readString(dir.resolve("slot-2.json"));
   corrupt.delete(2);require(!corrupt.occupied(2)&&corrupt.firstEmptySlot()==2,"delete frees slot");
   require(Files.readString(dir.resolve("slot-2.json.deleted.bak")).equals(deleted),"delete retains exact backup");
   require(!new ColorPresetStore(dir).occupied(2),"deleted palette stays deleted after restart");
   corrupt.save(2,source,false);require(new ColorPresetStore(dir).occupied(2),"add reuses free slot");
   expectFailure(()->corrupt.delete(0));expectFailure(()->corrupt.delete(6));
   System.out.println("ColorPresetStoreTest: PASS (five-slot/restart/allowlist/overwrite/backup/corruption/size/atomic cleanup)");
  }finally{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
 interface Checked{void run()throws Exception;}
 static void expectFailure(Checked call)throws Exception{boolean failed=false;try{call.run();}catch(java.io.IOException|IllegalArgumentException ex){failed=true;}require(failed,"must reject invalid operation");}
 static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
