/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.sdp.*;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * Simple configuration of encoding priorities.
 * 
 * @author Damian Minkov
 */
public class EncodingConfiguration
{
    private final Logger logger = Logger.getLogger(EncodingConfiguration.class);

    private static final String PROP_SDP_PREFERENCE =
        "net.java.sip.communicator.impl.media.sdppref";

    /**
     * SDP Codes of all video formats that JMF supports.
     */
    private final String[] availableVideoEncodings = new String[]
    { Integer.toString(Constants.H264_RTP_SDP),
    // javax.media.format.VideoFormat.H263_RTP
        Integer.toString(SdpConstants.H263),
        // javax.media.format.VideoFormat.JPEG_RTP
        Integer.toString(SdpConstants.JPEG),
        // javax.media.format.VideoFormat.H261_RTP
        Integer.toString(SdpConstants.H261) };

    /**
     * SDP Codes of all audio formats that JMF supports.
     */
    private final String[] availableAudioEncodings = new String[]
    {
    // ILBC
        Integer.toString(97),
        // javax.media.format.AudioFormat.G723_RTP
        Integer.toString(SdpConstants.G723),
        // javax.media.format.AudioFormat.GSM_RTP;
        Integer.toString(SdpConstants.GSM),
        // javax.media.format.AudioFormat.ULAW_RTP;
        Integer.toString(SdpConstants.PCMU),
        // javax.media.format.AudioFormat.DVI_RTP;
        Integer.toString(SdpConstants.DVI4_8000),
        // javax.media.format.AudioFormat.DVI_RTP;
        Integer.toString(SdpConstants.DVI4_16000),
        // javax.media.format.AudioFormat.ALAW;
        Integer.toString(SdpConstants.PCMA), Integer.toString(110),
        // javax.media.format.AudioFormat.G728_RTP;
        Integer.toString(SdpConstants.G728)
    // javax.media.format.AudioFormat.G729_RTP
        // g729 is not suppported by JMF
        // Integer.toString(SdpConstants.G729)
        };

    private final Set<String> supportedVideoEncodings =
        new TreeSet<String>(new EncodingComparator());

    private final Set<String> supportedAudioEncodings =
        new TreeSet<String>(new EncodingComparator());

    /**
     * That's where we keep format preferences matching SDP formats to integers.
     * We keep preferences for both audio and video formats here in case we'd
     * ever need to compare them to one another. In most cases however both
     * would be decorelated and other components (such as the UI) should present
     * them separately.
     */
    private final Map<String, Integer> encodingPreferences =
        new Hashtable<String, Integer>();

    private static final String[] customCodecs =
        new String[]
        {
            FMJConditionals.FMJ_CODECS ? "net.sf.fmj.media.codec.audio.alaw.Encoder"
                : "net.java.sip.communicator.impl.media.codec.audio.alaw.JavaEncoder",
            FMJConditionals.FMJ_CODECS ? "net.sf.fmj.media.codec.audio.alaw.DePacketizer"
                : "net.java.sip.communicator.impl.media.codec.audio.alaw.DePacketizer",
            FMJConditionals.FMJ_CODECS ? "net.sf.fmj.media.codec.audio.alaw.Packetizer"
                : "net.java.sip.communicator.impl.media.codec.audio.alaw.Packetizer",
            FMJConditionals.FMJ_CODECS ? "net.sf.fmj.media.codec.audio.ulaw.Packetizer"
                : "net.java.sip.communicator.impl.media.codec.audio.ulaw.Packetizer",
            "net.java.sip.communicator.impl.media.codec.video.h264.NativeEncoder",
            "net.java.sip.communicator.impl.media.codec.video.h264.NativeDecoder",
            "net.java.sip.communicator.impl.media.codec.video.ImageScaler",
            "net.java.sip.communicator.impl.media.codec.audio.speex.JavaEncoder",
            "net.java.sip.communicator.impl.media.codec.audio.speex.JavaDecoder",
            "net.java.sip.communicator.impl.media.codec.audio.ilbc.JavaEncoder",
            "net.java.sip.communicator.impl.media.codec.audio.ilbc.JavaDecoder" };

    /**
     * Custom Packages provided by Sip-Communicator
     */
    private static final String[] customPackages = new String[]
    { // datasource for low latency ALSA input
        "net.java.sip.communicator.impl", "net.sf.fmj" };

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
    private void initializeFormatPreferences()
    {
        // first init default preferences
        // video
        setEncodingPreference(Constants.H264_RTP_SDP, 1100);
        setEncodingPreference(SdpConstants.H263, 1000);
        setEncodingPreference(SdpConstants.JPEG, 950);
        setEncodingPreference(SdpConstants.H261, 800);

        // audio
        setEncodingPreference(SdpConstants.PCMU, 650);
        setEncodingPreference(SdpConstants.PCMA, 600);
        setEncodingPreference(97, 500);
        setEncodingPreference(SdpConstants.GSM, 450);
        setEncodingPreference(110, 350);
        setEncodingPreference(SdpConstants.DVI4_8000, 300);
        setEncodingPreference(SdpConstants.DVI4_16000, 250);
        setEncodingPreference(SdpConstants.G723, 150);
        setEncodingPreference(SdpConstants.G728, 100);

        // now override with those that are specified by the user.
        ConfigurationService confService =
            MediaActivator.getConfigurationService();

        List<String> sdpPreferences =
            confService.getPropertyNamesByPrefix(PROP_SDP_PREFERENCE, false);

        for (String pName : sdpPreferences)
        {
            String prefStr = confService.getString(pName);
            String fmtName =
                pName.substring(pName.lastIndexOf('.') + 1).replaceAll("sdp",
                    "");
            int preference = -1;
            int fmt = -1;
            try
            {
                preference = Integer.parseInt(prefStr);
                fmt = Integer.parseInt(fmtName);
            }
            catch (NumberFormatException exc)
            {
                logger.warn("Failed to parse format (" + fmtName
                    + ") or preference(" + prefStr + ").", exc);
                continue;
            }

            setEncodingPreference(fmt, preference);
        }

        // now update the arrays so that they are returned by order of
        // preference.
        updateSupportedEncodings();
    }

    /**
     * Updates the codecs in the supported sets according preferences in
     * encodingPreferences. If value is "0" the codec is disabled.
     */
    private void updateSupportedEncodings()
    {
        for (int i = 0; i < availableAudioEncodings.length; i++)
        {
            String ac = availableAudioEncodings[i];

            Integer pref1 = encodingPreferences.get(ac);
            int pref1IntValue = (pref1 == null) ? 0 : pref1.intValue();

            if (pref1IntValue > 0)
                supportedAudioEncodings.add(ac);
            else
                supportedAudioEncodings.remove(ac);
        }

        for (int i = 0; i < availableVideoEncodings.length; i++)
        {
            String ac = availableVideoEncodings[i];

            Integer pref1 = encodingPreferences.get(ac);
            int pref1IntValue = (pref1 == null) ? 0 : pref1.intValue();

            if (pref1IntValue > 0)
                supportedVideoEncodings.add(ac);
            else
                supportedVideoEncodings.remove(ac);
        }
    }

    /**
     * Updates the codecs in the set according preferences in
     * encodingPreferences. If value is "0" the codec is disabled.
     */
    public String[] updateEncodings(List<String> encs)
    {
        Set<String> result = new TreeSet<String>(new EncodingComparator());
        for (String c : encs)
        {
            Integer pref1 = encodingPreferences.get(c);
            int pref1IntValue = (pref1 == null) ? 0 : pref1.intValue();

            if (pref1IntValue > 0)
                result.add(c);
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Initializes codecs and formats.
     */
    public void initialize()
    {
        // init configuration
        initializeFormatPreferences();

        // register our own datasources
        registerCustomPackages();

        // register our custom codecs
        registerCustomCodecs();
    }

    /**
     * Sets <tt>pref</tt> as the preference associated with <tt>encoding</tt>.
     * Use this method for both audio and video encodings and don't worry if
     * preferences are equal since we rarely need to compare prefs of video
     * encodings to those of audio encodings.
     * 
     * @param encoding the SDP int of the encoding whose pref we're setting.
     * @param pref a positive int indicating the preference for that encoding.
     */
    private void setEncodingPreference(int encoding, int pref)
    {
        this.encodingPreferences.put(Integer.toString(encoding), pref);
    }

    /**
     * Sets <tt>pref</tt> as the preference associated with <tt>encoding</tt>.
     * Use this method for both audio and video encodings and don't worry if
     * preferences are equal since we rarely need to compare prefs of video
     * encodings to those of audio encodings.
     * 
     * @param encoding a string containing the SDP int of the encoding whose
     *            pref we're setting.
     * @param pref a positive int indicating the preference for that encoding.
     */
    public void setPriority(String encoding, int priority)
    {
        this.encodingPreferences.put(encoding, priority);

        // save the settings
        MediaActivator.getConfigurationService().setProperty(
            PROP_SDP_PREFERENCE + ".sdp" + encoding, priority);

        updateSupportedEncodings();
    }

    public int getPriority(String encoding)
    {
        return encodingPreferences.get(encoding);
    }

    /**
     * Register in JMF the custom codecs we provide
     */
    private void registerCustomCodecs()
    {
        // use a set to check if the codecs are already
        // registered in jmf.properties
        Set<String> registeredPlugins = new HashSet<String>();

        registeredPlugins.addAll(PlugInManager.getPlugInList(null, null,
            PlugInManager.CODEC));

        for (int i = 0; i < customCodecs.length; i++)
        {
            String className = customCodecs[i];

            if (registeredPlugins.contains(className))
            {
                logger.debug("Codec : " + className + " is already registered");
            }
            else
            {
                try
                {
                    Object instance = Class.forName(className).newInstance();

                    boolean result =
                        PlugInManager.addPlugIn(className, ((Codec) instance)
                            .getSupportedInputFormats(), ((Codec) instance)
                            .getSupportedOutputFormats(null),
                            PlugInManager.CODEC);
                    logger.debug("Codec : " + className
                        + " is succsefully registered : " + result);
                }
                catch (Throwable ex)
                {
                    logger.debug("Codec : " + className
                        + " is NOT succsefully registered", ex);
                }
            }
        }

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
    private void registerCustomPackages()
    {
        Vector<String> currentPackagePrefix =
            PackageManager.getProtocolPrefixList();

        for (int i = 0; i < customPackages.length; i++)
        {
            String className = customPackages[i];

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

    public String[] getAvailableVideoEncodings()
    {
        return availableAudioEncodings;
    }

    public String[] getAvailableAudioEncodings()
    {
        return availableVideoEncodings;
    }

    public String[] getSupportedVideoEncodings()
    {
        return supportedVideoEncodings
            .toArray(new String[supportedVideoEncodings.size()]);
    }

    public String[] getSupportedAudioEncodings()
    {
        return supportedAudioEncodings
            .toArray(new String[supportedAudioEncodings.size()]);
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
    private int compareEncodingPreferences(String enc1, String enc2)
    {
        Integer pref1 = encodingPreferences.get(enc1);
        int pref1IntValue = (pref1 == null) ? 0 : pref1.intValue();

        Integer pref2 = encodingPreferences.get(enc2);
        int pref2IntValue = (pref2 == null) ? 0 : pref2.intValue();

        return pref2IntValue - pref1IntValue;
    }

    /**
     * Comaparator sorting the sets according the settings in
     * encodingPreferences.
     */
    private class EncodingComparator
        implements Comparator<String>
    {
        public int compare(String s1, String s2)
        {
            return compareEncodingPreferences(s1, s2);
        }
    }
}
