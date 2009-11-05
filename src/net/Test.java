/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net;

/**
 * @author Emil Ivov
 */
public class Test
{
    public static void main(String[] args)
    {
        Double i = Double.NaN;
        if (i == i)
        {
            System.out.println("i == i");
        }
        else
        {
            System.out.println("i != i");
        }
    }
}
