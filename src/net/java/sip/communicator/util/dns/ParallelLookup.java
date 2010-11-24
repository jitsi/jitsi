/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.dns;

import java.text.*;

import net.java.sip.communicator.util.*;

import org.xbill.DNS.*;

/**
 *
 *
 * @author Emil Ivov
 */
public class ParallelLookup
{
    /**
     * The <tt>Logger</tt> used by the <tt>ParallelLookup</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ParallelLookup.class
                    .getName());

    /**
     *
     *
     * @param domain
     * @return
     */
    public Record[] lookup(String domain)

        throws ParseException
    {
        Record[] records = null;
        Lookup lookup;
        try
        {
            lookup = new Lookup(domain, Type.SRV);

            new LookupThread(lookup).start();
        }
        catch (TextParseException tpe)
        {
            logger.error("Failed to parse domain="+domain, tpe);
            throw new ParseException(tpe.getMessage(), 0);
        }

        return records;
    }

    /**
     * The {@link Thread} that executes a single {@link Lookup} in a
     * non-blocking way.
     */
    private final class LookupThread extends Thread
    {
        Lookup lookup;

        boolean done = false;

        public LookupThread(Lookup lookup)
        {
            this.lookup = lookup;
        }

        /**
         *
         */
        public void run()
        {
            lookup.run();

            synchronized(this)
            {
                done = true;
                notify();
            }
        }

        public Record[] lookUpAndWaitFor(long patience)
        {
            this.start();

            synchronized(this)
            {
                if ( !done )
                {
                    try
                    {
                        wait(patience);
                    }
                    catch(InterruptedException iexc)
                    {
                    }
                }

                done = true;
            }

            return lookup.getAnswers();
        }
    }
}
