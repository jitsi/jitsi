/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.msghistory;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;

/**
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public interface MessageHistoryService
{
    Collection findByStartDate(MetaContact contact, Date startDate)
        throws RuntimeException;
    Collection findByEndDate(MetaContact contact, Date endDate)
        throws RuntimeException;
    Collection findByPeriod(MetaContact contact, Date startDate, Date endDate)
        throws RuntimeException;
    Collection findByPeriod(MetaContact contact, Date startDate, Date endDate, String[] keywords)
        throws UnsupportedOperationException;
    Collection findByKeyword(MetaContact contact, String keyword)
        throws RuntimeException;
    Collection findByKeywords(MetaContact contact, String[] keywords)
        throws RuntimeException;
    Collection findLast(MetaContact contact, int count)
        throws RuntimeException;
}
