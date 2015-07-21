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
package net.java.sip.communicator.service.protocol;

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Provides implementations for some of the methods in the <tt>Call</tt>
 * abstract class to facilitate implementations.
 *
 * @param <T> the peer extension class like for example <tt>CallPeerSipImpl</tt>
 * or <tt>CallPeerJabberImpl</tt>
 * @param <U> the provider extension class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt> or
 * <tt>ProtocolProviderServiceJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public abstract class AbstractCall<T extends CallPeer,
                                   U extends ProtocolProviderService>
    extends Call
{
    /**
     * The list of <tt>CallPeer</tt>s of this <tt>Call</tt>. It is implemented
     * as a copy-on-write storage in order to optimize the implementation of
     * {@link Call#getCallPeers()}. It represents private state which is to not
     * be exposed to outsiders. An unmodifiable view which may safely be exposed
     * to outsiders without the danger of
     * <tt>ConcurrentModificationException</tt> is
     * {@link #unmodifiableCallPeers}.
     */
    private List<T> callPeers;

    /**
     * The <tt>Object</tt> which is used to synchronize the access to
     * {@link #callPeers} and {@link #unmodifiableCallPeers}.
     */
    private final Object callPeersSyncRoot = new Object();

    /**
     * The <tt>PropertyChangeSupport</tt> which helps this instance with
     * <tt>PropertyChangeListener</tt>s.
     */
    private final PropertyChangeSupport propertyChangeSupport
        = new PropertyChangeSupport(this);

    /**
     * An unmodifiable view of {@link #callPeers}. It may safely be exposed to
     * outsiders without the danger of <tt>ConcurrentModificationException</tt>
     * and thus optimizes the implementation of {@link Call#getCallPeers()}.
     */
    private List<T> unmodifiableCallPeers;

    /**
     * Creates a new Call instance.
     *
     * @param sourceProvider the proto provider that created us.
     */
    protected AbstractCall(U sourceProvider)
    {
        super(sourceProvider);

        callPeers = Collections.emptyList();
        unmodifiableCallPeers = Collections.unmodifiableList(callPeers);
    }

    /**
     * {@inheritDoc}
     *
     * Delegates to {@link #propertyChangeSupport}.
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a specific <tt>CallPeer</tt> to the list of <tt>CallPeer</tt>s of
     * this <tt>Call</tt> if the list does not contain it; otherwise, does
     * nothing. Does not fire {@link CallPeerEvent#CALL_PEER_ADDED}.
     * <p>
     * The method is named <tt>doAddCallPeer</tt> and not <tt>addCallPeer</tt>
     * because, at the time of its introduction, multiple extenders have already
     * defined an <tt>addCallPeer</tt> method with the same argument but with no
     * return value.
     * </p>
     *
     * @param callPeer the <tt>CallPeer</tt> to be added to the list of
     * <tt>CallPeer</tt>s of this <tt>Call</tt>
     * @return <tt>true</tt> if the list of <tt>CallPeer</tt>s of this
     * <tt>Call</tt> was modified as a result of the execution of the method;
     * otherwise, <tt>false</tt>
     * @throws NullPointerException if <tt>callPeer</tt> is <tt>null</tt>
     */
    protected boolean doAddCallPeer(T callPeer)
    {
        if (callPeer == null)
            throw new NullPointerException("callPeer");

        synchronized (callPeersSyncRoot)
        {
            if (callPeers.contains(callPeer))
                return false;
            else
            {
                /*
                 * The List of CallPeers of this Call is implemented as a
                 * copy-on-write storage in order to optimize the implementation
                 * of the Call.getCallPeers() method.
                 */

                List<T> newCallPeers = new ArrayList<T>(callPeers);

                if (newCallPeers.add(callPeer))
                {
                    callPeers = newCallPeers;
                    unmodifiableCallPeers
                        = Collections.unmodifiableList(callPeers);
                    return true;
                }
                else
                    return false;
            }
        }
    }

    /**
     * Removes a specific <tt>CallPeer</tt> from the list of <tt>CallPeer</tt>s
     * of this <tt>Call</tt> if the list does contain it; otherwise, does
     * nothing. Does not fire {@link CallPeerEvent#CALL_PEER_REMOVED}.
     * <p>
     * The method is named <tt>doRemoveCallPeer</tt> and not
     * <tt>removeCallPeer</tt> because, at the time of its introduction,
     * multiple extenders have already defined a <tt>removeCallPeer</tt> method
     * with the same argument but with no return value.
     * </p>
     *
     * @param callPeer the <tt>CallPeer</tt> to be removed from the list of
     * <tt>CallPeer</tt>s of this <tt>Call</tt>
     * @return <tt>true</tt> if the list of <tt>CallPeer</tt>s of this
     * <tt>Call</tt> was modified as a result of the execution of the method;
     * otherwise, <tt>false</tt>
     */
    protected boolean doRemoveCallPeer(T callPeer)
    {
        synchronized (callPeersSyncRoot)
        {
            /*
             * The List of CallPeers of this Call is implemented as a
             * copy-on-write storage in order to optimize the implementation of
             * the Call.getCallPeers() method.
             */

            List<T> newCallPeers = new ArrayList<T>(callPeers);

            if (newCallPeers.remove(callPeer))
            {
                callPeers = newCallPeers;
                unmodifiableCallPeers
                    = Collections.unmodifiableList(callPeers);
                return true;
            }
            else
                return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Delegates to {@link #propertyChangeSupport}.
     */
    @Override
    protected void firePropertyChange(
            String property,
            Object oldValue, Object newValue)
    {
        propertyChangeSupport.firePropertyChange(property, oldValue, newValue);
    }

    /**
     * Returns the number of peers currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of peers currently
     * associated with this call.
     */
    @Override
    public int getCallPeerCount()
    {
        return getCallPeerList().size();
    }

    /**
     * Gets an unmodifiable <tt>List</tt> of the <tt>CallPeer</tt>s of this
     * <tt>Call</tt>. The implementation of {@link Call#getCallPeers()} returns
     * an <tt>Iterator</tt> over the same <tt>List</tt>.
     *
     * @return an unmodifiable <tt>List</tt> of the <tt>CallPeer</tt>s of this
     * <tt>Call</tt>
     */
    public List<T> getCallPeerList()
    {
        synchronized (callPeersSyncRoot)
        {
            return unmodifiableCallPeers;
        }
    }

    /**
     * Returns an <tt>Iterator</tt> over the (list of) <tt>CallPeer</tt>s of
     * this <tt>Call</tt>. The returned <tt>Iterator</tt> operates over the
     * <tt>List</tt> returned by {@link #getCallPeerList()}.
     *
     * @return an <tt>Iterator</tt> over the (list of) <tt>CallPeer</tt>s of
     * this <tt>Call</tt>
     */
    @Override
    public Iterator<T> getCallPeers()
    {
        return getCallPeerList().iterator();
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this call.
     *
     * @return the <tt>ProtocolProviderService</tt> that created this call.
     */
    @Override
    @SuppressWarnings("unchecked")
    public U getProtocolProvider()
    {
        return (U) super.getProtocolProvider();
    }

    /**
     * {@inheritDoc}
     *
     * Delegates to {@link #propertyChangeSupport}.
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
