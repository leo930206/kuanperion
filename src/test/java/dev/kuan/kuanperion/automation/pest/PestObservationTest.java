package dev.kuan.kuanperion.automation.pest;

import dev.kuan.kuanperion.automation.ItemSnapshot;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PestObservationTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static String texture(String url){return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\""+url+"\"}}}").getBytes(StandardCharsets.UTF_8));}
    static ItemSnapshot tool(String id,List<String> lore){return new ItemSnapshot(id,"minecraft:paper","tool","Renamed tool","fingerprint",1,lore,true);}
    static final String HASH="70a1e836bf1968b2eaa4837227a19204f17295d870ee9e754bd6b6d60ddbed3c";
    public static void main(String[] args){
        var tab=PestTabObservation.parse(List.of("§aPests:"," Alive: §b3"," Plots: 1, #24"," Cooldown: 1m 20s"," Bonus: INACTIVE","Visitors: (5)"));
        check(tab.alive()==3&&tab.plotsKnown()&&tab.plots().equals(Set.of(1,24)),"scoped Pest count and exact plot IDs");
        check(tab.cooldown().state()==PestTabObservation.CooldownState.WAITING&&tab.cooldown().seconds()==80&&tab.bonus()==PestTabObservation.Bonus.INACTIVE,"scoped cooldown/bonus");
        check(PestTabObservation.parse(List.of("Alive: 9","Plots: 1")).alive()==-1,"bare Alive cannot impersonate Pest");
        check(PestTabObservation.parse(List.of("Pests:","Visitors: (5)","Alive: 9")).alive()==-1,"new section ends Pest scope");
        check(PestTabObservation.parse(List.of("Pests: (0)","Cooldown: READY")).alive()==0,"explicit zero is known, not unknown");
        check(PestTabObservation.parse(List.of("Pests:\nAlive: 8\nPlots: 2, 3")).plots().equals(Set.of(2,3)),"multiline server Tab entries");
        check(PestTabObservation.parse(List.of("\u200bPests:\u00a0","Alive: × 4")).alive()==4,"legacy formatting normalized without digit guessing");
        check(PestTabObservation.parse(List.of("Pests: 3","Alive: 4")).alive()==-1,"conflicting counts unknown, never max");
        check(PestTabObservation.parse(List.of("Pests: 9999999999999999999999","Alive: 3")).alive()==-1,"malformed counts cannot later become authorized");
        check(PestTabObservation.parse(List.of("Pests: -1")).alive()==-1,"negative count unknown");
        check(PestTabObservation.parse(List.of("Pests: 3","Plots: 1-2")).plotsKnown()==false,"range is not converted into plot12");
        check(!PestTabObservation.parse(List.of("Pests: 3","Plots: 0, 25")).plotsKnown(),"Barn/invalid plot not made a destination");
        check(!PestTabObservation.parse(List.of("Pests: 3","Plots: 1","Plots: 2")).plotsKnown(),"conflicting plot lists unknown");
        check(PestTabObservation.parse(List.of("Pests: 3","Cooldown: MAX PESTS")).cooldown().state()==PestTabObservation.CooldownState.MAX_PESTS,"maximum pests not a fabricated timer");
        check(PestTabObservation.parse(List.of("Pests: 3","Cooldown: 1m 99s")).cooldown().state()==PestTabObservation.CooldownState.UNKNOWN,"invalid seconds not normalized");
        check(PestTabObservation.parse(Collections.nCopies(129,"Pests: 3")).alive()==-1,"bounded input");
        check(PestTabObservation.parse(Arrays.asList(null,"Pests: 3")).alive()==-1,"null server entry fails closed");
        check(PestIdentity.knownTexture(texture("https://textures.minecraft.net/texture/"+HASH)),"exact known skin URL");
        check(PestIdentity.knownTexture(texture("http://textures.minecraft.net/texture/"+HASH)),"Mojang historical HTTP property, no fetch");
        for(String bad:List.of("https://evil.test/texture/"+HASH,"https://textures.minecraft.net.evil.test/texture/"+HASH,"https://textures.minecraft.net@evil.test/texture/"+HASH,"https://textures.minecraft.net:443/texture/"+HASH,"https://textures.minecraft.net/texture/"+HASH+"?note=x","https://textures.minecraft.net/texture/unknown"))
            check(!PestIdentity.knownTexture(texture(bad)),"untrusted/noncanonical texture URL rejected");
        check(!PestIdentity.knownTexture(Base64.getEncoder().encodeToString(("{\"note\":\""+HASH+"\"}").getBytes(StandardCharsets.UTF_8))),"hash substring outside skin URL not proof");
        check(!PestIdentity.knownTexture("%bad%")&&!PestIdentity.knownTexture("A".repeat(8193)),"bad/oversized base64");
        var duplicate="{\"textures\":{},\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/"+HASH+"\"}}}";
        check(!PestIdentity.knownTexture(Base64.getEncoder().encodeToString(duplicate.getBytes(StandardCharsets.UTF_8))),"duplicate JSON keys never authorize target");
        var lore=List.of("Ability: Vacuum RIGHT CLICK","Suck pests within 12.5 blocks","Vacuum Bag: 0/40");
        check(PestIdentity.vacuum(3,tool("INFINI_VACUUM",lore)).orElseThrow().range()==12.5,"exact official vacuum ID and explicit ability range, tools remain disposal protected");
        check(PestIdentity.vacuum(3,tool("TURBOMAX_VACUUM",lore)).isEmpty(),"non-VACUUM-category item is not a supported Pest tool");
        check(PestIdentity.vacuum(9,tool("INFINI_VACUUM",lore)).isEmpty(),"no main inventory tool selected");
        check(PestIdentity.vacuum(3,tool("INFINI_VACUUM",List.of("Legendary Vacuum"))).isEmpty(),"no rarity/name fallback range");
        check(PestIdentity.vacuum(3,tool("INFINI_VACUUM",List.of("Ability: Vacuum","within 5 blocks, within 15 blocks"))).isEmpty(),"ambiguous ability range rejected");
        check(PestIdentity.vacuum(3,tool("INFINI_VACUUM",List.of("Ability: Vacuum","within 999 blocks"))).isEmpty(),"unbounded tool range rejected");
        check(PestIdentity.vacuum(3,tool("INFINI_VACUUM",List.of("Ability: Vacuum Cleaner","within 5 blocks"))).isEmpty(),"different ability name not a prefix match");
        var deep="[".repeat(10)+"0"+"]".repeat(10);check(!PestIdentity.knownTexture(Base64.getEncoder().encodeToString(deep.getBytes(StandardCharsets.UTF_8))),"bounded JSON depth");
        var standalone=new PestTargets.Fact(1,new UUID(0,1),PestTargets.Kind.BAT,new PestTargets.Point(1,70,1),true,false,false);
        var marker=new PestTargets.Fact(2,new UUID(0,2),PestTargets.Kind.ARMOR_STAND,new PestTargets.Point(1,70,1),true,true,true);
        check(PestTargets.select(List.of(standalone)).isEmpty(),"unmarked bat is not classified as a pest");
        var targets=PestTargets.select(List.of(standalone,marker));check(targets.size()==1&&targets.get(0).id()==1&&targets.get(0).markerId()==2,"known invisible marker binds nearby real mob");
        var distant=new PestTargets.Fact(1,new UUID(0,1),PestTargets.Kind.BAT,new PestTargets.Point(10,70,10),true,false,false);
        targets=PestTargets.select(List.of(distant,marker));check(targets.size()==1&&targets.get(0).id()==2,"never bind distant ambient mob");
        var unknownMarker=new PestTargets.Fact(2,new UUID(0,2),PestTargets.Kind.ARMOR_STAND,new PestTargets.Point(1,70,1),true,true,false);
        check(PestTargets.select(List.of(standalone,unknownMarker)).isEmpty(),"unknown skin with nearby bat is not proof");
        var visibleMarker=new PestTargets.Fact(2,new UUID(0,2),PestTargets.Kind.ARMOR_STAND,new PestTargets.Point(1,70,1),true,false,true);
        check(PestTargets.select(List.of(visibleMarker)).isEmpty(),"ordinary decorated armor stand not assumed pest marker");
        check(PestTargets.select(List.of(marker,marker)).isEmpty(),"aliased entity IDs rejected");
        var twin=new PestTargets.Fact(3,new UUID(0,3),PestTargets.Kind.BAT,new PestTargets.Point(1,70,1),true,false,false);
        targets=PestTargets.select(List.of(marker,standalone,twin));check(targets.size()==1&&targets.get(0).id()==2,"ambiguous equidistant mobs are not guessed");
        var deadMarker=new PestTargets.Fact(2,new UUID(0,2),PestTargets.Kind.ARMOR_STAND,new PestTargets.Point(1,70,1),false,true,true);
        check(PestTargets.select(List.of(deadMarker,standalone)).isEmpty(),"dead/removed marker cannot authorize target");
        var excessive=new ArrayList<PestTargets.Fact>();for(int i=0;i<257;i++)excessive.add(new PestTargets.Fact(i,new UUID(0,i),PestTargets.Kind.ARMOR_STAND,new PestTargets.Point(i,70,1),true,true,true));
        check(PestTargets.select(excessive).isEmpty(),"candidate bound rejects oversized scans instead of O(n squared) render work");
        System.out.println("PestObservationTest: PASS (bounded scoped Tab, unknown/conflict rejection, exact skin URL and verified tool IDs, explicit ability range)");
    }
}
