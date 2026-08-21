# aa-modlib

Shared source for Create: Food and Create: Metalwork.

This is **not a mod and not a published artifact**. It has no mod id, no `mods.toml`, no version
number: consuming projects add it as a git submodule and compile the source into their own jar, so the
submodule commit is the version.

## Relocation

Everything here is authored under `dev.averageanime.lib`. Each consumer rewrites that to its own root
at build time -- `dev.averageanime.createfood.lib`, `dev.averageanime.createmetalwork.lib` -- because
both loaders resolve a duplicated fully-qualified class name to whichever jar wins the scan, silently
and non-deterministically. With a `ServiceLoader` interface in here, that would mean one mod picking up
the other mod's platform implementation.

So: never reference `dev.averageanime.lib` from a consumer directly. Reference the relocated root.

## Layout

    src/main/java/dev/averageanime/lib/    shared Java
    gradle/                                shared Gradle convention plugins
