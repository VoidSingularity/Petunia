package net.mcberry.petunia.patches.app;

import io.netty.util.ResourceLeakDetector;
import net.mcberry.berry.loader.BerryLoader;
import net.mcberry.berry.utils.ReflectionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import net.mcberry.petunia.patches.IClassPatcher;
import net.mcberry.petunia.patches.Patch;

import java.lang.management.ManagementFactory;

@Patch(SharedConstantsPatch.target)
public class SharedConstantsPatch implements IClassPatcher {
    static final String target = "net/minecraft/SharedConstants";

    @Override
    public void patch(ClassNode cn) {
        final String myname = ReflectionUtil.getInternalName(SharedConstantsPatch.class);

        // NeoForge: public static final boolean IS_RUNNING_WITH_JDWP
        cn.fields.add(new FieldNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "IS_RUNNING_WITH_JDWP", "Z", "Z", null
        ));

        // <clinit> method
        MethodNode clinit = null;
        for (MethodNode mn : cn.methods)
            if (mn.name.equals("<clinit>"))
                clinit = mn;
        if (clinit == null)
            throw new RuntimeException("<clinit> not found in net.minecraft.SharedConstants. Maybe outdated patch?");
        var iter = clinit.instructions.iterator();

        // NeoForge: IS_RUNNING_WITH_JDWP = ManagementFactory.getRuntimeMXBean().getInputArguments()
        //      .stream().anyMatch(str->str.startsWith("-agentlib:jdwp"))
        iter.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                myname,
                "isRunningWithJDWP",
                "()Z"
        ));
        iter.add(new FieldInsnNode(Opcodes.PUTSTATIC, target, "IS_RUNNING_WITH_JDWP", "Z"));
        // IS_RUNNING_IN_IDE = BerryLoader.isDevelopment()
        iter.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                ReflectionUtil.getInternalName(BerryLoader.class),
                "isDevelopment",
                "()Z"
        ));
        iter.add(new FieldInsnNode(Opcodes.PUTSTATIC, target, "IS_RUNNING_IN_IDE", "Z"));

        while (iter.hasNext()) {
            var insn = iter.next();
            if (insn instanceof MethodInsnNode meth && meth.owner.equals("io/netty/util/ResourceLeakDetector") && meth.name.equals("setLevel")) {
                // Change the method owner
                meth.owner = myname;
            }
        }
    }

    public static boolean isRunningWithJDWP() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments()
                .stream().anyMatch(str -> str.startsWith("-agentlib:jdwp"));
    }

    public static void setLevel(ResourceLeakDetector.Level level) {
        if (System.getProperty("io.netty.leakDetection.level") == null)
            ResourceLeakDetector.setLevel(level);
    }
}