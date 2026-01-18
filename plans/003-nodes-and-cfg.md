Two questions: 1) What means for a TransferResult to have two stores? Is it related to the way the control flow graph is structured (perhaps in ssa)? 2) If assignments aren't in SSA, how deep an expression goes in the         
right side of an assignment in the CFG, and should we refactor the HemileiaTransfer to handle nested method calls?

# Answer

Research: TransferResult Two Stores & CFG Expression Structure

Question 1: What does it mean for a TransferResult to have two stores?

Answer: Control Flow Paths (NOT SSA)

This is not related to SSA form. It's about path-sensitive dataflow analysis in the Control Flow Graph:                                                                                                                            
┌───────────────────────────┬──────────────────────┬─────────────────────────────────────────┐                                                                                                                                     
│    TransferResult Type    │        Stores        │                Use Case                 │                                                                                                                                     
├───────────────────────────┼──────────────────────┼─────────────────────────────────────────┤                                                                                                                                     
│ RegularTransferResult     │ 1 store              │ Sequential statements that can't branch │                                                                                                                                     
├───────────────────────────┼──────────────────────┼─────────────────────────────────────────┤                                                                                                                                     
│ ConditionalTransferResult │ 2 stores (then/else) │ Branch points in control flow           │                                                                                                                                     
└───────────────────────────┴──────────────────────┴─────────────────────────────────────────┘                                                                                                                                     
Why Method Invocations Return ConditionalTransferResult

Method invocations are modeled as ExceptionBlocks in the CFG because any method can throw:

MethodInvocationNode                                                                                                                                                                                                               
├── Then Store: normal completion path                                                                                                                                                                                         
└── Else Store: exception thrown path

Critical Implementation Detail

- getRegularStore() on a ConditionalTransferResult returns a merged/LUB (least upper bound) of both stores
- This merged store is computed FROM the two stores, not the source of truth
- Modifying only getRegularStore() does NOT propagate changes to either path

This is why we must do:                                                                                                                                                                                                            
if (result.containsTwoStores()) {                                                                                                                                                                                                  
modify(result.getThenStore());   // Normal path                                                                                                                                                                                
modify(result.getElseStore());   // Exception path                                                                                                                                                                             
} else {                                                                                                                                                                                                                           
modify(result.getRegularStore());                                                                                                                                                                                              
}
                                                                                                                                                                                                                                    
---                                                                                                                                                                                                                                
Question 2: CFG Expression Structure & Nested Method Calls

How Expressions Are Represented

Expressions form a tree of Nodes, but the CFG visits them in post-order (children before parents):

System.out.println(bar(v1) + ", " + bar(v2))

Visit order:
1. LocalVariableNode(v1)
2. MethodInvocationNode(bar(v1))     ← v1 marked moved here
3. StringLiteralNode(", ")
4. LocalVariableNode(v2)
5. MethodInvocationNode(bar(v2))     ← v2 marked moved here
6. StringConcatenateNode(...)
7. MethodInvocationNode(println(...))

Does HemileiaTransfer Need Refactoring for Nested Calls?

No, the current approach is correct. Here's why:

1. Only local variables can be moved - method return values are new values
2. Post-order traversal ensures inner calls are processed first
3. The instanceof LocalVariableNode check is intentional

For bar(baz(x)):
- baz(x) is visited first → x is marked moved
- bar(...) is visited second → its argument is a MethodInvocationNode (return value), not a LocalVariableNode
- Return values aren't "owned" variables that can be moved

Test Case Validation

The test usingOwnedVariableTwiceShouldResultInErrorEvenIfUseIsTrivial (line 152):                                                                                                                                                  
System.out.println(bar(v1) + ", " + bar(v1) + v2);

This works correctly because:
1. First bar(v1) → v1 is LocalVariableNode → marked moved
2. Second bar(v1) → v1 is LocalVariableNode → checked, found moved, error reported

 ---                                                                                                                                                                                                                                
Summary                                                                                                                                                                                                                            
┌────────────────────┬─────────────────────────────────────────────────────────────┐                                                                                                                                               
│      Question      │                           Answer                            │                                                                                                                                               
├────────────────────┼─────────────────────────────────────────────────────────────┤                                                                                                                                               
│ Two stores meaning │ Path-sensitive analysis (normal vs exception path), not SSA │                                                                                                                                               
├────────────────────┼─────────────────────────────────────────────────────────────┤                                                                                                                                               
│ Nested expressions │ Post-order traversal handles them automatically             │                                                                                                                                               
├────────────────────┼─────────────────────────────────────────────────────────────┤                                                                                                                                               
│ Refactoring needed │ No - current LocalVariableNode check is correct             │                                                                                                                                               
└────────────────────┴─────────────────────────────────────────────────────────────┘ 