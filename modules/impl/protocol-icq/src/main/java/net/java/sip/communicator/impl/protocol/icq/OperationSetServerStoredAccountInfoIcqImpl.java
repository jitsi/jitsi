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
package net.java.sip.communicator.impl.protocol.icq;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BirthDateDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.CountryDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.SpokenLanguageDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.StringDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joscar.snaccmd.icq.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.oscar.service.icon.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredAccountInfoIcqImpl
    extends AbstractOperationSetServerStoredAccountInfo
{
    private static final Logger logger =
        Logger.getLogger(OperationSetServerStoredAccountInfoIcqImpl.class);

    private InfoRetreiver infoRetreiver = null;
    private String uin = null;

    /**
     * The icq provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

//    public static final IcqType CMD_SET_FULLINFO = new IcqType(0x07D0, 0x0c3a);

    private int reqID = 0;

    // the value is int[]
    // int[0] - the max count of this detail
    // the Tlv type for this detail
    public static final Map<Class<? extends GenericDetail>, int[]> supportedTypes
        = new Hashtable<Class<? extends GenericDetail>, int[]>();
    static {
        supportedTypes.put(ServerStoredDetails.ImageDetail.class,       new int[]{1});
        supportedTypes.put(ServerStoredDetails.CountryDetail.class,     new int[]{1, 0x01A4});
        supportedTypes.put(ServerStoredDetails.NicknameDetail.class,    new int[]{1, 0x0154});
        supportedTypes.put(ServerStoredDetails.FirstNameDetail.class,   new int[]{1, 0x0140});
        supportedTypes.put(ServerStoredDetails.LastNameDetail.class,    new int[]{1, 0x014A});
        supportedTypes.put(ServerStoredDetails.EmailAddressDetail.class, new int[]{5, 0x015E});
        supportedTypes.put(ServerStoredDetails.CityDetail.class,        new int[]{1, 0x0190});
        supportedTypes.put(ServerStoredDetails.ProvinceDetail.class,    new int[]{1, 0x019A});
        supportedTypes.put(ServerStoredDetails.PhoneNumberDetail.class, new int[]{1, 0x0276});
        supportedTypes.put(ServerStoredDetails.FaxDetail.class,         new int[]{1, 0x0280});
        supportedTypes.put(ServerStoredDetails.AddressDetail.class,     new int[]{1, 0x0262});
        supportedTypes.put(ServerStoredDetails.MobilePhoneDetail.class, new int[]{1, 0x028A});
        supportedTypes.put(ServerStoredDetails.PostalCodeDetail.class,  new int[]{1, 0x026C});
        supportedTypes.put(ServerStoredDetails.GenderDetail.class,      new int[]{1, 0x017C});
        supportedTypes.put(ServerStoredDetails.WebPageDetail.class,     new int[]{1, 0x0213});
        supportedTypes.put(ServerStoredDetails.BirthDateDetail.class,   new int[]{1, 0x023A});
        supportedTypes.put(ServerStoredDetails.SpokenLanguageDetail.class, new int[]{3, 0x0186});
        supportedTypes.put(OriginCityDetail.class,          new int[]{1, 0x0320});
        supportedTypes.put(OriginProvinceDetail.class,      new int[]{1, 0x032A});
        supportedTypes.put(OriginCountryDetail.class,       new int[]{1, 0x0334});
        supportedTypes.put(ServerStoredDetails.WorkCityDetail.class,    new int[]{1, 0x029E});
        supportedTypes.put(ServerStoredDetails.WorkProvinceDetail.class, new int[]{1, 0x02A8});
        supportedTypes.put(ServerStoredDetails.WorkPhoneDetail.class,   new int[]{1, 0x02C6});
        supportedTypes.put(WorkFaxDetail.class,             new int[]{1, 0x02D0});
        supportedTypes.put(ServerStoredDetails.WorkAddressDetail.class, new int[]{1, 0x0294});
        supportedTypes.put(ServerStoredDetails.WorkPostalCodeDetail.class, new int[]{1, 0x02BC});
        supportedTypes.put(ServerStoredDetails.WorkCountryDetail.class, new int[]{1, 0x02B2});
        supportedTypes.put(ServerStoredDetails.WorkOrganizationNameDetail.class, new int[]{1, 0x01AE});
        supportedTypes.put(WorkDepartmentNameDetail.class,  new int[]{1, 0x01B8});
        supportedTypes.put(WorkPositionNameDetail.class,    new int[]{1, 0x01C2});
        supportedTypes.put(WorkOcupationDetail.class,       new int[]{1, 0x01CC});
        supportedTypes.put(ServerStoredDetails.WorkPageDetail.class,    new int[]{1, 0x02DA});
        supportedTypes.put(NotesDetail.class,               new int[]{1, 0x0258});
        supportedTypes.put(InterestDetail.class,            new int[]{10, 0x01EA});
        supportedTypes.put(ServerStoredDetails.TimeZoneDetail.class,    new int[]{1, 0x0316});
    }

    /**
     * Our image.
     */
    private ImageDetail accountImage = null;

    /**
     * Listener waiting for our image.
     */
    private IconUpdateListener iconListener = null;

    /**
     * Creates instance of OperationSetServerStoredAccountInfo
     * for icq protocol.
     * @param infoRetreiver the info retreiver
     * @param uin our own account uin
     * @param icqProvider the provider
     */
    public OperationSetServerStoredAccountInfoIcqImpl
        (InfoRetreiver infoRetreiver, String uin,
         ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.infoRetreiver = infoRetreiver;
        this.uin = uin;
        this.icqProvider = icqProvider;
    }

    /**
     * Returns all details currently available and set for our account.
     *
     * @return a java.util.Iterator over all details currently set our
     *   account.
     */
    public Iterator<GenericDetail> getAllAvailableDetails()
    {
        assertConnected();

        List<GenericDetail> ds = infoRetreiver.getContactDetails(uin);
        GenericDetail img = getImage();
        if(img != null)
            ds.add(img);

        return ds.iterator();
    }

    /**
     * Returns an iterator over all details that are instances of exactly the
     * same class as the one specified.
     *
     * @param detailClass one of the detail classes defined in the
     *   ServerStoredDetails class, indicating the kind of details we're
     *   interested in. <p>
     * @return a java.util.Iterator over all details of specified class.
     */
    public Iterator<GenericDetail> getDetails(
        Class<? extends GenericDetail> detailClass)
    {
        assertConnected();

        if(detailClass.equals(ImageDetail.class))
        {
            List<GenericDetail> res = new Vector<GenericDetail>();
            res.add(getImage());
            return res.iterator();
        }

        return infoRetreiver.getDetails(uin, detailClass);
    }

    /**
     * Returns an iterator over all details that are instances or descendants
     * of the specified class.
     *
     * @param detailClass one of the detail classes defined in the
     *   ServerStoredDetails class, indicating the kind of details we're
     *   interested in. <p>
     * @return a java.util.Iterator over all details that are instances or
     *   descendants of the specified class.
     */
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
        Class<T> detailClass)
    {
        assertConnected();

        if(ImageDetail.class.isAssignableFrom(detailClass))
        {
            List<ImageDetail> res = new Vector<ImageDetail>();

            res.add(getImage());

            @SuppressWarnings("unchecked")
            Iterator<T> tIt = (Iterator<T>) res.iterator();

            return tIt;
        }

        return infoRetreiver.getDetailsAndDescendants(uin, detailClass);
    }

    /**
     * The method returns the number of instances supported for a particular
     * detail type.
     *
     * @param detailClass the class whose max instance number we'd like to
     *   find out. <p>
     * @return int the maximum number of detail instances.
     */
    public int getMaxDetailInstances(Class<? extends GenericDetail> detailClass)
    {
        return supportedTypes.get(detailClass)[0];
    }

    /**
     * Returns all detail Class-es that the underlying implementation
     * supports setting.
     *
     * @return a java.util.Iterator over all detail classes supported by the
     *   implementation.
     */
    public Iterator<Class<? extends GenericDetail>> getSupportedDetailTypes()
    {
        return supportedTypes.keySet().iterator();
    }

    /**
     * Determines whether a detail class represents a detail supported by the
     * underlying implementation or not.
     *
     * @param detailClass the class the support for which we'd like to
     *   determine. <p>
     * @return true if the underlying implementation supports setting
     *   details of this type and false otherwise.
     */
    public boolean isDetailClassSupported(
        Class<? extends GenericDetail> detailClass)
    {
        return supportedTypes.get(detailClass) != null;
    }

    /**
     * Determines whether the underlying implementation supports edition
     * of this detail class.
     * <p>
     * @param detailClass the class whose edition we'd like to determine if it's
     * possible
     * @return true if the underlying implementation supports edition of this
     * type of detail and false otherwise.
     */
    public boolean isDetailClassEditable(
        Class<? extends GenericDetail> detailClass)
    {
        return 
            isDetailClassSupported(detailClass)
            && ImageDetail.class.isAssignableFrom(detailClass);
    }

    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (icqProvider == null)
            throw new IllegalStateException(
                "The icq provider must be non-null and signed on the ICQ "
                +"service before being able to communicate.");
        if (!icqProvider.isRegistered())
            throw new IllegalStateException(
                "The icq provider must be signed on the ICQ service before "
                +"being able to communicate.");
    }

    /**
     * Adds the specified detail to the list of details registered on-line
     * for this account. If such a detail already exists its max instance number
     * is consulted and if it allows it - a second instance is added or otherwise
     * and illegal argument exception is thrown. An IllegalArgumentException is
     * also thrown in case the class of the specified detail is not supported by
     * the underlying implementation, i.e. its class name was not returned by the
     * getSupportedDetailTypes() method.
     * <p>
     * @param detail the detail that we'd like registered on the server.
     * <p>
     * @throws IllegalArgumentException if such a detail already exists and its
     * max instances number has been atteined or if the underlying
     * implementation does not support setting details of the corresponding
     * class.
     * @throws OperationFailedException with code Network Failure if putting the
     * new value online has failed
     * @throws java.lang.ArrayIndexOutOfBoundsException if the number of
     * instances currently registered by the application is already equal to the
     * maximum number of supported instances (@see getMaxDetailInstances())
     */
    public void addDetail(GenericDetail detail) throws IllegalArgumentException,
        OperationFailedException, ArrayIndexOutOfBoundsException
    {
        assertConnected();

        if(!isDetailClassSupported(detail.getClass()))
            throw new IllegalArgumentException(
                "implementation does not support such details " +
                detail.getClass());

        List<GenericDetail> alreadySetDetails = new Vector<GenericDetail>();
        Iterator<GenericDetail> iter = getDetails(detail.getClass());
        while (iter.hasNext())
        {
            alreadySetDetails.add(iter.next());
        }

        if(alreadySetDetails.size() >= getMaxDetailInstances(detail.getClass()))
            throw new ArrayIndexOutOfBoundsException(
                "Max count for this detail is already reached");

        if (detail instanceof ImageDetail)
        {
            if (iconListener == null)
            {
                iconListener = new IconUpdateListener();

                this.icqProvider.getAimConnection().getExternalServiceManager().
                        getIconServiceArbiter().addIconRequestListener(
                        new IconUpdateListener());
            }

            icqProvider.getAimConnection().getMyBuddyIconManager().requestSetIcon(
                    ByteBlock.wrap(((ServerStoredDetails.ImageDetail) detail).getBytes()));
            infoRetreiver.detailsChanged(uin);

            fireServerStoredDetailsChangeEvent(icqProvider,
                ServerStoredDetailsChangeEvent.DETAIL_ADDED,
                null,
                detail);

            return;
        }

        // everything is ok , so set it
        alreadySetDetails.add(detail);

        SuccessResponseListener responseListener = new SuccessResponseListener();

        MetaFullInfoSetCmd cmd =
                    new MetaFullInfoSetCmd(Integer.parseInt(uin), reqID++);

        int typeOfDetail =
            supportedTypes.get(detail.getClass())[1];

        try
        {
            switch(typeOfDetail)
            {
                case 0x01A4 : cmd.setCountry(getCountryCode(((CountryDetail)detail).getLocale())); break;
                case 0x0154 : cmd.setNickName(((StringDetail)detail).getString()); break;
                case 0x0140 : cmd.setFirstName(((StringDetail)detail).getString()); break;
                case 0x014A : cmd.setLastName(((StringDetail)detail).getString()); break;
                case 0x015E : cmd.setEmail(((StringDetail)detail).getString(), false); break;
                case 0x0190 : cmd.setHomeCity(((StringDetail)detail).getString()); break;
                case 0x019A : cmd.setHomeState(((StringDetail)detail).getString()); break;
                case 0x0276 : cmd.setHomePhone(((StringDetail)detail).getString()); break;
                case 0x0280 : cmd.setHomeFax(((StringDetail)detail).getString()); break;
                case 0x0262 : cmd.setAddress(((StringDetail)detail).getString()); break;
                case 0x028A : cmd.setCellPhone(((StringDetail)detail).getString()); break;
                case 0x026C : cmd.setHomeZip(((StringDetail)detail).getString()); break;
                case 0x017C :
                    if(detail.equals(ServerStoredDetails.GenderDetail.FEMALE))
                        cmd.setGender(1);
                    else if(detail.equals(ServerStoredDetails.GenderDetail.MALE))
                        cmd.setGender(2);
                    else
                        cmd.setGender(0); break;
                case 0x0213 : cmd.setHomePage(((StringDetail)detail).getString()); break;
                case 0x023A : cmd.setBirthDay(((BirthDateDetail)detail).getCalendar().getTime()); break;
                case 0x0186 :
                    int[] langs = new int[3];
                    Arrays.fill(langs, -1);
                    int count = 0;
                    Iterator<GenericDetail> i
                        = getDetails(SpokenLanguageDetail.class);
                    while (i.hasNext())
                    {
                        GenericDetail item = i.next();
                        langs[count++]
                            = getLanguageCode(
                                ((SpokenLanguageDetail)item).getLocale());
                    }
                    langs[count] = getLanguageCode(((SpokenLanguageDetail)detail).getLocale());
                    cmd.setLanguages(langs[0], langs[1], langs[2]);
                    break;
                case 0x0320 : cmd.setOriginCity(((StringDetail)detail).getString()); break;
                case 0x032A : cmd.setOriginState(((StringDetail)detail).getString()); break;
                case 0x0334 : cmd.setOriginCountry(getCountryCode(((CountryDetail)detail).getLocale())); break;
                case 0x029E : cmd.setWorkCity(((StringDetail)detail).getString()); break;
                case 0x02A8 : cmd.setWorkState(((StringDetail)detail).getString()); break;
                case 0x02C6 : cmd.setWorkPhone(((StringDetail)detail).getString()); break;
                case 0x02D0 : cmd.setWorkFax(((StringDetail)detail).getString()); break;
                case 0x0294 : cmd.setWorkAddress(((StringDetail)detail).getString()); break;
                case 0x02BC : cmd.setWorkZip(((StringDetail)detail).getString()); break;
                case 0x02B2 : cmd.setWorkCountry(getCountryCode(((CountryDetail)detail).getLocale())); break;
                case 0x01AE : cmd.setWorkCompany(((StringDetail)detail).getString()); break;
                case 0x01B8 : cmd.setWorkDepartment(((StringDetail)detail).getString()); break;
                case 0x01C2 : cmd.setWorkPosition(((StringDetail)detail).getString()); break;
                case 0x01CC :
                    cmd.setWorkOccupationCode(getOccupationCode(((StringDetail)detail).getString())); break;
                case 0x02DA : cmd.setWorkWebPage(((StringDetail)detail).getString()); break;
                case 0x0258 : cmd.setNotes(((StringDetail)detail).getString()); break;
                case 0x01EA :
                    List<InterestDetail> interests = new ArrayList<InterestDetail>();
                    Iterator<GenericDetail> intIter
                        = getDetails(InterestDetail.class);
                    while (intIter.hasNext())
                    {
                        InterestDetail item = (InterestDetail) intIter.next();
                        interests.add(item);
                    }
                    setInterests(cmd, interests);
                    break;
                case 0x0316 :
                    int offset = ((ServerStoredDetails.TimeZoneDetail)detail).
                        getTimeZone().getRawOffset()/(60*60*1000);
                    cmd.setTimeZone(offset);
                    break;
            }
        }
        catch(IOException ex)
        {
            throw new OperationFailedException("Cannot add Detail!",
                OperationFailedException.NETWORK_FAILURE);
        }
        icqProvider.getAimConnection().getInfoService().getOscarConnection().
            sendSnacRequest(cmd, responseListener);

        responseListener.waitForEvent(5000);

        if(!responseListener.success)
            if(responseListener.timeout)
                throw new OperationFailedException("Adding Detail Failed!",
                            OperationFailedException.NETWORK_FAILURE);
            else
                throw new OperationFailedException("Adding Detail Failed!",
                            OperationFailedException.GENERAL_ERROR);

        infoRetreiver.detailsChanged(uin);

        fireServerStoredDetailsChangeEvent(icqProvider,
                ServerStoredDetailsChangeEvent.DETAIL_ADDED,
                null,
                detail);
    }

    private void setInterests(MetaFullInfoSetCmd cmd, List<InterestDetail> interests)
        throws IOException
    {
        int interestCount = interests.size();
        int[] interestsCategories = new int[interestCount];
        String[] interestsStr = new String[interestCount];
        for (int k = 0; k < interestCount; k++)
        {
            interestsStr[k] = interests.get(k).getInterest();
            interestsCategories[k] = getInterestCode(interestsStr[k]);
        }
        cmd.setInterests(interestsCategories, interestsStr);
    }

    /**
     * Removes the specified detail from the list of details stored online
     * for this account.
     *
     * @param detail the detail to remove
     * @return true if the specified detail existed and was successfully
     *   removed and false otherwise.
     * @throws OperationFailedException with code Network Failure if
     *   removing the detail from the server has failed
     */
    public boolean removeDetail(GenericDetail detail) throws
        OperationFailedException
    {
        assertConnected();

        // as there is no remove method for the details we will
        // set it with empty or default value

        boolean isFound = false;
        // as there is items like language, which must be changed all the values
        // we write not only the changed one but and the other found
        List<GenericDetail> foundValues = new ArrayList<GenericDetail>();
        Iterator<?> iter = infoRetreiver.getDetails(uin, detail.getClass());
        while (iter.hasNext())
        {
            GenericDetail item = (GenericDetail) iter.next();
            if(item.equals(detail))
            {
                isFound = true;
                foundValues.add(detail);
            }
            else
                foundValues.add(item);
        }
        // current detail value does not exist
        if(!isFound)
            return false;

        SuccessResponseListener responseListener = new SuccessResponseListener();

        MetaFullInfoSetCmd cmd =
                           new MetaFullInfoSetCmd(Integer.parseInt(uin), reqID++);

       int typeOfDetail =
           supportedTypes.get(detail.getClass())[1];

       try
       {
           switch(typeOfDetail)
           {
               case 0x01A4 : cmd.setCountry(-1); break;
               case 0x0154 : cmd.setNickName(null); break;
               case 0x0140 : cmd.setFirstName(null); break;
               case 0x014A : cmd.setLastName(null); break;
               case 0x015E : cmd.setEmail(null, false); break;
               case 0x0190 : cmd.setHomeCity(null); break;
               case 0x019A : cmd.setHomeState(null); break;
               case 0x0276 : cmd.setHomePhone(null); break;
               case 0x0280 : cmd.setHomeFax(null); break;
               case 0x0262 : cmd.setAddress(null); break;
               case 0x028A : cmd.setCellPhone(null); break;
               case 0x026C : cmd.setHomeZip(null); break;
               case 0x017C : cmd.setGender(0);break;
               case 0x0213 : cmd.setHomePage(null); break;
               case 0x023A : cmd.setBirthDay(null); break;
               case 0x0186 :
                   int[] langs = new int[3];
                   Arrays.fill(langs, -1);
                   cmd.setLanguages(langs[0], langs[1], langs[2]);
                   break;
               case 0x0320 : cmd.setOriginCity(null); break;
               case 0x032A : cmd.setOriginState(null); break;
               case 0x0334 : cmd.setOriginCountry(-1); break;
               case 0x029E : cmd.setWorkCity(null); break;
               case 0x02A8 : cmd.setWorkState(null); break;
               case 0x02C6 : cmd.setWorkPhone(null); break;
               case 0x02D0 : cmd.setWorkFax(null); break;
               case 0x0294 : cmd.setWorkAddress(null); break;
               case 0x02BC : cmd.setWorkZip(null); break;
               case 0x02B2 : cmd.setWorkCountry(-1); break;
               case 0x01AE : cmd.setWorkCompany(null); break;
               case 0x01B8 : cmd.setWorkDepartment(null); break;
               case 0x01C2 : cmd.setWorkPosition(null); break;
               case 0x01CC : cmd.setWorkOccupationCode(0);break;
               case 0x02DA : cmd.setWorkWebPage(null); break;
               case 0x0258 : cmd.setNotes(null); break;
               case 0x01EA : cmd.setInterests(new int[]{0}, new String[]{""}); break;
               case 0x0316 : cmd.setTimeZone(0); break;
           }
       }
       catch (IOException ex)
       {
           throw new OperationFailedException("Cannot add Detail!",
                                              OperationFailedException.NETWORK_FAILURE);
       }

       icqProvider.getAimConnection().getInfoService().getOscarConnection().
           sendSnacRequest(cmd, responseListener);

       responseListener.waitForEvent(5000);

       if (!responseListener.success && responseListener.timeout)
           throw new OperationFailedException("Replacing Detail Failed!",
                                              OperationFailedException.
                                              NETWORK_FAILURE);

       if (responseListener.success)
       {
           infoRetreiver.detailsChanged(uin);

           fireServerStoredDetailsChangeEvent(icqProvider,
                ServerStoredDetailsChangeEvent.DETAIL_REMOVED,
                detail,
                null);

           return true;
       }
       else
           return false;
    }

    /**
     * Replaces the currentDetailValue detail with newDetailValue and returns
     * true if the operation was a success or false if currentDetailValue did
     * not previously exist (in this case an additional call to addDetail is
     * required).
     *
     * @param currentDetailValue the detail value we'd like to replace.
     * @param newDetailValue the value of the detail that we'd like to
     *   replace currentDetailValue with.
     * @throws ClassCastException if newDetailValue is not an instance of
     *   the same class as currentDetailValue.
     * @throws OperationFailedException with code Network Failure if putting
     *   the new value back online has failed
     * @return boolean
     */
    public boolean replaceDetail(GenericDetail currentDetailValue,
                                 GenericDetail newDetailValue) throws
        ClassCastException, OperationFailedException
    {
        assertConnected();

        if(!newDetailValue.getClass().equals(currentDetailValue.getClass()))
            throw new ClassCastException("New value to be replaced is not as the current one");

        // if values are the same no change
        if(currentDetailValue.equals(newDetailValue))
            return true;

        boolean isFound = false;
        List<GenericDetail> alreadySetDetails = new Vector<GenericDetail>();
        Iterator<GenericDetail> iter
            = infoRetreiver.getDetails(uin, currentDetailValue.getClass());
        while (iter.hasNext())
        {
            GenericDetail item = iter.next();
            if(item.equals(currentDetailValue))
            {
                isFound = true;
                // add the details to the list. We will save the list on one pass
                // most of the multiple details require saving at one time, like Spoken Language
                // we are placing it at the right place. replacing the old one
                alreadySetDetails.add(newDetailValue);
            }
            else
                alreadySetDetails.add(item);
        }
        // current detail value does not exist
        if(!isFound)
            return false;

        //replacing in case of image
        if (newDetailValue instanceof ImageDetail)
        {
            if (iconListener == null)
            {
                iconListener = new IconUpdateListener();

                this.icqProvider.getAimConnection().getExternalServiceManager().
                        getIconServiceArbiter().addIconRequestListener(
                        new IconUpdateListener());
            }
            icqProvider.getAimConnection().getMyBuddyIconManager()
                .requestSetIcon(ByteBlock.wrap(
                    ((ServerStoredDetails.ImageDetail) newDetailValue)
                            .getBytes()));

            infoRetreiver.detailsChanged(uin);

            fireServerStoredDetailsChangeEvent(icqProvider,
                        ServerStoredDetailsChangeEvent.DETAIL_REPLACED,
                        currentDetailValue,
                        newDetailValue);

            return true;
        }

        SuccessResponseListener responseListener = new SuccessResponseListener();

//        // if toBeCleared == null. make it empty one
//        if(toBeCleared == null)
//            toBeCleared = new ArrayList();
//
//        // fix the spoken languages must be 3
//        int lastSpokenIndex = -1;
//        int countOfLanguages = 0;
//        boolean isLanguageFound = false;
//        for(int i = 0; i < changedData.size(); i++)
//        {
//            Object item = changedData.get(i);
//            if(item instanceof ServerStoredDetails.SpokenLanguageDetail)
//            {
//                isLanguageFound = true;
//                lastSpokenIndex = i;
//                countOfLanguages++;
//            }
//        }
//
//        if(isLanguageFound)
//        {
//            for (int i = countOfLanguages; i < 3; i++)
//            {
//                lastSpokenIndex++;
//                changedData.add(lastSpokenIndex,
//                                new ServerStoredDetails.SpokenLanguageDetail(null));
//            }
//        }
//
//        Iterator iter = changedData.iterator();
//        while(iter.hasNext())
//        {
//            ServerStoredDetails.GenericDetail item =
//                (ServerStoredDetails.GenericDetail)iter.next();
//
//            if(toBeCleared.contains(item))
//                changeDataTlvs.add(getClearTlv(item));
//            else
//                changeDataTlvs.add(getTlvForChange(item));
//        }



        MetaFullInfoSetCmd cmd =
            new MetaFullInfoSetCmd(Integer.parseInt(uin), reqID++);

        int typeOfDetail =
            supportedTypes.get(newDetailValue.getClass())[1];

        try
        {
            switch(typeOfDetail)
            {
                case 0x01A4 : cmd.setCountry(getCountryCode(((CountryDetail)newDetailValue).getLocale())); break;
                case 0x0154 : cmd.setNickName(((StringDetail)newDetailValue).getString()); break;
                case 0x0140 : cmd.setFirstName(((StringDetail)newDetailValue).getString()); break;
                case 0x014A :
                    cmd.setLastName(((StringDetail)newDetailValue).getString()); break;
                case 0x015E : cmd.setEmail(((StringDetail)newDetailValue).getString(), false); break;
                case 0x0190 : cmd.setHomeCity(((StringDetail)newDetailValue).getString()); break;
                case 0x019A : cmd.setHomeState(((StringDetail)newDetailValue).getString()); break;
                case 0x0276 : cmd.setHomePhone(((StringDetail)newDetailValue).getString()); break;
                case 0x0280 : cmd.setHomeFax(((StringDetail)newDetailValue).getString()); break;
                case 0x0262 : cmd.setAddress(((StringDetail)newDetailValue).getString()); break;
                case 0x028A : cmd.setCellPhone(((StringDetail)newDetailValue).getString()); break;
                case 0x026C : cmd.setHomeZip(((StringDetail)newDetailValue).getString()); break;
                case 0x017C :
                    if(newDetailValue.equals(ServerStoredDetails.GenderDetail.FEMALE))
                        cmd.setGender(1);
                    else if(newDetailValue.equals(ServerStoredDetails.GenderDetail.MALE))
                        cmd.setGender(2);
                    else
                        cmd.setGender(0); break;
                case 0x0213 : cmd.setHomePage(((StringDetail)newDetailValue).getString()); break;
                case 0x023A : cmd.setBirthDay(((BirthDateDetail)newDetailValue).getCalendar().getTime()); break;
                case 0x0186 :
                    int[] langs = new int[3];
                    Arrays.fill(langs, -1);
                    int count = 0;
                    Iterator<GenericDetail> i = getDetails(SpokenLanguageDetail.class);
                    while (i.hasNext())
                    {
                        GenericDetail item = i.next();
                        if(item.equals(currentDetailValue))
                            langs[count++] = getLanguageCode(((SpokenLanguageDetail)newDetailValue).getLocale());
                        else
                            langs[count++] = getLanguageCode(((SpokenLanguageDetail)item).getLocale());
                    }
                    cmd.setLanguages(langs[0], langs[1], langs[2]);
                    break;
                case 0x0320 : cmd.setOriginCity(((StringDetail)newDetailValue).getString()); break;
                case 0x032A : cmd.setOriginState(((StringDetail)newDetailValue).getString()); break;
                case 0x0334 : cmd.setOriginCountry(getCountryCode(((CountryDetail)newDetailValue).getLocale())); break;
                case 0x029E : cmd.setWorkCity(((StringDetail)newDetailValue).getString()); break;
                case 0x02A8 : cmd.setWorkState(((StringDetail)newDetailValue).getString()); break;
                case 0x02C6 : cmd.setWorkPhone(((StringDetail)newDetailValue).getString()); break;
                case 0x02D0 : cmd.setWorkFax(((StringDetail)newDetailValue).getString()); break;
                case 0x0294 : cmd.setWorkAddress(((StringDetail)newDetailValue).getString()); break;
                case 0x02BC : cmd.setWorkZip(((StringDetail)newDetailValue).getString()); break;
                case 0x02B2 : cmd.setWorkCountry(getCountryCode(((CountryDetail)newDetailValue).getLocale())); break;
                case 0x01AE : cmd.setWorkCompany(((StringDetail)newDetailValue).getString()); break;
                case 0x01B8 : cmd.setWorkDepartment(((StringDetail)newDetailValue).getString()); break;
                case 0x01C2 : cmd.setWorkPosition(((StringDetail)newDetailValue).getString()); break;
                case 0x01CC :
                    cmd.setWorkOccupationCode(getOccupationCode(((StringDetail)newDetailValue).getString())); break;
                case 0x02DA : cmd.setWorkWebPage(((StringDetail)newDetailValue).getString()); break;
                case 0x0258 : cmd.setNotes(((StringDetail)newDetailValue).getString()); break;
                case 0x01EA :
                    List<InterestDetail> interests
                        = new ArrayList<InterestDetail>();
                    Iterator<GenericDetail> intIter
                        = getDetails(InterestDetail.class);
                    while (intIter.hasNext())
                    {
                        InterestDetail item = (InterestDetail) intIter.next();
                        if(item.equals(currentDetailValue))
                            interests.add((InterestDetail) newDetailValue);
                        else
                            interests.add(item);
                    }
                    setInterests(cmd, interests);
                    break;
                case 0x0316 :
                    int offset = ((ServerStoredDetails.TimeZoneDetail)newDetailValue).
                        getTimeZone().getRawOffset()/(60*60*1000);
                    cmd.setTimeZone(offset);
                    break;
            }
        }
        catch (IOException ex)
        {
            throw new OperationFailedException("Cannot add Detail!",
                                               OperationFailedException.NETWORK_FAILURE);
        }
        icqProvider.getAimConnection().getInfoService().getOscarConnection().
            sendSnacRequest(
               cmd, responseListener);

        responseListener.waitForEvent(5000);

        if(!responseListener.success && responseListener.timeout)
            throw new OperationFailedException("Replacing Detail Failed!",
                            OperationFailedException.NETWORK_FAILURE);

        if(responseListener.success)
        {
            infoRetreiver.detailsChanged(uin);

            fireServerStoredDetailsChangeEvent(icqProvider,
                        ServerStoredDetailsChangeEvent.DETAIL_REPLACED,
                        currentDetailValue,
                        newDetailValue);

            return true;
        }
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.OperationSetServerStoredAccountInfo#save()
     * This method is currently unimplemented.
     * The idea behind this method is for users to call it only once, meaning 
     * that all ServerStoredDetails previously modified by addDetail/removeDetail
     * and/or replaceDetail will be saved online on the server in one step.
     * Currently, addDetail/removeDetail/replaceDetail methods are doing the
     * actual saving but in the future the saving part must be carried here. 
     */
    public void save() throws OperationFailedException {}

    /**
     * Requests the account image if its missing.
     * @return the new image or the one that has been already downloaded.
     */
    private ImageDetail getImage()
    {
        if(accountImage != null)
            return accountImage;

        if(iconListener == null)
        {
            iconListener = new IconUpdateListener();

            this.icqProvider.getAimConnection().getExternalServiceManager().
            getIconServiceArbiter().addIconRequestListener(
                new IconUpdateListener());
        }

        ExtraInfoData infoData =
            new ExtraInfoData(
                ExtraInfoData.FLAG_HASH_PRESENT,
                ByteBlock.EMPTY_BLOCK);
        icqProvider.getAimConnection().getExternalServiceManager().
            getIconServiceArbiter().requestIcon(new Screenname(uin), infoData);

        // now wait image to come
        iconListener.waitForImage(22000);

        return accountImage;
    }

    /**
     * Waiting for Acknowledge package and success byte.
     * To set that the operation was successful
     */
    private static class SuccessResponseListener
        implements SnacRequestListener
    {
        public Object waitingForResponseLock = new Object();

        private boolean ran = false;
        boolean success = false;

        private boolean timeout = false;

        public void handleSent(SnacRequestSentEvent evt)
        {}

        public void handleTimeout(SnacRequestTimeoutEvent event)
        {
            if (logger.isTraceEnabled())
                logger.trace("Timeout!");

            synchronized(waitingForResponseLock)
            {
                if (ran)
                    return;

                ran = true;
                timeout = true;
                waitingForResponseLock.notifyAll();
            }
        }

        public void handleResponse(SnacResponseEvent evt)
        {
            synchronized(waitingForResponseLock)
            {
                if (ran)
                    return;
                ran = true;
                if (evt.getSnacCommand() instanceof MetaFullInfoAckCmd)
                {
                    MetaFullInfoAckCmd cmd = (MetaFullInfoAckCmd) evt.getSnacCommand();
                    if (cmd.isCommandSuccesful())
                        success = true;
                }
                waitingForResponseLock.notifyAll();
            }
        }

        public void waitForEvent(int milliseconds)
        {
            synchronized (waitingForResponseLock){
                if(ran)
                    return;

                try
                {
                    waitingForResponseLock.wait(milliseconds);
                }
                catch (InterruptedException exc)
                {
                    logger.error("Interrupted while waiting for response."
                                 , exc);
                }
            }
        }
    }

    /**
     * Our format data
     */
    /**
     * Returns the Locale corresponding the index coming from icq server
     *
     * @param code int
     * @return Locale
     */
    static Locale getCountry(int code)
    {
        if (code == 0 || code == 9999) // not specified or other
            return null;

        String cryStr = countryIndexToLocaleString.get(code);

        return (cryStr == null) ? null : new Locale("", cryStr);
    }

    /**
     * Returns the index stored on the server corresponding the given locale
     * @param cLocale Locale
     * @return int
     */
    static int getCountryCode(Locale cLocale)
    {
        if (cLocale == null)
            return 0; // not specified

        for (Map.Entry<Integer, String> entry : countryIndexToLocaleString.entrySet())
        {
            Integer key = entry.getKey();
            String countryString = entry.getValue().toUpperCase();

            if (countryString.equals(cLocale.getCountry()))
                return key.intValue();
        }

        return 0; // not specified
    }

    /**
     * Returns the Locale corresponding the index coming from icq server
     * @param code int
     * @return Locale
     */
    static Locale getSpokenLanguage(int code)
    {
        if (code == 0 || code == 255) // not specified or other
            return null;

        return spokenLanguages[code];
    }
    /**
     * Returns the index stored on the server corresponding the given locale
     * @param locale Locale
     * @return int
     */
    static int getLanguageCode(Locale locale)
    {
        for (int i = 1; i < spokenLanguages.length; i++)
        {
            if (spokenLanguages[i].equals(locale))
                return i;
        }
        return -1;
    }

    /**
     * @param occupationStr String
     * @return int
     */
    static int getOccupationCode(String occupationStr)
    {
        for(int i = 0; i < occupations.length; i++)
        {
            if(occupations[i].equals(occupationStr))
                return i;
        }
        return 0;
    }

    /**
     * @param value String
     * @return int
     */
    static int getInterestCode(String value)
    {
        for (int i = 0; i < occupations.length; i++)
        {
            if (occupations[i].equals(value))
                return i;
        }
        return 0;
    }


    /**
     * Origin City of user
     */
    public static class OriginCityDetail
        extends ServerStoredDetails.CityDetail
    {
        public OriginCityDetail(String cityName)
        {
            super(cityName);
        }
    }

    /**
     * Origin Province of User
     */
    public static class OriginProvinceDetail
        extends ServerStoredDetails.ProvinceDetail
    {
        public OriginProvinceDetail(String workProvince)
        {
            super(workProvince);
        }
    }

    /**
     * Origin Postal Code of user
     */
    public static class OriginPostalCodeDetail
        extends ServerStoredDetails.PostalCodeDetail
    {
        public OriginPostalCodeDetail(String postalCode)
        {
            super(postalCode);
        }
    }

    /**
     * Fax at work
     */
    public static class WorkFaxDetail
        extends ServerStoredDetails.PhoneNumberDetail
    {
        public WorkFaxDetail(String number)
        {
            super(number);
            super.detailDisplayName = "WorkFax";
        }
    }

    /**
     * Work department
     */
    public static class WorkDepartmentNameDetail
        extends ServerStoredDetails.NameDetail
    {
        public WorkDepartmentNameDetail(String workDepartmentName)
        {
            super("Work Department Name", workDepartmentName);
        }
    }

    /**
     * User position name at work
     */
    public static class WorkPositionNameDetail
        extends ServerStoredDetails.StringDetail
    {
        public WorkPositionNameDetail(String workPos)
        {
            super("Work Position", workPos);
        }
    }

    /**
     * User ocupation at work
     */
    public static class WorkOcupationDetail
        extends ServerStoredDetails.StringDetail
    {
        public WorkOcupationDetail(String value)
        {
            super("Work Ocupation", value);
        }
    }

    /**
     * User notes
     */
    public static class NotesDetail
        extends ServerStoredDetails.StringDetail
    {
        public NotesDetail(String value)
        {
            super("Notes", value);
        }
    }

    /**
     * User interests
     */
    public static class InterestDetail
        extends ServerStoredDetails.InterestDetail
    {
        private String category = null;

        public InterestDetail(String value, String category)
        {
            super(value);
            this.category = category;
        }

        public String getCategory()
        {
            return category;
        }
    }

    /**
     * Origin country Code of user
     */
    public static class OriginCountryDetail
        extends ServerStoredDetails.CountryDetail
    {
        public OriginCountryDetail(Locale locale)
        {
            super(locale);
        }
    }

// String corresponding to type indexes
    static ServerStoredDetails.GenderDetail[] genders =
        new ServerStoredDetails.GenderDetail[]
        {
        null,
        ServerStoredDetails.GenderDetail.FEMALE,
        ServerStoredDetails.GenderDetail.MALE
    };

// this can be more simple
// just to init the strings in the sppec indexes
    private static Locale spokenLanguages[] =
        new Locale[]
        {
        null, // not specified
        new Locale("ar"), // Arabic
        new Locale("bh"), // LC_BHOJPURI  Bhojpuri
        new Locale("bg"), // LC_BULGARIAN Bulgarian
        new Locale("my"), // LC_BURMESE   Burmese
        new Locale("zh", "hk"), // LC_CONTONESE Cantonese official in Hong Kong SAR and Macau SAR
        new Locale("ca"), // LC_CATALAN   Catalan
        Locale.CHINA, // LC_CHINESE Chinese zh
        new Locale("hr"), // LC_CROATIAN    Croatian
        new Locale("cs"), // LC_CZECH   Czech
        new Locale("da"), // LC_DANISH  Danish
        new Locale("nl"), // LC_DUTCH   Dutch
        new Locale("en"), // LC_ENGLISH English
        new Locale("eo"), // LC_ESPERANTO   Esperanto
        new Locale("et"), // LC_ESTONIAN    Estonian
        new Locale("fa"), // LC_FARSI Farsi
        new Locale("fi"), // LC_FINNISH   Finnish
        new Locale("fr"), // LC_FRENCH    French
        new Locale("gd"), // LC_GAELIC    Gaelic
        new Locale("de"), // LC_GERMAN    German
        new Locale("el"), // LC_GREEK Greek
        new Locale("he"), // LC_HEBREW    Hebrew
        new Locale("hi"), // LC_HINDI Hindi
        new Locale("hu"), // LC_HUNGARIAN Hungarian
        new Locale("is"), // LC_ICELANDIC Icelandic
        new Locale("id"), // LC_INDONESIAN    Indonesian
        new Locale("it"), // LC_ITALIAN   Italian
        new Locale("ja"), // LC_JAPANESE  Japanese
        new Locale("km"), // LC_KHMER Khmer
        new Locale("ko"), // LC_KOREAN    Korean
        new Locale("lo"), // LC_LAO   Lao
        new Locale("lv"), // LC_LATVIAN   Latvian
        new Locale("lt"), // LC_LITHUANIAN    Lithuanian
        new Locale("ms"), // LC_MALAY Malay
        new Locale("no"), // LC_NORWEGIAN Norwegian
        new Locale("pl"), // LC_POLISH    Polish
        new Locale("pt"), // LC_PORTUGUESE    Portuguese
        new Locale("ro"), // LC_ROMANIAN  Romanian
        new Locale("ru"), // LC_RUSSIAN   Russian
        new Locale("sr"), // LC_SERBIAN   Serbian
        new Locale("sk"), // LC_SLOVAK    Slovak
        new Locale("sl"), // LC_SLOVENIAN Slovenian
        new Locale("so"), // LC_SOMALI    Somali
        new Locale("es"), // LC_SPANISH   Spanish
        new Locale("sw"), // LC_SWAHILI   Swahili
        new Locale("sv"), // LC_SWEDISH   Swedish
        new Locale("tl"), // LC_TAGALOG   Tagalog
        new Locale("tt"), // LC_TATAR Tatar
        new Locale("th"), // LC_THAI  Thau
        new Locale("tr"), // LC_TURKISH   Turkish
        new Locale("uk"), // LC_UKRAINIAN Ukarinian
        new Locale("ur"), // LC_URDU  Urdu
        new Locale("vi"), // LC_VIETNAMESE    Vietnamese
        new Locale("yi"), // LC_YIDDISH   Yiddish
        new Locale("yo"), // LC_YORUBA    Yoruba
        new Locale("af"), // LC_AFRIKAANS Afriaans
        new Locale("bs"), // LC_BOSNIAN   Bosnian
        new Locale("fa"), // LC_PERSIAN   Persian
        new Locale("sq"), // LC_ALBANIAN  Albanian
        new Locale("hy"), // LC_ARMENIAN  Armenian
        new Locale("pa"), // LC_PUNJABI   Punjabi
        new Locale("ch"), // LC_CHAMORRO  Chamorro
        new Locale("mn"), // LC_MONGOLIAN Mongolian
        new Locale("zh"), // LC_MANDARIN  Mandarin ???
        Locale.TAIWAN, // LC_TAIWANESE Taiwanese ??? zh
        new Locale("mk"), // LC_MACEDONIAN    Macedonian
        new Locale("sd"), // LC_SINDHI    Sindhi
        new Locale("cy"), // LC_WELSH Welsh
        new Locale("az"), // LC_AZERBAIJANI   Azerbaijani
        new Locale("ku"), // LC_KURDISH   Kurdish
        new Locale("gu"), // LC_GUJARATI  Gujarati
        new Locale("ta"), // LC_TAMIL Tamil
        new Locale("be"), // LC_BELORUSSIAN   Belorussian
        null // LC_OTHER     255     other
    };

    static String[] occupations = new String[]
        {
        "not specified",
        "academic",
        "administrative",
        "art/entertainment",
        "college student",
        "computers",
        "community & social",
        "education",
        "engineering",
        "financial services",
        "government",
        "high school student",
        "home",
        "ICQ - providing help",
        "law",
        "managerial",
        "manufacturing",
        "medical/health",
        "military",
        "non-government organization",
        "professional",
        "retail",
        "retired",
        "science & research",
        "sports",
        "technical",
        "university student",
        "web building",
        "other services"
    };

    static String[] interestsCategories = new String[]
        {
        "not specified",
        "art",
        "cars",
        "celebrity fans",
        "collections",
        "computers",
        "culture",
        "fitness",
        "games",
        "hobbies",
        "ICQ - help",
        "internet",
        "lifestyle",
        "movies",
        "music",
        "outdoors",
        "parenting",
        "pets and animals",
        "religion",
        "science",
        "skills",
        "sports",
        "web design",
        "ecology",
        "news and media",
        "government",
        "business",
        "mystics",
        "travel",
        "astronomy",
        "space",
        "clothing",
        "parties",
        "women",
        "social science",
        "60's",
        "70's",
        "40's",
        "50's",
        "finance and corporate",
        "entertainment",
        "consumer electronics",
        "retail stores",
        "health and beauty",
        "media",
        "household products",
        "mail order catalogue",
        "business services",
        "audio and visual",
        "sporting and athletic",
        "publishing",
        "home automation"
    };

// Hashtable holding the country index
// corresponding to the country locale string
    private static final Map<Integer, String> countryIndexToLocaleString
        = new Hashtable<Integer, String>();
    static
    {
//        countryIndexToLocaleString.put((0),""); //not specified
        countryIndexToLocaleString.put((1), "us"); //USA
        countryIndexToLocaleString.put((101), "ai"); //Anguilla
        countryIndexToLocaleString.put((102), "ag"); //Antigua
        countryIndexToLocaleString.put((1021), "ag"); //Antigua & Barbuda
        countryIndexToLocaleString.put((103), "bs"); //Bahamas
        countryIndexToLocaleString.put((104), "bb"); //Barbados
        countryIndexToLocaleString.put((105), "bm"); //Bermuda
        countryIndexToLocaleString.put((106), "vg"); //British Virgin Islands
        countryIndexToLocaleString.put((107), "ca"); //Canada
        countryIndexToLocaleString.put((108), "ky"); //Cayman Islands
        countryIndexToLocaleString.put((109), "dm"); //Dominica
        countryIndexToLocaleString.put((110), "do"); //Dominican Republic
        countryIndexToLocaleString.put((111), "gd"); //Grenada
        countryIndexToLocaleString.put((112), "jm"); //Jamaica
        countryIndexToLocaleString.put((113), "ms"); //Montserrat
        countryIndexToLocaleString.put((114), "kn"); //Nevis
        countryIndexToLocaleString.put((1141), "kn"); //Saint Kitts and Nevis
        countryIndexToLocaleString.put((115), "kn"); //St. Kitts
        countryIndexToLocaleString.put((116), "vc"); //St. Vincent & the Grenadines
        countryIndexToLocaleString.put((117), "tt"); //Trinidad & Tobago
        countryIndexToLocaleString.put((118), "tc"); //Turks & Caicos Islands
        countryIndexToLocaleString.put((120), "ag"); //Barbuda
        countryIndexToLocaleString.put((121), "pr"); //Puerto Rico
        countryIndexToLocaleString.put((122), "lc"); //Saint Lucia
        countryIndexToLocaleString.put((123), "vi"); //Virgin Islands (USA)
        countryIndexToLocaleString.put((178), "es"); //Canary Islands ???
        countryIndexToLocaleString.put((20), "eg"); //Egypt
        countryIndexToLocaleString.put((212), "ma"); //Morocco
        countryIndexToLocaleString.put((213), "dz"); //Algeria
        countryIndexToLocaleString.put((216), "tn"); //Tunisia
        countryIndexToLocaleString.put((218), "ly"); //Libyan Arab Jamahiriya
        countryIndexToLocaleString.put((220), "gm"); //Gambia
        countryIndexToLocaleString.put((221), "sn"); //Senegal
        countryIndexToLocaleString.put((222), "mr"); //Mauritania
        countryIndexToLocaleString.put((223), "ml"); //Mali
        countryIndexToLocaleString.put((224), "pg"); //Guinea
        countryIndexToLocaleString.put((225), "ci"); //Cote d'Ivoire
        countryIndexToLocaleString.put((226), "bf"); //Burkina Faso
        countryIndexToLocaleString.put((227), "ne"); //Niger
        countryIndexToLocaleString.put((228), "tg"); //Togo
        countryIndexToLocaleString.put((229), "bj"); //Benin
        countryIndexToLocaleString.put((230), "mu"); //Mauritius
        countryIndexToLocaleString.put((231), "lr"); //Liberia
        countryIndexToLocaleString.put((232), "sl"); //Sierra Leone
        countryIndexToLocaleString.put((233), "gh"); //Ghana
        countryIndexToLocaleString.put((234), "ng"); //Nigeria
        countryIndexToLocaleString.put((235), "td"); //Chad
        countryIndexToLocaleString.put((236), "cf"); //Central African Republic
        countryIndexToLocaleString.put((237), "cm"); //Cameroon
        countryIndexToLocaleString.put((238), "cv"); //Cape Verde Islands
        countryIndexToLocaleString.put((239), "st"); //Sao Tome & Principe
        countryIndexToLocaleString.put((240), "gq"); //Equatorial Guinea
        countryIndexToLocaleString.put((241), "ga"); //Gabon
        countryIndexToLocaleString.put((242), "cg"); //Congo, (Rep. of the)
        countryIndexToLocaleString.put((243), "cd"); //Congo, Democratic Republic of
        countryIndexToLocaleString.put((244), "ao"); //Angola
        countryIndexToLocaleString.put((245), "gw"); //Guinea-Bissau
//        countryIndexToLocaleString.put((246),""); //Diego Garcia ???
//        countryIndexToLocaleString.put((247),""); //Ascension Island ???
        countryIndexToLocaleString.put((248), "sc"); //Seychelles
        countryIndexToLocaleString.put((249), "sd"); //Sudan
        countryIndexToLocaleString.put((250), "rw"); //Rwanda
        countryIndexToLocaleString.put((251), "et"); //Ethiopia
        countryIndexToLocaleString.put((252), "so"); //Somalia
        countryIndexToLocaleString.put((253), "dj"); //Djibouti
        countryIndexToLocaleString.put((254), "ke"); //Kenya
        countryIndexToLocaleString.put((255), "tz"); //Tanzania
        countryIndexToLocaleString.put((256), "ug"); //Uganda
        countryIndexToLocaleString.put((257), "bi"); //Burundi
        countryIndexToLocaleString.put((258), "mz"); //Mozambique
        countryIndexToLocaleString.put((260), "zm"); //Zambia
        countryIndexToLocaleString.put((261), "mg"); //Madagascar
//        countryIndexToLocaleString.put((262),""); //Reunion Island ???
        countryIndexToLocaleString.put((263), "zw"); //Zimbabwe
        countryIndexToLocaleString.put((264), "na"); //Namibia
        countryIndexToLocaleString.put((265), "mw"); //Malawi
        countryIndexToLocaleString.put((266), "ls"); //Lesotho
        countryIndexToLocaleString.put((267), "bw"); //Botswana
        countryIndexToLocaleString.put((268), "sz"); //Swaziland
        countryIndexToLocaleString.put((269), "yt"); //Mayotte Island
        countryIndexToLocaleString.put((2691), "km"); //Comoros
        countryIndexToLocaleString.put((27), "za"); //South Africa
        countryIndexToLocaleString.put((290), "sh"); //St. Helena
        countryIndexToLocaleString.put((291), "er"); //Eritrea
        countryIndexToLocaleString.put((297), "aw"); //Aruba
//        countryIndexToLocaleString.put((298),""); //Faeroe Islands ???
        countryIndexToLocaleString.put((299), "gl"); //Greenland
        countryIndexToLocaleString.put((30), "gr"); //Greece
        countryIndexToLocaleString.put((31), "nl"); //Netherlands
        countryIndexToLocaleString.put((32), "be"); //Belgium
        countryIndexToLocaleString.put((33), "fr"); //France
        countryIndexToLocaleString.put((34), "es"); //Spain
        countryIndexToLocaleString.put((350), "gi"); //Gibraltar
        countryIndexToLocaleString.put((351), "pt"); //Portugal
        countryIndexToLocaleString.put((352), "lu"); //Luxembourg
        countryIndexToLocaleString.put((353), "ie"); //Ireland
        countryIndexToLocaleString.put((354), "is"); //Iceland
        countryIndexToLocaleString.put((355), "al"); //Albania
        countryIndexToLocaleString.put((356), "mt"); //Malta
        countryIndexToLocaleString.put((357), "cy"); //Cyprus
        countryIndexToLocaleString.put((358), "fi"); //Finland
        countryIndexToLocaleString.put((359), "bg"); //Bulgaria
        countryIndexToLocaleString.put((36), "hu"); //Hungary
        countryIndexToLocaleString.put((370), "lt"); //Lithuania
        countryIndexToLocaleString.put((371), "lv"); //Latvia
        countryIndexToLocaleString.put((372), "ee"); //Estonia
        countryIndexToLocaleString.put((373), "md"); //Moldova, Republic of
        countryIndexToLocaleString.put((374), "am"); //Armenia
        countryIndexToLocaleString.put((375), "by"); //Belarus
        countryIndexToLocaleString.put((376), "ad"); //Andorra
        countryIndexToLocaleString.put((377), "mc"); //Monaco
        countryIndexToLocaleString.put((378), "sm"); //San Marino
        countryIndexToLocaleString.put((379), "va"); //Vatican City
        countryIndexToLocaleString.put((380), "ua"); //Ukraine
//        countryIndexToLocaleString.put((381),""); //Yugoslavia ???
        countryIndexToLocaleString.put((3811), "cs"); //Yugoslavia - Serbia
        countryIndexToLocaleString.put((382), "cs"); //Yugoslavia - Montenegro
        countryIndexToLocaleString.put((385), "hr"); //Croatia
        countryIndexToLocaleString.put((386), "si"); //Slovenia
        countryIndexToLocaleString.put((387), "ba"); //Bosnia & Herzegovina
        countryIndexToLocaleString.put((389), "mk"); //Macedonia (F.Y.R.O.M.)
        countryIndexToLocaleString.put((39), "it"); //Italy
        countryIndexToLocaleString.put((40), "ro"); //Romania
        countryIndexToLocaleString.put((41), "ch"); //Switzerland
        countryIndexToLocaleString.put((4101), "li"); //Liechtenstein
        countryIndexToLocaleString.put((42), "cz"); //Czech Republic
        countryIndexToLocaleString.put((4201), "sk"); //Slovakia
        countryIndexToLocaleString.put((43), "at"); //Austria
        countryIndexToLocaleString.put((44), "gb"); //United Kingdom
//        countryIndexToLocaleString.put((441),""); //Wales ???
//        countryIndexToLocaleString.put((442),""); //Scotland ???
        countryIndexToLocaleString.put((45), "dk"); //Denmark
        countryIndexToLocaleString.put((46), "se"); //Sweden
        countryIndexToLocaleString.put((47), "no"); //Norway
        countryIndexToLocaleString.put((48), "pl"); //Poland
        countryIndexToLocaleString.put((49), "de"); //Germany
//        countryIndexToLocaleString.put((500),""); //Falkland Islands ???
        countryIndexToLocaleString.put((501), "bz"); //Belize
        countryIndexToLocaleString.put((502), "gt"); //Guatemala
        countryIndexToLocaleString.put((503), "sv"); //El Salvador
        countryIndexToLocaleString.put((504), "hn"); //Honduras
        countryIndexToLocaleString.put((505), "ni"); //Nicaragua
        countryIndexToLocaleString.put((506), "cr"); //Costa Rica
        countryIndexToLocaleString.put((507), "pa"); //Panama
        countryIndexToLocaleString.put((508), "pm"); //St. Pierre & Miquelon
        countryIndexToLocaleString.put((509), "ht"); //Haiti
        countryIndexToLocaleString.put((51), "pe"); //Peru
        countryIndexToLocaleString.put((52), "mx"); //Mexico
        countryIndexToLocaleString.put((53), "cu"); //Cuba
        countryIndexToLocaleString.put((54), "ar"); //Argentina
        countryIndexToLocaleString.put((55), "br"); //Brazil
        countryIndexToLocaleString.put((56), "cl"); //Chile, Republic of
        countryIndexToLocaleString.put((57), "co"); //Colombia
        countryIndexToLocaleString.put((58), "ve"); //Venezuela
        countryIndexToLocaleString.put((590), "gp"); //Guadeloupe
        countryIndexToLocaleString.put((5901), "an"); //French Antilles
        countryIndexToLocaleString.put((5902), "an"); //Antilles
        countryIndexToLocaleString.put((591), "bo"); //Bolivia
        countryIndexToLocaleString.put((592), "gy"); //Guyana
        countryIndexToLocaleString.put((593), "ec"); //Ecuador
        countryIndexToLocaleString.put((594), "gy"); //French Guyana
        countryIndexToLocaleString.put((595), "py"); //Paraguay
        countryIndexToLocaleString.put((596), "mq"); //Martinique
        countryIndexToLocaleString.put((597), "sr"); //Suriname
        countryIndexToLocaleString.put((598), "uy"); //Uruguay
        countryIndexToLocaleString.put((599), "an"); //Netherlands Antilles
        countryIndexToLocaleString.put((60), "my"); //Malaysia
        countryIndexToLocaleString.put((61), "au"); //Australia
        countryIndexToLocaleString.put((6101), "cc"); //Cocos-Keeling Islands
        countryIndexToLocaleString.put((6102), "cc"); //Cocos (Keeling) Islands
        countryIndexToLocaleString.put((62), "id"); //Indonesia
        countryIndexToLocaleString.put((63), "ph"); //Philippines
        countryIndexToLocaleString.put((64), "nz"); //New Zealand
        countryIndexToLocaleString.put((65), "sg"); //Singapore
        countryIndexToLocaleString.put((66), "th"); //Thailand
//        countryIndexToLocaleString.put((670),""); //Saipan Island ???
//        countryIndexToLocaleString.put((6701),""); //Rota Island  ???
//        countryIndexToLocaleString.put((6702),""); //Tinian Island ???
        countryIndexToLocaleString.put((671), "gu"); //Guam, US Territory of
        countryIndexToLocaleString.put((672), "cx"); //Christmas Island
        countryIndexToLocaleString.put((6722), "nf"); //Norfolk Island
        countryIndexToLocaleString.put((673), "bn"); //Brunei
        countryIndexToLocaleString.put((674), "nr"); //Nauru
        countryIndexToLocaleString.put((675), "pg"); //Papua New Guinea
        countryIndexToLocaleString.put((676), "to"); //Tonga
        countryIndexToLocaleString.put((677), "sb"); //Solomon Islands
        countryIndexToLocaleString.put((678), "vu"); //Vanuatu
        countryIndexToLocaleString.put((679), "fj"); //Fiji
        countryIndexToLocaleString.put((680), "pw"); //Palau
        countryIndexToLocaleString.put((681), "wf"); //Wallis & Futuna Islands
        countryIndexToLocaleString.put((682), "ck"); //Cook Islands
        countryIndexToLocaleString.put((683), "nu"); //Niue
        countryIndexToLocaleString.put((684), "as"); //American Samoa
        countryIndexToLocaleString.put((685), "ws"); //Western Samoa
        countryIndexToLocaleString.put((686), "ki"); //Kiribati
        countryIndexToLocaleString.put((687), "nc"); //New Caledonia
        countryIndexToLocaleString.put((688), "tv"); //Tuvalu
        countryIndexToLocaleString.put((689), "pf"); //French Polynesia
        countryIndexToLocaleString.put((690), "tk"); //Tokelau
        countryIndexToLocaleString.put((691), "fm"); //Micronesia, Federated States of
        countryIndexToLocaleString.put((692), "mh"); //Marshall Islands
        countryIndexToLocaleString.put((7), "ru"); //Russia
        countryIndexToLocaleString.put((705), "kz"); //Kazakhstan
        countryIndexToLocaleString.put((706), "kg"); //Kyrgyzstan
        countryIndexToLocaleString.put((708), "tj"); //Tajikistan
        countryIndexToLocaleString.put((709), "tm"); //Turkmenistan
        countryIndexToLocaleString.put((711), "uz"); //Uzbekistan
        countryIndexToLocaleString.put((81), "jp"); //Japan
        countryIndexToLocaleString.put((82), "kr"); //Korea, South
        countryIndexToLocaleString.put((84), "vn"); //Viet Nam
        countryIndexToLocaleString.put((850), "kp"); //Korea, North
        countryIndexToLocaleString.put((852), "hk"); //Hong Kong
        countryIndexToLocaleString.put((853), "mo"); //Macau
        countryIndexToLocaleString.put((855), "kh"); //Cambodia
        countryIndexToLocaleString.put((856), "la"); //Laos
        countryIndexToLocaleString.put((86), "cn"); //China
        countryIndexToLocaleString.put((880), "bd"); //Bangladesh
        countryIndexToLocaleString.put((886), "tw"); //Taiwan
        countryIndexToLocaleString.put((90), "tr"); //Turkey
        countryIndexToLocaleString.put((91), "in"); //India
        countryIndexToLocaleString.put((92), "pk"); //Pakistan
        countryIndexToLocaleString.put((93), "af"); //Afghanistan
        countryIndexToLocaleString.put((94), "lk"); //Sri Lanka
        countryIndexToLocaleString.put((95), "mm"); //Myanmar
        countryIndexToLocaleString.put((960), "mv"); //Maldives
        countryIndexToLocaleString.put((961), "lb"); //Lebanon
        countryIndexToLocaleString.put((962), "jo"); //Jordan
        countryIndexToLocaleString.put((963), "sy"); //Syrian Arab Republic
        countryIndexToLocaleString.put((964), "iq"); //Iraq
        countryIndexToLocaleString.put((965), "kw"); //Kuwait
        countryIndexToLocaleString.put((966), "sa"); //Saudi Arabia
        countryIndexToLocaleString.put((967), "ye"); //Yemen
        countryIndexToLocaleString.put((968), "om"); //Oman
        countryIndexToLocaleString.put((971), "ae"); //United Arabian Emirates
        countryIndexToLocaleString.put((972), "il"); //Israel
        countryIndexToLocaleString.put((973), "bh"); //Bahrain
        countryIndexToLocaleString.put((974), "qa"); //Qatar
        countryIndexToLocaleString.put((975), "bt"); //Bhutan
        countryIndexToLocaleString.put((976), "mn"); //Mongolia
        countryIndexToLocaleString.put((977), "np"); //Nepal
        countryIndexToLocaleString.put((98), "ir"); //Iran (Islamic Republic of)
        countryIndexToLocaleString.put((994), "az"); //Azerbaijan
        countryIndexToLocaleString.put((995), "ge"); //Georgia
//        countryIndexToLocaleString.put((9999),""); //other
    }

    /**
     * Notified if buddy icon is changed
     */
    private class IconUpdateListener
        implements IconRequestListener
    {
        public void buddyIconCleared(IconService iconService,
                                     Screenname screenname,
                                     ExtraInfoData extraInfoData)
        {}

        public void buddyIconUpdated(IconService iconService,
                                     Screenname screenname,
                                     ExtraInfoData extraInfoData,
                                     ByteBlock byteBlock)
        {
            if(byteBlock != null)
            {
                if(screenname.getFormatted().equals(uin))
                {
                    synchronized(this)
                    {
                        byte[] img = byteBlock.toByteArray();

                        if(img != null && img.length > 0)
                            accountImage = new ImageDetail("Account Image", img);

                        this.notifyAll();
                    }
                }
            }
        }

        public void waitForImage(long waitFor)
        {
            synchronized(this)
            {
                try{
                    if(accountImage == null)
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
     *
     */
//    private class ChangeDetailInfoCmd extends ToIcqCmd
//    {
//        // used when semding this command for changeg details stored on the server
//        private String senderUin = null;
//        private List changeDataTlvs = new LinkedList();
//
//        /**
//         * Constructs command send to server
//         * @param senderUin String the uin of the sender
//         * @param changedData List the data to be changed
//         * @param toBeCleared List the data to be cleared,
//         *      if no such data null can be passed
//         */
//        public ChangeDetailInfoCmd(String senderUin, List changedData,
//                                   List toBeCleared)
//        {
//            super(Long.parseLong(senderUin), CMD_SET_FULLINFO, 2);
//            this.senderUin = senderUin;
//
//            // if toBeCleared == null. make it empty one
//            if (toBeCleared == null)
//                toBeCleared = new ArrayList();
//
//            // fix the spoken languages must be 3
//            int lastSpokenIndex = -1;
//            int countOfLanguages = 0;
//            boolean isLanguageFound = false;
//            for (int i = 0; i < changedData.size(); i++)
//            {
//                Object item = changedData.get(i);
//                if (item instanceof ServerStoredDetails.SpokenLanguageDetail)
//                {
//                    isLanguageFound = true;
//                    lastSpokenIndex = i;
//                    countOfLanguages++;
//                }
//            }
//
//            if (isLanguageFound)
//            {
//                for (int i = countOfLanguages; i < 3; i++)
//                {
//                    lastSpokenIndex++;
//                    changedData.add(lastSpokenIndex,
//                                    new ServerStoredDetails.SpokenLanguageDetail(null));
//                }
//            }
//
//            Iterator iter = changedData.iterator();
//            while (iter.hasNext())
//            {
//                ServerStoredDetails.GenericDetail item =
//                    (ServerStoredDetails.GenericDetail) iter.next();
//
//                if (toBeCleared.contains(item))
//                    changeDataTlvs.add(getClearTlv(item));
//                else
//                    changeDataTlvs.add(getTlvForChange(item));
//            }
//        }
//
//        public void writeIcqData(OutputStream out)
//            throws IOException
//        {
//            // write tlvs with data here
//            Iterator iter = changeDataTlvs.iterator();
//            while (iter.hasNext())
//            {
//                DetailTlv item = (DetailTlv) iter.next();
//                item.write(out);
//            }
//
//        }
//
//        /**
//         * Correspondig the type of ServerStoredDetails returns empty Tlv or Tlv
//         * with default value
//         * @param detail GenericDetail
//         * @return DetailTlv
//         */
//        private DetailTlv getClearTlv(ServerStoredDetails.GenericDetail detail)
//        {
//            int typeOfDetail = supportedTypes.get(detail.getClass())[1];
//
//            DetailTlv result = new DetailTlv(supportedTypes.get(detail.getClass())[1]);
//
//            switch (typeOfDetail)
//            {
//                case 0x01A4: //CountryDetail
//                case 0x0334: //OriginCountryDetail
//                case 0x02B2: //WorkCountryDetail
//                    result.writeUShort(0);
//                    break;
//
//                case 0x0186: //SpokenLanguageDetail
//                    logger.trace("write lang 0");
//                    result.writeUShort(0);
//                case 0x017C: //GenderDetail
//                    result.writeUByte(0);
//                case 0x023A: //BirthDateDetail
//                    result.writeUShort(0);
//                    result.writeUShort(0);
//                    result.writeUShort(0);
//                case 0x01CC: //WorkOcupationDetail
//                    result.writeUShort(0);
//                case 0x01EA: //InterestDetail
//                    result.writeUShort(0);
//                    result.writeString("");
//                case 0x0316:
//                    result.writeUByte(0);
//                default:
//                    result.writeString("");
//            }
//
//            return result;
//        }
//
//        /**
//         * Converts ServerStoredDetails to Tlv which later is converted to bytes
//         * and send to server
//         * @param detail GenericDetail the detail
//         * @return DetailTlv
//         */
//        private DetailTlv getTlvForChange(ServerStoredDetails.GenericDetail detail)
//        {
//            int typeOfDetail = supportedTypes.get(detail.getClass())[1];
//
//            DetailTlv result = new DetailTlv(typeOfDetail);
//
//            switch (typeOfDetail)
//            {
//                case 0x01A4: //CountryDetail
//                case 0x0334: //OriginCountryDetail
//                case 0x02B2: //WorkCountryDetail
//                    result.writeUShort(getCountryCode( ( (ServerStoredDetails.
//                        LocaleDetail) detail).getLocale()));
//                    break;
//
//                case 0x0186: //SpokenLanguageDetail
//                    writeLanguageCode( (ServerStoredDetails.LocaleDetail) detail,
//                                      result);
//                    break;
//                case 0x017C: //GenderDetail
//                    writeGenderCode( (ServerStoredDetails.GenderDetail) detail,
//                                    result);
//                    break;
//                case 0x023A: //BirthDateDetail
//                    writeCalendarCode( (ServerStoredDetails.CalendarDetail) detail,
//                                      result);
//                    break;
//                case 0x01CC: //WorkOcupationDetail
//                    writeOccupationCode( (WorkOcupationDetail) detail, result);
//                    break;
//                case 0x01EA: //InterestDetail
//                    writeInterestCode( (InterestDetail) detail, result);
//                    break;
//                default:
//                    writeGenericDetail(detail, result);
//            }
//
//            return result;
//        }
//
//        /**
//         * Writes the corresponding index for Language from ServerStoredDetails to the tlv
//         * @param detail LocaleDetail
//         * @param tlv DetailTlv
//         */
//        private void writeLanguageCode(ServerStoredDetails.LocaleDetail detail,
//                                       DetailTlv tlv)
//        {
//            Locale newLang = detail.getLocale();
//            if (newLang == null)
//            {
//                // this indicates that we must set language as not specified
//                tlv.writeUShort(0);
//                logger.trace("write lang 0");
//            }
//            else
//            {
//                for (int i = 1; i < spokenLanguages.length; i++)
//                {
//                    // indicating that language is not set
//                    if (getSpokenLanguage(i).equals(newLang))
//                    {
//                        logger.trace("write lang " + i);
//                        tlv.writeUShort(i);
//                        return;
//                    }
//                }
//            }
//        }
//
//        /**
//         * Writes the corresponding index for Gender from ServerStoredDetails to the tlv
//         * @param detail GenderDetail
//         * @param tlv DetailTlv
//         */
//        private void writeGenderCode(ServerStoredDetails.GenderDetail detail,
//                                     DetailTlv tlv)
//        {
//            int gender = 0;
//
//            if (detail.equals(ServerStoredDetails.GenderDetail.FEMALE))
//                gender = 1;
//            else if (detail.equals(ServerStoredDetails.GenderDetail.MALE))
//                gender = 2;
//
//            tlv.writeUByte(gender);
//        }
//
//        /**
//         * Writes the corresponding index for Calendar(BirthDate) from ServerStoredDetails to the tlv
//         * @param detail CalendarDetail
//         * @param tlv DetailTlv
//         */
//        private void writeCalendarCode(ServerStoredDetails.CalendarDetail detail,
//                                       DetailTlv tlv)
//        {
//            Calendar calendar = detail.getCalendar();
//
//            tlv.writeUShort(calendar.get(Calendar.YEAR));
//            tlv.writeUShort(calendar.get(Calendar.MONTH));
//            tlv.writeUShort(calendar.get(Calendar.DAY_OF_MONTH));
//        }
//
//        /**
//         * Writes the corresponding index for Occupation from ServerStoredDetails to the tlv
//         * @param detail WorkOcupationDetail
//         * @param tlv DetailTlv
//         */
//        private void writeOccupationCode(WorkOcupationDetail detail, DetailTlv tlv)
//        {
//            for (int i = 0; i < occupations.length; i++)
//            {
//                if (occupations[i].equals(detail.getDetailValue()))
//                    tlv.writeUShort(i);
//            }
//        }
//
//        /**
//         * Writes the corresponding index for Interests from ServerStoredDetails to the tlv
//         * @param detail InterestDetail
//         * @param tlv DetailTlv
//         */
//        private void writeInterestCode(InterestDetail detail, DetailTlv tlv)
//        {
//            String category = detail.getCategory();
//            int categoryInt = 0;
//            for (int i = 0; i < interestsCategories.length; i++)
//            {
//                if (interestsCategories[i].equals(category))
//                {
//                    if (i != 0)
//                        categoryInt = i + 99;
//                    else
//                        categoryInt = 0;
//
//                    break;
//                }
//            }
//
//            tlv.writeUShort(categoryInt);
//            tlv.writeString(detail.getInterest());
//        }
//
//        /**
//         * Writes the corresponding value for ServerStoredDetails to the tlv
//         * @param detail GenericDetail
//         * @param tlv DetailTlv
//         */
//        private void writeGenericDetail(ServerStoredDetails.GenericDetail detail,
//                                        DetailTlv tlv)
//        {
//            if (detail instanceof ServerStoredDetails.StringDetail)
//            {
//                tlv.writeString( ( (ServerStoredDetails.StringDetail) detail).
//                                getString());
//            }
//            else
//            if (detail instanceof ServerStoredDetails.TimeZoneDetail)
//            {
//                int offset = ( (ServerStoredDetails.TimeZoneDetail) detail).
//                    getTimeZone().getRawOffset() / (60 * 60 * 1000);
//                tlv.writeUByte(offset);
//            }
//        }
//
//    }
//
//    /**
//     * Tlv set in command for changis user account info stored on server
//     * @author Damian Minkov
//     */
//    public class DetailTlv
//        implements Writable
//    {
//        private byte[] data = new byte[0];
//        private int type;
//
//        public DetailTlv(int type)
//        {
//            this.type = type;
//        }
//
//        public void write(OutputStream out) throws IOException
//        {
//            LEBinaryTools.writeUShort(out, type);
//            LEBinaryTools.writeUShort(out, data.length);
//            out.write(data);
//        }
//
//        public long getWritableLength()
//        {
//            return 4 + data.length;
//        }
//
//        public void writeUInt(long number)
//        {
//            byte[] tmp = LEBinaryTools.getUInt(number);
//            byte[] newData = new byte[data.length + tmp.length];
//
//            System.arraycopy(data, 0, newData, 0, data.length);
//            System.arraycopy(tmp, 0, newData, data.length, tmp.length);
//
//            data = newData;
//        }
//
//        public void writeUShort(int number)
//        {
//            byte[] tmp = LEBinaryTools.getUShort(number);
//            byte[] newData = new byte[data.length + tmp.length];
//
//            System.arraycopy(data, 0, newData, 0, data.length);
//            System.arraycopy(tmp, 0, newData, data.length, tmp.length);
//
//            data = newData;
//        }
//
//        public void writeUByte(int number)
//        {
//            byte[] tmp = LEBinaryTools.getUByte(number);
//            byte[] newData = new byte[data.length + tmp.length];
//
//            System.arraycopy(data, 0, newData, 0, data.length);
//            System.arraycopy(tmp, 0, newData, data.length, tmp.length);
//
//            data = newData;
//        }
//
//        public void writeString(String str)
//        {
//            if (str == null)
//                str = ""; // empty string so length will be 0 and nothing to be writen
//
//            byte[] tmp = BinaryTools.getAsciiBytes(str);
//
//            // save the string length before we process the string bytes
//            writeUShort(tmp.length);
//
//            byte[] newData = new byte[data.length + tmp.length];
//
//            System.arraycopy(data, 0, newData, 0, data.length);
//            System.arraycopy(tmp, 0, newData, data.length, tmp.length);
//
//            data = newData;
//        }
//
//        public String toString()
//        {
//            StringBuffer result = new StringBuffer();
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//            try
//            {
//                write(out);
//            }
//            catch (IOException ex)
//            {
//                ex.printStackTrace();
//                return null;
//            }
//
//            byte[] arrOut = out.toByteArray();
//            for (int i = 0; i < arrOut.length; i++)
//            {
//                byte temp = arrOut[i];
//                result.append(Integer.toHexString(temp & 0xFF)).append(' ');
//            }
//
//            return result.toString();
//        }
//    }
//


}
