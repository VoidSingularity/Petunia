package petunia.patches;

import berry.api.asm.ClassFile;
import berry.loader.BerryClassTransformer;

import java.io.IOException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class SimplePatchLoader implements BerryClassTransformer.ByteCodeTransformer {
    private final Map <String, IClassPatcher> map = new HashMap <> ();
    @Override
    public byte[] transform (ClassLoader loader, String name, Class <?> clazz, ProtectionDomain domain, byte[] buf) {
        // There are two types of patches:
        // 1. Modify one class per patch, like what (Neo)?Forge etc. do
        // These patches are easier, but may cause problems with some patches
        // 2. Modify mod classes, so they work fine
        // SimplePatchLoader loads type 1 patches from given package of given jar
        // A patch must implement IClassPatcher. By default, the target is loaded from
        // @Patch annotation, however a patch can override this method.
        // Multiple patches cannot share the same target in the same SimplePatchLoader.
        if (map.containsKey (name)) {
            // A patch is present for the current class
            IClassPatcher patcher = map.get (name);
            byte[] res = patcher.patch (buf);
            if (res == null) return buf;
            else return res;
        }
        return buf;
    }
    public SimplePatchLoader (JarFile jar, String pkg) {
        String prefix = pkg.replace ('.', '/') + "/";
        jar.entries () .asIterator () .forEachRemaining (entry -> {
            if (entry.isDirectory ()) return;
            var name = entry.getName ();
            if (name.startsWith (prefix) && name.endsWith (".class")) {
                var clsname = name.split ("\\.") [0] .replace ('/', '.');
                // This might be a patch!
                try {
                    var is = jar.getInputStream (entry);
                    ClassFile cf = new ClassFile (is.readAllBytes ());
                    is.close ();
                    // Check whether this class implements IClassPatcher
                    boolean flag = false;
                    for (var ref : cf.interfaces) {
                        if (cf.cls_name (ref) .equals ("petunia/patches/IClassPatcher")) {
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        // Instantiate this class and get the target
                        Class <? extends IClassPatcher> cls = (Class <? extends IClassPatcher>) Class.forName (clsname);
                        IClassPatcher instance = cls.getConstructor () .newInstance ();
                        map.put (instance.getTarget (), instance);
                    }
                } catch (IOException e) {
                    System.err.println ("IOE while loading a potential patch: " + name);
                    e.printStackTrace ();
                } catch (ReflectiveOperationException | ClassCastException e) {
                    System.err.println ("Failed to instantiate patch class " + clsname);
                    e.printStackTrace ();
                }
            }
        });
    }
}
