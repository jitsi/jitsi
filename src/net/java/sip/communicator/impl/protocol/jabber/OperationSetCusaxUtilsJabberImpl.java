/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.call.*;

/**
 * The <tt>OperationSetCusaxUtilsJabberImpl</tt> provides utility methods
 * related to the Jabber CUSAX implementation.
 *
 * @author Yana Stamcheva
 */
public class OperationSetCusaxUtilsJabberImpl
    implements OperationSetCusaxUtils
{
    /**
     * The parent jabber protocol provider.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * Creates an instance of <tt>OperationSetCusaxUtilsJabberImpl</tt> by
     * specifying the parent jabber <tt>ProtocolProviderServiceJabberImpl</tt>.
     *
     * @param jabberProvider the parent
     * <tt>ProtocolProviderServiceJabberImpl</tt>
     */
    public OperationSetCusaxUtilsJabberImpl(
        ProtocolProviderServiceJabberImpl jabberProvider)
    {
        this.jabberProvider = jabberProvider;
    }

    /**
     * Checks if the given <tt>detailAddress</tt> exists in the given
     * <tt>contact</tt> details.
     *
     * @param contact the <tt>Contact</tt>, which details to check
     * @param detailAddress the detail address we're looking for
     * @return <tt>true</tt> if the given <tt>detailAdress</tt> exists in the
     * details of the given <tt>contact</tt>
     */
    public boolean doesDetailBelong(Contact contact, String detailAddress)
    {
        List<String> contactPhones
            = ContactPhoneUtil.getContactAdditionalPhones(
                contact, null, false, false);

        if (contactPhones == null || contactPhones.size() <= 0)
            return false;

        Iterator<String> phonesIter = contactPhones.iterator();

        while (phonesIter.hasNext())
        {
            String phone = phonesIter.next();

            if (phone.equals(detailAddress)
                || PhoneNumberI18nService.normalize(phone)
                    .equals(detailAddress))
                return true;
        }

        return false;
    }
}