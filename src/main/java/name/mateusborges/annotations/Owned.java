package name.mateusborges.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Marks exclusive ownership of a value.
 * Equivalent to Rust's owned type {@code T}.
 *
 * <p>A value with {@code @Owned} has exactly one owner at a time.
 * When ownership is transferred (assigned to another variable or passed
 * to a function taking ownership), the original variable becomes invalid.
 *
 * <p>Example:
 * <pre>
 * &#64;Owned String s1 = "hello";
 * &#64;Owned String s2 = s1;  // ownership moves to s2
 * // s1 is now invalid
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface Owned {
}
