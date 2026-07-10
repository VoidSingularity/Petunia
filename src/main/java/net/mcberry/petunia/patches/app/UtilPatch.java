package net.mcberry.petunia.patches.app;

import net.mcberry.berry.utils.ReflectionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import net.mcberry.petunia.patches.IClassPatcher;
import net.mcberry.petunia.patches.Patch;

import java.util.HashSet;
import java.util.Set;

@Patch(UtilPatch.target)
public class UtilPatch implements IClassPatcher {
    static final String target = "net/minecraft/util/Util";

    @Override
    public void patch(ClassNode cn) {
        // doFetchChoiceType
        MethodNode dfct = null;
        // pause in ide
        Set<MethodNode> piis = new HashSet<>();
        for (MethodNode mn : cn.methods)
            if (mn.name.equals("doFetchChoiceType"))
                dfct = mn;
            else if (mn.name.endsWith("InIde"))
                piis.add(mn);

        if (dfct == null) {
            throw new RuntimeException("doFetchChoiceType not found in net.minecraft.util.Util. Maybe outdated patch?");
        }

        // Modify doFetchChoiceType
        for (var insn : dfct.instructions) {
            if (insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                var ii = (MethodInsnNode) insn;
                if (ii.name.equals("error")) ii.name = "debug"; // Logger.error -> Logger.debug
            } else if (insn.getOpcode() == Opcodes.GETSTATIC) {
                var gf = (FieldInsnNode) insn;
                if (gf.owner.equals(SharedConstantsPatch.target) && gf.name.equals("IS_RUNNING_IN_IDE"))
                    gf.owner = ReflectionUtil.getInternalName(UtilPatch.class); // NeoForge: never throw the exception (in vanilla it is thrown in dev)
            }
        }

        // pause in ide methods
        for (var pii : piis) patch_pause(pii);
    }

    private void patch_pause(MethodNode mn) {
        for (var insn : mn.instructions) {
            if (insn instanceof FieldInsnNode fi && fi.name.equals("IS_RUNNING_IN_IDE"))
                fi.name = "IS_RUNNING_WITH_JDWP"; // NeoForge: pause only with JDWP
        }
    }

    public static final boolean IS_RUNNING_IN_IDE = false;
}