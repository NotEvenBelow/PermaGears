package permagears;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import permagears.config.ConfigManager;
import permagears.config.AdvancementConfigManager;
import permagears.item.ModItems;
import permagears.respawn.SoulboundKeeper;
import permagears.util.InventoryApplier;

import java.nio.file.Path;

public class PermaGears implements ModInitializer {
    public static final String MOD_ID = "permagears";
    public static final Logger LOGGER = LoggerFactory.getLogger("PermaGears");

    public static final Identifier FIRST_SIGIL_STAT = new Identifier(MOD_ID, "first_sigil");

    @Override
    public void onInitialize() {
        Path configDir = Path.of("config");
        ConfigManager.init(configDir);
        AdvancementConfigManager.init(configDir);

        ModItems.registerAll();

        Registry.register(Registries.CUSTOM_STAT, FIRST_SIGIL_STAT, FIRST_SIGIL_STAT);
        Stats.CUSTOM.getOrCreateStat(FIRST_SIGIL_STAT);

        SoulboundKeeper.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                InventoryApplier.reconcileAllOnline(server);
                LOGGER.info("PermaGears: reconciled all online players at server start.");
            } catch (Throwable t) {
                LOGGER.warn("PermaGears: reconcileAllOnline failed: {}", t.toString());
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;

            if (ConfigManager.giveSigilOnFirstJoin()) {
                int given = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(FIRST_SIGIL_STAT));
                if (given <= 0) {
                    ItemStack sigil = new ItemStack(ModItems.FRACTURED_SIGIL);
                    boolean inserted = player.getInventory().insertStack(sigil);
                    if (!inserted) {
                        player.dropItem(sigil, false);
                    }
                    player.increaseStat(Stats.CUSTOM.getOrCreateStat(FIRST_SIGIL_STAT), 1);
                }
            }
            try {
                InventoryApplier.reconcilePlayer(player);
            } catch (Throwable t) {
                LOGGER.warn("PermaGears: reconcilePlayer({}) failed: {}", player.getEntityName(), t.toString());
            }
        });

        LOGGER.info("PermaGears initialized.");
    }
}