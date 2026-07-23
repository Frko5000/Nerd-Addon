package com.nerds.addon.mixin;

import com.nerds.addon.modules.WindowTitleRenamer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class WindowTitleMixin {
    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void nerdAddon$renameTitle(CallbackInfoReturnable<String> info) {
        Modules modules = Modules.get();
        if (modules == null) return;

        WindowTitleRenamer module = modules.get(WindowTitleRenamer.class);
        if (module != null && module.isActive()) info.setReturnValue(module.getTitle());
    }
}
