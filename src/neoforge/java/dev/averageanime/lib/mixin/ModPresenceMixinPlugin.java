package dev.averageanime.lib.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies a mixin config only when some other mod is present.
 *
 * <p>The detection is the interesting part. It goes through FML's loading mod list rather than probing
 * for one of the other mod's classes, because {@code Class.forName} here would be actively harmful:
 * config plugins run during mixin <em>prepare</em>, and loading a class resolves its supertypes with it.
 * Loading, say, Create's {@code BasinBlockEntity} pulls in {@code SmartBlockEntity} and then vanilla
 * {@code BlockEntity} -- putting a vanilla class on the classloader before other mods' configs have been
 * prepared. Any mod holding a mixin on it (Lithium targets {@code BlockEntity}) then aborts the launch
 * with {@code MixinTargetAlreadyLoadedException}.
 */
public abstract class ModPresenceMixinPlugin implements IMixinConfigPlugin {

    /** Mod that must be loaded for this config to apply. */
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
