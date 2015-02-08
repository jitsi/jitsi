/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

import java.awt.*;

public class CefWindowInfo
{
    static
    {
        CefApp.loadLibrary();
    }

    private static native void _delete_(long thiz);

    private static native long _new_();

    private static native void SetAsChild(
            long thiz,
            long hWndParent, int left, int top, int right, int bottom);

    final long ptr;

    public CefWindowInfo()
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

    public void SetAsChild(
            long hWndParent,
            int left, int top, int right, int bottom)
    {
        SetAsChild(ptr, hWndParent, left, top, right, bottom);
    }

    public void SetAsChild(long hWndParent, Rectangle windowRect)
    {
        SetAsChild(
                hWndParent,
                windowRect.x,
                windowRect.y,
                windowRect.x + windowRect.width,
                windowRect.y + windowRect.height);
    }
}
