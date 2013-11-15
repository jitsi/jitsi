/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
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
 */
public class ScOtrEngineImpl
    implements ScOtrEngine,
               ChatLinkClickedListener,
               ServiceListener
{
    class ScOtrEngineHost
        implements OtrEngineHost
    {
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

        public OtrPolicy getSessionPolicy(SessionID sessionID)
        {
            return getContactPolicy(getContact(sessionID));
        }

        public void injectMessage(SessionID sessionID, String messageText)
        {
            Contact contact = getContact(sessionID);
            OperationSetBasicInstantMessaging imOpSet
                = contact
                    .getProtocolProvider()
                        .getOperationSet(
                                OperationSetBasicInstantMessaging.class);
            Message message = imOpSet.createMessage(messageText);

            injectedMessageUIDs.add(message.getMessageUID());
            imOpSet.sendInstantMessage(contact, message);
        }

        public void showError(SessionID sessionID, String err)
        {
            ScOtrEngineImpl.this.showError(sessionID, err);
        }

        public void showWarning(SessionID sessionID, String warn)
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.SYSTEM_MESSAGE, warn,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public void unreadableMessageReceived(SessionID sessionID)
            throws OtrException
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            String error =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.unreadablemsgreceived",
                    new String[] {contact.getDisplayName()});
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.ERROR_MESSAGE, error,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public void unencryptedMessageReceived(SessionID sessionID, String msg)
            throws OtrException
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

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
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

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

            SmpProgressDialog progressDialog = progressDialogMap.get(contact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(contact, progressDialog);
            }

            progressDialog.setProgressFail();
            progressDialog.setVisible(true);
        }

        @Override
        public void smpAborted(SessionID sessionID) throws OtrException
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

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
    
                SmpProgressDialog progressDialog = progressDialogMap.get(contact);
                if (progressDialog == null)
                {
                    progressDialog = new SmpProgressDialog(contact);
                    progressDialogMap.put(contact, progressDialog);
                }
    
                progressDialog.setProgressFail();
                progressDialog.setVisible(true);
            }
        }

        @Override
        public void finishedSessionMessage(SessionID sessionID)
            throws OtrException
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            String error =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.sessionfinishederror",
                    new String[] {contact.getDisplayName()});
            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), new Date(),
                Chat.ERROR_MESSAGE, error,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        @Override
        public void requireEncryptedMessage(SessionID sessionID, String msgText)
            throws OtrException
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            String error =
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.activator.requireencryption",
                    new String[] {contact.getDisplayName()});
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
        public void askForSecret(SessionID sessionID, String question)
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            SmpAuthenticateBuddyDialog dialog =
                new SmpAuthenticateBuddyDialog(contact, question);
            dialog.setVisible(true);

            SmpProgressDialog progressDialog = progressDialogMap.get(contact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(contact, progressDialog);
            }
            
            progressDialog.init();
            progressDialog.setVisible(true);
        }

        @Override
        public void verify(SessionID sessionID, boolean approved)
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            OtrActivator.scOtrKeyManager.verify(contact);

            SmpProgressDialog progressDialog = progressDialogMap.get(contact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(contact, progressDialog);
            }
            
            progressDialog.setProgressSuccess();
            progressDialog.setVisible(true);
        }

        @Override
        public void unverify(SessionID sessionID)
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            OtrActivator.scOtrKeyManager.unverify(contact);
            
            SmpProgressDialog progressDialog = progressDialogMap.get(contact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(contact, progressDialog);
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
    }

    private static final Map<ScSessionID, Contact> contactsMap =
        new Hashtable<ScSessionID, Contact>();

    private static final Map<Contact, SmpProgressDialog> progressDialogMap =
        new ConcurrentHashMap<Contact, SmpProgressDialog>();

    public static Contact getContact(SessionID sessionID)
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

    public static SessionID getSessionID(Contact contact)
    {
        ProtocolProviderService pps = contact.getProtocolProvider();
        SessionID sessionID
            = new SessionID(
                    pps.getAccountID().getAccountUniqueID(),
                    contact.getAddress(),
                    pps.getProtocolName());

        synchronized (contactsMap)
        {
            if(contactsMap.containsKey(new ScSessionID(sessionID)))
                return sessionID;

            ScSessionID scSessionID = new ScSessionID(sessionID);

            contactsMap.put(scSessionID, contact);
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

    final OtrEngineHost otrEngineHost = new ScOtrEngineHost();

    private final OtrEngine otrEngine;

    public ScOtrEngineImpl()
    {
        otrEngine = new OtrEngineImpl(otrEngineHost);

        // Clears the map after previous instance
        // This is required because of OSGi restarts in the same VM on Android
        contactsMap.clear();

        this.otrEngine.addOtrEngineListener(new OtrEngineListener()
        {
            public void sessionStatusChanged(SessionID sessionID)
            {
                Contact contact = getContact(sessionID);
                if (contact == null)
                    return;

                String message = "";
                switch (otrEngine.getSessionStatus(sessionID))
                {
                case ENCRYPTED:
                    PublicKey remotePubKey =
                        otrEngine.getRemotePublicKey(sessionID);

                    PublicKey storedPubKey =
                        OtrActivator.scOtrKeyManager.loadPublicKey(contact);

                    if (!remotePubKey.equals(storedPubKey))
                        OtrActivator.scOtrKeyManager.savePublicKey(contact,
                            remotePubKey);

                    if (!OtrActivator.scOtrKeyManager.isVerified(contact))
                    {
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
                                        contact.getDisplayName(),
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
                    if(!ConfigurationUtils.isHistoryLoggingEnabled()
                        || !isHistoryLoggingEnabled(contact))
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

                    message
                        = OtrActivator.resourceService.getI18NString(
                                OtrActivator.scOtrKeyManager.isVerified(contact)
                                    ? "plugin.otr.activator.sessionstared"
                                    : "plugin.otr.activator"
                                        + ".unverifiedsessionstared",
                                new String[] { contact.getDisplayName() });

                    break;
                case FINISHED:
                    message =
                        OtrActivator.resourceService.getI18NString(
                            "plugin.otr.activator.sessionfinished",
                            new String[]
                            { contact.getDisplayName() });
                    break;
                case PLAINTEXT:
                    message =
                        OtrActivator.resourceService.getI18NString(
                            "plugin.otr.activator.sessionlost", new String[]
                            { contact.getDisplayName() });
                    break;
                }

                OtrActivator.uiService.getChat(contact).addMessage(
                    contact.getDisplayName(), new Date(),
                    Chat.SYSTEM_MESSAGE, message,
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

                for (ScOtrEngineListener l : getListeners())
                    l.sessionStatusChanged(contact);
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
            return ConfigurationUtils.isHistoryLoggingEnabled(
                metaContact.getMetaUID());
        else
            return true;
    }

    public void addListener(ScOtrEngineListener l)
    {
        synchronized (listeners)
        {
            if (!listeners.contains(l))
                listeners.add(l);
        }
    }

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

    public void endSession(Contact contact)
    {
        SessionID sessionID = getSessionID(contact);
        try
        {
            otrEngine.endSession(sessionID);
        }
        catch (OtrException e)
        {
            showError(sessionID, e.getMessage());
        }
    }

    public OtrPolicy getContactPolicy(Contact contact)
    {
        int policy =
            this.configurator.getPropertyInt(getSessionID(contact) + "policy",
                -1);
        if (policy < 0)
            return getGlobalPolicy();
        else
            return new OtrPolicyImpl(policy);
    }

    public OtrPolicy getGlobalPolicy()
    {
        return new OtrPolicyImpl(this.configurator.getPropertyInt("POLICY",
            OtrPolicy.OTRL_POLICY_MANUAL));
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

    public SessionStatus getSessionStatus(Contact contact)
    {
        return otrEngine.getSessionStatus(getSessionID(contact));
    }

    public boolean isMessageUIDInjected(String mUID)
    {
        return injectedMessageUIDs.contains(mUID);
    }

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

    public void refreshSession(Contact contact)
    {
        SessionID sessionID = getSessionID(contact);
        try
        {
            otrEngine.refreshSession(sessionID);
        }
        catch (OtrException e)
        {
            logger.error("Error refreshing session", e);
            showError(sessionID, e.getMessage());
        }
    }

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
                Iterator<Contact> i = contactsMap.values().iterator();

                while (i.hasNext())
                {
                    if (provider.equals(i.next().getProtocolProvider()))
                        i.remove();
                }
            }

            Iterator<Contact> i = progressDialogMap.keySet().iterator();

            while (i.hasNext())
            {
                if (provider.equals(i.next().getProtocolProvider()))
                    i.remove();
            }
        }
    }

    public void setContactPolicy(Contact contact, OtrPolicy policy)
    {
        String propertyID = getSessionID(contact) + "policy";
        if (policy == null)
            this.configurator.removeProperty(propertyID);
        else
            this.configurator.setProperty(propertyID, policy.getPolicy());

        for (ScOtrEngineListener l : getListeners())
            l.contactPolicyChanged(contact);
    }

    public void setGlobalPolicy(OtrPolicy policy)
    {
        if (policy == null)
            this.configurator.removeProperty("POLICY");
        else
            this.configurator.setProperty("POLICY", policy.getPolicy());

        for (ScOtrEngineListener l : getListeners())
            l.globalPolicyChanged();
    }

    public void showError(SessionID sessionID, String err)
    {
        Contact contact = getContact(sessionID);
        if (contact == null)
            return;

        OtrActivator.uiService.getChat(contact).addMessage(
            contact.getDisplayName(), new Date(),
            Chat.ERROR_MESSAGE, err,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }

    public void startSession(Contact contact)
    {
        SessionID sessionID = getSessionID(contact);
        try
        {
            otrEngine.startSession(sessionID);
        }
        catch (OtrException e)
        {
            logger.error("Error starting session", e);
            showError(sessionID, e.getMessage());
        }
    }

    public String transformReceiving(Contact contact, String msgText)
    {
        SessionID sessionID = getSessionID(contact);
        try
        {
            return otrEngine.transformReceiving(sessionID, msgText);
        }
        catch (OtrException e)
        {
            logger.error("Error receiving the message", e);
            showError(sessionID, e.getMessage());
            return null;
        }
    }

    public String transformSending(Contact contact, String msgText)
    {
        SessionID sessionID = getSessionID(contact);
        try
        {
            return otrEngine.transformSending(sessionID, msgText);
        }
        catch (OtrException e)
        {
            logger.error("Error transforming the message", e);
            showError(sessionID, e.getMessage());
            return null;
        }
    }

    private Session getSession(Contact contact)
    {
        SessionID sessionID = getSessionID(contact);
        return otrEngine.getSession(sessionID);
    }

    @Override
    public void initSmp(Contact contact, String question, String secret)
    {
        Session session = getSession(contact);
        try
        {
            session.initSmp(question, secret);

            SmpProgressDialog progressDialog = progressDialogMap.get(contact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(contact, progressDialog);
            }

            progressDialog.init();
            progressDialog.setVisible(true);
        }
        catch (OtrException e)
        {
            logger.error("Error initializing SMP session with contact "
                         + contact.getDisplayName(), e);
            showError(session.getSessionID(), e.getMessage());
        }
    }

    @Override
    public void respondSmp(Contact contact, String question, String secret)
    {
        Session session = getSession(contact);
        try
        {
            session.respondSmp(question, secret);

            SmpProgressDialog progressDialog = progressDialogMap.get(contact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(contact, progressDialog);
            }

            progressDialog.incrementProgress();
            progressDialog.setVisible(true);
        }
        catch (OtrException e)
        {
            logger.error(
                "Error occured when sending SMP response to contact "
                + contact.getDisplayName(), e);
            showError(session.getSessionID(), e.getMessage());
        }
    }

    @Override
    public void abortSmp(Contact contact)
    {
        Session session = getSession(contact);
        try
        {
            session.abortSmp();

            SmpProgressDialog progressDialog = progressDialogMap.get(contact);
            if (progressDialog == null)
            {
                progressDialog = new SmpProgressDialog(contact);
                progressDialogMap.put(contact, progressDialog);
            }

            progressDialog.dispose();
        }
        catch (OtrException e)
        {
            logger.error("Error aborting SMP session with contact "
                         + contact.getDisplayName(), e);
            showError(session.getSessionID(), e.getMessage());
        }
        
    }
}
