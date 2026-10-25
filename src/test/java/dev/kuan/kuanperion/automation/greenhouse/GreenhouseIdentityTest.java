package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.TextureIdentity;import java.util.*;import java.nio.charset.StandardCharsets;
public final class GreenhouseIdentityTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}static String b64(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 static String property(String hash){return b64("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/"+hash+"\"}}}");}
 public static void main(String[]args){
  var hashes=Map.of(GreenhouseIdentity.Kind.ASHWREATH,"5890f50780fdecedaa85aa40bf3399e9439ee68594c6d022688165608171681d",GreenhouseIdentity.Kind.TURTELLINI,"1d1bd06a6738d0da5053eae49a1362b89489d1ac004c222504536f7bcd07679d",GreenhouseIdentity.Kind.GLASSCORN,"297de27338b9f876e570d1cc01fe1beccfc940467c5c97c467e93e79c81c25ee");
  for(var e:hashes.entrySet()){check(TextureIdentity.hash(property(e.getValue())).orElseThrow().equals(e.getValue()),"shared strict offline texture projection");check(GreenhouseIdentity.kind(property(e.getValue())).orElseThrow()==e.getKey(),"exact curated mature-crop identity");}
  String good=property(hashes.get(GreenhouseIdentity.Kind.ASHWREATH));String json=new String(Base64.getDecoder().decode(good),StandardCharsets.UTF_8);
  for(String bad:List.of("not base64",b64(json+"{}"),b64(json.replace("textures.minecraft.net","textures.minecraft.net.evil.example")),b64(json.replace("https://","https://user@")),b64(json.replace("/texture/","/texture/%2f")),b64(json.replace("\"textures\":", "\"textures\":{},\"textures\":")),property("a".repeat(64))))check(GreenhouseIdentity.kind(bad).isEmpty(),"malformed/untrusted/ambiguous/unknown skin cannot authorize harvest");
  check(GreenhouseIdentity.kind(null).isEmpty()&&GreenhouseIdentity.kind("").isEmpty(),"unknown is not a ready plant");
  System.out.println("GreenhouseIdentityTest: PASS (three exact curated crop textures, strict shared offline parser, spoof/duplicate/trailing/unknown rejection)");
 }
}
