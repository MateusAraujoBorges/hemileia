package name.mateusborges.checker;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transfer function for the Hemileia ownership type system.
 *
 * <p>This class implements the dataflow transfer rules for ownership semantics:
 * <ul>
 *   <li><b>Move semantics:</b> When an {@code @Owned} value is assigned to another
 *       variable, the source is marked as moved.</li>
 *   <li><b>Borrow creation:</b> When a {@code @Borrowed} or {@code @MutBorrowed}
 *       value is created, track the borrow relationship.</li>
 *   <li><b>Borrow invalidation:</b> When a variable is reassigned, its old
 *       borrow (if any) is removed.</li>
 * </ul>
 */
public class HemileiaTransfer
        extends CFAbstractTransfer<HemileiaValue, HemileiaStore, HemileiaTransfer> {

    private static final Logger logger = LoggerFactory.getLogger(HemileiaTransfer.class);

    private final HemileiaAnnotatedTypeFactory atypeFactory;

    public HemileiaTransfer(HemileiaAnalysis analysis) {
        super(analysis);
        this.atypeFactory = (HemileiaAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    @Override
    public TransferResult<HemileiaValue, HemileiaStore> visitAssignment(
            AssignmentNode node, TransferInput<HemileiaValue, HemileiaStore> input) {

        TransferResult<HemileiaValue, HemileiaStore> result = super.visitAssignment(node, input);
        HemileiaStore store = result.getRegularStore();

        Node target = node.getTarget();
        Node expression = node.getExpression();

        // Get the type of the target (LHS)
        AnnotatedTypeMirror targetType = atypeFactory.getAnnotatedType(node.getTarget().getTree());

        // Handle the case where target is a local variable
        if (target instanceof LocalVariableNode targetVar) {
            Element targetElement = targetVar.getElement();

            // If the target previously held a borrow, remove it
            store.removeBorrow(targetElement);

            // If the target was moved, clear that status (it's being reassigned)
            store.clearMoved(targetElement);

            // Now handle the source (RHS)
            if (expression instanceof LocalVariableNode sourceVar) {
                Element sourceElement = sourceVar.getElement();
                AnnotatedTypeMirror sourceType = atypeFactory.getAnnotatedType(sourceVar.getTree());

                // Check if source is owned
                boolean sourceIsOwned = atypeFactory.hasOwned(sourceType);
                boolean targetIsOwned = atypeFactory.hasOwned(targetType);
                boolean targetIsBorrowed = atypeFactory.hasBorrowed(targetType);
                boolean targetIsMutBorrowed = atypeFactory.hasMutBorrowed(targetType);

                if (sourceIsOwned && targetIsOwned) {
                    // Move semantics: @Owned source -> @Owned target
                    // Mark the source as moved
                    store.markMoved(sourceElement);

                    // Update the source's type to @Moved in the store
                    AnnotationMirror movedAnno = atypeFactory.getMovedAnnotation();
                    AnnotatedTypeMirror movedType = sourceType.deepCopy();
                    movedType.replaceAnnotation(movedAnno);
                    HemileiaValue movedValue = analysis.createAbstractValue(
                            new AnnotationMirrorSet(movedAnno),
                            movedType.getUnderlyingType());
                    JavaExpression sourceExpr = new LocalVariable(sourceVar);
                    store.replaceValue(sourceExpr, movedValue);
                } else if (sourceIsOwned && (targetIsBorrowed || targetIsMutBorrowed)) {
                    // Borrow creation: @Owned source -> @Borrowed/@MutBorrowed target
                    store.addBorrow(targetElement, sourceElement, targetIsMutBorrowed);
                }
            }
        }

        return result;
    }

    @Override
    public TransferResult<HemileiaValue, HemileiaStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<HemileiaValue, HemileiaStore> input) {

        TransferResult<HemileiaValue, HemileiaStore> result = super.visitMethodInvocation(node, input);
        HemileiaStore store = result.getRegularStore();

        // Check each argument for ownership transfer
        var methodElement = node.getTarget().getMethod();
        var parameters = methodElement.getParameters();
        var arguments = node.getArguments();

        logger.debug("visitMethodInvocation: {} with {} args", methodElement.getSimpleName(), arguments.size());

        for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
            Node arg = arguments.get(i);

            // Get the parameter's declared type
            AnnotatedTypeMirror paramType = atypeFactory.getAnnotatedType(parameters.get(i));
            boolean paramIsOwned = atypeFactory.hasOwned(paramType);

            logger.debug("  arg[{}]: {} = {}, paramIsOwned={}", i, arg.getClass().getSimpleName(), arg, paramIsOwned);

            if (paramIsOwned && arg instanceof LocalVariableNode argVar) {
                Element argElement = argVar.getElement();
                AnnotatedTypeMirror argType = atypeFactory.getAnnotatedType(argVar.getTree());

                logger.debug("    argType={}, hasOwned={}", argType, atypeFactory.hasOwned(argType));

                if (atypeFactory.hasOwned(argType)) {
                    // Ownership is transferred to the method
                    store.markMoved(argElement);
                    logger.debug("    MARKED MOVED: {}", argElement.getSimpleName());
                    logger.debug("    store.isMoved({})={}", argElement.getSimpleName(), store.isMoved(argElement));
                    logger.debug("    store.getMovedVariables()={}", store.getMovedVariables());
                    logger.debug("    result type={}", result.getClass().getSimpleName());

                    // Update the argument's type to @Moved in the store
                    AnnotationMirror movedAnno = atypeFactory.getMovedAnnotation();
                    HemileiaValue movedValue = analysis.createAbstractValue(
                            new AnnotationMirrorSet(movedAnno),
                            argType.getUnderlyingType());
                    JavaExpression argExpr = new LocalVariable(argVar);
                    store.replaceValue(argExpr, movedValue);
                }
            }
        }

        return result;
    }
}
