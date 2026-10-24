#!/usr/bin/env python3
"""Exercise production Farming stall eligibility, timing, and Discord dispatch without a game/server."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile

root=Path(__file__).resolve().parent.parent
base=root/'src/main/java/dev/kuan/kuanperion'
source=(base/'KuanperionClient.java').read_text()
def extract(pattern):
    match=re.search(pattern,source)
    assert match, f'Missing production stall integration: {pattern}'
    start=match.start();opening=source.index('{',start);depth=1;end=opening+1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}');end+=1
    return source[start:end]
methods='\n'.join(extract(r'    private static [^\n]+ '+name+r'\(') for name in ['tickFarmingStallNotification','isFarmingIntentionalWait','isAlternatingRowCrop'])
enums='\n'.join(extract(r'    private enum '+name+r'\s*\{') for name in ['RowCropState','CactusState'])
tick=extract(r'    private static void tick\(')
assert tick.index('tickFarmingStallNotification(mc, System.nanoTime());') < tick.index('syncAutomationSound(mc);'), 'Monitor runs before runtime sound restoration'
for name in ['stopFarming','resetFarmingRuntime','onFarmingWorldChange','startFarmingAutoRecovery']:
    assert 'FARMING_STALL_GUARD.reset();' in extract(r'    private static [^\n]+ '+name+r'\('), f'Reset on {name}'
harness=r'''package dev.kuan.kuanperion;
import java.util.*;
import java.math.BigDecimal;
public class FarmingStallIntegrationTest {
    static final FarmingStallGuard FARMING_STALL_GUARD=new FarmingStallGuard();
    static final FarmingToolGuard FARMING_TOOL_GUARD=new FarmingToolGuard();
    static boolean abnormalHandlingEnabled,farmingEnabled,farmingEnabledThisSession,farmingCropDetected,farmingAutoRecovering,menuBuffer;
    static int farmingStateTicks;
    static boolean farmingStallDetectionEnabled;
    static BigDecimal farmingStallSeconds=BigDecimal.valueOf(5);
    static String pestWebhookUrl="";
    static FarmingCrop farmingCrop=FarmingCrop.WHEAT;
    static RowCropState rowCropState=RowCropState.HARVEST_PRIMARY;
    static CactusState cactusState=CactusState.HARVEST_LEFT;
    static final List<String> discord=new ArrayList<>();
    enum FarmingCrop { WHEAT,POTATO,CARROT,NETHER_WART,CACTUS,SUGAR_CANE,COCOA_BEAN,PUMPKIN_MELON,SUNFLOWER,WILD_ROSE,MUSHROOM }
    static class Minecraft {
        Player player=new Player();Object level=new Object(),options=new Object();Gui gui=new Gui();boolean paused;
        boolean isPaused() { return paused; }
    }
    static class Gui { Object current;Object screen() { return current; } }
    static class Player {
        double x,z;boolean alive=true;String tool="Correct Tool";
        double getX() { return x; } double getZ() { return z; } boolean isAlive() { return alive; }
        Item getMainHandItem() { return new Item(tool); }
    }
    record Item(String name) { Component getHoverName() { return new Component(name); } }
    record Component(String getString) {}
    static class StringUtil { static String stripColor(String value) { return value.replaceAll("(?i)§[0-9a-fk-or]",""); } }
    static boolean isFarmingPauseScreen(Object screen) { return screen!=null || menuBuffer; }
    static void stopFarming(Minecraft mc,boolean announce){farmingEnabled=farmingEnabledThisSession=false;FARMING_STALL_GUARD.reset();}
    static void saveConfig(){}
    static String warning(String s){return s;}
    static void say(Minecraft mc,String message){}
    static void sendDiscordNotification(String message) { discord.add(message); }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static final long SECOND=1_000_000_000L;
    static Minecraft reset() {
        FARMING_STALL_GUARD.reset();FARMING_TOOL_GUARD.reset("Correct Tool");
        abnormalHandlingEnabled=farmingStallDetectionEnabled=farmingEnabled=farmingEnabledThisSession=farmingCropDetected=true;farmingStallSeconds=BigDecimal.valueOf(5);
        farmingAutoRecovering=menuBuffer=false;farmingStateTicks=0;
        farmingCrop=FarmingCrop.WHEAT;rowCropState=RowCropState.HARVEST_PRIMARY;cactusState=CactusState.HARVEST_LEFT;
        pestWebhookUrl="https://example.test/hook";discord.clear();return new Minecraft();
    }
    static void sample(Minecraft mc,long now) { tickFarmingStallNotification(mc,now); }
    static void excluded(java.util.function.Consumer<Minecraft> exclusion,String label) {
        Minecraft mc=reset();sample(mc,0);sample(mc,4*SECOND);
        exclusion.accept(mc);sample(mc,5*SECOND);sample(mc,60*SECOND);
        check(discord.isEmpty(),"ignore "+label);
    }
    public static void main(String[] args) {
        Minecraft mc=reset();sample(mc,0);sample(mc,5*SECOND-1);check(discord.isEmpty(),"wait full five seconds");
        sample(mc,5*SECOND);check(discord.size()==1 && discord.get(0).contains("Farming") && discord.get(0).contains("5 秒"),"Discord alert exactly at five seconds");
        sample(mc,60*SECOND);sample(mc,600*SECOND);check(discord.size()==1,"one alert per continuous stall");
        check(!farmingEnabled && !farmingEnabledThisSession && !farmingAutoRecovering,"stall stops Farming and never restarts");
        farmingEnabled=farmingEnabledThisSession=true;
        mc.player.x=0.1;sample(mc,601*SECOND);sample(mc,606*SECOND-1);check(discord.size()==1,"movement rearms full interval");
        sample(mc,606*SECOND);check(discord.size()==2,"new stall after movement alerts again");
        mc=reset();for(int i=0;i<=120;i++) { mc.player.x=i*0.02;sample(mc,i*SECOND/10); }
        check(discord.isEmpty(),"slow cumulative movement is not stuck");
        mc=reset();for(int i=0;i<=50;i++) { mc.player.x=i%2==0?0:0.002;mc.player.z=i%2==0?0:-0.002;sample(mc,i*SECOND/10); }
        check(discord.size()==1,"tiny position jitter cannot prevent stall detection");
        excluded(m->farmingStallDetectionEnabled=false,"independent stall checkbox disabled");
        excluded(m->farmingEnabled=false,"manual Farming off / Mining only");
        excluded(m->farmingEnabledThisSession=false,"not started this session");
        excluded(m->farmingCropDetected=false,"crop identification");
        excluded(m->farmingAutoRecovering=true,"automatic recovery");
        excluded(m->m.gui.current=new Object(),"any open GUI even if smart pause is off");
        excluded(m->menuBuffer=true,"GUI-close buffer");
        excluded(m->m.paused=true,"paused game");
        excluded(m->m.player.tool="Wrong Tool","wrong/empty tool even without tool detection");
        excluded(m->m.player=null,"missing player");excluded(m->m.level=null,"missing world");
        excluded(m->m.options=null,"missing options");excluded(m->m.player.alive=false,"death");
        mc=reset();pestWebhookUrl="";sample(mc,0);sample(mc,5*SECOND);check(!farmingEnabled&&discord.isEmpty(),"missing Webhook must not prevent local stop");
        mc=reset();abnormalHandlingEnabled=false;sample(mc,0);sample(mc,5*SECOND);check(!farmingEnabled&&discord.size()==1,"stall checkbox independent of abnormal handling");
        mc=reset();farmingStallSeconds=BigDecimal.valueOf(8);sample(mc,0);sample(mc,8*SECOND-1);check(farmingEnabled&&discord.isEmpty(),"custom eight second wait");sample(mc,8*SECOND);check(!farmingEnabled&&discord.size()==1&&discord.get(0).contains("8 秒"),"custom timeout stops and notifies");
        excluded(m->farmingStateTicks=10,"normal turn wait");
        for(RowCropState state:List.of(RowCropState.PRIMARY_JUMP_THREE_SECONDS,RowCropState.SECONDARY_JUMP_THREE_SECONDS,RowCropState.WAIT_ONE_SECOND))
            excluded(m->rowCropState=state,"row crop planned wait "+state);
        for(CactusState state:List.of(CactusState.WAIT_BEFORE_HARVEST_LEFT,CactusState.WAIT_BEFORE_HARVEST_RIGHT))
            excluded(m->{farmingCrop=FarmingCrop.CACTUS;cactusState=state;},"cactus planned wait "+state);
        mc=reset();mc.gui.current=new dev.kuan.kuanperion.ui.MainGUI();sample(mc,0);sample(mc,5*SECOND);
        check(discord.size()==1,"own settings no longer suppress active farming stall monitor");
        mc=reset();sample(mc,0);sample(mc,4*SECOND);mc.gui.current=new Object();sample(mc,5*SECOND);sample(mc,100*SECOND);
        mc.gui.current=null;sample(mc,101*SECOND);sample(mc,106*SECOND-1);check(discord.isEmpty(),"GUI time cannot accumulate into stall");
        sample(mc,106*SECOND);check(discord.size()==1,"fresh five seconds after GUI resumes");
        mc=reset();sample(mc,0);sample(mc,4*SECOND);mc.player.tool="Air";sample(mc,5*SECOND);sample(mc,100*SECOND);
        mc.player.tool="§aCorrect Tool Mk. III";sample(mc,101*SECOND);sample(mc,106*SECOND-1);check(discord.isEmpty(),"wrong-tool wait does not accumulate");
        sample(mc,106*SECOND);check(discord.size()==1,"resume tool check with stripped formatting");
        for(FarmingCrop crop:FarmingCrop.values()) {
            mc=reset();farmingCrop=crop;sample(mc,0);sample(mc,5*SECOND);
            check(discord.size()==1,"normal active crop can report a stall: "+crop);
        }
        // Ignored state left over from another crop must not disable active monitoring.
        mc=reset();farmingCrop=FarmingCrop.SUGAR_CANE;rowCropState=RowCropState.WAIT_ONE_SECOND;cactusState=CactusState.WAIT_BEFORE_HARVEST_LEFT;
        sample(mc,0);sample(mc,5*SECOND);check(discord.size()==1,"other crop wait state cannot suppress monitoring");
        FarmingStallGuard guard=new FarmingStallGuard();
        check(!guard.update(true,0,0,Long.MAX_VALUE-SECOND),"start near clock wrap");
        check(guard.update(true,0,0,Long.MAX_VALUE-SECOND+5*SECOND),"elapsed timing safe across nanoTime wrap");
        guard.reset();check(!guard.update(true,0,0,-10*SECOND),"negative nanoTime baseline");
        check(!guard.update(false,0,0,0),"ineligible pure sample resets");
        check(!guard.update(true,0,0,10*SECOND) && guard.update(true,0,0,15*SECOND),"resume needs fresh interval");
        System.out.println("FarmingStallIntegrationTest: PASS (five-second boundary, movement/jitter, once per stall, GUI/tool/manual/wait/recovery exclusions, all crops, stop+Discord / independent checkbox / custom eight seconds / no-Webhook stop / clock wrap)");
    }
    // METHODS
    // ENUMS
}
'''.replace('    // METHODS',methods).replace('    // ENUMS',enums)
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'FarmingStallIntegrationTest.java';path.write_text(harness)
    java=Path(sys.argv[1])/'bin'
    stub=Path(tmp)/"MainGUI.java";stub.write_text("package dev.kuan.kuanperion.ui; public class MainGUI {}")
    deps=[stub]+[base/(name+'.java') for name in ['FarmingStallGuard','FarmingToolGuard','NotificationChannels']]
    subprocess.run([str(java/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,*map(str,deps),str(path)],check=True)
    subprocess.run([str(java/'java'),'-cp',tmp,'dev.kuan.kuanperion.FarmingStallIntegrationTest'],check=True)
