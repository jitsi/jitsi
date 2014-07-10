/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

/**
 * Extension for {@link ContactSourceService}s to provide a prefix for phone
 * number to be dialed.
 */
public interface PrefixedContactSourceService
{
    /**
     * Returns the global phone number prefix to be used when calling contacts
     * from this contact source.
     *
     * @return the global phone number prefix
     */
    public String getPhoneNumberPrefix();
}
