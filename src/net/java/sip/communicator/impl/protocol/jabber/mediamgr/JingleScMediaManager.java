/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.mediamgr;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smackx.jingle.*;
import org.jivesoftware.smackx.jingle.media.*;
import org.jivesoftware.smackx.jingle.nat.*;

/**
 * Implements a JingleMediaManager backed by JMF.
 *
 * based on Thiago Camargo's media manager from jingle
 *
 * @author Symphorien Wanko
 */
public class JingleScMediaManager extends JingleMediaManager
{
    /**
     * Logger for this class
     */
    private Logger logger = Logger.getLogger(JingleScMediaManager.class);

    /**
     * List of payload that this media manager supports. This list is based on
     * the one reported by the media service.
     */
    private List<PayloadType> payloads =
        new ArrayList<PayloadType>();

    /**
     * Creates a new instance of JingleScMediaManager
     */
    public JingleScMediaManager()
    {
        setupPayloads();
    }

    /**
     * Returns a new jingleMediaSession
     *
     * @param payloadType payloadType
     * @param remote      remote Candidate
     * @param local       local Candidate
     * @param jingleSession the session for which we create a media session
     *
     * @return JingleMediaSession
     */
    public JingleMediaSession createMediaSession(
            PayloadType payloadType,
            TransportCandidate remote,
            TransportCandidate local, JingleSession jingleSession)
    {
        return new AudioMediaSession(payloadType, remote, local, jingleSession);
    }

    /**
     * Setup API supported Payloads
     *
     * http://tools.ietf.org/html/rfc3551#page-32 to view the correspondence
     * between PayloadType and codec
     */
    private void setupPayloads()
    {

        /*
         * Initializing audioEnc to new String[0] isn't better than setting it
         * to null upon an exception because the former causes an allocation.
         * Besides, the exception is supposes to happen in rare/exceptional
         * situations i.e. in most of the cases the allocation will immediately
         * be thrown away as garbage. Checking for null is faster.
         */
        String[] audioEnc;

        try
        {
            audioEnc = JabberActivator.getMediaService().
                getSupportedAudioEncodings();
        }
        catch (Exception e)
        {
            audioEnc = null;

            logger.warn("Cannot get Audio encodings!", e);
        }

        if (audioEnc != null)
        {
            for (int i = 0; i < audioEnc.length; i++)
            {
                int payloadType =
                    MediaUtils.getPayloadType(Integer.parseInt(audioEnc[i]));
                String payloadName = MediaUtils.getPayloadName(payloadType);

                if (payloadName != null)
                {
                    payloads
                        .add(new PayloadType.Audio(payloadType, payloadName));
                }
                else if (payloadType >= 0)
                {
                    payloads
                        .add(new PayloadType.Audio(payloadType, audioEnc[i]));
                }
            }
        }
        if (payloads.isEmpty())
        {
            logger.warn("The list of payloads supported" +
                    " by JmfMediaManager is empty ");
        }
    }

    /**
     * Return all supported Payloads for this Manager
     *
     * @return The Payload List
     */

    public List<PayloadType> getPayloads()
    {
        return payloads;
    }
}
