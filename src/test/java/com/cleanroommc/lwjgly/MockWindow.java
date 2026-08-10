package com.cleanroommc.lwjgly;

import com.cleanroommc.lwjgly.spi.WindowBridge;

import java.util.ArrayDeque;
import java.util.Deque;

final class MockWindow implements WindowBridge {

    private final Deque<KeyEvent> keyEvents = new ArrayDeque<>();
    private final Deque<MouseEvent> mouseEvents = new ArrayDeque<>();

    String title = "test";
    ContextRequest contextRequest;
    ContextResult contextResult;
    RuntimeException adoptionFailure;
    boolean closeRequested, resized, fullscreen, vsync, grabbed, textInput, current;
    boolean focused = true;
    int width = 854;
    int height = 480;
    int downScancode = -1;
    int downButton = -1;
    int swaps, pumps;
    float mouseX, mouseY, deltaX, deltaY, wheel;
    long windowHandle = 0xBEEFL;
    long contextHandle = 0xCAFEL;
    long currentWindowHandle = windowHandle;

    void pushKey(KeyEvent event) {
        keyEvents.add(event);
    }

    void pushMouse(MouseEvent event) {
        mouseEvents.add(event);
    }

    @Override
    public long handle() {
        return windowHandle;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public void title(String title) {
        this.title = title;
    }

    @Override
    public boolean closeRequested() {
        return closeRequested;
    }

    @Override
    public boolean focused() {
        return focused;
    }

    @Override
    public boolean consumeResized() {
        boolean value = resized;
        resized = false;
        return value;
    }

    @Override
    public boolean fullscreen() {
        return fullscreen;
    }

    @Override
    public void fullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    @Override
    public void vsync(boolean vsync) {
        this.vsync = vsync;
    }

    @Override
    public ContextResult adoptContext(ContextRequest request) {
        contextRequest = request;
        if (adoptionFailure != null) {
            throw adoptionFailure;
        }
        current = true;
        return contextResult != null ? contextResult : ContextResult.accepted(currentWindowHandle, contextHandle);
    }

    @Override
    public void makeCurrent() {
        current = true;
    }

    @Override
    public void releaseContext() {
        current = false;
    }

    @Override
    public void swapBuffers() {
        swaps++;
    }

    @Override
    public void pump() {
        pumps++;
    }

    @Override
    public KeyEvent nextKeyEvent() {
        return keyEvents.poll();
    }

    @Override
    public int queuedKeyEvents() {
        return keyEvents.size();
    }

    @Override
    public boolean keyDown(int sdlScancode) {
        return sdlScancode == downScancode;
    }

    @Override
    public void textInput(boolean enabled) {
        textInput = enabled;
    }

    @Override
    public MouseEvent nextMouseEvent() {
        return mouseEvents.poll();
    }

    @Override
    public int queuedMouseEvents() {
        return mouseEvents.size();
    }

    @Override
    public boolean mouseButtonDown(int sdlButton) {
        return sdlButton == downButton;
    }

    @Override
    public float mouseX() {
        return mouseX;
    }

    @Override
    public float mouseY() {
        return mouseY;
    }

    @Override
    public float takeMouseDeltaX() {
        float value = deltaX;
        deltaX = 0;
        return value;
    }

    @Override
    public float takeMouseDeltaY() {
        float value = deltaY;
        deltaY = 0;
        return value;
    }

    @Override
    public float takeMouseWheel() {
        float value = wheel;
        wheel = 0;
        return value;
    }

    @Override
    public void mousePosition(float x, float y) {
        mouseX = x;
        mouseY = y;
    }

    @Override
    public void grabMouse(boolean grab) {
        grabbed = grab;
    }

    @Override
    public boolean mouseGrabbed() {
        return grabbed;
    }

}
