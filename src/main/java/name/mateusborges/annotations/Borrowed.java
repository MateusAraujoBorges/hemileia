package name.mateusborges.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Marks an immutable/shared borrow of a value.
 * Equivalent to Rust's immutable reference {@code &T}.
 *
 * <p>A {@code @Borrowed} reference allows read-only access to a value
 * without taking ownership. Multiple immutable borrows can coexist,
 * but cannot exist simultaneously with a mutable borrow.
 *
 * <p>Example:
 * <pre>
 * &#64;Owned String s = "hello";
 * &#64;Borrowed String r1 = s;  // immutable borrow
 * &#64;Borrowed String r2 = s;  // another immutable borrow - OK
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Owned.class)
public @interface Borrowed {
}
