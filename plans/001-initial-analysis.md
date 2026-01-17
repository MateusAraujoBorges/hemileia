# Hemileia: Lifetime Analysis Implementation Plan

## Current Status

### Completed Files
- [x] `src/main/java/name/mateusborges/annotations/Owned.java` - Updated with `@SubtypeOf`
- [x] `src/main/java/name/mateusborges/annotations/Borrowed.java` - Updated with `@SubtypeOf`
- [x] `src/main/java/name/mateusborges/annotations/MutBorrowed.java` - Updated with `@SubtypeOf`
- [x] `src/main/java/name/mateusborges/annotations/Moved.java` - Created (bottom type)
- [x] `src/main/java/name/mateusborges/checker/HemileiaChecker.java` - Created
- [x] `src/main/java/name/mateusborges/checker/HemileiaValue.java` - Created
- [x] `src/main/java/name/mateusborges/checker/HemileiaStore.java` - Created
- [x] `src/main/java/name/mateusborges/checker/HemileiaTransfer.java` - Created
- [x] `src/main/java/name/mateusborges/checker/HemileiaAnalysis.java` - Created
- [x] `src/main/java/name/mateusborges/checker/HemileiaAnnotatedTypeFactory.java` - Created (has compilation error)
- [x] `src/main/java/name/mateusborges/checker/HemileiaQualifierHierarchy.java` - Created
- [x] `src/main/java/name/mateusborges/checker/HemileiaVisitor.java` - Created
- [x] `src/main/java/name/mateusborges/checker/messages.properties` - Created
- [x] `pom.xml` - Updated with checkerframework profile

### Blocking Issue
**Compilation fails** due to API mismatch in `HemileiaAnnotatedTypeFactory.java:65`

---

## Overview

Implement Rust-style lifetime tracking for Java using the Checker Framework's built-in dataflow analysis infrastructure.

## Design Decision: Why Extend BaseTypeChecker, Not Raw Abstract Classes?

### Options Considered

1. **Subtyping Checker only** - Simplest, but insufficient
2. **BaseTypeChecker + GenericAnnotatedTypeFactory** - Simple and sufficient (chosen)
3. **Custom CFAbstractStore/CFAbstractAnalysis** - Most flexible, most complex

### Rationale

The **Subtyping Checker** handles structural type relationships but Rust's ownership rules are **flow-sensitive**:

| Rule | Subtyping Checker? | Needs Dataflow? |
|------|-------------------|-----------------|
| `@Owned` can't be assigned to `@MutBorrowed` | Yes | No |
| Use-after-move is invalid | No | Yes |
| Only one `@MutBorrowed` at a time | No | Yes |
| Borrow can't outlive owner | No | Yes |

**Decision**: Use `GenericAnnotatedTypeFactory` with `flowByDefault = true`. This provides:
- Built-in flow-sensitive type refinement (framework handles CFG traversal)
- Override specific methods to add ownership rules
- No need to implement custom `CFAbstractStore` subclass initially

The framework's default dataflow already tracks annotation state per variable through control flow. We add custom checks in the Visitor for ownership-specific rules.

---

## Implementation Plan

### Phase 1: Core Checker Infrastructure

**Files to create in `src/main/java/name/mateusborges/checker/`:**

#### 1.1 `HemileiaChecker.java`
- Extend `BaseTypeChecker`
- Entry point for javac integration
- Minimal implementation—just wire up the components

#### 1.2 `HemileiaAnnotatedTypeFactory.java`
- Extend `GenericAnnotatedTypeFactory<HemileiaValue, HemileiaStore, HemileiaTransfer, HemileiaAnalysis>`
- Enable flow-sensitive analysis
- Define supported type qualifiers: `@Owned`, `@Borrowed`, `@MutBorrowed`
- Set up qualifier hierarchy (subtyping relationships)

#### 1.3 `HemileiaVisitor.java`
- Extend `BaseTypeVisitor<HemileiaAnnotatedTypeFactory>`
- Override `visitAssignment()` to detect ownership transfer (move semantics)
- Override `visitIdentifier()` to detect use-after-move
- Override `visitMethodInvocation()` to check borrow rules on arguments

### Phase 2: Qualifier Hierarchy

Define subtyping relationships between annotations:

```
        @Owned (top - most permissive to hold)
       /      \
@Borrowed    @MutBorrowed
       \      /
        (bottom)
```

**Key rules:**
- `@Owned` can be borrowed as `@Borrowed` or `@MutBorrowed`
- `@Borrowed` and `@MutBorrowed` are incompatible with each other
- Assignments that would violate exclusivity are errors

### Phase 3: Ownership Transfer (Move Semantics)

Track when `@Owned` values are moved:

```java
@Owned String a = "hello";
@Owned String b = a;  // Move: 'a' is now invalid
System.out.println(a); // ERROR: use after move
```

**Implementation:**
- In `HemileiaVisitor.visitAssignment()`: when RHS is `@Owned`, mark the source variable as "moved"
- In `HemileiaVisitor.visitIdentifier()`: check if variable was moved, report error
- Track moved state in the dataflow store

### Phase 4: Borrow Checking

Enforce Rust's borrow rules:

```java
@Owned String owner = "hello";
@Borrowed String b1 = owner;    // OK: immutable borrow
@Borrowed String b2 = owner;    // OK: multiple immutable borrows allowed
@MutBorrowed String m = owner;  // ERROR: can't mut-borrow while immutably borrowed
```

**Implementation:**
- Track active borrows per variable in the store
- On `@MutBorrowed` assignment: check no other borrows exist
- On `@Borrowed` assignment: check no `@MutBorrowed` exists

### Phase 5: Scope-Based Lifetime Tracking

Detect when borrows outlive their owner:

```java
@Borrowed String dangling;
{
    @Owned String owner = "hello";
    dangling = owner;  // Borrow created
}  // owner goes out of scope
// dangling is now invalid - borrow outlived owner
```

**Implementation:**
- Associate each borrow with the scope depth where the owner was declared
- On scope exit: invalidate borrows whose owners left scope
- Use `GenericAnnotatedTypeFactory`'s built-in CFG analysis for scope tracking

---

## Files to Modify/Create

### New Files
| File | Purpose |
|------|---------|
| `src/main/java/name/mateusborges/checker/HemileiaChecker.java` | Checker entry point |
| `src/main/java/name/mateusborges/checker/HemileiaAnnotatedTypeFactory.java` | Type factory with flow-sensitivity |
| `src/main/java/name/mateusborges/checker/HemileiaVisitor.java` | Custom rule checking |
| `src/main/java/name/mateusborges/checker/HemileiaQualifierHierarchy.java` | Subtyping relationships |
| `src/main/java/name/mateusborges/checker/HemileiaValue.java` | Custom abstract value (if needed) |
| `src/main/java/name/mateusborges/checker/HemileiaStore.java` | Custom store for borrow tracking |
| `src/main/java/name/mateusborges/checker/HemileiaTransfer.java` | Transfer function for moves/borrows |
| `src/main/java/name/mateusborges/checker/HemileiaAnalysis.java` | Analysis coordinator |

### Existing Files to Update
| File | Change |
|------|--------|
| `pom.xml` | Ensure Checker Framework annotation processor is configured |

---

## Verification Plan

### Unit Tests
Run existing tests to ensure annotations still work:
```bash
./mvnw test
```

### Manual Verification
Create test cases that should fail:

1. **Use-after-move**: Assign `@Owned` then use original variable
2. **Conflicting borrows**: Create `@MutBorrowed` while `@Borrowed` exists
3. **Borrow outlives owner**: Return a borrow of a local variable

### Integration Test
Run the checker on test files:
```bash
./mvnw compile -Pcheckerframework
```

---

## Implementation Order

1. `HemileiaChecker.java` - Basic entry point
2. `HemileiaAnnotatedTypeFactory.java` - Enable flow-sensitivity, set up qualifiers
3. `HemileiaQualifierHierarchy.java` - Define subtyping
4. `HemileiaVisitor.java` - Add custom rule checks
5. Custom store/transfer/analysis classes as needed for borrow tracking
6. Tests for each rule

---

## Remaining Tasks

### 1. Fix Compilation Error in HemileiaAnnotatedTypeFactory.java

**File:** `src/main/java/name/mateusborges/checker/HemileiaAnnotatedTypeFactory.java:65`

**Error:** `method does not override or implement a method from a supertype`

**Problem:** The `createFlowAnalysis(int maxCountBeforeWidening)` method signature doesn't match the parent class. The Checker Framework 3.53.0 API may have changed or the method name differs.

**Fix Required:**
- Check the actual method signature in `GenericAnnotatedTypeFactory`
- Either remove the `@Override` annotation if creating a new method, or
- Match the exact signature from the parent class

---

### 2. Create Test Cases for Ownership Rules

Tests should be created in `src/test/java/name/mateusborges/checker/` to verify the checker correctly identifies ownership violations.

#### 2.1 Use-After-Move Tests (`UseAfterMoveTest.java`)

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| `testSimpleUseAfterMove` | Assign `@Owned` to another `@Owned`, then use original | Error: `use.after.move` |
| `testUseAfterMoveInMethodCall` | Pass `@Owned` to method taking `@Owned`, then use original | Error: `use.after.move` |
| `testNoErrorOnBorrow` | Create `@Borrowed` from `@Owned`, then use original | No error (borrow doesn't move) |
| `testReassignmentClearsMoved` | Move variable, then reassign it, then use | No error (reassignment clears moved) |
| `testMoveInConditional` | Move only in one branch of if-else | Error on use after merge point |

#### 2.2 Borrow Conflict Tests (`BorrowConflictTest.java`)

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| `testMultipleImmutableBorrowsAllowed` | Multiple `@Borrowed` from same owner | No error |
| `testMutBorrowWhileImmutableExists` | `@MutBorrowed` while `@Borrowed` active | Error: `borrow.conflict` |
| `testImmutableBorrowWhileMutExists` | `@Borrowed` while `@MutBorrowed` active | Error: `borrow.conflict` |
| `testMultipleMutBorrows` | Two `@MutBorrowed` from same owner | Error: `multiple.mut.borrow` |
| `testSequentialMutBorrows` | `@MutBorrowed`, end scope, new `@MutBorrowed` | No error (sequential is OK) |

#### 2.3 Lifetime/Scope Tests (`LifetimeTest.java`)

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| `testBorrowOutlivesOwner` | Borrow escapes scope of owner | Error: `borrow.outlives.owner` |
| `testBorrowEndsBeforeOwner` | Borrow in inner scope, owner in outer | No error |
| `testReturnBorrowOfLocal` | Return `@Borrowed` of local variable | Error: `borrow.outlives.owner` |

#### 2.4 Valid Usage Tests (`ValidOwnershipTest.java`)

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| `testSimpleOwnedDeclaration` | Declare and use `@Owned` | No error |
| `testOwnershipTransfer` | Move ownership, use new owner | No error |
| `testBorrowAndReturn` | Borrow in function, owner still valid after | No error |
| `testMutBorrowAndModify` | `@MutBorrowed`, modify, owner reflects change | No error |

---

### 3. Integration Test Setup

**File:** `src/test/java/name/mateusborges/checker/HemileiaCheckerTest.java`

Create a test harness that:
1. Compiles test source files with the Hemileia checker enabled
2. Verifies expected errors are reported
3. Verifies no false positives on valid code

Use the Checker Framework's test utilities:
```java
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
```

**Test Resources Directory Structure:**
```
src/test/resources/
├── useaftermove/           # Test cases for use-after-move
│   ├── UseAfterMoveValid.java
│   └── UseAfterMoveInvalid.java
├── borrowconflict/         # Test cases for borrow conflicts
│   ├── BorrowConflictValid.java
│   └── BorrowConflictInvalid.java
└── lifetime/               # Test cases for lifetime violations
    ├── LifetimeValid.java
    └── LifetimeInvalid.java
```

---

### 4. Update pom.xml for Test Execution

Add test dependencies and configuration:
- Add Checker Framework test utilities dependency
- Configure surefire plugin to run checker tests
- Ensure test resources are copied

---

## Out of Scope (Per CLAUDE.md)

- `unsafe` blocks
- Iterator invalidation (noted in TODO.md for future)
- Native memory safety (noted in TODO.md for future)
