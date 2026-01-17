package name.mateusborges.checker;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

import name.mateusborges.annotations.Borrowed;
import name.mateusborges.annotations.Moved;
import name.mateusborges.annotations.MutBorrowed;
import name.mateusborges.annotations.Owned;

/**
 * The annotated type factory for the Hemileia ownership type system.
 *
 * <p>This factory provides:
 * <ul>
 *   <li>Flow-sensitive type refinement (enabled by default)</li>
 *   <li>The qualifier hierarchy: {@code @Owned} > {@code @Borrowed}, {@code @MutBorrowed} > {@code @Moved}</li>
 *   <li>Helper methods for checking ownership annotations</li>
 * </ul>
 */
public class HemileiaAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<HemileiaValue, HemileiaStore, HemileiaTransfer, HemileiaAnalysis> {

    /** Cached annotation mirrors for efficient lookup */
    private final AnnotationMirror OWNED;
    private final AnnotationMirror BORROWED;
    private final AnnotationMirror MUT_BORROWED;
    private final AnnotationMirror MOVED;

    @SuppressWarnings("this-escape")
    public HemileiaAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        // Create cached annotation mirrors
        OWNED = AnnotationBuilder.fromClass(elements, Owned.class);
        BORROWED = AnnotationBuilder.fromClass(elements, Borrowed.class);
        MUT_BORROWED = AnnotationBuilder.fromClass(elements, MutBorrowed.class);
        MOVED = AnnotationBuilder.fromClass(elements, Moved.class);

        // Initialize the factory after setting up annotations
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Set.of(Owned.class, Borrowed.class, MutBorrowed.class, Moved.class);
    }

    @Override
    public HemileiaTransfer createFlowTransferFunction(
            org.checkerframework.framework.flow.CFAbstractAnalysis<HemileiaValue, HemileiaStore, HemileiaTransfer> analysis) {
        return new HemileiaTransfer((HemileiaAnalysis) analysis);
    }

    @Override
    protected HemileiaAnalysis createFlowAnalysis() {
        return new HemileiaAnalysis(checker, this);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new HemileiaQualifierHierarchy(this.getSupportedTypeQualifiers(), elements, this);
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        // Default to @Owned for all locations
        defs.addCheckedCodeDefault(OWNED, org.checkerframework.framework.qual.TypeUseLocation.ALL);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator());
    }

    // Helper methods for checking annotations

    /**
     * Checks if a type has the {@code @Owned} annotation.
     */
    public boolean hasOwned(AnnotatedTypeMirror type) {
        return type.hasPrimaryAnnotation(OWNED);
    }

    /**
     * Checks if a type has the {@code @Borrowed} annotation.
     */
    public boolean hasBorrowed(AnnotatedTypeMirror type) {
        return type.hasPrimaryAnnotation(BORROWED);
    }

    /**
     * Checks if a type has the {@code @MutBorrowed} annotation.
     */
    public boolean hasMutBorrowed(AnnotatedTypeMirror type) {
        return type.hasPrimaryAnnotation(MUT_BORROWED);
    }

    /**
     * Checks if a type has the {@code @Moved} annotation.
     */
    public boolean hasMoved(AnnotatedTypeMirror type) {
        return type.hasPrimaryAnnotation(MOVED);
    }

    /**
     * Gets the {@code @Owned} annotation mirror.
     */
    public AnnotationMirror getOwnedAnnotation() {
        return OWNED;
    }

    /**
     * Gets the {@code @Borrowed} annotation mirror.
     */
    public AnnotationMirror getBorrowedAnnotation() {
        return BORROWED;
    }

    /**
     * Gets the {@code @MutBorrowed} annotation mirror.
     */
    public AnnotationMirror getMutBorrowedAnnotation() {
        return MUT_BORROWED;
    }

    /**
     * Gets the {@code @Moved} annotation mirror.
     */
    public AnnotationMirror getMovedAnnotation() {
        return MOVED;
    }
}
