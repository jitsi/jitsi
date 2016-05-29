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
package net.java.sip.communicator.impl.osdependent.windows;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.*;

/**
 * Extension to missing user32 Windows APIs
 * 
 * @author Ingo Bauersachs
 */
interface User32Ex
    extends StdCallLibrary
{
    User32Ex INSTANCE = (User32Ex) Native.loadLibrary("user32", User32Ex.class,
        W32APIOptions.DEFAULT_OPTIONS);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms648074(v=vs.85).aspx
     */
    int LookupIconIdFromDirectoryEx(Memory presbits, boolean fIcon,
        int cxDesired, int cyDesired, int Flags);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms648061(v=vs.85).aspx
     */
    WinDef.HICON CreateIconFromResourceEx(Pointer pbIconBits,
        WinDef.DWORD cbIconBits, boolean fIcon, WinDef.DWORD dwVersion,
        int cxDesired, int cyDesired, int uFlags);
}