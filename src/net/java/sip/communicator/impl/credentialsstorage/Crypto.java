/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.credentialsstorage;

import net.java.sip.communicator.service.credentialsstorage.*;

/**
 * Allows to encrypt and decrypt text using a symmetric algorithm.
 *
 * @author Dmitri Melnikov
 */
public interface Crypto
{
    /**
     * Decrypts the cipher text and returns the result.
     * 
     * @param ciphertext base64 encoded encrypted data
     * @return decrypted data
     * @throws CryptoException when the ciphertext cannot be decrypted with the
     *             key or on decryption error.
     */
    public String decrypt(String ciphertext) throws CryptoException;

    /**
     * Encrypts the plain text and returns the result.
     * 
     * @param plaintext data to be encrypted
     * @return base64 encoded encrypted data
     * @throws CryptoException on encryption error
     */
    public String encrypt(String plaintext) throws CryptoException;
}
