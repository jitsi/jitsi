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

import java.io.*;
import java.util.*;

import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.tlv.*;

/**
 * Command for retreiving user info and setting user info,
 * user info which is stored on the server
 *
 * @author Damian Minkov
 */
public class FullUserInfoCmd
    extends SnacCommand
{

    /** A TLV type containing the ICQ-specific data. */
    private static final int TYPE_ICQ_DATA = 0x0001;

    private static CommandFactory commandFactory = new CommandFactory();

    private String senderUIN;

    // data that is send to the server
    private ByteArrayOutputStream icqDataOut = new ByteArrayOutputStream();

    private int primaryType = -1;
    private int secondaryType = -1;

    private int requestID = -1;

    boolean lastOfSequences = false;

    private static Hashtable<Integer, Hashtable<String, Object>> retreivedInfo
        = new Hashtable<Integer, Hashtable<String, Object>>();

    // properties for the retreived info
    final static String LAST_NAME = "LastName";
    final static String PHONE_NUMBER = "PhoneNumber";
    final static String SPEAK_LANG = "SpeakingLanguage";
    final static String HOME_COUNTRY = "HomeCountry";

    /**
     * Used when sending commands
     *
     * @param senderUIN String
     */
    public FullUserInfoCmd(String senderUIN)
    {
        super(21, 2);

        this.senderUIN = senderUIN;
    }

    /**
     * For constructing incoming commands
     *
     * @param packet SnacPacket
     */
    public FullUserInfoCmd(SnacPacket packet)
    {
        super(21, 3);

        Tlv icqDataTlv = TlvTools.readChain(packet.getData()).getLastTlv(TYPE_ICQ_DATA);
        if (icqDataTlv != null)
        {
            ByteBlock icqBlock = icqDataTlv.getData();

            ByteBlock icqData;

            int hdrlen = 8; // The expected header length, not counting the length field itself.
            senderUIN = String.valueOf(getUInt(icqBlock, 2));
            requestID = getUShort(icqBlock, 8); // request id

            this.primaryType = getUShort(icqBlock, 6);

            if (primaryType >= 1000)
            {
                this.secondaryType = getUShort(icqBlock, 10);
                hdrlen = 10;
            }

            if (icqBlock.getLength() >= hdrlen + 2)
            {
                icqData = icqBlock.subBlock(hdrlen + 2);
            }
            else
            {
                icqData = null;
            }

            processICQData(icqData);
        }
    }

    /**
     * Process the data in the received packet
     *
     * @param icqData ByteBlock
     */
    private void processICQData(ByteBlock icqData)
    {
        switch (secondaryType)
        {
            case 0x00C8 //USER_INFORMATION_BASIC
                : readBasicUserInfo(icqData, requestID);break;
            case 0x00DC //USER_INFORMATION_MORE
                : readMoreUserInfo(icqData, requestID);break;
            case 0x00EB //USER_INFORMATION_EXTENDED_EMAIL
                : readEmailUserInfo(icqData, requestID);break;
            case 0x010E //USER_INFORMATION_HOMEPAGE_CATEGORY
                : readHomePageUserInfo(icqData, requestID);break;
            case 0x00D2 //USER_INFORMATION_WORK
                : readWorkUserInfo(icqData, requestID);break;
            case 0x00E6 //USER_INFORMATION_ABOUT
                : readUserAboutInfo(icqData, requestID);break;
            case 0x00F0 //USER_INFORMATION_INTERESTS
                : readInterestsUserInfo(icqData, requestID);break;
            case 0x00FA //USER_INFORMATION_AFFILATIONS
                : readAffilationsUserInfo(icqData, requestID);break;
        }

    }

    /**
     * Writes this command's SNAC data block to the given stream.
     *
     * @param out the stream to which to write the SNAC data
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeData(OutputStream out)
            throws IOException
    {
        ByteArrayOutputStream icqout = new ByteArrayOutputStream();

        int hdrlen = 10; // The expected header length, not counting the length field itself.
        int primary = 0x07D0;
        int secondary = 0x0c3a;

        long icqUINlong = Long.parseLong(senderUIN);

        int length = hdrlen + icqDataOut.size();

        writeUShort(icqout, length);
        writeUInt(icqout, icqUINlong);
        writeUShort(icqout, primary);
        writeUShort(icqout, 0x0002); // the sequence

        writeUShort(icqout, secondary);

        icqDataOut.writeTo(icqout);

        new Tlv(TYPE_ICQ_DATA, ByteBlock.wrap(icqout.toByteArray())).write(out);
    }

    /**
     * Returns the stored info so far on the specified request
     *
     * @param requestID int
     * @return Hashtable
     */
    private Hashtable<String, Object> getInfoForRequest(int requestID)
    {
        Hashtable<String, Object> res
            = retreivedInfo.get(new Integer(requestID));

        if (res == null)
        {
            // this indicates that the info data
            // doesn't exists, so this is the first packet
            // from the sequence (basic info)

            res = new Hashtable<String, Object>();
            retreivedInfo.put(new Integer(requestID), res);
        }

        return res;
    }

    /**
     * Return the retreived info from the last received request
     * @return Hashtable
     */
    public Hashtable<String, Object> getInfo()
    {
        return getInfoForRequest(requestID);
    }

    /**
     * Method for parsing incoming data
     * Read data in BasicUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readBasicUserInfo(ByteBlock block, int requestID)
    {
        Hashtable<String, Object> infoData = getInfoForRequest(requestID);

        // sequence of 11 String fields
        String bscInfo[] = new String[11];
        int offset = readStrings(block, bscInfo, 1);

        int homeCountryCode = getUShort(block, offset);
        offset += 2;
        infoData.put(HOME_COUNTRY, new Integer(homeCountryCode));

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
//        infoData.add(new ServerStoredDetails.NicknameDetail(bscInfo[0]));
//        infoData.add(new ServerStoredDetails.FirstNameDetail(bscInfo[1]));

        if(bscInfo[2] != null)
            infoData.put(LAST_NAME, bscInfo[2]);

//        infoData.add(new ServerStoredDetails.EmailAddressDetail(bscInfo[3]));
//        infoData.add(new ServerStoredDetails.CityDetail(bscInfo[4]));
//        infoData.add(new ServerStoredDetails.ProvinceDetail(bscInfo[5]));

        if(bscInfo[6] != null)
            infoData.put(PHONE_NUMBER, bscInfo[6]);

//        infoData.add(new ServerStoredDetails.FaxDetail(bscInfo[7]));
//        infoData.add(new ServerStoredDetails.AddressDetail(bscInfo[8]));
//        infoData.add(new ServerStoredDetails.MobilePhoneDetail(bscInfo[9]));
//        infoData.add(new ServerStoredDetails.PostalCodeDetail(bscInfo[10]));
    }

    /**
     * Method for parsing incoming data
     * Read data in MoreUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readMoreUserInfo(ByteBlock block, int requestID)
    {
        Hashtable<String, Object> infoData = getInfoForRequest(requestID);

        int offset = 1;
        String[] tmp = new String[1];
//
//        int age = LEBinaryTools.getUShort(block, offset);
        offset += 2;
//
//        short gender = LEBinaryTools.getUByte(block, offset);
//        infoData.add(genders[gender]);
        offset += 1;
//
        offset = readStrings(block, tmp, offset);
//        try
//        {
//            infoData.add(new ServerStoredDetails.WebPageDetail(new URL(tmp[0])));
//        }
//        catch (MalformedURLException ex)
//        {}
//
//        int birthdayYear = LEBinaryTools.getUShort(block, offset);
        offset += 2;
//
//        short birthdayMonth = LEBinaryTools.getUByte(block, offset);
        offset += 1;
//
//        short birthdayDay = LEBinaryTools.getUByte(block, offset);
        offset += 1;
//
//        if(birthdayYear == 0 || birthdayMonth == 0 || birthdayDay == 0)
//        {
//            infoData.add(new ServerStoredDetails.BirthDateDetail(null));
//        }
//        else
//        {
//            Calendar birthDate = Calendar.getInstance();
//            birthDate.set(Calendar.YEAR, birthdayYear);
//            birthDate.set(Calendar.MONTH, birthdayMonth);
//            birthDate.set(Calendar.DAY_OF_MONTH, birthdayDay);
//
//            infoData.add(new ServerStoredDetails.BirthDateDetail(birthDate));
//        }
//
        ArrayList<Integer> langs = new ArrayList<Integer>();
        short speakingLanguage1 = getUByte(block, offset);
        offset += 1;
        if(speakingLanguage1 != 0 && speakingLanguage1 != 255)
        {
            langs.add(new Integer(speakingLanguage1));
        }

        short speakingLanguage2 = getUByte(block, offset);
        offset += 1;
        if(speakingLanguage2 != 0 && speakingLanguage2 != 255)
        {
            langs.add(new Integer(speakingLanguage2));
        }

        short speakingLanguage3 = getUByte(block, offset);
        offset += 1;
        if(speakingLanguage3 != 0 && speakingLanguage3 != 255)
        {
            langs.add(new Integer(speakingLanguage3));
        }

        infoData.put(SPEAK_LANG, langs);

//        int moreInfoUnknown = LEBinaryTools.getUShort(block, offset);
//        offset += 2;
//
//        offset = readStrings(block, tmp, offset);
//        infoData.add(new OriginCityDetail(tmp[0]));
//
//        offset = readStrings(block, tmp, offset);
//        infoData.add(new OriginProvinceDetail(tmp[0]));
//
//        int originCountryCode = LEBinaryTools.getUShort(block, offset);
//        offset += 2;
//        infoData.add(new OriginCountryDetail(getCountry(originCountryCode)));
//
//        short userGMTOffset = LEBinaryTools.getUByte(block, offset);
//        offset += 1;
//
//        TimeZone userTimeZone = null;
//        if(userGMTOffset >= 0)
//            userTimeZone = TimeZone.getTimeZone("GMT+" + userGMTOffset);
//        else
//            userTimeZone = TimeZone.getTimeZone("GMT" + userGMTOffset);
//
//        infoData.add(new ServerStoredDetails.TimeZoneDetail("GMT Offest", userTimeZone));
    }

    /**
     * Method for parsing incoming data
     * Read data in EmailUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readEmailUserInfo(ByteBlock block, int requestID)
    {
//        Vector infoData = getInfoForRequest(requestID);
//
//        int offset = 1;
//        String[] tmp = new String[1];
//
//        short emailCount = LEBinaryTools.getUByte(block, offset);
//        offset += 1;
//
//        String[] emails = new String[emailCount];
//        short[] emailRights = new short[emailCount];
//
//        for (int i = 0; i < emailCount; i++)
//        {
//            // per email rights
//            short publish = LEBinaryTools.getUByte(block, offset);
//            offset += 1;
//
//            offset = readStrings(block, tmp, offset);
//            infoData.add(new ServerStoredDetails.EmailAddressDetail(tmp[0]));
//            emailRights[i] = publish;
//        }
    }

    /**
     * Method for parsing incoming data
     * Read data in HomePageUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readHomePageUserInfo(ByteBlock block, int requestID)
    {
//        Vector infoData = getInfoForRequest(requestID);
//
//        int offset = 1;
//
//        //1-enabled, 0-disabled
//        short enabled = LEBinaryTools.getUByte(block, offset);
//        offset += 1;
//
//        int homePageCategory = LEBinaryTools.getUShort(block, offset);
//        offset += 2;
//
//        String[] tmp = new String[1];
//        offset = readStrings(block, tmp, offset);
//
//        try
//        {
//            infoData.add(new ServerStoredDetails.WebPageDetail(new URL(tmp[0])));
//        }
//        catch (MalformedURLException ex)
//        {}
    }

    /**
     * Method for parsing incoming data
     * Read data in WorkUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readWorkUserInfo(ByteBlock block, int requestID)
    {
//        Vector infoData = getInfoForRequest(requestID);
//
//        int offset = 1;
//
//        String[] workAddress = new String[6];
//        offset = readStrings(block, workAddress, offset);
//        infoData.add(new ServerStoredDetails.WorkCityDetail(workAddress[0]));
//        infoData.add(new ServerStoredDetails.WorkProvinceDetail(workAddress[1]));
//        infoData.add(new ServerStoredDetails.WorkPhoneDetail(workAddress[2]));
//        infoData.add(new WorkFaxDetail(workAddress[3]));
//        infoData.add(new ServerStoredDetails.WorkAddressDetail(workAddress[4]));
//        infoData.add(new ServerStoredDetails.WorkPostalCodeDetail(workAddress[5]));
//
//        int workCountryCode = LEBinaryTools.getUShort(block, offset);
//        offset += 2;
//        infoData.add(
//            new ServerStoredDetails.WorkCountryDetail(getCountry(workCountryCode)));
//
//        String[] workInfo = new String[3];
//        offset = readStrings(block, workInfo, offset);
//        infoData.add(new ServerStoredDetails.WorkOrganizationNameDetail(workInfo[0]));
//        infoData.add(new WorkDepartmentNameDetail(workInfo[1]));
//        infoData.add(new WorkPositionNameDetail(workInfo[2]));
//
//        int workOccupationCode = LEBinaryTools.getUShort(block, offset);
//        offset += 2;
//        if(workOccupationCode == 99)
//            infoData.add(new WorkOcupationDetail(occupations[occupations.length - 1]));
//        else
//            infoData.add(new WorkOcupationDetail(occupations[workOccupationCode]));
//
//        String[] tmp = new String[1];
//        offset = readStrings(block, tmp, offset);
//
//        try
//        {
//            infoData.add(new ServerStoredDetails.WorkPageDetail(new URL(tmp[0])));
//        }
//        catch (MalformedURLException ex)
//        {}
    }

    /**
     * Method for parsing incoming data
     * Read data in UserAboutInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readUserAboutInfo(ByteBlock block, int requestID)
    {
//        Vector infoData = getInfoForRequest(requestID);
//
//        int offset = 1;
//        String[] tmp = new String[1];
//        offset = readStrings(block, tmp, offset);
//
//        infoData.add(new NotesDetail(tmp[0]));
    }

    /**
     * Method for parsing incoming data
     * Read data in InterestsUserInfo command
     * @param block ByteBlock
     * @param requestID int
     */
    private void readInterestsUserInfo(ByteBlock block, int requestID)
    {
//        Vector infoData = getInfoForRequest(requestID);
//
//        int offset = 1;
//        String[] tmp = new String[1];
//
//        short interestsCount = LEBinaryTools.getUByte(block, offset);
//        offset += 1;
//
//        String[] interests = new String[interestsCount];
//        int[] categories = new int[interestsCount];
//
//        for (int i = 0; i < interestsCount; i++)
//        {
//            // per interest
//            int categoty = LEBinaryTools.getUShort(block, offset);
//            offset += 2;
//
//            offset = readStrings(block, tmp, offset);
//
//            if(categoty != 0)
//            {
//                // as the categories are between 100 and 150 we shift them
//                // because their string representations are stored in array
//                categoty = categoty - 99;
//            }
//            infoData.add(new InterestDetail(tmp[0], interestsCategories[categoty]));
//        }
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
     * Writes Byte data to the icqDataOut
     * which is send to the server
     * @param dataType int the data type used in the Tlv
     * @param value int
     */
    protected void writeOutByte(int dataType, int value)
    {
        try
        {
            writeUShort(icqDataOut, dataType);
            writeUShort(icqDataOut, 1);
            writeUByte(icqDataOut, value);
        }
        catch (IOException ex)
        {}
    }

    /**
     * Writes Short data to the icqDataOut
     * which is send to the server
     * @param dataType int the data type used in the Tlv
     * @param value int
     */
    protected void writeOutShort(int dataType, int value)
    {
        try
        {
            writeUShort(icqDataOut, dataType);
            writeUShort(icqDataOut, 2);
            writeUShort(icqDataOut, value);
        }
        catch (IOException ex)
        {}
    }

    /**
     * Writes String data to the icqDataOut
     * which is send to the server
     * @param dataType int the data type used in the Tlv
     * @param value String
     */
    protected void writeOutString(int dataType, String value)
    {
        try
        {
            byte[] data = BinaryTools.getAsciiBytes(value);
            writeUShort(icqDataOut, dataType);
            writeUShort(icqDataOut, data.length + 2);
            writeUShort(icqDataOut, data.length);
            icqDataOut.write(data);
        }
        catch (IOException ex)
        {}
    }

    /**
     * Writes Int data to the icqDataOut
     * which is send to the server
     * @param out OutputStream
     * @param number long the data type used in the Tlv
     * @throws IOException
     */
    private static void writeUInt(final OutputStream out, final long number)
        throws IOException
    {
        out.write(new byte[] {
            (byte)((number) & 0xff),
            (byte)((number >> 8) & 0xff),
            (byte)((number >> 16) & 0xff),
            (byte)((number >> 24) & 0xff)
        });
    }

    /**
     * Writes Short data to the stream
     * @param out OutputStream
     * @param number int
     * @throws IOException
     */
    private static void writeUShort(OutputStream out, int number)
        throws IOException
    {
        out.write(new byte[]
            {
            (byte)(number & 0xff),
            (byte)((number >> 8) & 0xff)
        });
    }

    /**
     * Writes Byte data to the stream
     * @param out OutputStream
     * @param number int
     * @throws IOException
     */
    private static void writeUByte(OutputStream out, int number)
        throws IOException
    {
        out.write(new byte[]{(byte) (number & 0xff)});
    }

    /**
     * Extracts Int from the given byte block
     * starting from the specified position
     * @param data ByteBlock
     * @param pos int
     * @return long
     */
    private static long getUInt(final ByteBlock data, final int pos)
    {
        if (data.getLength() - pos < 4)
        {
            return -1;
        }

        return ( ( data.get(pos + 3) & 0xffL) << 24)
            | ( ( data.get(pos + 2) & 0xffL) << 16)
            | ( ( data.get(pos + 1) & 0xffL) << 8)
            | ( data.get(pos) & 0xffL);
    }

    /**
     * Extracts Short from the given byte block
     * starting from the specified position
     * @param data ByteBlock
     * @param pos int
     * @return int
     */
    private static int getUShort(final ByteBlock data, final int pos)
    {
        if (data.getLength() - pos < 2)
        {
            return -1;
        }

        return ( (data.get(pos + 1) & 0xff) << 8) | (data.get(pos) & 0xff);
    }

    /**
     * Extracts Byte from the given byte block
     * starting from the specified position
     * @param data ByteBlock
     * @param pos int
     * @return short
     */
    public static short getUByte(final ByteBlock data, final int pos)
    {
        if (data.getLength() - pos < 1)
        {
            return -1;
        }

        return (short) (data.get(pos) & 0xff);
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
            final int textlen = getUShort(block, offset) - 1; // Don't include the ending NUL.
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
     * The factory used to pass incoming commands
     *
     * @return SnacCmdFactory
     */
    protected static SnacCmdFactory getCommandFactory()
    {
        return commandFactory;
    }

    /**
     * Return the command for requesting full user info
     * @param senderUIN String the uin of the sender
     * @param userInfoUIN String the uin of the requested user info
     * @return SnacCommand
     */
    protected static SnacCommand getFullInfoRequestCommand(String senderUIN, String userInfoUIN)
    {
        return new FullInfoRequest(senderUIN, userInfoUIN);
    }

    /**
     * Factory used for registering FullUserInfoCmd
     * for receiving command
     */
    private static class CommandFactory
        implements SnacCmdFactory
    {
        static final List<CmdType> SUPPORTED_TYPES =
        DefensiveTools.asUnmodifiableList(new CmdType[]
                                          {new CmdType(21, 3)});

        public SnacCommand genSnacCommand(SnacPacket packet)
        {
            // we are handling only one type of icq old style messages
            // so we will return it. We are sure that this command is 21,3
            return new FullUserInfoCmd(packet);
        }

        public List<CmdType> getSupportedTypes()
        {
            return SUPPORTED_TYPES;
        }
    }

    /**
     * Command used for requestin full user info
     */
    private static class FullInfoRequest
        extends SnacCommand
    {
        private String senderUIN;
        private String userInfoUIN;

        /**
         * Creating request for the specified user info
         * from specified sender
         *
         * @param senderUIN String
         * @param userInfoUIN String
         */
        FullInfoRequest(String senderUIN, String userInfoUIN)
        {
            super(21, 2);

            this.senderUIN = senderUIN;
            this.userInfoUIN = userInfoUIN;
        }

        /**
         * Writing data to the stream sending it to server
         * @param out OutputStream
         * @throws IOException
         */
        @Override
        public void writeData(OutputStream out) throws IOException
        {
            ByteArrayOutputStream icqout = new ByteArrayOutputStream();

            ByteArrayOutputStream icqDataOut = new ByteArrayOutputStream();
            writeUInt(icqDataOut, Long.parseLong(userInfoUIN));

            int hdrlen = 10; // The expected header length, not counting the length field itself.
            int primary = 0x07D0;
            int secondary = 0x04B2;

            long icqUINlong = Long.parseLong(senderUIN);

            int length = hdrlen + icqDataOut.size();

            writeUShort(icqout, length);
            writeUInt(icqout, icqUINlong);
            writeUShort(icqout, primary);
            writeUShort(icqout, 0x0002); // the sequence

            writeUShort(icqout, secondary);

            icqDataOut.writeTo(icqout);

            new Tlv(TYPE_ICQ_DATA, ByteBlock.wrap(icqout.toByteArray())).write(out);
        }
    }
}
