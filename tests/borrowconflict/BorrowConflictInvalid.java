import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

/**
 * Tests that verify borrow conflict errors are correctly detected.
 */
public class BorrowConflictInvalid {

    /**
     * Cannot create mutable borrow while immutable borrow exists.
     */
    void testMutBorrowWhileImmutableExists() {
        @Owned StringBuilder owner = new StringBuilder("hello");
        @Borrowed StringBuilder immBorrow = owner;  // immutable borrow
        // :: error: (borrow.conflict)
        @MutBorrowed StringBuilder mutBorrow = owner;  // ERROR: conflict with immutable
    }

    /**
     * Cannot create immutable borrow while mutable borrow exists.
     */
    void testImmutableBorrowWhileMutExists() {
        @Owned StringBuilder owner = new StringBuilder("hello");
        @MutBorrowed StringBuilder mutBorrow = owner;  // mutable borrow
        // :: error: (borrow.conflict)
        @Borrowed StringBuilder immBorrow = owner;  // ERROR: conflict with mutable
    }

    /**
     * Cannot create multiple mutable borrows.
     */
    void testMultipleMutBorrows() {
        @Owned StringBuilder owner = new StringBuilder("hello");
        @MutBorrowed StringBuilder mut1 = owner;  // first mutable borrow
        // :: error: (multiple.mut.borrow)
        @MutBorrowed StringBuilder mut2 = owner;  // ERROR: second mutable borrow
    }

    /**
     * Mutable borrow conflicts with multiple immutable borrows.
     */
    void testMutBorrowAfterMultipleImmutable() {
        @Owned StringBuilder owner = new StringBuilder("hello");
        @Borrowed StringBuilder imm1 = owner;
        @Borrowed StringBuilder imm2 = owner;  // multiple immutable OK
        // :: error: (borrow.conflict)
        @MutBorrowed StringBuilder mut = owner;  // ERROR: conflicts with immutable borrows
    }
}
