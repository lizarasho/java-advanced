package ru.ifmo.rain.rasho.arrayset;

import java.util.*;

@SuppressWarnings("unused")
public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final ReversedArrayList<E> list;
    private final Comparator<? super E> comparator;

    private ArraySet(ReversedArrayList<E> list, Comparator<? super E> comparator) {
        this.list = list;
        this.comparator = comparator;
    }

    @SuppressWarnings("unused")
    public ArraySet() {
        this(new ReversedArrayList<>(), null);
    }

    @SuppressWarnings("unused")
    public ArraySet(Collection<? extends E> collection) {
        this(new ReversedArrayList<>(new TreeSet<>(collection)), null);
    }

    @SuppressWarnings("unused")
    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {

        if (isStrictlySorted(collection, comparator)) {
            this.list = new ReversedArrayList<>(collection);
        } else {
            TreeSet<E> set = new TreeSet<>(comparator);
            set.addAll(collection);
            this.list = new ReversedArrayList<>(set);
        }

        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    private int compare(E first, E second, Comparator<? super E> comparator) {
        if (comparator == null) {
            return ((Comparable) first).compareTo(second);
        }
        return comparator.compare(first, second);
    }

    private boolean isStrictlySorted(Collection<? extends E> collection, Comparator<? super E> comparator) {
        E prev = null;
        for (E e : collection) {
            if (prev != null) {
                if (compare(prev, e, comparator) >= 0) {
                    return false;
                }
            }
            prev = e;
        }
        return true;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    private boolean isValid(int index) {
        return 0 <= index && index < list.size();
    }

    private int searchPosition(E element, int shiftIfFound, int shiftIfNotFound) {
        int ind = Collections.binarySearch(list, Objects.requireNonNull(element), comparator);
        if (ind >= 0) {
            return ind + shiftIfFound;
        }
        return -ind - 1 + shiftIfNotFound;
    }

    private E get(int index) {
        if (isValid(index)) {
            return list.get(index);
        }
        return null;
    }

    @Override
    public E lower(E element) {
        return get(searchPosition(element, -1, -1));
    }

    @Override
    public E floor(E element) {
        return get(searchPosition(element, 0, -1));
    }

    @Override
    public E ceiling(E element) {
        return get(searchPosition(element, 0, 0));
    }

    @Override
    public E higher(E element) {
        return get(searchPosition(element, 1, 0));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    private void checkNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E first() {
        checkNotEmpty();
        return list.get(0);
    }

    @Override
    public E last() {
        checkNotEmpty();
        return list.get(size() - 1);
    }

    private NavigableSet<E> emptySet() {
        return new ArraySet<>(new ReversedArrayList<>(), comparator);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int leftBorder = searchPosition(fromElement, fromInclusive ? 0 : 1, 0);
        int rightBorder = searchPosition(toElement, toInclusive ? 0 : -1, -1);

        if (isValid(leftBorder) && isValid(rightBorder) && rightBorder >= leftBorder) {
            return new ArraySet<>(new ReversedArrayList<>(list.subList(leftBorder, rightBorder + 1)), comparator);
        }
        return emptySet();
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        if (!list.isEmpty()) {
            return subSet(first(), true, toElement, inclusive);
        }
        return emptySet();
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        if (!list.isEmpty()) {
            return subSet(fromElement, inclusive, last(), true);
        }
        return emptySet();
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) throws IllegalArgumentException {
        if (compare(fromElement, toElement, comparator) > 0) {
            throw new IllegalArgumentException();
        }
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(list, (E) Objects.requireNonNull(o), comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        ReversedArrayList<E> reversedList = list.getReversedList();
        Comparator<? super E> reversedComparator = Collections.reverseOrder(comparator);
        return new ArraySet<>(reversedList, reversedComparator);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }
}