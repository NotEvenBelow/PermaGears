package permagears.mixin;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import permagears.config.AdvancementConfigManager;
import permagears.item.ModItems;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementsMixin {

    @Shadow @Final private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void permagears$onGrant(Advancement adv, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (!((PlayerAdvancementTracker)(Object)this).getProgress(adv).isDone()) return;

        if (!AdvancementConfigManager.isEnabled()) return;

        String id = adv.getId().toString();
        if (AdvancementConfigManager.isSigilAdvancement(id)) {
            ItemStack sigil = new ItemStack(ModItems.FRACTURED_SIGIL);
            if (!owner.getInventory().insertStack(sigil)) {
                owner.dropItem(sigil, false);
            }
        }
    }
}
