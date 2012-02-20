/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.security.*;
import java.security.spec.*;
import java.util.*;

import net.java.otr4j.crypto.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author George Politis
 */
public class ScOtrKeyManagerImpl
    implements ScOtrKeyManager
{
    private final OtrConfigurator configurator = new OtrConfigurator();

    private final List<ScOtrKeyManagerListener> listeners =
        new Vector<ScOtrKeyManagerListener>();

    public void addListener(ScOtrKeyManagerListener l)
    {
        synchronized (listeners)
        {
            if (!listeners.contains(l))
                listeners.add(l);
        }
    }

    public void removeListener(ScOtrKeyManagerListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    public void verify(Contact contact)
    {
        if ((contact == null) || isVerified(contact))
            return;

        this.configurator.setProperty(contact.getAddress()
            + ".publicKey.verified", true);

        for (ScOtrKeyManagerListener l : listeners)
            l.contactVerificationStatusChanged(contact);
    }

    public void unverify(Contact contact)
    {
        if ((contact == null) || !isVerified(contact))
            return;

        this.configurator.removeProperty(contact.getAddress()
            + ".publicKey.verified");

        for (ScOtrKeyManagerListener l : listeners)
            l.contactVerificationStatusChanged(contact);
    }

    public boolean isVerified(Contact contact)
    {
        if (contact == null)
            return false;
        
        return this.configurator.getPropertyBoolean(contact.getAddress()
            + ".publicKey.verified", false);
    }

    public String getRemoteFingerprint(Contact contact)
    {
        PublicKey remotePublicKey = loadPublicKey(contact);
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
        KeyPair keyPair = loadKeyPair(account);

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

    public void savePublicKey(Contact contact, PublicKey pubKey)
    {
        if (contact == null)
            return;

        X509EncodedKeySpec x509EncodedKeySpec =
            new X509EncodedKeySpec(pubKey.getEncoded());

        this.configurator.setProperty(contact.getAddress() + ".publicKey",
            x509EncodedKeySpec.getEncoded());

        this.configurator.removeProperty(contact.getAddress()
            + ".publicKey.verified");
    }

    public PublicKey loadPublicKey(Contact contact)
    {
        if (contact == null)
            return null;

        String userID = contact.getAddress();

        byte[] b64PubKey =
            this.configurator.getPropertyBytes(userID + ".publicKey");
        if (b64PubKey == null)
            return null;

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64PubKey);

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

    public KeyPair loadKeyPair(AccountID account)
    {
        if (account == null)
            return null;
        
        String accountID = account.getAccountUniqueID();
        // Load Private Key.
        byte[] b64PrivKey =
            this.configurator.getPropertyBytes(accountID + ".privateKey");
        if (b64PrivKey == null)
            return null;

        PKCS8EncodedKeySpec privateKeySpec =
            new PKCS8EncodedKeySpec(b64PrivKey);

        // Load Public Key.
        byte[] b64PubKey =
            this.configurator.getPropertyBytes(accountID + ".publicKey");
        if (b64PubKey == null)
            return null;

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64PubKey);

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

    public void generateKeyPair(AccountID account)
    {
        if (account == null)
            return;
        
        String accountID = account.getAccountUniqueID();
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
        PublicKey pubKey = keyPair.getPublic();
        X509EncodedKeySpec x509EncodedKeySpec =
            new X509EncodedKeySpec(pubKey.getEncoded());

        this.configurator.setProperty(accountID + ".publicKey",
            x509EncodedKeySpec.getEncoded());

        // Store Private Key.
        PrivateKey privKey = keyPair.getPrivate();
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec =
            new PKCS8EncodedKeySpec(privKey.getEncoded());

        this.configurator.setProperty(accountID + ".privateKey",
            pkcs8EncodedKeySpec.getEncoded());
    }
}
