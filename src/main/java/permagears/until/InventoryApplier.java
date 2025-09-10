package permagears.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import permagears.config.ConfigManager;

public final class InventoryApplier {
    private InventoryApplier() {}

    public static void reconcileAllOnline(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            reconcilePlayer(p);
        }
    }

    public static void reconcilePlayer(ServerPlayerEntity player) {
        var inv = player.getInventory();
        int size = inv.size();

        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inv.getStack(slot);
            if (stack.isEmpty()) continue;

            Identifier id = Registries.ITEM.getId(stack.getItem());
            boolean whitelisted = ConfigManager.isWhitelisted(id.toString());
            boolean soulOff = ConfigManager.disableSoulbound();

            NbtCompound nbt = stack.getNbt();
            boolean hasNbt = nbt != null;

            boolean managed = hasNbt && nbt.getBoolean("PermaGearsManaged");
            boolean unbreakable = hasNbt && nbt.getBoolean("Unbreakable");
            boolean flatSoul = hasNbt && nbt.getBoolean("permagears_soulbound");
            boolean nestedSoul = false;
            if (hasNbt && nbt.contains("permagears")) {
                NbtCompound pg = nbt.getCompound("permagears");
                nestedSoul = pg.contains("soulbound") && pg.getBoolean("soulbound");
            }

            if (whitelisted) {
                NbtCompound edit = stack.getOrCreateNbt();
                if (!unbreakable) edit.putBoolean("Unbreakable", true);
                if (!managed) edit.putBoolean("PermaGearsManaged", true);

                if (!soulOff) {
                    if (!flatSoul) edit.putBoolean("permagears_soulbound", true);
                    NbtCompound pg = edit.contains("permagears") ? edit.getCompound("permagears") : new NbtCompound();
                    if (!pg.getBoolean("soulbound")) pg.putBoolean("soulbound", true);
                    edit.put("permagears", pg);
                } else {
                    edit.remove("permagears_soulbound");
                    if (edit.contains("permagears")) {
                        NbtCompound pg = edit.getCompound("permagears");
                        pg.remove("soulbound");
                        if (pg.getKeys().isEmpty()) edit.remove("permagears"); else edit.put("permagears", pg);
                    }
                }
            } else {
                if (managed || unbreakable || flatSoul || nestedSoul) {
                    NbtCompound edit = stack.getOrCreateNbt();

                    edit.remove("permagears_soulbound");
                    if (edit.contains("permagears")) {
                        NbtCompound pg = edit.getCompound("permagears");
                        pg.remove("soulbound");
                        if (pg.getKeys().isEmpty()) edit.remove("permagears");
                        else edit.put("permagears", pg);
                    }

                    if (edit.getBoolean("Unbreakable")) edit.remove("Unbreakable");
                    edit.putBoolean("PermaGearsManaged", false);
                }
            }
        }

        player.playerScreenHandler.sendContentUpdates();
    }
}
