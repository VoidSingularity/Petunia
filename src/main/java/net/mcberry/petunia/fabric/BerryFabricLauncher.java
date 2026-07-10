package net.mcberry.petunia.fabric;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;

import net.mcberry.berry.utils.ReflectionUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;

import com.google.gson.JsonParser;

import net.mcberry.berry.loader.classloading.BerryClassLoader;
import net.mcberry.berry.api.mixins.BerryMixinService;
import net.mcberry.berry.loader.classloading.BerryClassTransformer;
import net.mcberry.berry.loader.BerryLoader;
import net.mcberry.berry.utils.Graph;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.game.minecraft.McVersion;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.game.minecraft.patch.EntrypointPatch;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import net.fabricmc.loader.impl.util.Arguments;

public class BerryFabricLauncher extends FabricLauncherBase {
    private final EnvType envtype;
    private final MinecraftGameProvider mgp;

    private static Optional<Path> getJarPath(String resource) {
        try {
            URL url = BerryFabricLauncher.class.getClassLoader().getResource(resource);
            if (url == null) {
                return Optional.empty();
            }
            if ("jar".equals(url.getProtocol())) {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                URL jar =  conn.getJarFileURL();
                return Optional.of(Paths.get(jar.toURI()));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public BerryFabricLauncher() throws ReflectiveOperationException {
        super();

        EnvType env;
        if (BerryLoader.getSide().equals("CLIENT")) env = EnvType.CLIENT;
        else env = EnvType.SERVER;
        envtype = env;

        MinecraftGameProvider provider = new MinecraftGameProvider();
        this.mgp = provider;

        Path mcjar = getJarPath(BerryLoader.getEntrypoint().replace(".", "/") + ".class").orElseThrow();
        ((List<Path>) ReflectionUtil.getFieldFrom(MinecraftGameProvider.class, mgp, "gameJars"))
                .add(mcjar);

        var graph = BerryClassTransformer.instance().all.graph;
        var remap = BerryClassTransformer.instance().remapper.graph;
        BerryClassTransformer.ByteCodeTransformer trans = (loader, name, clazz, domain, code) -> { loaded.add(name.replace(".", "/")); return code; };
        remap.addVertex(new Graph.Vertex("berry::fabric", trans));
        var vremap = graph.getVertices().get("berry::remap");
        BerryClassTransformer.ByteCodeTransformer pther = (loader, name, clazz, domain, code) -> patch(name, code);
        var vpatch = new Graph.Vertex("berry::fabricpatch", pther);
        graph.addVertex(vpatch);
        graph.addEdge(null, vpatch, vremap, null);
        trans = (loader, name, clazz, domain, code) -> {
            if (code == null) return code;
            if (name.startsWith("net/fabricmc/loader/")) return code;
            if (name.startsWith("net/fabricmc/api/")) return code;
            return FabricTransformer.transform(BerryLoader.isDevelopment(), envtype, name.replace('/', '.'), code);
        };
        var vtrans = new Graph.Vertex("berry::fabrictrans", trans);
        graph.addVertex(vtrans);
        graph.addEdge(null, vtrans, vpatch, null);

        var loader = FabricLoaderImpl.INSTANCE;
        loader.setGameProvider(provider);
        ReflectionUtil.invokeMethod(
                ReflectionUtil.getMethod(MinecraftGameProvider.class, "setupLogHandler"),
                provider, this, false
        );

        McVersion.Builder builder = new McVersion.Builder();
        String vers;
        var is = this.getClass().getClassLoader().getResourceAsStream("version.json");
        InputStreamReader reader = new InputStreamReader(is);
        var obj = JsonParser.parseReader(reader).getAsJsonObject();
        vers = obj.get("id").getAsString();
        builder.setNameAndRelease(vers);
        ReflectionUtil.setFieldTo(MinecraftGameProvider.class, provider, "versionData", builder.build());

        Arguments args = new Arguments();
        args.parse(BerryLoader.getArgs());
        ReflectionUtil.setFieldTo(MinecraftGameProvider.class, provider, "arguments", args);

        ReflectionUtil.invokeMethod(
                ReflectionUtil.getMethod(FabricLoaderImpl.class, "setGameDir"),
                loader, Path.of(BerryLoader.getGameDirectory())
        );

        loader.load();
        loader.freeze();
        loader.loadClassTweakers();
        BerryLoader.preloaders.add(cl -> patch());
        BerryLoader.preloaders.add(cl -> {
            Map<String, ModContainerImpl> configToModMap = new HashMap<>();
            for (ModContainerImpl mod : loader.getModsInternal()) {
                for (String config : mod.getMetadata().getMixinConfigs(env)) {
                    ModContainerImpl prev = configToModMap.putIfAbsent(config, mod);
                    if (prev != null) throw new RuntimeException(
                            String.format("Non-unique Mixin config name %s used by the mods %s and %s", config, prev.getMetadata().getId(), mod.getMetadata().getId()));
                    try {
                        Mixins.addConfiguration(config);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format("Error parsing or using Mixin config %s for mod %s", config, mod.getMetadata().getId()), t);
                    }
                }
            }
        });
        BerryLoader.preloaders.add(cl -> prelaunch());
    }

    private void prelaunch() {
        FabricLoaderImpl.INSTANCE.invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
    }

    private static final Set<String> loaded = new HashSet<>();

    // Inject entrypoints
    private final Map<String, ClassNode> patched = new HashMap<>();

    private void patch() {
        Function<String, ClassNode> source = (str) -> {
            str = str.replace('.', '/');
            try {
                return BerryMixinService.getClassNode(str, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        Consumer<ClassNode> emitter = (node) -> patched.put(node.name.replace('.', '/'), node);
        var patcher = new EntrypointPatch(mgp);
        patcher.process(this, source, emitter);
    }

    public byte[] patch(String name, byte[] code) {
        name = name.replace('.', '/'); // Probably useless?
        if (patched.containsKey(name)) {
            ClassWriter writer = new ClassWriter(0);
            ClassNode node = patched.get(name);
            node.accept(writer);
            return writer.toByteArray();
        }
        return code;
    }

    @Override
    public boolean isClassLoaded(String name) {
        return loaded.contains(name.replace(".", "/"));
    }

    @Override
    public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
        throw new NotImplementedException("Method is not supported");
    }

    @Override
    public EnvType getEnvironmentType() {
        return this.envtype;
    }

    @Override
    public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    @Override
    public ClassLoader getTargetClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public String getEntrypoint() {
        return BerryLoader.getEntrypoint();
    }

    @Override
    public List<Path> getClassPath() {
        throw new NotImplementedException("Method is not supported");
    }

    @Override
    public void addToClassPath(Path path, String... allowedPrefixes) {
        BerryClassLoader.getInstance().appendToClassPathForInstrumentation(path.toString());
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return this.getClass().getClassLoader().getResourceAsStream(name);
    }

    @Override
    public void setValidParentClassPath(Collection<Path> paths) {
        // throw new NotImplementedException ("Method is not supported");
    }

    @Override
    public void setAllowedPrefixes(Path path, String... prefixes) {
        // throw new NotImplementedException ("Method is not supported");
    }

    @Override
    public Manifest getManifest(Path originPath) {
        throw new NotImplementedException("Method is not supported");
    }
}
