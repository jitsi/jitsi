package net.java.sip.communicator.plugin.otr;

// import java.awt.Desktop;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.*;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.bouncycastle.util.encoders.Base64;

import net.java.otr4j.OtrEngine;
import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import net.java.sip.communicator.service.gui.PopupDialog;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;

public class ScOtrEngineImpl
    implements ScOtrEngine
{

    private List<ScOtrEngineListener> listeners =
        new Vector<ScOtrEngineListener>();

    public void addListener(ScOtrEngineListener l)
    {
        listeners.add(l);
    }

    public void removeListener(ScOtrEngineListener l)
    {
        listeners.remove(l);
    }

    private List<String> injectedMessageUIDs = new Vector<String>();

    public boolean isMessageUIDInjected(String mUID)
    {
        return injectedMessageUIDs.contains(mUID);
    }

    class ScOtrKeyManager
        implements OtrKeyManager
    {
        public KeyPair getKeyPair(SessionID sessionID)
        {
            String accountID = sessionID.getAccountID();
            KeyPair keyPair = loadKeyPair(accountID);
            if (keyPair == null)
                generateKeyPair(accountID);

            return loadKeyPair(accountID);
        }
    }

    class ScOtrEngineHost
        implements OtrEngineHost
    {
        public void showWarning(SessionID sessionID, String warn)
        {
            // TODO Dialog usage is not that great.
            OtrActivator.uiService.getPopupDialog().showMessagePopupDialog(
                warn, "OTR warning", PopupDialog.WARNING_MESSAGE);
        }

        public void showError(SessionID sessionID, String err)
        {
            // TODO Dialog usage is not that great.
            OtrActivator.uiService.getPopupDialog().showMessagePopupDialog(err,
                "OTR Error", PopupDialog.ERROR_MESSAGE);
        }

        public void injectMessage(SessionID sessionID, String messageText)
        {
            Contact contact = contactsMap.get(sessionID);
            OperationSetBasicInstantMessaging imOpSet =
                (OperationSetBasicInstantMessaging) contact
                    .getProtocolProvider().getOperationSet(
                        OperationSetBasicInstantMessaging.class);

            Message message = imOpSet.createMessage(messageText);
            injectedMessageUIDs.add(message.getMessageUID());
            imOpSet.sendInstantMessage(contact, message);
        }

        public OtrPolicy getSessionPolicy(SessionID sessionID)
        {
            return getContactPolicy(contactsMap.get(sessionID));
        }

        public void sessionStatusChanged(SessionID sessionID)
        {
            Contact contact = contactsMap.get(sessionID);
            if (contact == null)
                return;

            switch (otrEngine.getSessionStatus(sessionID))
            {
            case ENCRYPTED:
                PublicKey remotePubKey =
                    otrEngine.getRemotePublicKey(sessionID);

                PublicKey storedPubKey = loadPublicKey(sessionID.getUserID());

                if (!remotePubKey.equals(storedPubKey))
                    savePublicKey(sessionID.getUserID(), remotePubKey);
                break;
            }

            for (ScOtrEngineListener l : listeners)
            {
                l.sessionStatusChanged(contact);
            }
        }
    }

    private OtrEngine otrEngine =
        new OtrEngineImpl(new ScOtrEngineHost(), new ScOtrKeyManager());

    public boolean isContactVerified(Contact contact)
    {
        String id = getSessionNS(getSessionID(contact), "publicKey.verified");
        if (id == null || id.length() < 1)
            return false;

        return OtrActivator.configService.getBoolean(id, false);
    }

    Map<SessionID, Contact> contactsMap = new Hashtable<SessionID, Contact>();

    public void endSession(Contact contact)
    {
        otrEngine.endSession(getSessionID(contact));
    }

    public SessionStatus getSessionStatus(Contact contact)
    {
        return otrEngine.getSessionStatus(getSessionID(contact));
    }

    public String transformReceiving(Contact contact, String msgText)
    {
        return otrEngine.transformReceiving(getSessionID(contact), msgText);
    }

    public String transformSending(Contact contact, String msgText)
    {
        return otrEngine.transformSending(getSessionID(contact), msgText);
    }

    public void refreshSession(Contact contact)
    {
        otrEngine.refreshSession(getSessionID(contact));
    }

    public void startSession(Contact contact)
    {
        otrEngine.startSession(getSessionID(contact));
    }

    private SessionID getSessionID(Contact contact)
    {
        SessionID sessionID =
            new SessionID(contact.getProtocolProvider().getAccountID()
                .getAccountUniqueID(), contact.getAddress(), contact
                .getProtocolProvider().getProtocolName());

        contactsMap.put(sessionID, contact);
        return sessionID;
    }

    public String getRemoteFingerprint(Contact contact)
    {
        PublicKey remotePublicKey = loadPublicKey(contact.getAddress());
        if (remotePublicKey == null)
            return null;
        try
        {
            return new OtrCryptoEngineImpl().getFingerprint(remotePublicKey);
        }
        catch (OtrCryptoException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String getLocalFingerprint(AccountID account)
    {
        KeyPair keyPair = loadKeyPair(account.getAccountUniqueID());

        if (keyPair == null)
            return null;

        PublicKey pubKey = keyPair.getPublic();

        try
        {
            return new OtrCryptoEngineImpl().getFingerprint(pubKey);
        }
        catch (OtrCryptoException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private String getSessionNS(SessionID sessionID, String function)
    {
        try
        {
            return "net.java.sip.comunicator.plugin.otr."
                + URLEncoder.encode(sessionID.toString(), "UTF-8") + "."
                + function;
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
            return null;
        }
    }

    public void verifyContactFingerprint(Contact contact)
    {
        if (contact == null)
            return;

        String id = getSessionNS(getSessionID(contact), "publicKey.verified");
        if (id == null || id.length() < 1)
            return; // TODO provide error handling.

        OtrActivator.configService.setProperty(id, true);
    }

    public void forgetContactFingerprint(Contact contact)
    {
        if (contact == null)
            return;

        String id = getSessionNS(getSessionID(contact), "publicKey.verified");
        if (id == null || id.length() < 1)
            return; // TODO provide error handling.

        OtrActivator.configService.removeProperty(id);

    }

    public OtrPolicy getGlobalPolicy()
    {
        return new OtrPolicyImpl(OtrActivator.configService.getInt(
            "net.java.sip.comunicator.plugin.otr.POLICY",
            OtrPolicy.OTRL_POLICY_DEFAULT));
    }

    public void setGlobalPolicy(OtrPolicy policy)
    {
        if (policy == null)
            OtrActivator.configService
                .removeProperty("net.java.sip.comunicator.plugin.otr.POLICY");
        else
            OtrActivator.configService.setProperty(
                "net.java.sip.comunicator.plugin.otr.POLICY", policy
                    .getPolicy());

        for (ScOtrEngineListener l : listeners)
            l.globalPolicyChanged();
    }

    public void launchHelp()
    {
//        boolean fallback = false;
//        if (!Desktop.isDesktopSupported())
//        {
//            fallback = true;
//        }
//        else
//        {
//            try
//            {
//                Desktop.getDesktop().browse(
//                    new URI(OtrActivator.resourceService
//                        .getI18NString("plugin.otr.authbuddydialog.HELP_URI")));
//            }
//            catch (Exception ex)
//            {
//                // not possible.
//                fallback = true;
//            }
//        }
//
//        if (fallback)
//        {
//            // TODO Either find another way to launch the URI or display
//            // a
//            // dialog, we need to discuss this first.
//        }
    }

    public OtrPolicy getContactPolicy(Contact contact)
    {
        String id = getSessionNS(getSessionID(contact), "policy");
        if (id == null || id.length() < 1)
            return getGlobalPolicy();

        int policy = OtrActivator.configService.getInt(id, -1);
        if (policy < 0)
            return getGlobalPolicy();
        else
            return new OtrPolicyImpl(policy);
    }

    public void setContactPolicy(Contact contact, OtrPolicy policy)
    {
        String id = getSessionNS(getSessionID(contact), "policy");
        if (id == null || id.length() < 1)
            return;

        if (policy == null)
            OtrActivator.configService.removeProperty(id);
        else
            OtrActivator.configService.setProperty(id, policy.getPolicy());

        for (ScOtrEngineListener l : listeners)
            l.contactPolicyChanged(contact);

    }

    private KeyPair loadKeyPair(String accountID)
    {
        // Load Private Key.
        String idPrivKey;
        try
        {
            idPrivKey =
                "net.java.sip.comunicator.plugin.otr."
                    + URLEncoder.encode(accountID, "UTF-8") + ".privateKey";
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
            return null;
        }
        Object b64PrivKey = OtrActivator.configService.getProperty(idPrivKey);
        if (b64PrivKey == null)
            return null;

        PKCS8EncodedKeySpec privateKeySpec =
            new PKCS8EncodedKeySpec(Base64.decode((String) b64PrivKey));

        // Load Public Key.
        String idPubKey;
        try
        {
            idPubKey =
                "net.java.sip.comunicator.plugin.otr."
                    + URLEncoder.encode(accountID, "UTF-8") + ".publicKey";
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
            return null;
        }
        Object b64PubKey = OtrActivator.configService.getProperty(idPubKey);
        if (b64PubKey == null)
            return null;

        X509EncodedKeySpec publicKeySpec =
            new X509EncodedKeySpec(Base64.decode((String) b64PubKey));

        PublicKey publicKey;
        PrivateKey privateKey;

        // Generate KeyPair.
        KeyFactory keyFactory;
        try
        {
            keyFactory = KeyFactory.getInstance("DSA");
            publicKey = keyFactory.generatePublic(publicKeySpec);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (InvalidKeySpecException e)
        {
            e.printStackTrace();
            return null;
        }

        return new KeyPair(publicKey, privateKey);
    }

    public void generateKeyPair(String accountID)
    {
        KeyPair keyPair;
        try
        {
            keyPair = KeyPairGenerator.getInstance("DSA").genKeyPair();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return;
        }

        // Store Public Key.
        String idPubKey;
        try
        {
            idPubKey =
                "net.java.sip.comunicator.plugin.otr."
                    + URLEncoder.encode(accountID, "UTF-8") + ".publicKey";
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
            return;
        }
        PublicKey pubKey = keyPair.getPublic();
        X509EncodedKeySpec x509EncodedKeySpec =
            new X509EncodedKeySpec(pubKey.getEncoded());
        OtrActivator.configService.setProperty(idPubKey, new String(Base64
            .encode(x509EncodedKeySpec.getEncoded())));

        // Store Private Key.
        String idPrivKey;
        try
        {
            idPrivKey =
                "net.java.sip.comunicator.plugin.otr."
                    + URLEncoder.encode(accountID, "UTF-8") + ".privateKey";
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
            return;
        }

        PrivateKey privKey = keyPair.getPrivate();
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec =
            new PKCS8EncodedKeySpec(privKey.getEncoded());
        OtrActivator.configService.setProperty(idPrivKey, new String(Base64
            .encode(pkcs8EncodedKeySpec.getEncoded())));
    }

    private void savePublicKey(String userID, PublicKey pubKey)
    {
        String idPubKey;
        try
        {
            idPubKey =
                "net.java.sip.comunicator.plugin.otr."
                    + URLEncoder.encode(userID, "UTF-8") + ".publicKey";
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return;
        }
        X509EncodedKeySpec x509EncodedKeySpec =
            new X509EncodedKeySpec(pubKey.getEncoded());

        OtrActivator.configService.setProperty(idPubKey, new String(Base64
            .encode(x509EncodedKeySpec.getEncoded())));

        OtrActivator.configService.removeProperty(idPubKey + ".verified");
    }

    private PublicKey loadPublicKey(String userID)
    {
        String idPubKey;
        try
        {
            idPubKey =
                "net.java.sip.comunicator.plugin.otr."
                    + URLEncoder.encode(userID, "UTF-8") + ".publicKey";
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return null;
        }

        Object b64PubKey = OtrActivator.configService.getProperty(idPubKey);
        if (b64PubKey == null)
            return null;

        X509EncodedKeySpec publicKeySpec =
            new X509EncodedKeySpec(Base64.decode((String) b64PubKey));

        // Generate KeyPair.
        KeyFactory keyFactory;
        try
        {
            keyFactory = KeyFactory.getInstance("DSA");
            return keyFactory.generatePublic(publicKeySpec);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (InvalidKeySpecException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
