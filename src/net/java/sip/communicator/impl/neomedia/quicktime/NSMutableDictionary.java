/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents an Objective-C <tt>NSMutableDictionary</tt> object.
 *
 * @author Lubomir Marinov
 */
public class NSMutableDictionary
    extends NSDictionary
{

    /**
     * Initializes a new <tt>NSMutableDictionary</tt> instance which is to
     * represent a new Objective-C <tt>NSMutableDictionary</tt> object.
     */
    public NSMutableDictionary()
    {
        this(allocAndInit());
    }

    /**
     * Initializes a new <tt>NSMutableDictionary</tt> instance which is to
     * represent a specific Objective-C <tt>NSMutableDictionary</tt> object.
     *
     * @param ptr the pointer to the Objective-C <tt>NSMutableDictionary</tt>
     * object to be represented by the new instance
     */
    public NSMutableDictionary(long ptr)
    {
        super(ptr);
    }

    private static native long allocAndInit();

    public void setIntForKey(int value, long key)
    {
        setIntForKey(getPtr(), value, key);
    }

    private static native void setIntForKey(long ptr, int value, long key);
}
