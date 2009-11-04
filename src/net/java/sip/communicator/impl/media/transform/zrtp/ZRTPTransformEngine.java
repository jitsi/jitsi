/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import gnu.java.zrtp.*;
import gnu.java.zrtp.zidfile.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.impl.media.transform.*;
import net.java.sip.communicator.impl.media.transform.srtp.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.util.*;

import java.io.*;
import java.util.EnumSet;

/**
 * JMF extension/connector to support GNU ZRTP4J.
 *
 * ZRTP was developed by Phil Zimmermann and provides functions to negotiate
 * keys and other necessary data (crypto data) to set-up the Secure RTP (SRTP)
 * crypto context. Refer to Phil's ZRTP specification at his
 * <a href="http://zfoneproject.com/">Zfone project</a> site to get more
 * detailed information about the capabilities of ZRTP.
 *
 * <h3>Short overview of the ZRTP4J implementation</h3>
 *
 * ZRTP is a specific protocol to negotiate encryption algorithms and the
 * required key material. ZRTP uses a RTP session to exchange its protocol
 * messages.
 *
 * A complete GNU ZRTP4J implementation consists of two parts, the GNU ZRTP4J
 * core and specific code that binds the GNU ZRTP core to the underlying
 * RTP/SRTP stack and the operating system:
 * <ul>
 * <li> The GNU ZRTP core is independent of a specific RTP/SRTP stack and the
 * operationg system and consists of the ZRTP protocol state engine, the ZRTP
 * protocol messages, and the GNU ZRTP4J engine. The GNU ZRTP4J engine provides
 * methods to setup ZRTP message and to analyze received ZRTP messages, to
 * compute the crypto data required for SRTP, and to maintain the required
 * hashes and HMAC. </li>
 * <li> The second part of an implementation is specific <em>glue</em> code
 * the binds the GNU ZRTP core to the actual RTP/SRTP implementation and other
 * operating system specific services such as timers. </li>
 * </ul>
 *
 * The GNU ZRTP4J core uses a callback interface class (refer to ZrtpCallback)
 * to access RTP/SRTP or operating specific methods, for example to send data
 * via the RTP/SRTP stack, to access timers, provide mutex handling, and to
 * report events to the application.
 *
 * <h3>The ZRTPTransformEngine</h3>
 *
 * ZRTPTransformEngine implements code that is specific to the JMF
 * implementation.
 *
 * To perform its tasks ZRTPTransformEngine
 * <ul>
 * <li> extends specific classes to hook into the JMF RTP methods and the
 * RTP/SRTP send and receive queues </li>
 * <li> implements the ZrtpCallback interface to provide to enable data send and
 * receive other specific services (timer to GNU ZRTP4J </li>
 * <li> provides ZRTP specific methods that applications may use to control and
 * setup GNU ZRTP </li>
 * <li> can register and use an application specific callback class (refer to
 * ZrtpUserCallback) </li>
 * </ul>
 *
 * After instantiating a GNU ZRTP4J session (see below for a short example)
 * applications may use the ZRTP specific methods of ZRTPTransformEngine to
 * control and setup GNU ZRTP, for example enable or disable ZRTP processing or
 * getting ZRTP status information.
 *
 * GNU ZRTP4J provides a ZrtpUserCallback class that an application may extend
 * and register with ZRTPTransformEngine. GNU ZRTP4J and ZRTPTransformEngine use
 * the ZrtpUserCallback methods to report ZRTP events to the application. The
 * application may display this information to the user or act otherwise.
 *
 * The following figure depicts the relationships between ZRTPTransformEngine,
 * JMF implementation, the GNU ZRTP4J core, and an application that provides an
 * ZrtpUserCallback class.
 *
 * <pre>
 *
 *                  +---------------------------+
 *                  |  ZrtpTransformConnector   |
 *                  | extends TransformConnector|
 *                  | implements RTPConnector   |
 *                  +---------------------------+
 *                                |
 *                                | uses
 *                                |
 *  +----------------+      +-----+---------------+
 *  |  Application   |      |                     |      +----------------+
 *  |  instantiates  | uses | ZRTPTransformEngine | uses |                |
 *  | a ZRTP Session +------+    implements       +------+   GNU ZRTP4J   |
 *  |  and provides  |      |   ZrtpCallback      |      |      core      |
 *  |ZrtpUserCallback|      |                     |      | implementation |
 *  +----------------+      +---------------------+      |  (ZRtp et al)  |
 *                                                       |                |
 *                                                       +----------------+
 * </pre>
 *
 * The following short code snippets show how an application could instantiate a
 * ZrtpTransformConnector, get the ZRTP4J engine and initialize it. Then the
 * code get a RTP manager instance and initializes it with the
 * ZRTPTransformConnector. Plase note: setting the target must be done with the
 * connector, not with the RTP manager.
 *
 * <pre>
 * ...
 *   transConnector = (ZrtpTransformConnector)TransformManager
 *                                                  .createZRTPConnector(sa);
 *   zrtpEngine = transConnector.getEngine();
 *   zrtpEngine.setUserCallback(new MyCallback());
 *   if (!zrtpEngine.initialize(&quot;test_t.zid&quot;))
 *       System.out.println(&quot;iniatlize failed&quot;);
 *
 *   // initialize the RTPManager using the ZRTP connector
 *
 *   mgr = RTPManager.newInstance();
 *   mgr.initialize(transConnector);
 *
 *   mgr.addSessionListener(this);
 *   mgr.addReceiveStreamListener(this);
 *
 *   transConnector.addTarget(target);
 *   zrtpEngine.startZrtp();
 *
 *   ...
 * </pre>
 *
 * The <em>demo</em> folder contains a small example that shows how to use GNU
 * ZRTP4J.
 *
 * This ZRTPTransformEngine documentation shows the ZRTP specific extensions and
 * describes overloaded methods and a possible different behaviour.
 *
 * @author Werner Dittmann &lt;Werner.Dittmann@t-online.de>
 *
 */
public class ZRTPTransformEngine
    implements  TransformEngine,
                PacketTransformer,
                ZrtpCallback
{
    /**
     * Very simple Timeout provider class.
     *
     * This very simple timeout provider can handle one timeout request at
     * one time only. A second request would overwrite the first one and would
     * lead to unexpected results.
     *
     * @author Werner Dittmann <Werner.Dittmann@t-online.de>
     */
    private static class TimeoutProvider extends Thread
    {
        public TimeoutProvider(String name)
        {
            super(name);
        }

        private ZRTPTransformEngine executor;

        private long nextDelay = 0;

        private boolean newTask = false;

        private boolean stop = false;

        private final Object sync = new Object();

        public synchronized void requestTimeout(long delay,
                                                ZRTPTransformEngine tt)
        {
            synchronized (sync)
            {
                executor = tt;
                nextDelay = delay;
                newTask = true;
                sync.notifyAll();
            }
        }

        public void stopRun()
        {
            synchronized (sync)
            {
                stop = true;
                sync.notifyAll();
            }
        }

        public void cancelRequest()
        {
            synchronized (sync)
            {
                newTask = false;
                sync.notifyAll();
            }
        }

        public void run()
        {
            while (!stop)
            {
                synchronized (sync)
                {
                    while (!newTask && !stop)
                    {
                        try
                        {
                            sync.wait();
                        }
                        catch (InterruptedException e)
                        {
                            // e.printStackTrace();
                        }
                    }
                }
                long currentTime = System.currentTimeMillis();
                long endTime = currentTime + nextDelay;
                synchronized (sync) {
                    while ((currentTime < endTime) && newTask && !stop)
                    {
                        try
                        {
                            sync.wait(endTime - currentTime);
                        }
                        catch (InterruptedException e)
                        {
                            //e.printStackTrace();
                        }
                        currentTime = System.currentTimeMillis();
                    }
                }
                if (newTask && !stop)
                {
                    newTask = false;
                    executor.handleTimeout();
                }
            }
        }
    }

    private static final Logger logger
        = Logger.getLogger(ZRTPTransformEngine.class);

    /**
     * Each ZRTP packet has a fixed header of 12 bytes.
     */
    protected static final int ZRTP_PACKET_HEADER = 12;

    /**
     * This is the own ZRTP connector, required to send ZRTP packets
     * via the DatagramSocket.
     * (Note: in order to further multistream support this should be
     *  replaced with a connector array; each connector would handle
     *  a stream)
     */
    private TransformConnector zrtpConnector = null;

    /**
     * We need Out and In SRTPTransformer to transform RTP to SRTP and
     * vice versa.
     */
    private PacketTransformer srtpOutTransformer = null;
    private PacketTransformer srtpInTransformer = null;

    /**
     * User callback class.
     */
    private SecurityEventManager securityEventManager = null;

    /**
     * The ZRTP engine.
     */
    private ZRtp zrtpEngine = null;

    /**
     * ZRTP engine enable flag (used for auto-enable at initialization)
     */
    private boolean enableZrtp = false;

    /**
     * Client ID string initialized with the name of the ZRTP4j library
     */
    private String clientIdString = ZrtpConstants.clientId;

    /**
     * SSRC identifier for the ZRTP packets
     */
    private int ownSSRC = 0;

    /**
     * ZRTP packet sequence number
     */
    private short senderZrtpSeqNo = 0;

    /**
     * The number of sent packets
     */
    private long sendPacketCount = 0;

    /**
     * The timeout provider instance
     * This is used for handling the ZRTP timers
     */
    private TimeoutProvider timeoutProvider = null;

    /**
     * The current condition of the ZRTP engine
     */
    private boolean started = false;

    /**
     * Construct a ZRTPTransformEngine.
     *
     */
    public ZRTPTransformEngine()
    {
        senderZrtpSeqNo = 1;    // should be a random number
    }

    /**
     * Returns an instance of <tt>ZRTPCTransformer</tt>.
     * 
     * @see TransformEngine#getRTCPTransformer()
     */
    public PacketTransformer getRTCPTransformer()
    {
        return new ZRTPCTransformer(this);
    }

    /**
     * Returns this RTPTransformer.
     * 
     * @see TransformEngine#getRTPTransformer()
     */
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /**
     * Engine initialization method.
     * Calling this for engine initialization and start it with auto-sensing
     * and a given configuration setting.
     *
     * @param zidFilename The ZID file name
     * @param config The configuration data
     * @return true if initialization fails, false if succeeds
     */
    public boolean initialize(String zidFilename, ZrtpConfigure config) {
        return initialize(zidFilename, true, config);
    }

    /**
     * Engine initialization method.
     * Calling this for engine initialization and start it with defined
     * auto-sensing and a default configuration setting.
     *
     * @param zidFilename The ZID file name
     * @param autoEnable If true start with auto-sensing mode. 
     * @return true if initialization fails, false if succeeds
     */
    public boolean initialize(String zidFilename, boolean autoEnable) {
        return initialize(zidFilename, autoEnable, null);
    }

    /**
     * Default engine initialization method.
     * 
     * Calling this for engine initialization and start it with auto-sensing
     * and default configuration setting.
     *
     * @param zidFilename The ZID file name
     * @return true if initialization fails, false if succeeds
     */
    public boolean initialize(String zidFilename) {
        return initialize(zidFilename, true, null);
    }

    /**
     * Custom engine initialization method.
     * This allows to explicit specify if the engine starts with auto-sensing
     * or not.
     *
     * @param zidFilename The ZID file name
     * @param autoEnable Set this true to start with auto-sensing and false to
     * disable it.
     * @return true if initialization fails, false if succeeds
     */
    public synchronized boolean initialize(String zidFilename, 
            boolean autoEnable, ZrtpConfigure config)
    {
        // Get a reference to the FileAccessService
        BundleContext bc = MediaActivator.getBundleContext();
        ServiceReference faServiceReference = bc.getServiceReference(
                FileAccessService.class.getName());
        FileAccessService faService = (FileAccessService)
                bc.getService(faServiceReference);

        File file = null;
        try
        {
            // Create the zid file
            file = faService.getPrivatePersistentFile(zidFilename);
        }
        catch (Exception e)
        {
            logger.warn("Failed to create the zid file.");

            if (logger.isDebugEnabled())
                logger.debug("Failed to create the zid file.", e);
        }

        String zidFilePath = null;
        try
        {
            if (file != null)
                // Get the absolute path of the created zid file
                zidFilePath = file.getAbsolutePath();
        }
        catch (SecurityException e)
        {
            if (logger.isDebugEnabled())
                logger.debug(
                    "Failed to obtain the absolute path of the zid file.", e);
        }

        ZidFile zf = ZidFile.getInstance();
        if (!zf.isOpen())
        {
            String fname;
            if (zidFilePath == null)
            {
                String home = System.getenv("HOME");
                String baseDir = (home != null) ? ((home) + ("/.")) : ".";
                fname = baseDir + "GNUZRTP4J.zid";
                zidFilename = fname;
            }
            else
            {
                zidFilename = zidFilePath;
            }

            if (zf.open(zidFilename) < 0)
            {
                return false;
            }
        }
        if (config == null) {
            config = new ZrtpConfigure();
            config.setStandardConfig();
        }

        zrtpEngine = new ZRtp(zf.getZid(), this, clientIdString, config);

        if (timeoutProvider == null)
        {
            timeoutProvider = new TimeoutProvider("ZRTP");
            timeoutProvider.setDaemon(true);
            timeoutProvider.start();
        }

        enableZrtp = autoEnable;
        return true;
    }

    /**
     * Start the ZRTP stack immediately, not autosensing mode.
     */
    public void startZrtp()
    {
        if (zrtpEngine != null)
        {
            zrtpEngine.startZrtpEngine();
            started = true;
        }
    }

    /**
     * Stop ZRTP engine.
     */
    public void stopZrtp()
    {
        if (zrtpEngine != null)
        {
            zrtpEngine.stopZrtp();
            zrtpEngine = null;
            started = false;
        }
    }

    /**
     * Cleanup function for any remaining timers
     */
    public void cleanup()
    {
        if (timeoutProvider != null)
        {
            timeoutProvider.stopRun();
            timeoutProvider = null;
        }
    }

    /**
     * Set the SSRC of the RTP transmitter stream.
     * 
     * ZRTP fills the SSRC in the ZRTP messages.
     * 
     * @param ssrc
     */
    public void setOwnSSRC(long ssrc) {
        ownSSRC = (int)(ssrc & 0xffffffff);
    }

    /**
     * The data output stream calls this method to transform outgoing
     * packets.
     * 
     * @see PacketTransformer#transform(RawPacket)
     */
    public RawPacket transform(RawPacket pkt)
    {
        /*
         * Never transform outgoing ZRTP (invalid RTP) packets.
         * check if the first byte of the received data
         * matches the defined ZRTP pattern 0x10
         */
        
        byte[] buffer = pkt.getBuffer();
        int offset = pkt.getOffset();
        if ((buffer[offset] & 0x10) == 0x10) 
        {
            return pkt;
        }

        // ZRTP needs the SSRC of the sending stream.
        if (enableZrtp && ownSSRC == 0)
        {
            ownSSRC = (int)(pkt.readUnsignedIntAsLong(8) & 0xffffffff);
        }

        // If SRTP is active then srtpTransformer is set, use it.
        sendPacketCount++;
        if (srtpOutTransformer == null)
        {
            return pkt;
        }

        return srtpOutTransformer.transform(pkt);
    }

    /**
     * The input data stream calls this method to transform
     * incoming packets.
     * 
     * @see PacketTransformer#reverseTransform(RawPacket)
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {

        // Check if we need to start ZRTP
        if (!started && enableZrtp && ownSSRC != 0)
        {
            startZrtp();
        }
        /*
         * Check if incoming packt is a ZRTP packet, if not treat
         * it as normal RTP packet and handle it accordingly.
         */
        byte[] buffer = pkt.getBuffer();
        int offset = pkt.getOffset();
        if ((buffer[offset] & 0x10) != 0x10) 
        {
            if (srtpInTransformer == null)
            {
                return pkt;
            }
            pkt = srtpInTransformer.reverseTransform(pkt);
            // if packet was valid (i.e. not null) and ZRTP engine started and
            // in Wait for Confirm2 Ack then emulate a Conf2Ack packet. 
            // See ZRTP specification chap. 5.6
            if (pkt != null && started
                && zrtpEngine.inState(ZrtpStateClass.ZrtpStates.WaitConfAck))
            {
                zrtpEngine.conf2AckSecure();
            }
            return pkt;
        }

        /*
         * If ZRTP is enabled process it. 
         * 
         * In any case return null because ZRTP packets must never reach 
         * the application.
         */
        if (enableZrtp && started)
        {
            ZrtpRawPacket zPkt = new ZrtpRawPacket(pkt);
            if (!zPkt.checkCrc())
            {
                securityEventManager.showMessage(ZrtpCodes.MessageSeverity.Warning,
                        EnumSet.of(ZrtpCodes.WarningCodes.WarningCRCmismatch));
                return null;
            }
            // Check if it is really a ZRTP packet, if not don't process it
            if (!zPkt.hasMagic())
            {
                return null;
            }
            byte[] extHeader = zPkt.getMessagePart();
            zrtpEngine.processZrtpMessage(extHeader, zPkt.getSSRC());
        }
        return null;
    }

    /**
     * The callback method required by the ZRTP implementation.
     * First allocate space to hold the complete ZRTP packet, copy
     * the message part in its place, the initalize the header, counter,
     * SSRC and crc.
     *
     * @param data The ZRTP packet data
     * @return true if sending succeeds, false if it fails
     */
    public boolean sendDataZRTP(byte[] data)
    {
        int totalLength = ZRTP_PACKET_HEADER + data.length;
        byte[] tmp = new byte[totalLength];
        System.arraycopy(data, 0, tmp, ZRTP_PACKET_HEADER, data.length);
        ZrtpRawPacket packet = new ZrtpRawPacket(tmp, 0, tmp.length);

        packet.setSSRC(ownSSRC);

        packet.setSeqNum(senderZrtpSeqNo++);

        packet.setCrc();

        try
        {
            zrtpConnector.getDataOutputStream().write(  packet.getBuffer(),
                                                        packet.getOffset(),
                                                        packet.getLength());
        }
        catch (IOException e)
        {
            logger.warn("Failed to send ZRTP data.");

            if (logger.isDebugEnabled())
                logger.debug("Failed to send ZRTP data.", e);

            return false;
        }
        return true;
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#srtpSecretsReady(
     *          gnu.java.zrtp.ZrtpSrtpSecrets,
     *          gnu.java.zrtp.ZrtpCallback.EnableSecurity)
     */
    public boolean srtpSecretsReady(ZrtpSrtpSecrets secrets,
                                    EnableSecurity part)
    {
        SRTPPolicy srtpPolicy = null;

        if (part == EnableSecurity.ForSender)
        {
            // To encrypt packets: initiator uses initiator keys,
            // responder uses responder keys
            // Create a "half baked" crypto context first and store it. This is
            // the main crypto context for the sending part of the connection.
            if (secrets.getRole() == Role.Initiator)
            {
                srtpPolicy = new SRTPPolicy(SRTPPolicy.AESCM_ENCRYPTION,
                        secrets.getInitKeyLen() / 8,    // key length
                        SRTPPolicy.HMACSHA1_AUTHENTICATION,
                        20,                             // auth key length
                        secrets.getSrtpAuthTagLen() / 8,// auth tag length
                        secrets.getInitSaltLen() / 8    // salt length
                );

                SRTPTransformEngine engine = new SRTPTransformEngine(secrets
                        .getKeyInitiator(), secrets.getSaltInitiator(),
                        srtpPolicy, srtpPolicy);

                srtpOutTransformer = engine.getRTPTransformer();
            }
            else
            {
                srtpPolicy = new SRTPPolicy(SRTPPolicy.AESCM_ENCRYPTION,
                        secrets.getRespKeyLen() / 8,    // key length
                        SRTPPolicy.HMACSHA1_AUTHENTICATION,
                        20,                             // auth key length
                        secrets.getSrtpAuthTagLen() / 8,// auth taglength
                        secrets.getRespSaltLen() / 8    // salt length
                );

                SRTPTransformEngine engine = new SRTPTransformEngine(secrets
                        .getKeyResponder(), secrets.getSaltResponder(),
                        srtpPolicy, srtpPolicy);
                srtpOutTransformer = engine.getRTPTransformer();
            }
        }

        if (part == EnableSecurity.ForReceiver)
        {
            // To decrypt packets: initiator uses responder keys,
            // responder initiator keys
            // See comment above.
            if (secrets.getRole() == Role.Initiator)
            {
                srtpPolicy = new SRTPPolicy(SRTPPolicy.AESCM_ENCRYPTION,
                        secrets.getRespKeyLen() / 8,    // key length
                        SRTPPolicy.HMACSHA1_AUTHENTICATION,
                        20,                             // auth key length
                        secrets.getSrtpAuthTagLen() / 8,// auth tag length
                        secrets.getRespSaltLen() / 8    // salt length
                );

                SRTPTransformEngine engine = new SRTPTransformEngine(secrets
                        .getKeyResponder(), secrets.getSaltResponder(),
                        srtpPolicy, srtpPolicy);
                srtpInTransformer = engine.getRTPTransformer();
            }
            else
            {
                srtpPolicy = new SRTPPolicy(SRTPPolicy.AESCM_ENCRYPTION,
                        secrets.getInitKeyLen() / 8,    // key length
                        SRTPPolicy.HMACSHA1_AUTHENTICATION,
                        20,                             // auth key length
                        secrets.getSrtpAuthTagLen() / 8,// auth tag length
                        secrets.getInitSaltLen() / 8    // salt length
                );

                SRTPTransformEngine engine = new SRTPTransformEngine(secrets
                        .getKeyInitiator(), secrets.getSaltInitiator(),
                        srtpPolicy, srtpPolicy);
                srtpInTransformer = engine.getRTPTransformer();
            }
        }
        return true;
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#srtpSecretsOn(java.lang.String,
     *                                               java.lang.String, boolean)
     */
    public void srtpSecretsOn(String c, String s, boolean verified)
    {
        if (securityEventManager != null)
        {
            securityEventManager.secureOn(c);
        }

        if (securityEventManager != null && s != null)
        {
            securityEventManager.showSAS(s, verified);
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#srtpSecretsOff(
     *                              gnu.java.zrtp.ZrtpCallback.EnableSecurity)
     */
    public void srtpSecretsOff(EnableSecurity part)
    {
        if (part == EnableSecurity.ForSender)
        {
            srtpOutTransformer = null;
        }

        if (part == EnableSecurity.ForReceiver)
        {
            srtpInTransformer = null;
        }

        if (securityEventManager != null)
        {
            securityEventManager.secureOff();
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#activateTimer(int)
     */
    public int activateTimer(int time)
    {
        if (timeoutProvider != null)
        {
            timeoutProvider.requestTimeout(time, this);
        }

        return 1;
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#cancelTimer()
     */
    public int cancelTimer()
    {
        if (timeoutProvider != null)
        {
            timeoutProvider.cancelRequest();
        }
        return 1;
    }

    /**
     * Timeout handling function.
     * Delegates the handling to the ZRTP engine.
     */
    public void handleTimeout()
    {
        if (zrtpEngine != null)
        {
            zrtpEngine.processTimeout();
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#sendInfo(
     *                                 gnu.java.zrtp.ZrtpCodes.MessageSeverity,
     *                                 java.util.EnumSet)
     */
    public void sendInfo(ZrtpCodes.MessageSeverity severity, EnumSet<?> subCode)
    {
        if (securityEventManager != null)
        {
            securityEventManager.showMessage(severity, subCode);
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#zrtpNegotiationFailed(
     *                              gnu.java.zrtp.ZrtpCodes.MessageSeverity,
     *                              java.util.EnumSet)
     */
    public void zrtpNegotiationFailed(ZrtpCodes.MessageSeverity severity,
                                      EnumSet<?> subCode)
    {
        if (securityEventManager != null)
        {
            securityEventManager.zrtpNegotiationFailed(severity, subCode);
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#zrtpNotSuppOther()
     */
    public void zrtpNotSuppOther()
    {
        if (securityEventManager != null)
        {
            securityEventManager.zrtpNotSuppOther();
        }
    }

    /**
     * @see gnu.java.zrtp.ZrtpCallback#zrtpAskEnrollment(java.lang.String)
     */
    public void zrtpAskEnrollment(String info)
    {
        if (securityEventManager != null)
        {
            securityEventManager.zrtpAskEnrollment(info);
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#zrtpInformEnrollment(java.lang.String)
     */
    public void zrtpInformEnrollment(String info)
    {
        if (securityEventManager != null)
        {
            securityEventManager.zrtpInformEnrollment(info);
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#signSAS(java.lang.String)
     */
    public void signSAS(String sas)
    {
        if (securityEventManager != null)
        {
            securityEventManager.signSAS(sas);
        }
    }

    /**
     * 
     * @see gnu.java.zrtp.ZrtpCallback#checkSASSignature(java.lang.String)
     */
    public boolean checkSASSignature(String sas)
    {
        return ((securityEventManager != null)
                        ? securityEventManager.checkSASSignature(sas)
                        : false);
    }

    /**
     * Sets the enableZrtp flag.
     *
     * @param onOff The value for the enableZrtp flag.
     */
    public void setEnableZrtp(boolean onOff)
    {
        enableZrtp = onOff;
    }

    /**
     * Returns the enableZrtp flag.
     *
     * @return the enableZrtp flag.
     */
    public boolean isEnableZrtp()
    {
        return enableZrtp;
    }

    /**
     * Set the SAS as verified internally if the user confirms it
     */
    public void SASVerified()
    {
        if (zrtpEngine != null)
            zrtpEngine.SASVerified();
    }

    /**
     * Resets the internal engine SAS verified flag
     */
    public void resetSASVerified()
    {
        if (zrtpEngine != null)
            zrtpEngine.resetSASVerified();
    }

    /**
     * Method called when the user requests through GUI to switch a secured call
     * to unsecure mode. Just forwards the request to the Zrtp class.
     */
    public void requestGoClear()
    {
//        if (zrtpEngine != null)
//            zrtpEngine.requestGoClear();
    }

    /**
     * Method called when the user requests through GUI to switch a previously
     * unsecured call back to secure mode. Just forwards the request to the
     * Zrtp class.
     */
    public void requestGoSecure()
    {
//        if (zrtpEngine != null)
//            zrtpEngine.requestGoSecure();
    }

    /**
     * Sets the auxilliary secret data
     *
     * @param data The auxilliary secret data
     */
    public void setAuxSecret(byte[] data) 
    {
        if (zrtpEngine != null)
            zrtpEngine.setAuxSecret(data);
    }


    /**
     * Sets the PBX secret data
     *
     * @param data The PBX secret data
     */
    public void setPbxSecret(byte[] data) {
        if (zrtpEngine != null)
            zrtpEngine.setPbxSecret(data);
    }

    /**
     * Sets the client ID
     *
     * @param id The client ID
     */
    public void setClientId(String id)
    {
        clientIdString = id;
    }

    /**
     * Gets the Hello packet Hash
     *
     * @return the Hello packet hash
     */
    public String getHelloHash()
    {
        if (zrtpEngine != null)
            return zrtpEngine.getHelloHash();
        else
            return new String();
    }

    /**
     * Gets the multistream params
     *
     * @return the multistream params
     */
    public byte[] getMultiStrParams()
    {
        if (zrtpEngine != null)
            return zrtpEngine.getMultiStrParams();
        else
            return new byte[0];
    }

    /**
     * Sets the multistream params
     * (The multistream part needs further development)
     * @param parameters the multistream params
     */
    public void setMultiStrParams(byte[] parameters)
    {
        if (zrtpEngine != null) {
            zrtpEngine.setMultiStrParams(parameters);
        }
    }

    /**
     * Gets the multistream flag
     * (The multistream part needs further development)
     * @return the multistream flag
     */
    public boolean isMultiStream()
    {
        return ((zrtpEngine != null) ? zrtpEngine.isMultiStream() : false);
    }

    /**
     * Used to accept a PBX enrollment request
     * (The PBX part needs further development)
     * @param accepted The boolean value indicating if the request is accepted
     */
    public void acceptEnrollment(boolean accepted)
    {
        if (zrtpEngine != null)
            zrtpEngine.acceptEnrollment(accepted);
    }

    /**
     * Sets signature data for the Confirm packets
     *
     * @param data the signature data
     * @return true if signature data was successfully set
     */
    public boolean setSignatureData(byte[] data)
    {
        return ((zrtpEngine != null) ? zrtpEngine.setSignatureData(data)
                : false);
    }

    /**
     * Gets signature data
     *
     * @return the signature data
     */
    public byte[] getSignatureData()
    {
        if (zrtpEngine != null)
            return zrtpEngine.getSignatureData();
        else
            return new byte[0];
    }

    /**
     * Gets signature length
     *
     * @return the signature length
     */
    public int getSignatureLength()
    {
        return ((zrtpEngine != null) ? zrtpEngine.getSignatureLength() : 0);
    }

    /**
     * Sets the PBX enrollment flag (see chapter 8.3 of ZRTP standards)
     * (The PBX part needs further development)
     * @param yesNo The PBX enrollment flag
     */
    public void setPBXEnrollment(boolean yesNo)
    {
        if (zrtpEngine != null)
            zrtpEngine.setPBXEnrollment(yesNo);
    }

    /**
     * Method called by the Zrtp class as result of a GoClear request from the
     * other peer. An explicit user confirmation is needed before switching to
     * unsecured mode. This is asked through the user callback.
     */
    public void handleGoClear()
    {
        securityEventManager.confirmGoClear();
    }

    /**
     * Sets the RTP connector using this ZRTP engine
     * (This method should be changed to an addConnector to a connector array
     *  managed by the engine for implementing multistream mode)
     *
     * @param connector the connector to set
     */
    public void setConnector(TransformConnector connector)
    {
        zrtpConnector = connector;
    }

    /**
     * Sets the user callback class used to maintain the GUI ZRTP part
     *
     * @param ub The user callback class
     */
    public void setUserCallback(SecurityEventManager ub)
    {
        securityEventManager = ub;
    }

    /**
     * Returns the current status of the ZRTP engine
     *
     * @return the current status of the ZRTP engine
     */
    public boolean isStarted()
    {
       return started;
    }

    /**
     * Gets the user callback used to manage the GUI part of ZRTP
     *
     * @return the user callback
     */
    public SecurityEventManager getUserCallback()
    {
        return securityEventManager;
    }

    /**
     * Get other party's ZID (ZRTP Identifier) data
     *
     * This functions returns the other party's ZID that was receivied 
     * during ZRTP processing. 
     *
     * The ZID data can be retrieved after ZRTP receive the first Hello
     * packet from the other party. The application may call this method
     * for example during SAS processing in showSAS(...) user callback
     * method.
     *
     * @return the ZID data as byte array.
     */
    public byte[] getZid()
    {
         return ((zrtpEngine != null) ? zrtpEngine.getZid() : null);
    }
}
