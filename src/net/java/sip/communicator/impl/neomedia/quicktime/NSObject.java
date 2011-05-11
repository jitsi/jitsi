/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents the root of most Objective-C class hierarchies which which objects
 * inherit a basic interface to the runtime system and the ability to behave as
 * Objective-C objects.
 *
 * @author Lyubomir Marinov
 */
public class NSObject
{
    static
    {
        System.loadLibrary("jnquicktime");
    }

    /**
     * The pointer to the Objective-C object represented by this instance.
     */
    private long ptr;

    /**
     * Initializes a new <tt>NSObject</tt> instance which is to represent a
     * specific Objective-C object.
     *
     * @param ptr the pointer to the Objective-C object to be represented by the
     * new instance
     */
    public NSObject(long ptr)
    {
        setPtr(ptr);
    }

    /**
     * Gets the pointer to the Objective-C object represented by this instance.
     *
     * @return the pointer to the Objective-C object represented by this
     * instance
     */
    protected long getPtr()
    {
        return ptr;
    }

    /**
     * Decrements the reference count of the Objective-C object represented by
     * this instance. It is sent a <tt>dealloc</tt> message when its reference
     * count reaches <tt>0</tt>.
     */
    public void release()
    {
        release(ptr);
    }

    /**
     * Decrements the reference count of a specific Objective-C object. It is
     * sent a <tt>dealloc</tt> message when its reference count reaches
     * <tt>0</tt>.
     *
     * @param ptr the pointer to the Objective-C object to decrement the
     * reference count of
     */
    static native void release(long ptr);

    /**
     * Increments the reference count of the Objective-C object represented by
     * this instance.
     */
    public void retain()
    {
        retain(ptr);
    }

    /**
     * Increments the reference count of a specific Objective-C object.
     *
     * @param ptr the pointer to be Objective-C object to increment the
     * reference count of
     */
    static native void retain(long ptr);

    /**
     * Sets the pointer to the Objective-C object represented by this instance.
     *
     * @param ptr the pointer to the Objective-C object to be represented by
     * this instance
     */
    protected void setPtr(long ptr)
    {
        if (ptr == 0)
            throw new IllegalArgumentException("ptr");

        this.ptr = ptr;
    }
}
