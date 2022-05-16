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
package net.java.sip.communicator.impl.neomedia;

import java.util.*;
import javax.media.*;
import org.jitsi.impl.neomedia.device.*;

/**
 * Encapsulates a <tt>CaptureDeviceInfo</tt> for the purposes of its display in
 * the user interface.
 */
public class CaptureDeviceViewModel
{
    /**
     * The encapsulated info.
     */
    public final CaptureDeviceInfo info;

    /**
     * Creates the wrapper.
     *
     * @param info the info object we wrap.
     */
    public CaptureDeviceViewModel(CaptureDeviceInfo info)
    {
        this.info = info;
    }

    /**
     * Determines whether the <tt>CaptureDeviceInfo</tt> encapsulated by this
     * instance is equal (by value) to a specific
     * <tt>CaptureDeviceInfo</tt>.
     *
     * @param cdi the <tt>CaptureDeviceInfo</tt> to be determined whether it is
     *            equal (by value) to the <tt>CaptureDeviceInfo</tt>
     *            encapsulated by this instance
     * @return <tt>true</tt> if the <tt>CaptureDeviceInfo</tt> encapsulated
     * by this instance is equal (by value) to the specified <tt>cdi</tt>;
     * otherwise, <tt>false</tt>
     */
    public boolean equals(CaptureDeviceInfo cdi)
    {
        return Objects.equals(info, cdi);
    }

    /**
     * Gets a human-readable <tt>String</tt> representation of this instance.
     *
     * @return a <tt>String</tt> value which is a human-readable representation
     * of this instance
     */
    @Override
    public String toString()
    {
        String s;

        if (info == null)
        {
            s
                = NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.NO_DEVICE");
        }
        else
        {
            s = info.getName();
            if (info instanceof CaptureDeviceInfo2)
            {
                String transportType
                    = ((CaptureDeviceInfo2) info).getTransportType();

                if (transportType != null)
                {
                    s += " (" + transportType + ")";
                }
            }
        }
        return s;
    }
}
