package com.autoenchantingskill.engine;

import com.autoenchantingskill.config.EnchantBotConfig;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class EnchantmentBotController {

    public enum BotState {
        IDLE,
        OPEN_ENCHANT_TABLE,
        FILL_ENCHANT_SLOTS,
        EXECUTE_ENCHANT,
        CHECK_RESULT,
        CLOSE_FOR_GRINDSTONE,
        OPEN_GRINDSTONE,
        EXECUTE_GRINDSTONE,
        CLOSE_GRINDSTONE
    }

    private static BotState currentState = BotState.IDLE;
    private static int tickCooldown = 0;
    private static BlockPos cachedEnchantTablePos = null;
    private static BlockPos cachedGrindstonePos = null;

    public static BotState getCurrentState() {
        return currentState;
    }

    public static void startBot() {
        EnchantBotConfig.isRunning = true;
        EnchantBotConfig.resetStats();
        currentState = BotState.OPEN_ENCHANT_TABLE;
        tickCooldown = 2;
        sendNotification("AutoEnchantingSkill AKTIF!", Formatting.GREEN);
    }

    public static void stopBot(String reason, boolean isAlert) {
        EnchantBotConfig.isRunning = false;
        currentState = BotState.IDLE;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (isAlert) {
                client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                client.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.6f, 1.2f);
                client.inGameHud.setTitle(Text.literal("âš  PERINGATAN âš ").formatted(Formatting.RED, Formatting.BOLD));
                client.inGameHud.setSubtitle(Text.literal(reason).formatted(Formatting.YELLOW));
            }
            sendNotification(reason, isAlert ? Formatting.RED : Formatting.GOLD);
        }
    }

    public static void onClientTick(MinecraftClient client) {
        if (!EnchantBotConfig.isRunning || client.player == null || client.world == null) {
            return;
        }

        if (tickCooldown > 0) {
            tickCooldown--;
            return;
        }

        if (!EnchantBotConfig.infiniteLoop && EnchantBotConfig.currentCycleCount >= EnchantBotConfig.maxCycles) {
            stopBot("Target siklus (" + EnchantBotConfig.maxCycles + "x) tercapai!", false);
            return;
        }

        switch (currentState) {
            case OPEN_ENCHANT_TABLE -> handleOpenEnchantTable(client);
            case FILL_ENCHANT_SLOTS -> handleFillEnchantSlots(client);
            case EXECUTE_ENCHANT -> handleExecuteEnchant(client);
            case CHECK_RESULT -> handleCheckResult(client);
            case CLOSE_FOR_GRINDSTONE -> {
                client.player.closeHandledScreen();
                currentState = BotState.OPEN_GRINDSTONE;
                tickCooldown = EnchantBotConfig.actionDelayTicks;
            }
            case OPEN_GRINDSTONE -> handleOpenGrindstone(client);
            case EXECUTE_GRINDSTONE -> handleExecuteGrindstone(client);
            case CLOSE_GRINDSTONE -> {
                client.player.closeHandledScreen();
                EnchantBotConfig.currentCycleCount++;
                currentState = BotState.OPEN_ENCHANT_TABLE;
                tickCooldown = EnchantBotConfig.actionDelayTicks;
            }
            default -> {}
        }
    }

    private static void handleOpenEnchantTable(MinecraftClient client) {
        if (client.currentScreen instanceof EnchantmentScreen) {
            currentState = BotState.FILL_ENCHANT_SLOTS;
            return;
        }
        cachedEnchantTablePos = findNearestBlock(client, Blocks.ENCHANTING_TABLE, 4.5);
        if (cachedEnchantTablePos == null) {
            stopBot("Enchanting Table tidak ditemukan di sekitar!", true);
            return;
        }
        interactWithBlock(client, cachedEnchantTablePos);
        tickCooldown = EnchantBotConfig.actionDelayTicks + 2;
    }

    private static void handleFillEnchantSlots(MinecraftClient client) {
        if (!(client.player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) {
            currentState = BotState.OPEN_ENCHANT_TABLE;
            return;
        }

        ItemStack lapisSlot = handler.getSlot(1).getStack();
        if (lapisSlot.isEmpty() || lapisSlot.getCount() < 3) {
            int invLapisSlot = findItemInHandler(handler, Items.LAPIS_LAZULI, 2);
            if (invLapisSlot == -1) {
                stopBot("LAPIS LAZULI HABIS! Isi ulang Lapis untuk melanjutkan.", true);
                return;
            }
            client.interactionManager.clickSlot(handler.syncId, invLapisSlot, 0, SlotActionType.QUICK_MOVE, client.player);
            tickCooldown = EnchantBotConfig.actionDelayTicks;
            return;
        }

        ItemStack targetSlot = handler.getSlot(0).getStack();
        if (targetSlot.isEmpty()) {
            int invItemSlot = findUnenchantedTargetItem(handler);
            if (invItemSlot == -1) {
                stopBot("Tidak ada " + EnchantBotConfig.selectedTargetItem.getName().getString() + " polos di inventory!", true);
                return;
            }
            client.interactionManager.clickSlot(handler.syncId, invItemSlot, 0, SlotActionType.QUICK_MOVE, client.player);
            tickCooldown = EnchantBotConfig.actionDelayTicks;
            return;
        }

        currentState = BotState.EXECUTE_ENCHANT;
        tickCooldown = EnchantBotConfig.actionDelayTicks;
    }

    private static void handleExecuteEnchant(MinecraftClient client) {
        if (!(client.player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) {
            currentState = BotState.OPEN_ENCHANT_TABLE;
            return;
        }

        int tier = EnchantBotConfig.selectedTier;
        int requiredExp = handler.enchantmentPower[tier];

        if (requiredExp <= 0) {
            tickCooldown = 2;
            return;
        }

        if (!client.player.isCreative() && client.player.experienceLevel < requiredExp) {
            stopBot("EXP Level tidak cukup! Butuh Level " + requiredExp, true);
            return;
        }

        client.interactionManager.clickButton(handler.syncId, tier);
        currentState = BotState.CHECK_RESULT;
        tickCooldown = EnchantBotConfig.actionDelayTicks + 2;
    }

    private static void handleCheckResult(MinecraftClient client) {
        if (!(client.player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) {
            currentState = BotState.OPEN_ENCHANT_TABLE;
            return;
        }

        ItemStack enchantedStack = handler.getSlot(0).getStack();
        if (enchantedStack.isEmpty() || !enchantedStack.hasEnchantments()) {
            currentState = BotState.FILL_ENCHANT_SLOTS;
            return;
        }

        boolean matched = checkIfMatchesDesiredEnchantments(enchantedStack);
        client.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, client.player);

        if (matched && !EnchantBotConfig.desiredEnchantmentIds.isEmpty()) {
            client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            sendNotification("BERHASIL! Enchantment sesuai target didapatkan!", Formatting.AQUA);
            EnchantBotConfig.currentCycleCount++;

            if (EnchantBotConfig.infiniteLoop || EnchantBotConfig.currentCycleCount < EnchantBotConfig.maxCycles) {
                currentState = BotState.FILL_ENCHANT_SLOTS;
                tickCooldown = EnchantBotConfig.actionDelayTicks;
            } else {
                stopBot("Selesai! Target tercapai.", false);
            }
        } else {
            if (EnchantBotConfig.autoGrindstoneCycle) {
                currentState = BotState.CLOSE_FOR_GRINDSTONE;
                tickCooldown = EnchantBotConfig.actionDelayTicks;
            } else {
                EnchantBotConfig.currentCycleCount++;
                currentState = BotState.FILL_ENCHANT_SLOTS;
                tickCooldown = EnchantBotConfig.actionDelayTicks;
            }
        }
    }

    private static void handleOpenGrindstone(MinecraftClient client) {
        if (client.currentScreen instanceof GrindstoneScreen) {
            currentState = BotState.EXECUTE_GRINDSTONE;
            return;
        }
        cachedGrindstonePos = findNearestBlock(client, Blocks.GRINDSTONE, 4.5);
        if (cachedGrindstonePos == null) {
            stopBot("Grindstone tidak ditemukan di dekat Anda (radius 4.5 blok)!", true);
            return;
        }
        interactWithBlock(client, cachedGrindstonePos);
        tickCooldown = EnchantBotConfig.actionDelayTicks + 2;
    }

    private static void handleExecuteGrindstone(MinecraftClient client) {
        if (!(client.player.currentScreenHandler instanceof GrindstoneScreenHandler handler)) {
            currentState = BotState.OPEN_GRINDSTONE;
            return;
        }

        ItemStack inputSlot = handler.getSlot(0).getStack();
        ItemStack outputSlot = handler.getSlot(2).getStack();

        if (!outputSlot.isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, client.player);
            currentState = BotState.CLOSE_GRINDSTONE;
            tickCooldown = EnchantBotConfig.actionDelayTicks;
            return;
        }

        if (inputSlot.isEmpty()) {
            int enchantedInvSlot = findEnchantedTargetItemInGrindstone(handler);
            if (enchantedInvSlot == -1) {
                currentState = BotState.CLOSE_GRINDSTONE;
                tickCooldown = EnchantBotConfig.actionDelayTicks;
                return;
            }
            client.interactionManager.clickSlot(handler.syncId, enchantedInvSlot, 0, SlotActionType.QUICK_MOVE, client.player);
            tickCooldown = EnchantBotConfig.actionDelayTicks;
        }
    }

    private static boolean checkIfMatchesDesiredEnchantments(ItemStack stack) {
        ItemEnchantmentsComponent enchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            String id = entry.getIdAsString();
            int level = enchants.getLevel(entry);
            if (EnchantBotConfig.desiredEnchantmentIds.contains(id) && level >= EnchantBotConfig.minEnchantLevel) {
                return true;
            }
        }
        return false;
    }

    private static int findItemInHandler(net.minecraft.screen.ScreenHandler handler, net.minecraft.item.Item item, int startSlot) {
        for (int i = startSlot; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private static int findUnenchantedTargetItem(EnchantmentScreenHandler handler) {
        for (int i = 2; i < handler.slots.size(); i++) {
            ItemStack s = handler.getSlot(i).getStack();
            if (s.isOf(EnchantBotConfig.selectedTargetItem) && !s.hasEnchantments()) {
                return i;
            }
        }
        return -1;
    }

    private static int findEnchantedTargetItemInGrindstone(GrindstoneScreenHandler handler) {
        for (int i = 3; i < handler.slots.size(); i++) {
            ItemStack s = handler.getSlot(i).getStack();
            if (s.isOf(EnchantBotConfig.selectedTargetItem) && s.hasEnchantments()) {
                return i;
            }
        }
        return -1;
    }

    private static BlockPos findNearestBlock(MinecraftClient client, net.minecraft.block.Block targetBlock, double radius) {
        BlockPos playerPos = client.player.getBlockPos();
        int r = (int) Math.ceil(radius);
        BlockPos bestPos = null;
        double bestDistSq = radius * radius;

        for (BlockPos pos : BlockPos.iterate(playerPos.add(-r, -r, -r), playerPos.add(r, r, r))) {
            if (client.world.getBlockState(pos).isOf(targetBlock)) {
                double distSq = client.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    bestPos = pos.toImmutable();
                }
            }
        }
        return bestPos;
    }

    private static void interactWithBlock(MinecraftClient client, BlockPos pos) {
        Vec3d hitVec = Vec3d.ofCenter(pos);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
    }

    private static void sendNotification(String message, Formatting color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("[AutoEnchant] ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
                    .append(Text.literal(message).formatted(color)),
                false
            );
        }
    }
}
