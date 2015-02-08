/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

/**
 * Defines the reference count implementation methods within Chromium Embedded
 * Framework (CEF). All framework classes must extend the <tt>CefBase</tt>
 * class.
 *
 * @author Lyubomir Marinov
 */
public abstract class CefBase
{
    /**
     * The native/unmanaged <tt>CefBase</tt> implementation associated with and
     * represented by this instance.
     */
    private long ptr;

    /** Initializes a new <tt>CefBase</tt> instance. */
    protected CefBase()
    {
        ptr = _alloc_();
        if (ptr == 0)
        {
            throw new OutOfMemoryError();
        }
        else
        {
            ptr = _init_();
            if (ptr == 0)
                throw new IllegalStateException("ptr");
        }
    }

    /**
     * Allocates a native/unmanaged <tt>CefBase</tt> implementation to be
     * associated with this instance.
     *
     * @return the native/unmanaged <tt>CefBase</tt> implementation to be
     * associated with this instance
     */
    protected abstract long _alloc_();

    /**
     * Initializes the native/unmanaged <tt>CefBase</tt> implementation
     * associated with this instance.
     *
     * @return the native/unmanaged <tt>CefBase</tt> implementation to be
     * associated with this instance
     */
    protected long _init_()
    {
        return _init_(getPtr());
    }

    /**
     * Initializes the native/unmanaged <tt>CefBase</tt> implementation
     * associated with this instance.
     *
     * @param ptr the native/unmanaged <tt>CefBase</tt> implementation
     * associated with this instance that is to be initialized
     * @return the native/unmanaged <tt>CefBase</tt> implementation to be
     * associated with this instance
     */
    private native long _init_(long ptr);

    /**
     * Increments the reference count of the <tt>CefBase</tt> implementation
     * associated with and represented by this instance. The method should be
     * called for every new copy of a pointer to the object.
     *
     * @return the resulting reference count value. The returned value should be
     * used for diagnostic/testing purposes only.
     */
    public int AddRef()
    {
        return AddRef(getPtr());
    }

    /**
     * Increments the reference count of a specific <tt>CefBase</tt>
     * implementation. The method should be called for every new copy of a
     * pointer to the object.
     *
     * @return the resulting reference count value. The returned value should be
     * used for diagnostic/testing purposes only.
     */
    private native int AddRef(long ptr);

    /**
     * Gets the native/unmanaged <tt>CefBase</tt> implementation associated with
     * and represented by this instance.
     *
     * @return the native/unmanaged <tt>CefBase</tt> implementation associated
     * with and represented by this instance
     * @throws IllegalStateException if this instance is not associated with any
     * <tt>CefBase</tt> implementation
     */
    protected synchronized long getPtr()
    {
        long ptr = this.ptr;

        if (ptr == 0)
            throw new IllegalStateException("ptr");
        else
            return ptr;
    }

    public int GetRefCt()
    {
        return GetRefCt(getPtr());
    }

    private native int GetRefCt(long ptr);

    public int Release()
    {
        return Release(getPtr());
    }

    private native int Release(long ptr);
}
