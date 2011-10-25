/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.security.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author George Politis
 */
public class ScOtrEngineImpl
    implements ScOtrEngine
{
    private final OtrConfigurator configurator = new OtrConfigurator();

    private static final Map<SessionID, Contact> contactsMap =
        new Hashtable<SessionID, Contact>();

    private final List<String> injectedMessageUIDs = new Vector<String>();

    private final List<ScOtrEngineListener> listeners =
        new Vector<ScOtrEngineListener>();

    private final OtrEngine otrEngine
        = new OtrEngineImpl(new ScOtrEngineHost());

    public void addListener(ScOtrEngineListener l)
    {
        synchronized (listeners)
        {
            if (!listeners.contains(l))
                listeners.add(l);
        }
    }

    public void removeListener(ScOtrEngineListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    public boolean isMessageUIDInjected(String mUID)
    {
        return injectedMessageUIDs.contains(mUID);
    }

    class ScOtrEngineHost
        implements OtrEngineHost
    {
        public KeyPair getKeyPair(SessionID sessionID)
        {
            AccountID accountID =
                OtrActivator.getAccountIDByUID(sessionID.getAccountID());
            KeyPair keyPair =
                OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
            if (keyPair == null)
                OtrActivator.scOtrKeyManager.generateKeyPair(accountID);

            return OtrActivator.scOtrKeyManager.loadKeyPair(accountID);
        }

        public void showWarning(SessionID sessionID, String warn)
        {
            Contact contact = getContact(sessionID);
            if (contact == null)
                return;

            OtrActivator.uiService.getChat(contact).addMessage(
                contact.getDisplayName(), System.currentTimeMillis(),
                Chat.SYSTEM_MESSAGE, warn,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        }

        public void showError(SessionID sessionID, String err)
        {
            ScOtrEngineImpl.this.showError(sessionID, err);
        }

        public void injectMessage(SessionID sessionID, String messageText)
        {
            Contact contact = getContact(sessionID);
            OperationSetBasicInstantMessaging imOpSet
                = contact
                    .getProtocolProvider()
                        .getOperationSet(OperationSetBasicInstantMessaging.class);
            Message message = imOpSet.createMessage(messageText);

            injectedMessageUIDs.add(message.getMessageUID());
            imOpSet.sendInstantMessage(contact, message);
        }

        public OtrPolicy getSessionPolicy(SessionID sessionID)
        {
            return getContactPolicy(getContact(sessionID));
        }
    }

    public void showError(SessionID sessionID, String err)
    {
        Contact contact = getContact(sessionID);
        if (contact == null)
            return;

        OtrActivator.uiService.getChat(contact).addMessage(
            contact.getDisplayName(), System.currentTimeMillis(),
            Chat.ERROR_MESSAGE, err,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
    }
    
    public ScOtrEngineImpl()
    {
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
                        String unverifiedSessionWarning =
                            OtrActivator.resourceService
                                .getI18NString(
                                    "plugin.otr.activator.unverifiedsessionwarning",
                                    new String[]
                                    { contact.getDisplayName() });

                        OtrActivator.uiService.getChat(contact).addMessage(
                            contact.getDisplayName(),
                            System.currentTimeMillis(), Chat.SYSTEM_MESSAGE,
                            unverifiedSessionWarning,
                            OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

                    }
                    message =
                        OtrActivator.resourceService
                            .getI18NString(
                                (OtrActivator.scOtrKeyManager
                                    .isVerified(contact)) ? "plugin.otr.activator.sessionstared"
                                    : "plugin.otr.activator.unverifiedsessionstared",
                                new String[]
                                { contact.getDisplayName() });

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
                    contact.getDisplayName(), System.currentTimeMillis(),
                    Chat.SYSTEM_MESSAGE, message,
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE);

                for (ScOtrEngineListener l : listeners)
                {
                    l.sessionStatusChanged(contact);
                }
            }
        });
    }

    public static SessionID getSessionID(Contact contact)
    {
        SessionID sessionID =
            new SessionID(contact.getProtocolProvider().getAccountID()
                .getAccountUniqueID(), contact.getAddress(), contact
                .getProtocolProvider().getProtocolName());

        synchronized (contactsMap)
        {
            contactsMap.put(sessionID, contact);
        }

        return sessionID;
    }

    public static Contact getContact(SessionID sessionID)
    {
        return contactsMap.get(sessionID);
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

    public SessionStatus getSessionStatus(Contact contact)
    {
        return otrEngine.getSessionStatus(getSessionID(contact));
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
            showError(sessionID, e.getMessage());
            return null;
        }
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
            showError(sessionID, e.getMessage());
        }
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
            showError(sessionID, e.getMessage());
        }
    }

    public OtrPolicy getGlobalPolicy()
    {
        return new OtrPolicyImpl(this.configurator.getPropertyInt("POLICY",
            OtrPolicy.OTRL_POLICY_DEFAULT));
    }

    public void setGlobalPolicy(OtrPolicy policy)
    {
        if (policy == null)
            this.configurator.removeProperty("POLICY");
        else
            this.configurator.setProperty("POLICY", policy.getPolicy());

        for (ScOtrEngineListener l : listeners)
            l.globalPolicyChanged();
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

    public void setContactPolicy(Contact contact, OtrPolicy policy)
    {
        String propertyID = getSessionID(contact) + "policy";
        if (policy == null)
            this.configurator.removeProperty(propertyID);
        else
            this.configurator.setProperty(propertyID, policy.getPolicy());

        for (ScOtrEngineListener l : listeners)
            l.contactPolicyChanged(contact);
    }
}
