/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

/**
 * The <tt>DemuxContactSourceService</tt> provides a de-multiplexed copy of
 * the given <tt>ContactSourceService</tt>, where each contact detail like
 * telephone number or protocol contact address is represented as a single entry
 * in the query result set.
 *
 * @author Yana Stamcheva
 */
public abstract class DemuxContactSourceService
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
    public abstract ContactSourceService createDemuxContactSource(
        ContactSourceService contactSourceService);
}
