package berry.unify.mixins.injs;

import forge.mods.modlauncher.ArgumentHandler;
import forge.mods.modlauncher.LaunchPluginHandler;
import forge.mods.modlauncher.LaunchServiceHandler;
import forge.mods.modlauncher.Launcher;
import forge.mods.modlauncher.TransformingClassLoader;
import petunia.internal.mixins.asm.mixin.Mixin;
import petunia.internal.mixins.asm.mixin.injection.At;
import petunia.internal.mixins.asm.mixin.injection.Redirect;

@Mixin (Launcher.class)
public class ForgeLauncherMixin {
    @Redirect (method = "run", at = @At (value = "INVOKE", target = "Lforge/mods/modlauncher/LaunchServiceHandler;validateLaunchTarget(Lforge/mods/modlauncher/ArgumentHandler;)V"), remap = false)
    private void nullize_vlt (LaunchServiceHandler h1, ArgumentHandler h2) {}
    @Redirect (method = "run", at = @At (value = "INVOKE", target = "Ljava/lang/Thread;setContextClassLoader(Ljava/lang/ClassLoader;)V"), remap = false)
    private void nullize_ctxcl (Thread cur, ClassLoader cl) {}
    @Redirect (method = "run", at = @At (value = "INVOKE", target = 
        "Lforge/mods/modlauncher/LaunchServiceHandler;launch(Lforge/mods/modlauncher/ArgumentHandler;" +
        "Ljava/lang/ModuleLayer;Lforge/mods/modlauncher/TransformingClassLoader;Lforge/mods/modlauncher/LaunchPluginHandler;)V"
    ), remap = false)
    private void nullize_launch (LaunchServiceHandler lsh, ArgumentHandler ah, ModuleLayer ml, TransformingClassLoader tcl, LaunchPluginHandler lph) {}
}
