package org.lwjgl.openal;

import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

public final class ALCcontext {

    static IntBuffer createAttributeList(int frequency, int refresh, int sync) {
        return BufferUtils.createIntBuffer(7)
                .put(0x1007).put(frequency)
                .put(0x1008).put(refresh)
                .put(0x1009).put(sync)
                .put(0);
    }

    final long context;

    private boolean valid = true;

    ALCcontext(long context) {
        this.context = context;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ALCcontext that ? context == that.context : super.equals(other);
    }

    void setInvalid() {
        valid = false;
    }

    public boolean isValid() {
        return valid;
    }

}
