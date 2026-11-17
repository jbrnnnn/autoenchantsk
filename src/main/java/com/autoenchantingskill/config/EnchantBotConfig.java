package com.autoenchantingskill.config;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnchantBotConfig {
    public static boolean isRunning = false;
    public static Item selectedTargetItem = Items.DIAMOND_SWORD;
    public static Set<String> desiredEnchantmentIds = new HashSet<>();
    public static int minEnchantLevel = 1;

    public static int selectedTier = 2;
    public static boolean infiniteLoop = true;
    public static int maxCycles = 10;
    public static int currentCycleCount = 0;

    public static boolean autoGrindstoneCycle = true;
    public static int actionDelayTicks = 6;

    public static List<Item> getAllEnchantableToolsAndWeapons() {
        List<Item> enchantables = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item instanceof SwordItem ||
                item instanceof MiningToolItem ||
                item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof TridentItem ||
                item instanceof MaceItem ||
                item instanceof FishingRodItem ||
                item instanceof ShearsItem ||
                item instanceof ShieldItem ||
                item instanceof ArmorItem ||
                item == Items.BOOK) {
                enchantables.add(item);
            }
        }
        return enchantables;
    }

    public static void toggleEnchantment(RegistryKey<Enchantment> key) {
        String id = key.getValue().toString();
        if (desiredEnchantmentIds.contains(id)) {
            desiredEnchantmentIds.remove(id);
        } else {
            desiredEnchantmentIds.add(id);
        }
    }

    public static boolean isEnchantmentSelected(RegistryKey<Enchantment> key) {
        return desiredEnchantmentIds.contains(key.getValue().toString());
    }

    public static void resetStats() {
        currentCycleCount = 0;
    }
}
