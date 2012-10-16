/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.demuxcontactsource;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Provides an implementation of the <tt>DemuxContactSourceService</tt> abstract
 * class. This implementation provides a de-multiplexed protocol aware copy of
 * the given <tt>ContactSourceService</tt>.
 *
 * @author Yana Stamcheva
 */
public class DemuxContactSourceServiceImpl
    extends DemuxContactSourceService
{
    /**
     * Creates a demultiplexed copy of the given <tt>ContactSourceService</tt>,
     * where each contact detail like telephone number or protocol contact
     * address is represented as a single entry in the query result set.
     *
     * @param contactSourceService the original <tt>ContactSourceService</tt> to
     * be demultiplexed
     * @return a demultiplexed copy of the given <tt>ContactSourceService</tt>
     */
    @Override
    public ContactSourceService createDemuxContactSource(
        ContactSourceService contactSourceService)
    {
        return new DemuxContactSource(contactSourceService);
    }
}
