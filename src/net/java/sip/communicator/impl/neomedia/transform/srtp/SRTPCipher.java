/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 * 
*/
package net.java.sip.communicator.impl.neomedia.transform.srtp;

/**
 * SRTPCipher interface describes the abstract requirement for SRTP 
 * encryption algorithm. Given a byte stream and an initial vector (iv)
 * process the byte stream in place (either encrypt or decrypt)
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public interface SRTPCipher
{
    /**
     * Process (encrypt / decrypt) a byte stream, using the supplied 
     * initial vector.
     * 
     * @param data byte array containing the byte stream to be processed
     * @param offset byte stream star offset with data byte array
     * @param length byte stream length in bytes
     * @param iv initial vector for this operation
     */
    void process(byte[] data, int offset, int length, byte[] iv);
}
