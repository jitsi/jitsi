/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.callhistory;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The Call History Service stores info about calls made from various protocols
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public interface CallHistoryService
{
    /**
     * Returns all the calls made by all the contacts
     * in the supplied metacontact after the given date
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the calls
     * @return Collection of CallReceivedEvent
     * @throws RuntimeException
     */
    Collection findByStartDate(MetaContact contact, Date startDate)
        throws RuntimeException;

    /**
     * Returns all the calls made by all the contacts
     * in the supplied metacontact before the given date
     *
     * @param contact MetaContact
     * @param endDate Date the end date of the calls
     * @return Collection of CallReceivedEvent
     * @throws RuntimeException
     */
    Collection findByEndDate(MetaContact contact, Date endDate)
        throws RuntimeException;

    /**
     * Returns all the calls made by all the contacts
     * in the supplied metacontact between the given dates
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallReceivedEvent
     * @throws RuntimeException
     */
    Collection findByPeriod(MetaContact contact, Date startDate, Date endDate)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent calls made by all the contacts
     * in the supplied metacontact
     *
     * @param contact MetaContact
     * @param count calls count
     * @return Collection of CallReceivedEvent
     * @throws RuntimeException
     */
    Collection findLast(MetaContact contact, int count)
        throws RuntimeException;

}
