/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

/**
 * The <tt>ProtocolSourceContact</tt> provides a sorted
 * <tt>GenericSourceContact</tt>. <tt>SourceContact</tt>-s are sorted
 * alphabetically and based on their presence status.
 */
public class SortedGenericSourceContact
    extends GenericSourceContact
    implements Comparable<SourceContact>
{
    /**
     * The parent contact query.
     */
    private final ContactQuery parentQuery;

    /**
     * Creates an instance of <tt>ProtocolSourceContact</tt>.
     *
     * @param parentQuery the parent <tt>ContactQuery</tt>, which generated
     * this result contact
     * @param cSourceService the parent <tt>ContactSourceService</tt>, of
     * which this source contact is part
     * @param displayName the display name of the contact
     * @param contactDetails the list of contact details
     */
    public SortedGenericSourceContact(  ContactQuery parentQuery,
                                        ContactSourceService cSourceService,
                                        String displayName,
                                        List<ContactDetail> contactDetails)
    {
         super( cSourceService,
                displayName,
                contactDetails);

         this.parentQuery = parentQuery;
    }

    /**
     * Compares this contact with the specified object for order. Returns
     * a negative integer, zero, or a positive integer as this contact is
     * less than, equal to, or greater than the specified object.
     * <p>
     * The result of this method is calculated the following way:
     * <p>
     *  ( (10 - isOnline) - (10 - targetIsOnline)) * 100000000 <br>
            + getDisplayName()
                .compareToIgnoreCase(target.getDisplayName()) * 10000 <br>
            + compareDDetails * 1000 <br>
            + String.valueOf(hashCode())
                .compareToIgnoreCase(String.valueOf(o.hashCode()))
     * <p>
     * Or in other words ordering of source contacts would be first done by
     * presence status, then display name, then display details and finally
     * (in order to avoid equalities) be the hashCode of the object.
     * <p>
     * @param   o the <code>SourceContact</code> to be compared.
     * @return  a negative integer, zero, or a positive integer as this
     *  object is less than, equal to, or greater than the specified object.
     */
    public int compareTo(SourceContact o)
    {
        SourceContact target = o;

        int comparePresence = 0;
        if (getPresenceStatus() != null && target.getPresenceStatus() != null)
        {
            int isOnline
                = (getPresenceStatus().isOnline())
                ? 1
                : 0;
            int targetIsOnline
                = (target.getPresenceStatus().isOnline())
                ? 1
                : 0;

            comparePresence = ( (10 - isOnline) - (10 - targetIsOnline));
        }

        int compareDDetails = 0;
        if (getDisplayDetails() != null && target.getDisplayDetails() != null)
        {
            compareDDetails
                = getDisplayDetails()
                    .compareToIgnoreCase(target.getDisplayDetails());
        }

        return comparePresence * 100000000
            + getDisplayName()
                .compareToIgnoreCase(target.getDisplayName()) * 10000
            + compareDDetails * 100
            + String.valueOf(hashCode())
                .compareToIgnoreCase(String.valueOf(o.hashCode()));
    }

    /**
     * Returns the index of this source contact in its parent group.
     *
     * @return the index of this contact in its parent
     */
    @Override
    public int getIndex()
    {
        return parentQuery.getQueryResults().indexOf(this);
    }
}