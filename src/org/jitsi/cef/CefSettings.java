/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

public class CefSettings
{
    static
    {
        CefApp.loadLibrary();
    }

    private static native void _delete_(long thiz);

    private static native long _new_();

    private static native String getLocalesDirPath(long thiz);

    private static native boolean isMultiThreadedMessageLoop(long thiz);

    private static native boolean isSingleProcess(long thiz);

    private static native void setLocalesDirPath(long thiz, String s);

    private static native void setMultiThreadedMessageLoop(
            long thiz,
            boolean b);

    private static native void setSingleProcess(long thiz, boolean b);

    final long ptr;

    /** Initializes a new <tt>CefSettings</tt> instance. */
    public CefSettings()
    {
        ptr = _new_();
        if (ptr == 0)
            throw new OutOfMemoryError();
    }

    @Override
    protected void finalize()
        throws Throwable
    {
        try
        {
            _delete_(ptr);
        }
        finally
        {
            super.finalize();
        }
    }

    public String getLocalesDirPath()
    {
        return getLocalesDirPath(ptr);
    }

    public boolean isMultiThreadedMessageLoop()
    {
        return isMultiThreadedMessageLoop(ptr);
    }

    public boolean isSingleProcess()
    {
        return isSingleProcess(ptr);
    }

    public void setLocalesDirPath(String s)
    {
        setLocalesDirPath(ptr, s);
    }

    public void setMultiThreadedMessageLoop(boolean b)
    {
        setMultiThreadedMessageLoop(ptr, b);
    }

    public void setSingleProcess(boolean b)
    {
        setSingleProcess(ptr, b);
    }
}
