package org.lwjgl.util;

import org.lwjgl.Sys;

/** LWJGL 2 frame timer using a clock shared by all instances. */
public class Timer {

    private static final int QUERY_INTERVAL = 50;

    private static long resolution = Sys.getTimerResolution();
    private static long currentTime;
    private static int queryCount;

    static {
        tick();
    }

    public static void tick() {
        currentTime = Sys.getTime();
        if (++queryCount > QUERY_INTERVAL) {
            queryCount = 0;
            resolution = Sys.getTimerResolution();
        }
    }

    private long startTime;
    private long lastTime;
    private boolean paused;

    public Timer() {
        reset();
        resume();
    }

    public float getTime() {
        if (!paused) {
            lastTime = currentTime - startTime;
        }
        return (float) ((double) lastTime / (double) resolution);
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        paused = true;
    }

    public void reset() {
        set(0F);
    }

    public void resume() {
        paused = false;
        startTime = currentTime - lastTime;
    }

    public void set(float newTime) {
        long ticks = (long) ((double) newTime * (double) resolution);
        startTime = currentTime - ticks;
        lastTime = ticks;
    }

    @Override
    public String toString() {
        return "Timer[Time=" + getTime() + ", Paused=" + paused + "]";
    }
    
}
