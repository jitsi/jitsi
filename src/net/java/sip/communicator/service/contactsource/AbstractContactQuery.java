/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * Initializes a new <tt>AbstractContactQuery</tt> which is to be performed
     * by a specific <tt>ContactSourceService</tt>.
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
        ContactQueryListener[] ls;

        synchronized (listeners)
        {
            ls = listeners.toArray(new ContactQueryListener[listeners.size()]);
        }

        ContactReceivedEvent ev = new ContactReceivedEvent(this, contact);

        for (ContactQueryListener l : ls)
            l.contactReceived(ev);
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
}
