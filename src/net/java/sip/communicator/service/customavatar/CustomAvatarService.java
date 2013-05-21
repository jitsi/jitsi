/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.customavatar;

/**
 * Service implementers can give a custom way of retrieving
 * avatars for given contact address. ProtocolProviders will use
 * these methods to search for avatar if their contacts are missing
 * picture.
 * @author Damian Minkov
 */
public interface CustomAvatarService
{
    /**
     * Returns the avatar bytes for the given contact address.
     * @param contactAddress the address of a contact to search for its avatar.
     * @return image bytes.
     */
    public byte[] getAvatar(String contactAddress);
}
