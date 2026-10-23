package dev.kuan.kuanperion.renderer;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryUtil;

/** Runtime block-material icons from the active resource pack; no Minecraft assets bundled. */
final class BlockIconCache implements AutoCloseable {
    private record Icon(int image,int width,int height) {}
    private final long vg;
    private final NVGPaint paint;
    private final LinkedHashMap<String,Icon> icons=new LinkedHashMap<>(16,.75f,true);
    private Object modelSet;
    BlockIconCache(long vg,NVGPaint paint){this.vg=vg;this.paint=paint;}
    boolean draw(String id,float x,float y,float size){
        var current=Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if(current!=modelSet){close();modelSet=current;}
        Icon icon=icons.computeIfAbsent(id,this::load);
        if(icon.image==0)return false;
        // Animated textures are vertical strips. Display only the first square frame.
        NanoVG.nvgImagePattern(vg,x,y,size,size*icon.height/icon.width,0,icon.image,1,paint);
        NanoVG.nvgBeginPath(vg);NanoVG.nvgRect(vg,x,y,size,size);
        NanoVG.nvgFillPaint(vg,paint);NanoVG.nvgFill(vg);
        if(icons.size()>64){var it=icons.entrySet().iterator();var old=it.next();if(old.getValue().image!=0)NanoVG.nvgDeleteImage(vg,old.getValue().image);it.remove();}
        return true;
    }
    private Icon load(String id){
        try{
            var mc=Minecraft.getInstance();
            var state=BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)).defaultBlockState();
            var sprite=mc.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite().contents().name();
            var path=Identifier.fromNamespaceAndPath(sprite.getNamespace(),"textures/"+sprite.getPath()+".png");
            var resource=mc.getResourceManager().getResource(path);
            if(resource.isEmpty())return new Icon(0,1,1);
            byte[] png;
            try(var in=resource.get().open()){png=in.readNBytes(8*1024*1024+1);}
            if(png.length>8*1024*1024)return new Icon(0,1,1);
            ByteBuffer buffer=MemoryUtil.memAlloc(png.length);
            try{
                buffer.put(png).flip();int image=NanoVG.nvgCreateImageMem(vg,NanoVG.NVG_IMAGE_NEAREST,buffer);
                if(image==0)return new Icon(0,1,1);
                int[] width={0},height={0};NanoVG.nvgImageSize(vg,image,width,height);
                return new Icon(image,Math.max(1,width[0]),Math.max(1,height[0]));
            }finally{MemoryUtil.memFree(buffer);}
        }catch(java.io.IOException|IllegalArgumentException ex){return new Icon(0,1,1);}
    }
    @Override public void close(){for(var icon:icons.values())if(icon.image!=0)NanoVG.nvgDeleteImage(vg,icon.image);icons.clear();}
}
