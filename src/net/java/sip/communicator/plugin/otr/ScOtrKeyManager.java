/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.security.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author George Politis
 *
 */
public interface ScOtrKeyManager
{

    public abstract void addListener(ScOtrKeyManagerListener l);

    public abstract void removeListener(ScOtrKeyManagerListener l);

    public abstract void verify(Contact contact);

    public abstract void unverify(Contact contact);

    public abstract boolean isVerified(Contact contact);

    public abstract String getRemoteFingerprint(Contact contact);

    public abstract String getLocalFingerprint(AccountID account);

    public abstract void savePublicKey(Contact contact, PublicKey pubKey);

    public abstract PublicKey loadPublicKey(Contact contact);

    public abstract KeyPair loadKeyPair(AccountID accountID);

    public abstract void generateKeyPair(AccountID accountID);

}