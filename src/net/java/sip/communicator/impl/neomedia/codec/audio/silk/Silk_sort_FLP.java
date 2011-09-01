/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

/**
 * Insertion sort (fast for already almost sorted arrays):  
 *    Best case:  O(n)   for an already sorted array
 *    Worst case: O(n^2) for an inversely sorted array
 *    
 * To be implemented:
 * Shell short: http://en.wikipedia.org/wiki/Shell_sort
 * 
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class Silk_sort_FLP 
{
    /**
     * 
     * @param a Unsorted / Sorted vector
     * @param a_offset
     * @param index Index vector for the sorted elements
     * @param L Vector length
     * @param K Number of correctly sorted positions
     */
    static void SKP_Silk_insertion_sort_increasing_FLP
    (
            float[]            a,          /* I/O:  Unsorted / Sorted vector                */
            int a_offset,
            int[]              index,      /* O:    Index vector for the sorted elements    */
            final int        L,           /* I:    Vector length                           */
            final int        K            /* I:    Number of correctly sorted positions    */
    )
    {
        float value;
        int i, j;

        /* Safety checks */
        assert (K > 0);
        assert (L > 0);
        assert (L >= K);

        /* Write start indices in index vector */
        for (i = 0; i < K; i++)
        {
            index[i] = i;
        }

        /* Sort vector elements by value, increasing order */
        for (i = 1; i < K; i++)
        {
            value = a[a_offset + i];
            for (j = i - 1; (j >= 0) && (value < a[a_offset + j]); j--)
            {
                a[a_offset + j + 1] = a[a_offset + j]; /* Shift value */
                index[j + 1] = index[j]; /* Shift index */
            }
            a[a_offset + j + 1] = value; /* Write value */
            index[j + 1] = i; /* Write index */
        }

        /* If less than L values are asked check the remaining values, */
        /* but only spend CPU to ensure that the K first values are correct */
        for (i = K; i < L; i++)
        {
            value = a[a_offset + i];
            if (value < a[a_offset + K - 1])
            {
                for (j = K - 2; (j >= 0) && (value < a[a_offset + j]); j--)
                {
                    a[a_offset + j + 1] = a[a_offset + j]; /* Shift value */
                    index[j + 1] = index[j]; /* Shift index */
                }
                a[a_offset + j + 1] = value; /* Write value */
                index[j + 1] = i; /* Write index */
            }
        }
    }

    /**
     * 
     * @param a Unsorted / Sorted vector.
     * @param a_offset offset of valid data.
     * @param index Index vector for the sorted elements.
     * @param L Vector length.
     * @param K Number of correctly sorted positions.
     */
    static void SKP_Silk_insertion_sort_decreasing_FLP
    (
        float[]            a,          /* I/O:  Unsorted / Sorted vector                */
        int a_offset,
        int[]              index,      /* O:    Index vector for the sorted elements    */
        final int        L,           /* I:    Vector length                           */
        final int        K            /* I:    Number of correctly sorted positions    */
    )
    {
        float value;
        int i, j;

        /* Safety checks */
        assert (K > 0);
        assert (L > 0);
        assert (L >= K);

        /* Write start indices in index vector */
        for (i = 0; i < K; i++)
        {
            index[i] = i;
        }

        /* Sort vector elements by value, decreasing order */
        for (i = 1; i < K; i++)
        {
            value = a[a_offset + i];
            for (j = i - 1; (j >= 0) && (value > a[a_offset + j]); j--)
            {
                a[a_offset + j + 1] = a[a_offset + j]; /* Shift value */
                index[j + 1] = index[j]; /* Shift index */
            }
            a[a_offset + j + 1] = value; /* Write value */
            index[j + 1] = i; /* Write index */
        }

        /* If less than L values are asked check the remaining values, */
        /* but only spend CPU to ensure that the K first values are correct */
        for (i = K; i < L; i++)
        {
            value = a[a_offset + i];
            if (value > a[a_offset + K - 1])
            {
                for (j = K - 2; (j >= 0) && (value > a[a_offset + j]); j--)
                {
                    a[a_offset + j + 1] = a[a_offset + j]; /* Shift value */
                    index[j + 1] = index[j]; /* Shift index */
                }
                a[a_offset + j + 1] = value; /* Write value */
                index[j + 1] = i; /* Write index */
            }
        }
    }

    /**
     * 
     * @param a Unsorted / Sorted vector
     * @param a_offset offset of valid data.
     * @param L Vector length 
     */
    static void SKP_Silk_insertion_sort_increasing_all_values_FLP
    (
        float[]            a,          /* I/O:  Unsorted / Sorted vector                */
        int a_offset,
        final int        L            /* I:    Vector length                           */
    )
    {
        float value;
        int i, j;

        /* Safety checks */
        assert (L > 0);

        /* Sort vector elements by value, increasing order */
        for (i = 1; i < L; i++)
        {
            value = a[a_offset + i];
            for (j = i - 1; (j >= 0) && (value < a[a_offset + j]); j--)
            {
                a[a_offset + j + 1] = a[a_offset + j]; /* Shift value */
            }
            a[a_offset + j + 1] = value; /* Write value */
        }
    }
}
