package petunia.patches.app;

import io.netty.util.ResourceLeakDetector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import petunia.patches.IClassPatcher;
import petunia.patches.Patch;

import java.lang.management.ManagementFactory;

@Patch ("net/minecraft/SharedConstants")
public class SharedConstantsPatch implements IClassPatcher {
    @Override
    public void patch (ClassNode cn) {
        // NeoForge: public static final boolean IS_RUNNING_WITH_JDWP
        cn.fields.add (new FieldNode (
            Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            "IS_RUNNING_WITH_JDWP", "Z", "Z", null
        ));

        // <clinit> method
        MethodNode clinit = null;
        for (MethodNode mn : cn.methods)
            if (mn.name.equals ("<clinit>"))
                clinit = mn;
        if (clinit == null) throw new RuntimeException ("<clinit> not found in net.minecraft.SharedConstants. Maybe outdated patch?");
        var iter = clinit.instructions.iterator ();

        // NeoForge: IS_RUNNING_WITH_JDWP = ManagementFactory.getRuntimeMXBean().getInputArguments()
        //      .stream().anyMatch(str->str.startsWith("-agentlib:jdwp"))
        iter.add (new MethodInsnNode (
            Opcodes.INVOKESTATIC,
            "petunia/patches/app/SharedConstantsPatch",
            "isRunningWithJDWP",
            "()Z"
        ));
        iter.add (new FieldInsnNode (Opcodes.PUTSTATIC, "net/minecraft/SharedConstants", "IS_RUNNING_WITH_JDWP", "Z"));
        // IS_RUNNING_IN_IDE = BerryLoader.isDevelopment()
        iter.add (new MethodInsnNode (
            Opcodes.INVOKESTATIC,
            "berry/loader/BerryLoader",
            "isDevelopment",
            "()Z"
        ));
        iter.add (new FieldInsnNode (Opcodes.PUTSTATIC, "net/minecraft/SharedConstants", "IS_RUNNING_IN_IDE", "Z"));

        while (iter.hasNext ()) {
            var insn = iter.next ();
            if (insn instanceof MethodInsnNode meth && meth.owner.equals ("io/netty/util/ResourceLeakDetector") && meth.name.equals ("setLevel")) {
                // Change the method owner
                meth.owner = "petunia/patches/app/SharedConstantsPatch";
            }
        }
    }
    public static boolean isRunningWithJDWP () {
        return ManagementFactory.getRuntimeMXBean () .getInputArguments ()
                .stream () .anyMatch (str -> str.startsWith ("-agentlib:jdwp"));
    }
    public static void setLevel (ResourceLeakDetector.Level level) {
        if (System.getProperty ("io.netty.leakDetection.level") == null)
            ResourceLeakDetector.setLevel (level);
    }
}
