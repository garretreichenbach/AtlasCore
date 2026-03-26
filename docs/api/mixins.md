# Mixins

AtlasCore uses SpongePowered Mixin instead of StarMade's `overwriteClass()` mechanism. This is safer, more maintainable, and compatible with other mods that touch the same classes.

## Configuration

Mixins are declared in `core/src/main/resources/atlascore.mixins.json`:

```json
{
  "required": true,
  "package": "atlas.core.mixin",
  "compatibilityLevel": "JAVA_8",
  "mixins": [],
  "client": ["MixinCatalogPanelNew"],
  "server": [],
  "injectors": { "defaultRequire": 1 }
}
```

And referenced in `mod.json`:

```json
{
  "mixin_configs": ["atlascore.mixins.json"]
}
```

## Current Mixins

| Class | Target | Side | Purpose |
|-------|--------|------|---------|
| `MixinCatalogPanelNew` | `CatalogPanelNew` | Client | Injects custom tab into the catalog panel |

## Writing a Mixin

```java
@Mixin(value = TargetClass.class, remap = false)
public class MixinTargetClass {

    // Access a private field
    @Shadow
    private SomeType privateField;

    // Inject at the start of a method
    @Inject(method = "someMethod", at = @At("HEAD"))
    private void onSomeMethod(CallbackInfo ci) {
        // your code runs here, before the original
    }

    // Inject at the return point
    @Inject(method = "anotherMethod", at = @At("RETURN"))
    private void afterAnotherMethod(CallbackInfoReturnable<String> cir) {
        // runs just before the method returns
    }
}
```

## Common Patterns

### Inserting a new tab (`@Inject` at `HEAD`)

```java
@Inject(method = "recreateTabs", at = @At("RETURN"))
private void addCustomTab(CallbackInfo ci) {
    // add your tab to the already-populated tab list
}
```

### Modifying a return value

```java
@Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
private void modifyValue(CallbackInfoReturnable<Integer> cir) {
    cir.setReturnValue(cir.getReturnValue() + 1);
}
```

## Sub-Mods and Mixins

Sub-mods should **not** add their own `mixin_configs` unless they have a compelling reason. Prefer hooking through `IAtlasSubMod` callbacks first. If you truly need a game-class injection, open a PR to add it to AtlasCore's mixin package so it stays centralized.
