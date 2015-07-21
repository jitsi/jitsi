/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

/**
 * Utility class for awt windows management.
 *
 * @author Yana Stamcheva
 */
public class WindowUtils
{
    /**
     * The list of all <tt>Window</tt>s owned by this application.
     */
    private static final List<Window> WINDOW_LIST;

    static
    {
        /*
         * WINDOW_LIST is flawed because there are more calls to addWindow than
         * to removeWindow. Java 6 has introduced Window#getWindows so try to
         * use it instead.
         */
        Method Window_getWindows = null;

        try
        {
            Window_getWindows = Window.class.getMethod("getWindows");
        }
        catch (NoSuchMethodException nsme)
        {
            /*
             * Ignore the exception because we are just checking whether the
             * method exists.
             */
        }
        catch (SecurityException se)
        {
        }
        WINDOW_LIST
            = (Window_getWindows == null) ? new ArrayList<Window>() : null;
    }

    /**
     * Returns an array of all {@code Window}s, both owned and ownerless,
     * created by this application.
     * If called from an applet, the array includes only the {@code Window}s
     * accessible by that applet.
     * <p>
     * <b>Warning:</b> this method may return system created windows, such
     * as a print dialog. Applications should not assume the existence of
     * these dialogs, nor should an application assume anything about these
     * dialogs such as component positions, <code>LayoutManager</code>s
     * or serialization.
     *
     * @return Returns an array of all {@code Window}s.
     */
    public static Window[] getWindows()
    {
        if (WINDOW_LIST == null)
        {
            Method Window_getWindows = null;

            try
            {
                Window_getWindows = Window.class.getMethod("getWindows");
            }
            catch (NoSuchMethodException nsme)
            {
                /* Ignore it because we cannot really do anything useful. */
            }
            catch (SecurityException se)
            {
            }

            Object windows = null;

            if (Window_getWindows != null)
            {
                try
                {
                    windows = Window_getWindows.invoke(null);
                }
                catch (ExceptionInInitializerError eiie)
                {
                    /* Ignore it because we cannot really do anything useful. */
                }
                catch (IllegalAccessException iae)
                {
                }
                catch (IllegalArgumentException iae)
                {
                }
                catch (InvocationTargetException ite)
                {
                }
                catch (NullPointerException npe)
                {
                }
            }

            return
                (windows instanceof Window[])
                    ? (Window[]) windows
                    : new Window[0];
        }
        else
        {
            synchronized (WINDOW_LIST)
            {
                return WINDOW_LIST.toArray(new Window[WINDOW_LIST.size()]);
            }
        }
    }

    /**
     * Adds a {@link Window} into window list
     *
     * @param w {@link Window} to be added.
     */
    public static void addWindow(Window w)
    {
        if (WINDOW_LIST != null)
        {
            synchronized (WINDOW_LIST)
            {
                if (!WINDOW_LIST.contains(w))
                    WINDOW_LIST.add(w);
            }
        }
    }

    /**
     * Removes a {@link Window} into window list
     *
     * @param w {@link Window} to be removed.
     */
    public static void removeWindow(Window w)
    {
        if (WINDOW_LIST != null)
        {
            synchronized (WINDOW_LIST)
            {
                WINDOW_LIST.remove(w);
            }
        }
    }

}
