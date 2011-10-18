/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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

    /**
     * This PrintStream contains System.out when the class were initiated.
     * Normally that would be the system default System.out
     */
    private PrintStream systemOut;

    public static void setStdOutPrintingEnabled(boolean enabled)
    {
        stdOutPrintingEnabled = enabled;
    }

    public ScStdOut(PrintStream printStream)
    {
        super(printStream);
        systemOut = System.out;
    }

    /**
     * Returns the default System.out <tt>PrintStream</tt> that was in use
     * before this class was instantiated.
     *
     * @return the original System.out PrintStream
     */
    public PrintStream getSystemOut()
    {
        return systemOut;
    }

    /**
     * Prints <tt>string</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param string the <tt>String</tt> to print.
     */
    @Override
    public void print(String string)
    {
        if(stdOutPrintingEnabled)
            super.print(string);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>boolean</tt> to print.
     */
    @Override
    public void println(boolean x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>char</tt> to print.
     */
    @Override
    public void println(char x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>char[]</tt> to print.
     */
    @Override
    public void println(char[] x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>double</tt> to print.
     */
    @Override
    public void println(double x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>float</tt> to print.
     */
    @Override
    public void println(float x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>int</tt> to print.
     */
    @Override
    public void println(int x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>long</tt> to print.
     */
    @Override
    public void println(long x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>Object</tt> to print.
     */
    @Override
    public void println(Object x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>x</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param x the <tt>String</tt> to print.
     */
    @Override
    public void println(String x)
    {
        if(stdOutPrintingEnabled)
            super.println(x);
    }

    /**
     * Prints <tt>b</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param b the <tt>boolean</tt> to print.
     */
    @Override
    public void print(boolean b)
    {
        if(stdOutPrintingEnabled)
            super.print(b);
    }

    /**
     * Prints <tt>c</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param c the <tt>char</tt> to print.
     */
    @Override
    public void print(char c)
    {
        if(stdOutPrintingEnabled)
            super.print(c);
    }

    /**
     * Prints <tt>s</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param s the <tt>char[]</tt> to print.
     */
    @Override
    public void print(char[] s)
    {
        if(stdOutPrintingEnabled)
            super.print(s);
    }

    /**
     * Prints <tt>d</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param d the <tt>double</tt> to print.
     */
    @Override
    public void print(double d)
    {
        if(stdOutPrintingEnabled)
            super.print(d);
    }

    /**
     * Prints <tt>f</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param f the <tt>float</tt> to print.
     */
    @Override
    public void print(float f)
    {
        if(stdOutPrintingEnabled)
            super.print(f);
    }

    /**
     * Prints <tt>i</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param i the <tt>int</tt> to print.
     */
    @Override
    public void print(int i)
    {
        if(stdOutPrintingEnabled)
            super.print(i);
    }

    /**
     * Prints <tt>l</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param l the <tt>long</tt> to print.
     */
    @Override
    public void print(long l)
    {
        if(stdOutPrintingEnabled)
            super.print(l);
    }

    /**
     * Prints <tt>obj</tt> if <tt>stdOutPrintingEnabled</tt> is enabled.
     *
     * @param obj the <tt>Object</tt> to print.
     */
    @Override
    public void print(Object obj)
    {
        if(stdOutPrintingEnabled)
            super.print(obj);
    }

    /**
     * Prints an empty line <tt>stdOutPrintingEnabled</tt> is enabled.
     */
    @Override
    public void println()
    {
        if(stdOutPrintingEnabled)
            super.println();
    }
}
