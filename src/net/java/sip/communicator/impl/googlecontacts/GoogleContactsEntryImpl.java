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
package net.java.sip.communicator.impl.googlecontacts;

import java.util.*;

import net.java.sip.communicator.service.googlecontacts.*;

import com.google.gdata.data.*;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.extensions.*;
//disambiguation

/**
 * Google Contacts entry implementation.
 *
 * @author Sebastien Vincent
 */
public class GoogleContactsEntryImpl
    implements GoogleContactsEntry
{
    /**
     * Google Talk protocol type.
     */
    private static final String GOOGLETALK_PROTOCOL =
        "http://schemas.google.com/g/2005#GOOGLE_TALK";

    /**
     * Google Talk protocol type.
     */
    private static final String YAHOO_PROTOCOL =
        "http://schemas.google.com/g/2005#YAHOO";

    /**
     * Google Talk protocol type.
     */
    private static final String AIM_PROTOCOL =
        "http://schemas.google.com/g/2005#AIM";

    /**
     * Google Talk protocol type.
     */
    private static final String MSN_PROTOCOL =
        "http://schemas.google.com/g/2005#MSN";

    /**
     * Google Talk protocol type.
     */
    private static final String ICQ_PROTOCOL =
        "http://schemas.google.com/g/2005#ICQ";

    /**
     * Google Talk protocol type.
     */
    private static final String JABBER_PROTOCOL =
        "http://schemas.google.com/g/2005#JABBER";

    /**
     * Full name.
     */
    private String fullName = null;

    /**
     * Family name.
     */
    private String familyName = null;

    /**
     * Given name.
     */
    private String givenName = null;

    /**
     * Home mails list.
     */
    private final List<String> homeMails = new ArrayList<String>();

    /**
     * Work mails list.
     */
    private final List<String> workMails = new ArrayList<String>();

    /**
     * Home phones list.
     */
    private final List<String> homePhones = new ArrayList<String>();

    /**
     * Work phones list.
     */
    private final List<String> workPhones = new ArrayList<String>();

    /**
     * Mobile phones list.
     */
    private final List<String> mobilePhones = new ArrayList<String>();

    /**
     * IM addresses map.
     */
    private final Map<String, IMProtocol> imAddresses =
        new HashMap<String, IMProtocol>();

    /**
     * Photo link.
     */
    private String photoLink = null;

    /**
     * Google photo link.
     */
    private Link googlePhotoLink = null;

    /**
     * Get the full name.
     *
     * @return full name
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Get the family name.
     *
     * @return family name
     */
    public String getFamilyName()
    {
        return familyName;
    }

    /**
     * Get the given name.
     *
     * @return given name
     */
    public String getGivenName()
    {
        return givenName;
    }

    /**
     * Returns mails.
     *
     * @return mails
     */
    public List<String> getAllMails()
    {
        List<String> mails = new ArrayList<String>();

        for(String mail : homeMails)
        {
            mails.add(mail);
        }

        for(String mail : workMails)
        {
            mails.add(mail);
        }

        return mails;
    }

    /**
     * Adds a home mail address.
     *
     * @param mail the mail address
     */
    public void addHomeMail(String mail)
    {
        homeMails.add(mail);
    }

    /**
     * Returns home mail addresses.
     *
     * @return home mail addresses
     */
    public List<String> getHomeMails()
    {
        return homeMails;
    }

    /**
     * Adds a work mail address.
     *
     * @param mail the mail address
     */
    public void addWorkMails(String mail)
    {
        workMails.add(mail);
    }

    /**
     * Returns work mail addresses.
     *
     * @return work mail addresses
     */
    public List<String> getWorkMails()
    {
        return workMails;
    }

    /**
     * Returns telephone numbers.
     *
     * @return telephone numbers
     */
    public List<String> getAllPhones()
    {
        List<String> phones = new ArrayList<String>();

        for(String phone : mobilePhones)
        {
            phones.add(phone);
        }

        for(String phone : homePhones)
        {
            phones.add(phone);
        }

        for(String phone : workPhones)
        {
            phones.add(phone);
        }

        return phones;
    }

    /**
     * Adds a work telephone number.
     *
     * @param telephoneNumber the work telephone number
     */
    public void addWorkPhone(String telephoneNumber)
    {
        workPhones.add(telephoneNumber);
    }

    /**
     * Returns work telephone numbers.
     *
     * @return work telephone numbers
     */
    public List<String> getWorkPhones()
    {
        return workPhones;
    }

    /**
     * Adds a mobile telephone numbers.
     *
     * @param telephoneNumber the mobile telephone number
     */
    public void addMobilePhone(String telephoneNumber)
    {
        mobilePhones.add(telephoneNumber);
    }

    /**
     * Returns mobile telephone numbers.
     *
     * @return mobile telephone numbers
     */
    public List<String> getMobilePhones()
    {
        return mobilePhones;
    }

    /**
     * Adds a home telephone numbers.
     *
     * @param telephoneNumber the home telephone number
     */
    public void addHomePhone(String telephoneNumber)
    {
        homePhones.add(telephoneNumber);
    }

    /**
     * Returns home telephone numbers.
     *
     * @return home telephone numbers
     */
    public List<String> getHomePhones()
    {
        return homePhones;
    }

    /**
     * Get the photo full URI.
     *
     * @return the photo URI or null if there isn't
     */
    public String getPhoto()
    {
        return photoLink;
    }

    /**
     * Get the Google photo full URI.
     *
     * @return the Google photo URI or null if there isn't
     */
    public Link getPhotoLink()
    {
        return googlePhotoLink;
    }

    /**
     * Returns IM addresses.
     *
     * @return IM addresses
     */
    public Map<String, IMProtocol> getIMAddresses()
    {
        return imAddresses;
    }

    /**
     * Adds an IM address.
     *
     * @param imAddress IM address
     */
    public void addIMAddress(String imAddress, IMProtocol protocol)
    {
        imAddresses.put(imAddress, protocol);
    }

    /**
     * Set information.
     *
     * @param contact Google Contacts's <tt>ContactEntry</tt>
     */
    public void setField(ContactEntry contact)
    {
        Name name = contact.getName();

        if(name != null)
        {
            if(name.hasFullName())
            {
                fullName = name.getFullName().getValue();
            }

            if(name.hasFamilyName())
            {
                familyName = name.getFamilyName().getValue();
            }

            if(name.hasGivenName())
            {
                givenName = name.getGivenName().getValue();
            }
        }

        googlePhotoLink = contact.getContactPhotoLink();
        photoLink = googlePhotoLink.getHref();

        for(Email mail : contact.getEmailAddresses())
        {
            if(mail.getRel() == null)
            {
                homeMails.add(mail.getAddress());
            }
            else if(mail.getRel().contains("#home"))
            {
                homeMails.add(mail.getAddress());
            }
            else if(mail.getRel().contains("#work"))
            {
                workMails.add(mail.getAddress());
            }
            else
            {
                homeMails.add(mail.getAddress());
            }
        }

        for(PhoneNumber phone : contact.getPhoneNumbers())
        {
            if(phone.getRel() == null)
            {
                homePhones.add(phone.getPhoneNumber());
            }
            else if(phone.getRel().contains("#work"))
            {
                workPhones.add(phone.getPhoneNumber());
            }
            else if(phone.getRel().contains("#mobile"))
            {
                mobilePhones.add(phone.getPhoneNumber());
            }
            else if(phone.getRel().contains("#home"))
            {
                homePhones.add(phone.getPhoneNumber());
            }
            else
            {
                homePhones.add(phone.getPhoneNumber());
            }
        }

        for(Im imAddress : contact.getImAddresses())
        {
            String protocol = imAddress.getProtocol();
            IMProtocol proto;

            if(protocol == null)
            {
                proto = GoogleContactsEntry.IMProtocol.OTHER;
            }
            else if(protocol.equals(GOOGLETALK_PROTOCOL))
            {
                proto = GoogleContactsEntry.IMProtocol.GOOGLETALK;
            }
            else if(protocol.equals(YAHOO_PROTOCOL))
            {
                proto = GoogleContactsEntry.IMProtocol.YAHOO;
            }
            else if(protocol.equals(AIM_PROTOCOL))
            {
                proto = GoogleContactsEntry.IMProtocol.AIM;
            }
            else if(protocol.equals(MSN_PROTOCOL))
            {
                proto = GoogleContactsEntry.IMProtocol.MSN;
            }
            else if(protocol.equals(ICQ_PROTOCOL))
            {
                proto = GoogleContactsEntry.IMProtocol.ICQ;
            }
            else if(protocol.equals(JABBER_PROTOCOL))
            {
                proto = GoogleContactsEntry.IMProtocol.JABBER;
            }
            else
            {
                proto = GoogleContactsEntry.IMProtocol.OTHER;
            }

            imAddresses.put(imAddress.getAddress(), proto);
        }
    }

    /**
     * String representation of the <tt>GoogleContactsEntry</tt>.
     */
    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        if(fullName != null)
        {
            buffer.append("Full name: ");
            buffer.append(fullName);
            buffer.append("\n");
        }

        if(givenName != null || familyName != null)
        {
            buffer.append("Display name: ");
            buffer.append(givenName != null ? givenName : "");
            buffer.append(" ");
            buffer.append(familyName != null ? familyName : "");
            buffer.append("\n");
        }

        if(getAllMails().size() > 0)
        {
            buffer.append("Mail:\n");

            for(String mail : getAllMails())
            {
                buffer.append("\t");
                buffer.append(mail);
                buffer.append("\n");
            }
        }

        if(workPhones.size() > 0)
        {
            buffer.append("Work phones:\n");
            for(String phone : workPhones)
            {
                buffer.append("\t");
                buffer.append(phone);
                buffer.append("\n");
            }
        }

        if(homePhones.size() > 0)
        {
            buffer.append("Home phones:\n");
            for(String phone : homePhones)
            {
                buffer.append("\t");
                buffer.append(phone);
                buffer.append("\n");
            }
        }

        if(mobilePhones.size() > 0)
        {
            buffer.append("Mobile phones:\n");
            for(String phone : mobilePhones)
            {
                buffer.append("\t");
                buffer.append(phone);
                buffer.append("\n");
            }
        }

        if(imAddresses.size() > 0)
        {
            buffer.append("IM addresses:\n");

            for(Map.Entry<String, IMProtocol> entry : imAddresses.entrySet())
            {
                buffer.append("\t");
                buffer.append(entry.getKey());
                buffer.append(" (");
                buffer.append(entry.getValue());
                buffer.append(")\n");
            }
        }

        buffer.append("\n");
        return buffer.toString();
    }
}
