package org.lwjgl.opengl;

import org.lwjgl.PointerWrapperAbstract;

public final class GLSync extends PointerWrapperAbstract {

    private final long pointer;

    GLSync(long pointer) {
        this.pointer = pointer;
    }

    @Override
    public long getPointer() {
        return pointer;
    }

    @Override
    public boolean isValid() {
        return pointer != 0L;
    }

}
