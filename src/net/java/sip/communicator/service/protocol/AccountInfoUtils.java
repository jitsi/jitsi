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
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.AddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BirthDateDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.DisplayNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.EmailAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FirstNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenderDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.LastNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkAddressDetail;

/**
 * Utility class that would give to interested parties an easy access to some of
 * most popular account details, like : first name, last name, birth date, image,
 * etc.
 *
 * @author Yana Stamcheva
 */
public class AccountInfoUtils
{
    /**
     * Returns the first name of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the first name of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getFirstName(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        Iterator<GenericDetail> firstNameDetails
            =  accountInfoOpSet.getDetails(FirstNameDetail.class);

        if (firstNameDetails.hasNext())
        {
            FirstNameDetail firstName
                = (FirstNameDetail) firstNameDetails.next();

            if (firstName != null)
                return firstName.toString();
        }
        return null;
    }

    /**
     * Returns the last name of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the last name of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getLastName(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        LastNameDetail lastName = null;
        Iterator<GenericDetail> lastNameDetails
            =  accountInfoOpSet.getDetails(LastNameDetail.class);

        if (lastNameDetails.hasNext())
            lastName = (LastNameDetail) lastNameDetails.next();

        if(lastName == null)
            return null;

        return lastName.getString();
    }

    /**
     * Returns the display name of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the display name of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getDisplayName(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        DisplayNameDetail displayName = null;
        Iterator<GenericDetail> displayNameDetails
            =  accountInfoOpSet.getDetails(DisplayNameDetail.class);

        if (displayNameDetails.hasNext())
            displayName = (DisplayNameDetail) displayNameDetails.next();

        if(displayName == null)
            return null;

        return displayName.getString();
    }

    /**
     * Returns the image of the account, to which the given accountInfoOpSet
     * belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the image of the account, to which the given accountInfoOpSet
     * belongs.
     */
    public static byte[] getImage(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        ImageDetail image = null;
        Iterator<GenericDetail> imageDetails
            =  accountInfoOpSet.getDetails(ImageDetail.class);

        if (imageDetails.hasNext())
            image = (ImageDetail) imageDetails.next();

        return (image != null)
            ? image.getBytes()
            : null;
    }

    /**
     * Returns the birth date of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the birth date of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static Calendar getBirthDate(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        BirthDateDetail date = null;
        Iterator<GenericDetail> dateDetails
            =  accountInfoOpSet.getDetails(BirthDateDetail.class);

        if (dateDetails.hasNext())
            date = (BirthDateDetail) dateDetails.next();

        if(date == null)
            return null;

        return date.getCalendar();
    }

    /**
     * Returns the gender of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the gender of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getGender(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        GenderDetail gender = null;
        Iterator<GenericDetail> genderDetails
            =  accountInfoOpSet.getDetails(GenderDetail.class);

        if (genderDetails.hasNext())
            gender = (GenderDetail) genderDetails.next();

        if(gender == null)
            return null;

        return gender.getGender();
    }

    /**
     * Returns the address of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the address of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getAddress(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        AddressDetail address = null;
        Iterator<GenericDetail> addressDetails
            =  accountInfoOpSet.getDetails(AddressDetail.class);

        if (addressDetails.hasNext())
            address = (AddressDetail) addressDetails.next();

        if(address == null)
            return null;

        return address.getAddress();
    }

    /**
     * Returns the work address of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the work address of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getWorkAddress(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        WorkAddressDetail address = null;
        Iterator<GenericDetail> addressDetails
            =  accountInfoOpSet.getDetails(WorkAddressDetail.class);

        if (addressDetails.hasNext())
            address = (WorkAddressDetail) addressDetails.next();

        if(address == null)
            return null;

        return address.getAddress();
    }

    /**
     * Returns the email address of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the email address of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getEmailAddress(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        EmailAddressDetail address = null;
        Iterator<GenericDetail> addressDetails
            =  accountInfoOpSet.getDetails(EmailAddressDetail.class);

        if (addressDetails.hasNext())
            address = (EmailAddressDetail) addressDetails.next();

        if(address == null)
            return null;

        return address.getEMailAddress();
    }
}
