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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.neomedia.*;

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

        this.load();
    }

    /**
     * Saves values to account properties.
     */
    @Override
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
    @Override
    protected boolean satisfyAutoAnswerConditions(Call call)
    {
        // The jabber implementation does not support advanced auto answer
        // functionalities. We only need to check if the specific Call object
        // knows it has to be auto-answered.
        return call.isAutoAnswer();
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
