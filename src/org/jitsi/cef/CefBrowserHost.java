/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

public class CefBrowserHost
{
    public static boolean CreateBrowser(
            CefWindowInfo windowInfo,
            CefClient client,
            String url,
            CefBrowserSettings settings)
    {
        return
            CreateBrowser(
                    windowInfo.ptr,
                    (client == null) ? 0 : client.getPtr(),
                    url,
                    settings.ptr);
    }

    private static native boolean CreateBrowser(
            long windowInfo,
            long client,
            String url,
            long settings);

    static long CreateBrowserSync(
            CefWindowInfo windowInfo,
            CefClient client,
            String url,
            CefBrowserSettings settings)
    {
        return
            CreateBrowserSync(
                    windowInfo.ptr,
                    (client == null) ? 0 : client.getPtr(),
                    url,
                    settings.ptr);
    }

    private static native long CreateBrowserSync(
            long windowInfo,
            long client,
            String url,
            long settings);
}
