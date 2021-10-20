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

import lombok.extern.slf4j.*;
import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.xdata.*;
import org.jivesoftware.smackx.xdata.form.*;

/**
 * The Jabber implementation of the <tt>ChatRoomConfigurationForm</tt>
 * interface.
 *
 * @author Yana Stamcheva
 */
@Slf4j
public class ChatRoomConfigurationFormJabberImpl
    implements ChatRoomConfigurationForm
{
    private final  List<ChatRoomConfigurationFormFieldJabberImpl> configFormFields
        = new ArrayList<>();

    /**
     * The smack chat room configuration form.
     */
    protected Form smackConfigForm;

    /**
     * The smack multi user chat is the one to which we'll send the form once
     * filled out.
     */
    private final MultiUserChat smackMultiUserChat;

    /**
     * Creates an instance of <tt>ChatRoomConfigurationFormJabberImpl</tt> by
     * specifying the corresponding smack multi user chat and smack
     * configuration form.
     *
     * @param multiUserChat the smack multi user chat, to which we'll send the
     * configuration form once filled out
     * @param smackConfigForm the smack configuration form
     */
    public ChatRoomConfigurationFormJabberImpl(
        MultiUserChat multiUserChat, Form smackConfigForm)
    {
        this.smackMultiUserChat = multiUserChat;
        this.smackConfigForm = smackConfigForm;

        for (FormField smackFormField
            : smackConfigForm.getDataForm().getFields())
        {
            if(smackFormField == null
                || smackFormField.getType().equals(FormField.Type.hidden))
                continue;

            ChatRoomConfigurationFormFieldJabberImpl jabberConfigField
                = new ChatRoomConfigurationFormFieldJabberImpl(
                smackFormField);

            configFormFields.add(jabberConfigField);
        }
    }

    /**
     * Returns an Iterator over a list of
     * <tt>ChatRoomConfigurationFormFields</tt>.
     *
     * @return an Iterator over a list of
     * <tt>ChatRoomConfigurationFormFields</tt>
     */
    public List<ChatRoomConfigurationFormField> getConfigurationSet()
    {
        return Collections.unmodifiableList(configFormFields);
    }

    /**
     * Sends the ready smack configuration form to the multi user chat.
     */
    public void submit() throws OperationFailedException
    {
        logger.trace("Sends chat room configuration form to the server.");
        try
        {
            FillableForm submitForm = smackConfigForm.getFillableForm();
            for (ChatRoomConfigurationFormFieldJabberImpl f : configFormFields)
            {
                if (f.getType().equals(
                    ChatRoomConfigurationFormField.TYPE_TEXT_FIXED))
                {
                    continue;
                }

                if (f.getValues().size() == 1)
                {
                    Object value = f.getValues().get(0);
                    if (value instanceof Integer)
                    {
                        submitForm.setAnswer(f.getName(), (Integer) value);
                    }
                    else if (value instanceof Boolean)
                    {
                        submitForm.setAnswer(f.getName(), (Boolean) value);
                    }
                    else if (value instanceof CharSequence)
                    {
                        submitForm.setAnswer(f.getName(), (CharSequence) value);
                    }
                }
                else
                {
                    submitForm.setAnswer(f.getName(), f.getValuesAsString());
                }
            }
            smackMultiUserChat.sendConfigurationForm(submitForm);
        }
        catch (XMPPException
                | SmackException.NoResponseException
                | SmackException.NotConnectedException
                | InterruptedException e)
        {
            logger.error("Failed to submit the configuration form.", e);

            throw new OperationFailedException(
                "Failed to submit the configuration form.",
                OperationFailedException.GENERAL_ERROR);
        }
    }
}
