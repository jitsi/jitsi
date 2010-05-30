/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.icq.*;

/**
 * @author Damian Minkov
 */
public class InfoRetreiver
{
    private static final Logger logger =
        Logger.getLogger(InfoRetreiver.class);

    /**
     * A callback to the ICQ provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    // the uin of the account using us,
    // used when sending commands for user info to the server
    private String ownerUin = null;

    // here is kept all the details retrieved so far
    private final Map<String, List<GenericDetail>> retreivedDetails
        = new Hashtable<String, List<GenericDetail>>();

    // used to generate request id when sending commands for retrieving user info
    private static int requestID = 0;

    /**
     * As all the Full User Info comes in
     * sequences of 8 packets according to the
     * requestID we keep the stored Info so far.
     */
    private static final Map<Integer, List<GenericDetail>> retreivedInfo
        = new Hashtable<Integer, List<GenericDetail>>();


    protected InfoRetreiver
        (ProtocolProviderServiceIcqImpl icqProvider, String ownerUin)
    {
        this.icqProvider = icqProvider;
        this.ownerUin = ownerUin;
    }

    /**
     * returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator<GenericDetail> getDetailsAndDescendants(
        String uin,
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(uin);
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        for (GenericDetail item : details)
            if (detailClass.isInstance(item))
                result.add(item);

        return result.iterator();
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator<GenericDetail> getDetails(
        String uin,
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(uin);
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        for (GenericDetail item : details)
        {
            if(detailClass.equals(item.getClass()))
                result.add(item);
        }

        return result.iterator();
    }

    /**
     * request the full info for the given uin
     * waits and return this details
     *
     * @param uin String
     * @return Vector
     */
    protected List<GenericDetail> getContactDetails(String uin)
    {
        List<GenericDetail> result = retreivedDetails.get(uin);

        if(result == null)
        {
            int reqID = requestID++;

            //retrieve the details
            long toICQUin = Long.parseLong(uin);
            MetaFullInfoRequest infoRequest =
                new MetaFullInfoRequest(
                    Long.parseLong(ownerUin),
                    reqID,
                    toICQUin);

            UserInfoResponseRetriever responseRetriever =
                new UserInfoResponseRetriever(reqID);

            icqProvider.getAimConnection().getInfoService().getOscarConnection()
                .sendSnacRequest(infoRequest, responseRetriever);

            responseRetriever.waitForLastInfo(60000);

            result = responseRetriever.result;

            if (result == null)
                result = new LinkedList<GenericDetail>();

            retreivedDetails.put(uin, result);
        }

        return new LinkedList<GenericDetail>(result);
    }

    /**
     * waits for the last snac from the full info response sequence
     */
    private class UserInfoResponseRetriever extends SnacRequestAdapter
    {
        private final int requestID;
        List<GenericDetail> result = null;

        UserInfoResponseRetriever(int requestID)
        {
            this.requestID = requestID;
        }

        public void handleResponse(SnacResponseEvent e)
        {
            SnacCommand snac = e.getSnacCommand();

            if (snac instanceof MetaBasicInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received basic info");
                readBasicUserInfo((MetaBasicInfoCmd)snac);
            }
            else if (snac instanceof MetaMoreInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received meta more info");
                readMoreUserInfo((MetaMoreInfoCmd)snac);
            }
            else if (snac instanceof MetaEmailInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received email info");
                readEmailUserInfo((MetaEmailInfoCmd)snac);
            }
            else if (snac instanceof MetaHomepageCategoryInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received home page info");
                readHomePageUserInfo((MetaHomepageCategoryInfoCmd)snac);
            }
            else if (snac instanceof MetaWorkInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received work info");
                readWorkUserInfo((MetaWorkInfoCmd)snac);
            }
            else if (snac instanceof MetaNotesInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received notes info");
                readUserAboutInfo((MetaNotesInfoCmd)snac);
            }
            else if (snac instanceof MetaInterestsInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received interest info");
                readInterestsUserInfo((MetaInterestsInfoCmd)snac);
            }
            else if (snac instanceof MetaAffiliationsInfoCmd)
            {
                if (logger.isInfoEnabled())
                    logger.info("received affiliations info");
                readAffilationsUserInfo((MetaAffiliationsInfoCmd)snac);

                result =
                    getInfoForRequest(((MetaAffiliationsInfoCmd)snac).getId());
                // this is the last packet
                synchronized(this){this.notifyAll();}
            }
        }

        public void waitForLastInfo(long waitFor)
        {
            synchronized(this)
            {
                try
                {
                    wait(waitFor);
                }
                catch (InterruptedException ex)
                {
                    if (logger.isDebugEnabled())
                        logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }
    }

    /**
     * wait for response of our ShorInfo Requests
     */
    private static class ShortInfoResponseRetriever extends SnacRequestAdapter
    {
        String nickname = null;

        public void handleResponse(SnacResponseEvent e)
        {
            SnacCommand snac = e.getSnacCommand();

            if (snac instanceof MetaShortInfoCmd)
            {
                MetaShortInfoCmd infoSnac = (MetaShortInfoCmd)snac;

                nickname = infoSnac.getNickname();

                synchronized(this){this.notifyAll();}
            }
        }
    }


    /**
     * when detail is changed we remove it from the cache,
     * from retreivedDetails so the next time we want the details
     * we are shure they are get from the server and are actual
     *
     * @param uin String
     */
    protected void detailsChanged(String uin)
    {
        retreivedDetails.remove(uin);
    }

    /**
     * Get the nickname of the specified uin
     * @param uin String the uin
     * @return String the nickname of the uin
     */
    public String getNickName(String uin)
    {
        ShortInfoResponseRetriever responseRetriever =
                new ShortInfoResponseRetriever();

        long longUin = Long.parseLong(uin);
        MetaShortInfoRequest req =
            new MetaShortInfoRequest(Long.parseLong(ownerUin), 2, longUin);

        icqProvider.getAimConnection().getInfoService().getOscarConnection()
            .sendSnacRequest(req, responseRetriever);

        synchronized(responseRetriever)
        {
            try{
                responseRetriever.wait(30000);
            }
            catch (InterruptedException ex)
            {
                //we don't care
            }
        }

        return responseRetriever.nickname;
    }

    /**
     * Metods retrieving data and storing it
     */
    /**
     * Returns the stored info so far on the specified request
     *
     * @param requestID int
     * @return List
     */
    private List<GenericDetail> getInfoForRequest(int requestID)
    {
        List<GenericDetail> res = retreivedInfo.get(requestID);

        if (res == null)
        {
            // this indicates that the info data
            // doesn't exists, so this is the first packet
            // from the sequence (basic info)

            res = new LinkedList<GenericDetail>();
            retreivedInfo.put(requestID, res);
        }

        return res;
    }

    /**
    * Method for parsing incoming data
    * Read data in MetaBasicInfoCmd command
    * @param cmd MetaBasicInfoCmd
    */
    private void readBasicUserInfo(MetaBasicInfoCmd cmd)
    {
        List<GenericDetail> infoData = getInfoForRequest(cmd.getId());

        Locale countryCodeLocale =
            OperationSetServerStoredAccountInfoIcqImpl.getCountry(cmd.getCountryCode());
        if (countryCodeLocale != null)
            infoData.add(new ServerStoredDetails.CountryDetail(
                countryCodeLocale));

        // the following are not used
        // cmd.getGmtOffset()
        // cmd.isAuthorization()
        // cmd.isWebAware()
        // cmd.isPublishPrimaryEmail()

        // everything is read lets store it
        String tmp = null;
        if ((tmp = cmd.getNickname()) != null)
            infoData.add(new ServerStoredDetails.NicknameDetail(tmp));
        if ((tmp = cmd.getFirstName()) != null)
            infoData.add(new ServerStoredDetails.FirstNameDetail(tmp));
        if ((tmp = cmd.getLastName()) != null)
            infoData.add(new ServerStoredDetails.LastNameDetail(tmp));
        if ((tmp = cmd.getEmail()) != null)
            infoData.add(new ServerStoredDetails.EmailAddressDetail(tmp));
        if ((tmp = cmd.getHomeCity()) != null)
            infoData.add(new ServerStoredDetails.CityDetail(tmp));
        if ((tmp = cmd.getHomeState()) != null)
            infoData.add(new ServerStoredDetails.ProvinceDetail(tmp));
        if ((tmp = cmd.getHomePhone()) != null)
            infoData.add(new ServerStoredDetails.PhoneNumberDetail(tmp));
        if ((tmp = cmd.getHomeFax()) != null)
            infoData.add(new ServerStoredDetails.FaxDetail(tmp));
        if ((tmp = cmd.getHomeAddress()) != null)
            infoData.add(new ServerStoredDetails.AddressDetail(tmp));
        if ((tmp = cmd.getCellPhone()) != null)
            infoData.add(new ServerStoredDetails.MobilePhoneDetail(tmp));
        if ((tmp = cmd.getHomeZip()) != null)
            infoData.add(new ServerStoredDetails.PostalCodeDetail(tmp));
    }

    /**
     * Method for parsing incoming data
     * Read data in MoreUserInfo command
     * @param cmd MetaMoreInfoCmd
     */
    private void readMoreUserInfo(MetaMoreInfoCmd cmd)
    {
        List<GenericDetail> infoData = getInfoForRequest(cmd.getId());

        ServerStoredDetails.GenderDetail gender =
            OperationSetServerStoredAccountInfoIcqImpl.genders[cmd.getGender()];
        if(gender != null)
            infoData.add(gender);

        String tmp = null;

        try
        {
            if((tmp = cmd.getHomepage()) != null)
                infoData.add(new ServerStoredDetails.WebPageDetail(new URL(tmp)));
        }
        catch (MalformedURLException ex)
        {}

        Calendar birthDate = Calendar.getInstance();
        birthDate.setTime(cmd.getBirthDate());
        infoData.add(new ServerStoredDetails.BirthDateDetail(birthDate));

        Locale spokenLanguage1 =
            OperationSetServerStoredAccountInfoIcqImpl.
            getSpokenLanguage(cmd.getSpeakingLanguages()[0]);
        if(spokenLanguage1 != null)
            infoData.add(
                new ServerStoredDetails.SpokenLanguageDetail(spokenLanguage1));

        Locale spokenLanguage2 =
            OperationSetServerStoredAccountInfoIcqImpl.
            getSpokenLanguage(cmd.getSpeakingLanguages()[1]);
        if(spokenLanguage2 != null)
            infoData.add(
                new ServerStoredDetails.SpokenLanguageDetail(spokenLanguage2));

        Locale spokenLanguage3 =
            OperationSetServerStoredAccountInfoIcqImpl.
            getSpokenLanguage(cmd.getSpeakingLanguages()[2]);
        if(spokenLanguage3 != null)
            infoData.add(
                new ServerStoredDetails.SpokenLanguageDetail(spokenLanguage3));

        if((tmp = cmd.getOriginalCity()) != null)
            infoData.add(
            new OperationSetServerStoredAccountInfoIcqImpl.OriginCityDetail(tmp));

        if((tmp = cmd.getOriginalState()) != null)
            infoData.add(
            new OperationSetServerStoredAccountInfoIcqImpl.OriginProvinceDetail(tmp));

        Locale originCountryLocale =
            OperationSetServerStoredAccountInfoIcqImpl.getCountry(cmd.getOriginalCountryCode());
        if(originCountryLocale != null)
            infoData.add(
            new OperationSetServerStoredAccountInfoIcqImpl.
                OriginCountryDetail(originCountryLocale));


        int userGMTOffset = cmd.getTimeZone();
        TimeZone userTimeZone = null;
        if(userGMTOffset >= 0)
            userTimeZone = TimeZone.getTimeZone("GMT+" + userGMTOffset);
        else
            userTimeZone = TimeZone.getTimeZone("GMT" + userGMTOffset);

        infoData.add(new ServerStoredDetails.TimeZoneDetail("GMT Offest", userTimeZone));
    }

    /**
     * Method for parsing incoming data
     * Read data in EmailUserInfo command
     * @param cmd MetaEmailInfoCmd
     */
    private void readEmailUserInfo(MetaEmailInfoCmd cmd)
    {
        List<GenericDetail> infoData = getInfoForRequest(cmd.getId());
        String emails[] = cmd.getEmails();

        if (emails == null)
            return;

        for (String email : emails)
        {
            infoData.add(new ServerStoredDetails.EmailAddressDetail(email));
        }
    }

    /**
     * Method for parsing incoming data
     * Read data in HomePageUserInfo command
     * @param cmd MetaHomepageCategoryInfoCmd
     */
    private void readHomePageUserInfo(MetaHomepageCategoryInfoCmd cmd)
    {
        List<GenericDetail> infoData = getInfoForRequest(cmd.getId());
        String tmp = null;

        try
        {
            if ((tmp = cmd.getKeywords()) != null)
                infoData.add(new ServerStoredDetails.WebPageDetail(new URL(tmp)));
        }
        catch (MalformedURLException ex)
        {}
    }

    /**
     * Method for parsing incoming data
     * Read data in WorkUserInfo command
     * @param cmd MetaWorkInfoCmd
     */
    private void readWorkUserInfo(MetaWorkInfoCmd cmd)
    {
        List<GenericDetail> infoData = getInfoForRequest(cmd.getId());

        String tmp = null;
        if ((tmp = cmd.getWorkCity()) != null)
            infoData.add(new ServerStoredDetails.WorkCityDetail(tmp));
        if ((tmp = cmd.getWorkState()) != null)
            infoData.add(new ServerStoredDetails.WorkProvinceDetail(tmp));
        if ((tmp = cmd.getWorkPhone()) != null)
            infoData.add(new ServerStoredDetails.WorkPhoneDetail(tmp));
        if ((tmp = cmd.getWorkFax()) != null)
            infoData.add(
                new OperationSetServerStoredAccountInfoIcqImpl.WorkFaxDetail(tmp));
        if ((tmp = cmd.getWorkAddress()) != null)
            infoData.add(new ServerStoredDetails.WorkAddressDetail(tmp));
        if ((tmp = cmd.getWorkZipCode()) != null)
            infoData.add(new ServerStoredDetails.WorkPostalCodeDetail(tmp));

        Locale workCountry =
            OperationSetServerStoredAccountInfoIcqImpl.getCountry(cmd.getWorkCountryCode());
        if (workCountry != null)
            infoData.add(new ServerStoredDetails.WorkCountryDetail(workCountry));

        if ((tmp = cmd.getWorkCompany()) != null)
            infoData.add(new ServerStoredDetails.WorkOrganizationNameDetail(tmp));
        if ((tmp = cmd.getWorkDepartment()) != null)
            infoData.add(
            new OperationSetServerStoredAccountInfoIcqImpl.WorkDepartmentNameDetail(tmp));
        if ((tmp = cmd.getWorkPosition()) != null)
            infoData.add(
            new OperationSetServerStoredAccountInfoIcqImpl.WorkPositionNameDetail(tmp));

        int workOccupationCode = cmd.getWorkOccupationCode();
        if (workOccupationCode == 99)
            infoData.add(
                new OperationSetServerStoredAccountInfoIcqImpl.
                    WorkOcupationDetail(OperationSetServerStoredAccountInfoIcqImpl.
                        occupations[OperationSetServerStoredAccountInfoIcqImpl.occupations.length - 1]));
        else
            infoData.add(
            new OperationSetServerStoredAccountInfoIcqImpl.
                WorkOcupationDetail(
                    OperationSetServerStoredAccountInfoIcqImpl.occupations[workOccupationCode]));

        try
        {
            if ((tmp = cmd.getWorkWebPage()) != null)
                infoData.add(new ServerStoredDetails.WorkPageDetail(new URL(tmp)));
        }
        catch (MalformedURLException ex)
        {}
    }

    /**
     * Method for parsing incoming data
     * Read data in UserAboutInfo command
     * @param cmd MetaNotesInfoCmd
     */
    private void readUserAboutInfo(MetaNotesInfoCmd cmd)
    {
        List<GenericDetail> infoData = getInfoForRequest(cmd.getId());

        if (cmd.getNotes() != null)
            infoData.add(
                new OperationSetServerStoredAccountInfoIcqImpl.NotesDetail(cmd.getNotes()));
    }

    /**
     * Method for parsing incoming data
     * Read data in InterestsUserInfo command
     * @param cmd MetaInterestsInfoCmd
     */
    private void readInterestsUserInfo(MetaInterestsInfoCmd cmd)
    {
        List<GenericDetail> infoData = getInfoForRequest(cmd.getId());

        int[] categories = cmd.getCategories();
        String[] interests = cmd.getInterests();
        for (int i = 0; i < interests.length; i++)
        {
            int category = categories[i];
            if (category != 0)
            {
                // as the categories are between 100 and 150 we shift them
                // because their string representations are stored in array
                category = category - 99;
            }
            if(category <= interests.length)
            {
                infoData.add(
                    new OperationSetServerStoredAccountInfoIcqImpl.InterestDetail(
                        interests[i],
                        OperationSetServerStoredAccountInfoIcqImpl.interestsCategories[category]));
            }
        }
    }

    /**
     * Not used for now
     * @param cmd MetaAffiliationsInfoCmd
     */
    private void readAffilationsUserInfo(MetaAffiliationsInfoCmd cmd)
    {
//        Vector<GenericDetail> infoData = getInfoForRequest(cmd.getId());
    }
}
