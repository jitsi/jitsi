/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Defines an <tt>Exception</tt> which reports an <tt>NSError</tt>.
 *
 * @author Lubomir Marinov
 */
public class NSErrorException
    extends Exception
{

    /**
     * The <tt>NSError</tt> reported by this instance.
     */
    private final NSError error;

    /**
     * Initializes a new <tt>NSErrorException</tt> instance which is to report a
     * specific Objective-C <tt>NSError</tt>.
     *
     * @param errorPtr the pointer to the Objective-C <tt>NSError</tt> object to
     * be reported by the new instance
     */
    public NSErrorException(long errorPtr)
    {
        this(new NSError(errorPtr));
    }

    /**
     * Initializes a new <tt>NSErrorException</tt> instance which is to report a
     * specific <tt>NSError</tt>.
     *
     * @param error the <tt>NSError</tt> to be reported by the new instance
     */
    public NSErrorException(NSError error)
    {
        this.error = error;
    }

    /**
     * Gets the <tt>NSError</tt> reported by this instance.
     *
     * @return the <tt>NSError</tt> reported by this instance
     */
    public NSError getError()
    {
        return error;
    }
}
