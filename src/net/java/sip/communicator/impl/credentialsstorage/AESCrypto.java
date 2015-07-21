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
package net.java.sip.communicator.impl.credentialsstorage;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

/**
 * Performs encryption and decryption of text using AES algorithm.
 *
 * @author Dmitri Melnikov
 */
public class AESCrypto
    implements Crypto
{
    /**
     * The algorithm associated with the key.
     */
    private static final String KEY_ALGORITHM = "AES";

    /**
     * AES in ECB mode with padding.
     */
    private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5PADDING";

    /**
     * Salt used when creating the key.
     */
    private static byte[] SALT =
    { 0x0C, 0x0A, 0x0F, 0x0E, 0x0B, 0x0E, 0x0E, 0x0F };

    /**
     * Possible length of the keys in bits.
     */
    private static int[] KEY_LENGTHS = new int[]{256, 128};

    /**
     * Number of iterations to use when creating the key.
     */
    private static int ITERATION_COUNT = 1024;

    /**
     * Key derived from the master password to use for encryption/decryption.
     */
    private Key key;

    /**
     * Decryption object.
     */
    private Cipher decryptCipher;

    /**
     * Encryption object.
     */
    private Cipher encryptCipher;

    /**
     * Creates the encryption and decryption objects and the key.
     *
     * @param masterPassword used to derive the key. Can be null.
     */
    public AESCrypto(String masterPassword)
    {
        try
        {
            // we try init of key with suupplied lengths
            // we stop after the first successful attempt
            for (int i = 0; i < KEY_LENGTHS.length; i++)
            {
                decryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
                encryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);

                try
                {
                    initKey(masterPassword, KEY_LENGTHS[i]);

                    // its ok stop trying
                    break;
                }
                catch (InvalidKeyException e)
                {
                    if(i == KEY_LENGTHS.length - 1)
                        throw e;
                }
            }
        }
        catch (InvalidKeyException e)
        {
            throw new RuntimeException("Invalid key", e);
        }
        catch (InvalidKeySpecException e)
        {
            throw new RuntimeException("Invalid key specification", e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Algorithm not found", e);
        }
        catch (NoSuchPaddingException e)
        {
            throw new RuntimeException("Padding not found", e);
        }
    }

    /**
     * Initialize key with specified length.
     *
     * @param masterPassword used to derive the key. Can be null.
     * @param keyLength Length of the key in bits.
     * @throws InvalidKeyException if the key is invalid (bad encoding,
     * wrong length, uninitialized, etc).
     * @throws NoSuchAlgorithmException if the algorithm chosen does not exist
     * @throws InvalidKeySpecException if the key specifications are invalid
     */
    private void initKey(String masterPassword, int keyLength)
        throws  InvalidKeyException,
                NoSuchAlgorithmException,
                InvalidKeySpecException
    {
        // if the password is empty, we get an exception constructing the key
        if (masterPassword == null)
        {
            // here a default password can be set,
            // cannot be an empty string
            masterPassword = " ";
        }

        // Password-Based Key Derivation Function found in PKCS5 v2.0.
        // This is only available with java 6.
        SecretKeyFactory factory =
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        // Make a key from the master password
        KeySpec spec =
            new PBEKeySpec(masterPassword.toCharArray(), SALT,
                ITERATION_COUNT, keyLength);
        SecretKey tmp = factory.generateSecret(spec);
        // Make an algorithm specific key
        key = new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);

        // just a check whether the key size is wrong
        encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        decryptCipher.init(Cipher.DECRYPT_MODE, key);
    }

    /**
     * Decrypts the cyphertext using the key.
     *
     * @param ciphertext base64 encoded encrypted data
     * @return decrypted data
     * @throws CryptoException when the ciphertext cannot be decrypted with the
     *             key or on decryption error.
     */
    public String decrypt(String ciphertext) throws CryptoException
    {
        try
        {
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
            return new String(decryptCipher.doFinal(Base64.decode(ciphertext)),
                "UTF-8");
        }
        catch (BadPaddingException e)
        {
            throw new CryptoException(CryptoException.WRONG_KEY, e);
        }
        catch (Exception e)
        {
            throw new CryptoException(CryptoException.DECRYPTION_ERROR, e);
        }
    }

    /**
     * Encrypts the plaintext using the key.
     *
     * @param plaintext data to be encrypted
     * @return base64 encoded encrypted data
     * @throws CryptoException on encryption error
     */
    public String encrypt(String plaintext) throws CryptoException
    {
        try
        {
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            return new String(Base64.encode(encryptCipher.doFinal(plaintext
                .getBytes("UTF-8"))));
        }
        catch (Exception e)
        {
            throw new CryptoException(CryptoException.ENCRYPTION_ERROR, e);
        }
    }
}
