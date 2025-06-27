package berry.unify;

import berry.loader.BerryModInitializer;
import berry.loader.JarContainer;
import berry.unify.fabric.BerryFabricLauncher;
import berry.utils.StringSorter;

public class PetuniaInitializer implements BerryModInitializer {
    static JarContainer thisjar;
    @Override
    public void preinit (StringSorter sorter, JarContainer jar, String name) {
        thisjar = jar;
        sorter.addValue (name);
        sorter.addRule ("berrybuiltins", name);
        try { ExternalLibrariesGenerated.init (); }
        catch (Exception e) { throw new RuntimeException (e); }
    }
    public void initialize (String[] argv) {
        // Fabric
        new BerryFabricLauncher ();
    }
}
