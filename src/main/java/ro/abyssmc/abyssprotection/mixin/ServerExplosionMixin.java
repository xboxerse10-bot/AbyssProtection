package ro.abyssmc.abyssprotection.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ro.abyssmc.abyssprotection.AbyssProtection;


import java.lang.reflect.Field;

/** Conservative region explosion guard. Avoids changing world data or entities when denied.
 * This mixin requires runtime testing on the target Minecraft version.
 */
@Mixin(targets = "net.minecraft.world.level.ServerExplosion")
public abstract class ServerExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true, require = 0)
    private void abyssprotection$protectExplosions(CallbackInfoReturnable<?> ci) {
        try {
            ServerLevel level = null;
            Vec3 center = null;
            String sourceName = "";
            for (Class<?> type = this.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value instanceof ServerLevel l) level = l;
                    if (value instanceof Vec3 v) center = v;
                    if (value instanceof net.minecraft.world.entity.Entity entity) {
                        sourceName = entity.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
                    }
                }
            }
            if (level == null || center == null) return;
            String flag = sourceName.contains("tnt") ? "tnt" : sourceName.contains("creeper") ? "creeper" : "other-explosion";
            if (!AbyssProtection.explosion(level, BlockPos.containing(center), flag)) ci.cancel();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Do not crash the server if a Minecraft update changes explosion internals.
        }
    }
}

