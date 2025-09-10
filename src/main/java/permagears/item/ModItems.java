package permagears.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import permagears.PermaGears;

public class ModItems {
    public static final Item FRACTURED_SIGIL =
            new FracturedSigilItem(new Item.Settings().maxCount(64));

    public static void registerAll() {
        Registry.register(
                Registries.ITEM,
                new Identifier(PermaGears.MOD_ID, "fractured_sigil"),
                FRACTURED_SIGIL
        );

        // Add the item into the Ingredients creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> entries.add(FRACTURED_SIGIL));
    }
}
