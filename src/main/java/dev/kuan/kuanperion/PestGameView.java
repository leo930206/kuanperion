// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;

import dev.kuan.kuanperion.automation.*;
import dev.kuan.kuanperion.automation.pest.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.StringUtil;

/** Client-thread, on-demand, read-only Pest facts; the separate leased controller owns effects. */
public final class PestGameView {
    private PestGameView(){}
    public record Snapshot(long world,long observedAt,PestTabObservation tab,List<PestTargets.Target> targets,
                           List<PestIdentity.Vacuum> vacuums,int selectedSlot,boolean complete,PestTrapObservation traps,List<PestTrapTarget> trapTargets){
        public Snapshot {targets=List.copyOf(targets);vacuums=List.copyOf(vacuums);Objects.requireNonNull(tab);Objects.requireNonNull(traps);trapTargets=List.copyOf(trapTargets);}
        public Snapshot(long world,long observedAt,PestTabObservation tab,List<PestTargets.Target> targets,List<PestIdentity.Vacuum> vacuums,int selectedSlot,boolean complete){this(world,observedAt,tab,targets,vacuums,selectedSlot,complete,PestTrapObservation.unknown(),List.of());}
        public boolean fresh(long currentWorld,long now){long elapsed=now-observedAt;return complete&&world>0&&world==currentWorld&&elapsed>=0&&elapsed<=2_000_000_000L;}
    }
    private static Snapshot unknown(long world,long now){return new Snapshot(world,now,PestTabObservation.unknown(),List.of(),List.of(),-1,false);}
    public static Optional<PestIdentity.Vacuum> vacuum(ItemStack stack,int slot){return PestIdentity.vacuum(slot,AutomationGameView.item(stack,true));}
    public static boolean knownHead(ItemStack stack){
        if(stack==null||stack.isEmpty()||!stack.is(Items.PLAYER_HEAD)||stack.has(DataComponents.CUSTOM_NAME))return false;
        var profile=stack.get(DataComponents.PROFILE);if(profile==null||!profile.skinPatch().equals(net.minecraft.world.entity.player.PlayerSkin.Patch.EMPTY)||profile.partialProfile().properties()==null)return false;
        // Never resolveProfile()/fetch a skin. More than one property is an ambiguous identity.
        var textures=profile.partialProfile().properties().get("textures");
        return textures.size()==1&&PestIdentity.knownTexture(textures.iterator().next().value());
    }
    public static Optional<PestTrapTarget> trapMarker(int id,UUID uuid,String label,net.minecraft.world.phys.Vec3 position){
        if(position==null)return Optional.empty();try{return PestTrapTarget.read(id,uuid,StringUtil.stripColor(label),new PestTargets.Point(position.x,position.y,position.z));}catch(RuntimeException malformed){return Optional.empty();}
    }
    public static Snapshot read(Minecraft mc,AutomationContext context,long now){
        long world=context==null?0:context.worldId();
        if(mc==null||context==null||!context.garden()||context.hardBlocked()||mc.level==null||mc.player==null||!mc.isSameThread())return unknown(world,now);
        try{
            var facts=new ArrayList<PestTargets.Fact>();var traps=new ArrayList<PestTrapTarget>();int inspected=0;
            for(var entity:mc.level.entitiesForRendering()){
                if(++inspected>4096)return unknown(world,now);
                if(entity==mc.player)continue;
                PestTargets.Kind kind=entity instanceof ArmorStand?PestTargets.Kind.ARMOR_STAND:entity instanceof Bat?PestTargets.Kind.BAT:entity instanceof Silverfish?PestTargets.Kind.SILVERFISH:PestTargets.Kind.OTHER;
                if(kind==PestTargets.Kind.OTHER)continue;
                boolean available=!entity.isRemoved()&&(!(entity instanceof LivingEntity living)||!living.isDeadOrDying());
                if(available&&entity instanceof ArmorStand stand&&stand.hasCustomName()){
                    trapMarker(stand.getId(),stand.getUUID(),stand.getCustomName().getString(),stand.position().add(0,stand.getEyeHeight()-.5,0)).ifPresent(traps::add);
                    if(traps.size()>24)return unknown(world,now);
                }
                boolean known=entity instanceof ArmorStand stand&&knownHead(stand.getItemBySlot(EquipmentSlot.HEAD));
                if(kind==PestTargets.Kind.ARMOR_STAND&&!known)continue;
                if(facts.size()>=256)return unknown(world,now);
                var position=new PestTargets.Point(entity.getX(),entity.getY(),entity.getZ());
                facts.add(new PestTargets.Fact(entity.getId(),entity.getUUID(),kind,position,available,entity.isInvisible(),known));
            }
            if(facts.stream().map(PestTargets.Fact::id).distinct().count()!=facts.size()||facts.stream().map(PestTargets.Fact::uuid).distinct().count()!=facts.size())return unknown(world,now);
            var lines=GardenTabReader.read(mc);var tab=PestTabObservation.parse(lines);var tools=new ArrayList<PestIdentity.Vacuum>();
            for(int slot=0;slot<9;slot++)vacuum(mc.player.getInventory().getItem(slot),slot).ifPresent(tools::add);
            return new Snapshot(world,now,tab,PestTargets.select(facts),tools,mc.player.getInventory().getSelectedSlot(),tab.alive()>=0,PestTrapObservation.parse(lines),PestTrapTarget.unique(traps));
        }catch(RuntimeException malformed){return unknown(world,now);}
    }
}
