import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

class BorrowTests {

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

    void mutableBorrowShouldAllowValuesToBeUsedMultipleTimes() {
        @Owned StringBuilder v1 = new StringBuilder();
        @MutBorrowed StringBuilder v2 = v1;
        v2.append(42);
        v2.append(43);
    }

    void mutableBorrowShouldAllowValuesToBeReusedAfterBorrowEnds() {
        @Owned StringBuilder v1 = new StringBuilder();
        @MutBorrowed StringBuilder v2 = v1;
        v2.append(42);
        System.out.println(v1.toString());
    }

    // @formatter:off
    /**
     * fn foo(v1: &mut Vec<i32>) -> i32 {
     *     // do stuff with v1 and v2
     *     v1.push(1);
     *     v1.push(2);
     *     // return the answer
     *     42
     * }
     *
     * fn main() {
     *     let mut v1 = vec![1, 2, 3];
     *
     *     let answer = foo(&mut v1);
     *     println!("{:?} {answer}", v1);
     * }
     */
    // @formatter:on

    void chainOfCustodyBorrowsWithNonLexicalLifetimes() {
        @Owned StringBuilder v1 = new StringBuilder();
        @MutBorrowed StringBuilder v2 = v1;
        @MutBorrowed StringBuilder v3 = v2;

        v3.append(3); // v3 borrow ends here
        v2.append(2); // v2 borrow ends here
        v1.append(1);
    }


    /**
     * fn main() {
     *     let mut v1 = vec![1, 2, 3];
     *     let mut v2 = &mut v1;
     *     let mut v3 = &mut v2;
     *
     *     v3.push(1);
     *     v2.push(3);
     *     v3.push(1);
     *     v1.push(2);
     *     v3.push(1);
     *
     *     // let answer = foo(&mut v1);
     *     // println!("{:?} {answer}", v1);
     *     println!("{:?}", v1);
     * }
     */

    // Need to implement non-lexical lifetimes
//    void chainOfCustodyBorrowsWithNonLexicalLifetimesPreventMixedDirectionUses() {
//        @Owned StringBuilder v1 = new StringBuilder();
//        @MutBorrowed StringBuilder v2 = v1;
//        @MutBorrowed StringBuilder v3 = v2;
//
//        v3.append(3);
//        // :: error: (multiple.mut.borrow)
//        v2.append(2);
//        v3.append(3);
//        // :: error: (multiple.mut.borrow)
//        v1.append(1);
//        v3.append(3);
//    }
}