package com.example.quickuse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class QuickUseClient implements ClientModInitializer {
    public static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        Config.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickuse.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                "category.quickuse"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient mc) {
        while (openMenuKey.wasPressed()) {
            if (mc.currentScreen == null && mc.player != null) {
                mc.setScreen(new QuickUseScreen());
            }
        }

        if (mc.player == null || mc.interactionManager == null) return;

        long window = mc.getWindow().getHandle();
        boolean canUse = mc.currentScreen == null;

        for (UseEntry e : Config.entries) {
            boolean down = e.isDown(window);
            if (canUse && down && !e.wasDown) {
                QuickUser.use(mc, e);
            }
            e.wasDown = down;
        }
    }
}
