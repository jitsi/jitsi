/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec;

import java.io.*;
import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Simple configuration of encoding priorities.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class EncodingConfiguration
{

    /**
     * The <tt>Logger</tt> used by this <tt>EncodingConfiguration</tt> instance
     * for logging output.
     */
    private final Logger logger = Logger.getLogger(EncodingConfiguration.class);

    private static final String PROP_SDP_PREFERENCE
        = "net.java.sip.communicator.impl.neomedia.codec.EncodingConfiguration";

    /**
     * The indicator which determines whether the G.729 codec is enabled.
     *
     * WARNING: The use of G.729 may require a license fee and/or royalty fee in
     * some countries and is licensed by
     * <a href="http://www.sipro.com">SIPRO Lab Telecom</a>.
     */
    public static final boolean G729 = false;

    private static final String[] CUSTOM_CODECS =
        {
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.alaw.Encoder"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.alaw.JavaEncoder",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.alaw.DePacketizer"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.alaw.DePacketizer",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.alaw.Packetizer"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.alaw.Packetizer",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.ulaw.Packetizer"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.ulaw.Packetizer",
            "net.java.sip.communicator.impl.neomedia.codec.video.h264.JNIEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.video.h264.Packetizer",
            "net.java.sip.communicator.impl.neomedia.codec.video.h264.JNIDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.video.ImageScaler",
            "net.java.sip.communicator.impl.neomedia.codec.audio.speex.JavaEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.speex.JavaDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.ilbc.JavaEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.ilbc.JavaDecoder",
            G729
                ? "net.java.sip.communicator.impl.neomedia.codec.audio.g729.JavaEncoder"
                : null,
            G729
                ? "net.java.sip.communicator.impl.neomedia.codec.audio.g729.JavaDecoder"
                : null
        };

    /**
     * Custom Packages provided by Sip-Communicator
     */
    private static final String[] CUSTOM_PACKAGES = new String[]
    { // datasource for low latency ALSA input
        "net.java.sip.communicator.impl", "net.sf.fmj" };

    /**
     * The <tt>Comparator</tt> which sorts the sets according to the settings in
     * encodingPreferences.
     */
    private final Comparator<MediaFormat> encodingComparator
        = new Comparator<MediaFormat>()
                {
                    public int compare(MediaFormat s1, MediaFormat s2)
                    {
                        return compareEncodingPreferences(s1, s2);
                    }
                };

    /**
     * That's where we keep format preferences matching SDP formats to integers.
     * We keep preferences for both audio and video formats here in case we'd
     * ever need to compare them to one another. In most cases however both
     * would be decorelated and other components (such as the UI) should present
     * them separately.
     */
    private final Map<MediaFormat, Integer> encodingPreferences =
        new Hashtable<MediaFormat, Integer>();

    /**
     * The cache of supported <tt>AudioMediaFormat</tt>s ordered by decreasing
     * priority.
     */
    private Set<MediaFormat> supportedAudioEncodings;

    /**
     * The cache of supported <tt>VideoMediaFormat</tt>s ordered by decreasing
     * priority.
     */
    private Set<MediaFormat> supportedVideoEncodings;

    /**
     * Default constructor.
     */
    public EncodingConfiguration()
    {
    }

    /**
     * Retrieves (from the configuration service) preferences specified for
     * various formats and assigns default ones to those that haven't been
     * mentioned.
     */
    public void initializeFormatPreferences()
    {
        // first init default preferences
        // video
        setEncodingPreference(
            "H264",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            1100);
        setEncodingPreference(
            "H263",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            1000);
        setEncodingPreference(
            "JPEG",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            950);
        setEncodingPreference(
            "H261",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            800);

        // audio
        setEncodingPreference("PCMU", 8000, 650);
        setEncodingPreference("PCMA", 8000, 600);
        setEncodingPreference("iLBC", 8000, 500);
        setEncodingPreference("GSM", 8000, 450);
        setEncodingPreference("speex", 8000, 352);
        setEncodingPreference("speex", 16000, 351);
        setEncodingPreference("speex", 32000, 350);
        setEncodingPreference("DVI4", 8000, 300);
        setEncodingPreference("DVI4", 16000, 250);
        setEncodingPreference("G723", 8000, 150);
        setEncodingPreference("G728", 8000, 100);
        setEncodingPreference("G729", 8000, 50);

        // now override with those that are specified by the user.
        ConfigurationService confService
            = NeomediaActivator.getConfigurationService();

        for (String pName
                : confService
                    .getPropertyNamesByPrefix(PROP_SDP_PREFERENCE, false))
        {
            String prefStr = confService.getString(pName);
            String fmtName
                = pName
                    .substring(pName.lastIndexOf('.') + 1)
                        .replaceAll("sdp", ""); // legacy
            int preference = -1;
            String encoding;
            double clockRate;
            try
            {
                preference = Integer.parseInt(prefStr);

                int encodingClockRateSeparator = fmtName.lastIndexOf('/');

                if (encodingClockRateSeparator > -1)
                {
                    encoding = fmtName.substring(0, encodingClockRateSeparator);
                    clockRate
                        = Double
                            .parseDouble(
                                fmtName
                                    .substring(encodingClockRateSeparator + 1));
                }
                else
                {
                    encoding = fmtName;
                    clockRate = MediaFormatFactory.CLOCK_RATE_NOT_SPECIFIED;
                }
            }
            catch (NumberFormatException nfe)
            {
                logger
                    .warn(
                        "Failed to parse format ("
                            + fmtName
                            + ") or preference ("
                            + prefStr
                            + ").",
                        nfe);
                continue;
            }

            setEncodingPreference(encoding, clockRate, preference);
        }

        // now update the arrays so that they are returned by order of
        // preference.
        updateSupportedEncodings();
    }

    /**
     * Updates the codecs in the supported sets according to the preferences in
     * encodingPreferences. If the preference value is <tt>0</tt>, the codec is
     * disabled.
     */
    private void updateSupportedEncodings()
    {
        /*
         * If they need updating, their caches are invalid and need rebuilding
         * next time they are requested.
         */
        supportedAudioEncodings = null;
        supportedVideoEncodings = null;
    }

    /**
     * Gets the <tt>Set</tt> of enabled available <tt>MediaFormat</tt>s with the
     * specified <tt>MediaType</tt> sorted in decreasing priority.
     *
     * @param type the <tt>MediaType</tt> of the <tt>MediaFormat</tt>s to get
     * @return a <tt>Set</tt> of enabled available <tt>MediaFormat</tt>s with
     * the specified <tt>MediaType</tt> sorted in decreasing priority
     */
    private Set<MediaFormat> updateSupportedEncodings(MediaType type)
    {
        Set<MediaFormat> supported
            = new TreeSet<MediaFormat>(encodingComparator);

        for (MediaFormat format : getAvailableEncodings(type))
            if (getPriority(format) > 0)
                supported.add(format);
        return supported;
    }

    /**
     * Sets <tt>pref</tt> as the preference associated with <tt>encoding</tt>.
     * Use this method for both audio and video encodings and don't worry if
     * preferences are equal since we rarely need to compare prefs of video
     * encodings to those of audio encodings.
     *
     * @param encoding the SDP int of the encoding whose pref we're setting.
     * @param clockRate
     * @param pref a positive int indicating the preference for that encoding.
     */
    private void setEncodingPreference(String encoding, double clockRate, int pref)
    {
        MediaFormat mediaFormat = MediaUtils.getMediaFormat(encoding, clockRate);

        if (mediaFormat != null)
            encodingPreferences.put(mediaFormat, pref);
    }

    /**
     * Sets <tt>pref</tt> as the preference associated with <tt>encoding</tt>.
     * Use this method for both audio and video encodings and don't worry if
     * preferences are equal since we rarely need to compare prefs of video
     * encodings to those of audio encodings.
     *
     * @param encoding a string containing the SDP int of the encoding whose
     * pref we're setting.
     * @param priority a positive int indicating the preference for that encoding.
     */
    public void setPriority(MediaFormat encoding, int priority)
    {
        encodingPreferences.put(encoding, priority);

        // save the settings
        NeomediaActivator
            .getConfigurationService()
                .setProperty(
                    PROP_SDP_PREFERENCE
                        + "."
                        + encoding.getEncoding()
                        + "/"
                        + encoding.getClockRateString(),
                    priority);

        updateSupportedEncodings();
    }

    public int getPriority(MediaFormat encoding)
    {

        /*
         * Directly returning encodingPreference.get(encoding) will throw a
         * NullPointerException if encodingPreferences does not contain a
         * mapping for encoding.
         */
        Integer priority = encodingPreferences.get(encoding);

        return (priority == null) ? 0 : priority;
    }

    /**
     * Register in JMF the custom codecs we provide
     */
    public void registerCustomCodecs()
    {
        // Register the custom codec which haven't already been registered.
        @SuppressWarnings("unchecked")
        Collection<String> registeredPlugins
            = new HashSet<String>(
                    PlugInManager
                        .getPlugInList(null, null, PlugInManager.CODEC));
        boolean commit = false;

        for (String className : CUSTOM_CODECS)
        {

            /*
             * A codec with a className of null is configured at compile time to
             * not be registered.
             */
            if (className == null)
                continue;

            if (registeredPlugins.contains(className))
            {
                logger.debug("Codec : " + className + " is already registered");
            }
            else
            {
                commit = true;

                boolean registered;
                Throwable exception = null;

                try
                {
                    Codec codec = (Codec)
                        Class.forName(className).newInstance();

                    registered =
                        PlugInManager.addPlugIn(
                            className,
                            codec.getSupportedInputFormats(),
                            codec.getSupportedOutputFormats(null),
                            PlugInManager.CODEC);
                }
                catch (Throwable ex)
                {
                    registered = false;
                    exception = ex;
                }
                if (registered)
                    logger.debug(
                            "Codec "
                            + className
                            + " is successfully registered");
                else
                    logger.debug(
                            "Codec "
                            + className
                            + " is NOT succsefully registered", exception);
            }
        }

        if (commit)
            try
            {
                PlugInManager.commit();
            }
            catch (IOException ex)
            {
                logger.error("Cannot commit to PlugInManager", ex);
            }

        // Register the custom codec formats with the RTP manager once at
        // initialization. This is needed for the Sun JMF implementation. It
        // causes the registration of the formats with the static FormatInfo
        // instance of com.sun.media.rtp.RTPSessionMgr, which in turn makes the
        // formats available when the supported encodings arrays are generated
        // in initProcessor(). In other JMF implementations this might not be
        // needed, but should do no harm.

        // Commented as it fails to load alaw codec
        // RTPManager rtpManager = RTPManager.newInstance();
        // CallSessionImpl.registerCustomCodecFormats(rtpManager);
        // rtpManager.dispose();
    }

    /**
     * Register in JMF the custom packages we provide
     */
    public void registerCustomPackages()
    {
        @SuppressWarnings("unchecked")
        Vector<String> currentPackagePrefix
            = PackageManager.getProtocolPrefixList();

        for (String className : CUSTOM_PACKAGES)
        {
            // linear search in a loop, but it doesn't have to scale since the
            // list is always short
            if (!currentPackagePrefix.contains(className))
            {
                currentPackagePrefix.add(className);
                logger.debug("Adding package  : " + className);
            }
        }

        PackageManager.setProtocolPrefixList(currentPackagePrefix);
        PackageManager.commitProtocolPrefixList();
        logger.debug("Registering new protocol prefix list : "
            + currentPackagePrefix);
    }

    public MediaFormat[] getAvailableEncodings(MediaType type)
    {
        return MediaUtils.getMediaFormats(type);
    }

    /**
     * Gets the supported <tt>MediaFormat</tt>s i.e. the enabled available
     * <tt>MediaFormat</tt>s sorted in decreasing priority.
     *
     * @param type the <tt>MediaType</tt> of the supported <tt>MediaFormat</tt>s
     * to get
     * @return an array of the supported <tt>MediaFormat</tt>s i.e. the enabled
     * available <tt>MediaFormat</tt>s sorted in decreasing priority
     */
    public MediaFormat[] getSupportedEncodings(MediaType type)
    {
        Set<MediaFormat> supportedEncodings;

        switch (type)
        {
        case AUDIO:
            if (supportedAudioEncodings == null)
                supportedAudioEncodings = updateSupportedEncodings(type);
            supportedEncodings = supportedAudioEncodings;
            break;
        case VIDEO:
            if (supportedVideoEncodings == null)
                supportedVideoEncodings = updateSupportedEncodings(type);
            supportedEncodings = supportedVideoEncodings;
            break;
        default:
            return MediaUtils.EMPTY_MEDIA_FORMATS;
        }
        return
            supportedEncodings
                .toArray(new MediaFormat[supportedEncodings.size()]);
    }

    /**
     * Compares the two formats for order. Returns a negative integer, zero, or
     * a positive integer as the first format has been assigned a preference
     * higher, equal to, or greater than the one of the second.
     * <p>
     *
     * @param enc1 the first format to compare for preference.
     * @param enc2 the second format to compare for preference.
     *
     * @return a negative integer, zero, or a positive integer as the first
     *         format has been assigned a preference higher, equal to, or
     *         greater than the one of the second.
     */
    private int compareEncodingPreferences(MediaFormat enc1, MediaFormat enc2)
    {
        Integer pref1 = encodingPreferences.get(enc1);
        int pref1IntValue = (pref1 == null) ? 0 : pref1;

        Integer pref2 = encodingPreferences.get(enc2);
        int pref2IntValue = (pref2 == null) ? 0 : pref2;

        return pref2IntValue - pref1IntValue;
    }
}
