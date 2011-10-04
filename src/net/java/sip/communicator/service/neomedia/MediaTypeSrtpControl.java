package net.java.sip.communicator.service.neomedia;

public class MediaTypeSrtpControl implements Comparable<MediaTypeSrtpControl>
{
    public MediaType mediaType;
    public SrtpControlType srtpControlType;

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
        return mediaType == other.mediaType && srtpControlType == other.srtpControlType;
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
