/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.util.*;

/**
 * Represents an event coming from RTCP that meant to tell codec
 * to do something (i.e send a keyframe, ...).
 *
 * @author Sebastien Vincent
 */
public class RTCPFeedbackEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Transport layer type (payload type).
     */
    public static final int PT_TL = 205;

    /**
     * Payload-specific type (payload type).
     */
    public static final int PT_PS = 206;

    /**
     * Picture Loss Indication message type.
     */
    public static final int FMT_PLI = 1;

    /**
     * Full Intra-frame Request message type.
     */
    public static final int FMT_FIR = 4;

    /**
     * Feedback message type.
     */
    private int feedbackMessageType = 0;

    /**
     * Payload type.
     */
    private int payloadType = 0;

    /**
     * Constructor.
     *
     * @param src source
     * @param feedbackMessageType FMT
     * @param payloadType PT
     */
    public RTCPFeedbackEvent(Object src, int feedbackMessageType,
            int payloadType)
    {
        super(src);

        this.feedbackMessageType = feedbackMessageType;
        this.payloadType = payloadType;
    }

    /**
     * Get feedback message type.
     *
     * @return message type
     */
    public int getFeedbackMessageType()
    {
        return feedbackMessageType;
    }

    /**
     * Get payload type of RTCP packet.
     *
     * @return payload type
     */
    public int getPayloadType()
    {
        return payloadType;
    }
}

