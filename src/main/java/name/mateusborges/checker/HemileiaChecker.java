package name.mateusborges.checker;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;

/**
 * The main entry point for the Hemileia type checker.
 *
 * <p>Hemileia implements Rust-style ownership and borrowing semantics for Java.
 * It enforces the following rules at compile time:
 * <ul>
 *   <li>Each value has exactly one owner at a time</li>
 *   <li>When ownership is transferred (moved), the original variable becomes invalid</li>
 *   <li>You can have either one mutable borrow OR any number of immutable borrows</li>
 *   <li>Borrows must not outlive the owner</li>
 * </ul>
 *
 * <p>Usage: Run javac with the processor flag:
 * <pre>
 * javac -processor name.mateusborges.checker.HemileiaChecker MyFile.java
 * </pre>
 */
@StubFiles("jdk.astub")
@RelevantJavaTypes(Object.class)
public class HemileiaChecker extends BaseTypeChecker {

    @Override
    public boolean shouldResolveReflection() {
        return false;
    }
}
