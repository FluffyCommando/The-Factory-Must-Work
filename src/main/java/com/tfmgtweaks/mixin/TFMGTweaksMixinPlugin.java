package com.tfmgtweaks.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Refuses to load without TFMG:CE, since every mixin in this build targets TFMG:CE 1.3.2+. */
public class TFMGTweaksMixinPlugin implements IMixinConfigPlugin {
    private static final String CE_MARKER_CLASS = "com.drmangotea.tfmg.content.engines.fuels.EngineFuelType";

    @Override
    public void onLoad(String mixinPackage) {
        boolean isCommunityEdition;
        try {
            Class.forName(CE_MARKER_CLASS, false, TFMGTweaksMixinPlugin.class.getClassLoader());
            isCommunityEdition = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            isCommunityEdition = false;
        }
        if (!isCommunityEdition) {
            throw new IllegalStateException("[tfmgtweaks] This build of Create: The Factory Must WORK is for TFMG:CE, "
                    + "but original TFMG is installed. Use the TFMG 1.2.x build of this mod instead.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
