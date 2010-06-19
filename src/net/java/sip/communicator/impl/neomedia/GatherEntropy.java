package net.java.sip.communicator.impl.neomedia;

import java.io.*;

import javax.media.*;
import javax.media.control.FormatControl;
import javax.media.format.*;
import javax.media.protocol.*;

import gnu.java.zrtp.utils.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.util.Logger;

/**
 * GatherEntropy initializes the Fortuna PRNG with entropy data.
 *
 * GatherEntropy gets the media device configuration and checks which media
 * systems are available. It then reads some data fom media input (capture)
 * devices and uses this data to seed the Fortuna PRNG. The ZrtpFortuna PRNG
 * is a singleton and all other methods that require random data shall use
 * this singleton.
 * 
 * Use GatherEntropy during startup and initialization phase of SIP 
 * Communicator but after initialization of the media devices to get entropy
 * data at the earliest point. Also make sure that entropy data is read from
 * local sources only and that entropy data is never send out (via networks 
 * for example).
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 * @author Lubomir Marinov
 */
public class GatherEntropy
{
    /**
     * The <tt>Logger</tt> used by <tt>GetherEntropy</tt>
     * class for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(GatherEntropy.class);

    /**
     * Device config to look for catpture devices.
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
     * How many audio buffer to read.
     * 
     * Javasound buffers contain audio data for 125ms mono, at 8000Hz this
     * computes to 2000 bytes, in total we have 200000 bytes of randon data
     * 
     * Portaudio does not honor the format setting, it always captures
     * at 44100 Hz.
     * 
     * Portaudio buffers contain audio data for 20ms mono, at 44100Hz this
     * computes to 1764 bytes, in total we have 176400 bytes of random data
     */
    final private static int NUM_OF_BUFFERS = 100;

    public GatherEntropy(DeviceConfiguration deviceConfiguration) 
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
    protected int getGatheredEntropy() {
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
        retValue = gatherer.preparePortAudioEntropy();
        if (retValue)
            gatherer.start();
        return retValue;
    }

    private class GatherAudio extends Thread implements BufferTransferHandler
    {
        /**
         * The PortAudio <tt>DataSource</tt> which provides
         * {@link #portAudioStream}.
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
        private Buffer firstBuf = new Buffer();
        private boolean bufferAvailable = false;
        private Object bufferSync = new Object();
        
        /**
         * Prepares to read entropy data from portaudio capture device.
         * 
         * The method gets an PortAudio instance with a set of capture 
         * parameters.
         * 
         * @return True if the PortAudio input stream is available. 
         */
        private boolean preparePortAudioEntropy()
        {
            MediaLocator audioCaptureDeviceLocator
                = deviceConfiguration.getAudioCaptureDevice().getLocator();

            if (audioCaptureDeviceLocator == null)
                return false;

            try {
                dataSource = Manager.createDataSource(audioCaptureDeviceLocator);
            } catch (NoDataSourceException e) {
                logger.warn("No data source during entropy preparation", e);
                return false;
            } catch (IOException e) {
                logger.warn("Got an IO Exception during entropy preparation", e);
                return false;
            }
            FormatControl fc = ((CaptureDevice)dataSource).getFormatControls()[0];

            // Javasound honors this setting, Portaudio uses a fixed setting.
            fc.setFormat(new AudioFormat(
                    AudioFormat.LINEAR,
                    8000,
                    16 /* sampleSizeInBits */,
                    1 /* channels */,
                    AudioFormat.LITTLE_ENDIAN,
                    AudioFormat.SIGNED,
                    Format.NOT_SPECIFIED /* frameSizeInBits */,
                    Format.NOT_SPECIFIED /* frameRate */,
                    Format.byteArray)
            );

            if (dataSource instanceof PullBufferDataSource) {
                audioStream = ((PullBufferDataSource) dataSource).getStreams()[0];
            }
            else {
                audioStream = ((PushBufferDataSource) dataSource).getStreams()[0];
                ((PushBufferStream)audioStream).setTransferHandler(this);
            }
            return (audioStream != null);
        }

        public void transferData(PushBufferStream stream) {
            try {
                stream.read(firstBuf);
            } catch (IOException e) {
                logger.warn("Got IOException during transfer data", e);
            }
            synchronized (bufferSync) {
                bufferAvailable = true;
                bufferSync.notifyAll();
            }
        }
        /**
         * Gather entropy from portaudio capture device and seed Fortuna PRNG.
         * 
         * The method gathers a number of samples and seeds the Fortuna PRNG.
         */
        public void run()
        {
            ZrtpFortuna fortuna = ZrtpFortuna.getInstance();

            if ((dataSource == null) || (audioStream == null))
                return;

            try {
                dataSource.start();

                for (int i = 0; i < NUM_OF_BUFFERS; i++) 
                {
                    if (audioStream instanceof PushBufferStream) {
                        synchronized (bufferSync) {
                            while (!bufferAvailable) {
                                try {
                                    bufferSync.wait();
                                } catch (InterruptedException e) {
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
            return;
        }
    }
}
