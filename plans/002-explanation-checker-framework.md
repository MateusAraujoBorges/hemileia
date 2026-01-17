Based on my exploration of the codebase, here's a comprehensive explanation:

1. How the Checker Framework's Main Algorithm Works

The Checker Framework implements pluggable type checking through a three-phase algorithm:

Phase 1: Type Hierarchy Definition

The framework first establishes subtyping relationships between your custom type qualifiers. In Hemileia:

@Borrowed    @MutBorrowed   (tops - incomparable to each other)   
    \          /                                                                                                              
       @Owned              (default)
         |                                                                                                    
       @Moved              (bottom)

This hierarchy enables implicit conversions: an @Owned value can be passed where @Borrowed or @MutBorrowed is expected (borrowed implicitly), but not vice versa.

Phase 2: Dataflow Analysis

The framework performs flow-sensitive type refinement by traversing the control flow graph (CFG):

1. Forward analysis propagates abstract values through statements
2. At each statement, a transfer function updates the store (mapping variables → abstract values)
3. At merge points (if/else joins, loop headers), stores are combined via least upper bound
4. Fixed-point iteration continues until stores stabilize

This allows tracking state changes like "variable x was moved on line 10, so it's invalid on line 11."

Phase 3: AST Visitor Checking

After dataflow analysis, a visitor walks the AST and checks constraints at each node, reporting errors when rules are violated.
                                                                                                                                                                                                                                    
---                                                                                                                                                                                                                               
2. Purpose of Each Class in name.mateusborges.checker

HemileiaChecker.java — Entry Point

- Extends BaseTypeChecker
- The class name you pass to javac -processor
- Bootstraps everything else via naming convention: HemileiaChecker → looks for HemileiaAnnotatedTypeFactory, HemileiaVisitor, etc.

HemileiaAnnotatedTypeFactory.java — Type Factory

- Creates and caches annotation mirrors (@Owned, @Borrowed, etc.)
- Sets @Owned as the default qualifier for unannotated types
- Wires together the qualifier hierarchy, analysis, and transfer function:
    - createQualifierHierarchy() → returns HemileiaQualifierHierarchy
    - createFlowAnalysis() → returns HemileiaAnalysis
    - createFlowTransferFunction() → returns HemileiaTransfer

HemileiaQualifierHierarchy.java — Subtyping Rules

- Defines isSubtypeQualifiers(): Is @Moved ≤ @Owned? Yes. Is @Borrowed ≤ @MutBorrowed? No.
- Defines leastUpperBound() and greatestLowerBound() for merge points
- Key insight: @Borrowed and @MutBorrowed are incomparable — this enforces Rust's rule that you can't have both simultaneously.

HemileiaStore.java — Dataflow State

- Extends CFAbstractStore to track ownership-specific state:
    - Moved set: Which variables have been moved away
    - Active borrows map: Owner → set of borrowing variables
    - Borrow sources map: Borrower → owner
    - Mutable borrow flags: Which borrows are mutable
- leastUpperBound() merges stores at control flow joins: a variable is moved if moved in any branch; a borrow survives only if active in all branches.

HemileiaTransfer.java — State Transitions

- Defines what happens to the store at each statement:
    - Assignment x = y where both are @Owned: Mark y as moved
    - Borrow @Borrowed x = y: Record borrow relationship without moving
    - Method call consume(x) where parameter is @Owned: Mark x as moved
- This is where the "transfer function" from dataflow analysis is implemented.

HemileiaValue.java — Abstract Value

- Simple wrapper around CFAbstractValue
- Represents the type+annotations of a variable at a specific program point
- Tracks whether something is @Owned, @Borrowed, @MutBorrowed, or @Moved

HemileiaAnalysis.java — Analysis Orchestrator

- Extends CFAbstractAnalysis
- Glues together store, value, and transfer function
- Factory methods: createEmptyStore(), createCopiedStore(), createAbstractValue()
- Skips primitives and void (they don't have ownership semantics)

HemileiaVisitor.java — Error Reporter

- Walks the AST and reports errors using info from the store:
    - visitIdentifier(): Checks use-after-move
    - visitVariable() / visitAssignment(): Checks borrow conflicts
    - visitMethodInvocation(): Checks arguments for use-after-move
- Error keys map to messages.properties for human-readable messages

messages.properties — Error Messages

use.after.move=use of moved value: '%s' was moved and is no longer valid                                                                                                                                                          
borrow.conflict=cannot borrow '%s': conflicting borrow exists                                                                                                                                                                     
multiple.mut.borrow=cannot mutably borrow '%s': already mutably borrowed
                                                                                                                                                                                                                                    
---                                                                                                                                                                                                                               
3. Test Suite Structure

Unit Tests (src/test/java/name/mateusborges/)

Simple JUnit 5 tests demonstrating the API:                                                                                                                                                                                       
┌────────────────────────────┬──────────────────────────────────────────────────────────┐                                                                                                                                         
│            File            │                         Purpose                          │                                                                                                                                         
├────────────────────────────┼──────────────────────────────────────────────────────────┤                                                                                                                                         
│ OwnershipTransferTest.java │ Tests move semantics: transfer, pass to function, return │                                                                                                                                         
├────────────────────────────┼──────────────────────────────────────────────────────────┤                                                                                                                                         
│ BorrowingRulesTest.java    │ Tests borrowing: multiple immutable, exclusive mutable   │                                                                                                                                         
└────────────────────────────┴──────────────────────────────────────────────────────────┘                                                                                                                                         
Run with: ./mvnw test -Dtest=OwnershipTransferTest

Framework Tests (src/test/java/name/mateusborges/checker/)

HemileiaCheckerTest.java extends CheckerFrameworkPerDirectoryTest. It runs the checker on test files and verifies expected errors using inline annotations:

// In tests/useaftermove/UseAfterMoveInvalid.java                                                                                                                                                                                 
@Owned String s2 = s1;                                                                                                                                                                                                            
// :: error: (use.after.move)                                                                                                                                                                                                     
System.out.println(s1);  // ERROR: s1 was moved

The // :: error: (use.after.move) comment tells the test framework to expect that error on the next line.

Test Resources (tests/)

tests/                                                                                                                                                                                                                            
├── useaftermove/                                                                                                                                                                                                                 
│   ├── UseAfterMoveInvalid.java   # Should report use.after.move errors                                                                                                                                                          
│   └── UseAfterMoveValid.java     # Should compile cleanly                                                                                                                                                                       
├── borrowconflict/                                                                                                                                                                                                               
│   ├── BorrowConflictInvalid.java # Should report borrow.conflict/multiple.mut.borrow                                                                                                                                            
│   └── BorrowConflictValid.java   # Should compile cleanly                                                                                                                                                                       
└── validusage/                                                                                                                                                                                                                   
└── ValidOwnership.java        # Comprehensive valid patterns

Running Tests

# Run all tests
./mvnw test

# Run only framework checker tests
./mvnw test -Dtest=HemileiaCheckerTest

# Run only unit tests
./mvnw test -Dtest=OwnershipTransferTest,BorrowingRulesTest

The framework tests verify both positive cases (errors are caught) and negative cases (valid code doesn't trigger false positives). 