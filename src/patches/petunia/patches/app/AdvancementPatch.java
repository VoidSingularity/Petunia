package petunia.patches.app;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.Advancement;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import petunia.patches.IClassPatcher;
import petunia.patches.Patch;

import java.util.Objects;


@Patch("net/minecraft/advancements/Advancement")
public class AdvancementPatch implements IClassPatcher {
    @Override
    public void patch(ClassNode cn){
        Codec<Advancement> CODEC = null;
        for(FieldNode fn:cn.fields){
            if(fn.name.equals("CODEC")){
                CODEC=(Codec<Advancement>) fn.value;
            }
        }
        if (CODEC == null) throw new RuntimeException ("CODEC not found in net.minecraft.Advancement. Maybe outdated patch?");
        //Codec<Optional<net.neoforged.neoforge.common.conditions.WithConditions<Advancement>>>
        cn.fields.add(new FieldNode(
                Opcodes.ASM9,
                Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,
                "CONDITIONAL_CODEC",
                "Lcom/mojang/serialization/Codec;",
                "Lcom/mojang/serialization/Codec<Tjava/util/Optional<net/neoforged/neoforge/common/conditions/WithConditions<Tnet/minecraft/advancements/Advancement;>;>;>;",
                ConditionalOps.createConditionalCodecWithConditions(CODEC)
        ));
    }
}
