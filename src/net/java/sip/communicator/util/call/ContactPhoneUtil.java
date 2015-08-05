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
package net.java.sip.communicator.util.call;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;

/**
 * Utility class used to check if there is a telephony service, video calls and
 * desktop sharing enabled for a protocol specific <tt>Contact</tt>.
 *
 * @author Yana Stamcheva
 */
public class ContactPhoneUtil
{
    /**
     * The logger for this class.
     */
    private static  final Logger logger
        = Logger.getLogger(ContactPhoneUtil.class);

    /**
     * Searches for phones for the contact.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact to check.
     * @param listener the <tt>DetailsResponseListener</tt> if we're interested
     * in obtaining results that came later
     * @param onlyVideo whether to include only video phones.
     * @param localized whether to localize phones.
     * @return list of phones, or null if we will use the listeners
     * for the result.
     */
    public static List<String> getContactAdditionalPhones(
                                            Contact contact,
                                            DetailsResponseListener listener,
                                            boolean onlyVideo,
                                            boolean localized)
    {
        OperationSetServerStoredContactInfo infoOpSet =
            contact.getProtocolProvider().getOperationSet(
                OperationSetServerStoredContactInfo.class);
        Iterator<GenericDetail> details;
        ArrayList<String> phonesList = new ArrayList<String>();

        if(infoOpSet != null)
        {
            try
            {
                if(listener != null)
                {
                    details = infoOpSet.requestAllDetailsForContact(
                        contact, listener);

                    if(details == null)
                        return null;
                }
                else
                {
                    details = infoOpSet.getAllDetailsForContact(contact);
                }

                ArrayList<String> phoneNumbers = new ArrayList<String>();
                while(details.hasNext())
                {
                    GenericDetail d = details.next();

                    if(d instanceof PhoneNumberDetail &&
                        !(d instanceof PagerDetail) &&
                        !(d instanceof FaxDetail))
                    {
                        PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                        String number = pnd.getNumber();
                        if(number != null &&
                            number.length() > 0)
                        {
                            if(!(d instanceof VideoDetail) && onlyVideo)
                                continue;

                            // skip duplicate numbers
                            if(phoneNumbers.contains(number))
                                continue;

                            phoneNumbers.add(number);

                            if(!localized)
                            {
                                phonesList.add(number);
                                continue;
                            }

                            phonesList.add(number
                                + " (" + getLocalizedPhoneNumber(d) + ")");
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                logger.error("Error obtaining server stored contact info");
            }
        }

        return phonesList;
    }

    /**
     * Returns localized phone number.
     *
     * @param d the detail.
     * @return the localized phone number.
     */
    protected static String getLocalizedPhoneNumber(GenericDetail d)
    {
        if(d instanceof WorkPhoneDetail)
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.WORK_PHONE");
        }
        else if(d instanceof MobilePhoneDetail)
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.MOBILE_PHONE");
        }
        else if(d instanceof VideoDetail)
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.VIDEO_PHONE");
        }
        else
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.HOME");
        }
    }
}
