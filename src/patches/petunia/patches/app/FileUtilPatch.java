package petunia.patches.app;

import berry.utils.ReflectionUtil;
import net.minecraft.FileUtil;
//import net.neoforged.neoforge.common.NeoForgeMod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import petunia.patches.IClassPatcher;
import petunia.patches.Patch;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

@Patch ("net/minecraft/FileUtil")
public class FileUtilPatch implements IClassPatcher {
    @Override
    public void patch (ClassNode cn) {
        // NeoForge: RESERVED_WINDOWS_FILENAMES_NEOFORGE =
        //      Pattern.compile(".*\\.|(?:CON|PRN|AUX|NUL|CLOCK\\$|CONIN\\$|CONOUT\\$|(?:COM|LPT)[¹²³0-9])(?:\\..*)?", 2)
        cn.fields.add (new FieldNode (
            Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            "RESERVED_WINDOWS_FILENAMES_NEOFORGE", "Ljava/util/regex/Pattern;", "Ljava/util/regex/Pattern;", null
        ));

        MethodNode clinit = null, ippp = null;
        for (MethodNode mn : cn.methods)
            if (mn.name.equals ("<clinit>"))
                clinit = mn;
            else if (mn.name.equals ("isPathPartPortable"))
                ippp = mn;
        if (clinit == null) throw new RuntimeException ("<clinit> not found in net.minecraft.FileUtil. Maybe outdated patch?");
        if (ippp == null) throw new RuntimeException ("isPathPartPortable not found in net.minecraft.FileUtil. Maybe outdated patch?");
        var iter = clinit.instructions.iterator ();
        // Initialize RESERVED_WINDOWS_FILENAMES_NEOFORGE
        iter.add (new LdcInsnNode (".*\\.|(?:CON|PRN|AUX|NUL|CLOCK\\$|CONIN\\$|CONOUT\\$|(?:COM|LPT)[¹²³0-9])(?:\\..*)?"));
        iter.add (new LdcInsnNode (2));
        iter.add (new MethodInsnNode (Opcodes.INVOKESTATIC, "java/util/regex/Pattern", "compile", "(Ljava/lang/String;I)Ljava/util/regex/Pattern;"));
        iter.add (new FieldInsnNode (Opcodes.PUTSTATIC, "net/minecraft/FileUtil", "RESERVED_WINDOWS_FILENAMES_NEOFORGE", "Ljava/util/regex/Pattern;"));
        // redirect isPathPartPortable to here
        ippp.instructions.clear ();
        iter = ippp.instructions.iterator ();
        iter.add (new VarInsnNode (Opcodes.ALOAD, 0));
        iter.add (new MethodInsnNode (Opcodes.INVOKESTATIC, "petunia/patches/app/FileUtilPatch", "portable", "(Ljava/lang/String;)Z"));
        iter.add (new InsnNode (Opcodes.IRETURN));
    }
    private static Field orig = null, neof = null;
    private static void init () {
        if (orig == null) {
            orig = ReflectionUtil.getField (FileUtil.class, "RESERVED_WINDOWS_FILENAMES");
            neof = ReflectionUtil.getField (FileUtil.class, "RESERVED_WINDOWS_FILENAMES_NEOFORGE");
            orig.setAccessible (true);
            neof.setAccessible (true);
        }
    }
    private static Pattern choose () {
        init ();
        try {
            // TODO: Uncomment this when NeoForge initialization part has been done
//            if (NeoForgeMod.getProperFilenameValidation ()) return (Pattern) neof.get (null);
//            else
                return (Pattern) orig.get (null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException (e);
        }
    }
    public static boolean portable (String path) {
        return ! choose () .matcher (path) .matches ();
    }
}
