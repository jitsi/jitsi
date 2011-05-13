/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Provides a base implementation of <tt>PullBufferStream</tt> in order to
 * facilitate implementers by taking care of boilerplate in the most common
 * cases.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractPullBufferStream
    extends AbstractBufferStream
    implements PullBufferStream
{

    /**
     * Initializes a new <tt>AbstractPullBufferStream</tt> instance which is to
     * have its <tt>Format</tt>-related information abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param dataSource the <tt>PullBufferDataSource</tt> which is creating the
     * new instance so that it becomes one of its <tt>streams</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     */
    protected AbstractPullBufferStream(
            PullBufferDataSource dataSource,
            FormatControl formatControl)
    {
        super(dataSource, formatControl);
    }

    /**
     * Determines if {@link #read(Buffer)} will block.
     *
     * @return <tt>true</tt> if read block, <tt>false</tt> otherwise
     */
    public boolean willReadBlock()
    {
        return true;
    }
}
