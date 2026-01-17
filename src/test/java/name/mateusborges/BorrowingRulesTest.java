package name.mateusborges;

import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates Rust-style borrowing rules in Java.
 *
 * <p>In Rust:
 * <pre>
 * fn main() {
 *     let mut s = String::from("hello");
 *
 *     let r1 = &s;     // immutable borrow
 *     let r2 = &s;     // multiple immutable borrows OK
 *     println!("{} {}", r1, r2);
 *
 *     let r3 = &mut s; // mutable borrow (after immutable borrows end)
 *     r3.push_str(" world");
 * }
 * </pre>
 *
 * <p>Borrowing rules:
 * <ul>
 *   <li>You can have either one mutable borrow OR any number of immutable borrows</li>
 *   <li>You cannot have both mutable and immutable borrows at the same time</li>
 *   <li>Borrows must not outlive the owner</li>
 * </ul>
 */
public class BorrowingRulesTest {

    /**
     * Demonstrates that multiple immutable borrows are allowed simultaneously.
     *
     * <p>In Rust:
     * <pre>
     * let s = String::from("hello");
     * let r1 = &s;  // OK
     * let r2 = &s;  // OK - multiple immutable borrows allowed
     * println!("{} {}", r1, r2);
     * </pre>
     */
    @Test
    public void testMultipleImmutableBorrows() {
        @Owned String s = "hello";
        @Borrowed String r1 = s;  // immutable borrow
        @Borrowed String r2 = s;  // another immutable borrow - OK
        // Both r1 and r2 can be used simultaneously
        System.out.println(r1 + " " + r2);
    }

    /**
     * Demonstrates that mutable borrows are exclusive.
     *
     * <p>In Rust:
     * <pre>
     * let mut s = String::from("hello");
     * let r = &mut s;  // mutable borrow
     * r.push_str(" world");
     * // Cannot create another borrow while r exists
     * </pre>
     */
    @Test
    public void testMutableBorrowIsExclusive() {
        @Owned StringBuilder s = new StringBuilder("hello");
        @MutBorrowed StringBuilder r = s;  // mutable borrow - exclusive access
        // No other borrows (mutable or immutable) can exist while r is active
        r.append(" world");
        System.out.println(r);  // "hello world"
    }

    /**
     * Demonstrates passing an immutable borrow to a function.
     *
     * <p>The function can read the value but the caller retains ownership.
     *
     * <p>In Rust:
     * <pre>
     * fn read_value(s: &String) {
     *     println!("{}", s);
     * }
     *
     * let s = String::from("hello");
     * read_value(&s);  // borrow
     * println!("{}", s);  // s is still valid
     * </pre>
     */
    @Test
    public void testBorrowToFunction() {
        @Owned String s = "hello";
        readValue(s);  // pass an immutable borrow
        // s is still valid - we retain ownership
        System.out.println(s);  // OK
    }

    /**
     * Reads a borrowed value. Does not take ownership.
     *
     * @param value an immutable borrow - caller retains ownership
     */
    private void readValue(@Borrowed String value) {
        System.out.println("Reading: " + value);
        // value is NOT dropped - it's just a borrow
    }

    /**
     * Demonstrates passing a mutable borrow to a function.
     *
     * <p>The function can modify the value but the caller retains ownership.
     *
     * <p>In Rust:
     * <pre>
     * fn modify_value(s: &mut String) {
     *     s.push_str(" world");
     * }
     *
     * let mut s = String::from("hello");
     * modify_value(&mut s);  // mutable borrow
     * println!("{}", s);  // "hello world"
     * </pre>
     */
    @Test
    public void testMutableBorrowToFunction() {
        @Owned StringBuilder s = new StringBuilder("hello");
        modifyValue(s);  // pass a mutable borrow
        // s is still valid and reflects the modification
        System.out.println(s);  // "hello world"
    }

    /**
     * Modifies a mutably borrowed value. Does not take ownership.
     *
     * @param value a mutable borrow - caller retains ownership
     */
    private void modifyValue(@MutBorrowed StringBuilder value) {
        value.append(" world");
        // value is NOT dropped - it's just a borrow
    }
}
