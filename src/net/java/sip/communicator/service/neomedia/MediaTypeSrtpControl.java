/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Utility class to combine <tt>MediaType</tt> and <tt>SrtpControlType</tt> as a
 * map key.
 * 
 * @author Ingo Bauersachs
 */
public class MediaTypeSrtpControl implements Comparable<MediaTypeSrtpControl>
{
    public final MediaType mediaType;
    public final SrtpControlType srtpControlType;

    /**
     * Creates a new instance of this class.
     * @param mt The <tt>MediaType</tt> for this key.
     * @param sct The <tt>SrtpControlType</tt> for this key.
     */
    public MediaTypeSrtpControl(MediaType mt, SrtpControlType sct)
    {
        mediaType = mt;
        srtpControlType = sct;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null || obj.getClass() != MediaTypeSrtpControl.class)
            return false;

        MediaTypeSrtpControl other = (MediaTypeSrtpControl)obj;
        return mediaType == other.mediaType
            && srtpControlType == other.srtpControlType;
    }

    @Override
    public int hashCode()
    {
        return mediaType.hashCode() ^ srtpControlType.hashCode();
    }

    public int compareTo(MediaTypeSrtpControl o)
    {
        return getWeight() == o.getWeight() ?
                0 :
                getWeight() < o.getWeight() ? 
                    -1 : 1;
    }

    private int getWeight()
    {
        int mtWeight = 0;
        switch(mediaType)
        {
            case AUDIO:
                mtWeight = 1;
                break;
            case VIDEO:
                mtWeight = 2;
                break;
        }
        int stWeight = 0;
        switch(srtpControlType)
        {
            case ZRTP:
                stWeight = 1;
                break;
            case MIKEY:
                stWeight = 2;
                break;
            case SDES:
                stWeight = 3;
                break;
        }
        return mtWeight * 10 + stWeight;
    }
}
