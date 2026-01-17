package name.mateusborges.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Marks a value that has been moved and is no longer valid.
 * This is the bottom type in the ownership hierarchy.
 *
 * <p>A value becomes {@code @Moved} after its ownership has been transferred
 * to another variable. Any use of a {@code @Moved} value is a compile-time error.
 *
 * <p>This annotation is applied automatically by the dataflow analysis and
 * should not be used directly by programmers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Borrowed.class, MutBorrowed.class})
public @interface Moved {
}
