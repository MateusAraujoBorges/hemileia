package name.mateusborges.checker;

import javax.lang.model.type.TypeMirror;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * The dataflow analysis for the Hemileia ownership type system.
 *
 * <p>This analysis extends the standard Checker Framework dataflow analysis
 * with ownership-specific tracking for moved variables and active borrows.
 */
public class HemileiaAnalysis
        extends CFAbstractAnalysis<HemileiaValue, HemileiaStore, HemileiaTransfer> {

    public HemileiaAnalysis(
            BaseTypeChecker checker,
            GenericAnnotatedTypeFactory<HemileiaValue, HemileiaStore, HemileiaTransfer, HemileiaAnalysis> factory) {
        super(checker, factory, factory.getQualifierHierarchy().numberOfIterationsBeforeWidening());
    }

    @Override
    public HemileiaStore createEmptyStore(boolean sequentialSemantics) {
        return new HemileiaStore(this, sequentialSemantics);
    }

    @Override
    public HemileiaStore createCopiedStore(HemileiaStore store) {
        return new HemileiaStore(store);
    }

    @Override
    public HemileiaValue createAbstractValue(
            AnnotationMirrorSet annotations, TypeMirror underlyingType) {
        if (underlyingType == null) {
            return null;
        }
        javax.lang.model.type.TypeKind kind = underlyingType.getKind();
        // Don't track primitive types, void, or executable types
        if (kind.isPrimitive() || kind == javax.lang.model.type.TypeKind.VOID
                || kind == javax.lang.model.type.TypeKind.EXECUTABLE
                || kind == javax.lang.model.type.TypeKind.NONE
                || kind == javax.lang.model.type.TypeKind.ERROR) {
            return null;
        }
        return new HemileiaValue(this, annotations, underlyingType);
    }
}
