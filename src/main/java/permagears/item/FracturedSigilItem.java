package permagears.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class FracturedSigilItem extends Item {
    public FracturedSigilItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC; 
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("Made by ChatGPT (no joke)").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }
}
