/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * This class represents the separate thread that a <code>TestingServer</code>
 * uses to listen for incoming connections.
 * A reference to the object that launched the thread is also kept too in order
 * to have easier access to the server settings.
 *
 * @author Mihai Balan
 */
public class TestingServerThread
    extends Thread
{
    /**
     * The server that launched us.
     */
    private TestingServer launcher = null;

    /**
     * The server's <code>ServerSocket</code>. We keep a reference to it to as
     * the call to <code>ServerSocket.accept()</code> is made in the thread (us)
     * and not in the server to avoid deadlocks.
     */
    private ServerSocket serverSocket;
    /**
     * <code>Socket</code> used for network I/O.
     */
    private Socket socket;

    /**
     * Creates a new thread for listening for connections.
     * @param launcher the <code>TestingServer</code> object that created us.
     * @param serverSock the <code>ServerSocket</code> used to listen for
     * connections on.
     */
    public TestingServerThread(TestingServer launcher, ServerSocket serverSock)
    {
        /* would a check for null (mainly for the launcher) and an eventual
         * NullPointerException make sense here?*/
        this.launcher = launcher;
        this.serverSocket = serverSock;
    }

    /**
     * Effectively launches the server thread. It starts listening for
     * connections on <code>serverSocket</code>, and upon a successful request
     * serves the file according to the <code>launcher</code>'s settings.
     */
    public void run()
    {
        if (launcher.isActive())
        {
            try {
            socket = serverSocket.accept();

            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            processRequest(in);

            launcher.stop();

            } catch (IOException e)
            {
            e.printStackTrace();
            socket = null;
            } catch (InterruptedException ie)
            {
            ie.printStackTrace();
            }
        }
    }

    /**
     * Processes the HTTP request and initiates the sending of a response
     * according to the server settings.
     *
     * @param in network end-point from which we read.
     *
     * @throws IOException
     */
    public void processRequest(BufferedReader in)
        throws IOException
    {
        String crtLine;
        String httpVersion = null;
        boolean gotRequest = false;

        //XXX: Debug
        //System.out.println("Processing request. Dump follows:");

        //look for the get string and totally and blindly ignore all other
        //HTTP headers
        while (! "".equals(crtLine = in.readLine().trim()) && crtLine != null)
        {
            //XXX: Debug
            //System.out.println("> " + crtLine);
            if (crtLine.startsWith("GET /"))
            {
            gotRequest = true;
            int httpPos = -1;
            httpPos = crtLine.lastIndexOf(" HTTP/");
            if (httpPos != -1)
                httpVersion = crtLine.substring(httpPos + 1);
            else
                httpVersion = "HTTP/1.0";
            }
        }

        //if no file was requested, just abort
        if (! gotRequest)
            return;

        BufferedWriter out = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));

        sendResponse(out, httpVersion);
    }

    /**
     * Builds and sends the HTTP response according to the server settings.
     *
     * @param out the network end-point to which we write to.
     * @param httpVersion textual representation of the HTTP version used.
     * Should either be "HTTP/1.0" or "HTTP/1.1".
     *
     * @throws IOException
     */
    public void sendResponse(BufferedWriter out, String httpVersion)
        throws IOException
    {
        String content = "";
        SimpleDateFormat dateFormatter =
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

        //getting output
        switch (launcher.getServerBehaviour())
        {
        case TestingServer.INVALID:
            content = launcher.usesAtom() ? FeedFactory.getAtomInvalid()
                : FeedFactory.getRssInvalid();
            break;
        case TestingServer.VALID:
            content = launcher.usesAtom() ? FeedFactory.getAtom()
                : FeedFactory.getRss();
            break;
        case TestingServer.VALID_UPDATE:
            content = launcher.usesAtom() ? FeedFactory.getAtomUpdated()
                : FeedFactory.getRssUpdated();
            break;
        case TestingServer.VALID_NEW:
            content = launcher.usesAtom() ? FeedFactory.getAtomNew()
                : FeedFactory.getRssNew();
            break;
        }

        //XXX: Debug
        //System.out.println("Sending response headers...");

        //building response HTTP headers
        out.write(httpVersion + "200 OK\r\n");
        out.write("Server: RssTestingServer/0.0.1\r\n");
        out.write("Date: " + dateFormatter.format(new Date()) + "\r\n");
        out.write("Content-Type: text/html; charset=utf-8\r\n");
        out.write("Content-length: " + content.length() + "\r\n");
        out.write("\r\n");

        //outputting the file
        out.write(content);

        out.close();
        socket.close();
        socket = null;
    }
}
