package petunia.patches;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.FileOutputStream;
import java.io.IOException;

public interface IClassPatcher {
    /**
     * Patch byte code of a class.
     * @param buffer The byte code buffer
     * @return Patched buffer, or null if unpatched
     */
    default byte[] patch (byte[] buffer) {
        ClassReader cr = new ClassReader (buffer);
        ClassNode cn = new ClassNode ();
        cr.accept (cn, 0);
        patch(cn);
        ClassWriter cw = new ClassWriter (0);
        cn.accept (cw);
        return cw.toByteArray ();
    }
    /**
     * Patch a ClassNode
     * @param cn ClassNode of the class to be patched
     */
    default void patch(ClassNode cn) {}
    /**
     * Internal name of the class to be patched
     * @return The internal name
     */
    default String getTarget () {
        // The class MUST be annotated with @Patch to use this default method
        var annotation = this.getClass () .getAnnotation (Patch.class);
        if (annotation != null) return annotation.value ();
        throw new UnsupportedOperationException ("Patch must be annotated with @Patch or override getTarget()");
    }
    /**
     * Debug method
     */
    default void debug (byte[] buffer) {
        String fn = "../../patched_classes/" + this.getTarget () .replace ('/', '.') + ".class";
        try {
            var os = new FileOutputStream (fn);
            os.write (buffer);
            os.close ();
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }
    /**
     * Debug method
     */
    default void debug (ClassWriter cw) {
        debug (cw.toByteArray ());
    }
    /**
     * Debug method
     */
    default void debug (ClassNode cn) {
        ClassWriter cw = new ClassWriter (0);
        cn.accept (cw);
        debug (cw.toByteArray ());
    }
}
