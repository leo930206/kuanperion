package dev.kuan.kuanperion.renderer;

import org.lwjgl.opengl.*;

/** Restore real GL state without changing Minecraft's state caches behind its back. */
final class GlFrameState implements AutoCloseable {
    private final int drawFbo=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int readFbo=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int vao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    private final int arrayBuffer=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    private final int uniformBuffer=GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING);
    private final int uniformZero=GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING,0);
    private final int activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int textureZero,samplerZero;
    private final int[] viewport=new int[4],scissor=new int[4],colorMask=new int[4];
    private final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND),clip=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),stencil=GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
    private final int frontFace=GL11.glGetInteger(GL11.GL_FRONT_FACE),cullFace=GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
    private final int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int equationRgb=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),equationAlpha=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    private final int clearStencil=GL11.glGetInteger(GL11.GL_STENCIL_CLEAR_VALUE);
    private final int[] front=stencilState(false),back=stencilState(true);

    GlFrameState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissor);
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK,colorMask);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        textureZero=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        samplerZero=GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        GL13.glActiveTexture(activeTexture);
    }
    private static int[] stencilState(boolean back) {
        return new int[]{
            GL11.glGetInteger(back?GL20.GL_STENCIL_BACK_FUNC:GL11.GL_STENCIL_FUNC),
            GL11.glGetInteger(back?GL20.GL_STENCIL_BACK_REF:GL11.GL_STENCIL_REF),
            GL11.glGetInteger(back?GL20.GL_STENCIL_BACK_VALUE_MASK:GL11.GL_STENCIL_VALUE_MASK),
            GL11.glGetInteger(back?GL20.GL_STENCIL_BACK_WRITEMASK:GL11.GL_STENCIL_WRITEMASK),
            GL11.glGetInteger(back?GL20.GL_STENCIL_BACK_FAIL:GL11.GL_STENCIL_FAIL),
            GL11.glGetInteger(back?GL20.GL_STENCIL_BACK_PASS_DEPTH_FAIL:GL11.GL_STENCIL_PASS_DEPTH_FAIL),
            GL11.glGetInteger(back?GL20.GL_STENCIL_BACK_PASS_DEPTH_PASS:GL11.GL_STENCIL_PASS_DEPTH_PASS)};
    }
    private static void restoreStencil(int face,int[] values) {
        GL20.glStencilFuncSeparate(face,values[0],values[1],values[2]);
        GL20.glStencilMaskSeparate(face,values[3]);
        GL20.glStencilOpSeparate(face,values[4],values[5],values[6]);
    }
    private static void capability(int cap,boolean enabled) {if(enabled)GL11.glEnable(cap);else GL11.glDisable(cap);}
    @Override public void close() {
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,arrayBuffer);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER,0,uniformZero);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER,uniformBuffer);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,textureZero);
        GL33.glBindSampler(0,samplerZero);
        GL13.glActiveTexture(activeTexture);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFbo);
        GL11.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        GL11.glScissor(scissor[0],scissor[1],scissor[2],scissor[3]);
        GL11.glColorMask(colorMask[0]!=0,colorMask[1]!=0,colorMask[2]!=0,colorMask[3]!=0);
        GL14.glBlendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
        GL20.glBlendEquationSeparate(equationRgb,equationAlpha);
        GL11.glFrontFace(frontFace);GL11.glCullFace(cullFace);
        restoreStencil(GL11.GL_FRONT,front);restoreStencil(GL11.GL_BACK,back);GL11.glClearStencil(clearStencil);
        capability(GL11.GL_DEPTH_TEST,depth);capability(GL11.GL_CULL_FACE,cull);capability(GL11.GL_BLEND,blend);capability(GL11.GL_SCISSOR_TEST,clip);capability(GL11.GL_STENCIL_TEST,stencil);
    }
}
