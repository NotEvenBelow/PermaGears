package permagears.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import permagears.config.ConfigManager;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(
        method = "dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void permagears$blockDrop2(ItemStack stack, boolean throwRandomly,
                                       CallbackInfoReturnable<ItemEntity> cir) {
        permagears$handleDrop(stack, cir);
    }

    @Inject(
        method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void permagears$blockDrop3(ItemStack stack, boolean throwRandomly, boolean retainOwnership,
                                       CallbackInfoReturnable<ItemEntity> cir) {
        permagears$handleDrop(stack, cir);
    }

    private void permagears$handleDrop(ItemStack stack, CallbackInfoReturnable<ItemEntity> cir) {
        if (ConfigManager.disableDropLock()) return; 
        if (stack == null || stack.isEmpty()) return;
        PlayerEntity self = (PlayerEntity)(Object)this;

        if (!permagears$isProtected(stack) && !permagears$containerHasProtected(stack)) return;

        if (self instanceof ServerPlayerEntity spe) {
            PlayerInventory inv = spe.getInventory();
            ItemStack working = stack; 

            boolean ok = inv.insertStack(working);

            if (!ok && !working.isEmpty()) {
                var handler = spe.playerScreenHandler;
                ItemStack cursor = handler.getCursorStack();
                if (cursor.isEmpty()) {
                    handler.setCursorStack(working);
                    working = ItemStack.EMPTY;
                } else if (ItemStack.canCombine(cursor, working)) {
                    int space = cursor.getMaxCount() - cursor.getCount();
                    if (space > 0) {
                        int move = Math.min(space, working.getCount());
                        cursor.increment(move);
                        working.decrement(move);
                        handler.setCursorStack(cursor);
                    }
                }

                if (!working.isEmpty()) {
                    if (spe.getOffHandStack().isEmpty()) {
                        spe.setStackInHand(Hand.OFF_HAND, working);
                        working = ItemStack.EMPTY;
                    } else {
                        int empty = inv.getEmptySlot();
                        if (empty >= 0) {
                            inv.setStack(empty, working);
                            working = ItemStack.EMPTY;
                        }
                    }

                    if (!working.isEmpty() && spe.getMainHandStack().isEmpty()) {
                        spe.setStackInHand(Hand.MAIN_HAND, working);
                        working = ItemStack.EMPTY;
                    }
                }

                handler.sendContentUpdates();
            }
        }

        cir.setReturnValue(null);
    }

    private static boolean permagears$isProtected(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return false;
        if (nbt.getBoolean("PermaGearsManaged")) return true;
        if (nbt.getBoolean("permagears_soulbound")) return true;
        if (nbt.contains("permagears", NbtElement.COMPOUND_TYPE)
            && nbt.getCompound("permagears").getBoolean("soulbound")) return true;
        if (nbt.getBoolean("Unbreakable")) return true;
        return false;
    }

    private static boolean permagears$containerHasProtected(ItemStack container) {
        NbtCompound root = container.getNbt();
        if (root == null) return false;

        if (root.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
            NbtCompound bet = root.getCompound("BlockEntityTag");
            if (permagears$anyProtectedInCommonLists(bet)) return true;
        }

        if (permagears$anyProtectedInCommonLists(root)) return true;

        return false;
    }

    private static boolean permagears$anyProtectedInCommonLists(NbtCompound where) {
        String[] keys = new String[]{"Items", "items", "Inventory", "inventory", "Contents", "contents"};
        for (String k : keys) {
            if (where.contains(k, NbtElement.LIST_TYPE)) {
                NbtList list = where.getList(k, NbtElement.COMPOUND_TYPE);
                if (permagears$listHasProtectedItem(list)) return true;
            }
        }
        return false;
    }

    private static boolean permagears$listHasProtectedItem(NbtList list) {
        for (int i = 0; i < list.size(); i++) {
            NbtCompound item = list.getCompound(i);
            if (item.contains("tag", NbtElement.COMPOUND_TYPE)) {
                NbtCompound tag = item.getCompound("tag");
                if (permagears$nbtHasProtectedFlags(tag)) return true;
            }
        }
        return false;
    }

    private static boolean permagears$nbtHasProtectedFlags(NbtCompound tag) {
        if (tag.getBoolean("PermaGearsManaged")) return true;
        if (tag.getBoolean("permagears_soulbound")) return true;
        if (tag.contains("permagears", NbtElement.COMPOUND_TYPE)
            && tag.getCompound("permagears").getBoolean("soulbound")) return true;
        if (tag.getBoolean("Unbreakable")) return true;
        return false;
    }
}
