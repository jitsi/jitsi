/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;

import javax.media.*;
import javax.media.control.FormatControl;
import javax.media.format.*;
import javax.media.protocol.*;

import gnu.java.zrtp.utils.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.util.*;

/**
 * <tt>ZrtpFortunaEntropyGatherer</tt> initializes the Fortuna PRNG with entropy
 * data.
 *
 * <tt>ZrtpFortunaEntropyGatherer</tt> gets the media device configuration and
 * checks which media systems are available. It then reads some data from media
 * input (capture) devices and uses this data to seed the Fortuna PRNG. The
 * <tt>ZrtpFortuna</tt> PRNG is a singleton and all other methods that require
 * random data shall use this singleton.
 *
 * Use <tt>ZrtpFortunaEntropyGatherer</tt> during startup and initialization
 * phase of Jitsi but after initialization of the media devices to get entropy
 * data at the earliest point. Also make sure that entropy data is read from
 * local sources only and that entropy data is never send out (via networks
 * for example).
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 * @author Lyubomir Marinov
 */
public class ZrtpFortunaEntropyGatherer
{
    /**
     * The <tt>Logger</tt> used by <tt>GatherEntropy</tt>
     * class for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ZrtpFortunaEntropyGatherer.class);

    /**
     * Device config to look for capture devices.
     */
    private final DeviceConfiguration deviceConfiguration;

    /**
     * Other methods shall/may check this to see if Fortuna was seeded with
     * entropy.
     */
    private static boolean entropyOk = false;

    /**
     * Number of gathered entropy bytes.
     */
    private int gatheredEntropy = 0;

    /**
     * How many bytes to gather. This number depends on sample rate, sample
     * size, number of channels and number of audio seconds to use for random
     * data.
     */
    private int bytesToGather = 0;

    /**
     * Bytes per 20ms time slice.
     */
    private int bytes20ms = 0;

    /**
     * How many seconds of audio to read.
     *
     */
    private static final int NUM_OF_SECONDS = 2;

    /**
     * Constructor.
     *
     * @param deviceConfiguration <tt>DeviceConfiguration</tt> needed to
     * initialize <tt>GatherEntropy</tt>
     */
    public ZrtpFortunaEntropyGatherer(DeviceConfiguration deviceConfiguration)
    {
        this.deviceConfiguration = deviceConfiguration;
    }

    /**
     * Get status of entropy flag.
     *
     * @return Status if entropy was gathered and set in Fortuna PRNG.
     */
    public static boolean isEntropyOk()
    {
        return entropyOk;
    }

    /**
     * @return the number of gathered entropy bytes.
     */
    protected int getGatheredEntropy()
    {
        return gatheredEntropy;
    }
    /**
     * Set entropy to ZrtpFortuna singleton.
     *
     * The methods reads entropy data and seeds the ZrtpFortuna singleton.
     * The methods seeds the first pool (0) of Fortuna to make sure that
     * this entropy is always used.
     *
     * @return true if entropy data was available, false otherwise.
     */
    public boolean setEntropy()
    {
        boolean retValue = false;
        GatherAudio gatherer = new GatherAudio();
        retValue = gatherer.prepareAudioEntropy();
        if (retValue)
            gatherer.start();
        return retValue;
    }

    private class GatherAudio
        extends Thread
        implements BufferTransferHandler
    {
        /**
         * The PortAudio <tt>DataSource</tt> which provides
         * {@link #audioStream}.
         */
        private DataSource dataSource = null;

        /**
         * The <tt>PortAudioStream</tt> from which audio data is captured.
         */
        private SourceStream audioStream = null;

        /**
         * The next three elements control the push buffer that Javasound
         * uses.
         */
        private final Buffer firstBuf = new Buffer();
        private boolean bufferAvailable = false;
        private final Object bufferSync = new Object();

        /**
         * Prepares to read entropy data from portaudio capture device.
         *
         * The method gets an PortAudio instance with a set of capture
         * parameters.
         *
         * @return True if the PortAudio input stream is available.
         */
        private boolean prepareAudioEntropy()
        {
            CaptureDeviceInfo audioCaptureDevice =
                    deviceConfiguration.getAudioCaptureDevice();
            if (audioCaptureDevice == null)
                return false;

            MediaLocator audioCaptureDeviceLocator
                = audioCaptureDevice.getLocator();

            if (audioCaptureDeviceLocator == null)
                return false;

            try
            {
                dataSource = Manager.createDataSource(audioCaptureDeviceLocator);
            }
            catch (NoDataSourceException e)
            {
                logger.warn("No data source during entropy preparation", e);
                return false;
            }
            catch (IOException e)
            {
                logger.warn("Got an IO Exception during entropy preparation", e);
                return false;
            }
            FormatControl fc = ((CaptureDevice)dataSource).getFormatControls()[0];
            AudioFormat af = (AudioFormat)fc.getFormat();
            int framesToRead = (int)(af.getSampleRate() * NUM_OF_SECONDS);
            int frameSize = (af.getSampleSizeInBits() / 8) * af.getChannels();
            bytesToGather = framesToRead * frameSize;
            bytes20ms = frameSize * (int)(af.getSampleRate() /50);

            if (dataSource instanceof PullBufferDataSource)
            {
                audioStream = ((PullBufferDataSource) dataSource).getStreams()[0];
            }
            else
            {
                audioStream = ((PushBufferDataSource) dataSource).getStreams()[0];
                ((PushBufferStream)audioStream).setTransferHandler(this);
            }
            return (audioStream != null);
        }

        public void transferData(PushBufferStream stream)
        {
            try
            {
                stream.read(firstBuf);
            }
            catch (IOException e)
            {
                logger.warn("Got IOException during transfer data", e);
            }
            synchronized (bufferSync)
            {
                bufferAvailable = true;
                bufferSync.notifyAll();
            }
        }
        /**
         * Gather entropy from portaudio capture device and seed Fortuna PRNG.
         *
         * The method gathers a number of samples and seeds the Fortuna PRNG.
         */
        @Override
        public void run()
        {
            ZrtpFortuna fortuna = ZrtpFortuna.getInstance();

            if ((dataSource == null) || (audioStream == null))
                return;

            try
            {
                dataSource.start();

                int i = 0;
                while (gatheredEntropy < bytesToGather)
                {
                    if (audioStream instanceof PushBufferStream)
                    {
                        synchronized (bufferSync)
                        {
                            while (!bufferAvailable)
                            {
                                try
                                {
                                    bufferSync.wait();
                                }
                                catch (InterruptedException e)
                                {
                                    // ignore
                                }
                            }
                            bufferAvailable = false;
                        }
                    }
                    else
                        ((PullBufferStream)audioStream).read(firstBuf);
                    byte[] entropy = (byte[])firstBuf.getData();
                    gatheredEntropy += entropy.length;

                    // distribute first buffers evenly over the pools, put
                    // others on the first pools. This method is adapted to
                    // SC requirements to get random data
                    if (i < 32)
                        fortuna.addSeedMaterial(entropy);
                    else
                    {
                        fortuna
                            .addSeedMaterial((i%3), entropy, 0, entropy.length);
                    }
                    i = gatheredEntropy / bytes20ms;
                }
                entropyOk = true;
                if (logger.isInfoEnabled())
                    logger.info("GatherEntropy got: " + gatheredEntropy + " bytes");
            }
            catch (IOException ioex)
            {
                // ignore exception
            }
            finally
            {
                audioStream = null;
                dataSource.disconnect();
            }
            // this forces a Fortuna to use the new seed (entropy) data.
            byte[] random = new byte[300];
            fortuna.nextBytes(random);
        }
    }
}
