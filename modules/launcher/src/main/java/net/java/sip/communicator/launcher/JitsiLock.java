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
package net.java.sip.communicator.launcher;

import java.io.*;
import java.net.*;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import net.java.sip.communicator.launchutils.*;

/**
 * This class is used to prevent from running multiple instances of Jitsi. The
 * class binds a socket somewhere on the localhost domain and records its socket
 * address in the Jitsi configuration directory.
 * All following instances of Jitsi (and hence this class) will look for this
 * record in the configuration directory and try to connect to the original
 * instance through the socket address in there.
 *
 * @author Emil Ivov
 */
@Slf4j
public class JitsiLock
    extends Thread
{
    /**
     * Indicates that something went wrong. More information will probably be
     * available in the console ... if anyone cares at all.
     */
    public static final int LOCK_ERROR = 300;

    /**
     * Returned by the soft start method to indicate that we have successfully
     * started and locked the configuration directory.
     */
    public static final int SUCCESS = 0;

    /**
     * Returned by the soft start method to indicate that an instance of Jitsi
     * has been already started and we should exit. This return code also
     * indicates that all arguments were passed to that new instance.
     */
    public static final int ALREADY_STARTED = 301;

    /**
     * The name of the file that we use to store the address and port that this
     * lock is bound on.
     */
    private static final String LOCK_FILE_NAME = ".lock";

    /**
     * The name of the property that we use to store the address that we bind on
     * in this class.
     */
    private static final String PNAME_LOCK_ADDRESS = "lockAddress";

    /**
     * The name of the property that we use to store the address that we bind on
     * in this class.
     */
    private static final String PNAME_LOCK_PORT = "lockPort";

    /**
     * The header preceding each of the arguments that we toss around between
     * instances of Jitsi
     */
    private static final String ARGUMENT = "Argument";

    /**
     * The name of the header that contains the number of arguments that we send
     * from one instance to another.
     */
    private static final String ARG_COUNT = "Arg-Count";

    /**
     * The name of the header that contains any error messages resulting from
     * remote argument handling.
     */
    private static final String ERROR_ARG = "ERROR";

    /**
     * The carriage return, line feed sequence (\r\n).
     */
    private static final String CRLF = "\r\n";

    /**
     * The number of milliseconds that we should wait for a remote SC instance
     * to come back to us.
     */
    private final long LOCK_COMMUNICATION_DELAY = 1000;

    /**
     * The socket that we use for cross instance lock and communication.
     */
    private ServerSocket instanceServerSocket = null;

    /**
     * Retry times reading the lock file.
     */
    private static final int LOCK_FILE_READ_RETRY = 8;

    /**
     * Time between retires reading lock file in milliseconds.
     */
    private static final long LOCK_FILE_READ_WAIT = 500;

    /**
     * Tries to lock the configuration directory. If lock-ing is not possible
     * because a previous instance is already running, then it transmits the
     * list of args to that running instance.
     * <p>
     * There are three possible outcomes of this method. 1. We lock
     * successfully; 2. We fail to lock because another instance of Jitsi is
     * already running; 3. We fail to lock for some unknown error. Each of these
     * cases is represented by an error code returned as a result.
     *
     * @param args the array of arguments that we are to submit in case an
     * instance of Jitsi has already been started.
     * @return an error or success code indicating the outcome of the lock
     * operation.
     */
    public int tryLock(String[] args)
    {
        // first check whether we have a file.
        File lockFile = getLockFile();

        if (lockFile.exists())
        {
            InetSocketAddress lockAddress = readLockFileRetrying(lockFile);

            if (lockAddress != null)
            {
                // we have a valid lockAddress and hence possibly an already
                // running instance of SC. Try to communicate with it.
                if (interInstanceConnect(lockAddress, args) == SUCCESS)
                {
                    return ALREADY_STARTED;
                }
            }

            // our lockFile is probably stale and left from a previous instance.
            // or an instance that is still running but is not responding.
            lockFile.delete();
        }

        // if we get here then this means that we should go for a real lock
        // initialization
        return lock(lockFile);
    }

    /**
     * Locks the configuration directory by binding our lock socket and
     * recording the lock file into the configuration directory. Returns SUCCESS
     * if everything goes well and ERROR if something fails. This method does
     * not return the ALREADY_RUNNING code as it is assumed that this has
     * already been checked before calling this method.
     *
     * @param lockFile the file that we should use to lock the configuration
     * directory.
     * @return the SUCCESS or ERROR codes defined by this class.
     */
    private int lock(File lockFile)
    {
        // seven time retry binding to port
        int retries = 7;
        while(startLockServer() != SUCCESS && retries > 0)
        {
            retries--;
        }

        // right the bind address in the file
        try
        {
            lockFile.getParentFile().mkdirs();
            lockFile.createNewFile();
        }
        catch (IOException e)
        {
            logger.error("Failed to create lock file" + lockFile, e);
        }

        lockFile.deleteOnExit();
        DeleteOnHaltHook.add(lockFile.getAbsolutePath());

        writeLockFile(lockFile, (InetSocketAddress) instanceServerSocket.getLocalSocketAddress());
        return SUCCESS;
    }

    /**
     * Creates and binds a socket on <tt>lockAddress</tt> and then starts a
     * <tt>LockServer</tt> instance so that we would start interacting with
     * other instances of Jitsi that are trying to start.
     *
     * @return the <tt>ERROR</tt> code if something goes wrong and
     * <tt>SUCCESS</tt> otherwise.
     */
    private int startLockServer()
    {
        try
        {
            // check config directory
            instanceServerSocket = new ServerSocket();
        }
        catch (IOException exc)
        {
            // Just checked the impl and this doesn't seem to ever be thrown
            // .... ignore ...
            logger.error("Couldn't create server socket", exc);
            return LOCK_ERROR;
        }

        try
        {
            instanceServerSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        }
        catch (IOException exc)
        {
            logger.error("Couldn't create server socket", exc);
            return LOCK_ERROR;
        }

        LockServer lockServ = new LockServer(instanceServerSocket);
        lockServ.start();

        return SUCCESS;
    }

    /**
     * Calls reading of the lock file, retrying if there is nothing written in
     * it.
     *
     * @param lockFile the file that we are to parse.
     * @return the <tt>SocketAddress</tt> that we should use to communicate with
     * a possibly already running version of Jitsi.
     */
    private InetSocketAddress readLockFileRetrying(File lockFile)
    {
        int retries = LOCK_FILE_READ_RETRY;
        InetSocketAddress res = null;

        while(res == null && retries > 0)
        {
            res = readLockFile(lockFile);

            if(res == null)
            {
                try
                {
                    Thread.sleep(LOCK_FILE_READ_WAIT);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }

            retries--;
        }

        return res;
    }

    /**
     * Parses the <tt>lockFile</tt> into a standard Properties Object and
     * verifies it for completeness. The method also tries to validate the
     * contents of <tt>lockFile</tt> and asserts presence of all properties
     * mandated by this version.
     *
     * @param lockFile the file that we are to parse.
     * @return the <tt>SocketAddress</tt> that we should use to communicate with
     * a possibly already running version of Jitsi.
     */
    private InetSocketAddress readLockFile(File lockFile)
    {
        Properties lockProperties = new Properties();

        try
        {
            lockProperties.load(new FileInputStream(lockFile));
        }
        catch (Exception exc)
        {
            logger.error("Failed to read lock properties.", exc);
            return null;
        }

        String lockAddressStr = lockProperties.getProperty(PNAME_LOCK_ADDRESS);
        if (lockAddressStr == null)
        {
            logger.error("Lock file contains no lock address.");
            return null;
        }

        String lockPort = lockProperties.getProperty(PNAME_LOCK_PORT);
        if (lockPort == null)
        {
            logger.error("Lock file contains no lock port.");
            return null;
        }

        InetAddress lockAddress = findLocalAddress(lockAddressStr);

        if (lockAddress == null)
        {
            logger.error(lockAddressStr + " is not a valid local address.");
            return null;
        }

        int port;
        try
        {
            port = Integer.parseInt(lockPort);
        }
        catch (NumberFormatException exc)
        {
            logger.error(lockPort + " is not a valid port number.", exc);
            return null;
        }

        return new InetSocketAddress(lockAddress, port);
    }

    /**
     * Records our <tt>lockAddress</tt> into <tt>lockFile</tt> using the standard properties format.
     *
     * @param lockFile    the file that we should store the address in.
     * @param lockAddress the address that we have to record.
     */
    private void writeLockFile(File lockFile, InetSocketAddress lockAddress)
    {
        Properties lockProperties = new Properties();

        lockProperties.setProperty(
                PNAME_LOCK_ADDRESS,
                lockAddress.getAddress().getHostAddress());
        lockProperties.setProperty(
                PNAME_LOCK_PORT,
                Integer.toString(lockAddress.getPort()));

        try
        {
            lockProperties.store(
                    new FileOutputStream(lockFile),
                    "Jitsi lock file. This file will be automatically removed"
                        + " when execution of Jitsi terminates.");
        }
        catch (IOException e)
        {
            logger.error("Failed to create lock file", e);
        }
    }

    /**
     * Returns a reference to the file that we should be using to lock Jitsi's
     * home directory, whether it exists or not.
     *
     * @return a reference to the file that we should be using to lock Jitsi's
     * home directory.
     */
    private File getLockFile()
    {
        String homeDirLocation =
            System
                .getProperty(Jitsi.PNAME_SC_CACHE_DIR_LOCATION);
        String homeDirName = System
                        .getProperty(Jitsi.PNAME_SC_HOME_DIR_NAME);

        return new File(new File(homeDirLocation, homeDirName), LOCK_FILE_NAME);
    }

    /**
     * Returns an <tt>InetAddress</tt> instance corresponding to
     * <tt>addressStr</tt> or <tt>null</tt> if no such address exists on the
     * local interfaces.
     *
     * @param addressStr the address string that we are trying to resolve into
     * an <tt>InetAddress</tt>
     * @return an <tt>InetAddress</tt> instance corresponding to
     * <tt>addressStr</tt> or <tt>null</tt> if none of the local interfaces has
     * such an address.
     */
    private InetAddress findLocalAddress(String addressStr)
    {
        Enumeration<NetworkInterface> ifaces;

        try
        {
            ifaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException exc)
        {
            logger.error(
                  "Could not extract the list of local intefcaces.",
                  exc);
            return null;
        }

        // loop through local interfaces
        while (ifaces.hasMoreElements())
        {
            NetworkInterface iface = ifaces.nextElement();

            Enumeration<InetAddress> addresses = iface.getInetAddresses();

            // loop iface addresses
            while (addresses.hasMoreElements())
            {
                InetAddress addr = addresses.nextElement();

                if (addr.getHostAddress().equals(addressStr))
                    return addr;
            }
        }
        return null;
    }

    /**
     * Initializes a client TCP socket, connects if to <tt>sockAddr</tt> and
     * sends all <tt>args</tt> to it.
     *
     * @param sockAddr the address that we are to connect to.
     * @param args the args that we need to send to <tt>sockAddr</tt>.
     * @return <tt>SUCCESS</tt> upond success and <tt>ERROR</tt> if anything
     * goes wrong.
     */
    private int interInstanceConnect(InetSocketAddress sockAddr, String[] args)
    {
        try
        {
            Socket interInstanceSocket = new Socket(sockAddr.getAddress(),
                            sockAddr.getPort());

            LockClient lockClient = new LockClient(interInstanceSocket);
            lockClient.start();

            PrintStream printStream = new PrintStream(interInstanceSocket
                            .getOutputStream());

            printStream.print(ARG_COUNT + "=" + args.length + CRLF);

            for (String arg : args)
            {
                printStream.print(ARGUMENT + "=" + arg + CRLF);
            }

            lockClient.waitForReply(LOCK_COMMUNICATION_DELAY);

            //NPEs are handled in catch so no need to check whether or not we
            //actually have a reply.
            String serverReadArgCountStr = lockClient.message
                .substring((ARG_COUNT + "=").length());

            int serverReadArgCount = Integer.parseInt(serverReadArgCountStr);
            if (logger.isDebugEnabled())
                logger.debug("Server read " + serverReadArgCount + " args.");

            if(serverReadArgCount != args.length)
                return LOCK_ERROR;

            printStream.flush();
            printStream.close();
            interInstanceSocket.close();
        }
        //catch IOExceptions, NPEs and NumberFormatExceptions here.
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to connect to a running sc instance.");
            return LOCK_ERROR;
        }

        return SUCCESS;
    }

    /**
     * We use this thread to communicate with an already running instance of
     * Jitsi. This thread will listen for a reply to a message that we've sent
     * to the other instance. We will wait for this message for a maximum of
     * <tt>runDuration</tt> milliseconds and then consider the remote instance
     * dead.
     */
    private static class LockClient extends Thread
    {
        /**
         * The <tt>String</tt> that we've read from the socketInputStream
         */
        public String message = null;

        /**
         * The socket that this <tt>LockClient</tt> is created to read from.
         */
        private final Socket interInstanceSocket;

        /**
         * Creates a <tt>LockClient</tt> that should read whatever data we
         * receive on <tt>sockInputStream</tt>.
         *
         * @param commSocket the socket that this client should be reading from.
         */
        public LockClient(Socket commSocket)
        {
            super(LockClient.class.getName());
            setDaemon(true);
            this.interInstanceSocket = commSocket;
        }


        /**
         * Blocks until a reply has been received or until run<tt>Duration</tt>
         * milliseconds had passed.
         *
         * @param runDuration the number of seconds to wait for a reply from the
         * remote instance
         */
        public void waitForReply(long runDuration)
        {
            try
            {
                synchronized(this)
                {
                    //return if we have already received a message.
                    if(message != null)
                        return;

                    wait(runDuration);
                }

                if (logger.isDebugEnabled())
                    logger.debug("Done waiting. Will close socket");
                interInstanceSocket.close();
            }
            catch (Exception exception)
            {
                logger.error("Failed to close our inter instance input stream",
                                exception);
            }
        }

        /**
         * Simply collects everything that we read from the InputStream that
         * this <tt>InterInstanceCommunicationClient</tt> was created with.
         */
        @Override
        public void run()
        {
            try
            {
                BufferedReader lineReader = new BufferedReader(
                    new InputStreamReader(interInstanceSocket
                        .getInputStream()));

                //we only need to read a single line and then bail out.
                message = lineReader.readLine();
                if (logger.isDebugEnabled())
                    logger.debug("Message is " + message);
                synchronized(this)
                {
                    notifyAll();
                }
            }
            catch (IOException exc)
            {
                // does not necessarily mean something is wrong. Could be
                // that we got tired of waiting and want to quit.
                if (logger.isInfoEnabled())
                {
                    logger.info(
                            "An IOException is thrown while reading sock",
                            exc);
                }
            }
        }
    }

    /**
     * We start this thread when running Jitsi as a means of notifying others
     * that this is
     */
    private static class LockServer extends Thread
    {
        /**
         * The socket that we use for cross instance lock and communication.
         */
        private final ServerSocket lockSocket;

        /**
         * Creates an instance of this <tt>LockServer</tt> wrapping the
         * specified <tt>serverSocket</tt>. It is expected that the serverSocket
         * will be already bound and ready to accept.
         *
         * @param serverSocket the serverSocket that we should use for inter
         * instance communication.
         */
        public LockServer(ServerSocket serverSocket)
        {
            super(LockServer.class.getName());
            setDaemon(true);
            this.lockSocket = serverSocket;
        }

        @Override
        public void run()
        {
            try
            {
                while (true)
                {
                    Socket instanceSocket = lockSocket.accept();
                    new LockServerConnectionProcessor(instanceSocket).start();
                }
            }
            catch (Exception exc)
            {
                logger.warn("Someone tried ", exc);
            }
        }
    }

    /**
     * We use this thread to handle individual messages in server side inter
     * instance communication.
     */
    private static class LockServerConnectionProcessor extends Thread
    {
        /**
         * The socket that we will be using to communicate with the fellow Jitsi
         * instance.
         */
        private final Socket connectionSocket;

        /**
         * Creates an instance of <tt>LockServerConnectionProcessor</tt> that
         * would handle parameters received through the
         * <tt>connectionSocket</tt>.
         *
         * @param connectionSocket the socket that we will be using to read
         * arguments from the remote Jitsi instance.
         */
        public LockServerConnectionProcessor(Socket connectionSocket)
        {
            this.connectionSocket = connectionSocket;
        }

        /**
         * Starts reading messages arriving through the connection socket.
         */
        @Override
        public void run()
        {
            InputStream is;
            PrintWriter printer;
            try
            {
                is = connectionSocket.getInputStream();
                printer = new PrintWriter(connectionSocket
                                .getOutputStream());
            }
            catch (IOException exc)
            {
                logger.warn("Failed to read arguments from another SC instance",
                                exc);
                return;
            }

            ArrayList<String> argsList = new ArrayList<String>();

            logger.debug("Handling incoming connection");
            int argCount = 1024;
            try
            {
                var lineReader = new BufferedReader(new InputStreamReader(is));

                while (true)
                {
                    String line = lineReader.readLine();
                    logger.debug(line);

                    if (line.startsWith(ARG_COUNT))
                    {
                        argCount = Integer.parseInt(line
                                        .substring((ARG_COUNT + "=").length()));
                    }
                    else if (line.startsWith(ARGUMENT))
                    {
                        String arg = line.substring((ARGUMENT + "=").length());
                        argsList.add(arg);
                    }
                    // ignore unknown headers.

                    if (argCount <= argsList.size())
                        break;
                }

                // first tell the remote application that everything went OK
                // and end the connection so that it could exit
                printer.print(ARG_COUNT + "=" + argCount + CRLF);
                printer.close();
                connectionSocket.close();

                // now let's handle what we've got
                String[] args = new String[argsList.size()];
                LaunchArgHandler.getInstance()
                    .handleConcurrentInvocationRequestArgs(
                                argsList.toArray(args));
            }
            catch (IOException exc)
            {
                if (logger.isInfoEnabled())
                    logger.info("An IOException is thrown while "
                                + "processing remote args", exc);

                printer.print(ERROR_ARG + "=" + exc.getMessage());
            }
        }
    }
}
