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
 * <p>The hierarchy is designed to match Rust's implicit borrowing semantics:
 * <pre>
 *  @Borrowed    @MutBorrowed (tops)
 *         \      /
 *          @Owned
 *             |
 *          @Moved (bottom)
 * </pre>
 *
 * <p>Key relationships:
 * <ul>
 *   <li>{@code @Owned} is a subtype of both {@code @Borrowed} and {@code @MutBorrowed} -
 *       owned values can be implicitly borrowed</li>
 *   <li>{@code @Borrowed} and {@code @MutBorrowed} are incomparable - you cannot
 *       convert between them (enforces exclusivity)</li>
 *   <li>{@code @Moved} is the bottom type - moved values cannot be used</li>
 * </ul>
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

        // @Borrowed and @MutBorrowed are incomparable with each other
        if (AnnotationUtils.areSame(subAnno, BORROWED) && AnnotationUtils.areSame(superAnno, MUT_BORROWED)) {
            return false;
        }
        if (AnnotationUtils.areSame(subAnno, MUT_BORROWED) && AnnotationUtils.areSame(superAnno, BORROWED)) {
            return false;
        }

        // @Owned is a subtype of both @Borrowed and @MutBorrowed
        // This allows owned values to be implicitly borrowed
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
        // For incomparable types (@Borrowed and @MutBorrowed), there is no single LUB
        // We return @Borrowed as a conservative choice (read-only access)
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
        // For incomparable types (@Borrowed and @MutBorrowed), the GLB is @Owned
        // (the greatest type that is a subtype of both)
        return OWNED;
    }
}
