/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import javax.media.protocol.*;

/**
 * Represents a filter which determines whether a specific <tt>DataSource</tt>
 * is to be selected or deselected by the caller of the filter.
 *
 * @author Lubomir Marinov
 */
public interface DataSourceFilter
{

    /**
     * Determines whether a specific <tt>DataSource</tt> is accepted by this
     * filter i.e. whether the caller of this filter should include it in its
     * selection.
     *
     * @param dataSource the <tt>DataSource</tt> to be checked whether it is
     * accepted by this filter
     * @return <tt>true</tt> if this filter accepts the specified
     * <tt>DataSource</tt> i.e. if the caller of this filter should include it
     * in its selection; otherwise, <tt>false</tt>
     */
    public boolean accept(DataSource dataSource);
}
