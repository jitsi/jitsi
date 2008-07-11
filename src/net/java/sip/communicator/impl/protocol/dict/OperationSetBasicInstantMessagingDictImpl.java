/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;
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
    implements OperationSetBasicInstantMessaging,
               RegistrationStateChangeListener
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicInstantMessagingDictImpl.class);
    
    /**
     * Currently registered message listeners.
     */
    private Vector messageListeners = new Vector();

    /**
     * The currently valid persistent presence operation set.
     */
    private OperationSetPersistentPresenceDictImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceDictImpl parentProvider = null;

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

        parentProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Registers a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages.
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    public void addMessageListener(MessageListener listener)
    {
        if(!messageListeners.contains(listener))
        {
            messageListeners.add(listener);
        }
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *   subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageDictImpl(new String(content), contentType,
                                  contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return new MessageDictImpl(messageText, DEFAULT_MIME_TYPE,
                                  DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception
     * of incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    public void removeMessageListener(MessageListener listener)
    {
        messageListeners.remove(listener);
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
        
        // Display the queried word
        fireMessageDelivered(message, to);

        this.submitDictQuery((ContactDictImpl) to, message);
    }

    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     *
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    private void fireMessageDelivered(Message message, Contact to)
    {
        MessageDeliveredEvent evt
            = new MessageDeliveredEvent(message, to, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageDelivered(evt);
        }
    }

    /**
     * Notifies all registered message listeners that a message has been
     * received.
     *
     * @param message the <tt>Message</tt> that has been received.
     * @param from the <tt>Contact</tt> that <tt>message</tt> was received from.
     */
    private void fireMessageReceived(Message message, Contact from)
    {
        MessageReceivedEvent evt
            = new MessageReceivedEvent(message, from, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageReceived(evt);
        }
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
        DictAdapter dictAdapter = dictContact.getDictAdapter();
        boolean doMatch = false;
        DictResultset fctResult;
        
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
            fctResult = dictAdapter.define(database, word);
            msg = this.createMessage(this.retrieveDefine(fctResult, word).getBytes()
                    , HTML_MIME_TYPE
                    , DEFAULT_MIME_ENCODING,
                    null);
        }
        catch(DictException dex)
        {
            if (dex.getErrorCode() == 552)
            { // No word found, we are going to try the match command
                doMatch = true;
            }
            else
            { // Otherwise we display the error returned by the server
                msg = this.createMessage(manageException(dex, database));
            }
        }
        catch(Exception ex)
        {
            logger.error("Failed to retrieve Definition. Error was: "
                    + ex.getMessage()
                    , ex);
        }
        
        if (doMatch)
        {
            // Trying the match command
            try
            {
                fctResult = dictAdapter.match(database, word);
                msg = this.createMessage(this.retrieveMatch(fctResult, word));
            }
            catch(DictException dex)
            {
                msg = this.createMessage(manageException(dex, database));
            }
            catch(Exception ex)
            {
                logger.error("Failed to retrieve Match. Error was: "
                        + ex.getMessage()
                        , ex);
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
    private String retrieveDefine(DictResultset data, String word)
    {
        String result;
        DictResult resultData;

        result = "<pre>";
        
        for (int i=0; i<data.getNbResults(); i++)
        {
            resultData = data.getResultset(i);
            
            if(i != 0 && resultData.hasNext())
            {
                result += "<hr>";
            }
            while (resultData.hasNext())
            {
                result += resultData.next() + "\n";
            }
            result = result.replaceAll("\n+\\z", "\n");
        }
        result += "</pre>";
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
    private String retrieveMatch(DictResultset data, String word)
    {
        String result = "";
        String temp;
        DictResult resultData;
        boolean isStart = true;
        
        result = "No definitions found for \""+ word +"\", perhaps you mean:\n";
        
        for (int i=0; i<data.getNbResults(); i++)
        {
            resultData = data.getResultset(i);
            
            while(resultData.hasNext())
            {
                temp = resultData.next();
                
                if (isStart)
                {
                    isStart = false;
                }
                else
                {
                    result += ", ";
                }
                
                // Return format : dictCode "match word"
                temp = (temp.split(" ", 2))[1];
                
                if (temp.indexOf(" ") == -1)
                {
                    temp = temp.substring(1, temp.length() -1);
                }
                
                result += temp;
            }
        }
        
        return result;
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
        if (errorCode == 550)
        {
            return "The current dictionary '" + database + "' doesn't exist anymore on the server";
        }
        else if (errorCode == 551)
        {
            return "The current strategy isn't available on the server";
        }

        return dix.getErrorMessage();
    }
}
