package dev.averageanime.lib.compat;

/** How many distinct fluids a Create basin holds on each side. On by default; a consumer wanting it configurable calls {@link #gate}. */
public final class BasinFluidCapacity {

    /** Four fits every alloy anyone has needed. */
    public static final int INPUT_TANKS = 4;

    /** Matched to {@link #INPUT_TANKS} so a recipe consuming four fluids can produce four; per-segment capacity stays at Create's 1000 mB. */
    public static final int OUTPUT_TANKS = 4;

    private static java.util.function.BooleanSupplier enabled = () -> true;

    private BasinFluidCapacity() {}

    /** Overrides the always-on default, for a consumer that exposes this as a config option. */
    public static void gate(java.util.function.BooleanSupplier check) {
        enabled = check;
    }

    public static boolean isEnabled() {
        return enabled.getAsBoolean();
    }
}
