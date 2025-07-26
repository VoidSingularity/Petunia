package petunia.patches;

import berry.loader.*;
import berry.utils.Graph;
import berry.utils.StringSorter;

public class PetuniaPatchMod implements BerryModInitializer {
    static JarContainer thisjar;
    @Override
    public void preinit (StringSorter sorter, JarContainer jar, String name) {
        thisjar = jar;
        sorter.addValue (name);
        sorter.addRule (name, "petunia");
    }
    @Override
    public void initialize (String[] argv) {
        var graph = BerryClassTransformer.instance () .all.graph;
        var vpatch = new Graph.Vertex ("petunia::patch", new SimplePatchLoader (thisjar.file (), "petunia.patches.app"));
        var vremap = graph.getVertices () .get ("berry::remap");
        var vmixin = graph.getVertices () .get ("berry::mixin");
        graph.addVertex (vpatch);
        graph.addEdge (null, vremap, vpatch, null);
        graph.addEdge (null, vpatch, vmixin, null);
    }
}
