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
package net.java.sip.communicator.slick.protocol.icq;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.util.*;

/**
 * Testing of the user and account info. Tests fo reading , adding , replacing,
 * removing and error handling.
 * @author Damian Minkov
 */
public class TestOperationSetServerStoredInfo
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetServerStoredInfo.class);

    private IcqSlickFixture fixture = new IcqSlickFixture();

    private OperationSetServerStoredAccountInfo
        opSetServerStoredAccountInfo = null;

    private OperationSetServerStoredContactInfo
        opSetServerStoredContactInfo = null;

    private OperationSetPresence opSetPresence = null;

    public TestOperationSetServerStoredInfo(String name)
    {
        super(name);
    }

    /**
     * Get a reference to the contact and account info operation sets.
     * @throws Exception if this is not a good day.
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map<String, OperationSet> supportedOperationSets =
            fixture.provider.getSupportedOperationSets();

        if ( supportedOperationSets == null
            || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this ICQ implementation. ");

        opSetServerStoredAccountInfo =
            (OperationSetServerStoredAccountInfo)supportedOperationSets.get(
                OperationSetServerStoredAccountInfo.class.getName());

        opSetServerStoredContactInfo =
            (OperationSetServerStoredContactInfo)supportedOperationSets.get(
                OperationSetServerStoredContactInfo.class.getName());

        //if the op set is null then the implementation doesn't offer a account info.
        //operation set which is unacceptable for icq.
        if (opSetServerStoredAccountInfo == null)
        {
            throw new NullPointerException(
                "No implementation for Account Info was found");
        }

        //if the op set is null then the implementation doesn't offer a contact info.
        //operation set which is unacceptable for icq.
        if (opSetServerStoredContactInfo == null)
        {
            throw new NullPointerException(
                "No implementation for Contact Info was found");
        }

        opSetPresence =
            (OperationSetPresence)supportedOperationSets.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence == null)
        {
            throw new NullPointerException(
                "An implementation of the ICQ service must provide an "
                + "implementation of at least one of the PresenceOperationSets");
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        fixture.tearDown();
    }

    /**
     * Creates a test suite containing tests of this class in a specific order.
     * We'll first execute tests beginning with the "test" prefix and then go to
     * ordered tests. We first execute tests for reading info, then writing.
     * Then the ordered tests - error handling and finaly for removing details
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static Test suite()
    {
        TestSuite suite
            = new TestSuite(TestOperationSetServerStoredInfo.class);

        // error handling expects that all three languages are set.
        // this is done in previous tests
//        suite.addTest(new TestOperationSetServerStoredInfo("errorHandling"));
        // the final one as we will remove some of the already set values
//        suite.addTest(new TestOperationSetServerStoredInfo("removingItems"));

        return suite;
    }

    /**
     * Test reading info. changing info of the TesterAgen and checking
     * it through the contactInfo operation set
     */
    public void testReadInfo()
    {
        Object lock = new Object();

        // make a random name. To be sure its not the value we used the last test
        int suffix = (int)(Math.random()*100);
        String lastName = "TesterAgent" + String.valueOf(suffix);

        IcqSlickFixture.testerAgent.setUserInfoLastName(lastName);

        // give the server time to change things
        synchronized(lock)
        {
            try{
                lock.wait(4000);}
            catch (InterruptedException ex){}
        }

        // make the phonenumber also random
        String phoneNumber = "+3591234" + suffix;
        IcqSlickFixture.testerAgent.setUserInfoPhoneNumber(phoneNumber);

        // give the server time to change things
        synchronized(lock)
        {
            try{
                lock.wait(4000);}
            catch (InterruptedException ex){}
        }

        // Getting a random language index between 1 and 72 ,
        // see spokenLanguages array
        int lang1 =  1 + (int)(Math.random() * 72);
        int lang2 =  1 + (int)(Math.random() * 72);
        int lang3 =  1 + (int)(Math.random() * 72);

        // setting this languages as spoken languages
        IcqSlickFixture.testerAgent.setUserInfoLanguage(lang1, lang2, lang3);

        // give the server time to change things
        synchronized(lock)
        {
            try{
                lock.wait(4000);}
            catch (InterruptedException ex){}
        }

        // get a random country from countryIndexToLocaleString Array
        // 232 is the count of the countries in this array
        int countryRandom =  0 + (int)(Math.random() * 232);
        int countryCode = ((Integer)countryIndexToLocaleString[countryRandom][0]).intValue();
        String countryAbr = (String)countryIndexToLocaleString[countryRandom][1];
        IcqSlickFixture.testerAgent.setUserInfoHomeCountry(countryCode);

        // give the server time to change things
        synchronized(lock)
        {
            try{
                lock.wait(4000);}
            catch (InterruptedException ex){}
        }

        Contact testerAgentContact
            = opSetPresence.findContactByID(IcqSlickFixture.testerAgent.getIcqUIN());

        // Get the last name info
        Iterator<GenericDetail> iter =
            opSetServerStoredContactInfo.
                getDetails(testerAgentContact,
                           ServerStoredDetails.LastNameDetail.class);

        while (iter.hasNext())
        {
            ServerStoredDetails.LastNameDetail item
                = (ServerStoredDetails.LastNameDetail) iter.next();

            assertEquals("The LastName we set is not set or not read properly"
                     , item.getName()
                     , lastName);
            break;
        }

        // Get phone number info
        iter =
            opSetServerStoredContactInfo.
                getDetails(testerAgentContact,
                           ServerStoredDetails.PhoneNumberDetail.class);
        while (iter.hasNext())
        {
            ServerStoredDetails.PhoneNumberDetail item = (ServerStoredDetails.PhoneNumberDetail) iter.next();

            assertEquals("The PhoneNumber we set is not set or not read properly"
                     , item.getNumber()
                     , phoneNumber);
            break;
        }

        // get the spoken languages
        iter =
            opSetServerStoredContactInfo.
                getDetails(testerAgentContact,
                           ServerStoredDetails.SpokenLanguageDetail.class);
        List<Locale> spokenLanguagesServer = new ArrayList<Locale>();
        while (iter.hasNext())
        {
            ServerStoredDetails.SpokenLanguageDetail item
                = (ServerStoredDetails.SpokenLanguageDetail)iter.next();
            spokenLanguagesServer.add(item.getLocale());
        }

        assertEquals("spoken languages must be 3 "
                     , 3
                     , spokenLanguagesServer.size());

        assertTrue("Must contain langiage " + spokenLanguages[lang1],
                   spokenLanguagesServer.contains(spokenLanguages[lang1]));

        assertTrue("Must contain langiage " + spokenLanguages[lang2],
                   spokenLanguagesServer.contains(spokenLanguages[lang2]));

        assertTrue("Must contain langiage " + spokenLanguages[lang3],
                   spokenLanguagesServer.contains(spokenLanguages[lang3]));

        // get home country code detail
        iter =
            opSetServerStoredContactInfo.
                getDetails(testerAgentContact,
                           ServerStoredDetails.CountryDetail.class);
        while (iter.hasNext())
        {
            ServerStoredDetails.CountryDetail item
                = (ServerStoredDetails.CountryDetail) iter.next();

            logger.info("read item value: " + item.getLocale().getDisplayCountry());

            assertEquals("The Country we set is not set or not read properly"
                     , item.getLocale()
                     , new Locale("", countryAbr));
            break;
        }

    }

    /**
     * Testing changing of the details.
     * Changing the details from the account info operation set
     * and checking the values retreived from TesterAgent
     */
    public void testWriteInfo()
    {
        Object lock = new Object();

        // first get the details if existing
        ServerStoredDetails.LastNameDetail lastNameDetail = null;
        ServerStoredDetails.PhoneNumberDetail phoneNumberDetail = null;
//        Iterator iterSpokenLangDetails = null;
        ServerStoredDetails.CountryDetail homeCountryDetail = null;

        // Get Last name info detail
        Iterator<GenericDetail> iter =
            opSetServerStoredAccountInfo.
            getDetails(ServerStoredDetails.LastNameDetail.class);
        if (iter.hasNext())
            lastNameDetail = (ServerStoredDetails.LastNameDetail) iter.next();

        // Get phone number info
        iter = opSetServerStoredAccountInfo.
            getDetails(ServerStoredDetails.PhoneNumberDetail.class);
        if (iter.hasNext())
            phoneNumberDetail = (ServerStoredDetails.PhoneNumberDetail)
                iter.next();

        // Get spoken languages
//        iterSpokenLangDetails = opSetServerStoredAccountInfo.
//            getDetails(ServerStoredDetails.SpokenLanguageDetail.class);

        // Get home country code detail
        iter = opSetServerStoredAccountInfo.
            getDetails(ServerStoredDetails.CountryDetail.class);
        if (iter.hasNext())
            homeCountryDetail = (ServerStoredDetails.CountryDetail) iter.
                next();

        // make a random name to be sure its different every test
        int suffix = (int) (Math.random() * 100);
        String newLastName = "TesterAgent" + String.valueOf(suffix);
        // the phone number also random
        String newPhoneNumber = "+3591234" + suffix;

        // random languages - between 1 and 72
//        int[] newLanguages =
//            {
//            (1 + (int) (Math.random() * 72)),
//            (1 + (int) (Math.random() * 72)),
//            (1 + (int) (Math.random() * 72))
//        };

        // the countries are between 0 and 232, see countryIndexToLocaleString,
        // which lengthe is 232
        int countryRandom = 0 + (int) (Math.random() * 232);
        int newCountryCode = ( (Integer) countryIndexToLocaleString[
                              countryRandom][0]).intValue();
        String newCountryAbr = (String) countryIndexToLocaleString[
            countryRandom][1];

        try
        {
            // now if existing replace detail or add if not
            // using the new generated values
            if (lastNameDetail != null)
            {
                assertTrue("Cannot set Detail LastName : " + newLastName,
                           opSetServerStoredAccountInfo.replaceDetail(
                    lastNameDetail,
                    new ServerStoredDetails.LastNameDetail(newLastName)));
            }
            else
            {
                opSetServerStoredAccountInfo.addDetail(
                    new ServerStoredDetails.LastNameDetail(newLastName));
            }

            // give time to server to change things
            synchronized (lock)
            {
                try{
                    lock.wait(5000);}
                catch (InterruptedException ex)
                {}
            }

            if (phoneNumberDetail != null)
            {
                assertTrue("Cannot set Detail PhoneNumver : " +
                           newPhoneNumber,
                           opSetServerStoredAccountInfo.replaceDetail(
                    phoneNumberDetail,
                    new ServerStoredDetails.PhoneNumberDetail(
                    newPhoneNumber)));
            }
            else
            {
                opSetServerStoredAccountInfo.addDetail(
                    new ServerStoredDetails.PhoneNumberDetail(
                    newPhoneNumber));
            }

            // give time to server to change things
            synchronized (lock)
            {
                try{
                    lock.wait(5000);}
                catch (InterruptedException ex)
                {}
            }

            if (homeCountryDetail != null)
            {
                assertTrue("Cannot set Detail Country : " + newCountryAbr,

                           opSetServerStoredAccountInfo.replaceDetail(
                    homeCountryDetail,
                    new ServerStoredDetails.CountryDetail(new Locale("",
                    newCountryAbr)))); ;
            }
            else
            {
                opSetServerStoredAccountInfo.addDetail(
                    new ServerStoredDetails.CountryDetail(new Locale("",
                    newCountryAbr)));
            }

            // give time to server to change things
            synchronized (lock)
            {
                try{
                    lock.wait(5000);}
                catch (InterruptedException ex)
                {}
            }

//            int numberOfChangedLanguages = 0;
//            while (iterSpokenLangDetails.hasNext())
//            {
//                ServerStoredDetails.SpokenLanguageDetail item =
//                    (ServerStoredDetails.SpokenLanguageDetail)
//                    iterSpokenLangDetails.next();
//
//                // if we are here so there is language - replace it
//                int newLang = newLanguages[numberOfChangedLanguages++];
//
//                opSetServerStoredAccountInfo.replaceDetail(item,
//                    new ServerStoredDetails.SpokenLanguageDetail(
//                        spokenLanguages[newLang]));
//
//                // give time to server to change things, as we change the languages one by one
//                synchronized (lock)
//                {
//                    try{
//                        lock.wait(10000);}
//                    catch (InterruptedException ex)
//                    {}
//                }
//            }
//            // if not all languages set , set the rest. they are not existing,
//            // so add them
//            for (int i = numberOfChangedLanguages; i < 3; i++)
//            {
//                int newLang = newLanguages[numberOfChangedLanguages++];
//
//                opSetServerStoredAccountInfo.addDetail(
//                    new ServerStoredDetails.SpokenLanguageDetail(
//                        spokenLanguages[newLang]));
//
//                // give time to server to change things
//                synchronized (lock)
//                {
//                    try{
//                        lock.wait(10000);
//                    }
//                    catch (InterruptedException ex)
//                    {}
//                }
//            }
//
//            // give time to server to change things
//            synchronized (lock)
//            {
//                try
//                {
//                    lock.wait(5000);
//                }
//                catch (InterruptedException ex)
//                {}
//            }
            logger.trace("Finished Setting values!");
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            throw new RuntimeException(
                "Error setting detail! Max detail instances is reached!", ex);
        }
        catch (IllegalArgumentException ex)
        {
            throw new RuntimeException(
                "Error setting detail! Detail max instances reached - cannot be set!", ex);
        }
        catch (OperationFailedException ex)
        {
            logger.error("", ex);
            throw new RuntimeException(
                "Error setting detail! Network Failure!", ex);
        }
        catch (ClassCastException ex)
        {
            throw new RuntimeException(
                "Error setting detail! ", ex);
        }

        logger.trace("Proceeding to Testing values!");
        // make the tests here

        Hashtable<String, Object> userInfo = IcqSlickFixture.testerAgent.getUserInfo(fixture.ourUserID);

        assertEquals("The LastName we set is not set or not read properly"
                     , newLastName
                     , userInfo.get(FullUserInfoCmd.LAST_NAME));

        assertEquals("The PhoneNumber we set is not set or not read properly"
                     , newPhoneNumber
                     , userInfo.get(FullUserInfoCmd.PHONE_NUMBER));

        List<?> languageCodes
            = (ArrayList<?>) userInfo.get(FullUserInfoCmd.SPEAK_LANG);
        ArrayList<Locale> languages = new ArrayList<Locale>();
        // convert language codes to locales in the list
        Iterator<?> languageCodeIter = languageCodes.iterator();
        while (languageCodeIter.hasNext())
        {
            languages
                .add(
                    spokenLanguages[
                        ((Integer) languageCodeIter.next()).intValue()]);
        }
//        assertEquals("The number of spoken languages dowsn't match",
//            newLanguages.length,
//            languages.size());
//
//        for (int i = 0; i < newLanguages.length; i++)
//        {
//            assertTrue("The Spoken Language we set is not set or " +
//                       "not read properly : " + newLanguages[i],
//                languages.contains(spokenLanguages[newLanguages[i]]));
//        }

        assertEquals("The Country we set is not set or not read properly"
                     , new Integer(newCountryCode)
                     , userInfo.get(FullUserInfoCmd.HOME_COUNTRY));
    }

    /**
     * Checking if the error handling works (all throw clauses in the methods)
     * If max number of details is ok. Chacking of details classes.
     */
    public void errorHandling()
    {
        Iterator<GenericDetail> iter =
            opSetServerStoredAccountInfo.
                getDetails(ServerStoredDetails.SpokenLanguageDetail.class);

        ArrayList<GenericDetail> initialLanguages
            = new ArrayList<GenericDetail>();
        while (iter.hasNext())
            initialLanguages.add(iter.next());

        assertEquals("There must be 3 language details!", 3, initialLanguages.size());

        try
        {
            opSetServerStoredAccountInfo.addDetail(new ServerStoredDetails.
                SpokenLanguageDetail(null));

            fail("As there is already reched the max instances of languages. Exception must be thrown");
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {}
        catch (OperationFailedException ex)
        {}
        catch (IllegalArgumentException ex)
        {}

        DummyDetail dummyDetail = new DummyDetail();

        try
        {
            assertFalse("This class is not supported!",
                        opSetServerStoredAccountInfo.isDetailClassSupported(dummyDetail.getClass()));

            opSetServerStoredAccountInfo.addDetail(dummyDetail);

            fail("As this class is not supperted. Exception must be thrown");
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {}
        catch (OperationFailedException ex)
        {}
        catch (IllegalArgumentException ex)
        {}

        try
        {
            opSetServerStoredAccountInfo.replaceDetail(new ServerStoredDetails.
                SpokenLanguageDetail(null),
                dummyDetail);

            fail("The parameters are from different classes. Exception must be thrown");
        }
        catch (OperationFailedException ex1)
        {}
        catch (ClassCastException ex1)
        {}
    }

    /**
     * Details used only for class checking when passing it to
     * modification methods. As its not in the implementation
     * ClassCastException must be thrown
     */
    private class DummyDetail
        extends ServerStoredDetails.NameDetail
    {
        DummyDetail()
        {super("TestName", "mayName");}
    }

    /**
     * Removing details from the account info operation set and checking
     * if they are removed from the implementation and from the server.
     * After removing detail - the next retreiving of the info updates all the
     * details from the server
     */
    public void removingItems()
    {
        Object lock = new Object();

        try
        {
            Iterator<GenericDetail> iter =
                opSetServerStoredAccountInfo.
                    getDetails(ServerStoredDetails.SpokenLanguageDetail.class);

            List<GenericDetail> initialLanguages
                = new ArrayList<GenericDetail>();
            while (iter.hasNext())
                initialLanguages.add(iter.next());

            // now remove those languages
            iter = initialLanguages.iterator();
            while (iter.hasNext())
            {
                assertTrue(
                    "Error removing language!",
                    opSetServerStoredAccountInfo.removeDetail(iter.next()));
                synchronized (lock)
                {
                try{
                    lock.wait(4000);
                }
                catch (InterruptedException ex)
                {}
            }

            }

            // give time to server
            synchronized (lock){
                try{
                    lock.wait(4000);
                }
                catch (InterruptedException ex)
                {}
            }

            iter =
                opSetServerStoredAccountInfo.
                    getDetails(ServerStoredDetails.SpokenLanguageDetail.class);

            List<GenericDetail> languages = new ArrayList<GenericDetail>();
            while (iter.hasNext())
                languages.add(iter.next());
            logger.trace("languages " + languages.size());

            // there must be no languages after the last retrieve
            assertEquals("There must be no language details!", 0, languages.size());
        }
        catch (OperationFailedException ex)
        {
            throw new RuntimeException(
                    "Error setting or retreiving detail! Network Failure!", ex);
        }
    }

    // Indexes of countries as stored in the icq server
    // and their corresponding locale strings
    private static Object[][] countryIndexToLocaleString =
    {
            //        {new Integer(0),""}, //not specified
            {new Integer(1), "us"}, //USA
            {new Integer(101), "ai"}, //Anguilla
            {new Integer(102), "ag"}, //Antigua
            {new Integer(1021), "ag"}, //Antigua & Barbuda
            {new Integer(103), "bs"}, //Bahamas
            {new Integer(104), "bb"}, //Barbados
            {new Integer(105), "bm"}, //Bermuda
            {new Integer(106), "vg"}, //British Virgin Islands
            {new Integer(107), "ca"}, //Canada
            {new Integer(108), "ky"}, //Cayman Islands
            {new Integer(109), "dm"}, //Dominica
            {new Integer(110), "do"}, //Dominican Republic
            {new Integer(111), "gd"}, //Grenada
            {new Integer(112), "jm"}, //Jamaica
            {new Integer(113), "ms"}, //Montserrat
            {new Integer(114), "kn"}, //Nevis
            {new Integer(1141), "kn"}, //Saint Kitts and Nevis
            {new Integer(115), "kn"}, //St. Kitts
            {new Integer(116), "vc"}, //St. Vincent & the Grenadines
            {new Integer(117), "tt"}, //Trinidad & Tobago
            {new Integer(118), "tc"}, //Turks & Caicos Islands
            {new Integer(120), "ag"}, //Barbuda
            {new Integer(121), "pr"}, //Puerto Rico
            {new Integer(122), "lc"}, //Saint Lucia
            {new Integer(123), "vi"}, //Virgin Islands (USA)
            {new Integer(178), "es"}, //Canary Islands ???
            {new Integer(20), "eg"}, //Egypt
            {new Integer(212), "ma"}, //Morocco
            {new Integer(213), "dz"}, //Algeria
            {new Integer(216), "tn"}, //Tunisia
            {new Integer(218), "ly"}, //Libyan Arab Jamahiriya
            {new Integer(220), "gm"}, //Gambia
            {new Integer(221), "sn"}, //Senegal
            {new Integer(222), "mr"}, //Mauritania
            {new Integer(223), "ml"}, //Mali
            {new Integer(224), "pg"}, //Guinea
            {new Integer(225), "ci"}, //Cote d'Ivoire
            {new Integer(226), "bf"}, //Burkina Faso
            {new Integer(227), "ne"}, //Niger
            {new Integer(228), "tg"}, //Togo
            {new Integer(229), "bj"}, //Benin
            {new Integer(230), "mu"}, //Mauritius
            {new Integer(231), "lr"}, //Liberia
            {new Integer(232), "sl"}, //Sierra Leone
            {new Integer(233), "gh"}, //Ghana
            {new Integer(234), "ng"}, //Nigeria
            {new Integer(235), "td"}, //Chad
            {new Integer(236), "cf"}, //Central African Republic
            {new Integer(237), "cm"}, //Cameroon
            {new Integer(238), "cv"}, //Cape Verde Islands
            {new Integer(239), "st"}, //Sao Tome & Principe
            {new Integer(240), "gq"}, //Equatorial Guinea
            {new Integer(241), "ga"}, //Gabon
            {new Integer(242), "cg"}, //Congo, (Rep. of the)
            {new Integer(243), "cd"}, //Congo, Democratic Republic of
            {new Integer(244), "ao"}, //Angola
            {new Integer(245), "gw"}, //Guinea-Bissau
        //        {new Integer(246),""}, //Diego Garcia ???
        //        {new Integer(247),""}, //Ascension Island ???
            {new Integer(248), "sc"}, //Seychelles
            {new Integer(249), "sd"}, //Sudan
            {new Integer(250), "rw"}, //Rwanda
            {new Integer(251), "et"}, //Ethiopia
            {new Integer(252), "so"}, //Somalia
            {new Integer(253), "dj"}, //Djibouti
            {new Integer(254), "ke"}, //Kenya
            {new Integer(255), "tz"}, //Tanzania
            {new Integer(256), "ug"}, //Uganda
            {new Integer(257), "bi"}, //Burundi
            {new Integer(258), "mz"}, //Mozambique
            {new Integer(260), "zm"}, //Zambia
            {new Integer(261), "mg"}, //Madagascar
        //        {new Integer(262),""}, //Reunion Island ???
            {new Integer(263), "zw"}, //Zimbabwe
            {new Integer(264), "na"}, //Namibia
            {new Integer(265), "mw"}, //Malawi
            {new Integer(266), "ls"}, //Lesotho
            {new Integer(267), "bw"}, //Botswana
            {new Integer(268), "sz"}, //Swaziland
            {new Integer(269), "yt"}, //Mayotte Island
            {new Integer(2691), "km"}, //Comoros
            {new Integer(27), "za"}, //South Africa
            {new Integer(290), "sh"}, //St. Helena
            {new Integer(291), "er"}, //Eritrea
            {new Integer(297), "aw"}, //Aruba
        //        {new Integer(298),""}, //Faeroe Islands ???
            {new Integer(299), "gl"}, //Greenland
            {new Integer(30), "gr"}, //Greece
            {new Integer(31), "nl"}, //Netherlands
            {new Integer(32), "be"}, //Belgium
            {new Integer(33), "fr"}, //France
            {new Integer(34), "es"}, //Spain
            {new Integer(350), "gi"}, //Gibraltar
            {new Integer(351), "pt"}, //Portugal
            {new Integer(352), "lu"}, //Luxembourg
            {new Integer(353), "ie"}, //Ireland
            {new Integer(354), "is"}, //Iceland
            {new Integer(355), "al"}, //Albania
            {new Integer(356), "mt"}, //Malta
            {new Integer(357), "cy"}, //Cyprus
            {new Integer(358), "fi"}, //Finland
            {new Integer(359), "bg"}, //Bulgaria
            {new Integer(36), "hu"}, //Hungary
            {new Integer(370), "lt"}, //Lithuania
            {new Integer(371), "lv"}, //Latvia
            {new Integer(372), "ee"}, //Estonia
            {new Integer(373), "md"}, //Moldova, Republic of
            {new Integer(374), "am"}, //Armenia
            {new Integer(375), "by"}, //Belarus
            {new Integer(376), "ad"}, //Andorra
            {new Integer(377), "mc"}, //Monaco
            {new Integer(378), "sm"}, //San Marino
            {new Integer(379), "va"}, //Vatican City
            {new Integer(380), "ua"}, //Ukraine
        //        {new Integer(381),""}, //Yugoslavia ???
            {new Integer(3811), "cs"}, //Yugoslavia - Serbia
            {new Integer(382), "cs"}, //Yugoslavia - Montenegro
            {new Integer(385), "hr"}, //Croatia
            {new Integer(386), "si"}, //Slovenia
            {new Integer(387), "ba"}, //Bosnia & Herzegovina
            {new Integer(389), "mk"}, //Macedonia (F.Y.R.O.M.)
            {new Integer(39), "it"}, //Italy
            {new Integer(40), "ro"}, //Romania
            {new Integer(41), "ch"}, //Switzerland
            {new Integer(4101), "li"}, //Liechtenstein
            {new Integer(42), "cz"}, //Czech Republic
            {new Integer(4201), "sk"}, //Slovakia
            {new Integer(43), "at"}, //Austria
            {new Integer(44), "gb"}, //United Kingdom
        //        {new Integer(441),""}, //Wales ???
        //        {new Integer(442),""}, //Scotland ???
            {new Integer(45), "dk"}, //Denmark
            {new Integer(46), "se"}, //Sweden
            {new Integer(47), "no"}, //Norway
            {new Integer(48), "pl"}, //Poland
            {new Integer(49), "de"}, //Germany
        //        {new Integer(500),""}, //Falkland Islands ???
            {new Integer(501), "bz"}, //Belize
            {new Integer(502), "gt"}, //Guatemala
            {new Integer(503), "sv"}, //El Salvador
            {new Integer(504), "hn"}, //Honduras
            {new Integer(505), "ni"}, //Nicaragua
            {new Integer(506), "cr"}, //Costa Rica
            {new Integer(507), "pa"}, //Panama
            {new Integer(508), "pm"}, //St. Pierre & Miquelon
            {new Integer(509), "ht"}, //Haiti
            {new Integer(51), "pe"}, //Peru
            {new Integer(52), "mx"}, //Mexico
            {new Integer(53), "cu"}, //Cuba
            {new Integer(54), "ar"}, //Argentina
            {new Integer(55), "br"}, //Brazil
            {new Integer(56), "cl"}, //Chile, Republic of
            {new Integer(57), "co"}, //Colombia
            {new Integer(58), "ve"}, //Venezuela
            {new Integer(590), "gp"}, //Guadeloupe
            {new Integer(5901), "an"}, //French Antilles
            {new Integer(5902), "an"}, //Antilles
            {new Integer(591), "bo"}, //Bolivia
            {new Integer(592), "gy"}, //Guyana
            {new Integer(593), "ec"}, //Ecuador
            {new Integer(594), "gy"}, //French Guyana
            {new Integer(595), "py"}, //Paraguay
            {new Integer(596), "mq"}, //Martinique
            {new Integer(597), "sr"}, //Suriname
            {new Integer(598), "uy"}, //Uruguay
            {new Integer(599), "an"}, //Netherlands Antilles
            {new Integer(60), "my"}, //Malaysia
            {new Integer(61), "au"}, //Australia
            {new Integer(6101), "cc"}, //Cocos-Keeling Islands
            {new Integer(6102), "cc"}, //Cocos (Keeling) Islands
            {new Integer(62), "id"}, //Indonesia
            {new Integer(63), "ph"}, //Philippines
            {new Integer(64), "nz"}, //New Zealand
            {new Integer(65), "sg"}, //Singapore
            {new Integer(66), "th"}, //Thailand
        //        {new Integer(670),""}, //Saipan Island ???
        //        {new Integer(6701),""}, //Rota Island  ???
        //        {new Integer(6702),""}, //Tinian Island ???
            {new Integer(671), "gu"}, //Guam, US Territory of
            {new Integer(672), "cx"}, //Christmas Island
            {new Integer(6722), "nf"}, //Norfolk Island
            {new Integer(673), "bn"}, //Brunei
            {new Integer(674), "nr"}, //Nauru
            {new Integer(675), "pg"}, //Papua New Guinea
            {new Integer(676), "to"}, //Tonga
            {new Integer(677), "sb"}, //Solomon Islands
            {new Integer(678), "vu"}, //Vanuatu
            {new Integer(679), "fj"}, //Fiji
            {new Integer(680), "pw"}, //Palau
            {new Integer(681), "wf"}, //Wallis & Futuna Islands
            {new Integer(682), "ck"}, //Cook Islands
            {new Integer(683), "nu"}, //Niue
            {new Integer(684), "as"}, //American Samoa
            {new Integer(685), "ws"}, //Western Samoa
            {new Integer(686), "ki"}, //Kiribati
            {new Integer(687), "nc"}, //New Caledonia
            {new Integer(688), "tv"}, //Tuvalu
            {new Integer(689), "pf"}, //French Polynesia
            {new Integer(690), "tk"}, //Tokelau
            {new Integer(691), "fm"}, //Micronesia, Federated States of
            {new Integer(692), "mh"}, //Marshall Islands
            {new Integer(7), "ru"}, //Russia
            {new Integer(705), "kz"}, //Kazakhstan
            {new Integer(706), "kg"}, //Kyrgyzstan
            {new Integer(708), "tj"}, //Tajikistan
            {new Integer(709), "tm"}, //Turkmenistan
            {new Integer(711), "uz"}, //Uzbekistan
            {new Integer(81), "jp"}, //Japan
            {new Integer(82), "kr"}, //Korea, South
            {new Integer(84), "vn"}, //Viet Nam
            {new Integer(850), "kp"}, //Korea, North
            {new Integer(852), "hk"}, //Hong Kong
            {new Integer(853), "mo"}, //Macau
            {new Integer(855), "kh"}, //Cambodia
            {new Integer(856), "la"}, //Laos
            {new Integer(86), "cn"}, //China
            {new Integer(880), "bd"}, //Bangladesh
            {new Integer(886), "tw"}, //Taiwan
            {new Integer(90), "tr"}, //Turkey
            {new Integer(91), "in"}, //India
            {new Integer(92), "pk"}, //Pakistan
            {new Integer(93), "af"}, //Afghanistan
            {new Integer(94), "lk"}, //Sri Lanka
            {new Integer(95), "mm"}, //Myanmar
            {new Integer(960), "mv"}, //Maldives
            {new Integer(961), "lb"}, //Lebanon
            {new Integer(962), "jo"}, //Jordan
            {new Integer(963), "sy"}, //Syrian Arab Republic
            {new Integer(964), "iq"}, //Iraq
            {new Integer(965), "kw"}, //Kuwait
            {new Integer(966), "sa"}, //Saudi Arabia
            {new Integer(967), "ye"}, //Yemen
            {new Integer(968), "om"}, //Oman
            {new Integer(971), "ae"}, //United Arabian Emirates
            {new Integer(972), "il"}, //Israel
            {new Integer(973), "bh"}, //Bahrain
            {new Integer(974), "qa"}, //Qatar
            {new Integer(975), "bt"}, //Bhutan
            {new Integer(976), "mn"}, //Mongolia
            {new Integer(977), "np"}, //Nepal
            {new Integer(98), "ir"}, //Iran (Islamic Republic of)
            {new Integer(994), "az"}, //Azerbaijan
            {new Integer(995), "ge"} //Georgia
//        {new Integer(9999),""}, //other

        };

    // the index in the array is the index stored in icq server
    // the values are the corresponding locales
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
}
