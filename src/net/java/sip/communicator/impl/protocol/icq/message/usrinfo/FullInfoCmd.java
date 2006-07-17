/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.usrinfo;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.tlv.*;

/**
 * Parses the incoming data for the requested user info.
 * The request is made by FullInfoRequest
 *
 * @author Damian Minkov
 */
public class FullInfoCmd
    extends SnacCommand
{
    private static final Logger logger = Logger.getLogger(FullInfoCmd.class);

    /**
     * As all the FullUserInfo comes in
     * sequences of 8 packets acording to the
     * requestID we keep the stored Info so far.
     */
    private static Hashtable retreivedInfo = new Hashtable();

    private boolean lastOfSequences = false;
    private int requestID = -1;

    // used when semding this command for changeg details stored on the server
    private String senderUin = null;
    private List changeDataTlvs = new LinkedList();
    public static final IcqType CMD_SET_FULLINFO = new IcqType(0x07D0, 0x0c3a);
    private static final int TYPE_ICQ_DATA = 0x0001;

    // String corresponding to type indexes
    private static ServerStoredDetails.GenderDetail[] genders =
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

    private static String[] occupations = new String[]{
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

    private String[] interestsCategories = new String[]{
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
    private static Hashtable countryIndexToLocaleString = new Hashtable();
    static
    {
//        countryIndexToLocaleString.put(new Integer(0),""); //not specified
        countryIndexToLocaleString.put(new Integer(1),"us"); //USA
        countryIndexToLocaleString.put(new Integer(101),"ai"); //Anguilla
        countryIndexToLocaleString.put(new Integer(102),"ag"); //Antigua
        countryIndexToLocaleString.put(new Integer(1021),"ag"); //Antigua & Barbuda
        countryIndexToLocaleString.put(new Integer(103),"bs"); //Bahamas
        countryIndexToLocaleString.put(new Integer(104),"bb"); //Barbados
        countryIndexToLocaleString.put(new Integer(105),"bm"); //Bermuda
        countryIndexToLocaleString.put(new Integer(106),"vg"); //British Virgin Islands
        countryIndexToLocaleString.put(new Integer(107),"ca"); //Canada
        countryIndexToLocaleString.put(new Integer(108),"ky"); //Cayman Islands
        countryIndexToLocaleString.put(new Integer(109),"dm"); //Dominica
        countryIndexToLocaleString.put(new Integer(110),"do"); //Dominican Republic
        countryIndexToLocaleString.put(new Integer(111),"gd"); //Grenada
        countryIndexToLocaleString.put(new Integer(112),"jm"); //Jamaica
        countryIndexToLocaleString.put(new Integer(113),"ms"); //Montserrat
        countryIndexToLocaleString.put(new Integer(114),"kn"); //Nevis
        countryIndexToLocaleString.put(new Integer(1141),"kn"); //Saint Kitts and Nevis
        countryIndexToLocaleString.put(new Integer(115),"kn"); //St. Kitts
        countryIndexToLocaleString.put(new Integer(116),"vc"); //St. Vincent & the Grenadines
        countryIndexToLocaleString.put(new Integer(117),"tt"); //Trinidad & Tobago
        countryIndexToLocaleString.put(new Integer(118),"tc"); //Turks & Caicos Islands
        countryIndexToLocaleString.put(new Integer(120),"ag"); //Barbuda
        countryIndexToLocaleString.put(new Integer(121),"pr"); //Puerto Rico
        countryIndexToLocaleString.put(new Integer(122),"lc"); //Saint Lucia
        countryIndexToLocaleString.put(new Integer(123),"vi"); //Virgin Islands (USA)
        countryIndexToLocaleString.put(new Integer(178),"es"); //Canary Islands ???
        countryIndexToLocaleString.put(new Integer(20),"eg"); //Egypt
        countryIndexToLocaleString.put(new Integer(212),"ma"); //Morocco
        countryIndexToLocaleString.put(new Integer(213),"dz"); //Algeria
        countryIndexToLocaleString.put(new Integer(216),"tn"); //Tunisia
        countryIndexToLocaleString.put(new Integer(218),"ly"); //Libyan Arab Jamahiriya
        countryIndexToLocaleString.put(new Integer(220),"gm"); //Gambia
        countryIndexToLocaleString.put(new Integer(221),"sn"); //Senegal
        countryIndexToLocaleString.put(new Integer(222),"mr"); //Mauritania
        countryIndexToLocaleString.put(new Integer(223),"ml"); //Mali
        countryIndexToLocaleString.put(new Integer(224),"pg"); //Guinea
        countryIndexToLocaleString.put(new Integer(225),"ci"); //Cote d'Ivoire
        countryIndexToLocaleString.put(new Integer(226),"bf"); //Burkina Faso
        countryIndexToLocaleString.put(new Integer(227),"ne"); //Niger
        countryIndexToLocaleString.put(new Integer(228),"tg"); //Togo
        countryIndexToLocaleString.put(new Integer(229),"bj"); //Benin
        countryIndexToLocaleString.put(new Integer(230),"mu"); //Mauritius
        countryIndexToLocaleString.put(new Integer(231),"lr"); //Liberia
        countryIndexToLocaleString.put(new Integer(232),"sl"); //Sierra Leone
        countryIndexToLocaleString.put(new Integer(233),"gh"); //Ghana
        countryIndexToLocaleString.put(new Integer(234),"ng"); //Nigeria
        countryIndexToLocaleString.put(new Integer(235),"td"); //Chad
        countryIndexToLocaleString.put(new Integer(236),"cf"); //Central African Republic
        countryIndexToLocaleString.put(new Integer(237),"cm"); //Cameroon
        countryIndexToLocaleString.put(new Integer(238),"cv"); //Cape Verde Islands
        countryIndexToLocaleString.put(new Integer(239),"st"); //Sao Tome & Principe
        countryIndexToLocaleString.put(new Integer(240),"gq"); //Equatorial Guinea
        countryIndexToLocaleString.put(new Integer(241),"ga"); //Gabon
        countryIndexToLocaleString.put(new Integer(242),"cg"); //Congo, (Rep. of the)
        countryIndexToLocaleString.put(new Integer(243),"cd"); //Congo, Democratic Republic of
        countryIndexToLocaleString.put(new Integer(244),"ao"); //Angola
        countryIndexToLocaleString.put(new Integer(245),"gw"); //Guinea-Bissau
//        countryIndexToLocaleString.put(new Integer(246),""); //Diego Garcia ???
//        countryIndexToLocaleString.put(new Integer(247),""); //Ascension Island ???
        countryIndexToLocaleString.put(new Integer(248),"sc"); //Seychelles
        countryIndexToLocaleString.put(new Integer(249),"sd"); //Sudan
        countryIndexToLocaleString.put(new Integer(250),"rw"); //Rwanda
        countryIndexToLocaleString.put(new Integer(251),"et"); //Ethiopia
        countryIndexToLocaleString.put(new Integer(252),"so"); //Somalia
        countryIndexToLocaleString.put(new Integer(253),"dj"); //Djibouti
        countryIndexToLocaleString.put(new Integer(254),"ke"); //Kenya
        countryIndexToLocaleString.put(new Integer(255),"tz"); //Tanzania
        countryIndexToLocaleString.put(new Integer(256),"ug"); //Uganda
        countryIndexToLocaleString.put(new Integer(257),"bi"); //Burundi
        countryIndexToLocaleString.put(new Integer(258),"mz"); //Mozambique
        countryIndexToLocaleString.put(new Integer(260),"zm"); //Zambia
        countryIndexToLocaleString.put(new Integer(261),"mg"); //Madagascar
//        countryIndexToLocaleString.put(new Integer(262),""); //Reunion Island ???
        countryIndexToLocaleString.put(new Integer(263),"zw"); //Zimbabwe
        countryIndexToLocaleString.put(new Integer(264),"na"); //Namibia
        countryIndexToLocaleString.put(new Integer(265),"mw"); //Malawi
        countryIndexToLocaleString.put(new Integer(266),"ls"); //Lesotho
        countryIndexToLocaleString.put(new Integer(267),"bw"); //Botswana
        countryIndexToLocaleString.put(new Integer(268),"sz"); //Swaziland
        countryIndexToLocaleString.put(new Integer(269),"yt"); //Mayotte Island
        countryIndexToLocaleString.put(new Integer(2691),"km"); //Comoros
        countryIndexToLocaleString.put(new Integer(27),"za"); //South Africa
        countryIndexToLocaleString.put(new Integer(290),"sh"); //St. Helena
        countryIndexToLocaleString.put(new Integer(291),"er"); //Eritrea
        countryIndexToLocaleString.put(new Integer(297),"aw"); //Aruba
//        countryIndexToLocaleString.put(new Integer(298),""); //Faeroe Islands ???
        countryIndexToLocaleString.put(new Integer(299),"gl"); //Greenland
        countryIndexToLocaleString.put(new Integer(30),"gr"); //Greece
        countryIndexToLocaleString.put(new Integer(31),"nl"); //Netherlands
        countryIndexToLocaleString.put(new Integer(32),"be"); //Belgium
        countryIndexToLocaleString.put(new Integer(33),"fr"); //France
        countryIndexToLocaleString.put(new Integer(34),"es"); //Spain
        countryIndexToLocaleString.put(new Integer(350),"gi"); //Gibraltar
        countryIndexToLocaleString.put(new Integer(351),"pt"); //Portugal
        countryIndexToLocaleString.put(new Integer(352),"lu"); //Luxembourg
        countryIndexToLocaleString.put(new Integer(353),"ie"); //Ireland
        countryIndexToLocaleString.put(new Integer(354),"is"); //Iceland
        countryIndexToLocaleString.put(new Integer(355),"al"); //Albania
        countryIndexToLocaleString.put(new Integer(356),"mt"); //Malta
        countryIndexToLocaleString.put(new Integer(357),"cy"); //Cyprus
        countryIndexToLocaleString.put(new Integer(358),"fi"); //Finland
        countryIndexToLocaleString.put(new Integer(359),"bg"); //Bulgaria
        countryIndexToLocaleString.put(new Integer(36),"hu"); //Hungary
        countryIndexToLocaleString.put(new Integer(370),"lt"); //Lithuania
        countryIndexToLocaleString.put(new Integer(371),"lv"); //Latvia
        countryIndexToLocaleString.put(new Integer(372),"ee"); //Estonia
        countryIndexToLocaleString.put(new Integer(373),"md"); //Moldova, Republic of
        countryIndexToLocaleString.put(new Integer(374),"am"); //Armenia
        countryIndexToLocaleString.put(new Integer(375),"by"); //Belarus
        countryIndexToLocaleString.put(new Integer(376),"ad"); //Andorra
        countryIndexToLocaleString.put(new Integer(377),"mc"); //Monaco
        countryIndexToLocaleString.put(new Integer(378),"sm"); //San Marino
        countryIndexToLocaleString.put(new Integer(379),"va"); //Vatican City
        countryIndexToLocaleString.put(new Integer(380),"ua"); //Ukraine
//        countryIndexToLocaleString.put(new Integer(381),""); //Yugoslavia ???
        countryIndexToLocaleString.put(new Integer(3811),"cs"); //Yugoslavia - Serbia
        countryIndexToLocaleString.put(new Integer(382),"cs"); //Yugoslavia - Montenegro
        countryIndexToLocaleString.put(new Integer(385),"hr"); //Croatia
        countryIndexToLocaleString.put(new Integer(386),"si"); //Slovenia
        countryIndexToLocaleString.put(new Integer(387),"ba"); //Bosnia & Herzegovina
        countryIndexToLocaleString.put(new Integer(389),"mk"); //Macedonia (F.Y.R.O.M.)
        countryIndexToLocaleString.put(new Integer(39),"it"); //Italy
        countryIndexToLocaleString.put(new Integer(40),"ro"); //Romania
        countryIndexToLocaleString.put(new Integer(41),"ch"); //Switzerland
        countryIndexToLocaleString.put(new Integer(4101),"li"); //Liechtenstein
        countryIndexToLocaleString.put(new Integer(42),"cz"); //Czech Republic
        countryIndexToLocaleString.put(new Integer(4201),"sk"); //Slovakia
        countryIndexToLocaleString.put(new Integer(43),"at"); //Austria
        countryIndexToLocaleString.put(new Integer(44),"gb"); //United Kingdom
//        countryIndexToLocaleString.put(new Integer(441),""); //Wales ???
//        countryIndexToLocaleString.put(new Integer(442),""); //Scotland ???
        countryIndexToLocaleString.put(new Integer(45),"dk"); //Denmark
        countryIndexToLocaleString.put(new Integer(46),"se"); //Sweden
        countryIndexToLocaleString.put(new Integer(47),"no"); //Norway
        countryIndexToLocaleString.put(new Integer(48),"pl"); //Poland
        countryIndexToLocaleString.put(new Integer(49),"de"); //Germany
//        countryIndexToLocaleString.put(new Integer(500),""); //Falkland Islands ???
        countryIndexToLocaleString.put(new Integer(501),"bz"); //Belize
        countryIndexToLocaleString.put(new Integer(502),"gt"); //Guatemala
        countryIndexToLocaleString.put(new Integer(503),"sv"); //El Salvador
        countryIndexToLocaleString.put(new Integer(504),"hn"); //Honduras
        countryIndexToLocaleString.put(new Integer(505),"ni"); //Nicaragua
        countryIndexToLocaleString.put(new Integer(506),"cr"); //Costa Rica
        countryIndexToLocaleString.put(new Integer(507),"pa"); //Panama
        countryIndexToLocaleString.put(new Integer(508),"pm"); //St. Pierre & Miquelon
        countryIndexToLocaleString.put(new Integer(509),"ht"); //Haiti
        countryIndexToLocaleString.put(new Integer(51),"pe"); //Peru
        countryIndexToLocaleString.put(new Integer(52),"mx"); //Mexico
        countryIndexToLocaleString.put(new Integer(53),"cu"); //Cuba
        countryIndexToLocaleString.put(new Integer(54),"ar"); //Argentina
        countryIndexToLocaleString.put(new Integer(55),"br"); //Brazil
        countryIndexToLocaleString.put(new Integer(56),"cl"); //Chile, Republic of
        countryIndexToLocaleString.put(new Integer(57),"co"); //Colombia
        countryIndexToLocaleString.put(new Integer(58),"ve"); //Venezuela
        countryIndexToLocaleString.put(new Integer(590),"gp"); //Guadeloupe
        countryIndexToLocaleString.put(new Integer(5901),"an"); //French Antilles
        countryIndexToLocaleString.put(new Integer(5902),"an"); //Antilles
        countryIndexToLocaleString.put(new Integer(591),"bo"); //Bolivia
        countryIndexToLocaleString.put(new Integer(592),"gy"); //Guyana
        countryIndexToLocaleString.put(new Integer(593),"ec"); //Ecuador
        countryIndexToLocaleString.put(new Integer(594),"gy"); //French Guyana
        countryIndexToLocaleString.put(new Integer(595),"py"); //Paraguay
        countryIndexToLocaleString.put(new Integer(596),"mq"); //Martinique
        countryIndexToLocaleString.put(new Integer(597),"sr"); //Suriname
        countryIndexToLocaleString.put(new Integer(598),"uy"); //Uruguay
        countryIndexToLocaleString.put(new Integer(599),"an"); //Netherlands Antilles
        countryIndexToLocaleString.put(new Integer(60),"my"); //Malaysia
        countryIndexToLocaleString.put(new Integer(61),"au"); //Australia
        countryIndexToLocaleString.put(new Integer(6101),"cc"); //Cocos-Keeling Islands
        countryIndexToLocaleString.put(new Integer(6102),"cc"); //Cocos (Keeling) Islands
        countryIndexToLocaleString.put(new Integer(62),"id"); //Indonesia
        countryIndexToLocaleString.put(new Integer(63),"ph"); //Philippines
        countryIndexToLocaleString.put(new Integer(64),"nz"); //New Zealand
        countryIndexToLocaleString.put(new Integer(65),"sg"); //Singapore
        countryIndexToLocaleString.put(new Integer(66),"th"); //Thailand
//        countryIndexToLocaleString.put(new Integer(670),""); //Saipan Island ???
//        countryIndexToLocaleString.put(new Integer(6701),""); //Rota Island  ???
//        countryIndexToLocaleString.put(new Integer(6702),""); //Tinian Island ???
        countryIndexToLocaleString.put(new Integer(671),"gu"); //Guam, US Territory of
        countryIndexToLocaleString.put(new Integer(672),"cx"); //Christmas Island
        countryIndexToLocaleString.put(new Integer(6722),"nf"); //Norfolk Island
        countryIndexToLocaleString.put(new Integer(673),"bn"); //Brunei
        countryIndexToLocaleString.put(new Integer(674),"nr"); //Nauru
        countryIndexToLocaleString.put(new Integer(675),"pg"); //Papua New Guinea
        countryIndexToLocaleString.put(new Integer(676),"to"); //Tonga
        countryIndexToLocaleString.put(new Integer(677),"sb"); //Solomon Islands
        countryIndexToLocaleString.put(new Integer(678),"vu"); //Vanuatu
        countryIndexToLocaleString.put(new Integer(679),"fj"); //Fiji
        countryIndexToLocaleString.put(new Integer(680),"pw"); //Palau
        countryIndexToLocaleString.put(new Integer(681),"wf"); //Wallis & Futuna Islands
        countryIndexToLocaleString.put(new Integer(682),"ck"); //Cook Islands
        countryIndexToLocaleString.put(new Integer(683),"nu"); //Niue
        countryIndexToLocaleString.put(new Integer(684),"as"); //American Samoa
        countryIndexToLocaleString.put(new Integer(685),"ws"); //Western Samoa
        countryIndexToLocaleString.put(new Integer(686),"ki"); //Kiribati
        countryIndexToLocaleString.put(new Integer(687),"nc"); //New Caledonia
        countryIndexToLocaleString.put(new Integer(688),"tv"); //Tuvalu
        countryIndexToLocaleString.put(new Integer(689),"pf"); //French Polynesia
        countryIndexToLocaleString.put(new Integer(690),"tk"); //Tokelau
        countryIndexToLocaleString.put(new Integer(691),"fm"); //Micronesia, Federated States of
        countryIndexToLocaleString.put(new Integer(692),"mh"); //Marshall Islands
        countryIndexToLocaleString.put(new Integer(7),"ru"); //Russia
        countryIndexToLocaleString.put(new Integer(705),"kz"); //Kazakhstan
        countryIndexToLocaleString.put(new Integer(706),"kg"); //Kyrgyzstan
        countryIndexToLocaleString.put(new Integer(708),"tj"); //Tajikistan
        countryIndexToLocaleString.put(new Integer(709),"tm"); //Turkmenistan
        countryIndexToLocaleString.put(new Integer(711),"uz"); //Uzbekistan
        countryIndexToLocaleString.put(new Integer(81),"jp"); //Japan
        countryIndexToLocaleString.put(new Integer(82),"kr"); //Korea, South
        countryIndexToLocaleString.put(new Integer(84),"vn"); //Viet Nam
        countryIndexToLocaleString.put(new Integer(850),"kp"); //Korea, North
        countryIndexToLocaleString.put(new Integer(852),"hk"); //Hong Kong
        countryIndexToLocaleString.put(new Integer(853),"mo"); //Macau
        countryIndexToLocaleString.put(new Integer(855),"kh"); //Cambodia
        countryIndexToLocaleString.put(new Integer(856),"la"); //Laos
        countryIndexToLocaleString.put(new Integer(86),"cn"); //China
        countryIndexToLocaleString.put(new Integer(880),"bd"); //Bangladesh
        countryIndexToLocaleString.put(new Integer(886),"tw"); //Taiwan
        countryIndexToLocaleString.put(new Integer(90),"tr"); //Turkey
        countryIndexToLocaleString.put(new Integer(91),"in"); //India
        countryIndexToLocaleString.put(new Integer(92),"pk"); //Pakistan
        countryIndexToLocaleString.put(new Integer(93),"af"); //Afghanistan
        countryIndexToLocaleString.put(new Integer(94),"lk"); //Sri Lanka
        countryIndexToLocaleString.put(new Integer(95),"mm"); //Myanmar
        countryIndexToLocaleString.put(new Integer(960),"mv"); //Maldives
        countryIndexToLocaleString.put(new Integer(961),"lb"); //Lebanon
        countryIndexToLocaleString.put(new Integer(962),"jo"); //Jordan
        countryIndexToLocaleString.put(new Integer(963),"sy"); //Syrian Arab Republic
        countryIndexToLocaleString.put(new Integer(964),"iq"); //Iraq
        countryIndexToLocaleString.put(new Integer(965),"kw"); //Kuwait
        countryIndexToLocaleString.put(new Integer(966),"sa"); //Saudi Arabia
        countryIndexToLocaleString.put(new Integer(967),"ye"); //Yemen
        countryIndexToLocaleString.put(new Integer(968),"om"); //Oman
        countryIndexToLocaleString.put(new Integer(971),"ae"); //United Arabian Emirates
        countryIndexToLocaleString.put(new Integer(972),"il"); //Israel
        countryIndexToLocaleString.put(new Integer(973),"bh"); //Bahrain
        countryIndexToLocaleString.put(new Integer(974),"qa"); //Qatar
        countryIndexToLocaleString.put(new Integer(975),"bt"); //Bhutan
        countryIndexToLocaleString.put(new Integer(976),"mn"); //Mongolia
        countryIndexToLocaleString.put(new Integer(977),"np"); //Nepal
        countryIndexToLocaleString.put(new Integer(98),"ir"); //Iran (Islamic Republic of)
        countryIndexToLocaleString.put(new Integer(994),"az"); //Azerbaijan
        countryIndexToLocaleString.put(new Integer(995),"ge"); //Georgia
//        countryIndexToLocaleString.put(new Integer(9999),""); //other
    }

    // the value is int[]
    // int[0] - the max count of this detail
    // the Tlv type for this detail
    public static Hashtable supportedTypes = new Hashtable();
    static {
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
        supportedTypes.put(FullInfoCmd.OriginCityDetail.class,          new int[]{1, 0x0320});
        supportedTypes.put(FullInfoCmd.OriginProvinceDetail.class,      new int[]{1, 0x032A});
        supportedTypes.put(FullInfoCmd.OriginCountryDetail.class,       new int[]{1, 0x0334});
        supportedTypes.put(ServerStoredDetails.WorkCityDetail.class,    new int[]{1, 0x029E});
        supportedTypes.put(ServerStoredDetails.WorkProvinceDetail.class, new int[]{1, 0x02A8});
        supportedTypes.put(ServerStoredDetails.WorkPhoneDetail.class,   new int[]{1, 0x02C6});
        supportedTypes.put(FullInfoCmd.WorkFaxDetail.class,             new int[]{1, 0x02D0});
        supportedTypes.put(ServerStoredDetails.WorkAddressDetail.class, new int[]{1, 0x0294});
        supportedTypes.put(ServerStoredDetails.WorkPostalCodeDetail.class, new int[]{1, 0x02BC});
        supportedTypes.put(ServerStoredDetails.WorkCountryDetail.class, new int[]{1, 0x02B2});
        supportedTypes.put(ServerStoredDetails.WorkOrganizationNameDetail.class, new int[]{1, 0x01AE});
        supportedTypes.put(FullInfoCmd.WorkDepartmentNameDetail.class,  new int[]{1, 0x01B8});
        supportedTypes.put(FullInfoCmd.WorkPositionNameDetail.class,    new int[]{1, 0x01C2});
        supportedTypes.put(FullInfoCmd.WorkOcupationDetail.class,       new int[]{1, 0x01CC});
        supportedTypes.put(ServerStoredDetails.WorkPageDetail.class,    new int[]{1, 0x02DA});
        supportedTypes.put(FullInfoCmd.NotesDetail.class,               new int[]{1, 0x0258});
        supportedTypes.put(FullInfoCmd.InterestDetail.class,            new int[]{10, 0x01EA});
        supportedTypes.put(ServerStoredDetails.TimeZoneDetail.class,    new int[]{1, 0x0316});
    }

    /**
     * Construct incoming command from server
     * @param packet FromIcqCmd
     */
    public FullInfoCmd(FromIcqCmd packet)
    {
        super(21, 3);

        this.requestID = packet.getId();

        switch (packet.getType().getSecondary())
        {
            case AbstractIcqCmd.USER_INFORMATION_BASIC
            :
                readBasicUserInfo(packet.getIcqData(), packet.getId());
                break;
            case AbstractIcqCmd.USER_INFORMATION_MORE
            :
                readMoreUserInfo(packet.getIcqData(), packet.getId());
                break;
            case AbstractIcqCmd.USER_INFORMATION_EXTENDED_EMAIL
            :
                readEmailUserInfo(packet.getIcqData(), packet.getId());
                break;
            case AbstractIcqCmd.USER_INFORMATION_HOMEPAGE_CATEGORY
            :
                readHomePageUserInfo(packet.getIcqData(), packet.getId());
                break;
            case AbstractIcqCmd.USER_INFORMATION_WORK
            :
                readWorkUserInfo(packet.getIcqData(), packet.getId());
                break;
            case AbstractIcqCmd.USER_INFORMATION_ABOUT
            :
                readUserAboutInfo(packet.getIcqData(), packet.getId());
                break;
            case AbstractIcqCmd.USER_INFORMATION_INTERESTS
            :
                readInterestsUserInfo(packet.getIcqData(), packet.getId());
                break;
            case AbstractIcqCmd.USER_INFORMATION_AFFILATIONS
            :
                readAffilationsUserInfo(packet.getIcqData(), packet.getId());
                break;
        }
    }

    /**
     * Constructs command send to server
     * @param senderUin String the uin of the sender
     * @param changedData List the data to be changed
     * @param toBeCleared List the data to be cleared,
     *      if no such data null can be passed
     */
    public FullInfoCmd(String senderUin, List changedData, List toBeCleared)
    {
        super(21,2);
        this.senderUin = senderUin;

        // if toBeCleared == null. make it empty one
        if(toBeCleared == null)
            toBeCleared = new ArrayList();

        // fix the spoken languages must be 3
        int lastSpokenIndex = -1;
        int countOfLanguages = 0;
        boolean isLanguageFound = false;
        for(int i = 0; i < changedData.size(); i++)
        {
            Object item = changedData.get(i);
            if(item instanceof ServerStoredDetails.SpokenLanguageDetail)
            {
                isLanguageFound = true;
                lastSpokenIndex = i;
                countOfLanguages++;
            }
        }

        if(isLanguageFound)
        {
            for (int i = countOfLanguages; i < 3; i++)
            {
                lastSpokenIndex++;
                changedData.add(lastSpokenIndex,
                                new ServerStoredDetails.SpokenLanguageDetail(null));
            }
        }

        Iterator iter = changedData.iterator();
        while(iter.hasNext())
        {
            ServerStoredDetails.GenericDetail item =
                (ServerStoredDetails.GenericDetail)iter.next();

            if(toBeCleared.contains(item))
                changeDataTlvs.add(getClearTlv(item));
            else
                changeDataTlvs.add(getTlvForChange(item));
        }
    }

    /**
     * Data writen to the sever corresponding the changes passed to the constructor
     * @param out OutputStream
     * @throws IOException
     */
    public void writeData(OutputStream out) throws IOException
    {
        ByteArrayOutputStream icqout = new ByteArrayOutputStream();

        ByteArrayOutputStream icqDataOut = new ByteArrayOutputStream();

        // write tlvs with data here
        Iterator iter = changeDataTlvs.iterator();
        while(iter.hasNext())
        {
            DetailTlv item = (DetailTlv)iter.next();
            item.write(icqDataOut);
        }

        int hdrlen = 10; // The expected header length, not counting the length field itself.
        int primary = CMD_SET_FULLINFO.getPrimary();
        int secondary = CMD_SET_FULLINFO.getSecondary();

        long icqUINlong = Long.parseLong(senderUin);

        int length = hdrlen + icqDataOut.size();

        LEBinaryTools.writeUShort(icqout, length);
        LEBinaryTools.writeUInt(icqout, icqUINlong);
        LEBinaryTools.writeUShort(icqout, primary);
        LEBinaryTools.writeUShort(icqout, 0x0002); // the sequence

        LEBinaryTools.writeUShort(icqout, secondary);

        icqDataOut.writeTo(icqout);

        new Tlv(TYPE_ICQ_DATA, ByteBlock.wrap(icqout.toByteArray())).write(out);
    }

    /**
     * Converts ServerStoredDetails to Tlv which later is converted to bytes
     * and send to server
     * @param detail GenericDetail the detail
     * @return DetailTlv
     */
    private DetailTlv getTlvForChange(ServerStoredDetails.GenericDetail detail)
    {
        int typeOfDetail = ((int[])supportedTypes.get(detail.getClass()))[1];

        DetailTlv result = new DetailTlv(typeOfDetail);

        switch(typeOfDetail)
        {
            case 0x01A4 : //CountryDetail
            case 0x0334 : //OriginCountryDetail
            case 0x02B2 : //WorkCountryDetail
                result.writeUShort(getCountryCode(((ServerStoredDetails.LocaleDetail)detail).getLocale()));break;

            case 0x0186 : //SpokenLanguageDetail
                writeLanguageCode((ServerStoredDetails.LocaleDetail)detail, result);break;
            case 0x017C : //GenderDetail
                writeGenderCode((ServerStoredDetails.GenderDetail)detail, result);break;
            case 0x023A : //BirthDateDetail
                writeCalendarCode((ServerStoredDetails.CalendarDetail)detail, result);break;
            case 0x01CC : //WorkOcupationDetail
                writeOccupationCode((WorkOcupationDetail)detail, result);break;
            case 0x01EA : //InterestDetail
                writeInterestCode((InterestDetail)detail, result);break;
            default :
                writeGenericDetail(detail, result);
        }

        return result;
    }

    /**
     * Correspondig the type of ServerStoredDetails returns empty Tlv or Tlv
     * with default value
     * @param detail GenericDetail
     * @return DetailTlv
     */
    private DetailTlv getClearTlv(ServerStoredDetails.GenericDetail detail)
    {
        int typeOfDetail = ((int[])supportedTypes.get(detail.getClass()))[1];

        DetailTlv result = new DetailTlv(((int[])supportedTypes.get(detail.getClass()))[1]);

        switch(typeOfDetail)
        {
            case 0x01A4 : //CountryDetail
            case 0x0334 : //OriginCountryDetail
            case 0x02B2 : //WorkCountryDetail
                result.writeUShort(0);break;

            case 0x0186 : //SpokenLanguageDetail
                logger.trace("write lang 0");
                result.writeUShort(0);
            case 0x017C : //GenderDetail
                result.writeUByte(0);
            case 0x023A : //BirthDateDetail
                result.writeUShort(0);
                result.writeUShort(0);
                result.writeUShort(0);
            case 0x01CC : //WorkOcupationDetail
                result.writeUShort(0);
            case 0x01EA : //InterestDetail
                result.writeUShort(0);
                result.writeString("");
            case 0x0316 :
                result.writeUByte(0);
            default :
                result.writeString("");
        }

        return result;
    }

    /**
     * Writes the corresponding index for Language from ServerStoredDetails to the tlv
     * @param detail LocaleDetail
     * @param tlv DetailTlv
     */
    private void writeLanguageCode(ServerStoredDetails.LocaleDetail detail, DetailTlv tlv)
    {
        Locale newLang = detail.getLocale();
        if(newLang == null)
        {
            // this indicates that we must set language as not specified
            tlv.writeUShort(0);
            logger.trace("write lang 0");
        }
        else
        {
            for (int i = 1; i < spokenLanguages.length; i++)
            {
                // indicating that language is not set
                if (getSpokenLanguage(i).equals(newLang))
                {
                    logger.trace("write lang " + i);
                    tlv.writeUShort(i);
                    return;
                }
            }
        }
    }

    /**
     * Writes the corresponding index for Gender from ServerStoredDetails to the tlv
     * @param detail GenderDetail
     * @param tlv DetailTlv
     */
    private void writeGenderCode(ServerStoredDetails.GenderDetail detail, DetailTlv tlv)
    {
        int gender = 0;

        if(detail.equals(ServerStoredDetails.GenderDetail.FEMALE))
            gender = 1;
        else if(detail.equals(ServerStoredDetails.GenderDetail.MALE))
            gender = 2;

        tlv.writeUByte(gender);
    }

    /**
     * Writes the corresponding index for Calendar(BirthDate) from ServerStoredDetails to the tlv
     * @param detail CalendarDetail
     * @param tlv DetailTlv
     */
    private void writeCalendarCode(ServerStoredDetails.CalendarDetail detail, DetailTlv tlv)
    {
        Calendar calendar = detail.getCalendar();

        tlv.writeUShort(calendar.get(Calendar.YEAR));
        tlv.writeUShort(calendar.get(Calendar.MONTH));
        tlv.writeUShort(calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Writes the corresponding index for Occupation from ServerStoredDetails to the tlv
     * @param detail WorkOcupationDetail
     * @param tlv DetailTlv
     */
    private void writeOccupationCode(WorkOcupationDetail detail, DetailTlv tlv)
    {
        for(int i = 0; i < occupations.length; i++)
        {
            if(occupations[i].equals(detail.getDetailValue()))
                tlv.writeUShort(i);
        }
    }

    /**
     * Writes the corresponding index for Interests from ServerStoredDetails to the tlv
     * @param detail InterestDetail
     * @param tlv DetailTlv
     */
    private void writeInterestCode(InterestDetail detail, DetailTlv tlv)
    {
        String category = detail.getCategory();
        int categoryInt = 0;
        for(int i = 0; i < interestsCategories.length; i++)
        {
            if(interestsCategories[i].equals(category))
            {
                if(i != 0)
                    categoryInt = i + 99;
                else
                    categoryInt = 0;

                break;
            }
        }

        tlv.writeUShort(categoryInt);
        tlv.writeString(detail.getInterest());
    }

    /**
     * Writes the corresponding value for ServerStoredDetails to the tlv
     * @param detail GenericDetail
     * @param tlv DetailTlv
     */
    private void writeGenericDetail(ServerStoredDetails.GenericDetail detail,  DetailTlv tlv)
    {
        if(detail instanceof ServerStoredDetails.StringDetail)
        {
            tlv.writeString(((ServerStoredDetails.StringDetail)detail).getString());
        }
        else
            if(detail instanceof ServerStoredDetails.TimeZoneDetail)
            {
                int offset = ((ServerStoredDetails.TimeZoneDetail)detail).
                    getTimeZone().getRawOffset()/(60*60*1000);
                tlv.writeUByte(offset);
            }
    }

    /**
     * Method for parsing incoming data
     * Read data in BasicUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readBasicUserInfo(ByteBlock block, int requestID)
    {
        List infoData = getInfoForRequest(requestID);

        // sequence of 11 String fields
        String bscInfo[] = new String[11];
        int offset = readStrings(block, bscInfo, 1);

        int homeCountryCode = LEBinaryTools.getUShort(block, offset);
        offset += 2;
        Locale countryCodeLocale = getCountry(homeCountryCode);
        if(countryCodeLocale != null)
            infoData.add(new ServerStoredDetails.CountryDetail(countryCodeLocale));

        // the following are not used
//        short GMT_Offset = LEBinaryTools.getUByte(block, offset);
//        offset++;
//        short authFlag = LEBinaryTools.getUByte(block, offset);
//        offset++;
//        short webAwareFlag = LEBinaryTools.getUByte(block, offset);
//        offset++;
//        short directConnectionPermissionsFlag = LEBinaryTools.getUByte(block, offset);
//        offset++;
//        short publishPrimaryEmailFlag = LEBinaryTools.getUByte(block, offset);
//        offset++;

        // everything is read lets store it
        if(bscInfo[0] != null)
            infoData.add(new ServerStoredDetails.NicknameDetail(bscInfo[0]));
        if(bscInfo[1] != null)
            infoData.add(new ServerStoredDetails.FirstNameDetail(bscInfo[1]));
        if(bscInfo[2] != null)
            infoData.add(new ServerStoredDetails.LastNameDetail(bscInfo[2]));
        if(bscInfo[3] != null)
            infoData.add(new ServerStoredDetails.EmailAddressDetail(bscInfo[3]));
        if(bscInfo[4] != null)
            infoData.add(new ServerStoredDetails.CityDetail(bscInfo[4]));
        if(bscInfo[5] != null)
            infoData.add(new ServerStoredDetails.ProvinceDetail(bscInfo[5]));
        if(bscInfo[6] != null)
            infoData.add(new ServerStoredDetails.PhoneNumberDetail(bscInfo[6]));
        if(bscInfo[7] != null)
            infoData.add(new ServerStoredDetails.FaxDetail(bscInfo[7]));
        if(bscInfo[8] != null)
            infoData.add(new ServerStoredDetails.AddressDetail(bscInfo[8]));
        if(bscInfo[9] != null)
            infoData.add(new ServerStoredDetails.MobilePhoneDetail(bscInfo[9]));
        if(bscInfo[10] != null)
            infoData.add(new ServerStoredDetails.PostalCodeDetail(bscInfo[10]));
    }

    /**
     * Method for parsing incoming data
     * Read data in MoreUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readMoreUserInfo(ByteBlock block, int requestID)
    {
        List infoData = getInfoForRequest(requestID);

        int offset = 1;
        String[] tmp = new String[1];

//        int age = LEBinaryTools.getUShort(block, offset);
        offset += 2;

        short genderCode = LEBinaryTools.getUByte(block, offset);
        ServerStoredDetails.GenderDetail gender = genders[genderCode];
        if(gender != null)
            infoData.add(gender);
        offset += 1;

        offset = readStrings(block, tmp, offset);
        try
        {
            if(tmp[0] != null)
                infoData.add(new ServerStoredDetails.WebPageDetail(new URL(tmp[0])));
        }
        catch (MalformedURLException ex)
        {}

        int birthdayYear = LEBinaryTools.getUShort(block, offset);
        offset += 2;

        short birthdayMonth = LEBinaryTools.getUByte(block, offset);
        offset += 1;

        short birthdayDay = LEBinaryTools.getUByte(block, offset);
        offset += 1;

        if(birthdayYear != 0 && birthdayMonth != 0 && birthdayDay != 0)
        {
            Calendar birthDate = Calendar.getInstance();
            birthDate.set(Calendar.YEAR, birthdayYear);
            birthDate.set(Calendar.MONTH, birthdayMonth);
            birthDate.set(Calendar.DAY_OF_MONTH, birthdayDay);

            infoData.add(new ServerStoredDetails.BirthDateDetail(birthDate));
        }

        short speakingLanguage1 = LEBinaryTools.getUByte(block, offset);
        offset += 1;
        logger.trace("read lang 1 " + speakingLanguage1);
        Locale spokenLanguage1 = getSpokenLanguage(speakingLanguage1);
        if(spokenLanguage1 != null)
        infoData.add(
            new ServerStoredDetails.SpokenLanguageDetail(spokenLanguage1));

        short speakingLanguage2 = LEBinaryTools.getUByte(block, offset);
        offset += 1;
        logger.trace("read lang 2 " + speakingLanguage2);
        Locale spokenLanguage2 = getSpokenLanguage(speakingLanguage2);
        if(spokenLanguage2 != null)
        infoData.add(
            new ServerStoredDetails.SpokenLanguageDetail(spokenLanguage2));

        short speakingLanguage3 = LEBinaryTools.getUByte(block, offset);
        offset += 1;
        logger.trace("read lang 3 " + speakingLanguage3);
        Locale spokenLanguage3 = getSpokenLanguage(speakingLanguage3);
        if(spokenLanguage3 != null)
        infoData.add(
            new ServerStoredDetails.SpokenLanguageDetail(spokenLanguage3));

//        int moreInfoUnknown = LEBinaryTools.getUShort(block, offset);
        offset += 2;

        offset = readStrings(block, tmp, offset);
        if(tmp[0] != null)
            infoData.add(new OriginCityDetail(tmp[0]));

        offset = readStrings(block, tmp, offset);
        if(tmp[0] != null)
            infoData.add(new OriginProvinceDetail(tmp[0]));

        int originCountryCode = LEBinaryTools.getUShort(block, offset);
        offset += 2;
        Locale originCountryLocale = getCountry(originCountryCode);
        if(originCountryLocale != null)
            infoData.add(new OriginCountryDetail(originCountryLocale));

        short userGMTOffset = LEBinaryTools.getUByte(block, offset);
        offset += 1;

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
     * @param block ByteBlock
     * @param requestID int
     */
    private void readEmailUserInfo(ByteBlock block, int requestID)
    {
        List infoData = getInfoForRequest(requestID);

        int offset = 1;
        String[] tmp = new String[1];

        short emailCount = LEBinaryTools.getUByte(block, offset);
        offset += 1;

//        short[] emailRights = new short[emailCount];
        for (int i = 0; i < emailCount; i++)
        {
            // per email rights
            short publish = LEBinaryTools.getUByte(block, offset);
            offset += 1;

            offset = readStrings(block, tmp, offset);
            if(tmp[0] != null)
                infoData.add(new ServerStoredDetails.EmailAddressDetail(tmp[0]));
//            emailRights[i] = publish;
        }
    }

    /**
     * Method for parsing incoming data
     * Read data in HomePageUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readHomePageUserInfo(ByteBlock block, int requestID)
    {
        List infoData = getInfoForRequest(requestID);

        int offset = 1;

        //1-enabled, 0-disabled
//        short enabled = LEBinaryTools.getUByte(block, offset);
        offset += 1;

//        int homePageCategory = LEBinaryTools.getUShort(block, offset);
        offset += 2;

        String[] tmp = new String[1];
        offset = readStrings(block, tmp, offset);

        try
        {
            if(tmp[0] != null)
                infoData.add(new ServerStoredDetails.WebPageDetail(new URL(tmp[0])));
        }
        catch (MalformedURLException ex)
        {}
    }

    /**
     * Method for parsing incoming data
     * Read data in WorkUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readWorkUserInfo(ByteBlock block, int requestID)
    {
        List infoData = getInfoForRequest(requestID);

        int offset = 1;

        String[] workAddress = new String[6];
        offset = readStrings(block, workAddress, offset);
        if(workAddress[0] != null)
            infoData.add(new ServerStoredDetails.WorkCityDetail(workAddress[0]));
        if(workAddress[1] != null)
            infoData.add(new ServerStoredDetails.WorkProvinceDetail(workAddress[1]));
        if(workAddress[2] != null)
            infoData.add(new ServerStoredDetails.WorkPhoneDetail(workAddress[2]));
        if(workAddress[3] != null)
            infoData.add(new WorkFaxDetail(workAddress[3]));
        if(workAddress[4] != null)
            infoData.add(new ServerStoredDetails.WorkAddressDetail(workAddress[4]));
        if(workAddress[5] != null)
            infoData.add(new ServerStoredDetails.WorkPostalCodeDetail(workAddress[5]));

        int workCountryCode = LEBinaryTools.getUShort(block, offset);
        offset += 2;
        Locale workCountry = getCountry(workCountryCode);
        if(workCountry != null)
            infoData.add(new ServerStoredDetails.WorkCountryDetail(workCountry));

        String[] workInfo = new String[3];
        offset = readStrings(block, workInfo, offset);
        if(workInfo[0] != null)
            infoData.add(new ServerStoredDetails.WorkOrganizationNameDetail(workInfo[0]));
        if(workInfo[1] != null)
            infoData.add(new WorkDepartmentNameDetail(workInfo[1]));
        if(workInfo[2] != null)
            infoData.add(new WorkPositionNameDetail(workInfo[2]));

        int workOccupationCode = LEBinaryTools.getUShort(block, offset);
        offset += 2;
        if(workOccupationCode == 99)
            infoData.add(new WorkOcupationDetail(occupations[occupations.length - 1]));
        else
            infoData.add(new WorkOcupationDetail(occupations[workOccupationCode]));

        String[] tmp = new String[1];
        offset = readStrings(block, tmp, offset);

        try
        {
            if(tmp[0] != null)
                infoData.add(new ServerStoredDetails.WorkPageDetail(new URL(tmp[0])));
        }
        catch (MalformedURLException ex)
        {}
    }

    /**
     * Method for parsing incoming data
     * Read data in UserAboutInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readUserAboutInfo(ByteBlock block, int requestID)
    {
        List infoData = getInfoForRequest(requestID);

        int offset = 1;
        String[] tmp = new String[1];
        offset = readStrings(block, tmp, offset);

        if(tmp[0] != null)
            infoData.add(new NotesDetail(tmp[0]));
    }

    /**
     * Method for parsing incoming data
     * Read data in InterestsUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readInterestsUserInfo(ByteBlock block, int requestID)
    {
        List infoData = getInfoForRequest(requestID);

        int offset = 1;
        String[] tmp = new String[1];

        short interestsCount = LEBinaryTools.getUByte(block, offset);
        offset += 1;

//        String[] interests = new String[interestsCount];
//        int[] categories = new int[interestsCount];

        for (int i = 0; i < interestsCount; i++)
        {
            // per interest
            int categoty = LEBinaryTools.getUShort(block, offset);
            offset += 2;

            offset = readStrings(block, tmp, offset);

            if(categoty != 0)
            {
                // as the categories are between 100 and 150 we shift them
                // because their string representations are stored in array
                categoty = categoty - 99;
            }
            infoData.add(new InterestDetail(tmp[0], interestsCategories[categoty]));
        }
    }

    /**
     * Not used for now
     * @param block ByteBlock data
     * @param requestID int the request id
     */
    private void readAffilationsUserInfo(ByteBlock block, int requestID)
    {
//        Vector infoData = getInfoForRequest(requestID);
//
//        int offset = 1;
//        String[] tmp = new String[1];
//
//        short pastCategoryCount = LEBinaryTools.getUByte(block, offset);
//        offset += 1;
//
//        int[] pastCategoryCodes = new int[pastCategoryCount];
//        String[] pastCategories = new String[pastCategoryCount];
//
//        for (int i = 0; i < pastCategoryCount; i++)
//        {
//            pastCategoryCodes[i] = LEBinaryTools.getUShort(block, offset);
//            offset += 2;
//
//            offset = readStrings(block, tmp, offset);
//            pastCategories[i] = tmp[0];
//        }
//
//        short affCategoryCount = LEBinaryTools.getUByte(block, offset);
//        offset += 1;
//
//        int[] affCategoryCodes = new int[pastCategoryCount];
//        String[] affCategories = new String[pastCategoryCount];
//
//        for (int i = 0; i < affCategoryCount; i++)
//        {
//            affCategoryCodes[i] = LEBinaryTools.getUShort(block, offset);
//            offset += 2;
//
//            offset = readStrings(block, tmp, offset);
//            affCategories[i] = tmp[0];
//        }

        // this is the last packet
        lastOfSequences = true;
    }

    /**
     * Checks the type of the current command
     * @param type IcqType
     * @return boolean
     */
    public static boolean isOfType(IcqType type)
    {
        switch (type.getSecondary())
        {
            case AbstractIcqCmd.USER_INFORMATION_BASIC:
                return true;
            case AbstractIcqCmd.USER_INFORMATION_MORE:
                return true;
            case AbstractIcqCmd.USER_INFORMATION_EXTENDED_EMAIL:
                return true;
            case AbstractIcqCmd.USER_INFORMATION_HOMEPAGE_CATEGORY:
                return true;
            case AbstractIcqCmd.USER_INFORMATION_WORK:
                return true;
            case AbstractIcqCmd.USER_INFORMATION_ABOUT:
                return true;
            case AbstractIcqCmd.USER_INFORMATION_INTERESTS:
                return true;
            case AbstractIcqCmd.USER_INFORMATION_AFFILATIONS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether this command is last of the sequence
     * @return boolean
     */
    public boolean isLastOfSequences()
    {
        return lastOfSequences;
    }

    /**
     * The request id of the current command
     * @return int
     */
    public int getRequestID()
    {
        return requestID;
    }

    /**
     * Return the retreived info from the last received request
     * @return List
     */
    public List getInfo()
    {
        return getInfoForRequest(getRequestID());
    }

    /**
     * Returns the stored info so far on the specified request
     *
     * @param requestID int
     * @return List
     */
    protected List getInfoForRequest(int requestID)
    {
        List res = (List) retreivedInfo.get(new Integer(requestID));

        if (res == null)
        {
            // this indicates that the info data
            // doesn't exists, so this is the first packet
            // from the sequence (basic info)

            res = new LinkedList();
            retreivedInfo.put(new Integer(requestID), res);
        }

        return res;
    }

    /**
     * Extracts String from the given byte block
     * starting from the specified position
     *
     * @param block ByteBlock
     * @param result String[] the result strings
     * @param offset int
     * @return int
     */
    private static int readStrings(ByteBlock block, String[] result, int offset)
    {
        for (int i = 0; i < result.length; i++)
        {
            final int textlen = LEBinaryTools.getUShort(block, offset) - 1; // Don't include the ending NUL.
            offset += 2;

            if (textlen > 0)
            {
                ByteBlock field = block.subBlock(offset, textlen);
                result[i] = OscarTools.getString(field, "US-ASCII");
                offset += textlen;
            }
            offset++; // Skip trailing NUL.
        }

        return offset;
    }

    /**
     * Returns the Locale corresponding the index coming from icq server
     *
     * @param code int
     * @return Locale
     */
    private static Locale getCountry(int code)
    {
        if(code == 0 || code == 9999) // not specified or other
            return null;

        String cryStr = (String)countryIndexToLocaleString.get(new Integer(code));

        // if no such country
        if(cryStr == null)
            return null;

        return new Locale("", cryStr);
    }

    /**
     * Returns the index stored on the server corresponding the given locale
     * @param cLocale Locale
     * @return int
     */
    private static int getCountryCode(Locale cLocale)
    {
        if(cLocale == null)
            return 0;// not specified

        Iterator iter = countryIndexToLocaleString.keySet().iterator();
        while(iter.hasNext())
        {
            Integer key = (Integer)iter.next();

            String countryString = ((String)countryIndexToLocaleString.get(key)).toUpperCase();
            if(countryString.equals(cLocale.getCountry()))
                return key.intValue();
        }

        return 0; // not specified
    }

    /**
     * Returns the Locale corresponding the index coming from icq server
     * @param code int
     * @return Locale
     */
    private static Locale getSpokenLanguage(int code)
    {
        if(code == 0 || code == 255) // not specified or other
            return null;

        return spokenLanguages[code];
    }

    /**
     * Origin City of user
     */
    public static class OriginCityDetail extends ServerStoredDetails.CityDetail
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
        public NotesDetail (String value)
        {
            super("Notes", value);
        }
    }

    /**
     * User interests
     */
    public static class InterestDetail extends ServerStoredDetails.InterestDetail
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
}
