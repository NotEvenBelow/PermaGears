package permagears.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(
        method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void permagears$guardDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;

        ItemStack stack = self.getStack();
        if (stack.isEmpty()) return;

        var nbt = stack.getNbt();
        boolean managed = nbt != null && nbt.getBoolean("PermaGearsManaged") && nbt.getBoolean("Unbreakable");
        if (!managed) return;

        cir.setReturnValue(false);
        cir.cancel();
    }
}