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
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.dict4j.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Instant messaging functionalities for the Dict protocol.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class OperationSetBasicInstantMessagingDictImpl
    extends AbstractOperationSetBasicInstantMessaging
    implements RegistrationStateChangeListener
{
    /**
     * The currently valid persistent presence operation set.
     */
    private OperationSetPersistentPresenceDictImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceDictImpl parentProvider = null;

    private DictAccountID accountID;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     * @param opSetPersPresence the currently valid
     * <tt>OperationSetPersistentPresenceDictImpl</tt> instance.
     */
    public OperationSetBasicInstantMessagingDictImpl(
                ProtocolProviderServiceDictImpl        provider,
                OperationSetPersistentPresenceDictImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
        this.accountID = (DictAccountID) provider.getAccountID();

        parentProvider.addRegistrationStateChangeListener(this);
    }

    @Override
    public Message createMessage(String content)
    {
        return new MessageDictImpl(content, HTML_MIME_TYPE,
                DEFAULT_MIME_ENCODING, null);
    }

    @Override
    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MessageDictImpl(content, contentType, encoding, subject);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying ICQ stack is not
     *   registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *   belonging to the underlying implementation.
     */
    public void sendInstantMessage(Contact to, Message message)
        throws  IllegalStateException,
                IllegalArgumentException
    {
        if( !(to instanceof ContactDictImpl) )
        {
           throw new IllegalArgumentException(
               "The specified contact is not a Dict contact."
               + to);
        }

        // Remove all html tags from the message
        message = createMessage(Html2Text.extractText(message.getContent()));

        // Display the queried word
        fireMessageDelivered(message, to);

        this.submitDictQuery((ContactDictImpl) to, message);
    }

    /**
     * Determines whether the protocol provider (or the protocol itself) supports
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return true even when offline messaging is not
     * supported, and then have the sendMessage method throw an
     * OperationFailedException with code - OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return false;
    }

    /**
     * Determines whether the protocol supports the supplied content type.
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else if(contentType.equals(HTML_MIME_TYPE))
            return true;
        else
           return false;
    }

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderServiceDictImpl</tt>
     * instance that this operation set belongs to.
     */
    public ProtocolProviderServiceDictImpl getParentProvider()
    {
        return this.parentProvider;
    }

    /**
     * Returns a reference to the presence operation set instance used by our
     * source provider.
     *
     * @return a reference to the <tt>OperationSetPersistentPresenceDictImpl</tt>
     * instance used by this provider.
     */
    public OperationSetPersistentPresenceDictImpl getOpSetPersPresence()
    {
        return this.opSetPersPresence;
    }

    /**
     * The method is called by the ProtocolProvider whenever a change in the
     * registration state of the corresponding provider has occurred.
     *
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {

    }


    /**
     * Create, execute and display a query to a dictionary (ContactDictImpl)
     *
     * @param dictContact the contact containing the database name
     * @param message the message containing the word
     */
    private void submitDictQuery(ContactDictImpl dictContact, Message message)
    {
        Message msg = this.createMessage("");

        String database = dictContact.getContactID();
        DictConnection conn = this.parentProvider.getConnection();
        boolean doMatch = false;

        String word;

        // Formatting the query message, if the word as one or more spaces we
        // put it between quotes to prevent errors
        word = message.getContent().replace("\"", "").trim();
        if (word.indexOf(' ') > 0)
        {
            word = "\"" + word + "\"";
        }

        // Try to get the definition of the work
        try
        {
            List<Definition> definitions = conn.define(database, word);
            msg = this.createMessage(retrieveDefine(definitions, word));
        }
        catch(DictException dx)
        {
            if (dx.getErrorCode() == DictReturnCode.NO_MATCH)
            { // No word found, we are going to try the match command
                doMatch = true;
            }
            else
            { // Otherwise we display the error returned by the server
                msg = this.createMessage(manageException(dx, database));
            }
        }

        if (doMatch)
        {
            // Trying the match command
            try
            {
                List<MatchWord> matchWords = conn.match(database, word,
                        this.accountID.getStrategy());
                msg = this.createMessage(retrieveMatch(matchWords, word));
            }
            catch(DictException dx)
            {
                msg = this.createMessage(manageException(dx, database));
            }
        }

        // Send message
        fireMessageReceived(msg, dictContact);
    }

    /**
     * Generate the display of the results of the Define command
     *
     * @param data the result of the Define command
     * @param word the queried word
     * @return the formatted result
     */
    private String retrieveDefine(List<Definition> data, String word)
    {
        StringBuffer res = new StringBuffer();
        Definition def;

        for (int i=0; i<data.size(); i++)
        {
            def = data.get(i);

            if(i != 0 && data.size() > 0)
            {
                res.append("<hr>");
            }
            res.append(def.getDefinition().replaceAll("\n", "<br>"))
                .append("<div align=\"right\"><font size=\"-2\">-- From ")
                .append(def.getDictionary())
                .append("</font></div>");
        }

        String result = res.toString();
        result = formatResult(result, "\\\\", "<em>", "</em>");
        result = formatResult(result, "[\\[\\]]", "<cite>", "</cite>");
        result = formatResult(result, "[\\{\\}]", "<strong>", "</strong>");
        result = formatWordDefined(result, word);

        return result;
    }

    /**
     * Makes a stronger emphasis for the word defined.
     * @param result    The text containing the definition of the word.
     * @param word      The word defined to display with bold font. For this we
     * had the strong HTML tag.
     * @return          Returns the result text with an strong emphasis of all
     * the occurences of the word defined.
     */
    private String formatWordDefined(String result, String word)
    {
        String tmpWord;

        tmpWord = word.toUpperCase();
        result = result.replaceAll("\\b" + tmpWord + "\\b", "<strong>" + tmpWord + "</strong>");
        tmpWord = word.toLowerCase();
        result = result.replaceAll("\\b" + tmpWord + "\\b", "<strong>" + tmpWord + "</strong>");
        if(tmpWord.length() > 1)
        {
            tmpWord = tmpWord.substring(0, 1).toUpperCase() + tmpWord.substring(1);
            result = result.replaceAll("\\b" + tmpWord + "\\b", "<strong>" + tmpWord + "</strong>");
        }

        return result;
    }

    /**
     * Remplaces special characters into HTML tags to make some emphasis.
     * @param result    The text containing the definition of the word.
     * @param regex     The special character to replace with HTML tags.
     * @param startTag  The start HTML tag to use.
     * @param endTag    The end HTML tag to use.
     * @return          The result with all special characters replaced by HTML
     * tags.
     */
    private String formatResult(String result, String regex, String startTag, String endTag)
    {
        String[] tmp = result.split(regex);
        String res = "";

        for(int i = 0; i < (tmp.length - 1); i += 2)
        {
                res += tmp[i] + startTag + tmp[i+1] + endTag;
        }
        if((tmp.length % 2) != 0)
        {
                res += tmp[tmp.length - 1];
        }
        return res;
    }

    /**
     * Generate the display of the results of the Match command
     *
     * @param data the result of the Match command
     * @param word the queried word
     * @return the formatted result
     */
    private String retrieveMatch(List<MatchWord> data, String word)
    {
        StringBuffer result = new StringBuffer();
        boolean isStart = true;

        result.append(DictActivator.getResources()
            .getI18NString("plugin.dictaccregwizz.MATCH_RESULT", new String[] {word}));

        for (int i=0; i<data.size(); i++)
        {
            if (isStart)
                isStart = false;
            else
                result.append(", ");

            result.append(data.get(i).getWord());
        }

        return result.toString();
    }

    /**
     * Manages the return exception of a dict query.
     *
     * @param dix The exception returned by the adapter
     * @param database The dictionary used
     * @return Exception message
     */
    private String manageException(DictException dix, String database)
    {
        int errorCode = dix.getErrorCode();

        // We change the text only for exception 550 (invalid dictionary) and 551 (invalid strategy)
        if (errorCode == DictReturnCode.INVALID_DATABASE)
        {
            return DictActivator.getResources()
                .getI18NString("plugin.dictaccregwizz.INVALID_DATABASE", new String[] {database});
        }
        else if (errorCode == DictReturnCode.INVALID_STRATEGY)
        {
            return DictActivator.getResources()
                .getI18NString("plugin.dictaccregwizz.INVALID_STRATEGY");
        }
        else if (errorCode == DictReturnCode.NO_MATCH)
        {
            return DictActivator.getResources()
                .getI18NString("plugin.dictaccregwizz.NO_MATCH");
        }

        return dix.getMessage();
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt>. Resources are not supported by this operation set
     * implementation.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param toResource the resource to which the message should be send
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    @Override
    public void sendInstantMessage( Contact to,
                                    ContactResource toResource,
                                    Message message)
        throws  IllegalStateException,
                IllegalArgumentException
    {
        sendInstantMessage(to, message);
    }
}
