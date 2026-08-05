package org.lwjgl.openal;

import java.util.HashMap;
import java.util.Map;

public final class ALCdevice {

    final long device;
    private final Map<Long, ALCcontext> contexts = new HashMap<>();

    private boolean valid = true;

    ALCdevice(long device) {
        this.device = device;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ALCdevice that ? device == that.device : super.equals(other);
    }

    void addContext(ALCcontext context) {
        synchronized (contexts) {
            contexts.put(context.context, context);
        }
    }

    void removeContext(ALCcontext context) {
        synchronized (contexts) {
            contexts.remove(context.context);
        }
    }

    void setInvalid() {
        valid = false;
        synchronized (contexts) {
            contexts.values().forEach(ALCcontext::setInvalid);
            contexts.clear();
        }
    }

}
