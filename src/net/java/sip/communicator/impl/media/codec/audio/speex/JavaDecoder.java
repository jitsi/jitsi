/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec.audio.speex;

import java.io.*;
import javax.media.*;
import javax.media.format.*;

import org.xiph.speex.*;
import net.java.sip.communicator.impl.media.codec.*;

/**
 * Speex to PCM java decoder
 * @author Damian Minkov
 **/
public class JavaDecoder
    extends com.ibm.media.codec.audio.AudioCodec
{
    private Format lastFormat = null;
    private SpeexDecoder decoder = null;

    /**
     * Creates the decoder and inits supported input formats.
     */
    public JavaDecoder()
    {
        supportedInputFormats = new AudioFormat[]
        {
            new AudioFormat(Constants.SPEEX_RTP,
                            8000,
                            8,
                            1,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED),
            new AudioFormat(Constants.SPEEX_RTP,
                            16000,
                            8,
                            1,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED),
            new AudioFormat(Constants.SPEEX_RTP,
                            32000,
                            8,
                            1,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED)
        };
        inputFormats = supportedInputFormats;

        defaultOutputFormats = new AudioFormat[]
            {new AudioFormat(AudioFormat.LINEAR)};

        PLUGIN_NAME = "Speex Decoder";
    }

    /**
     * Returns the output format that matches the supplied input format.
     * @param in
     * @return
     */
    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;

        supportedOutputFormats = new AudioFormat[]
            {
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED //isSigned());
            )};

        return supportedOutputFormats;

    }

    /**
     * Does nothing.
     */
    public void open()
    {}

    /**
     * Does nothing.
     */
    public void close()
    {}

    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;

        decoder = new SpeexDecoder();

        int sampleRate =
            (int)inFormat.getSampleRate();

        int band = JavaEncoder.NARROW_BAND;

        if(sampleRate == 16000)
            band = JavaEncoder.WIDE_BAND;
        else if(sampleRate == 32000)
            band = JavaEncoder.ULTRA_WIDE_BAND;

        decoder.init(band,
                     sampleRate,
                     inFormat.getChannels(),
                     false);
    }

    /**
     * Process the input and decodes it.
     * @param inputBuffer the input data.
     * @param outputBuffer the result data.
     * @return state of the process.
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer))
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        byte[] inData = (byte[]) inputBuffer.getData();

        int inpLength = inputBuffer.getLength();

        int outLength = 0;

        int inOffset = inputBuffer.getOffset();
        int outOffset = outputBuffer.getOffset();

        Format newFormat = inputBuffer.getFormat();

        if (lastFormat != newFormat)
        {
            initConverter( (AudioFormat) newFormat);
        }

        try
        {
            decoder.processData(inData, inOffset, inpLength);
            outLength = decoder.getProcessedDataByteSize();

            byte[] outData = validateByteArraySize(outputBuffer, outLength);

            decoder.getProcessedData(outData, outOffset);
        }
        catch (StreamCorruptedException ex)
        {
            ex.printStackTrace();
        }

        updateOutput(outputBuffer, outputFormat, outLength, 0);

        return BUFFER_PROCESSED_OK;
    }

//    public java.lang.Object[] getControls()
//    {
//        if (controls == null)
//        {
//            controls = new Control[1];
//            controls[0] = new com.sun.media.controls.SilenceSuppressionAdapter(this, false, false);
//        }
//        return (Object[]) controls;
//    }

    // TODO clean code (remove) array if really not needed
//    static int SpeexSubModeSz[] =
//        {
//        0, 43, 119, 160,
//        220, 300, 364, 492,
//        79, 0, 0, 0,
//        0, 0, 0, 0};
//    static int SpeexInBandSz[] =
//        {
//        1, 1, 4, 4,
//        4, 4, 4, 4,
//        8, 8, 16, 16,
//        32, 32, 64, 64};

    /**
     * Counts the samples in given data
     * @param data byte[]
     * @param len int
     * @return int
     */
    // TODO: clean code (remove) following method if really not used
    
//    private static int speex_get_samples(byte[] data, int len)
//    {
//        int bit = 0;
//        int cnt = 0;
//        int off = 0;
//
//        int c;
//
//        //DEBU(G "speex_get_samples(%d)\n", len);
//        while ((len * 8 - bit) >= 5) {
//            /* skip wideband frames */
//            off = speex_get_wb_sz_at(data, len, bit);
//            if (off < 0)  {
//                //DEBU(G "\tERROR reading wideband frames\n");
//                break;
//            }
//            bit += off;
//
//            if ((len * 8 - bit) < 5) {
//                //DEBU(G "\tERROR not enough bits left after wb\n");
//                break;
//            }
//
//            /* get control bits */
//            c = get_n_bits_at(data, 5, bit);
//            //DEBU(G "\tCONTROL: %d at %d\n", c, bit);
//            bit += 5;
//
//            if (c == 15) {
//                //DEBU(G "\tTERMINATOR\n");
//                break;
//            } else if (c == 14) {
//                /* in-band signal; next 4 bits contain signal id */
//                c = get_n_bits_at(data, 4, bit);
//                bit += 4;
//                //DEBUG "\tIN-BAND %d bits\n", SpeexInBandSz[c]);
//                bit += SpeexInBandSz[c];
//            } else if (c == 13) {
//                /* user in-band; next 5 bits contain msg len */
//                c = get_n_bits_at(data, 5, bit);
//                bit += 5;
//                //DEBUG "\tUSER-BAND %d bytes\n", c);
//                bit += c * 8;
//            } else if (c > 8) {
//                //DEBU(G "\tUNKNOWN sub-mode %d\n", c);
//                break;
//            } else {
//                /* skip number bits for submode (less the 5 control bits) */
//                //DEBU(G "\tSUBMODE %d %d bits\n", c, SpeexSubModeSz[c]);
//                bit += SpeexSubModeSz[c] - 5;
//
//                cnt += 160; /* new frame */
//            }
//        }
//        //DEBU(G "\tSAMPLES: %d\n", cnt);
//        return cnt;
//    }

    // TODO: clean code (remove) following method if really not used - wide band?
//    private static int get_n_bits_at(byte[] data, int n, int bit)
//    {
//        int byteInt = bit / 8; /* byte containing first bit */
//        int rem = 8 - (bit % 8); /* remaining bits in first byte */
//        int ret = 0;
//
//        if (n <= 0 || n > 8)
//            return 0;
//
//        if (rem < n)
//        {
//            ret = (data[byteInt] << (n - rem));
//            ret |= (data[byteInt +1] >> (8 - n + rem));
//        }
//        else
//        {
//            ret = (data[byteInt] >> (rem - n));
//        }
//
//        return (ret & (0xff >> (8 - n)));
//    }
//
    // TODO clean code (remove) array if really not needed
//    static int SpeexWBSubModeSz[] =
//        {
//        0, 36, 112, 192,
//        352, 0, 0, 0};

    // TODO: clean code (remove) following method if really not used - wide band?
    
//    private static int speex_get_wb_sz_at(byte[] data, int len, int bit)
//    {
//        int off = bit;
//        int c;
//
//        /* skip up to two wideband frames */
//        if ( ( (len * 8 - off) >= 5) &&
//            get_n_bits_at(data, 1, off) != 0)
//        {
//            c = get_n_bits_at(data, 3, off + 1);
//            off += SpeexWBSubModeSz[c];
//
//            if ( ( (len * 8 - off) >= 5) &&
//                get_n_bits_at(data, 1, off) != 0)
//            {
//                c = get_n_bits_at(data, 3, off + 1);
//                off += SpeexWBSubModeSz[c];
//
//                if ( ( (len * 8 - off) >= 5) &&
//                    get_n_bits_at(data, 1, off) != 0)
//                {
//                    /* too many in a row */
//                    //DEBU(G "\tCORRUPT too many wideband streams in a row\n");
//                    return -1;
//                }
//            }
//
//        }
//        return off - bit;
//    }
}
