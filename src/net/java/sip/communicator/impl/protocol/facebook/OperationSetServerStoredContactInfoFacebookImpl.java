/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.imageio.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;

/**
 * Operation Set Server Stored Contact Info Facebook Implementation
 *
 * @author Dai Zhiwei
 */
public class OperationSetServerStoredContactInfoFacebookImpl
    implements OperationSetServerStoredContactInfo
{
    /**
     * Our class logger
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetServerStoredContactInfoFacebookImpl.class);

    /**
     * A callback to the Facebook provider that created us.
     */
    private final ProtocolProviderServiceFacebookImpl parentProvider;

    /**
     * All the details retrieved so far is kept here
     */
    private final Map<String, List<GenericDetail>> retreivedDetails
        = new Hashtable<String, List<GenericDetail>>();

    /**
     * Details retreived addresses
     */
    private final Set<String> detailsRetreivedAddresses = new HashSet<String>();

    /**
     * Host address for retreiving images
     */
    private static String fileHostUrl = "http://static.ak.fbcdn.net";

    /**
     * Creates a new instance of this class using the specified parent
     * <tt>provider</tt>.
     *
     * @param provider the provider that's creating us.
     */
    protected OperationSetServerStoredContactInfoFacebookImpl(
        ProtocolProviderServiceFacebookImpl provider)
    {
        this.parentProvider = provider;
    }

    /**
     * Returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator<GenericDetail> getDetailsAndDescendants(
            Contact contact,
            Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(contact.getAddress());
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        if (details == null)
            return result.iterator();

        for (GenericDetail item : details)
            //the class AND its descendants
            if (detailClass.isInstance(item))
                result.add(item);

        return result.iterator();
    }

    /**
     * Returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator<GenericDetail> getDetails(
            Contact contact,
            Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(contact.getAddress());
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        if (details == null)
            return result.iterator();

        for (GenericDetail item : details)
            //exactly that class not its descendants
            if (detailClass.equals(item.getClass()))
                result.add(item);

        return result.iterator();
    }

    /**
     * Request the full info for the given uin
     * waits and return this details
     *
     * @param contact Contact
     * @return Iterator
     */
    public Iterator<GenericDetail> getAllDetailsForContact(Contact contact)
    {
        List<GenericDetail> details = getContactDetails(contact.getAddress());

        if(details == null)
            return new LinkedList<GenericDetail>().iterator();
        else
            return new LinkedList<GenericDetail>(details).iterator();
    }

    /**
     * Request the full info for the given contactAddress
     * waits and return this details
     *
     * @param contactAddress String
     * @return Vector the details
     */
    private List<GenericDetail> getContactDetails(String contactAddress)
    {
        List<GenericDetail> result = retreivedDetails.get(contactAddress);

        if ((result == null)
                || !detailsRetreivedAddresses.contains(contactAddress))
        {
            result = new LinkedList<GenericDetail>();
            try
            {
                /**
                 * Summary:
                 * First Name;
                 * Middle Name;
                 * Last Name;
                 * Gender;
                 * Birth Date;
                 * Age;
                 * E-mail;
                 * Phone..
                 *
                 * Extended:
                 * ...
                 */
                FacebookAdapter adapter = parentProvider.getAdapter();
                OperationSetPersistentPresenceFacebookImpl presenceOS
                    = getParentPresenceOperationSet();

                if(adapter == null || presenceOS == null)
                    return null;

                String tmpValueStr = "Who Am I";
                ContactFacebookImpl contact = (ContactFacebookImpl)
                    presenceOS.findContactByID(contactAddress);

                //avatar, name, first name
                if(contact != null)
                {
                    /*
                     * TODO fixme "Avatar" should be loaded from resources.
                     * properties
                     */
                    byte[] imageBytes = contact.getBigImage();
                    if(imageBytes != null && imageBytes.length > 0)
                        result.add(new ServerStoredDetails.ImageDetail(
                            "Avatar", imageBytes));

                    FacebookUser metaInfo = contact.getContactInfo();
                    if(metaInfo == null)
                        return null;

                    tmpValueStr = metaInfo.name;
                    if(tmpValueStr != null)
                        result
                            .add(
                                new ServerStoredDetails.DisplayNameDetail(
                                        tmpValueStr));

                    tmpValueStr = metaInfo.firstName;
                    if(tmpValueStr != null)
                        result
                            .add(
                                new ServerStoredDetails.FirstNameDetail(
                                        tmpValueStr));
                }

                String profilePage
                    = adapter.getSession().getProfilePage(contactAddress);

                if(profilePage == null)
                    throw new Exception("Failed to load profile page");

                logger.trace("====== Profile Page: ======\n" + profilePage);

                /**
                 * @fixme should we fill the summary pannel?
                 */

                //add this contact into the set,
                //so that we needn't to fetch his/her info again
                detailsRetreivedAddresses.add(contactAddress);

                //class="profile_info"><dl class="info">
                //<div class="profile_info_container">
                String tmpPrefix = "<div class=\"profile_info_container\">";
                int beginPos = profilePage.indexOf(tmpPrefix);

                if(beginPos >= 0)
                {
                    //do something
                    beginPos += tmpPrefix.length();
                    if(beginPos >= profilePage.length())
                        throw new Exception("Failed to load profile page");

                    String tmpLabelStr;
                    int tmpLeft = profilePage.indexOf("<dt>", beginPos);
                    int tmpRight = 0;
                    while(tmpLeft >= 0 && tmpLeft < profilePage.length())
                    {
                        tmpRight = profilePage.indexOf("</dt>", tmpLeft);
                        if(tmpRight >= 0)
                        {
                            tmpLabelStr
                                = profilePage.substring(tmpLeft + 4, tmpRight);
                            tmpLabelStr = getText(tmpLabelStr);
                            if(tmpLabelStr.endsWith(":"))
                                tmpLabelStr = tmpLabelStr
                                    .substring(0, tmpLabelStr.length()-1);
                            //label done!

                            tmpLeft = profilePage.indexOf("<dd>", tmpRight);
                            if(tmpLeft >= 0)
                            {
                                tmpRight
                                    = profilePage.indexOf("</dd>", tmpLeft);
                                if(tmpRight >= 0)
                                {
                                    tmpValueStr = profilePage.substring(
                                                    tmpLeft + 4, tmpRight);
                                    if(tmpValueStr.startsWith("<img src=\"/")
                                       && tmpValueStr.endsWith("\" border=0>"))
                                    {
                                        //<img src="/string_image.php?ct=AAAAAQAQVVaveiWcR6O91PkY7zr1NgAAABesyLV-PSZ6liviNVJWiOP6XeTgkFFyaMM%2C&fp=8.7&state=0&highlight=1386786477" border=0>
                                        tmpValueStr = tmpValueStr.substring(
                                                10, tmpValueStr.length() - 11);
                                        //"http://static.ak.fbcdn.net"
                                        byte[] imageBytes = getImage(
                                                fileHostUrl + tmpValueStr);
                                        if(imageBytes != null
                                           && imageBytes.length > 0)
                                            result.add(new ServerStoredDetails
                                                .ImageDetail(
                                                    tmpLabelStr, imageBytes));
                                    }
                                    else
                                    {
                                        tmpValueStr = getText(tmpValueStr);
                                        //value done!
                                        result.add(new ServerStoredDetails
                                            .StringDetail(
                                                tmpLabelStr, tmpValueStr));
                                    }
                                }
                                else
                                    break;
                            }
                            else
                                break;
                        }
                        else
                            break;
                        tmpLeft = profilePage.indexOf("<dt>", tmpRight + 5);
                    }
                }
                /*//Gender
                String tmpPrefix = "<td class=\"data\"><div id=\'Gender-data"
                    +"\'class=\"datawrap\">";
                String tmpPostfix = "</div>";
                int beginPos = profilePage.indexOf(tmpPrefix);
                int endPos;
                if(beginPos >= 0){
                    endPos = profilePage.indexOf(tmpPostfix, beginPos);
                    if(endPos >= 0){
                        tmp = null;
                        tmp = profilePage.substring(beginPos
                            + tmpPrefix.length(), endPos);
                        if(tmp != null)
                            result.add(new ServerStoredDetails
                                .GenderDetail(getText(tmp)));
                    }
                }

                //birthday
                tmpPrefix = "<td class=\"data\"><div id=\'Birthday-data\'class"
                    +"=\"datawrap\">";
                tmpPostfix = "</a></div>";
                beginPos = profilePage.indexOf(tmpPrefix);
                if(beginPos >= 0){
                    endPos = profilePage.indexOf(tmpPostfix, beginPos);
                    if(endPos >= 0){
                        tmp = null;
                        tmp = profilePage.substring(beginPos
                            + tmpPrefix.length(), endPos);
                        if(tmp != null)
                            //@fixme birthday label
                            result.add(new ServerStoredDetails.StringDetail(
                                "Birthday", getText(tmp)));
                    }
                }

                //Hometown
                tmpPrefix = "<td class=\"data\"><div id=\'Hometown-data\'class"
                    +"=\"datawrap\">";
                tmpPostfix = "</a></div>";
                beginPos = profilePage.indexOf(tmpPrefix);
                if(beginPos >= 0){
                    endPos = profilePage.indexOf(tmpPostfix, beginPos);
                    if(endPos >= 0){
                        tmp = null;
                        tmp = profilePage.substring(beginPos
                            + tmpPrefix.length(), endPos);
                        if(tmp != null)
                            result.add(new ServerStoredDetails
                                .StringDetail("Hometown", getText(tmp)));
                    }
                }*/
            }
            catch (Exception exc)
            {
                logger.error("Cannot load details for contact "
                    + contactAddress + " : " + exc.getMessage()
                    , exc);
            }
        }

        retreivedDetails.put(contactAddress, result);

        return new LinkedList<GenericDetail>(result);
    }

    /**
     * Returns the persistent presence operation set that this contact belongs
     * to.
     *
     * @return the <tt>OperationSetPersistentPresenceFacebookImpl</tt> that
     *         this contact belongs to.
     */
    private OperationSetPersistentPresenceFacebookImpl
        getParentPresenceOperationSet()
    {
        return
            (OperationSetPersistentPresenceFacebookImpl)
                parentProvider
                    .getOperationSet(OperationSetPersistentPresence.class);
    }

    /**
     * Get text from html formatted string
     *
     * @param srcStr the html formatted string
     * @return the plain text
     */
    public static String getText(String srcStr)
    {
        String text = "";

        int left = 0;
        int cur = srcStr.indexOf("<", left);
        while(cur >= 0)
        {
            text += srcStr.substring(left, cur);
            left = srcStr.indexOf(">", cur);
            if(left < 0)
                break;

            left++;
            if(left < srcStr.length())
            {
                cur = srcStr.indexOf("<", left);
                continue;
            }
            else
                break;
        }
        if(left >= 0)
            text += srcStr.substring(left);

        /*
         * support for some special symbols, e.g.:
         * & = &amp;
         * (space) = &nbsp;
         * > = &gt;
         * < = &lt;
         * " = &quot;
         * ' = &#039;
         * © = &copy;
         * ® = &reg;
         */
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&#039;", "'");
        text = text.replaceAll("&copy;", "©");
        text = text.replaceAll("&reg;", "®");

        return text;
    }


    /**
     * Returns the bytes of the image at the specified <tt>urlStr</tt> location
     * or <tt>null</tt> if we fail to retrieve it for some reason.
     *
     * @param urlStr
     *
     * @return a <tt>byte[]</tt> array containing the bytes of the image at the
     * specified <tt>urlStr</tt> location or <tt>null</tt> if we fail to
     * retrieve it for some reason.
     */
    public static byte[] getImage(String urlStr)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        try
        {
            URL url = new URL(urlStr);
            BufferedImage newAvatar = ImageIO.read(url);

            javax.imageio.ImageIO.write(newAvatar, "PNG", byteArrayOS);
        }
        catch (IOException e)
        {
            logger.warn("IOException occured when loading image", e);
            // OK, we use the defaultAvatar temporarily
            return null;
        }
        finally
        {
            try
            {
                byteArrayOS.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return byteArrayOS.toByteArray();
    }
}
