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
package net.java.sip.communicator.service.protocol.yahooconstants;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An enumeration containing all status instances that MUST be supported by
 * an implementation of the yahoo protocol. Implementations may
 * support other forms of PresenceStatus but they MUST ALL support those
 * enumerated here.
 * <p>
 * For testing purposes, this class also provides a <tt>List</tt> containing
 * all of the status fields.
 *
 * @author Damian Minkov
 */
public class YahooStatusEnum
    extends PresenceStatus
{
    /**
     * The <tt>Logger</tt> used by the <tt>YahooStatusEnum</tt> class and its
     * instances for logging output.
     */
    private static Logger logger = Logger.getLogger(YahooStatusEnum.class);

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final YahooStatusEnum AVAILABLE
        = new YahooStatusEnum(65, "Available",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-online.png"));

    /**
     * The Not Available status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately than
     * when in an Away status ;-P ) upon initiation of communication.
     */
    public static final YahooStatusEnum BE_RIGHT_BACK
        = new YahooStatusEnum(48, "Be Right Back",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-away.png"));

    /**
     * The Idle status. Indicates that the user is not using the messanger.
     */
    public static final YahooStatusEnum IDLE
        = new YahooStatusEnum(46, "Idle",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-idle.png"));

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final YahooStatusEnum INVISIBLE
        = new YahooStatusEnum(45, "Invisible",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-invisible.png"));

    /**
     * The STEPPED_OUT  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final YahooStatusEnum STEPPED_OUT
        = new YahooStatusEnum(40, "Stepped out",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-away.png"));

    /**
     * The Out to lunch status. Indicates that the user is eating.
     */
    public static final YahooStatusEnum OUT_TO_LUNCH
        = new YahooStatusEnum(39, "Out to lunch",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-lunch.png"));

    /**
     * The Not at home status. Indicates that the user is not at home.
     */
    public static final YahooStatusEnum NOT_AT_HOME
        = new YahooStatusEnum(38, "Not at home",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-na.png"));

    /**
     * The Not at desk status. Indicates that the user is not at his desk, but
     * somewhere in the office.
     */
    public static final YahooStatusEnum NOT_AT_DESK
        = new YahooStatusEnum(36, "Not at desk",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-na.png"));

    /**
     * The Not in office status. Indicates that the user is out of the office.
     */
    public static final YahooStatusEnum NOT_IN_OFFICE
        = new YahooStatusEnum(34, "Not in office",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-na.png"));

    /**
     * The On vacation status. Indicates that the user is somewhere on the
     * beach or skiing.
     */
    public static final YahooStatusEnum ON_VACATION
        = new YahooStatusEnum(33, "On vacation",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-vacation.png"));

    /**
     * The On the phone status. Indicates that the user is talking to the phone.
     */
    public static final YahooStatusEnum ON_THE_PHONE
        = new YahooStatusEnum(31, "On the phone",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-phone.png"));

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final YahooStatusEnum BUSY
        = new YahooStatusEnum(30, "Busy",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-busy.png"));

    /**
     * The Offline status. Indicates the user does not seem to be connected
     * to the network or at least does not want us to know she is
     */
    public static final YahooStatusEnum OFFLINE
        = new YahooStatusEnum(0, "Offline",
                loadIcon("resources/images/protocol/yahoo/yahoo16x16-offline.png"));

    /**
     * The minimal set of states that any implementation must support.
     */
    public static final ArrayList<YahooStatusEnum> yahooStatusSet
        = new ArrayList<YahooStatusEnum>();
    static{
            yahooStatusSet.add(AVAILABLE);
            yahooStatusSet.add(BE_RIGHT_BACK);
            yahooStatusSet.add(BUSY);
            yahooStatusSet.add(IDLE);
            yahooStatusSet.add(INVISIBLE);
            yahooStatusSet.add(NOT_AT_DESK);
            yahooStatusSet.add(NOT_AT_HOME);
            yahooStatusSet.add(NOT_IN_OFFICE);
            yahooStatusSet.add(OFFLINE);
            yahooStatusSet.add(ON_THE_PHONE);
            yahooStatusSet.add(ON_VACATION);
            yahooStatusSet.add(OUT_TO_LUNCH);
            yahooStatusSet.add(STEPPED_OUT);
    }

    /**
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected YahooStatusEnum(int status, String statusName, byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath) {
        InputStream is = YahooStatusEnum.class.getClassLoader()
            .getResourceAsStream(imagePath);

        if(is == null)
            return null;

        byte[] icon = null;
        try {
            icon = new byte[is.available()];
            is.read(icon);
        } catch (IOException exc) {
            logger.error("Failed to load icon: " + imagePath, exc);
        }
        return icon;
    }
}
