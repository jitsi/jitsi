/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

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
        FirstNameDetail firstName = null;
        Iterator<GenericDetail> firstNameDetails
            =  accountInfoOpSet.getDetails(FirstNameDetail.class);

        if (firstNameDetails.hasNext())
            firstName = (FirstNameDetail) firstNameDetails.next();

        if(firstName == null)
            return null;

        return firstName.getString();
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
