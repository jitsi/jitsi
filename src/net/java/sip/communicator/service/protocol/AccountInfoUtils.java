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
        Iterator<FirstNameDetail> firstNameDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.FirstNameDetail.class);

        if (firstNameDetails.hasNext())
        {
            firstName = firstNameDetails.next();
        }

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
        Iterator<LastNameDetail> lastNameDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.LastNameDetail.class);

        if (lastNameDetails.hasNext())
        {
            lastName = lastNameDetails.next();
        }

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
        Iterator<DisplayNameDetail> displayNameDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.DisplayNameDetail.class);

        if (displayNameDetails.hasNext())
        {
            displayName = displayNameDetails.next();
        }

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
        Iterator<ImageDetail> imageDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.ImageDetail.class);

        if (imageDetails.hasNext())
        {
            image = imageDetails.next();
        }

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
        Iterator<BirthDateDetail> dateDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.BirthDateDetail.class);

        if (dateDetails.hasNext())
        {
            date = dateDetails.next();
        }

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
        Iterator<GenderDetail> genderDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.GenderDetail.class);

        if (genderDetails.hasNext())
        {
            gender = genderDetails.next();
        }

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
        Iterator<AddressDetail> addressDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.AddressDetail.class);

        if (addressDetails.hasNext())
        {
            address = addressDetails.next();
        }

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
        Iterator<WorkAddressDetail> addressDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.WorkAddressDetail.class);

        if (addressDetails.hasNext())
        {
            address = addressDetails.next();
        }

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
        Iterator<EmailAddressDetail> addressDetails
            =  accountInfoOpSet.getDetails(
                    ServerStoredDetails.EmailAddressDetail.class);

        if (addressDetails.hasNext())
        {
            address = addressDetails.next();
        }

        if(address == null)
            return null;

        return address.getEMailAddress();
    }
}