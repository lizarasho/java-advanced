package ru.ifmo.rain.rasho.arrayset;

import java.util.*;

public class ReversedArrayList<E> extends AbstractList<E> implements RandomAccess {

    private final List<E> list;
    private final boolean reversed;

    private ReversedArrayList(List<E> list, boolean reversed) {
        this.list = list;
        this.reversed = reversed;
    }

    ReversedArrayList(Collection<? extends E> collection) {
        this(new ArrayList<>(collection), false);
    }

    ReversedArrayList(List<E> list) {
        this(list, false);
    }

    ReversedArrayList(TreeSet<E> list) {
        this(new ArrayList<>(list), false);
    }

    ReversedArrayList() {
        this(Collections.emptyList(), false);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public E get(int index) {
        return reversed ? list.get(size() - 1 - index) : list.get(index);
    }

    ReversedArrayList<E> getReversedList() {
        return new ReversedArrayList<>(list, !reversed);
    }
}