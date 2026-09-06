package dev.averageanime.lib.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies a mixin config only when another mod is present. Detection goes through FML's loading mod list, never {@code Class.forName}, which would load supertypes during mixin prepare and abort the launch. */
public abstract class ModPresenceMixinPlugin implements IMixinConfigPlugin {

    protected abstract String requiredModId();

    private boolean present;

    @Override
    public void onLoad(String mixinPackage) {
        present = LoadingModList.get().getModFileById(requiredModId()) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return present;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
