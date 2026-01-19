import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

public class BorrowTests {

    // @formatter:off
    /**
     * fn foo(v1: &Vec<i32>, v2: &Vec<i32>) -> i32 {
     *     // do stuff with v1 and v2
     *     println!("{:?} {:?}", v1, v2);
     *     // return the answer
     *     42
     * }
     * fn main() {
     *     let v1 = vec![1, 2, 3];
     *     let v2 = vec![1, 2, 3];
     *
     *     let answer = foo(&v1, &v2);
     *     println!("{:?} {:?} {answer}", v1, v2);
     * }
     */
    // @formatter:on

    int foo(@Borrowed StringBuilder v1, @Borrowed StringBuilder v2) {
        // Do stuff
        System.out.println(v1.toString() + " " + v2.toString());
        return v1.length() + v2.length();
    }

    void borrowingShouldAllowValuesToBeReusedAfterBorrowEnds() {
        @Owned StringBuilder v1 = new StringBuilder();
        @Owned StringBuilder v2 = new StringBuilder();

        int answer = foo(v1, v2);
        System.out.println(v1.toString() + v2.toString() + answer);
    }

}