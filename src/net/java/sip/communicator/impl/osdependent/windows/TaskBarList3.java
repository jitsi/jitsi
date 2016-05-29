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

import java.awt.*;
import java.awt.image.*;

import org.jitsi.util.*;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.COM.*;
import com.sun.jna.platform.win32.Guid.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinNT.*;
import com.sun.jna.ptr.*;

/**
 * JNA wrapper for the ITaskBarList3 COM interface.
 * https://msdn.microsoft.com/en-us/library/dd391696(v=vs.85).aspx
 * 
 * @author Ingo Bauersachs
 */
public class TaskBarList3
    extends Unknown
{
    private static final GUID CLSID_TaskbarList =
        new GUID("{56FDF344-FD6D-11d0-958A-006097C9A090}");

    private static final GUID IID_ITaskbarList3 =
        new GUID("{ea1afb91-9e28-4b86-90e9-9e9f8a5eefaf}");

    private static TaskBarList3 instance;

    /**
     * Gets the ITaskBarList3 interface and initializes it with HrInit
     * @return A ready to use TaskBarList3 object.
     * @throws COMException when the interface could not be accessed
     */
    public static TaskBarList3 getInstance()
    {
        if (instance == null && OSUtils.IS_WINDOWS)
        {
            Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, 0);
            PointerByReference p = new PointerByReference();
            WinNT.HRESULT hr =
                Ole32.INSTANCE.CoCreateInstance(CLSID_TaskbarList, Pointer.NULL,
                    ObjBase.CLSCTX_ALL, IID_ITaskbarList3, p);
            COMUtils.checkRC(hr);
            instance = new TaskBarList3(p.getValue());
        }

        return instance;
    }

    private TaskBarList3(Pointer p)
    {
        super(p);
        HrInit();
    }

    // VTable
    // ------
    // IUnknown:
    // 0: AddRef
    // 1: QueryInterface
    // 2: Release
    //
    // ITaskBarList:
    // 3: HrInit
    // 4: AddTab
    // 5: DeleteTab
    // 6: ActivateTab
    // 7: SetActiveAlt
    //
    // ITaskBarList2
    // 8: MarkFullscreenWindow
    //
    // ITaskBarList3:
    // 9: SetProgressValue
    // 10: SetProgressState
    // 11: RegisterTab
    // 12: UnregisterTab
    // 13: SetTabOrder
    // 14: SetTabActive
    // 15: ThumbBarAddButtons
    // 16: ThumbBarAddButtons
    // 17: ThumbBarSetImageList
    // 18: SetOverlayIcon
    // 19: SetThumbnailTooltip
    // 20: SetThumbnailClip
    //
    // ITaskbarList4:
    // 21: SetTabProperties

    /**
     * https://msdn.microsoft.com/en-us/library/bb774650(v=vs.85).aspx
     */
    private void HrInit()
    {
        int hr = this._invokeNativeInt(3, new Object[]
        { this.getPointer() });
        COMUtils.checkRC(new HRESULT(hr));
    }

    /**
     * https://msdn.microsoft.com/en-us/library/dd391696(v=vs.85).aspx
     */
    private void SetOverlayIcon(HWND hwnd, HICON hIcon, String pszDescription)
    {
        int hr = this._invokeNativeInt(18, new Object[]
        { this.getPointer(), hwnd, hIcon, pszDescription });
        COMUtils.checkRC(new HRESULT(hr));
    }

    /**
     * Sets an overlay image to the taskbar icon.
     * @param frame The window that should receive the overlay
     * @param image The overlay image, can be <tt>null</tt> to clear the overlay
     * @param description An optional tooltip text, can be <tt>null</tt>
     */
    public void SetOverlayIcon(Component frame, BufferedImage image,
        String description)
    {
        HICON ico = null;
        if (image != null)
        {
            byte[] iconBytes = ImageConverter.writeTransparentIcoImage(image);
            ico = ImageConverter.createIcon(iconBytes);
        }

        HWND hwnd = new HWND(Native.getComponentPointer(frame));
        SetOverlayIcon(hwnd, ico, description);
    }
}
