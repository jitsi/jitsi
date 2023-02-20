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
package net.java.sip.communicator.plugin.chatalerter;

import com.sun.jna.*;
import com.sun.jna.platform.unix.*;
import com.sun.jna.platform.unix.X11.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;
import java.awt.*;
import java.awt.Taskbar.*;
import java.awt.event.*;
import org.apache.commons.lang3.*;

public abstract class Alert
{
    interface X11Extended
        extends X11
    {
        X11Extended INSTANCE = Native.load("X11", X11Extended.class);

        int XSetWMHints(Display display, X11.Window w, XWMHints wmhints);
    }

    public static Alert newInstance()
    {
        if (SystemUtils.IS_OS_MAC)
        {
            return new MacAlert();
        }
        else if (SystemUtils.IS_OS_WINDOWS)
        {
            return new Alert()
            {
                @Override
                public void alert(Frame frame)
                {
                    if (!frame.isActive())
                    {
                        HWND hwnd = new HWND(Native.getComponentPointer(frame));
                        WinUser.FLASHWINFO fwInfo = new FLASHWINFO();
                        fwInfo.dwFlags =
                            User32.FLASHW_ALL | User32.FLASHW_TIMERNOFG;
                        fwInfo.uCount = Integer.MAX_VALUE;
                        fwInfo.dwTimeout = 0;
                        fwInfo.hWnd = hwnd;
                        User32.INSTANCE.FlashWindowEx(fwInfo);
                    }
                }
            };
        }
        else
        {
            return new XAlert();
        }
    }

    public abstract void alert(Frame frame);

    private static class MacAlert extends Alert
    {
        private Taskbar taskbar;

        private MacAlert()
        {
            if (Taskbar.isTaskbarSupported())
            {
                taskbar = Taskbar.getTaskbar();
                if (!taskbar.isSupported(Feature.USER_ATTENTION))
                {
                    taskbar = null;
                }
            }
        }

        @Override
        public void alert(Frame frame)
        {
            if (taskbar != null)
            {
                taskbar.requestWindowUserAttention(frame);
            }
        }
    }

    private static class XAlert
        extends Alert
    {
        private static final Display display = new Display();

        @Override
        public void alert(Frame frame)
        {
            if (!frame.isActive())
            {
                frame.addWindowListener(new WindowAdapter()
                {
                    public void windowActivated(WindowEvent e)
                    {
                        Frame frame = (Frame) e.getSource();
                        frame.removeWindowListener(this);
                        setUrgencyHint(frame, false);
                    }
                });
                setUrgencyHint(frame, true);
            }
        }

        private static void setUrgencyHint(Frame frame, boolean alert)
        {
            final long XUrgencyHint = 1 << 8L;
            XWMHints hints = null;
            try
            {
                X11Extended.Window w =
                    new X11Extended.Window(Native.getComponentID(frame));
                hints = X11Extended.INSTANCE.XGetWMHints(display, w);
                if (alert)
                {
                    hints.flags
                        .setValue(hints.flags.longValue() | XUrgencyHint);
                }
                else
                {
                    hints.flags
                        .setValue(hints.flags.longValue() ^ XUrgencyHint);
                }
                X11Extended.INSTANCE.XSetWMHints(display, w, hints);
            }
            finally
            {
                if (hints != null)
                {
                    X11Extended.INSTANCE.XFree(hints.getPointer());
                }
            }
        }
    }
}
