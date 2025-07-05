package berry.unify.forge;

import berry.loader.BerryClassLoader;
import berry.loader.BerryLoader;
import forge.mods.modlauncher.Launcher;

public class BerryForgeLauncher {
    public BerryForgeLauncher () {
        BerryClassLoader.getInstance () .controlResources ("log4j2.component.properties", BerryClassLoader.empty ());
        Launcher.main (BerryLoader.getArgs ()); // Useless, currently
    }
}
