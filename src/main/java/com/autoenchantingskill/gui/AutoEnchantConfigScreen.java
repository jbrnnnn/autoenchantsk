package com.autoenchantingskill.gui;

import com.autoenchantingskill.config.EnchantBotConfig;
import com.autoenchantingskill.engine.EnchantmentBotController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class AutoEnchantConfigScreen extends Screen {

    private List<Item> allEnchantableItems;
    private List<RegistryEntry.Reference<Enchantment>> allEnchantments = new ArrayList<>();
    private int itemScrollOffset = 0;
    private int enchantScrollOffset = 0;
    private TextFieldWidget cycleCountField;

    public AutoEnchantConfigScreen() {
        super(Text.literal("AutoEnchantingSkill - Control Center"));
    }

    @Override
    protected void init() {
        super.init();
        allEnchantableItems = EnchantBotConfig.getAllEnchantableToolsAndWeapons();

        if (client != null && client.world != null) {
            Registry<Enchantment> registry = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            allEnchantments = registry.streamEntries().toList();
        }

        int leftX = 20;
        int midX = this.width / 3 + 15;
        int rightX = (this.width / 3) * 2 + 15;
        int panelWidth = (this.width / 3) - 30;

        int tierBtnW = (panelWidth - 10) / 3;
        for (int i = 0; i < 3; i++) {
            final int tierIdx = i;
            String label = (i == EnchantBotConfig.selectedTier ? "Â§aâœ” " : "") + "Tier " + (i + 1);
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                EnchantBotConfig.selectedTier = tierIdx;
                this.clearAndInit();
            }).dimensions(rightX + (i * (tierBtnW + 5)), 55, tierBtnW, 20).build());
        }

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Mode Siklus: " + (EnchantBotConfig.infiniteLoop ? "Â§bâˆž TAK TERHINGGA" : "Â§eTERBATAS")),
            btn -> {
                EnchantBotConfig.infiniteLoop = !EnchantBotConfig.infiniteLoop;
                this.clearAndInit();
            }
        ).dimensions(rightX, 85, panelWidth, 20).build());

        cycleCountField = new TextFieldWidget(this.textRenderer, rightX, 112, panelWidth, 18, Text.literal("Jumlah Siklus"));
        cycleCountField.setText(String.valueOf(EnchantBotConfig.maxCycles));
        cycleCountField.setEditable(!EnchantBotConfig.infiniteLoop);
        cycleCountField.setChangedListener(val -> {
            try {
                int parsed = Integer.parseInt(val);
                if (parsed > 0) EnchantBotConfig.maxCycles = parsed;
            } catch (NumberFormatException ignored) {}
        });
        this.addDrawableChild(cycleCountField);

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Auto-Grindstone Loop: " + (EnchantBotConfig.autoGrindstoneCycle ? "Â§aON" : "Â§cOFF")),
            btn -> {
                EnchantBotConfig.autoGrindstoneCycle = !EnchantBotConfig.autoGrindstoneCycle;
                this.clearAndInit();
            }
        ).dimensions(rightX, 140, panelWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Min Level Target: Â§6Lv." + EnchantBotConfig.minEnchantLevel),
            btn -> {
                EnchantBotConfig.minEnchantLevel = (EnchantBotConfig.minEnchantLevel % 5) + 1;
                this.clearAndInit();
            }
        ).dimensions(rightX, 168, panelWidth, 20).build());

        Text masterText = EnchantBotConfig.isRunning
            ? Text.literal("â¹ HENTIKAN ENCHANT").formatted(Formatting.RED, Formatting.BOLD)
            : Text.literal("â–¶ MULAI AUTO-ENCHANT").formatted(Formatting.GREEN, Formatting.BOLD);

        this.addDrawableChild(ButtonWidget.builder(masterText, btn -> {
            if (EnchantBotConfig.isRunning) {
                EnchantmentBotController.stopBot("Dihentikan secara manual oleh pengguna.", false);
            } else {
                EnchantmentBotController.startBot();
                this.close();
            }
        }).dimensions(rightX, this.height - 50, panelWidth, 30).build());

        int visibleRows = (this.height - 80) / 22;
        for (int i = 0; i < visibleRows; i++) {
            int idx = i + itemScrollOffset;
            if (idx >= allEnchantableItems.size()) break;
            Item item = allEnchantableItems.get(idx);
            boolean isSelected = (item == EnchantBotConfig.selectedTargetItem);
            String prefix = isSelected ? "Â§eâ–¶ " : "Â§7";

            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(prefix + item.getName().getString()),
                btn -> {
                    EnchantBotConfig.selectedTargetItem = item;
                    this.clearAndInit();
                }
            ).dimensions(leftX + 22, 45 + (i * 22), panelWidth - 22, 20).build());
        }

        for (int i = 0; i < visibleRows; i++) {
            int idx = i + enchantScrollOffset;
            if (idx >= allEnchantments.size()) break;
            RegistryEntry.Reference<Enchantment> entry = allEnchantments.get(idx);
            boolean selected = EnchantBotConfig.isEnchantmentSelected(entry.registryKey());
            String name = Enchantment.getName(entry, 1).getString().replace(" I", "");
            String prefix = selected ? "Â§a[âœ”] " : "Â§8[ ] ";

            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(prefix + name),
                btn -> {
                    EnchantBotConfig.toggleEnchantment(entry.registryKey());
                    this.clearAndInit();
                }
            ).dimensions(midX, 45 + (i * 22), panelWidth, 20).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC12121A);
        context.fill(0, 0, this.width, 32, 0xEE1E1E2E);
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("âœ¦ AUTO ENCHANTING SKILL âœ¦").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
            this.width / 2, 11, 0xFFFFFF);

        int leftX = 20;
        int midX = this.width / 3 + 15;
        int rightX = (this.width / 3) * 2 + 15;

        context.drawTextWithShadow(this.textRenderer, Text.literal("1. PILIH TOOL / WEAPON").formatted(Formatting.GOLD, Formatting.BOLD), leftX, 35, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("2. TARGET ENCHANTMENT").formatted(Formatting.AQUA, Formatting.BOLD), midX, 35, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("3. PENGATURAN SIKLUS").formatted(Formatting.GREEN, Formatting.BOLD), rightX, 35, 0xFFFFFF);

        int visibleRows = (this.height - 80) / 22;
        for (int i = 0; i < visibleRows; i++) {
            int idx = i + itemScrollOffset;
            if (idx >= allEnchantableItems.size()) break;
            Item item = allEnchantableItems.get(idx);
            context.drawItem(new ItemStack(item), leftX + 2, 47 + (i * 22));
        }

        String statusStr = "Siklus Berjalan: " + EnchantBotConfig.currentCycleCount +
            (EnchantBotConfig.infiniteLoop ? " / âˆž" : " / " + EnchantBotConfig.maxCycles);
        context.drawTextWithShadow(this.textRenderer, Text.literal(statusStr).formatted(Formatting.GRAY), rightX, 198, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Scroll mouse di kolom 1 & 2").formatted(Formatting.DARK_GRAY), rightX, 212, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int delta = verticalAmount > 0 ? -1 : 1;
        if (mouseX < this.width / 3.0) {
            itemScrollOffset = Math.max(0, Math.min(allEnchantableItems.size() - 5, itemScrollOffset + delta));
            this.clearAndInit();
            return true;
        } else if (mouseX < (this.width / 3.0) * 2.0) {
            enchantScrollOffset = Math.max(0, Math.min(allEnchantments.size() - 5, enchantScrollOffset + delta));
            this.clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
