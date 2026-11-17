package com.autoenchantingskill.gui;

import com.autoenchantingskill.config.EnchantBotConfig;
import com.autoenchantingskill.engine.EnchantmentBotController;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AutoEnchantHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.getDebugHud().shouldShowDebugHud() || !EnchantBotConfig.isRunning) {
            return;
        }

        TextRenderer tr = client.textRenderer;
        int x = 8;
        int y = 8;
        int boxWidth = 175;
        int boxHeight = 84;

        context.fill(x, y, x + boxWidth, y + boxHeight, 0xCC12121A);
        context.fill(x, y, x + 3, y + boxHeight, 0xFFA855F7);
        context.fill(x, y, x + boxWidth, y + 1, 0x44FFFFFF);

        context.drawItem(new ItemStack(EnchantBotConfig.selectedTargetItem), x + 7, y + 5);
        context.drawTextWithShadow(tr,
            Text.literal("AutoEnchantingSkill").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
            x + 27, y + 9, 0xFFFFFF);

        String stateLabel = formatBotState(EnchantmentBotController.getCurrentState());
        context.drawTextWithShadow(tr,
            Text.literal("Status: ").formatted(Formatting.GRAY)
                .append(Text.literal(stateLabel).formatted(Formatting.GREEN)),
            x + 8, y + 26, 0xFFFFFF);

        String cycleStr = EnchantBotConfig.currentCycleCount +
            (EnchantBotConfig.infiniteLoop ? " / âˆž" : " / " + EnchantBotConfig.maxCycles);
        String tierStr = "Tier " + (EnchantBotConfig.selectedTier + 1);

        context.drawTextWithShadow(tr,
            Text.literal("Siklus: ").formatted(Formatting.GRAY)
                .append(Text.literal(cycleStr).formatted(Formatting.AQUA))
                .append(Text.literal(" (" + tierStr + ")").formatted(Formatting.YELLOW)),
            x + 8, y + 39, 0xFFFFFF);

        int lapisCount = countTotalLapis(client);
        Formatting lapisColor = lapisCount <= 9 ? Formatting.RED : (lapisCount <= 32 ? Formatting.YELLOW : Formatting.BLUE);
        String lapisWarning = lapisCount <= 9 ? " [SEGERA HABIS!]" : "";

        context.drawTextWithShadow(tr,
            Text.literal("Sisa Lapis: ").formatted(Formatting.GRAY)
                .append(Text.literal(lapisCount + " pcs" + lapisWarning).formatted(lapisColor, Formatting.BOLD)),
            x + 8, y + 52, 0xFFFFFF);

        Text grindstoneStatus;
        if (!EnchantBotConfig.autoGrindstoneCycle) {
            grindstoneStatus = Text.literal("NONAKTIF").formatted(Formatting.DARK_GRAY);
        } else if (isGrindstonePhase(EnchantmentBotController.getCurrentState())) {
            grindstoneStatus = Text.literal("SEDANG DISENCHANT...").formatted(Formatting.GOLD, Formatting.BOLD);
        } else {
            grindstoneStatus = Text.literal("SIAGA (AUTO)").formatted(Formatting.GREEN);
        }

        context.drawTextWithShadow(tr,
            Text.literal("Grindstone: ").formatted(Formatting.GRAY).append(grindstoneStatus),
            x + 8, y + 65, 0xFFFFFF);
    }

    private int countTotalLapis(MinecraftClient client) {
        int total = 0;
        if (client.player == null) return 0;

        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.LAPIS_LAZULI)) {
                total += stack.getCount();
            }
        }

        if (client.player.currentScreenHandler instanceof EnchantmentScreenHandler handler) {
            ItemStack tableLapis = handler.getSlot(1).getStack();
            if (tableLapis.isOf(Items.LAPIS_LAZULI)) {
                total += tableLapis.getCount();
            }
        }
        return total;
    }

    private String formatBotState(EnchantmentBotController.BotState state) {
        return switch (state) {
            case IDLE -> "Standby";
            case OPEN_ENCHANT_TABLE -> "Membuka Meja Enchant";
            case FILL_ENCHANT_SLOTS -> "Memasukkan Item & Lapis";
            case EXECUTE_ENCHANT -> "Melakukan Enchant...";
            case CHECK_RESULT -> "Mengecek Hasil Enchant";
            case CLOSE_FOR_GRINDSTONE -> "Menutup Meja Enchant";
            case OPEN_GRINDSTONE -> "Mencari Grindstone";
            case EXECUTE_GRINDSTONE -> "Disenchant di Grindstone";
            case CLOSE_GRINDSTONE -> "Selesai Disenchant";
        };
    }

    private boolean isGrindstonePhase(EnchantmentBotController.BotState state) {
        return state == EnchantmentBotController.BotState.CLOSE_FOR_GRINDSTONE ||
               state == EnchantmentBotController.BotState.OPEN_GRINDSTONE ||
               state == EnchantmentBotController.BotState.EXECUTE_GRINDSTONE ||
               state == EnchantmentBotController.BotState.CLOSE_GRINDSTONE;
    }
}
