import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.Owned;

/**
 * Tests that verify valid usage does not trigger false positives.
 */
public class UseAfterMoveValid {

    /**
     * Borrowing does not move - original should still be usable.
     */
    void testNoErrorOnBorrow() {
        @Owned String owner = "hello";
        @Borrowed String borrow = owner;  // Borrow, not move
        System.out.println(owner);  // OK: borrowing doesn't invalidate owner
        System.out.println(borrow);
    }

    /**
     * Reassignment after move clears the moved state.
     */
    void testReassignmentClearsMoved() {
        @Owned String a = "hello";
        @Owned String b = a;  // Move
        a = "world";  // Reassign - 'a' is valid again
        System.out.println(a);  // OK: a has been reassigned
    }

    /**
     * Using the new owner after a move is valid.
     */
    void testUseNewOwner() {
        @Owned String a = "hello";
        @Owned String b = a;  // Move ownership to b
        System.out.println(b);  // OK: b is the owner now
    }

    /**
     * Ownership transfer to function, new value returned.
     */
    void testOwnershipReturnFromFunction() {
        @Owned String a = createOwned();  // a owns the returned value
        System.out.println(a);  // OK
    }

    @Owned String createOwned() {
        return "new value";
    }

    /**
     * Multiple borrows without move.
     */
    void testMultipleBorrowsNoMove() {
        @Owned String owner = "hello";
        @Borrowed String b1 = owner;
        @Borrowed String b2 = owner;
        System.out.println(owner);  // OK: owner still valid
        System.out.println(b1);
        System.out.println(b2);
    }
}
