package com.autoenchantingskill;

import com.autoenchantingskill.config.EnchantBotConfig;
import com.autoenchantingskill.engine.EnchantmentBotController;
import com.autoenchantingskill.gui.AutoEnchantConfigScreen;
import com.autoenchantingskill.gui.AutoEnchantHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AutoEnchantingSkillClient implements ClientModInitializer {

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autoenchantingskill.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.autoenchantingskill"
        ));

        HudRenderCallback.EVENT.register(new AutoEnchantHudOverlay());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AutoEnchantConfigScreen());
                }
            }

            if (EnchantBotConfig.isRunning) {
                EnchantmentBotController.onClientTick(client);
            }
        });
    }
}
