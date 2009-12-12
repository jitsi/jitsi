/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

/**
 * The <tt>CsrcAudioLevelListener</tt> delivers audio level events reported by
 * the remote party in cases where it (the remote party) is acting as a mixer,
 * mixing flows from numerous contributors. It is up to upper layers such as
 * SIP to define means of determining the exact members that the CSRC IDs and
 * hence audio levels participants belong to.
 *
 * @author Emil Ivov
 */
public interface CsrcAudioLevelListener
{

    /**
     * Called by the media service implementation after it has received audio
     * levels for the various participants (Contributing SouRCes) that are
     * taking part in a conference call.
     *
     * @param audioLevels the new set of levels for the various contributing
     * sources in the conference call.
     */
    public void audioLevelsReceived(final long[][] audioLevels);

}
