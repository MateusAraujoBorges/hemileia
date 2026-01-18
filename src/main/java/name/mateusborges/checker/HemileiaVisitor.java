package name.mateusborges.checker;

import javax.lang.model.element.Element;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

/**
 * The visitor for the Hemileia ownership type system.
 *
 * <p>This visitor checks Rust-style ownership rules at each AST node:
 * <ul>
 *   <li><b>Use-after-move:</b> Reports an error if a moved variable is used</li>
 *   <li><b>Borrow conflicts:</b> Reports an error if mutable and immutable borrows
 *       are created simultaneously, or if multiple mutable borrows exist</li>
 *   <li><b>Ownership transfer:</b> Validates that ownership is properly transferred</li>
 * </ul>
 */
public class HemileiaVisitor extends BaseTypeVisitor<HemileiaAnnotatedTypeFactory> {

    private static final Logger logger = LoggerFactory.getLogger(HemileiaVisitor.class);

    /** Error message for using a moved value */
    private static final String USE_AFTER_MOVE = "use.after.move";

    /** Error message for conflicting borrows */
    private static final String BORROW_CONFLICT = "borrow.conflict";

    /** Error message for multiple mutable borrows */
    private static final String MULTIPLE_MUT_BORROW = "multiple.mut.borrow";

    public HemileiaVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void p) {
        checkUseAfterMove(tree);
        return super.visitIdentifier(tree, p);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void p) {
        ExpressionTree initializer = tree.getInitializer();
        if (initializer != null) {
            checkBorrowRules(tree, initializer);
        }
        return super.visitVariable(tree, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void p) {
        checkBorrowRules(tree.getVariable(), tree.getExpression());
        return super.visitAssignment(tree, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        // Check each argument for use-after-move
        for (ExpressionTree arg : tree.getArguments()) {
            if (arg instanceof IdentifierTree identTree) {
                checkUseAfterMove(identTree);
            }
        }
        return super.visitMethodInvocation(tree, p);
    }

    /**
     * Checks if a variable has been moved and is being used.
     */
    private void checkUseAfterMove(IdentifierTree tree) {
        // Skip if this identifier is an assignment target - reassigning a moved variable is allowed
        if (isAssignmentTarget(tree)) {
            logger.debug("Skipping use-after-move check for assignment target: {}", tree.getName());
            return;
        }

        Element element = atypeFactory.getAnnotatedType(tree).getUnderlyingType() != null
                ? org.checkerframework.javacutil.TreeUtils.elementFromUse(tree)
                : null;

        if (element == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            long position = com.sun.source.util.Trees.instance(checker.getProcessingEnvironment())
                .getSourcePositions()
                .getStartPosition(atypeFactory.getPath(tree).getCompilationUnit(), tree);
            logger.debug("checkUseAfterMove: {} at position {}", element.getSimpleName(), position);
        }

        // Get the store at this program point
        HemileiaStore store = atypeFactory.getStoreBefore(tree);
        logger.debug("  store={}, isMoved={}",
            store != null ? "present" : "null",
            store != null ? store.isMoved(element) : "N/A");
        if (store != null && store.isMoved(element)) {
            logger.debug("  REPORTING ERROR via store.isMoved");
            checker.reportError(tree, USE_AFTER_MOVE, element.getSimpleName());
        }

        // Also check if the type has @Moved annotation (from dataflow)
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        logger.debug("  type={}, hasMoved={}", type, atypeFactory.hasMoved(type));
        if (atypeFactory.hasMoved(type)) {
            logger.debug("  REPORTING ERROR via hasMoved");
            checker.reportError(tree, USE_AFTER_MOVE, element.getSimpleName());
        }
    }

    /**
     * Checks if the given tree is the target of an assignment (being written to, not read).
     * In Rust semantics, assigning to a moved variable is allowed - it clears the moved state.
     */
    private boolean isAssignmentTarget(IdentifierTree tree) {
        TreePath path = atypeFactory.getPath(tree);
        if (path == null) {
            return false;
        }

        TreePath parentPath = path.getParentPath();
        if (parentPath == null) {
            return false;
        }

        Tree parent = parentPath.getLeaf();

        // Check if this identifier is the left-hand side of an assignment
        if (parent instanceof AssignmentTree assignment) {
            return assignment.getVariable() == tree;
        }

        // Check compound assignments (+=, -=, etc.) - these both read AND write, so not pure assignment targets
        // For compound assignments, we still need to check use-after-move since the old value is read
        if (parent instanceof CompoundAssignmentTree) {
            return false;
        }

        return false;
    }

    /**
     * Checks borrow rules when creating a borrow from an assignment.
     */
    private void checkBorrowRules(Tree target, ExpressionTree source) {
        if (!(source instanceof IdentifierTree sourceIdent)) {
            return;
        }

        Element sourceElement = org.checkerframework.javacutil.TreeUtils.elementFromUse(sourceIdent);
        if (sourceElement == null) {
            return;
        }

        AnnotatedTypeMirror targetType = atypeFactory.getAnnotatedType(target);
        AnnotatedTypeMirror sourceType = atypeFactory.getAnnotatedType(source);

        boolean targetIsBorrowed = atypeFactory.hasBorrowed(targetType);
        boolean targetIsMutBorrowed = atypeFactory.hasMutBorrowed(targetType);
        boolean sourceIsOwned = atypeFactory.hasOwned(sourceType);

        if (!sourceIsOwned) {
            return;
        }

        HemileiaStore store = atypeFactory.getStoreBefore(source);
        if (store == null) {
            return;
        }

        if (targetIsMutBorrowed) {
            // Creating a mutable borrow - check for conflicts
            if (store.hasMutableBorrow(sourceElement)) {
                // Already has a mutable borrow
                checker.reportError(target, MULTIPLE_MUT_BORROW, sourceElement.getSimpleName());
            } else if (store.hasImmutableBorrows(sourceElement)) {
                // Has immutable borrows - conflict
                checker.reportError(target, BORROW_CONFLICT, sourceElement.getSimpleName());
            }
        } else if (targetIsBorrowed) {
            // Creating an immutable borrow - check for mutable borrow conflict
            if (store.hasMutableBorrow(sourceElement)) {
                checker.reportError(target, BORROW_CONFLICT, sourceElement.getSimpleName());
            }
        }
    }
}
