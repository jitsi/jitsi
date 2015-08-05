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
package net.java.sip.communicator.plugin.otr;

import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.otr4j.*;
import net.java.otr4j.crypto.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.plugin.otr.authdialog.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 *
 * @author George Politis
 * @author Lyubomir Marinov
 * @author Pawel Domas
 * @author Marin Dzhigarov
 * @author Danny van Heumen
 */
public class ScOtrEngineImpl
    implements ScOtrEngine,
               ChatLinkClickedListener,
               ServiceListener
{
    private class ScOtrEngineHost
        implements OtrEngineHost
    {
        @Override
        public KeyPair getLocalKeyPair(SessionID sessionID)
        {
            AccountID accountID =
                OtrActivator.getAccountIDByUID(sessionID.getAccountID());
            KeyPair keyPair =
                OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
            if (keyPair == null)
                OtrActivator.scOtrKeyManager.generateKeyPair(accountID);

            return OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
        }

        @Override
        public OtrPolicy getSessionPolicy(SessionID sessionID)
        {
            return getContactPolicy(getOtrContact(sessionID).contact);
        }

        @Override
        public void injectMessage(SessionID sessionID, String messageText)
        {
            OtrContact otrContact = getOtrContact(sessionID);
            Contact contact = otrContact.contact;
            ContactResource resource = null;

            if (contact.supportResources())
            {
                Collection<ContactResource> resources = contact.getResources();
                if (resources != null)
                {
                    for (ContactResource r : resources)
                    {
                        if (r.equals(otrContact.resource))
                        {
                            resource = r;
                            break;
                        }
                    }
                }
            }

            OperationSetBasicInstantMessaging imOpSet
                = contact
                    .getProtocolProvider()
                        .getOperationSet(
                                OperationSetBasicInstantMessaging.class);

            // This is a dirty way of detecting whether the injected message
            // contains HTML markup. If this is the case then we should create
            // the message with the appropriate content type so that the remote
            // party can properly display the HTML.
            // When otr4j injects QueryMessages it calls
            // OtrEngineHost.getFallbackMessage() which is currently the only
            // host method that uses HTML so we can simply check if the injected
            // message contains the string that getFallbackMessage() returns.
            String otrHtmlFallbackMessage
                = "<a href=\"http://en.wikipedia.org/wiki/Off-the-Record_Messaging\">";
            String contentType
                = messageText.contains(otrHtmlFallbackMessage)
                    ? OperationSetBasicInstantMessaging.HTML_MIME_TYPE
                    : OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE;
            Message message
                = imOpSet.createMessage(
                        messageText,
                        contentType,
                        OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
                        null);

            injectedMessageUIDs.add(message.getMessageUID());
            imOpSet.sendInstantMessage(contact, resource, message);
        }

        @Override
        public void showError(SessionID sessionID, String err)
        {
            ScOtrEngineImpl.this.showError(sessionID, err);
        }

        public void showWarning(SessionID sessionID, String warn)
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            Contact contact = otrContact.contact;
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.SYSTEM_MESSAGE, warn,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public void unreadableMessageReceived(SessionID sessionID)
            throws OtrException
        {
            OtrContact otrContact = getOtrContact(sessionID);
            String resourceName = otrContact.resource != null ?
                "/" + otrContact.resource.getResourceName() : "";

            Contact contact = otrContact.contact;
            String error =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.unreadablemsgreceived",
                    new String[]
                        {contact.getDisplayName() + resourceName});
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.ERROR_MESSAGE, error,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public void unencryptedMessageReceived(SessionID sessionID, String msg)
            throws OtrException
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            Contact contact = otrContact.contact;
            String warn =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.unencryptedmsgreceived");
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.SYSTEM_MESSAGE, warn,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public void smpError(SessionID sessionID, int tlvType, boolean cheated)
            throws OtrException
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            Contact contact = otrContact.contact;
            logger.debug("SMP error occurred"
                        + ". Contact: " + contact.getDisplayName()
                        + ". TLV type: " + tlvType
                        + ". Cheated: " + cheated);

            String error =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.smperror");
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.ERROR_MESSAGE, error,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

            SmpProgressDialog progressDialog = progressDialogMap.get(otrContact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(otrContact, progressDialog);
            }

            progressDialog.setProgressFail();
            progressDialog.setVisible(true);
        }

        @Override
        public void smpAborted(SessionID sessionID) throws OtrException
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            Contact contact = otrContact.contact;
            Session session = otrEngine.getSession(sessionID);
            if (session.isSmpInProgress())
            {
                String warn =
                    OtrActivator.resourceService.getI18NString(
                        "plugin.otr.activator.smpaborted",
                        new String[] {contact.getDisplayName()});
                OtrActivator.uiService.getChat(contact).addMessage(
                    contact.getDisplayName(), new Date(),
                    Chat.SYSTEM_MESSAGE, warn,
                    OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

                SmpProgressDialog progressDialog =
                    progressDialogMap.get(otrContact);
                if (progressDialog == null)
                {
                    progressDialog = new SmpProgressDialog(contact);
                    progressDialogMap.put(otrContact, progressDialog);
                }

                progressDialog.setProgressFail();
                progressDialog.setVisible(true);
            }
        }

        @Override
        public void finishedSessionMessage(SessionID sessionID, String msgText)
            throws OtrException
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            String resourceName = otrContact.resource != null ?
                "/" + otrContact.resource.getResourceName() : "";
            Contact contact = otrContact.contact;
            String error =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.sessionfinishederror",
                    new String[]
                        {msgText, contact.getDisplayName() + resourceName});
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.ERROR_MESSAGE, error,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public void requireEncryptedMessage(SessionID sessionID, String msgText)
            throws OtrException
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

                Contact contact = otrContact.contact;
            String error =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.requireencryption",
                    new String[]
                        {msgText});
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.ERROR_MESSAGE, error,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public byte[] getLocalFingerprintRaw(SessionID sessionID)
        {
            AccountID accountID =
                OtrActivator.getAccountIDByUID(sessionID.getAccountID());
            return
                OtrActivator.scOtrKeyManager.getLocalFingerprintRaw(accountID);
        }

        @Override
        public void askForSecret(
            SessionID sessionID, InstanceTag receiverTag, String question)
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            Contact contact = otrContact.contact;
            SmpAuthenticateBuddyDialog dialog =
                new SmpAuthenticateBuddyDialog(
                    otrContact, receiverTag, question);
            dialog.setVisible(true);

            SmpProgressDialog progressDialog = progressDialogMap.get(otrContact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(otrContact, progressDialog);
            }

            progressDialog.init();
            progressDialog.setVisible(true);
        }

        @Override
        public void verify(
            SessionID sessionID, String fingerprint, boolean approved)
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            Contact contact = otrContact.contact;
            OtrActivator.scOtrKeyManager.verify(otrContact, fingerprint);

            SmpProgressDialog progressDialog = progressDialogMap.get(otrContact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(otrContact, progressDialog);
            }

            progressDialog.setProgressSuccess();
            progressDialog.setVisible(true);
        }

        @Override
        public void unverify(SessionID sessionID, String fingerprint)
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            Contact contact = otrContact.contact;
            OtrActivator.scOtrKeyManager.unverify(otrContact, fingerprint);

            SmpProgressDialog progressDialog = progressDialogMap.get(otrContact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(otrContact, progressDialog);
            }

            progressDialog.setProgressFail();
            progressDialog.setVisible(true);
        }

        @Override
        public String getReplyForUnreadableMessage(SessionID sessionID)
        {
            AccountID accountID =
                OtrActivator.getAccountIDByUID(sessionID.getAccountID());

            return OtrActivator.resourceService.getI18NString(
                "plugin.otr.activator.unreadablemsgreply",
                new String[] {accountID.getDisplayName(),
                              accountID.getDisplayName()});
        }

        @Override
        public String getFallbackMessage(SessionID sessionID)
        {
            AccountID accountID =
                OtrActivator.getAccountIDByUID(sessionID.getAccountID());

            return OtrActivator.resourceService.getI18NString(
                "plugin.otr.activator.fallbackmessage",
                new String[] {accountID.getDisplayName()});
        }

        @Override
        public void multipleInstancesDetected(SessionID sessionID)
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            String resourceName = otrContact.resource != null ?
                "/" + otrContact.resource.getResourceName() : "";
            Contact contact = otrContact.contact;
            String message =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.multipleinstancesdetected",
                    new String[]
                        {contact.getDisplayName() + resourceName});
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(),
                new Date(), Chat.SYSTEM_MESSAGE,
                message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);
        }

        @Override
        public void messageFromAnotherInstanceReceived(SessionID sessionID)
        {
            OtrContact otrContact = getOtrContact(sessionID);
            if (otrContact == null)
                return;

            String resourceName = otrContact.resource != null ?
                "/" + otrContact.resource.getResourceName() : "";
            Contact contact = otrContact.contact;
            String message =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.msgfromanotherinstance",
                    new String[]
                        {contact.getDisplayName() + resourceName});
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(),
                new Date(), Chat.SYSTEM_MESSAGE,
                message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE);
        }

        /**
         * Provide fragmenter instructions according to the Instant Messaging
         * transport channel of the contact's protocol.
         */
        @Override
        public FragmenterInstructions getFragmenterInstructions(
            final SessionID sessionID)
        {
            final OtrContact otrContact = getOtrContact(sessionID);
            final OperationSetBasicInstantMessagingTransport transport =
                otrContact.contact.getProtocolProvider().getOperationSet(
                    OperationSetBasicInstantMessagingTransport.class);
            if (transport == null)
            {
                // There is no operation set for querying transport parameters.
                // Assuming transport capabilities are unlimited.
                if (logger.isDebugEnabled())
                {
                    logger.debug("No implementation of "
                        + "BasicInstantMessagingTransport available. Assuming "
                        + "OTR defaults for OTR fragmentation instructions.");
                }
                return null;
            }
            int messageSize = transport.getMaxMessageSize(otrContact.contact);
            if (messageSize
                == OperationSetBasicInstantMessagingTransport.UNLIMITED)
            {
                messageSize = FragmenterInstructions.UNLIMITED;
            }
            int numberOfMessages =
                transport.getMaxNumberOfMessages(otrContact.contact);
            if (numberOfMessages
                == OperationSetBasicInstantMessagingTransport.UNLIMITED)
            {
                numberOfMessages = FragmenterInstructions.UNLIMITED;
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("OTR fragmentation instructions for sending a "
                    + "message to " + otrContact.contact.getDisplayName()
                    + " (" + otrContact.contact.getAddress()
                    + "). Maximum number of " + "messages: " + numberOfMessages
                    + ", maximum message size: " + messageSize);
            }
            return new FragmenterInstructions(numberOfMessages, messageSize);
        }
    }

    /**
     * The max timeout period elapsed prior to establishing a TIMED_OUT session.
     */
    private static final int SESSION_TIMEOUT =
        OtrActivator.configService.getInt(
            "net.java.sip.communicator.plugin.otr.SESSION_STATUS_TIMEOUT",
            30000);

    /**
     * Manages the scheduling of TimerTasks that are used to set Contact's
     * ScSessionStatus (to TIMED_OUT) after a period of time.
     */
    private ScSessionStatusScheduler scheduler = new ScSessionStatusScheduler();

    /**
     * This mapping is used for taking care of keeping SessionStatus and
     * ScSessionStatus in sync for every Session object.
     */
    private Map<SessionID, ScSessionStatus> scSessionStatusMap =
        new ConcurrentHashMap<SessionID, ScSessionStatus>();

    private static final Map<ScSessionID, OtrContact> contactsMap =
        new Hashtable<ScSessionID, OtrContact>();

    private static final Map<OtrContact, SmpProgressDialog> progressDialogMap =
        new ConcurrentHashMap<OtrContact, SmpProgressDialog>();

    public static OtrContact getOtrContact(SessionID sessionID)
    {
        return contactsMap.get(new ScSessionID(sessionID));
    }

    /**
     * Returns the <tt>ScSessionID</tt> for given <tt>UUID</tt>.
     * @param guid the <tt>UUID</tt> identifying <tt>ScSessionID</tt>.
     * @return the <tt>ScSessionID</tt> for given <tt>UUID</tt> or <tt>null</tt>
     *         if no matching session found.
     */
    public static ScSessionID getScSessionForGuid(UUID guid)
    {
        for(ScSessionID scSessionID : contactsMap.keySet())
        {
            if(scSessionID.getGUID().equals(guid))
            {
                return scSessionID;
            }
        }
        return null;
    }

    public static SessionID getSessionID(OtrContact otrContact)
    {
        ProtocolProviderService pps = otrContact.contact.getProtocolProvider();
        String resourceName = otrContact.resource != null ?
            "/" + otrContact.resource.getResourceName() : "";
        SessionID sessionID
            = new SessionID(
                    pps.getAccountID().getAccountUniqueID(),
                    otrContact.contact.getAddress() + resourceName,
                    pps.getProtocolName());

        synchronized (contactsMap)
        {
            if(contactsMap.containsKey(new ScSessionID(sessionID)))
                return sessionID;

            ScSessionID scSessionID = new ScSessionID(sessionID);

            contactsMap.put(scSessionID, otrContact);
        }

        return sessionID;
    }

    private final OtrConfigurator configurator = new OtrConfigurator();

    private final List<String> injectedMessageUIDs = new Vector<String>();

    private final List<ScOtrEngineListener> listeners =
        new Vector<ScOtrEngineListener>();

    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(ScOtrEngineImpl.class);

    private final OtrEngineHost otrEngineHost = new ScOtrEngineHost();

    private final OtrSessionManager otrEngine;

    public ScOtrEngineImpl()
    {
        otrEngine = new OtrSessionManagerImpl(otrEngineHost);

        // Clears the map after previous instance
        // This is required because of OSGi restarts in the same VM on Android
        contactsMap.clear();
        scSessionStatusMap.clear();

        this.otrEngine.addOtrEngineListener(new OtrEngineListener()
        {
            @Override
            public void sessionStatusChanged(SessionID sessionID)
            {
                OtrContact otrContact = getOtrContact(sessionID);
                if (otrContact == null)
                    return;

                String resourceName = otrContact.resource != null ?
                    "/" + otrContact.resource.getResourceName() : "";
                Contact contact = otrContact.contact;
                // Cancels any scheduled tasks that will change the
                // ScSessionStatus for this Contact
                scheduler.cancel(otrContact);

                ScSessionStatus scSessionStatus = getSessionStatus(otrContact);
                String message = "";
                final Session session = otrEngine.getSession(sessionID);
                switch (session.getSessionStatus())
                {
                case ENCRYPTED:
                    scSessionStatus = ScSessionStatus.ENCRYPTED;
                    scSessionStatusMap.put(sessionID, scSessionStatus);
                    PublicKey remotePubKey = session.getRemotePublicKey();

                    String remoteFingerprint = null;
                    try
                    {
                        remoteFingerprint =
                            new OtrCryptoEngineImpl().
                                getFingerprint(remotePubKey);
                    }
                    catch (OtrCryptoException e)
                    {
                        logger.debug(
                            "Could not get the fingerprint from the "
                            + "public key of contact: " + contact);
                    }

                    List<String> allFingerprintsOfContact =
                        OtrActivator.scOtrKeyManager.
                            getAllRemoteFingerprints(contact);
                    if (allFingerprintsOfContact != null)
                    {
                        if (!allFingerprintsOfContact.contains(
                                remoteFingerprint))
                        {
                            OtrActivator.scOtrKeyManager.saveFingerprint(
                                contact, remoteFingerprint);
                        }
                    }

                    if (!OtrActivator.scOtrKeyManager.isVerified(
                            contact, remoteFingerprint))
                    {
                        OtrActivator.scOtrKeyManager.unverify(
                            otrContact, remoteFingerprint);
                        UUID sessionGuid = null;
                        for(ScSessionID scSessionID : contactsMap.keySet())
                        {
                            if(scSessionID.getSessionID().equals(sessionID))
                            {
                                sessionGuid = scSessionID.getGUID();
                                break;
                            }
                        }

                        OtrActivator.uiService.getChat(contact)
                            .addChatLinkClickedListener(ScOtrEngineImpl.this);

                        String unverifiedSessionWarning
                            = OtrActivator.resourceService.getI18NString(
                                    "plugin.otr.activator"
                                        + ".unverifiedsessionwarning",
                                    new String[]
                                    {
                                        contact.getDisplayName() + resourceName,
                                        this.getClass().getName(),
                                        "AUTHENTIFICATION",
                                        sessionGuid.toString()
                                    });
                        OtrActivator.uiService.getChat(contact).addMessage(
                            contact.getDisplayName(),
                            new Date(), Chat.SYSTEM_MESSAGE,
                            unverifiedSessionWarning,
                            OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

                    }

                    // show info whether history is on or off
                    String otrAndHistoryMessage;
                    if(!OtrActivator.getMessageHistoryService()
                        .isHistoryLoggingEnabled() || 
                        !isHistoryLoggingEnabled(contact))
                    {
                        otrAndHistoryMessage =
                            OtrActivator.resourceService.getI18NString(
                                "plugin.otr.activator.historyoff",
                                new String[]{
                                    OtrActivator.resourceService
                                        .getSettingsString(
                                            "service.gui.APPLICATION_NAME"),
                                    this.getClass().getName(),
                                    "showHistoryPopupMenu"
                                });
                    }
                    else
                    {
                        otrAndHistoryMessage =
                            OtrActivator.resourceService.getI18NString(
                                "plugin.otr.activator.historyon",
                                new String[]{
                                    OtrActivator.resourceService
                                        .getSettingsString(
                                            "service.gui.APPLICATION_NAME"),
                                    this.getClass().getName(),
                                    "showHistoryPopupMenu"
                                });
                    }
                    OtrActivator.uiService.getChat(contact).addMessage(
                        contact.getDisplayName(),
                        new Date(), Chat.SYSTEM_MESSAGE,
                        otrAndHistoryMessage,
                        OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

                    message =
                        OtrActivator.resourceService.getI18NString(
                            "plugin.otr.activator.multipleinstancesdetected",
                            new String[]
                                {contact.getDisplayName()});

                    if (contact.supportResources()
                        && contact.getResources() != null
                        && contact.getResources().size() > 1)
                        OtrActivator.uiService.getChat(contact).addMessage(
                            contact.getDisplayName(),
                            new Date(), Chat.SYSTEM_MESSAGE,
                            message,
                            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

                    message
                        = OtrActivator.resourceService.getI18NString(
                                OtrActivator.scOtrKeyManager.isVerified(
                                    contact, remoteFingerprint)
                                    ? "plugin.otr.activator.sessionstared"
                                    : "plugin.otr.activator"
                                        + ".unverifiedsessionstared",
                                new String[]
                                    {contact.getDisplayName() + resourceName});

                    break;
                case FINISHED:
                    scSessionStatus = ScSessionStatus.FINISHED;
                    scSessionStatusMap.put(sessionID, scSessionStatus);
                    message =
                        OtrActivator.resourceService.getI18NString(
                            "plugin.otr.activator.sessionfinished",
                            new String[]
                                {contact.getDisplayName() + resourceName});
                    break;
                case PLAINTEXT:
                    scSessionStatus = ScSessionStatus.PLAINTEXT;
                    scSessionStatusMap.put(sessionID, scSessionStatus);
                    message =
                        OtrActivator.resourceService.getI18NString(
                            "plugin.otr.activator.sessionlost", new String[]
                                {contact.getDisplayName() + resourceName});
                    break;
                }

                OtrActivator.uiService.getChat(contact).addMessage(
                    contact.getDisplayName(), new Date(),
                    Chat.SYSTEM_MESSAGE, message,
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

                for (ScOtrEngineListener l : getListeners())
                    l.sessionStatusChanged(otrContact);
            }

            @Override
            public void multipleInstancesDetected(SessionID sessionID)
            {
                OtrContact otrContact = getOtrContact(sessionID);
                if (otrContact == null)
                    return;

                for (ScOtrEngineListener l : getListeners())
                    l.multipleInstancesDetected(otrContact);
            }

            @Override
            public void outgoingSessionChanged(SessionID sessionID)
            {
                OtrContact otrContact = getOtrContact(sessionID);
                if (otrContact == null)
                    return;

                for (ScOtrEngineListener l : getListeners())
                    l.outgoingSessionChanged(otrContact);
            }
        });
    }

    /**
     * Checks whether history is enabled for the metacontact containing
     * the <tt>contact</tt>.
     * @param contact the contact to check.
     * @return whether chat logging is enabled while chatting
     * with <tt>contact</tt>.
     */
    private boolean isHistoryLoggingEnabled(Contact contact)
    {
        MetaContact metaContact = OtrActivator
            .getContactListService().findMetaContactByContact(contact);
        if(metaContact != null)
            return OtrActivator.getMessageHistoryService()
                .isHistoryLoggingEnabled(metaContact.getMetaUID());
        else
            return true;
    }

    @Override
    public void addListener(ScOtrEngineListener l)
    {
        synchronized (listeners)
        {
            if (!listeners.contains(l))
                listeners.add(l);
        }
    }

    @Override
    public void chatLinkClicked(URI url)
    {
        String action = url.getPath();
        if(action.equals("/AUTHENTIFICATION"))
        {
            UUID guid = UUID.fromString(url.getQuery());

            if(guid == null)
                throw new RuntimeException(
                        "No UUID found in OTR authenticate URL");

            // Looks for registered action handler
            OtrActionHandler actionHandler
                    = ServiceUtils.getService(
                            OtrActivator.bundleContext,
                            OtrActionHandler.class);

            if(actionHandler != null)
            {
                actionHandler.onAuthenticateLinkClicked(guid);
            }
            else
            {
                logger.error("No OtrActionHandler registered");
            }
        }
    }

    @Override
    public void endSession(OtrContact otrContact)
    {
        SessionID sessionID = getSessionID(otrContact);
        try
        {
            setSessionStatus(otrContact, ScSessionStatus.PLAINTEXT);

            otrEngine.getSession(sessionID).endSession();
        }
        catch (OtrException e)
        {
            showError(sessionID, e.getMessage());
        }
    }

    @Override
    public OtrPolicy getContactPolicy(Contact contact)
    {
        ProtocolProviderService pps = contact.getProtocolProvider();
        SessionID sessionID
            = new SessionID(
                pps.getAccountID().getAccountUniqueID(),
                contact.getAddress(),
                pps.getProtocolName());
        int policy =
            this.configurator.getPropertyInt(sessionID + "contact_policy",
                -1);
        if (policy < 0)
            return getGlobalPolicy();
        else
            return new OtrPolicyImpl(policy);
    }

    @Override
    public OtrPolicy getGlobalPolicy()
    {
        /*
         * SEND_WHITESPACE_TAG bit will be lowered until we stabilize the OTR.
         */
        int defaultScOtrPolicy =
            OtrPolicy.OTRL_POLICY_DEFAULT & ~OtrPolicy.SEND_WHITESPACE_TAG;
        return new OtrPolicyImpl(this.configurator.getPropertyInt(
            "GLOBAL_POLICY", defaultScOtrPolicy));
    }

    /**
     * Gets a copy of the list of <tt>ScOtrEngineListener</tt>s registered with
     * this instance which may safely be iterated without the risk of a
     * <tt>ConcurrentModificationException</tt>.
     *
     * @return a copy of the list of <tt>ScOtrEngineListener<tt>s registered
     * with this instance which may safely be iterated without the risk of a
     * <tt>ConcurrentModificationException</tt>
     */
    private ScOtrEngineListener[] getListeners()
    {
        synchronized (listeners)
        {
            return listeners.toArray(new ScOtrEngineListener[listeners.size()]);
        }
    }

    /**
     * Manages the scheduling of TimerTasks that are used to set Contact's
     * ScSessionStatus after a period of time.
     * 
     * @author Marin Dzhigarov
     */
    private class ScSessionStatusScheduler
    {
        private final Timer timer = new Timer();

        private final Map<OtrContact, TimerTask> tasks =
            new ConcurrentHashMap<OtrContact, TimerTask>();

        public void scheduleScSessionStatusChange(
            final OtrContact otrContact, final ScSessionStatus status)
        {
            cancel(otrContact);

            TimerTask task
                = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        setSessionStatus(otrContact, status);
                    }
                };
            timer.schedule(task, SESSION_TIMEOUT);
            tasks.put(otrContact, task);
        }

        public void cancel(final OtrContact otrContact)
        {
            TimerTask task = tasks.get(otrContact);
            if (task != null)
                task.cancel();
            tasks.remove(otrContact);
        }

        public void serviceChanged(ServiceEvent ev)
        {
            Object service
                = OtrActivator.bundleContext.getService(
                    ev.getServiceReference());

            if (!(service instanceof ProtocolProviderService))
                return;
    
            if (ev.getType() == ServiceEvent.UNREGISTERING)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) service;
    
                Iterator<OtrContact> i = tasks.keySet().iterator();
    
                while (i.hasNext())
                {
                    OtrContact otrContact = i.next();
                    if (provider.equals(
                        otrContact.contact.getProtocolProvider()))
                    {
                        cancel(otrContact);
                        i.remove();
                    }
                }
            }
        }
    }

    private void setSessionStatus(OtrContact contact, ScSessionStatus status)
    {
        scSessionStatusMap.put(getSessionID(contact), status);
        scheduler.cancel(contact);
        for (ScOtrEngineListener l : getListeners())
            l.sessionStatusChanged(contact);
    }

    @Override
    public ScSessionStatus getSessionStatus(OtrContact contact)
    {
        SessionID sessionID = getSessionID(contact);
        SessionStatus sessionStatus = otrEngine.getSession(sessionID).getSessionStatus();
        ScSessionStatus scSessionStatus = null;
        if (!scSessionStatusMap.containsKey(sessionID))
        {
            switch (sessionStatus)
            {
            case PLAINTEXT:
                scSessionStatus = ScSessionStatus.PLAINTEXT;
                break;
            case ENCRYPTED:
                scSessionStatus = ScSessionStatus.ENCRYPTED;
                break;
            case FINISHED:
                scSessionStatus = ScSessionStatus.FINISHED;
                break;
            }
            scSessionStatusMap.put(sessionID, scSessionStatus);
        }
        return scSessionStatusMap.get(sessionID);
    }

    @Override
    public boolean isMessageUIDInjected(String mUID)
    {
        return injectedMessageUIDs.contains(mUID);
    }

    @Override
    public void launchHelp()
    {
        ServiceReference ref =
            OtrActivator.bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

        if (ref == null)
            return;

        BrowserLauncherService service =
            (BrowserLauncherService) OtrActivator.bundleContext.getService(ref);

        service.openURL(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.HELP_URI"));
    }

    @Override
    public void refreshSession(OtrContact otrContact)
    {
        SessionID sessionID = getSessionID(otrContact);
        try
        {
            otrEngine.getSession(sessionID).refreshSession();
        }
        catch (OtrException e)
        {
            logger.error("Error refreshing session", e);
            showError(sessionID, e.getMessage());
        }
    }

    @Override
    public void removeListener(ScOtrEngineListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    /**
     * Cleans the contactsMap when <tt>ProtocolProviderService</tt>
     * gets unregistered.
     */
    @Override
    public void serviceChanged(ServiceEvent ev)
    {
        Object service
            = OtrActivator.bundleContext.getService(ev.getServiceReference());

        if (!(service instanceof ProtocolProviderService))
            return;

        if (ev.getType() == ServiceEvent.UNREGISTERING)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Unregistering a ProtocolProviderService, cleaning"
                            + " OTR's ScSessionID to Contact map.");
                logger.debug(
                        "Unregistering a ProtocolProviderService, cleaning"
                            + " OTR's Contact to SpmProgressDialog map.");
            }

            ProtocolProviderService provider
                = (ProtocolProviderService) service;

            synchronized(contactsMap)
            {
                Iterator<OtrContact> i = contactsMap.values().iterator();

                while (i.hasNext())
                {
                    OtrContact otrContact = i.next();
                    if (provider.equals(
                        otrContact.contact.getProtocolProvider()))
                    {
                        scSessionStatusMap.remove(getSessionID(otrContact));
                        i.remove();
                    }
                }
            }

            Iterator<OtrContact> i = progressDialogMap.keySet().iterator();

            while (i.hasNext())
            {
                if (provider.equals(i.next().contact.getProtocolProvider()))
                    i.remove();
            }
            scheduler.serviceChanged(ev);
        }
    }

    @Override
    public void setContactPolicy(Contact contact, OtrPolicy policy)
    {
        ProtocolProviderService pps = contact.getProtocolProvider();
        SessionID sessionID
            = new SessionID(
                pps.getAccountID().getAccountUniqueID(),
                contact.getAddress(),
                pps.getProtocolName());

        String propertyID = sessionID + "contact_policy";
        if (policy == null)
            this.configurator.removeProperty(propertyID);
        else
            this.configurator.setProperty(propertyID, policy.getPolicy());

        for (ScOtrEngineListener l : getListeners())
            l.contactPolicyChanged(contact);
    }

    @Override
    public void setGlobalPolicy(OtrPolicy policy)
    {
        if (policy == null)
            this.configurator.removeProperty("GLOBAL_POLICY");
        else
            this.configurator.setProperty("GLOBAL_POLICY", policy.getPolicy());

        for (ScOtrEngineListener l : getListeners())
            l.globalPolicyChanged();
    }

    public void showError(SessionID sessionID, String err)
    {
        OtrContact otrContact = getOtrContact(sessionID);
        if (otrContact == null)
            return;

        Contact contact = otrContact.contact;
        OtrActivator.uiService.getChat(contact).addMessage(
            contact.getDisplayName(), new Date(),
            Chat.ERROR_MESSAGE, err,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    @Override
    public void startSession(OtrContact otrContact)
    {
        SessionID sessionID = getSessionID(otrContact);

        ScSessionStatus scSessionStatus = getSessionStatus(otrContact);
        scSessionStatus = ScSessionStatus.LOADING;
        scSessionStatusMap.put(sessionID, scSessionStatus);
        for (ScOtrEngineListener l : getListeners())
        {
            l.sessionStatusChanged(otrContact);
        }

        scheduler.scheduleScSessionStatusChange(
            otrContact, ScSessionStatus.TIMED_OUT);

        try
        {
            otrEngine.getSession(sessionID).startSession();
        }
        catch (OtrException e)
        {
            logger.error("Error starting session", e);
            showError(sessionID, e.getMessage());
        }
    }

    @Override
    public String transformReceiving(OtrContact otrContact, String msgText)
    {
        SessionID sessionID = getSessionID(otrContact);
        try
        {
            return otrEngine.getSession(sessionID).transformReceiving(msgText);
        }
        catch (OtrException e)
        {
            logger.error("Error receiving the message", e);
            showError(sessionID, e.getMessage());
            return null;
        }
    }

    @Override
    public String[] transformSending(OtrContact otrContact, String msgText)
    {
        SessionID sessionID = getSessionID(otrContact);
        try
        {
            return otrEngine.getSession(sessionID).transformSending(msgText);
        }
        catch (OtrException e)
        {
            logger.error("Error transforming the message", e);
            showError(sessionID, e.getMessage());
            return null;
        }
    }

    private Session getSession(OtrContact contact)
    {
        SessionID sessionID = getSessionID(contact);
        return otrEngine.getSession(sessionID);
    }

    @Override
    public void initSmp(OtrContact otrContact, String question, String secret)
    {
        Session session = getSession(otrContact);
        try
        {
            session.initSmp(question, secret);

            SmpProgressDialog progressDialog = progressDialogMap.get(otrContact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(otrContact.contact);
                progressDialogMap.put(otrContact, progressDialog);
            }

            progressDialog.init();
            progressDialog.setVisible(true);
        }
        catch (OtrException e)
        {
            logger.error("Error initializing SMP session with contact "
                         + otrContact.contact.getDisplayName(), e);
            showError(session.getSessionID(), e.getMessage());
        }
    }

    @Override
    public void respondSmp( OtrContact otrContact,
                            InstanceTag receiverTag,
                            String question,
                            String secret)
    {
        Session session = getSession(otrContact);
        try
        {
            session.respondSmp(receiverTag, question, secret);

            SmpProgressDialog progressDialog = progressDialogMap.get(otrContact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(otrContact.contact);
                progressDialogMap.put(otrContact, progressDialog);
            }

            progressDialog.incrementProgress();
            progressDialog.setVisible(true);
        }
        catch (OtrException e)
        {
            logger.error(
                "Error occured when sending SMP response to contact "
                + otrContact.contact.getDisplayName(), e);
            showError(session.getSessionID(), e.getMessage());
        }
    }

    @Override
    public void abortSmp(OtrContact otrContact)
    {
        Session session = getSession(otrContact);
        try
        {
            session.abortSmp();

            SmpProgressDialog progressDialog = progressDialogMap.get(otrContact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(otrContact.contact);
                progressDialogMap.put(otrContact, progressDialog);
            }

            progressDialog.dispose();
        }
        catch (OtrException e)
        {
            logger.error("Error aborting SMP session with contact "
                         + otrContact.contact.getDisplayName(), e);
            showError(session.getSessionID(), e.getMessage());
        }
    }

    @Override
    public PublicKey getRemotePublicKey(OtrContact otrContact)
    {
        if (otrContact == null)
            return null;

        Session session = getSession(otrContact);

        return session.getRemotePublicKey();
    }

    @Override
    public List<Session> getSessionInstances(OtrContact otrContact)
    {
        if (otrContact == null)
            return Collections.emptyList();
        return getSession(otrContact).getInstances();
    }

    @Override
    public boolean setOutgoingSession(OtrContact contact, InstanceTag tag)
    {
        if (contact == null)
            return false;

        Session session = getSession(contact);

        scSessionStatusMap.remove(session.getSessionID());
        return session.setOutgoingInstance(tag);
    }

    @Override
    public Session getOutgoingSession(OtrContact contact)
    {
        if (contact == null)
            return null;

        SessionID sessionID = getSessionID(contact);

        return otrEngine.getSession(sessionID).getOutgoingInstance();
    }
}
