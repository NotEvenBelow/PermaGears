package permagears.mixin;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import permagears.config.ConfigManager;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract NbtCompound getOrCreateNbt();
    @Shadow public abstract NbtCompound getNbt();
    @Shadow public abstract net.minecraft.item.Item getItem();

    @Inject(method = "<init>(Lnet/minecraft/item/ItemConvertible;I)V", at = @At("RETURN"))
    private void permagears$afterCtorWithCount(ItemConvertible item, int count, CallbackInfo ci) {
        permagears$markIfWhitelisted();
    }

    @Inject(method = "<init>(Lnet/minecraft/item/ItemConvertible;)V", at = @At("RETURN"))
    private void permagears$afterCtor(ItemConvertible item, CallbackInfo ci) {
        permagears$markIfWhitelisted();
    }

    private void permagears$markIfWhitelisted() {
        ConfigManager.ensureLoadedDefault();

        ItemStack self = (ItemStack) (Object) this;
        Identifier id = Registries.ITEM.getId(self.getItem());
        if (id == null) return;

        if (!ConfigManager.isWhitelisted(id.toString())) return;

        NbtCompound nbt = getNbt();
        boolean managed     = nbt != null && nbt.getBoolean("PermaGearsManaged");
        boolean unbreakable = nbt != null && nbt.getBoolean("Unbreakable");
        boolean flatSoul    = nbt != null && nbt.getBoolean("permagears_soulbound");
        boolean nestedSoul  = false;
        if (nbt != null && nbt.contains("permagears")) {
            NbtCompound pg = nbt.getCompound("permagears");
            nestedSoul = pg.contains("soulbound") && pg.getBoolean("soulbound");
        }

        if (managed && unbreakable) {
            if (ConfigManager.disableSoulbound()) {
                if (flatSoul || nestedSoul) {
                    NbtCompound edit = getOrCreateNbt();
                    edit.remove("permagears_soulbound");
                    if (edit.contains("permagears")) {
                        NbtCompound pg = edit.getCompound("permagears");
                        pg.remove("soulbound");
                        if (pg.getKeys().isEmpty()) edit.remove("permagears"); else edit.put("permagears", pg);
                    }
                }
            } else if (flatSoul && nestedSoul) {
                return; 
            }
        }

        NbtCompound edit = getOrCreateNbt();

        edit.putBoolean("Unbreakable", true);
        edit.putBoolean("PermaGearsManaged", true);

        if (!ConfigManager.disableSoulbound()) {
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
    }
}
