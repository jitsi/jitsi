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
            String normalizedPhone = JabberActivator.getPhoneNumberI18nService()
                .normalize(phone);

            if (phone.equals(detailAddress)
                || normalizedPhone.equals(detailAddress)
                || detailAddress.contains(phone)
                || detailAddress.contains(normalizedPhone))
                return true;
        }

        return false;
    }

    /**
     * Returns the linked CUSAX provider for this protocol provider.
     *
     * @return the linked CUSAX provider for this protocol provider or null
     * if such isn't specified
     */
    public ProtocolProviderService getLinkedCusaxProvider()
    {
        return null;
    }
}
