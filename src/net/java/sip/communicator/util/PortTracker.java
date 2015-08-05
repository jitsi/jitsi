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
package net.java.sip.communicator.util;

/**
 * The <tt>PortTracker</tt> class allows for a controlled selection of bind
 * ports. This is typically useful in cases where we would like to set bounds
 * for the ports that we are going to use for a particular socket. For example,
 * at the time of writing of this class, this policy allows Jitsi to bind RTP
 * sockets on ports that are always between 5000 and 6000 (default values). It
 * is also used to allow for different port ranges for Audio and Video streams.
 *
 * @author Emil Ivov
 */
public class PortTracker
{
    /**
     * The local logger.
     */
    private static final Logger logger = Logger.getLogger(NetworkUtils.class);

    /**
     * The minimum port number that this allocator would be allocate to return.
     */
    private int minPort = NetworkUtils.MIN_PORT_NUMBER;

    /**
     * The maximum port number that this allocator would be allocate to return.
     */
    private int maxPort = NetworkUtils.MAX_PORT_NUMBER;

    /**
     * The next port that we will return if asked.
     */
    private int port = -1;

    /**
     * Initializes a port tracker with the specified port range.
     *
     * @param minPort the minimum port that we would like to bind on
     * @param maxPort the maximum port that we would like to bind on
     */
    public PortTracker(int minPort, int maxPort)
    {
        setRange(minPort, maxPort);
    }

    /**
     * Returns the next port that the using class is supposed to try allocating.
     *
     * @return the next port that the using class is supposed to try allocating.
     */
    public int getPort()
    {
        return port;
    }

    /**
     * (Re)Sets the range that this tracker returns values in. The method would
     * also update the value of the next port to allocate in case it is
     * currently outside the specified range. The method also allows configuring
     * this allocator in a way that it would always return the same port. This
     * would happen when <tt>newMinPort</tt> is equal to <tt>newMaxPort</tt>
     * which would make both equal to the only possible value.
     *
     * @param newMinPort the minimum port that we would like to bind on
     * @param newMaxPort the maximum port that we would like to bind on
     *
     * @throws IllegalArgumentException if the arguments do not correspond to
     * valid port numbers, or in case newMaxPort < newMinPort
     */
    public void setRange(int newMinPort, int newMaxPort)
        throws IllegalArgumentException
    {
        //validate
        if((newMaxPort < newMinPort)
                || !NetworkUtils.isValidPortNumber(newMinPort)
                || !NetworkUtils.isValidPortNumber(newMaxPort))
        {
            throw new IllegalArgumentException(
                    "[" + newMinPort + ", " + newMaxPort
                        + "] is not a valid port range.");
        }

        //reset bounds
        minPort = newMinPort;
        maxPort = newMaxPort;

        /*
         * Make sure that nextPort is within the specified range. Preserve value
         * if already valid.
         */
        if (port < minPort || port > maxPort)
            port = minPort;
    }

    /**
     * Attempts to set the range specified by the min and max port string
     * params. If the attempt fails, for reasons such as invalid porameters,
     * this method will simply return without an exception and without an impact
     * on the state of this class.
     *
     * @param newMinPort the minimum port that we would like to bind on
     * @param newMaxPort the maximum port that we would like to bind on
     */
    public void tryRange(String newMinPort, String newMaxPort)
    {
        try
        {
            setRange(
                    Integer.parseInt(newMinPort),
                    Integer.parseInt(newMaxPort));
        }
        catch(Exception e)//Null, NumberFormat, IllegalArgument
        {
            logger.info(
                    "Ignoring invalid port range [" + newMinPort + ", "
                        + newMaxPort + "]");
            if (logger.isDebugEnabled())
                logger.debug("Cause: ", e);
        }
    }

    /**
     * Sets the next port to specified value unless allowing the caller to
     * request validation and force the port into the range that this tracker
     * operates in.
     *
     * @param nextPort the next port we'd like this tracker to return.
     * @param validate determines whether this tracker should bring the new
     * value into its current range.
     */
    public void setNextPort(int nextPort, boolean  validate)
    {
        /*
         * Make sure that nextPort is within the specified range unless
         */
        if ((nextPort < minPort || nextPort > maxPort ) && validate)
        {
            port = minPort;
        }
        else
        {
            this.port = nextPort;
        }
    }

    /**
     * Sets the next port to specified value unless it is outside the range that
     * this tracker operates in, in which case it sets it to the minimal
     * possible.
     *
     * @param nextPort the next port we'd like this tracker to return.
     */
    public void setNextPort(int nextPort)
    {
        setNextPort(nextPort, true);
    }

    /**
     * Returns the lowest/minimum port that this tracker would use.
     *
     * @return the minimum port number allowed by this tracker.
     */
    public int getMinPort()
    {
        return minPort;
    }

    /**
     * Returns the highest/maximum port that this tracker would use.
     *
     * @return the maximum port number allowed by this tracker.
     */
    public int getMaxPort()
    {
        return maxPort;
    }

    /**
     * Attempts to create a port tracker that uses the min and max values
     * indicated by the <tt>newMinPortString</tt> and <tt>newMinPortString</tt>
     * strings and returns it if successful. The method fails silently
     * (returning <tt>null</tt>) otherwise.
     *
     * @param newMinPortString the {@link String} containing the minimum port
     * number that this tracker should allow.
     * @param newMaxPortString the {@link String} containing the minimum port
     * number that this tracker should allow.
     *
     * @return the newly created port tracker or <tt>null</tt> if the string
     * params do not contain valid port numbers.
     */
    public static PortTracker createTracker(String newMinPortString,
                                            String newMaxPortString)
    {
        try
        {
            int minPort = Integer.parseInt(newMinPortString);
            int maxPort = Integer.parseInt(newMaxPortString);

            return new PortTracker(minPort, maxPort);
        }
        catch(Exception exc)//Null, NumberFormat, IllegalArgument
        {
            logger.info("Ignoring invalid port range ["+ newMinPortString
                                              + " to " + newMaxPortString +"]");


            logger.debug("Cause: ", exc);
            return null;
        }
    }

}
