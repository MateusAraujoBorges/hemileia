import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.Owned;

/**
 * Tests that verify use-after-move errors are correctly detected.
 */
public class UseAfterMoveInvalid {

    /**
     * Simple use-after-move: assign to another owned variable, then use original.
     */
    void testSimpleUseAfterMove() {
        @Owned String a = "hello";
        @Owned String b = a;  // Move: 'a' is now invalid
        // :: error: (use.after.move)
        System.out.println(a);  // ERROR: use after move
    }

    /**
     * Use-after-move in method call: pass owned value to method taking ownership.
     */
    void testUseAfterMoveInMethodCall() {
        @Owned String value = "hello";
        takeOwnership(value);  // ownership transferred
        // :: error: (use.after.move)
        System.out.println(value);  // ERROR: use after move
    }

    /**
     * Method that takes ownership of its parameter.
     */
    void takeOwnership(@Owned String s) {
        System.out.println(s);
    }

    /**
     * Use-after-move with multiple uses.
     */
    void testMultipleUsesAfterMove() {
        @Owned String a = "hello";
        @Owned String b = a;  // Move
        // :: error: (use.after.move)
        String len = a;  // ERROR
        // :: error: (use.after.move)
        System.out.println(a);  // ERROR
    }
}
