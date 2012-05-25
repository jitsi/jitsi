/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import java.util.*;

/**
 * An Operation Set defining option to unconditionally auto answer incoming
 * calls.
 *
 * @author Damian Minkov
 * @author Vincent Lucas
 */
public class OperationSetAutoAnswerJabberImpl
    extends AbstractOperationSetBasicAutoAnswer
{
    /**
     * Creates this operation set, loads stored values, populating
     * local variable settings.
     *
     * @param protocolProvider the parent Protocol Provider.
     */
    public OperationSetAutoAnswerJabberImpl(
            ProtocolProviderServiceJabberImpl protocolProvider)
    {
        super(protocolProvider);
    }

    /**
     * Saves values to account properties.
     */
    protected void save()
    {
        AccountID acc = protocolProvider.getAccountID();
        Map<String, String> accProps = acc.getAccountProperties();

        // lets clear anything before saving :)
        accProps.put(AUTO_ANSWER_UNCOND_PROP, null);

        if(answerUnconditional)
            accProps.put(AUTO_ANSWER_UNCOND_PROP, Boolean.TRUE.toString());

        accProps.put(AUTO_ANSWER_WITH_VIDEO_PROP,
                Boolean.toString(this.answerWithVideo));

        acc.setAccountProperties(accProps);
        JabberActivator.getProtocolProviderFactory().storeAccount(acc);
    }

    /**
     * Checks if the call satisfy the auto answer conditions.
     *
     * @param call The new incoming call to auto-answer if needed.
     *
     * @return <tt>true</tt> if the call satisfy the auto answer conditions.
     * <tt>False</tt> otherwise.
     */
    protected boolean satisfyAutoAnswerConditions(Call call)
    {
        // Nothing to do here, as long as the jabber account does not implements
        // advanced auto answer functionnalities.
        return false;
    }

    /**
     * Auto-answers to a call with "audio only" or "audio/video" if the incoming
     * call is a video call.
     *
     * @param call The new incoming call to auto-answer if needed.
     * @param directions The media type (audio / video) stream directions.
     *
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    public boolean autoAnswer(
            Call call,
            Map<MediaType, MediaDirection> directions)
    {
        boolean isVideoCall = false;
        MediaDirection direction = directions.get(MediaType.VIDEO);

        if(direction != null)
        {
            isVideoCall = (direction == MediaDirection.SENDRECV);
        }

        return super.autoAnswer(call, isVideoCall);
    }
}
