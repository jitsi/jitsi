/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.keyshare;

/**
 * KeyProvider defines the currently available provider types,
 *
 * @author Damian Minkov
 */
public enum KeyProviderAlgorithm
{
        /**
         * The dummy provider
         */
        DUMMY_PROVIDER,
        /**
         * zrtp provider
         */
        ZRTP_PROVIDER
}
