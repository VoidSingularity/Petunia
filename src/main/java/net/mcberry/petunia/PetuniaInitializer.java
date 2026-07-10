package net.mcberry.petunia;

import net.mcberry.berry.loader.BerryLoader;
import net.mcberry.berry.loader.BerryModInitializer;
import net.mcberry.berry.loader.ExternalLibraryCollection;
import net.mcberry.berry.loader.JarContainer;
import net.mcberry.berry.loader.classloading.BerryClassTransformer;
import net.mcberry.berry.utils.Graph;
import net.mcberry.berry.utils.StringSorter;
import net.mcberry.petunia.fabric.BerryFabricLauncher;
import net.mcberry.petunia.patches.SimplePatchLoader;

public class PetuniaInitializer implements BerryModInitializer {
    static JarContainer thisjar;
    static ExternalLibraryCollection fabriclib;

    @Override
    public void preinit(StringSorter sorter, JarContainer jar, String name) {
        thisjar = jar;
        sorter.addValue(name);
        sorter.addRule("berrybuiltins", name);
        fabriclib = new ExternalLibrariesInfo();
        fabriclib.initialize();
    }

    public void initialize(String[] argv) {
        // Patches
        var graph = BerryClassTransformer.instance().all.graph;
        var vpatch = new Graph.Vertex("petunia::patches", new SimplePatchLoader(thisjar.file(), "net.mcberry.petunia.patches.app"));
        var vmixin = graph.getVertices().get("berry::mixin");
        graph.addVertex(vpatch);
        graph.addEdge(null, vpatch, vmixin, null);
        // Fabric
        fabriclib.imports();
        try {
            if (BerryLoader.isDevelopment()) System.setProperty("fabric.development", "true");
            new BerryFabricLauncher();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
