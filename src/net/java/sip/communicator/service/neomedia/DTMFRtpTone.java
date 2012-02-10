/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents all DTMF tones for RTP method (RFC4733).
 *
 * @author JM HEITZ
 * @author Romain Philibert
 * @author Emil Ivov
 */
public final class DTMFRtpTone
{
    /**
     * The "0" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_0=new DTMFRtpTone("0", (byte)0);

    /**
     * The "1" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_1=new DTMFRtpTone("1", (byte)1);

    /**
     * The "2" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_2=new DTMFRtpTone("2", (byte)2);

    /**
     * The "3" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_3=new DTMFRtpTone("3", (byte)3);

    /**
     * The "4" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_4=new DTMFRtpTone("4", (byte)4);

    /**
     * The "5" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_5=new DTMFRtpTone("5", (byte)5);

    /**
     * The "6" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_6=new DTMFRtpTone("6", (byte)6);

    /**
     * The "7" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_7=new DTMFRtpTone("7", (byte)7);

    /**
     * The "8" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_8=new DTMFRtpTone("8", (byte)8);

    /**
     * The "9" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_9=new DTMFRtpTone("9", (byte)9);

    /**
     * The "*" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_STAR=new DTMFRtpTone("*", (byte)10);

    /**
     * The "#" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_SHARP=new DTMFRtpTone("#", (byte)11);

    /**
     * The "A" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_A=new DTMFRtpTone("A", (byte)12);

    /**
     * The "B" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_B=new DTMFRtpTone("B", (byte)13);

    /**
     * The "C" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_C=new DTMFRtpTone("C", (byte)14);

    /**
     * The "D" DTMF Tone
     */
    public static final DTMFRtpTone DTMF_D=new DTMFRtpTone("D", (byte)15);

    /**
     * The value of the DTMF tone
     */
    private final String value;

    /**
     * The code of the tone, as specified by RFC 4733, and the we'll actually
     * be sending over the wire.
     */
    private final byte code;

    /**
     * Creates a DTMF instance with the specified tone value. The method is
     * private since one would only have to use predefined static instances.
     *
     * @param value one of the DTMF_XXX fields, indicating the value of the tone.
     * @param code the of the DTMF tone that we'll actually be sending over the
     * wire, as specified by RFC 4733.
     */
    private DTMFRtpTone(String value, byte code)
    {
        this.value = value;
        this.code = code;
    }

    /**
     * Returns the string representation of this DTMF tone.
     *
     * @return the <tt>String</tt> representation of this DTMF tone.
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * Indicates whether some other object is "equal to" this tone.
     *
     * @param target the reference object with which to compare.
     *
     * @return  <tt>true</tt> if target represents the same tone as this
     * object.
     */
    public boolean equals(Object target)
    {
        if(!(target instanceof DTMFRtpTone))
        {
            return false;
        }
        DTMFRtpTone targetDTMFTone = (DTMFRtpTone)(target);

        return targetDTMFTone.value.equals(this.value);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>. The method would actually return the
     * hashcode of the string representation of this DTMF tone.
     *
     * @return  a hash code value for this object (same as calling
     * getValue().hashCode()).
     */
    public int hashCode()
    {
        return getValue().hashCode();
    }

    /**
     * Returns the RFC 4733 code of this DTMF tone.
     *
     * @return the RFC 4733 code of this DTMF tone.
     */
    public byte getCode()
    {
        return code;
    }

    /**
     * Maps between protocol and media DTMF objects.
     * @param tone The DTMFTone to be mapped to an DTMFRtpTone.
     * @return The DTMFRtpTone corresponding to the tone specified.
     */
    public static DTMFRtpTone mapTone(DTMFTone tone)
    {
        if(tone.equals(DTMFTone.DTMF_0))
            return DTMFRtpTone.DTMF_0;
        else if(tone.equals(DTMFTone.DTMF_1))
            return DTMFRtpTone.DTMF_1;
        else if(tone.equals(DTMFTone.DTMF_2))
            return DTMFRtpTone.DTMF_2;
        else if(tone.equals(DTMFTone.DTMF_3))
            return DTMFRtpTone.DTMF_3;
        else if(tone.equals(DTMFTone.DTMF_4))
            return DTMFRtpTone.DTMF_4;
        else if(tone.equals(DTMFTone.DTMF_5))
            return DTMFRtpTone.DTMF_5;
        else if(tone.equals(DTMFTone.DTMF_6))
            return DTMFRtpTone.DTMF_6;
        else if(tone.equals(DTMFTone.DTMF_7))
            return DTMFRtpTone.DTMF_7;
        else if(tone.equals(DTMFTone.DTMF_8))
            return DTMFRtpTone.DTMF_8;
        else if(tone.equals(DTMFTone.DTMF_9))
            return DTMFRtpTone.DTMF_9;
        else if(tone.equals(DTMFTone.DTMF_A))
            return DTMFRtpTone.DTMF_A;
        else if(tone.equals(DTMFTone.DTMF_B))
            return DTMFRtpTone.DTMF_B;
        else if(tone.equals(DTMFTone.DTMF_C))
            return DTMFRtpTone.DTMF_C;
        else if(tone.equals(DTMFTone.DTMF_D))
            return DTMFRtpTone.DTMF_D;
        else if(tone.equals(DTMFTone.DTMF_SHARP))
            return DTMFRtpTone.DTMF_SHARP;
        else if(tone.equals(DTMFTone.DTMF_STAR))
            return DTMFRtpTone.DTMF_STAR;

        return null;
    }
}
