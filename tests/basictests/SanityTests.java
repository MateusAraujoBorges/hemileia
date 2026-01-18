import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

public class SimpleBorrowTests {

    /**
     * fn main() {
     *     let x = 1;
     *     let y = x;
     *     println!("{y}");
     *     println!("{x}");
     * }
     */

    void primitiveBorrows() {
        @Owned x = 1;
        @Owned y = x;
        System.out.println(y);
        System.out.println(x);
    }
}