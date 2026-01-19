# Hemileia

A pluggable type system for Java that implements Rust-style ownership and borrowing semantics using the Checker Framework.

## Project Overview

Hemileia is an academic project that brings Rust's memory safety guarantees to Java at compile time through a custom type checker. The name comes from *Hemileia*, a genus of rust fungi—a nod to the Rust programming language.

## Goals

- Implement a subset of Rust's ownership/borrowing type system as a Java pluggable type checker
- Enforce memory safety rules at compile time via annotations
- Leverage the Checker Framework for type system implementation

## Annotations

The type system uses the following annotations to express ownership semantics:

| Annotation | Rust Equivalent | Description |
|------------|-----------------|-------------|
| `@Owned` | `T` (owned) | Exclusive ownership of a value |
| `@Borrowed` | `&T` | Immutable/shared borrow |
| `@MutBorrowed` | `&mut T` | Mutable/exclusive borrow |

## Build & Run

Always use the mvn wrapper `mvnw`

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Package
./mvnw package
```

## Project Structure

```
src/
├── main/java/name/mateusborges/
│   ├── checker/          # Checker Framework type checker implementation
│   ├── annotations/      # @Owned, @Borrowed, @MutBorrowed definitions
│   └── ...
└── test/java/            # Test cases for the type checker
```

## Folders to ignore

 - `plans` - Old claude code plans that are not relevant anymore.

## Checker Framework Integration

This project uses the [Checker Framework](https://checkerframework.org/) to implement the pluggable type system. Key components to implement:

1. **Annotations** - Type qualifiers (`@Owned`, `@Borrowed`, `@MutBorrowed`)
2. **Type Hierarchy** - Subtyping relationships between qualifiers
3. **Visitor/Checker** - Rules for ownership transfer and borrow checking

## Rust Rules to Enforce (Subset)

1. Each value has exactly one owner at a time
2. When the owner goes out of scope, the value is dropped
3. You can have either one mutable borrow OR any number of immutable borrows (not both)
4. Borrows must not outlive the owner

## Rust features that are out of scope

1. unsafe blocks

## Dependencies

- Java 25
- Maven
- Checker Framework
- SLF4J with Logback

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

## Checker Framework Technical Notes

### TransferResult Types

When implementing transfer functions in `HemileiaTransfer.java`, be aware that `visit*` methods can return different `TransferResult` types:

- **`RegularTransferResult`**: Single store for normal flow. Use `getRegularStore()`.
- **`ConditionalTransferResult`**: Two stores (then/else) for conditional flow. Method invocations return this type because they can throw exceptions.

**Critical Pattern**: When modifying stores in a transfer function, always handle both types:

```java
if (result.containsTwoStores()) {
    modifyStore(result.getThenStore());
    modifyStore(result.getElseStore());
} else {
    modifyStore(result.getRegularStore());
}
```

Modifying only `getRegularStore()` on a `ConditionalTransferResult` will NOT propagate changes to the normal flow path.

Reference: [NullnessTransfer.java](https://github.com/typetools/checker-framework/blob/master/checker/src/main/java/org/checkerframework/checker/nullness/NullnessTransfer.java)

### Custom Store State and Fixpoint Detection

When extending `CFAbstractStore` with custom fields (like `movedVariables`, `activeBorrows`), you **must** override `supersetOf()` to include those fields in the comparison. The Checker Framework uses `supersetOf()` (via `equals()`) for fixpoint detection during loop analysis.

**Why this matters**: Without overriding `supersetOf()`, the dataflow analysis only compares the VALUE maps (type refinements). If your custom fields change but VALUE maps are equal, the analysis will prematurely declare fixpoint and stop iterating—causing bugs like use-after-move not being detected in loops.

**Required pattern** in custom store classes:

```java
@Override
protected boolean supersetOf(CFAbstractStore<V, S> other) {
    if (!super.supersetOf(other)) {
        return false;
    }
    if (!(other instanceof MyStore otherStore)) {
        return false;
    }
    // Include ALL custom fields in comparison
    if (!this.customSet.containsAll(otherStore.customSet)) {
        return false;
    }
    // ... check other custom fields
    return true;
}
```

Also override `hashCode()` for consistency.

Reference: [NullnessStore.java](https://github.com/typetools/checker-framework/blob/master/checker/src/main/java/org/checkerframework/checker/nullness/NullnessStore.java)

### Stub Files for JDK/Library Annotations

Stub files (`.astub`) allow you to add type annotations to JDK classes and third-party libraries without modifying their source code. This is essential for annotating methods like `StringBuilder.toString()` with `@Borrowed` receiver types.

**Location**: Stub files must be in the same directory as the checker class:
```
src/main/java/name/mateusborges/checker/
├── HemileiaChecker.java
├── jdk.astub              # Stub file for JDK classes
└── ...
```

**Registration**: Add the `@StubFiles` annotation to the checker class:

```java
import org.checkerframework.framework.qual.StubFiles;

@StubFiles("jdk.astub")
@RelevantJavaTypes(Object.class)
public class HemileiaChecker extends BaseTypeChecker {
    // ...
}
```

**Maven Configuration**: The `pom.xml` must include `.astub` files in the resources:

```xml
<resources>
    <resource>
        <directory>src/main/java</directory>
        <includes>
            <include>**/*.properties</include>
            <include>**/*.astub</include>
        </includes>
    </resource>
</resources>
```

**Explicit Receiver Parameters**: To annotate the `this` parameter of instance methods, use Java's explicit receiver parameter syntax:

```java
// In jdk.astub
package java.lang;

class StringBuilder {
    // Annotate `this` as @Borrowed - method doesn't consume ownership
    String toString(@Borrowed StringBuilder this) {}

    // Annotate `this` as @MutBorrowed - method mutates but doesn't consume
    StringBuilder append(@MutBorrowed StringBuilder this, String str);
}
```

The explicit receiver parameter (`Type this`) is a Java language feature (since Java 8) that allows annotating the receiver. It doesn't change the method signature—it just provides a place to attach annotations.

**Stub file format notes**:
- Import annotations at the top of the file (before `package`)
- Use `{}` for method bodies (or omit entirely for abstract methods)
- Only include methods you want to annotate—others inherit defaults
- For inherited methods, annotate them in the class where you want the annotation to apply

Reference: [Checker Framework Stub Files Manual](https://checkerframework.org/manual/#stub)
