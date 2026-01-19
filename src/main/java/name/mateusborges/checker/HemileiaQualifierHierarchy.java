package name.mateusborges.checker;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.Moved;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

/**
 * The qualifier hierarchy for the Hemileia ownership type system.
 *
 * <p>The hierarchy is a linear chain based on capability inclusion:
 * <pre>
 *     @Borrowed (top - read-only capability)
 *         ↑
 *     @MutBorrowed (read/write capability)
 *         ↑
 *     @Owned (all capabilities: read/write/move)
 *         ↑
 *     @Moved (bottom - no capabilities)
 * </pre>
 *
 * <p>Key relationships:
 * <ul>
 *   <li>{@code @Owned} is a subtype of {@code @MutBorrowed} - owned values can be mutably borrowed</li>
 *   <li>{@code @MutBorrowed} is a subtype of {@code @Borrowed} - read/write access includes read-only</li>
 *   <li>{@code @Moved} is the bottom type - moved values cannot be used</li>
 * </ul>
 *
 * <p>This matches Rust's coercion semantics where {@code &mut T} can coerce to {@code &T}.
 */
public class HemileiaQualifierHierarchy extends ElementQualifierHierarchy {

    private final AnnotationMirror OWNED;
    private final AnnotationMirror BORROWED;
    private final AnnotationMirror MUT_BORROWED;
    private final AnnotationMirror MOVED;

    public HemileiaQualifierHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses,
            Elements elements,
            HemileiaAnnotatedTypeFactory factory) {
        super(qualifierClasses, elements, factory);

        OWNED = AnnotationBuilder.fromClass(elements, Owned.class);
        BORROWED = AnnotationBuilder.fromClass(elements, Borrowed.class);
        MUT_BORROWED = AnnotationBuilder.fromClass(elements, MutBorrowed.class);
        MOVED = AnnotationBuilder.fromClass(elements, Moved.class);
    }

    @Override
    public boolean isSubtypeQualifiers(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        // Same annotation is always a subtype of itself
        if (AnnotationUtils.areSame(subAnno, superAnno)) {
            return true;
        }

        // @Moved is the bottom - @Moved is a subtype of everything
        if (AnnotationUtils.areSame(subAnno, MOVED)) {
            return true;
        }

        // @MutBorrowed <: @Borrowed (read/write includes read-only)
        if (AnnotationUtils.areSame(subAnno, MUT_BORROWED) && AnnotationUtils.areSame(superAnno, BORROWED)) {
            return true;
        }

        // @Owned <: @MutBorrowed and @Owned <: @Borrowed (owned values can be borrowed)
        if (AnnotationUtils.areSame(subAnno, OWNED)) {
            return AnnotationUtils.areSame(superAnno, BORROWED)
                    || AnnotationUtils.areSame(superAnno, MUT_BORROWED);
        }

        // Nothing else is a subtype relationship
        return false;
    }

    @Override
    public AnnotationMirror leastUpperBoundQualifiers(AnnotationMirror a1, AnnotationMirror a2) {
        if (AnnotationUtils.areSame(a1, a2)) {
            return a1;
        }
        if (isSubtypeQualifiers(a1, a2)) {
            return a2;
        }
        if (isSubtypeQualifiers(a2, a1)) {
            return a1;
        }
        // Linear hierarchy - should not reach here, but @Borrowed is the top
        return BORROWED;
    }

    @Override
    public AnnotationMirror greatestLowerBoundQualifiers(AnnotationMirror a1, AnnotationMirror a2) {
        if (AnnotationUtils.areSame(a1, a2)) {
            return a1;
        }
        if (isSubtypeQualifiers(a1, a2)) {
            return a1;
        }
        if (isSubtypeQualifiers(a2, a1)) {
            return a2;
        }
        // Linear hierarchy - should not reach here, but @Moved is the bottom
        return MOVED;
    }
}
