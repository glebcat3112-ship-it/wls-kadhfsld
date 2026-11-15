package com.example.quickuse;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class QuickUser {

    public static void use(MinecraftClient mc, UseEntry e) {
        ClientPlayerEntity player = mc.player;
        ClientPlayerInteractionManager im = mc.interactionManager;
        if (player == null || im == null) return;

        Item target = e.resolveItem();
        if (target == null) {
            say(mc, Text.literal("[QuickUse] Неизвестный предмет: «" + e.item + "»").formatted(Formatting.RED));
            return;
        }

        String name = e.displayName(target);
        PlayerInventory inv = player.getInventory();
        int selected = inv.selectedSlot;

        // 1) Уже в руке
        if (inv.getStack(selected).isOf(target)) {
            rightClick(mc);
            sayUsed(mc, name);
            return;
        }

        // 2) Ищем: сначала хотбар (0-8), потом основной инвентарь (9-35)
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(target)) {
                slot = i;
                break;
            }
        }
        if (slot == -1) {
            say(mc, Text.literal("[QuickUse] Предмет не найден: " + name).formatted(Formatting.RED));
            return;
        }

        if (slot < 9) {
            // Предмет в хотбаре: переключаем слот, используем, возвращаем слот
            inv.selectedSlot = slot;
            rightClick(mc);
            inv.selectedSlot = selected; // сервер получит возврат слота на следующем тике
        } else {
            // Предмет в инвентаре: меняем местами с активным слотом хотбара, используем, меняем обратно
            int syncId = player.playerScreenHandler.syncId;
            im.clickSlot(syncId, slot, selected, SlotActionType.SWAP, player);
            rightClick(mc);
            im.clickSlot(syncId, slot, selected, SlotActionType.SWAP, player);
        }

        sayUsed(mc, name);
    }

    /** Имитация обычного ПКМ: сущность -> блок -> предмет. */
    private static void rightClick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        ClientPlayerInteractionManager im = mc.interactionManager;
        if (player == null || im == null || mc.world == null) return;

        Hand hand = Hand.MAIN_HAND;
        HitResult hit = mc.crosshairTarget;

        if (hit != null) {
            switch (hit.getType()) {
                case ENTITY -> {
                    EntityHitResult ehr = (EntityHitResult) hit;
                    Entity entity = ehr.getEntity();
                    if (!mc.world.getWorldBorder().contains(entity.getBlockPos())) return;
                    ActionResult r = im.interactEntityAtLocation(player, entity, ehr, hand);
                    if (!r.isAccepted()) r = im.interactEntity(player, entity, hand);
                    if (r instanceof ActionResult.Success s) {
                        swing(player, hand, s);
                        return;
                    }
                }
                case BLOCK -> {
                    BlockHitResult bhr = (BlockHitResult) hit;
                    ActionResult r = im.interactBlock(player, hand, bhr);
                    if (r instanceof ActionResult.Success s) {
                        swing(player, hand, s);
                        return;
                    }
                    if (r instanceof ActionResult.Fail) return;
                }
                default -> {
                }
            }
        }

        if (player.getStackInHand(hand).isEmpty()) return;
        ActionResult r = im.interactItem(player, hand);
        if (r instanceof ActionResult.Success s) {
            swing(player, hand, s);
        }
    }

    private static void swing(ClientPlayerEntity player, Hand hand, ActionResult.Success s) {
        if (s.swingSource() == ActionResult.SwingSource.CLIENT) {
            player.swingHand(hand);
        }
    }

    private static void sayUsed(MinecraftClient mc, String name) {
        say(mc, Text.literal("[QuickUse] ").formatted(Formatting.GOLD)
                .append(Text.literal(name).formatted(Formatting.YELLOW))
                .append(Text.literal(" использован").formatted(Formatting.GREEN)));
    }

    /** Сообщение только в локальный чат (на сервер не отправляется). */
    private static void say(MinecraftClient mc, Text text) {
        mc.inGameHud.getChatHud().addMessage(text);
    }
}
