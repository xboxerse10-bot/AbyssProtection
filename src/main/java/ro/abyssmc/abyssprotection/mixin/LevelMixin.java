package ro.abyssmc.abyssprotection.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ro.abyssmc.abyssprotection.AbyssProtection;
@Mixin(Level.class) public class LevelMixin {
@Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true) private void ap$change(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> ci){if((Object)this instanceof ServerLevel l&&!AbyssProtection.worldChange(l,pos,state))ci.setReturnValue(false);}
}
