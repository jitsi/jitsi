/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.provdisc.dhcp;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * DHCP transaction class.
 *
 * @author Sebastien Vincent
 */
public class DHCPTransaction
{
    /**
     * Logger.
     */
    private final Logger logger
        = Logger.getLogger(DHCPTransaction.class);

    /**
     * Number of retransmission before giving up.
     */
    private int maxRetransmit = 2;

    /**
     * Current number of retransmission.
     */
    private int nbRetransmit = 0;

    /**
     * Fix interval for retransmission. This final interval will be obtained
     * by adding a random number between [-1, 1].
     */
    private int interval = 2;

    /**
     * The Timer that will trigger retransmission.
     */
    private Timer timer = null;

    /**
     * The DHCP packet content.
     */
    private final DatagramPacket message;

    /**
     * The socket that will be used to retransmit DHCP packet.
     */
    private final DatagramSocket sock;

    /**
     * Constructor.
     *
     * @param sock UDP socket
     * @param message DHCP packet content
     */
    public DHCPTransaction(DatagramSocket sock, DatagramPacket message)
    {
        this.sock = sock;
        this.message = message;
        this.timer = new Timer();
    }

    /**
     * Schedule a timer for retransmission.
     *
     * @throws Exception if message cannot be sent on the socket
     */
    public void schedule() throws Exception
    {
        sock.send(message);

        /* choose a random between [-1, 1] */
        int rand = new Random().nextInt(2) - 1;
        timer.schedule(new RetransmissionHandler(), (interval + rand) * 1000);
    }

    /**
     * Cancel the transaction (i.e stop retransmission).
     */
    public void cancel()
    {
        timer.cancel();
    }

    /**
     * Set the maximum retransmission for a transaction.
     *
     * @param maxRetransmit maximum retransmission for this transaction
     */
    public void setMaxRetransmit(int maxRetransmit)
    {
        this.maxRetransmit = maxRetransmit;
    }

    /**
     * Set the fixed interval for retransmission.
     *
     * @param interval interval to set
     */
    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    /**
     * A <tt>TimerTask</tt> that will handle retransmission of DHCP INFORM.
     *
     * @author Sebastien Vincent
     */
    private class RetransmissionHandler
        extends TimerTask
    {
        /**
         * Thread entry point.
         */
        @Override
        public void run()
        {
            int rand = new Random().nextInt(2) - 1;

            try
            {
                sock.send(message);
            }
            catch(Exception e)
            {
                logger.warn("Failed to send DHCP packet", e);
            }

            nbRetransmit++;
            if(nbRetransmit < maxRetransmit)
            {
                timer.schedule(new RetransmissionHandler(),
                        (interval + rand) * 1000);
            }
        }
    };
}
