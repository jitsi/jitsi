/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import java.net.*;
import java.io.*;

/**
 * This classed is used in the automatic testing of the RSS protocol. It acts as
 * a <b>very</b> simple HTTP server that can serve RSS files. It has the ability
 * to simulate an update to the file, or to send an invalid file.
 *
 * The usual flow when using a <code>TestingServer</code> is as follows:
 * <ol>
 * <li>Create <code>TestingServer</code> object.</li>
 * <li>Set server behaviour through calls to <code>setAtomUsage()</code> and
 *  <code>setServerBehaviour()</code>.</li>
 * <li>Start the server with <code>start()</code>.</li>
 * <li>Retrieve contents served by the server.</li>
 * <li>Stop the server with <code>stop()</code>.</li>
 * <li>If necessary rewind from 2.</li>
 * </ol>
 *
 * @see #setAtomUsage(boolean)
 * @see #setServerBehaviour(int)
 * @see #start()
 * @see #stop()
 * @author Mihai Balan
 */
public class TestingServer
{
    /* Java 1.5 specific - could simplfy things a lot when the switch is decided
     * enum ServerResponse
     * {
     *     VALID, VALID_UPDATE, VALID_NEW, INVALID
     * }
     */
    /**
     * Numeric constant specifying an invalid file to be served.
     */
    public static final int INVALID = 0;

    /**
     * Numeric constant specifying a valid file to be served.
     */
    public static final int VALID = 1;

    /**
     * Numeric constant specifying a valid, updated file to be served.
     */
    public static final int VALID_UPDATE = 2;

    /**
     * Numeric constant specifying a valid, new file to be served.
     */
    public static final int VALID_NEW = 3;

    /**
     * <code>true</code> if ATOM-like files are used, <code>false</code>
     * otherwise.
     */
    private boolean usesAtom;

    /**
     * Flag specifying the current type of file used.
     */
    private int currentFile = VALID;

    /**
     * ServerSocket used to listen for incoming connections.
     */
    private ServerSocket server = null;

    /**
     * Thread for responding to client requests.
     */
    private TestingServerThread runner = null;

    /**
     * <code>true</code> if the server was successfully launched (through a call
     * to <code>start()</code>, <code>false</code> otherwise.
     */
    private boolean serverActive;

    /**
     * Public constructor. Creates the server and binds it to port 8080 of the
     * loop-back address. It uses port 8080 instead of the more
     * standard port 80, because on Linux machines binding on ports smaller than
     * 1023 requires root privileges.
     * @throws IOException in case the server cannot bind to the specified
     * address/port, it throws an IOException detailing the problem.
     */
    public TestingServer()
        throws IOException
    {
        server = new ServerSocket(8080, 20,
            InetAddress.getByName(null));
        usesAtom = false;
        serverActive = false;
        currentFile = INVALID;
    }

    /**
     * Sets whether or not to use ATOM-like files or not
     * @param usesAtom <code>true</code> to serve ATOM-like files,
     * <code>false</code> otherwise.
     */
    public void setAtomUsage(boolean usesAtom)
    {
        this.usesAtom = usesAtom;
    }

    /**
     * Accessor for the field variable <code>usesAtom</code>
     * @return <code>true</code> if ATOM-like files are used, <code>false</code>
     * otherwise.
     */
    public boolean usesAtom()
    {
        return this.usesAtom;
    }

    /**
     * Returns the current state of the server.
     * @return <code>true</code> if a successful call to <code>start()</code>
     * has been issued and the server hasn't yet processed any request,
     * <code>false</code> otherwise.
     */
    public boolean isActive()
    {
        return serverActive;
    }

    /**
     * Sets the type of file the server serves.
     * @param type int enum specifying the type of file.
     */
    public void setServerBehaviour(int type)
    {
        if (type == INVALID || type == VALID || type == VALID_UPDATE
            || type == VALID_NEW )
            currentFile = type;
        else
            currentFile = INVALID;
    }

    /**
     * Return the type of file currently server by the server.
     * @return file type.
     */
    public int getServerBehaviour()
    {
        return currentFile;
    }

    /**
     * Starts the listening process for incoming connections in a separate
     * thread.
     */
    public void start()
    {
        //only allow one "instance" of the server to be running at a given time
        if (serverActive)
            return;

        //create new thread and launch
        runner = new TestingServerThread(this, server);
        serverActive = true;
        runner.start();
    }

    /**
     * Takes the server into an inactive state. If the listening thread is still
     * running it waits for it to end.
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException
    {
        //wait for listening thread to end
        if (runner.isAlive())
            runner.join();

        //restore server state
        serverActive = false;
        runner = null;
    }
}
