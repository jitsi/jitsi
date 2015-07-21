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
package net.java.sip.communicator.service.contactsource;

import java.util.*;

/**
 * Provides an abstract implementation of the basic functionality of
 * <tt>ContactQuery</tt> and allows extenders to focus on the specifics of their
 * implementation.
 *
 * @author Lyubomir Marinov
 * @param <T> the very type of <tt>ContactSourceService</tt> which performs the
 * <tt>ContactQuery</tt>
 */
public abstract class AbstractContactQuery<T extends ContactSourceService>
    implements ContactQuery
{
    /**
     * The <tt>ContactSourceService</tt> which is performing this
     * <tt>ContactQuery</tt>.
     */
    private final T contactSource;

    /**
     * The <tt>List</tt> of <tt>ContactQueryListener</tt>s which are to be
     * notified by this <tt>ContactQuery</tt> about changes in its status, the
     * receipt of new <tt>ContactSource</tt>s via this <tt>ContactQuery</tt>,
     * etc.
     */
    private final List<ContactQueryListener> listeners
        = new LinkedList<ContactQueryListener>();

    /**
     * The status of this <tt>ContactQuery</tt> which is one of the
     * <tt>QUERY_XXX</tt> constants defined by the <tt>ContactQuery</tt> class.
     */
    private int status = QUERY_IN_PROGRESS;

    /**
     * Initializes a new <tt>AbstractContactQuery</tt> which is to be performed
     * by a specific <tt>ContactSourceService</tt>. The status of the new
     * instance is {@link ContactQuery#QUERY_IN_PROGRESS}.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>AbstractContactQuery</tt>
     */
    protected AbstractContactQuery(T contactSource)
    {
        this.contactSource = contactSource;
    }

    /**
     * Adds a <tt>ContactQueryListener</tt> to the list of listeners interested
     * in notifications about this <tt>ContactQuery</tt> changing its status,
     * the receipt of new <tt>SourceContact</tt>s via this
     * <tt>ContactQuery</tt>, etc.
     *
     * @param l the <tt>ContactQueryListener</tt> to be added to the list of
     * listeners interested in the notifications raised by this
     * <tt>ContactQuery</tt>
     * @see ContactQuery#addContactQueryListener(ContactQueryListener)
     */
    public void addContactQueryListener(ContactQueryListener l)
    {
        if (l == null)
            throw new NullPointerException("l");
        else
        {
            synchronized (listeners)
            {
                if (!listeners.contains(l))
                    listeners.add(l);
            }
        }
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    public void cancel()
    {
        if (getStatus() == QUERY_IN_PROGRESS)
            setStatus(QUERY_CANCELED);
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a new <tt>SourceContact</tt> has been
     * received.
     *
     * @param contact the <tt>SourceContact</tt> which has been received and
     * which the registered <tt>ContactQueryListener</tt>s are to be notified
     * about
     * @param showMoreEnabled indicates whether show more label should be shown 
     * or not.
     */
    protected void fireContactReceived(SourceContact contact, 
        boolean showMoreEnabled)
    {
        ContactQueryListener[] ls;

        synchronized (listeners)
        {
            ls = listeners.toArray(new ContactQueryListener[listeners.size()]);
        }

        ContactReceivedEvent ev 
            = new ContactReceivedEvent(this, contact, showMoreEnabled);

        for (ContactQueryListener l : ls)
        {
            l.contactReceived(ev);
        }
    }
    
    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a new <tt>SourceContact</tt> has been
     * received.
     *
     * @param contact the <tt>SourceContact</tt> which has been received and
     * which the registered <tt>ContactQueryListener</tt>s are to be notified
     * about
     */
    protected void fireContactReceived(SourceContact contact)
    {
        fireContactReceived(contact, true);
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
     * removed.
     *
     * @param contact the <tt>SourceContact</tt> which has been removed and
     * which the registered <tt>ContactQueryListener</tt>s are to be notified
     * about
     */
    protected void fireContactRemoved(SourceContact contact)
    {
        ContactQueryListener[] ls;

        synchronized (listeners)
        {
            ls = listeners.toArray(new ContactQueryListener[listeners.size()]);
        }

        ContactRemovedEvent ev = new ContactRemovedEvent(this, contact);

        for (ContactQueryListener l : ls)
            l.contactRemoved(ev);
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
     * changed.
     *
     * @param contact the <tt>SourceContact</tt> which has been changed and
     * which the registered <tt>ContactQueryListener</tt>s are to be notified
     * about
     */
    protected void fireContactChanged(SourceContact contact)
    {
        ContactQueryListener[] ls;

        synchronized (listeners)
        {
            ls = listeners.toArray(new ContactQueryListener[listeners.size()]);
        }

        ContactChangedEvent ev = new ContactChangedEvent(this, contact);

        for (ContactQueryListener l : ls)
            l.contactChanged(ev);
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that its state has changed.
     *
     * @param eventType the type of the <tt>ContactQueryStatusEvent</tt> to be
     * fired which can be one of the <tt>QUERY_XXX</tt> constants defined by
     * <tt>ContactQueryStatusEvent</tt>
     */
    protected void fireQueryStatusChanged(int eventType)
    {
        ContactQueryListener[] ls;

        synchronized (listeners)
        {
            ls = listeners.toArray(new ContactQueryListener[listeners.size()]);
        }

        ContactQueryStatusEvent ev
            = new ContactQueryStatusEvent(this, eventType);

        for (ContactQueryListener l : ls)
            l.queryStatusChanged(ev);
    }

    /**
     * Gets the <tt>ContactSourceService</tt> which is performing this
     * <tt>ContactQuery</tt>.
     *
     * @return the <tt>ContactSourceService</tt> which is performing this
     * <tt>ContactQuery</tt>
     * @see ContactQuery#getContactSource()
     */
    public T getContactSource()
    {
        return contactSource;
    }

    /**
     * Gets the status of this <tt>ContactQuery</tt> which can be one of the
     * <tt>QUERY_XXX</tt> constants defined by <tt>ContactQuery</tt>.
     *
     * @return the status of this <tt>ContactQuery</tt> which can be one of the
     * <tt>QUERY_XXX</tt> constants defined by <tt>ContactQuery</tt>
     * @see ContactQuery#getStatus()
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Removes a <tt>ContactQueryListener</tt> from the list of listeners
     * interested in notifications about this <tt>ContactQuery</tt> changing its
     * status, the receipt of new <tt>SourceContact</tt>s via this
     * <tt>ContactQuery</tt>, etc.
     *
     * @param l the <tt>ContactQueryListener</tt> to be removed from the list of
     * listeners interested in notifications raised by this <tt>ContactQuery</tt>
     * @see ContactQuery#removeContactQueryListener(ContactQueryListener)
     */
    public void removeContactQueryListener(ContactQueryListener l)
    {
        if (l != null)
        {
            synchronized (listeners)
            {
                listeners.remove(l);
            }
        }
    }

    /**
     * Sets the status of this <tt>ContactQuery</tt>.
     *
     * @param status {@link ContactQuery#QUERY_CANCELED},
     * {@link ContactQuery#QUERY_COMPLETED}, or
     * {@link ContactQuery#QUERY_ERROR}
     */
    public void setStatus(int status)
    {
        if (this.status != status)
        {
            int eventType;

            switch (status)
            {
            case QUERY_CANCELED:
                eventType = ContactQueryStatusEvent.QUERY_CANCELED;
                break;
            case QUERY_COMPLETED:
                eventType = ContactQueryStatusEvent.QUERY_COMPLETED;
                break;
            case QUERY_ERROR:
                eventType = ContactQueryStatusEvent.QUERY_ERROR;
                break;
            case QUERY_IN_PROGRESS:
            default:
                throw new IllegalArgumentException("status");
            }

            this.status = status;
            fireQueryStatusChanged(eventType);
        }
    }
}
