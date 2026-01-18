package name.mateusborges.checker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.Element;

import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

/**
 * The dataflow store for the Hemileia ownership type system.
 *
 * <p>In addition to standard type refinement (handled by the superclass),
 * this store tracks:
 * <ul>
 *   <li><b>Moved variables:</b> Variables whose values have been moved away.
 *       Any use of a moved variable is an error.</li>
 *   <li><b>Active borrows:</b> For each owned variable, tracks which other
 *       variables are currently borrowing it (immutably or mutably).</li>
 *   <li><b>Borrow sources:</b> For each borrowed variable, tracks which
 *       owned variable it borrows from.</li>
 * </ul>
 */
public class HemileiaStore extends CFAbstractStore<HemileiaValue, HemileiaStore> {

    /**
     * Set of variable elements that have been moved (ownership transferred away).
     * Using an element from this set is a use-after-move error.
     */
    private final Set<Element> movedVariables;

    /**
     * Maps owned variables to the set of variables currently borrowing from them.
     * Used to enforce borrow exclusivity rules.
     */
    private final Map<Element, Set<Element>> activeBorrows;

    /**
     * Maps borrowed variables to their source (the owned variable they borrow from).
     * Used to track borrow lifetimes.
     */
    private final Map<Element, Element> borrowSources;

    /**
     * Maps borrowed variables to whether they are mutable borrows.
     */
    private final Map<Element, Boolean> mutableBorrows;

    public HemileiaStore(
            CFAbstractAnalysis<HemileiaValue, HemileiaStore, HemileiaTransfer> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        this.movedVariables = new HashSet<>();
        this.activeBorrows = new HashMap<>();
        this.borrowSources = new HashMap<>();
        this.mutableBorrows = new HashMap<>();
    }

    /**
     * Copy constructor.
     */
    protected HemileiaStore(HemileiaStore other) {
        super(other);
        this.movedVariables = new HashSet<>(other.movedVariables);
        this.activeBorrows = new HashMap<>();
        for (Map.Entry<Element, Set<Element>> entry : other.activeBorrows.entrySet()) {
            this.activeBorrows.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        this.borrowSources = new HashMap<>(other.borrowSources);
        this.mutableBorrows = new HashMap<>(other.mutableBorrows);
    }

    @Override
    public HemileiaStore copy() {
        return new HemileiaStore(this);
    }

    /**
     * Marks a variable as moved. After this, any use of the variable is an error.
     *
     * @param element the variable element that has been moved
     */
    public void markMoved(Element element) {
        movedVariables.add(element);
    }

    /**
     * Checks if a variable has been moved.
     *
     * @param element the variable element to check
     * @return true if the variable has been moved
     */
    public boolean isMoved(Element element) {
        return movedVariables.contains(element);
    }

    /**
     * Clears the moved status for a variable (e.g., when it's reassigned).
     *
     * @param element the variable element
     */
    public void clearMoved(Element element) {
        movedVariables.remove(element);
    }

    /**
     * Records that a variable is borrowing from an owner.
     *
     * @param borrower the variable that is borrowing
     * @param owner the owned variable being borrowed from
     * @param isMutable true if this is a mutable borrow
     */
    public void addBorrow(Element borrower, Element owner, boolean isMutable) {
        activeBorrows.computeIfAbsent(owner, k -> new HashSet<>()).add(borrower);
        borrowSources.put(borrower, owner);
        mutableBorrows.put(borrower, isMutable);
    }

    /**
     * Removes a borrow when the borrowing variable goes out of scope or is reassigned.
     *
     * @param borrower the variable that was borrowing
     */
    public void removeBorrow(Element borrower) {
        Element owner = borrowSources.remove(borrower);
        mutableBorrows.remove(borrower);
        if (owner != null) {
            Set<Element> borrows = activeBorrows.get(owner);
            if (borrows != null) {
                borrows.remove(borrower);
                if (borrows.isEmpty()) {
                    activeBorrows.remove(owner);
                }
            }
        }
    }

    /**
     * Gets the set of variables currently borrowing from an owned variable.
     *
     * @param owner the owned variable
     * @return set of borrowing variables, or empty set if none
     */
    public Set<Element> getBorrowsFrom(Element owner) {
        return activeBorrows.getOrDefault(owner, Set.of());
    }

    /**
     * Gets the owner that a borrowed variable borrows from.
     *
     * @param borrower the borrowed variable
     * @return the owner element, or null if not a borrow
     */
    public Element getBorrowSource(Element borrower) {
        return borrowSources.get(borrower);
    }

    /**
     * Checks if a variable has a mutable borrow active.
     *
     * @param owner the owned variable to check
     * @return true if there is an active mutable borrow
     */
    public boolean hasMutableBorrow(Element owner) {
        Set<Element> borrows = activeBorrows.get(owner);
        if (borrows == null) {
            return false;
        }
        for (Element borrower : borrows) {
            if (Boolean.TRUE.equals(mutableBorrows.get(borrower))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a variable has any immutable borrows active.
     *
     * @param owner the owned variable to check
     * @return true if there are active immutable borrows
     */
    public boolean hasImmutableBorrows(Element owner) {
        Set<Element> borrows = activeBorrows.get(owner);
        if (borrows == null) {
            return false;
        }
        for (Element borrower : borrows) {
            if (!Boolean.TRUE.equals(mutableBorrows.get(borrower))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a specific variable is a mutable borrow.
     *
     * @param borrower the variable to check
     * @return true if it is a mutable borrow
     */
    public boolean isMutableBorrow(Element borrower) {
        return Boolean.TRUE.equals(mutableBorrows.get(borrower));
    }

    /**
     * Gets all moved variables.
     *
     * @return set of moved variable elements
     */
    public Set<Element> getMovedVariables() {
        return Set.copyOf(movedVariables);
    }

    @Override
    public HemileiaStore leastUpperBound(HemileiaStore other) {
        HemileiaStore result = super.leastUpperBound(other);
        // At merge points, a variable is moved if it's moved in either branch
        result.movedVariables.addAll(this.movedVariables);
        result.movedVariables.addAll(other.movedVariables);
        // Borrows are invalidated at merge points if they differ
        // (conservative: keep borrows that exist in both)
        for (Map.Entry<Element, Set<Element>> entry : this.activeBorrows.entrySet()) {
            Set<Element> otherBorrows = other.activeBorrows.get(entry.getKey());
            if (otherBorrows != null) {
                Set<Element> intersection = new HashSet<>(entry.getValue());
                intersection.retainAll(otherBorrows);
                if (!intersection.isEmpty()) {
                    result.activeBorrows.put(entry.getKey(), intersection);
                }
            }
        }
        for (Element borrower : this.borrowSources.keySet()) {
            if (other.borrowSources.containsKey(borrower)) {
                result.borrowSources.put(borrower, this.borrowSources.get(borrower));
                result.mutableBorrows.put(borrower, this.mutableBorrows.get(borrower));
            }
        }
        return result;
    }

    @Override
    public HemileiaStore widenedUpperBound(HemileiaStore previous) {
        HemileiaStore result = super.widenedUpperBound(previous);
        // Same semantics as leastUpperBound for ownership tracking
        result.movedVariables.addAll(this.movedVariables);
        result.movedVariables.addAll(previous.movedVariables);
        return result;
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<HemileiaValue, HemileiaStore> other) {
        if (!super.supersetOf(other)) {
            return false;
        }

        if (!(other instanceof HemileiaStore otherHemileia)) {
            return false;
        }

        // A superset must contain all moved variables from the other store
        if (!this.movedVariables.containsAll(otherHemileia.movedVariables)) {
            return false;
        }

        // Check borrow relationships
        for (Map.Entry<Element, Set<Element>> entry : otherHemileia.activeBorrows.entrySet()) {
            Set<Element> thisBorrows = this.activeBorrows.get(entry.getKey());
            if (thisBorrows == null || !thisBorrows.containsAll(entry.getValue())) {
                return false;
            }
        }

        for (Map.Entry<Element, Element> entry : otherHemileia.borrowSources.entrySet()) {
            if (!Objects.equals(this.borrowSources.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }

        for (Map.Entry<Element, Boolean> entry : otherHemileia.mutableBorrows.entrySet()) {
            if (!Objects.equals(this.mutableBorrows.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + movedVariables.hashCode();
        result = 31 * result + activeBorrows.hashCode();
        result = 31 * result + borrowSources.hashCode();
        result = 31 * result + mutableBorrows.hashCode();
        return result;
    }

    @Override
    protected String internalVisualize(CFGVisualizer<HemileiaValue, HemileiaStore, ?> viz) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.internalVisualize(viz));
        if (!movedVariables.isEmpty()) {
            sb.append("  moved: ").append(movedVariables).append("\n");
        }
        if (!activeBorrows.isEmpty()) {
            sb.append("  borrows: ").append(activeBorrows).append("\n");
        }
        return sb.toString();
    }
}
