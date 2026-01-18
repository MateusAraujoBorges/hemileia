package name.mateusborges.checker;

import java.io.File;
import java.util.List;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test harness for the Hemileia ownership checker.
 *
 * <p>This test runs the HemileiaChecker on test source files and verifies
 * that expected errors are reported. Test files use the Checker Framework
 * convention of annotating expected errors with comments:
 *
 * <pre>
 * // :: error: (error.key)
 * </pre>
 *
 * <p>The test will fail if:
 * <ul>
 *   <li>An expected error is not reported</li>
 *   <li>An unexpected error is reported</li>
 * </ul>
 */
public class HemileiaCheckerTest extends CheckerFrameworkPerDirectoryTest {

    public HemileiaCheckerTest(List<File> testFiles) {
        super(
            testFiles,
            HemileiaChecker.class,
            "hemileia",
            "-Anomsgtext"  // Suppress additional message text for cleaner comparison
        );
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {
                "basictests"
//            "useaftermove",
//            "borrowconflict",
//            "validusage"
        };
    }
}
