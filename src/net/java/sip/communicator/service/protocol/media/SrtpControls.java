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
package net.java.sip.communicator.service.protocol.media;

import org.jitsi.service.neomedia.*;

/**
 * Represents a sorted set of <tt>SrtpControl</tt> implementations.
 *
 * @author Lyubomir Marinov
 */
public class SrtpControls
{
    private static final SrtpControlType[] SORTED_SRTP_CONTROL_TYPES
        = {
            SrtpControlType.ZRTP,
            SrtpControlType.DTLS_SRTP,
            SrtpControlType.MIKEY,
            SrtpControlType.SDES
        };

    /**
     * The <tt>SrtpControl</tt> implementations which are the elements of this
     * sorted set.
     */
    private final SrtpControl[][] elements
        = new SrtpControl
            [MediaType.values().length]
                [SrtpControlType.values().length];

    /**
     * Initializes a new <tt>SrtpControls</tt> instance.
     */
    public SrtpControls()
    {
    }

    public SrtpControl findFirst(MediaType mediaType)
    {
        SrtpControl element = null;

        for (SrtpControlType srtpControlType : SORTED_SRTP_CONTROL_TYPES)
        {
            element = get(mediaType, srtpControlType);
            if (element != null)
                break;
        }
        return element;
    }

    public SrtpControl get(MediaType mediaType, SrtpControlType srtpControlType)
    {
        return elements[mediaType.ordinal()][srtpControlType.ordinal()];
    }

    public SrtpControl getOrCreate(
            MediaType mediaType,
            SrtpControlType srtpControlType)
    {
        SrtpControl[] elements = this.elements[mediaType.ordinal()];
        int index = srtpControlType.ordinal();
        SrtpControl element = elements[index];

        if (element == null)
        {
            element
                = ProtocolMediaActivator.getMediaService().createSrtpControl(
                        srtpControlType);
            if (element != null)
                elements[index] = element;
        }
        return element;
    }

    public SrtpControl remove(
            MediaType mediaType,
            SrtpControlType srtpControlType)
    {
        SrtpControl[] elements = this.elements[mediaType.ordinal()];
        int index = srtpControlType.ordinal();
        SrtpControl element = elements[index];

        elements[index] = null;
        return element;
    }

    public void set(MediaType mediaType, SrtpControl element)
    {
        SrtpControlType srtpControlType = element.getSrtpControlType();

        elements[mediaType.ordinal()][srtpControlType.ordinal()] = element;
    }
}
