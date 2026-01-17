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