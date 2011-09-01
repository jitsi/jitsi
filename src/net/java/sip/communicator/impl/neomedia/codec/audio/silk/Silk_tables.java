/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

public class Silk_tables
{
    static final int  PITCH_EST_MAX_LAG_MS = 18;          /* 18 ms -> 56 Hz */
    static final int  PITCH_EST_MIN_LAG_MS = 2;          /* 2 ms -> 500 Hz */

    /**
     * Copies the specified range of the specified array into a new array. The
     * initial index of the range (<tt>from</tt>) must lie between zero and
     * <tt>original.length</tt>, inclusive. The value at <tt>original[from]</tt>
     * is placed into the initial element of the copy (unless
     * <tt>from == original.length</tt> or <tt>from == to</tt>). Values from
     * subsequent elements in the original array are placed into subsequent
     * elements in the copy. The final index of the range (<tt>to</tt>), which
     * must be greater than or equal to <tt>from</tt>, may be greater than
     * <tt>original.length</tt>, in which case <tt>0</tt> is placed in all
     * elements of the copy whose index is greater than or equal to
     * <tt>original.length - from</tt>. The length of the returned array will be
     * <tt>to - from</tt>.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to  the final index of the range to be copied, exclusive. (This
     * index may lie outside the array.)
     * @return a new array containing the specified range from the original
     * array, truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt> or
     * <tt>from &gt; original.length()</tt>
     * @throws IllegalArgumentException if <tt>from &gt; to</tt>
     * @throws NullPointerException if <tt>original</tt> is <tt>null</tt>
     */
    static int[] copyOfRange(int[] original, int from, int to)
    {
        if ((from < 0) || (from > original.length))
            throw new ArrayIndexOutOfBoundsException(from);
        if (from > to)
            throw new IllegalArgumentException("to");

        int length = to - from;
        int[] copy = new int[length];

        for (int c = 0, o = from; c < length; c++, o++)
            copy[c] = (o < original.length) ? original[o] : 0;

        return copy;
    }
}
