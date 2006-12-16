// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   FormatInfo.java

package com.sun.media.rtp;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

// Referenced classes of package com.sun.media.rtp:
//            SSRCCache

public class FormatInfo
{

    public FormatInfo()
    {
        cache = null;
        formatList = new Format[111];
        initFormats();
    }

    public void setCache(SSRCCache ssrccache)
    {
        cache = ssrccache;
    }

    public void add(int i, Format format)
    {
        if(i >= formatList.length)
            expandTable(i);
        Format format1;
        if((format1 = formatList[i]) != null)
            return;
        formatList[i] = format;
        if(cache != null && (format instanceof VideoFormat))
            cache.clockrate[i] = 0x15f90;
        if(cache != null && (format instanceof AudioFormat))
            if(mpegAudio.matches(format))
                cache.clockrate[i] = 0x15f90;
            else
                cache.clockrate[i] = (int)((AudioFormat)format).getSampleRate();
    }

    private void expandTable(int i)
    {
        Format aformat[] = new Format[i + 1];
        for(int j = 0; j < formatList.length; j++)
            aformat[j] = formatList[j];

        formatList = aformat;
    }

    public Format get(int i)
    {
        return i < formatList.length ? formatList[i] : null;
    }

    public int getPayload(Format format)
    {
        if(format.getEncoding() != null && format.getEncoding().equals("g729a/rtp"))
            format = new AudioFormat("g729/rtp");
        for(int i = 0; i < formatList.length; i++)
            if(format.matches(formatList[i]))
                return i;

        return -1;
    }

    public void initFormats()
    {
        formatList[0] = new AudioFormat("ULAW/rtp", 8000D, 8, 1);
        formatList[3] = new AudioFormat("gsm/rtp", 8000D, -1, 1);
        formatList[4] = new AudioFormat("g723/rtp", 8000D, -1, 1);
        formatList[5] = new AudioFormat("dvi/rtp", 8000D, 4, 1);
        formatList[8] = new AudioFormat("ALAW/rtp", 8000D, 8, 1);
        formatList[14] = new AudioFormat("mpegaudio/rtp", -1D, -1, -1);
        formatList[15] = new AudioFormat("g728/rtp", 8000D, -1, 1);
        formatList[16] = new AudioFormat("dvi/rtp", 11025D, 4, 1);
        formatList[17] = new AudioFormat("dvi/rtp", 22050D, 4, 1);
        formatList[18] = new AudioFormat("g729/rtp", 8000D, -1, 1);
        formatList[26] = new VideoFormat("jpeg/rtp");
        formatList[31] = new VideoFormat("h261/rtp");
        formatList[32] = new VideoFormat("mpeg/rtp");
        formatList[34] = new VideoFormat("h263/rtp");
        formatList[42] = new VideoFormat("h263-1998/rtp");
        formatList[97] = new AudioFormat("ilbc/rtp", 8000D, -1, 1);
        formatList[98] = new AudioFormat("ilbc/rtp", 8000D, -1, 1);
	formatList[110] = new AudioFormat("speex/rtp", 8000D, 8, 1);
    }

    public static boolean isSupported(int i)
    {
        switch(i)
        {
        case 0: // '\0'
        case 3: // '\003'
        case 4: // '\004'
        case 5: // '\005'
        case 6: // '\006'
        case 8: // '\b'
        case 14: // '\016'
        case 15: // '\017'
        case 16: // '\020'
        case 17: // '\021'
        case 18: // '\022'
        case 26: // '\032'
        case 31: // '\037'
        case 32: // ' '
        case 34: // '"'
        case 97: 
        case 98: 
        case 110: 
            return true;

        case 1: // '\001'
        case 2: // '\002'
        case 7: // '\007'
        case 9: // '\t'
        case 10: // '\n'
        case 11: // '\013'
        case 12: // '\f'
        case 13: // '\r'
        case 19: // '\023'
        case 20: // '\024'
        case 21: // '\025'
        case 22: // '\026'
        case 23: // '\027'
        case 24: // '\030'
        case 25: // '\031'
        case 27: // '\033'
        case 28: // '\034'
        case 29: // '\035'
        case 30: // '\036'
        case 33: // '!'
        default:
            return false;
        }
    }

    private SSRCCache cache;
    public static final int PAYLOAD_NOTFOUND = -1;
    Format formatList[];
    static AudioFormat mpegAudio = new AudioFormat("mpegaudio/rtp");

}
