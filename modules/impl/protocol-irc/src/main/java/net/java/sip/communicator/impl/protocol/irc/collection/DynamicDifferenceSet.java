/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.irc.collection;

import java.util.*;

/**
 * Dynamic difference set.
 *
 * A custom implementation that constructs a (dynamic) difference set from 2
 * input instances. The first input instance is the 'source' which is used as
 * the base data set. The second input is the 'removals', which are then removed
 * from the source set. The dynamic set is computed each time a method call is
 * executed.
 *
 * This set is immutable. That is, the modifier methods are unsupported. Changes
 * that do happen are derived from changes in the 'source' set or the 'removals'
 * set.
 *
 * Keep in mind that any operation on this dynamic returns a result that is only
 * valid in the moment. Even calling two subsequent methods can result in
 * difference or conflicting results.
 *
 * @author Danny van Heumen
 *
 * @param <E> The type of element that is stored in the set.
 */
public class DynamicDifferenceSet<E>
    implements Set<E>
{
    /**
     * SYNCHRONIZED The source or base data set. This set is the basis and contains all the
     * elements that can possibly be in the dynamic set.
     */
    private final Set<E> source;

    /**
     * SYNCHRONIZED The removals set contains elements that are removed during the
     * calculation of the difference set at the moment.
     */
    private final Set<E> removals;

    /**
     * Constructor for creating a difference set instance.
     *
     * @param source The source data set.
     * @param removals The removals data set which will be used to create the
     *            difference.
     */
    public DynamicDifferenceSet(final Set<E> source,
        final Set<E> removals)
    {
        if (source == null)
        {
            throw new IllegalArgumentException("source cannot be null");
        }
        this.source = source;
        if (removals == null)
        {
            throw new IllegalArgumentException("removals cannot be null");
        }
        this.removals = removals;
    }

    /**
     * Calculate the difference set based on the current state of the data.
     *
     * @return Returns the current difference set that is calculated on the fly.
     */
    private Set<E> calculate()
    {
        final TreeSet<E> current;
        synchronized (source)
        {
            current = new TreeSet<E>(source);
        }
        synchronized (removals)
        {
            current.removeAll(removals);
        }
        return current;
    }

    /**
     * Get the size of the difference set. (Keep in mind that these numbers are
     * only momentary and may be obsolete as soon as they are calculated.)
     */
    @Override
    public int size()
    {
        return calculate().size();
    }

    /**
     * Check if the difference set of the moment is empty. (Keep in mind that
     * the result may be obsolete as soon as it is calculated.)
     */
    @Override
    public boolean isEmpty()
    {
        return calculate().isEmpty();
    }

    /**
     * Check if an element is contained in the difference set.
     */
    @Override
    public boolean contains(Object o)
    {
        return this.source.contains(o) && !this.removals.contains(o);
    }

    /**
     * Get an iterator for the difference set of the moment.
     */
    @Override
    public Iterator<E> iterator()
    {
        return calculate().iterator();
    }

    /**
     * Get an array of the current difference set.
     */
    @Override
    public Object[] toArray()
    {
        return calculate().toArray();
    }

    /**
     * Get an array of the current difference set.
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return calculate().toArray(a);
    }

    /**
     * Adding elements to a difference set is unsupported.
     */
    @Override
    public boolean add(E e)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Removing elements from a difference set is unsupported.
     */
    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Check if all provided elements are contained in the difference set of the
     * moment.
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        return calculate().containsAll(c);
    }

    /**
     * Adding elements to a difference set is unsupported.
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Removing elements from a difference set is unsupported.
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Modifying a difference set is unsupported.
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Modifying a difference set is unsupported.
     */
    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }
}
