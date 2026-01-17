import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

/**
 * Tests that verify valid borrowing patterns do not trigger errors.
 */
public class BorrowConflictValid {

    /**
     * Multiple immutable borrows are allowed simultaneously.
     */
    void testMultipleImmutableBorrowsAllowed() {
        @Owned String owner = "hello";
        @Borrowed String b1 = owner;  // OK
        @Borrowed String b2 = owner;  // OK - multiple immutable allowed
        @Borrowed String b3 = owner;  // OK - still fine
        System.out.println(b1 + b2 + b3);
    }

    /**
     * Single mutable borrow is allowed.
     */
    void testSingleMutBorrow() {
        @Owned StringBuilder owner = new StringBuilder("hello");
        @MutBorrowed StringBuilder mut = owner;  // OK - single mutable borrow
        mut.append(" world");
        System.out.println(mut);
    }

    /**
     * Single immutable borrow is allowed.
     */
    void testSingleImmutableBorrow() {
        @Owned String owner = "hello";
        @Borrowed String borrow = owner;  // OK
        System.out.println(borrow);
    }

    /**
     * Owner is still usable while borrowed (read-only access).
     */
    void testOwnerUsableWhileBorrowed() {
        @Owned String owner = "hello";
        @Borrowed String borrow = owner;
        System.out.println(owner);  // OK - owner still readable
        System.out.println(borrow);
    }
}
