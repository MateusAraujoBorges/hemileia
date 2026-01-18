import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

public class SanityTests {

    /**
     * fn main() {
     *     let x = 1;
     *     let y = x;
     *     println!("{y}");
     *     println!("{x}");
     * }
     */

    void localPrimitivesResultInCopies() {
        // :: error: (anno.on.irrelevant)
        @Owned int x = 1;
        // :: error: (anno.on.irrelevant)
        @Borrowed int y = x;
        System.out.println(y);
        System.out.println(x);
    }

    /**
     * fn foo(v1: Vec<i32>, v2: Vec<i32>) -> (Vec<i32>, Vec<i32>, i32) {
     *     // do stuff with v1 and v2
     *     println!("{:?}, {:?}", v1, v2);
     *     // hand back ownership, and the result of our function
     *     (v1, v2, 42)
     * }
     *
     * fn main() {
     *     let v1 = vec![1, 2, 3];
     *     let v2 = vec![1, 2, 3];
     *
     *     let (v1, v2, answer) = foo(v1, v2);
     *     println!("{:?}, {:?}, {answer}", v1, v2);
     * }
     */

    record FooResult(@Owned StringBuilder v1, @Owned StringBuilder v2, int answer) {}

    @Owned FooResult foo(@Owned StringBuilder v1, @Owned StringBuilder v2) {
        // do stuff with v1 and v2
        System.out.println(v1.toString() + ", " + v2.toString());
        // hand back ownership, and the result of our function
        return new FooResult(v1, v2, 42);
    }

    void ownershipTransferAndReturn() {
        @Owned StringBuilder v1 = new StringBuilder("1, 2, 3");
        @Owned StringBuilder v2 = new StringBuilder("1, 2, 3");

        @Owned FooResult result = foo(v1, v2);
        System.out.println(result.v1().toString() + ", " + result.v2().toString() + ", " + result.answer());
    }

    /**
     * let v = vec![1, 2, 3];
     *
     * let v2 = v;
     *
     * println!("v[0] is: {}", v[0]);
     */

    void onlyOneBindingAtATime() {
        @Owned StringBuilder v1 = new StringBuilder("1, 2, 3");
        @Owned StringBuilder v2 = v1;
        // :: error: (use.after.move)
        System.out.println(v1);
    }


    /**
     * fn foo(v1: Vec<i32>, v2: Vec<i32>) -> (Vec<i32>, Vec<i32>, i32) {
     *     // do stuff with v1 and v2
     *     println!("{:?}, {:?}", v1, v2);
     *     // hand back ownership, and the result of our function
     *     (v1, v2, 42)
     * }
     *
     * fn main() {
     *     let mut v1 = vec![1, 2, 3];
     *     let mut v2 = vec![1, 2, 3];
     *     let mut answer = 11;
     *
     *     if v1.len() == v2.len() {
     *         let (v1k, v2k, answerk) = foo(v1, v2);
     *         v1 = v1k;
     *         v2 = v2k;
     *         answer = answerk;
     *     }
     *     println!("{:?}, {:?}, {answer}", v1, v2);
     * }
     */

    void ownershipTransferAndReturnWithBranches() {
        @Owned StringBuilder v1 = new StringBuilder("1, 2, 3");
        @Owned StringBuilder v2 = new StringBuilder("1, 2, 3");

        if (v1.length() != v2.length()) {
            @Owned FooResult result = foo(v1, v2);
            v1 = result.v1();
            v2 = result.v2();
        }
        System.out.println(v1 + ", " + v2.toString());
    }

    void ownershipTransferWithoutReturn() {
        @Owned StringBuilder v1 = new StringBuilder("1, 2, 3");
        @Owned StringBuilder v2 = new StringBuilder("1, 2, 3");

        @Owned FooResult result = foo(v1, v2);

        // :: error: (use.after.move)
        System.out.println(v1 + ", " + v2);
    }

    void ownershipTransferWithoutReturnAndMethodCallsInPrintArg() {
        @Owned StringBuilder v1 = new StringBuilder("1, 2, 3");
        @Owned StringBuilder v2 = new StringBuilder("1, 2, 3");

        @Owned FooResult result = foo(v1, v2);

        // :: error: (use.after.move)
        System.out.println(v1 + ", " + v2.toString());
    }

    /**
     * fn bar(v1: Vec<i32>) -> i32 {
     *     if v1.len() > 5 { 1 } else { 2 }
     * }
     *
     * fn main() {
     *     let mut v1 = vec![1, 2, 3];
     *     let mut v2 = vec![1, 2, 3];
     *
     *     println!("{:?}, {:?}, {:?}, ", bar(v1), bar(v1), v2);
     * }
     */

    int bar(@Owned StringBuilder sb) {
        return sb.length() > 5 ? 1 : 2;
    }

    void usingOwnedVariableTwiceShouldResultInErrorEvenIfUseIsTrivial() {
        @Owned StringBuilder v1 = new StringBuilder("1, 2, 3");
        @Owned StringBuilder v2 = new StringBuilder("1, 2, 3");

        // :: error: (use.after.move)
        System.out.println(bar(v1) + ", " + bar(v1) + v2);
    }
}