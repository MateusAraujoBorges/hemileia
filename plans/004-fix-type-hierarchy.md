# Plan: Fix @Borrowed Annotation Type Hierarchy

## Problem Summary

The test `BorrowTests.java` fails with 3 unexpected errors:
- Line 25: `(annotations.on.use)` on `@Borrowed` parameters
- Line 27-28: `(method.invocation)` when calling methods on borrowed references

## Root Cause

**The `@SubtypeOf` annotations on qualifier classes contradict the custom `HemileiaQualifierHierarchy`.**

### Current (broken) `@SubtypeOf` declarations:
```
@Owned (top - empty @SubtypeOf, has @DefaultQualifierInHierarchy)
   ↑
@Borrowed (@SubtypeOf(Owned.class))
@MutBorrowed (@SubtypeOf(Owned.class))
   ↑
@Moved (@SubtypeOf({Borrowed.class, MutBorrowed.class}))
```

### Custom hierarchy (in `isSubtypeQualifiers()`):
```
@Borrowed    @MutBorrowed (tops)
      \      /
       @Owned (owned values can be implicitly borrowed)
          |
       @Moved (bottom)
```

The Checker Framework validates that `@SubtypeOf` annotations are consistent with the hierarchy. When `@Borrowed` declares `@SubtypeOf(Owned.class)` but the custom hierarchy says `@Owned <: @Borrowed`, this causes validation errors.

## Fix

Align the `@SubtypeOf` annotations with the intended hierarchy:

### File: `src/main/java/name/mateusborges/annotations/Borrowed.java`
**Change:** `@SubtypeOf(Owned.class)` → `@SubtypeOf({})`

### File: `src/main/java/name/mateusborges/annotations/MutBorrowed.java`
**Change:** `@SubtypeOf(Owned.class)` → `@SubtypeOf({})`

### File: `src/main/java/name/mateusborges/annotations/Owned.java`
**Change:** `@SubtypeOf({})` → `@SubtypeOf({Borrowed.class, MutBorrowed.class})`

### File: `src/main/java/name/mateusborges/annotations/Moved.java`
**Change:** `@SubtypeOf({Borrowed.class, MutBorrowed.class})` → `@SubtypeOf({Owned.class})`

Note: `@Owned` keeps `@DefaultQualifierInHierarchy` - this marks the default for unannotated types and doesn't need to be at the hierarchy top.

## Verification

Run the tests to confirm the fix:
```bash
./mvnw test -Dtest=HemileiaCheckerTest
```

Expected: All 9 expected diagnostics found, 0 unexpected diagnostics (excluding Notes).


# Plan v2: Fix Type Hierarchy Inconsistency in HemileiaQualifierHierarchy

Problem Summary

The structural @SubtypeOf annotations implement a linear capability-based hierarchy, but the custom isSubtypeQualifiers() method incorrectly makes @Borrowed and @MutBorrowed incomparable.

Current Structural Hierarchy (correct):

@Borrowed (top - read-only capability)                                                                                                                                                                                             
↑                                                                                                                                                                                                                              
@MutBorrowed (read/write capability)                                                                                                                                                                                               
↑                                                                                                                                                                                                                              
@Owned (all capabilities: read/write/move)                                                                                                                                                                                         
↑                                                                                                                                                                                                                              
@Moved (bottom - no capabilities)

Current isSubtypeQualifiers() (incorrect):

Lines 70-76 make @Borrowed and @MutBorrowed incomparable, contradicting the structural hierarchy.

Capability-Based Reasoning                                                                                                                                                                                                         
┌──────────────┬───────────────────┬─────────────────────────┐                                                                                                                                                                     
│  Qualifier   │   Capabilities    │   Can Substitute For    │                                                                                                                                                                     
├──────────────┼───────────────────┼─────────────────────────┤                                                                                                                                                                     
│ @Owned       │ read, write, move │ @MutBorrowed, @Borrowed │                                                                                                                                                                     
├──────────────┼───────────────────┼─────────────────────────┤                                                                                                                                                                     
│ @MutBorrowed │ read, write       │ @Borrowed               │                                                                                                                                                                     
├──────────────┼───────────────────┼─────────────────────────┤                                                                                                                                                                     
│ @Borrowed    │ read              │ (top)                   │                                                                                                                                                                     
├──────────────┼───────────────────┼─────────────────────────┤                                                                                                                                                                     
│ @Moved       │ none              │ (bottom)                │                                                                                                                                                                     
└──────────────┴───────────────────┴─────────────────────────┘                                                                                                                                                                     
In Rust, &mut T can be coerced to &T because read/write access includes read-only access.

Fix

File: src/main/java/name/mateusborges/checker/HemileiaQualifierHierarchy.java

1. Remove incomparability check (lines 70-76):                                                                                                                                                                                     
   Delete the code that returns false for @MutBorrowed <: @Borrowed
2. Add @MutBorrowed <: @Borrowed relationship:                                                                                                                                                                                     
   Update the subtype logic to include this relationship
3. Update docstring (lines 18-37):                                                                                                                                                                                                 
   Change from diamond hierarchy to linear hierarchy
4. Simplify LUB/GLB (lines 89-119):                                                                                                                                                                                                
   Remove special cases for incomparable types since hierarchy is now linear

Files to Modify

- src/main/java/name/mateusborges/checker/HemileiaQualifierHierarchy.java

Verification

./mvnw test -Dtest=HemileiaCheckerTest

Expected: All tests pass (9/9 expected diagnostics, 0 unexpected errors).                                                                                                                                                          
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌

Requested permissions:                                                                                                                                                                                                             
· Bash(prompt: run tests)                                         