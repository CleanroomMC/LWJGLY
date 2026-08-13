package org.lwjgl.opengl;

import org.lwjgl.PointerWrapperAbstract;

public final class GLSync extends PointerWrapperAbstract {

    GLSync(long pointer) {
        super(pointer);
    }

    @Override
    public boolean isValid() {
        return pointer != 0L;
    }

}
