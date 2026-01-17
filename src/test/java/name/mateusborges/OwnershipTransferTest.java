package name.mateusborges;

import name.mateusborges.annotations.Owned;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates Rust-style ownership transfer (move semantics) in Java.
 *
 * <p>In Rust:
 * <pre>
 * fn main() {
 *     let s1 = String::from("hello");
 *     let s2 = s1;           // ownership moves to s2
 *     // println!("{}", s1); // ERROR: s1 is no longer valid
 *     println!("{}", s2);    // OK
 * }
 * </pre>
 *
 * <p>The {@code @Owned} annotation marks exclusive ownership. When a value
 * is assigned to another variable, ownership transfers and the original
 * variable becomes invalid.
 */
public class OwnershipTransferTest {

    /**
     * Demonstrates basic ownership transfer between variables.
     *
     * <p>When s1 is assigned to s2, ownership moves to s2.
     * After this point, s1 would be invalid (enforced by the checker).
     */
    @Test
    public void testOwnershipTransfer() {
        @Owned String s1 = "hello";
        @Owned String s2 = s1;  // ownership transfers from s1 to s2
        // At this point, s1 is no longer valid - using it would be an error
        // The checker should flag any use of s1 after this line
        System.out.println(s2);  // OK - s2 now owns the value
    }

    /**
     * Demonstrates ownership transfer when passing to a function.
     *
     * <p>When a value is passed to a function that takes ownership,
     * the caller loses ownership and cannot use the value afterward.
     */
    @Test
    public void testOwnershipTransferToFunction() {
        @Owned String message = "hello world";
        takeOwnership(message);  // ownership transfers to the function
        // message is now invalid - using it would be an error
    }

    /**
     * Takes ownership of a value. The value is "dropped" when this function ends.
     *
     * <p>In Rust, this would be:
     * <pre>
     * fn take_ownership(value: String) {
     *     println!("{}", value);
     * } // value is dropped here
     * </pre>
     *
     * @param value the owned value - caller transfers ownership
     */
    private void takeOwnership(@Owned String value) {
        System.out.println(value);
        // value is dropped at end of scope
    }

    /**
     * Demonstrates that ownership can be returned from a function.
     *
     * <p>In Rust:
     * <pre>
     * fn give_ownership() -> String {
     *     String::from("hello")
     * }
     * </pre>
     */
    @Test
    public void testOwnershipReturn() {
        @Owned String s = createOwned();  // ownership transfers to s
        System.out.println(s);  // OK - s owns the value
    }

    /**
     * Creates and returns an owned value, transferring ownership to the caller.
     *
     * @return a new owned String
     */
    private @Owned String createOwned() {
        @Owned String s = "created value";
        return s;  // ownership transfers to caller
    }
}
