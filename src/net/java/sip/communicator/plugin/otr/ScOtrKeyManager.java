/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.security.*;
import java.util.*;

import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
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

    public abstract void verify(OtrContact contact, String fingerprint);

    public abstract void unverify(OtrContact contact, String fingerprint);

    public abstract boolean isVerified(Contact contact, String fingerprint);

    public abstract String getFingerprintFromPublicKey(PublicKey pubKey);

    public abstract List<String> getAllRemoteFingerprints(Contact contact);

    public abstract String getLocalFingerprint(AccountID account);

    public abstract byte[] getLocalFingerprintRaw(AccountID account);

    public abstract void saveFingerprint(Contact contact, String fingerprint);

    public abstract KeyPair loadKeyPair(AccountID accountID);

    public abstract void generateKeyPair(AccountID accountID);

}
