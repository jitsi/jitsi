/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.csrc;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;

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
     * The buffer where we store audio levels.
     */
    private byte[] audioLevelList = null;

    /**
     * Indicates the length that we are currently using in the
     * <tt>audioLevelList</tt> buffer.
     */
    private int audioLevelListLength = 0;

    private byte[] extensionBuff = null;

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
        long[] csrcList = mediaStream.getLocalContributingSourceIDs();

        if(csrcList == null || csrcList.length == 0)
        {
            //nothing to do.
            return pkt;
        }

        pkt.setCsrcList( csrcList);

        //attach audio levels if we are expected to do so.
        if(this.csrcAudioLevelExtID > 0
           && mediaStream instanceof AudioMediaStreamImpl)
        {
            AudioMediaStreamImpl audioStream
                = (AudioMediaStreamImpl)mediaStream;

            byte[] levelsExt = createLevelExtensionBuffer(csrcList);

            pkt.addExtension(levelsExt);

        }

        return pkt;
    }

    /**
     * Sets the ID that this transformer should be using for audio level
     * extensions or disables audio level extensions if <tt>extID</tt> is
     * <tt>-1</tt>.
     *
     * @param extID ID that this transformer should be using for audio level
     * extensions or <tt>-1</tt> if audio level extensions should be disabled
     *
     */
    public void setCsrcAudioLevelAudioLevelExtensionID(byte extID)
    {
        this.csrcAudioLevelExtID = extID;

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
        byte[] extensionBuff = new byte[1 + //CSRC one byte extension hdr
                                        csrcList.length ];

        extensionBuff[0] = (byte)((csrcAudioLevelExtID << 4) | csrcList.length);

        int csrcOffset = 1; // initial offset is equal to ext hdr size

        for(long csrc : csrcList)
        {
            extensionBuff[csrcOffset] = (byte)(csrc >> 24);
            extensionBuff[csrcOffset+1] = (byte)(csrc >> 16);
            extensionBuff[csrcOffset+2] = (byte)(csrc >> 8);
            extensionBuff[csrcOffset+3] = (byte)csrc;

            csrcOffset += 4;
        }

        return extensionBuff;
    }

}
