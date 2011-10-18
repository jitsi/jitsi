/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents an Objective-C <tt>NSError</tt> object.
 *
 * @author Lubomir Marinov
 */
public class NSError
    extends NSObject
{
    /**
     * Initializes a new <tt>NSError</tt> instance which is to represent a
     * specific Objective-C <tt>NSError</tt> object.
     *
     * @param ptr the pointer to the Objective-C <tt>NSError</tt> object to be
     * represented by the new instance
     */
    public NSError(long ptr)
    {
        super(ptr);
    }

    /**
     * Called by the garbage collector to release system resources and perform
     * other cleanup.
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize()
    {
        release();
    }
}
