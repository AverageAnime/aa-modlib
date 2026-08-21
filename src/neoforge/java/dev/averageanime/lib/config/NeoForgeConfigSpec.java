package dev.averageanime.lib.config;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Drives NeoForge's config builder from a {@link ConfigSpecBuilder}.
 *
 * <p>A mod describes its options once against the neutral interface; this turns that description into a
 * {@code ModConfigSpec}. Everything NeoForge supports and Fabric does not -- restart flags, element
 * hints -- is honoured here and ignored on the other side.
 */
public final class NeoForgeConfigSpec {

    private NeoForgeConfigSpec() {}

    public static ConfigSpecBuilder adapt(ModConfigSpec.Builder b) {
        return new ConfigSpecBuilder() {
            @Override public void push(String path) { b.push(path); }
            @Override public void pop() { b.pop(); }

            @Override
            public Supplier<Boolean> defineBool(String key, boolean defaultValue) {
                return b.define(key, defaultValue)::get;
            }

            @Override
            public Supplier<Integer> defineInt(String key, int defaultValue, int min, int max, boolean gameRestart) {
                if (gameRestart) b.gameRestart();
                return b.defineInRange(key, defaultValue, min, max)::get;
            }

            @Override
            public Supplier<List<? extends String>> defineList(String key, List<String> defaultValue,
                    Supplier<String> elementHint, Predicate<Object> elementValidator, boolean gameRestart) {
                if (gameRestart) b.gameRestart();
                return b.defineListAllowEmpty(key, defaultValue, elementHint, elementValidator)::get;
            }
        };
    }

    /** Opens NeoForge's generated config screen from the mod list. */
    public static class Screen implements IConfigScreenFactory {
        @Override
        public @NotNull net.minecraft.client.gui.screens.Screen createScreen(
                @NotNull ModContainer modContainer, @NotNull net.minecraft.client.gui.screens.Screen parent) {
            return new ConfigurationScreen(modContainer, parent);
        }
    }
}
