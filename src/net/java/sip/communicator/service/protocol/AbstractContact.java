/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An abstract base implementation of the {@link Contact} interface which is to
 * aid implementers.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractContact
    implements Contact
{
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        else if (obj == this)
            return true;
        else if (!obj.getClass().equals(getClass()))
            return false;
        else
        {
            Contact contact = (Contact) obj;
            ProtocolProviderService protocolProvider
                = contact.getProtocolProvider();
            ProtocolProviderService thisProtocolProvider
                = getProtocolProvider();

            if ((protocolProvider == null)
                    ? (thisProtocolProvider == null)
                    : protocolProvider.equals(thisProtocolProvider))
            {
                String address = contact.getAddress();
                String thisAddress = getAddress();

                return
                    (address == null)
                        ? (thisAddress == null)
                        : address.equals(thisAddress);
            }
            else
                return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hashCode = 0;

        ProtocolProviderService protocolProvider = getProtocolProvider();

        if (protocolProvider != null)
            hashCode += protocolProvider.hashCode();

        String address = getAddress();

        if (address != null)
            hashCode += address.hashCode();

        return hashCode;
    }
}
