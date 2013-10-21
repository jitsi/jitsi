/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.plaf.basic.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.plugin.desktoputil.*;
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
            OtrPolicy.OTRL_POLICY_DEFAULT));
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

    /**
     * The dialog that pops up when SMP negotiation starts.
     * It contains a progress bar that indicates the status of the SMP
     * authentication process.
     */
    @SuppressWarnings("serial")
    private class SmpProgressDialog
        extends SIPCommDialog
    {
        private final JProgressBar progressBar = new JProgressBar(0, 100);

        private final Color successColor = new Color(86, 140, 2);

        private final Color failColor = new Color(204, 0, 0);

        private final JLabel iconLabel = new JLabel();

        /**
         * Instantiates SmpProgressDialog.
         * 
         * @param contact The contact that this dialog is associated with.
         */
        public SmpProgressDialog(Contact contact)
        {
            setTitle(
                OtrActivator.resourceService.getI18NString(
                    "plugin.otr.smpprogressdialog.TITLE"));

            JPanel mainPanel = new TransparentPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
            mainPanel.setPreferredSize(new Dimension(300, 70));

            String authFromText =
                String.format(
                    OtrActivator.resourceService
                        .getI18NString(
                            "plugin.otr.authbuddydialog.AUTHENTICATION_FROM",
                            new String[] {contact.getDisplayName()}));

            JPanel labelsPanel = new TransparentPanel();
            labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.X_AXIS));

            labelsPanel.add(iconLabel);
            labelsPanel.add(Box.createRigidArea(new Dimension(5,0)));
            labelsPanel.add(new JLabel(authFromText));

            mainPanel.add(labelsPanel);
            mainPanel.add(progressBar);

            init();

            this.getContentPane().add(mainPanel);
            this.pack();
        }

        /**
         * Initializes the progress bar and sets it's progression to 1/3.
         */
        public void init()
        {
            progressBar.setUI(new BasicProgressBarUI() {
                private Rectangle r = new Rectangle();

                @Override
                protected void paintIndeterminate(Graphics g, JComponent c) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                    r = getBox(r);
                    g.setColor(progressBar.getForeground());
                    g.fillOval(r.x, r.y, r.width, r.height);
                }
            });
            progressBar.setValue(33);
            progressBar.setForeground(successColor);
            progressBar.setStringPainted(false);
            iconLabel.setIcon(
                OtrActivator.resourceService.getImage(
                    "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_22x22"));
        }

        /**
         * Sets the progress bar to 2/3 of completion.
         */
        public void incrementProgress()
        {
            progressBar.setValue(66);
        }

        /**
         * Sets the progress bar to green.
         */
        public void setProgressSuccess()
        {
            progressBar.setValue(100);
            progressBar.setForeground(successColor);
            progressBar.setStringPainted(true);
            progressBar.setString(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.smpprogressdialog.AUTHENTICATION_SUCCESS"));
            iconLabel.setIcon(
                OtrActivator.resourceService.getImage(
                    "plugin.otr.ENCRYPTED_ICON_22x22"));
        }

        /**
         * Sets the progress bar to red.
         */
        public void setProgressFail()
        {
            progressBar.setValue(100);
            progressBar.setForeground(failColor);
            progressBar.setStringPainted(true);
            progressBar.setString(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.smpprogressdialog.AUTHENTICATION_FAIL"));
        }
    }

    /**
     * The dialog that pops up when the remote party send us SMP
     * request. It contains detailed information for the user about
     * the authentication process and allows him to authenticate.
     *
     */
    @SuppressWarnings("serial")
    private class SmpAuthenticateBuddyDialog
        extends SIPCommDialog
    {
        private final Contact contact;

        private final String question;

        SmpAuthenticateBuddyDialog(Contact contact, String question)
        {
            this.contact = contact;
            this.question = question;
            initComponents();
        }

        private void initComponents()
        {
            this.setTitle(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.TITLE"));

            // The main panel that contains all components.
            JPanel mainPanel = new TransparentPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
            mainPanel.setPreferredSize(new Dimension(300, 350));

            // Add "authentication from contact" to the main panel.
            JTextArea authenticationFrom = new CustomTextArea();
            Font newFont =
                new Font(
                    UIManager.getDefaults().getFont("TextArea.font").
                        getFontName()
                    , Font.BOLD
                    , 14);
            authenticationFrom.setFont(newFont);
            String authFromText =
                String.format(
                    OtrActivator.resourceService
                        .getI18NString(
                            "plugin.otr.authbuddydialog.AUTHENTICATION_FROM",
                            new String[] {contact.getDisplayName()}));
            authenticationFrom.setText(authFromText);
            mainPanel.add(authenticationFrom);

            // Add "general info" text to the main panel.
            JTextArea generalInfo = new CustomTextArea();
            generalInfo.setText(OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.authbuddydialog.AUTHENTICATION_INFO"));
            mainPanel.add(generalInfo);

            // Add "authentication-by-secret" info text to the main panel.
            JTextArea authBySecretInfo = new CustomTextArea();
            newFont =
                new Font(
                    UIManager.getDefaults().getFont("TextArea.font").
                        getFontName()
                    , Font.ITALIC
                    , 10);
            authBySecretInfo.setText(OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.authbuddydialog.AUTH_BY_SECRET_INFO_RESPOND"));
            authBySecretInfo.setFont(newFont);
            mainPanel.add(authBySecretInfo);

            // Create a panel to add question/answer related components
            JPanel questionAnswerPanel = new JPanel(new GridBagLayout());
            questionAnswerPanel.setBorder(BorderFactory.createEtchedBorder());

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 5, 0, 5);
            c.weightx = 0;

            // Add question label.
            JLabel questionLabel =
                new JLabel(
                    OtrActivator.resourceService
                        .getI18NString(
                            "plugin.otr.authbuddydialog.QUESTION_RESPOND"));
            questionAnswerPanel.add(questionLabel, c);

            // Add the question.
            c.insets = new Insets(0, 5, 5, 5);
            c.gridy = 1;
            JTextArea questionArea = 
                new CustomTextArea();
            newFont =
                new Font(
                    UIManager.getDefaults().getFont("TextArea.font").
                        getFontName()
                    , Font.BOLD
                    , UIManager.getDefaults().getFont("TextArea.font")
                        .getSize());
            questionArea.setFont(newFont);
            questionArea.setText(question);
            questionAnswerPanel.add(questionArea, c);

            // Add answer label.
            c.insets = new Insets(5, 5, 5, 5);
            c.gridy = 2;
            JLabel answerLabel =
                new JLabel(OtrActivator.resourceService
                    .getI18NString("plugin.otr.authbuddydialog.ANSWER"));
            questionAnswerPanel.add(answerLabel, c);

            // Add the answer text field.
            c.gridy = 3;
            final JTextField answerTextBox = new JTextField();
            questionAnswerPanel.add(answerTextBox, c);

            // Add the question/answer panel to the main panel.
            mainPanel.add(questionAnswerPanel);

            // Buttons panel.
            JPanel buttonPanel = new TransparentPanel(new GridBagLayout());
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

            JButton helpButton =
                new JButton(OtrActivator.resourceService
                    .getI18NString("plugin.otr.authbuddydialog.HELP"));
            helpButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    OtrActivator.scOtrEngine.launchHelp();
                }
            });

            c.gridwidth = 1;
            c.gridy = 0;
            c.gridx = 0;
            c.weightx = 0;
            c.insets = new Insets(5, 5, 5, 20);
            buttonPanel.add(helpButton, c);

            JButton cancelButton =
                new JButton(OtrActivator.resourceService
                    .getI18NString("plugin.otr.authbuddydialog.CANCEL"));
            cancelButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    ScOtrEngineImpl.this.abortSmp(contact);
                    SmpAuthenticateBuddyDialog.this.dispose();
                }
            });
            c.insets = new Insets(5, 5, 5, 5);
            c.gridx = 1;
            buttonPanel.add(cancelButton, c);

            c.gridx = 2;
            JButton authenticateButton =
                new JButton(OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.AUTHENTICATE_BUDDY"));
            authenticateButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    ScOtrEngineImpl.this.respondSmp(
                        contact, question, answerTextBox.getText());
                    SmpAuthenticateBuddyDialog.this.dispose();
                }
            });

            buttonPanel.add(authenticateButton, c);

            this.getContentPane().add(mainPanel, BorderLayout.NORTH);
            this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            this.pack();
        }
    }
}
