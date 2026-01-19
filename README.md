# FAQ (from Claude, to be reviewed)
## Why Primitives Are Excluded

Annotating local primitive variables result in a warning such as "anno.on.irrelevant".

The checker uses `@RelevantJavaTypes(Object.class)` to exclude Java primitives from borrow checking. This mirrors Rust's `Copy` trait behavior:

- **Rust's Copy trait**: Primitives (`i32`, `f64`, `bool`, etc.) implement `Copy`, so they're duplicated on assignment rather than moved. The borrow checker doesn't track ownership for them.
- **Java primitives have value semantics**: When you pass an `int` to a method or assign it to another variable, the value is copied. There's no aliasing—you cannot have two variables referring to the "same" `int` in memory.
- **No reference semantics for primitives**: Unlike objects where multiple variables can reference the same heap object (creating aliasing concerns), primitives are always copied. You cannot create a "borrow" of a primitive.

Since Java primitives inherently have copy/value semantics, ownership tracking is irrelevant for them—only reference types (objects) need borrow checking.

## What is the type hierarchy?

The qualifier hierarchy is a linear chain based on capability inclusion:

```
    @Borrowed (top - read-only capability)
        ↑
    @MutBorrowed (read/write capability)
        ↑
    @Owned (all capabilities: read/write/move)
        ↑
    @Moved (bottom - no capabilities)
```

Key relationships:
- `@Owned <: @MutBorrowed` — owned values can be mutably borrowed
- `@MutBorrowed <: @Borrowed` — read/write access includes read-only access
- `@Moved` is the bottom type — moved values cannot be used

This matches Rust's coercion semantics where `&mut T` can coerce to `&T`. In capability terms, a type with more capabilities (read+write) can substitute for one requiring fewer capabilities (read-only).

## Logging

The project uses SLF4J with Logback for logging. Configuration is in `src/main/resources/logback.xml`.

### Adjusting Log Levels

To enable debug logging for the Hemileia checker, edit `src/main/resources/logback.xml` and change:

```xml
<logger name="name.mateusborges.checker" level="WARN" />
```

to:

```xml
<logger name="name.mateusborges.checker" level="DEBUG" />
```

Available log levels (from most to least verbose): `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`

## Why do I need to override `supersetOf()` in custom stores?

When your store tracks custom state beyond the standard VALUE map (like `movedVariables` or `activeBorrows`), you **must** override `supersetOf()` to include those fields in the comparison.

The Checker Framework's dataflow analysis uses `supersetOf()` (via `equals()`) to detect when fixpoint is reached during loop analysis. Without the override:

1. First loop iteration: Your transfer function updates custom fields (e.g., marks variables as moved)
2. At the loop back-edge: The framework compares stores to check for fixpoint
3. **Bug**: Parent's `supersetOf()` only compares VALUE maps, not your custom fields
4. VALUE maps may be equal even though custom fields differ → premature fixpoint declared
5. Analysis stops before propagating your custom state through subsequent iterations

**Symptom**: Errors like use-after-move not detected inside loops, even though they're detected in straight-line code.

**Fix**: Override `supersetOf()` to include all custom fields:

```java
@Override
protected boolean supersetOf(CFAbstractStore<V, S> other) {
    if (!super.supersetOf(other)) return false;
    if (!(other instanceof MyStore o)) return false;
    return this.movedVariables.containsAll(o.movedVariables);
    // ... and other custom fields
}
```