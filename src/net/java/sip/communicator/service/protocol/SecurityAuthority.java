/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Implemented by the user interface, this interface allows a protocol provider
 * to asynchronosly demand passwords necessary for authentication agaisn various
 * realms.
 * <p>
 * Or in other (simpler words) this is a callback or a hook that the UI would
 * give a protocol provider so that the protocol provider could
 * requestCredentials() when necessary (when a password is not available for
 * a server, or once it has changed, or redemand one after a faulty
 * authentication)
 *
 * @author Emil Ivov
 */
public interface SecurityAuthority
{

    /**
     * Returns a Credentials object associated with the specified realm.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(String          realm,
                                             UserCredentials defaultValues);
}
