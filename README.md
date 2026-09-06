# aa-modlib - Minecraft 1.21.1, NeoForge + Fabric

### Platform

- `ModPlatform` — the parts of a loader a mod actually has to ask about: whether a mod is loaded, whether
  this is a dev or datagen run, client or server, the config directory, and locating a resource inside
  every loaded mod file. A consumer extends it with its own interface and implements the whole thing once
  per loader.
- `Services` — resolves the loader-specific implementation.
- `AddonSource` — one mod file that contains a named resource, for addons that ship data rather than code.

### Config

- `TomlListReader` — reads named lists straight out of a TOML file, for registration that has to happen
  before either loader has loaded its config spec.
- `ConfigSpecBuilder`, `AddonSpecLoader`, and the per-loader `NeoForgeConfigSpec` / `FabricConfigSpec` —
  one spec description, built into whichever config system the loader uses.
- `ConfigSpecOverlay` — wraps a `ConfigSpecBuilder` so something outside the schema supplies the default
  an option is declared with, keyed by the dotted path the option sits at. The schema goes on naming its
  own built-in defaults; each is offered to the overlay on the way through, so a schema of any size is
  adapted by wrapping it once rather than by threading an extra argument through every `define` call.
  `reportDeclared` hands back every key that was declared, which is how a contribution naming a key
  nobody declares gets reported instead of sitting silently inert.
- `ListParseCache` — parses each config line once rather than on every read.
- `RegistryCondition` — a trailing `@absent:<modids>`, `@mod:<modids>`, `@any:<modids>` or `@always`
  clause on any config-driven registry line, deciding whether that entry registers at all. `absent:` is
  how a mod stands aside for another that already ships the same content instead of giving the player two
  of everything; `mod:` is the mirror, for content only meaningful when another mod supplies the input,
  and `any:` is its loose form, where several mods could supply it and one is enough. Clauses chain,
  and every one of them has to hold: `@absent:alltheores@mod:majruszsdifficulty` is content worth adding
  only where one mod supplies its input and another is not already shipping the result, which neither
  half says alone. Also carries `inherit`, which copies a default's clauses onto a config file written
  before they existed — otherwise deduplication would silently never happen for anyone who had already
  played.
- `ItemFilter`, `TabFilter` — shared halves of filters whose final form differs per loader.

### Fluids

- `FluidBlock` — one builder producing a fluid's source, flowing, block and bucket, per loader.
- `FluidEntry`, `FluidTextureColor` — a fluid's texture and the color averaged from it, so a fluid needs
  no declared color and a redrawn texture cannot drift out of step with a constant.
- `FluidOverlayRenderer`, `FluidSurfaceRenderer`, and the Fabric fog and screen-effect mixins — what
  being submerged in a custom fluid looks like. Fabric has no equivalent of NeoForge's client fluid
  extensions, so the fog colour and the submerged overlay are reached by patching the renderers directly.
  A mixin config carries a single package prefix, so these cannot share the consuming mod's existing one:
  each mod registers a config of its own naming the relocated package.
- `HotLiquidBlock`, reached by `FluidBlock.burnsEntities()` — a fluid that burns whatever stands in it on
  lava's own terms, by calling vanilla's `Entity.lavaHurt`. Heat is the block's doing, not the fluid
  type's: nothing in vanilla reads a fluid's temperature to decide damage, and joining `minecraft:lava`
  to inherit it would also inherit turning to stone on contact with water. Off unless a mod asks for it.

### Registry and blocks

- `RegistryHooks`, `RegistryLookup`, `LazyEntry` — registration and by-id lookup without naming another
  mod's types, so an absent mod costs a lookup rather than the launch.
- `BiteBlock`, `StackedDisplayBlock`, `GenericDisplayBlockEntity`, `DisplayEntry`, `PatternRegistry` —
  blocks that show what is placed on or in them.
- `FluidTankBlockEntity`, `StorageAccess` — fluid storage across the two loaders' incompatible APIs.

### Recipes

- `RecipeReflection` — naming another mod's recipe types without compiling against it, so a mod can stay
  off the classpath and the guarantee remains checkable.
- `ApplicationRecipes`, `DippingRecipes` — shared recipe shapes for applying one item to another.
- `RecipeSubjects`, `EnabledConditions`, and the per-loader `EnabledCondition` / `RecipeConditions` —
  a `<modid>:enabled` recipe condition, registered under the consumer's own namespace so two of them
  never collide. A mod registering its content from an editable config list ships recipes naming
  things the player can take away; without this they load anyway, against an id that was never
  registered. `EnabledConditions` is the codec half, reading a single `id` or a list of `ids`.
  Everything is available until a consumer calls `RecipeSubjects.gate` with its own check, so
  declining the capability is just not calling it.

### Datagen (NeoForge source set; Fabric consumes the generated output)

- `LoadCondition` — one condition stated once and emitted in **both** loaders' spellings, `enabled`
  among them: the datagen that writes a gated recipe and the mod that answers the gate then agree by
  construction rather than by a string spelled the same way twice.
- `Variant` — turns a priority-ordered list of candidates into mutually exclusive, mod-gated variants.
  Recipe inputs can address a tag and stop caring who filled it; outputs must name one id, so where two
  mods ship the same thing the recipe is written once per candidate and gated so exactly one loads.
- `RecipeJson` — builders for the recipe JSON shapes Create and its addons read: ingredients, results,
  chance outputs, and the condition block heading every file. Amounts stay with the consumer, because
  two mods measuring the same fluid can disagree on what an ingot is worth and a constant here would
  quietly impose one of them.
- `DatagenHelpers` — a fluid's inset block model, its convention tag, and the loot table for a block that
  drops itself.
- `DedupedTagAppender` — appends tag entries while dropping any pair already added, which a provider
  walking both items and blocks will otherwise produce without meaning to.

### Create compatibility (NeoForge source set)

- `BasinFluidCapacity` and its two mixins — raise Create's basin from two distinct fluids to four on each
  side, on both the block and the recipe codec. Two is the single biggest constraint on writing alloy
  recipes: an alloy of three metals cannot be expressed at all, and mods work around it by inventing
  intermediate alloys that exist only to get under the limit. `OUTPUT_TANKS` matches `INPUT_TANKS` because
  a recipe consuming four fluids then had nowhere to put more than Create's stock two; per-segment
  capacity is untouched at 1000 mB. The recipe-side half is required rather than cosmetic, because on
  1.21.1 `ProcessingRecipe.validate()` is wired into the codec, so a recipe declaring more fluids than the
  cap fails to load rather than merely logging.

  On by default; a consumer that wants it configurable calls `BasinFluidCapacity.gate` with its own
  check. Pair with `ModPresenceMixinPlugin` so it applies only when Create is installed.

### Build

- `multiloader-common`, `multiloader-loader`, `multiloader-resources` — the convention plugins each
  consumer's `buildSrc` compiles, so a multiloader build is described once rather than hand-copied per
  mod. The bundled LICENSE is named after the mod so that nested jars keep one license file each, using
  a filesystem-safe form of `mod_name` rather than the display name: Loom unpacks the sources jar onto
  the real filesystem to remap it, and a display name like `Create: Metalwork` puts a colon into a path,
  which fails `:fabric:remapSourcesJar` outright on Windows.

### Misc

- `EffectChain`, `ItemSpawns` — status effect sequences and item spawning helpers.
- `ModPresenceMixinPlugin` — applies a mixin only when the mod it targets is present.