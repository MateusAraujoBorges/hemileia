package name.mateusborges.checker;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * The abstract value for the Hemileia ownership type system.
 *
 * <p>This value tracks the ownership state of a variable, which can be one of:
 * <ul>
 *   <li>{@code @Owned} - the variable owns its value</li>
 *   <li>{@code @Borrowed} - the variable is an immutable borrow</li>
 *   <li>{@code @MutBorrowed} - the variable is a mutable borrow</li>
 *   <li>{@code @Moved} - the variable's value has been moved away</li>
 * </ul>
 */
public class HemileiaValue extends CFAbstractValue<HemileiaValue> {

    public HemileiaValue(
            CFAbstractAnalysis<HemileiaValue, HemileiaStore, HemileiaTransfer> analysis,
            AnnotationMirrorSet annotations,
            javax.lang.model.type.TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }

    /**
     * Returns the set of annotations for this value.
     *
     * @return the annotation set
     */
    public Set<AnnotationMirror> getAnnotationSet() {
        return getAnnotations();
    }
}
