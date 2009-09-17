/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Class for representing all the different DTMF tones.
 *
 * @author JM HEITZ
 */
public final class DTMFTone
{
    /**
     * The "A" DTMF Tone
     */
    public static final DTMFTone DTMF_A=new DTMFTone("A");

    /**
     * The "B" DTMF Tone
     */
    public static final DTMFTone DTMF_B=new DTMFTone("B");

    /**
     * The "C" DTMF Tone
     */
    public static final DTMFTone DTMF_C=new DTMFTone("C");

    /**
     * The "D" DTMF Tone
     */
    public static final DTMFTone DTMF_D=new DTMFTone("D");

    /**
     * The "0" DTMF Tone
     */
    public static final DTMFTone DTMF_0=new DTMFTone("0");

    /**
     * The "1" DTMF Tone
     */
    public static final DTMFTone DTMF_1=new DTMFTone("1");

    /**
     * The "2" DTMF Tone
     */
    public static final DTMFTone DTMF_2=new DTMFTone("2");

    /**
     * The "3" DTMF Tone
     */
    public static final DTMFTone DTMF_3=new DTMFTone("3");

    /**
     * The "4" DTMF Tone
     */
    public static final DTMFTone DTMF_4=new DTMFTone("4");

    /**
     * The "5" DTMF Tone
     */
    public static final DTMFTone DTMF_5=new DTMFTone("5");

    /**
     * The "6" DTMF Tone
     */
    public static final DTMFTone DTMF_6=new DTMFTone("6");

    /**
     * The "7" DTMF Tone
     */
    public static final DTMFTone DTMF_7=new DTMFTone("7");

    /**
     * The "8" DTMF Tone
     */
    public static final DTMFTone DTMF_8=new DTMFTone("8");

    /**
     * The "9" DTMF Tone
     */
    public static final DTMFTone DTMF_9=new DTMFTone("9");

    /**
     * The "*" DTMF Tone
     */
    public static final DTMFTone DTMF_STAR=new DTMFTone("*");

    /**
     * The "#" DTMF Tone
     */
    public static final DTMFTone DTMF_SHARP=new DTMFTone("#");

    /**
     * The value of the DTMF tone
     */
    private final String value;

    /**
     * Creates a DTMF instance with the specified tone value. The method is
     * private since one would only have to use predefined static instances.
     *
     * @param value one of te DTMF_XXX fields, indicating the value of the tone.
     */
    private DTMFTone(String value)
    {
        this.value = value;
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
        if(!(target instanceof DTMFTone))
        {
            return false;
        }
        DTMFTone targetDTMFTone = (DTMFTone)(target);

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
}
