/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;

/**
 * This class provides a PrintWriter implementation that we use to replace
 * System.out so that we could capture output from all libs or SC code that
 * uses calls to System.out.println();
 *
 * @author Emil Ivov
 */
public class ScStdOut extends PrintStream
{
    private static boolean stdOutPrintingEnabled = false;

    public static void setStdOutPrintingEnabled(boolean enabled)
    {
        stdOutPrintingEnabled = enabled;
    }

    public ScStdOut(PrintStream printStream)
    {
        super(printStream);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(java.lang.String)
     */
    @Override
    public void print(String s)
    {
        if(stdOutPrintingEnabled)
            super.print(s);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(boolean)
     */
    @Override
    public void println(boolean x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(char)
     */
    @Override
    public void println(char x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(char[])
     */
    @Override
    public void println(char[] x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(double)
     */
    @Override
    public void println(double x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(float)
     */
    @Override
    public void println(float x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(int)
     */
    @Override
    public void println(int x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(long)
     */
    @Override
    public void println(long x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(java.lang.Object)
     */
    @Override
    public void println(Object x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println(java.lang.String)
     */
    @Override
    public void println(String x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(boolean)
     */
    @Override
    public void print(boolean b)
    {
        if(stdOutPrintingEnabled)
            super.print(b);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(char)
     */
    @Override
    public void print(char c)
    {
        if(stdOutPrintingEnabled)
            super.print(c);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(char[])
     */
    @Override
    public void print(char[] s)
    {
        if(stdOutPrintingEnabled)
            super.print(s);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(double)
     */
    @Override
    public void print(double d)
    {
        if(stdOutPrintingEnabled)
            super.print(d);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(float)
     */
    @Override
    public void print(float f)
    {
        if(stdOutPrintingEnabled)
            super.print(f);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(int)
     */
    @Override
    public void print(int i)
    {
        if(stdOutPrintingEnabled)
            super.print(i);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(long)
     */
    @Override
    public void print(long l)
    {
        if(stdOutPrintingEnabled)
            super.print(l);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#print(java.lang.Object)
     */
    @Override
    public void print(Object obj)
    {
        if(stdOutPrintingEnabled)
            super.print(obj);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#println()
     */
    @Override
    public void println()
    {
        if(stdOutPrintingEnabled)
            super.println();
    }


}
