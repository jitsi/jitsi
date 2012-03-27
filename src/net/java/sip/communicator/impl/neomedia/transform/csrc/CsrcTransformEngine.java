/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.csrc;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.service.neomedia.*;

/**
 * We use this engine to add the list of CSRC identifiers in RTP packets that
 * we send to conference participants during calls where we are the mixer.
 *
 * @author Emil Ivov
 */
public class CsrcTransformEngine
    implements TransformEngine,
               PacketTransformer
{
    /**
     * The <tt>MediaStreamImpl</tt> that this transform engine was created to
     * transform packets for.
     */
    private final MediaStreamImpl mediaStream;

    /**
     * The number currently assigned to CSRC audio level extensions or
     * <tt>-1</tt> if no such ID has been set and audio level extensions should
     * not be transmitted.
     */
    private byte csrcAudioLevelExtID = -1;

    /**
     * The buffer that we use to encode the csrc audio level extensions.
     */
    private byte[] extensionBuff = null;

    /**
     * Indicates the length that we are currently using in the
     * <tt>extensionBuff</tt> buffer.
     */
    private int extensionBuffLen = 0;

    /**
     * The dispatcher that is delivering audio levels to the media steam.
     */
    private CsrcAudioLevelDispatcher csrcLevelDispatcher = null;

    /**
     * The direction that we are supposed to handle audio levels in.
     */
    private MediaDirection audioLevelDirection = MediaDirection.INACTIVE;

    /**
     * Creates an engine instance that will be adding CSRC lists to the
     * specified <tt>stream</tt>.
     *
     * @param stream that <tt>MediaStream</tt> whose RTP packets we are going
     * to be adding CSRC lists. to
     */
    public CsrcTransformEngine(MediaStreamImpl stream)
    {
        this.mediaStream = stream;
    }

    /**
     * Close the transformer and underlying transform engine.
     * 
     * Nothing to do here. 
     */
    public void close() 
    {
    }

    /**
     * Always returns <tt>null</tt> since this engine does not require any
     * RTCP transformations.
     *
     * @return <tt>null</tt> since this engine does not require any
     * RTCP transformations.
     */
    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }

    /**
     * Returns a reference to this class since it is performing RTP
     * transformations in here.
     *
     * @return a reference to <tt>this</tt> instance of the
     * <tt>CsrcTransformEngine</tt>.
     */
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /**
     * Extracts the list of CSRC identifiers and passes it to the
     * <tt>MediaStream</tt> associated with this engine. Other than that the
     * method does not do any transformations since CSRC lists are part of
     * RFC 3550 and they shouldn't be disrupting the rest of the application.
     *
     * @param pkt the RTP <tt>RawPacket</tt> that we are to extract a CSRC list
     * from.
     *
     * @return the same <tt>RawPacket</tt> that was received as a parameter
     * since we don't need to worry about hiding the CSRC list from the rest
     * of the RTP stack.
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        if (csrcAudioLevelExtID > 0 && audioLevelDirection.allowsReceiving())
        {
            //extract the audio levels and send them to the dispatcher.
            long[] levels = pkt.extractCsrcLevels(csrcAudioLevelExtID);

            if(levels != null)
            {
                if (csrcLevelDispatcher == null)
                {
                    csrcLevelDispatcher = new CsrcAudioLevelDispatcher();
                    new Thread(csrcLevelDispatcher).start();
                }

                csrcLevelDispatcher.addLevels(levels);
            }
        }
        return pkt;
    }

    /**
     * Extracts the list of CSRC identifiers representing participants currently
     * contributing to the media being sent by the <tt>MediaStream</tt>
     * associated with this engine and (unless the list is empty) encodes them
     * into the <tt>RawPacket</tt>.
     *
     * @param pkt the RTP <tt>RawPacket</tt> that we need to add a CSRC list to.
     *
     * @return the updated <tt>RawPacket</tt> instance containing the list of
     * CSRC identifiers.
     */
    public synchronized RawPacket transform(RawPacket pkt)
    {
        // if somebody has modified the packet and added an extension
        // don't process it. As ZRTP creates special rtp packets carring no
        // rtp data and those packets are used only by zrtp we don't use them.
        if(pkt.getExtensionBit())
            return pkt;

        long[] csrcList = mediaStream.getLocalContributingSourceIDs();

        if(csrcList == null || csrcList.length == 0)
        {
            //nothing to do.
            return pkt;
        }

        pkt.setCsrcList( csrcList);

        //attach audio levels if we are expected to do so.
        if(this.csrcAudioLevelExtID > 0
           && audioLevelDirection.allowsSending()
           && mediaStream instanceof AudioMediaStreamImpl)
        {
            byte[] levelsExt = createLevelExtensionBuffer(csrcList);

            pkt.addExtension(levelsExt, extensionBuffLen);
        }

        return pkt;
    }

    /**
     * Stops threads that this transform engine is using for even delivery.
     */
    public void stop()
    {
        if(csrcLevelDispatcher != null)
            csrcLevelDispatcher.stop();
    }

    /**
     * Sets the ID that this transformer should be using for audio level
     * extensions or disables audio level extensions if <tt>extID</tt> is
     * <tt>-1</tt>.
     *
     * @param extID ID that this transformer should be using for audio level
     * extensions or <tt>-1</tt> if audio level extensions should be disabled
     * @param dir the direction that we are expected to hand this extension in.
     *
     */
    public void setCsrcAudioLevelAudioLevelExtensionID(byte           extID,
                                                       MediaDirection dir)
    {
        this.csrcAudioLevelExtID = extID;
        this.audioLevelDirection = dir;
    }

    /**
     * Creates a audio level extension buffer containing the level extension
     * header and the audio levels corresponding to (and in the same order as)
     * the <tt>CSRC</tt> IDs in the <tt>csrcList</tt>
     *
     * @param csrcList the list of CSRC IDs whose level we'd like the extension
     * to contain.
     * @return the extension buffer in the form that it should be added to the
     * RTP packet.
     */
    private byte[] createLevelExtensionBuffer(long[] csrcList)
    {
        int buffLen = 1 + //CSRC one byte extension hdr
                      csrcList.length;

        // calculate extension padding
        int padLen = 4 - buffLen%4;

        if(padLen == 4)
            padLen = 0;

        buffLen += padLen;

        byte[] extensionBuff = getExtensionBuff(buffLen);

        extensionBuff[0]
            = (byte)((csrcAudioLevelExtID << 4) | (csrcList.length - 1));

        int csrcOffset = 1; // initial offset is equal to ext hdr size

        for(long csrc : csrcList)
        {
            byte level
                = (byte)
                    ((AudioMediaStreamImpl)mediaStream)
                        .getLastMeasuredAudioLevel(csrc);

            extensionBuff[csrcOffset] = level;
            csrcOffset ++;
        }

        return extensionBuff;
    }

    /**
     * Returns a reusable byte array which is guaranteed to have the requested
     * <tt>ensureCapacity</tt> length and sets our internal length keeping
     * var.
     *
     * @param ensureCapacity the minimum length that we need the returned buffer
     * to have.
     * @return a reusable <tt>byte[]</tt> array guaranteed to have a length
     * equal to or greater than <tt>ensureCapacity</tt>.
     */
    private byte[] getExtensionBuff(int ensureCapacity)
    {
        if (extensionBuff == null || extensionBuff.length < ensureCapacity)
            extensionBuff = new byte[ensureCapacity];

        extensionBuffLen = ensureCapacity;
        return extensionBuff;
    }

    /**
     * A simple thread that waits for new levels to be reported from incoming
     * RTP packets and then delivers them to the <tt>AudioMediaStream</tt>
     * associated with this engine. The reason we need to do this in a separate
     * thread is of course the time sensitive nature of incoming RTP packets.
     */
    private class CsrcAudioLevelDispatcher
        implements Runnable
    {
        /** Indicates whether this thread is supposed to be running */
        private boolean isRunning = false;

        /** The levels that we last received from the reverseTransform thread*/
        private long[] lastReportedLevels = null;

        /**
         * Waits for new levels to be reported via the <tt>addLevels()</tt>
         * method and then delivers them to the <tt>AudioMediaStream</tt> that
         * we are associated with.
         */
        public void run()
        {
            isRunning = true;

            //no point in listening if our stream is not an audio one.
            if(!(mediaStream instanceof AudioMediaStreamImpl))
                return;

            long[] temp = null;

            while(isRunning)
            {
                synchronized(this)
                {
                    if(lastReportedLevels == null)
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException ie) {}
                    }

                    temp = lastReportedLevels;
                    // make lastReportedLevels to null
                    // so we will wait for the next level on next iteration
                    lastReportedLevels = null;
                }

                if(temp != null)
                {
                    //now notify our listener
                    if (mediaStream != null)
                    {
                        ((AudioMediaStreamImpl)mediaStream)
                            .fireConferenceAudioLevelEvent(temp);
                    }
                }
            }
        }

        /**
         * A level matrix that we should deliver to our media stream and
         * its listeners in a separate thread.
         *
         * @param levels the levels that we'd like to queue for processing.
         */
        public void addLevels(long[] levels)
        {
            synchronized(this)
            {
                this.lastReportedLevels = levels;

                notifyAll();
            }
        }

        /**
         * Causes our run method to exit so that this thread would stop
         * handling levels.
         */
        public void stop()
        {
            synchronized(this)
            {
                this.lastReportedLevels = null;
                isRunning = false;

                notifyAll();
            }
        }
    }

}
