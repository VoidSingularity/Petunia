// Copyright (C) 2025 VoidSingularity

// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or (at
// your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package berry.unify.mixins.impl;

import petunia.internal.mixins.asm.launch.MixinBootstrap;
import petunia.internal.mixins.asm.mixin.MixinEnvironment;
import petunia.internal.mixins.asm.mixin.Mixins;
import berry.loader.BerryClassTransformer;
import berry.utils.Graph;

public class MixinInitialize {
    public static void initialize () {
        // How
        try {
            MixinInitialize.class.getClassLoader () .loadClass (Mixins.class.getName ());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException (e);
        }
        MixinBootstrap.init ();
        MixinEnvironment.gotoPhase (MixinEnvironment.Phase.INIT);
        MixinEnvironment.gotoPhase (MixinEnvironment.Phase.DEFAULT);
        BerryClassTransformer.ByteCodeTransformer transformer = (loader, name, clazz, domain, code) -> {
            try {
                name = name.replace ('/', '.');
                return BerryMixinService.transformer.transformClassBytes (name, name, code);
            } catch (Throwable t) {
                System.err.printf("[PETUNIA/MIXIN] Error transforming class %s%n", name);
                t.printStackTrace ();
                return null;
            }
        };
        var graph = BerryClassTransformer.instance () .all.graph;
        var vmixin = new Graph.Vertex ("petunia::mixin", transformer);
        var vremap = graph.getVertices () .get ("berry::remap");
        var vpatch = graph.getVertices () .get ("petunia::patch");
        var vberry = graph.getVertices () .get ("berry::mixin");
        graph.addVertex (vmixin);
        if (vpatch != null) graph.addEdge (null, vpatch, vmixin, null);
        else graph.addEdge (null, vremap, vmixin, null);
        graph.addEdge (null, vmixin, vberry, null);
        Mixins.addConfiguration ("petunia_mixins.json");
    }
}
