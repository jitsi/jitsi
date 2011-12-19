/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.dns;

/**
 * Defines how DNSSEC validation errors should be handled.
 * 
 * @author Ingo Bauersachs
 */
public enum SecureResolveMode
{
    /**
     * Any DNSSEC data is completely ignored.
     */
    IgnoreDnssec,

    /**
     * The result of a query is only returned if it validated successfully.
     */
    SecureOnly,

    /**
     * The result of a query is returned if it validated successfully or when
     * the zone is unsigned.
     */
    SecureOrUnsigned,

    /**
     * If the result of a query is bogus (manipulated, incorrect), the user is
     * to be asked how to proceed.
     */
    WarnIfBogus,

    /**
     * If the result of a query is bogus (manipulated, incorrect) or if the zone
     * is unsigned, the user is to be asked how to proceed.
     */
    WarnIfBogusOrUnsigned
}
