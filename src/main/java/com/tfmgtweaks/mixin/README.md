# Mixins

Put mixins here, then list them in `src/main/resources/tfmgtweaks.mixins.json`
under `"mixins"` (or `"client"` for client-only ones).

Two common patterns, both used heavily in TFMG's own `mixin/accessor` package:

## 1. Accessor/Invoker — reach a private field/method without changing behavior

```java
package com.tfmgtweaks.mixin.accessor;

import com.drmangotea.tfmg.content.machinery.vat.VatBlockEntity; // example target
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VatBlockEntity.class)
public interface VatBlockEntityAccessor {
    @Accessor("somePrivateField")
    int tfmgtweaks$getSomePrivateField();

    @Invoker("somePrivateMethod")
    void tfmgtweaks$callSomePrivateMethod();
}
```

## 2. Inject/Redirect — actually change TFMG's behavior (the real bug fixes)

```java
package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.VatBlockEntity; // example target
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VatBlockEntity.class)
public class VatBlockEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$fixSomeBug(CallbackInfo ci) {
        // ...
    }
}
```

Note the naming convention: prefix injected/accessor members with `tfmgtweaks$`
(TFMG does the same with `tfmg$`) so they can't collide with real members.

Remember: whatever class you `@Mixin(...)` onto must actually be on the
compile classpath, which means the TFMG jar in `/libs` has to match the
version you're running in-game, or the field/method names may have moved.
