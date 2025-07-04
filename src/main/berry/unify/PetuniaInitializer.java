package berry.unify;

import java.io.IOException;

import berry.api.BundledJar;
import berry.api.asm.AccessTransformer;
import berry.api.asm.ShadowClass;
import berry.loader.BerryClassLoader;
import berry.loader.BerryModInitializer;
import berry.loader.ExternalLibraryCollection;
import berry.loader.JarContainer;
import berry.loader.JarProcessor;
import berry.unify.fabric.BerryFabricLauncher;
import berry.unify.fabric.FabricLibrariesGenerated;
import berry.unify.forge.BerryForgeLauncher;
import berry.unify.forge.ForgeLibrariesGenerated;
import berry.unify.mixins.impl.MixinInitialize;
import berry.utils.StringSorter;

public class PetuniaInitializer implements BerryModInitializer {
    static JarContainer thisjar;
    static ExternalLibraryCollection elfabric, elforge;
    @Override
    public void preinit (StringSorter sorter, JarContainer jar, String name) {
        thisjar = jar;
        sorter.addValue (name);
        sorter.addRule ("berrybuiltins", name);
        elfabric = new FabricLibrariesGenerated (); elfabric.initialize ();
        elforge = new ForgeLibrariesGenerated (); elforge.initialize ();
    }
    private static String shadow (String hash, ShadowConfigGenerated.Info info) {
        String prefix = ExternalLibraryCollection.getPrefix ();
        String oloc = prefix + hash + ".jar",
               nloc = prefix + "shadow-" + hash + ".jar";
        try {
            var processor = new ShadowClass (info.from (), info.to ()) .concat (new AccessTransformer ());
            JarProcessor.process (processor, oloc, nloc);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
        return nloc;
    }
    private static void shadow (ExternalLibraryCollection collection, ShadowConfigGenerated.Info info) {
        for (String hash : collection.keySet ()) {
            BerryClassLoader.getInstance () .appendToClassPathForInstrumentation (shadow (hash, info));
        }
    }
    @SuppressWarnings ("deprecation")
    public void initialize (String[] argv) {
        BundledJar.addBundled (thisjar);
        MixinInitialize.initialize ();
        // AT is applied to the bytecode and not to other things, so this is fine
        elfabric.imports ();
        // However, shadows applies to class names, etc. so we have to do this
        shadow (elforge, ShadowConfigGenerated.forge);
        // Fabric
        new BerryFabricLauncher ();
        // Forge
        new BerryForgeLauncher ();
    }
}
