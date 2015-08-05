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
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.muc.*;

/**
 * The Jabber implementation of the <tt>ChatRoomConfigurationForm</tt>
 * interface.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomConfigurationFormJabberImpl
    implements ChatRoomConfigurationForm
{
    /**
     * The logger of this class.
     */
    private Logger logger
        = Logger.getLogger(ChatRoomConfigurationFormJabberImpl.class);

    /**
     * The smack chat room configuration form.
     */
    protected Form smackConfigForm;

    /**
     * The form that will be filled out and submitted by user.
     */
    protected Form smackSubmitForm;

    /**
     * The smack multi user chat is the one to which we'll send the form once
     * filled out.
     */
    private MultiUserChat smackMultiUserChat;

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
        this.smackSubmitForm = smackConfigForm.createAnswerForm();
    }

    /**
     * Returns an Iterator over a list of
     * <tt>ChatRoomConfigurationFormFields</tt>.
     *
     * @return an Iterator over a list of
     * <tt>ChatRoomConfigurationFormFields</tt>
     */
    public Iterator<ChatRoomConfigurationFormField> getConfigurationSet()
    {
        Vector<ChatRoomConfigurationFormField> configFormFields = new Vector<ChatRoomConfigurationFormField>();

        Iterator<FormField> smackFormFields =  smackConfigForm.getFields();

        while(smackFormFields.hasNext())
        {
            FormField smackFormField = smackFormFields.next();

            if(smackFormField == null
                || smackFormField.getType().equals(FormField.TYPE_HIDDEN))
                continue;

            ChatRoomConfigurationFormFieldJabberImpl jabberConfigField
                = new ChatRoomConfigurationFormFieldJabberImpl(
                    smackFormField, smackSubmitForm);

            configFormFields.add(jabberConfigField);
        }

        return Collections.unmodifiableList(configFormFields).iterator();
    }

    /**
     * Sends the ready smack configuration form to the multi user chat.
     */
    public void submit()
        throws OperationFailedException
    {
        if (logger.isTraceEnabled())
            logger.trace("Sends chat room configuration form to the server.");

        try
        {
            smackMultiUserChat.sendConfigurationForm(smackSubmitForm);
        }
        catch (XMPPException e)
        {
            logger.error("Failed to submit the configuration form.", e);

            throw new OperationFailedException(
                "Failed to submit the configuration form.",
                OperationFailedException.GENERAL_ERROR);
        }
    }
}
