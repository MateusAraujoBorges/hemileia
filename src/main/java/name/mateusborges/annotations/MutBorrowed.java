package name.mateusborges.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Marks a mutable/exclusive borrow of a value.
 * Equivalent to Rust's mutable reference {@code &mut T}.
 *
 * <p>A {@code @MutBorrowed} reference allows mutable access to a value
 * without taking ownership. Only one mutable borrow can exist at a time,
 * and it cannot coexist with any immutable borrows.
 *
 * <p>Example:
 * <pre>
 * &#64;Owned StringBuilder s = new StringBuilder("hello");
 * &#64;MutBorrowed StringBuilder r = s;  // mutable borrow - exclusive
 * r.append(" world");
 * // Cannot have other borrows while r exists
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Owned.class)
public @interface MutBorrowed {
}
