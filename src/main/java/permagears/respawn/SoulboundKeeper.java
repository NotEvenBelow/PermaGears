package permagears.respawn;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameRules;
import permagears.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulboundKeeper {
    private static final Map<UUID, List<ItemStack>> STASH = new ConcurrentHashMap<>();

    private SoulboundKeeper() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (ConfigManager.disableSoulbound()) return true; // feature off
            if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return true;

            UUID id = player.getUuid();
            if (STASH.containsKey(id)) return true;

            List<ItemStack> kept = takeSoulboundFromInventory(player);

            var handler = player.playerScreenHandler;
            ItemStack cursor = handler.getCursorStack();
            if (!cursor.isEmpty() && isSoulbound(cursor)) {
                addSafe(kept, cursor);
                handler.setCursorStack(ItemStack.EMPTY);
            }

            if (!kept.isEmpty()) {
                STASH.put(id, kept);
                handler.sendContentUpdates();
            }
            return true;
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            if (ConfigManager.disableSoulbound()) {
                STASH.remove(oldPlayer.getUuid());
                return; // feature off
            }

            if (newPlayer.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                STASH.remove(oldPlayer.getUuid());
                return;
            }

            List<ItemStack> stash = STASH.remove(oldPlayer.getUuid());
            if (stash == null || stash.isEmpty()) return;

            PlayerInventory inv = newPlayer.getInventory();
            for (ItemStack stack : stash) {
                if (stack == null || stack.isEmpty()) continue;
                if (!inv.insertStack(stack)) {
                    newPlayer.dropItem(stack, false, true);
                }
            }
            newPlayer.playerScreenHandler.sendContentUpdates();
        });
    }

    private static List<ItemStack> takeSoulboundFromInventory(ServerPlayerEntity player) {
        List<ItemStack> out = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        stripList(inv.main, out);
        stripList(inv.armor, out);
        stripList(inv.offHand, out);

        player.playerScreenHandler.sendContentUpdates();
        return out;
    }

    private static void stripList(DefaultedList<ItemStack> list, List<ItemStack> out) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            if (!s.isEmpty() && isSoulbound(s)) {
                addSafe(out, s);
                list.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static void addSafe(List<ItemStack> out, ItemStack stack) {
        int max = Math.max(1, stack.getMaxCount());
        int remaining = Math.max(1, stack.getCount());
        if (remaining <= max) {
            out.add(stack.copy());
            return;
        }
        while (remaining > 0) {
            int take = Math.min(max, remaining);
            ItemStack part = stack.copy();
            part.setCount(take);
            out.add(part);
            remaining -= take;
        }
    }

    public static boolean isSoulbound(ItemStack stack) {
        if (!stack.hasNbt()) return false;
        var nbt = stack.getNbt();
        if (nbt == null) return false;

        if (nbt.contains("permagears_soulbound") && nbt.getBoolean("permagears_soulbound")) return true;

        if (nbt.contains("permagears")) {
            var pg = nbt.getCompound("permagears");
            if (pg.contains("soulbound") && pg.getBoolean("soulbound")) return true;
        }
        return false;
    }
}
