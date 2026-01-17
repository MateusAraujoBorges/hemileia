import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

/**
 * Tests demonstrating valid ownership patterns that should compile without errors.
 */
public class ValidOwnership {

    /**
     * Simple owned declaration and use.
     */
    void testSimpleOwnedDeclaration() {
        @Owned String s = "hello";
        System.out.println(s);  // OK
    }

    /**
     * Valid ownership transfer.
     */
    void testOwnershipTransfer() {
        @Owned String a = "hello";
        @Owned String b = a;  // Move ownership
        System.out.println(b);  // OK - b is the owner now
    }

    /**
     * Borrow and return - owner still valid after borrow ends.
     */
    void testBorrowAndReturn() {
        @Owned String owner = "hello";
        readBorrowed(owner);  // Borrow for function call
        System.out.println(owner);  // OK - owner still valid after borrow ends
    }

    void readBorrowed(@Borrowed String s) {
        System.out.println("Reading: " + s);
    }

    /**
     * Mutable borrow and modify.
     */
    void testMutBorrowAndModify() {
        @Owned StringBuilder owner = new StringBuilder("hello");
        modifyBorrowed(owner);
        System.out.println(owner);  // OK - reflects modification
    }

    void modifyBorrowed(@MutBorrowed StringBuilder s) {
        s.append(" world");
    }

    /**
     * Ownership chain through function calls.
     */
    void testOwnershipChain() {
        @Owned String a = createString();  // Ownership from function
        @Owned String b = transformString(a);  // Transfer through function
        System.out.println(b);  // OK
    }

    @Owned String createString() {
        return "created";
    }

    @Owned String transformString(@Owned String input) {
        return input + " transformed";
    }

    /**
     * Reassigning an owned variable.
     */
    void testReassignment() {
        @Owned String a = "first";
        System.out.println(a);
        a = "second";  // OK - reassignment
        System.out.println(a);
        a = "third";  // OK
        System.out.println(a);
    }

    /**
     * Borrowing from method parameter.
     */
    void testBorrowFromParameter(@Owned String param) {
        @Borrowed String borrow = param;
        System.out.println(borrow);
        System.out.println(param);  // OK - param still valid (borrowing doesn't move)
    }
}
