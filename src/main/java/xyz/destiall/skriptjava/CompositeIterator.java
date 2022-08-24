package xyz.destiall.skriptjava;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class CompositeIterator<T> implements Iterator<T> {
    private final Iterator<? extends T>[] iterators;
    private int iteratorIndex = 0;

    public CompositeIterator(Iterator<? extends T>... iterators) {
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        if (iteratorIndex >= iterators.length) {
            return false;
        }
        if (iterators[iteratorIndex].hasNext()) {
            return true;
        }
        iteratorIndex++;

        if (iteratorIndex >= iterators.length) {
            return false;
        }

        return iterators[iteratorIndex].hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return iterators[iteratorIndex].next();
    }
}
