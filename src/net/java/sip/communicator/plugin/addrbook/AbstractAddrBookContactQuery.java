/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

import net.java.sip.communicator.service.contactsource.*;

import java.util.*;
import java.util.regex.*;

/**
 * Abstract query that keep tracks of the source contacts
 * that were created.
 *
 * @author Damian Minkov
 */
public abstract class AbstractAddrBookContactQuery<T extends ContactSourceService>
    extends AsyncContactQuery<T>
{
    /**
     * A list of all source contact results.
     */
    protected final List<SourceContact> sourceContacts
        = new LinkedList<SourceContact>();

    /**
     * Initializes a new <tt>AbstractAddrBookContactQuery</tt> which is to perform
     * a specific <tt>query</tt> in the Address Book on behalf of a
     * specific <tt>ContactSourceService</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> is
     * being queried
     **/
    public AbstractAddrBookContactQuery(
            T contactSource,
            Pattern query)
    {
        super(contactSource, query);
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
    @Override
    protected void fireContactReceived(SourceContact contact)
    {
        synchronized (sourceContacts)
        {
            sourceContacts.add(contact);
        }

        super.fireContactReceived(contact);
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
    @Override
    protected void fireContactRemoved(SourceContact contact)
    {
        synchronized (sourceContacts)
        {
            sourceContacts.remove(contact);
        }

        super.fireContactRemoved(contact);
    }

    /**
     * Clear.
     */
    public void clear()
    {
        synchronized (sourceContacts)
        {
            sourceContacts.clear();
        }
    }

    /**
     * Searches for source contact with the specified id.
     * @param id the id to search for
     * @return the source contact found or null.
     */
    protected SourceContact findSourceContactByID(String id)
    {
        synchronized(sourceContacts)
        {
            for(SourceContact sc : sourceContacts)
            {
                Object scID = sc.getData(SourceContact.DATA_ID);

                if(id.equals(scID))
                    return sc;
            }
        }

        // not found
        return null;
    }
}
