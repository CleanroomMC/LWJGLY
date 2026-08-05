package org.lwjgl.opengl;

/**
 * Adapts ATI_separate_stencil to GL 2.0.
 * The ATI function sets both faces, so glStencilFuncSeparateATI issues front and back calls.
 */
public class ATISeparateStencil {

    public static final int GL_STENCIL_BACK_FUNC_ATI = 34816;
    public static final int GL_STENCIL_BACK_FAIL_ATI = 34817;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_FAIL_ATI = 34818;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_PASS_ATI = 34819;

    public static void glStencilOpSeparateATI(int face, int sfail, int dpfail, int dppass) {
        GL20.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    public static void glStencilFuncSeparateATI(int frontfunc, int backfunc, int ref, int mask) {
        GL20.glStencilFuncSeparate(GL11.GL_FRONT, frontfunc, ref, mask);
        GL20.glStencilFuncSeparate(GL11.GL_BACK, backfunc, ref, mask);
    }

}
