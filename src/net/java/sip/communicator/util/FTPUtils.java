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

import java.io.*;
import java.net.*;

/**
 * Utility class that allows to check the size of ftp file.
 * 
 * @author Hristo Terezov
 */
public class FTPUtils
{ 
    /**
     * The connection 
     * to the FTP server.
     */
    private Socket socket = null;

    /**
     * The reader from the connection.
     */
    private BufferedReader reader = null;

    /**
     * The writer which is used to send commands to the server.
     */
    private BufferedWriter writer = null;

    /**
     * Default port constant. It is used when the port is not available in URL.
     */
    private final int DEFAULT_PORT = 21;

    /**
     * Constant for the invalid file size.
     */
    private final int INVALID_FILE_SIZE = -1;

    /**
     * The user name for the FTP connection.
     */
    private String user = null;

    /**
     * The password for the FTP connection.
     */
    private String pass = null;

    /**
     * The path to the file.
     */
    private String path = null;

    /**
     * The host name of the FTP server.
     */
    private String host = null;

    /**
     * The port for the FTP connection.
     */
    private int port = DEFAULT_PORT;

    /**
     * Parses the URL, connects to the FTP server and then executes the login.
     *
     * @param urlString the URL of the file.
     * @throws Exception if something with parsing the URL or connection or 
     * login fails.
     */
    public FTPUtils(String urlString) throws Exception
    {
        parseUrl(urlString);
        socket = new Socket( host, port);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));
        checkConnectionGreetings();
        login();
    }

    /**
     * Reads the connection greetings messages from the FTP server
     * checks the response code.
     * 
     * @throws Exception if the response code is not for success.
     */
    private void checkConnectionGreetings() throws Exception
    {
       String code = getResponseCode();
       if(!code.equals("220"))
       {
           throw new Exception("Connection Error.");
       }
    }

    /**
     * Reads the response messages from the FTP server and
     * returns the response code.
     * 
     * @return the response code.
     * @throws Exception if <tt>readLine</tt> fails.
     */
    private String getResponseCode() throws Exception
    {
        String line;
        while((line = readLine()).charAt(3) != ' ');

        return line.substring(0, 3);
    }

    /**
     * Executes the login sequence of FTP commands based on RFC 959.
     * 
     * @throws Exception if the login fails.
     */
    private void login() throws Exception
    {
        sendLine("USER " + user);
        String code = getResponseCode();
        if(code.equals("331") || code.equals("332"))
        {
            sendLine("PASS " + pass);
            code = getResponseCode();
            if(!code.equals("230"))
            {
                throw new Exception("Login error.");
            }
        }
        else if(!code.equals("230"))
        {
            throw new Exception("Login error.");
        }
    }

    /**
     * Sends FTP command for the size of the file and reads and parses
     * the response from the the FTP server.
     * 
     * @return the size of the file in bytes.
     * @throws Exception if <tt>readLine</tt> fails.
     */
    public int getSize() throws Exception
    {
        sendLine("SIZE " + path);

        String line = readLine();
        if(!line.startsWith("213 "))
        {
            throw new Exception("Size Error.");
        }

        String fileSizeStr = line.substring(4);
        int fileSize = INVALID_FILE_SIZE;

        try
        {
            fileSize = Integer.parseInt(fileSizeStr);
        }
        catch (NumberFormatException e)
        {
            return INVALID_FILE_SIZE;
        }

        return fileSize;
    }

    /**
     * Parses the URL to host, port, user, password and path to the file parts.
     *
     * @param urlString the URL of the file.
     * @throws Exception if the parsing of the URL fails.
     */
    private void parseUrl(String urlString) throws Exception
    {
        URL url = new URL(urlString);

        host = url.getHost();
        port = url.getPort();
        if (port == -1)
        {
            port = DEFAULT_PORT;
        }

        String tmpUserInfo = url.getUserInfo();
        if(tmpUserInfo != null)
        {
            int separatorIdx = tmpUserInfo.lastIndexOf(':');
            if (separatorIdx != -1)
            {
                pass = tmpUserInfo.substring(separatorIdx+1);
                user = tmpUserInfo.substring(0, separatorIdx);
            }
        }

        if(user == null)
        {
            user ="anonymus";
        }

        if(pass == null)
        {
            pass ="anonymus";
        }

        path = url.getPath();
        if(path == "")
        {
            throw new Exception("Not available path.");
        }
    }

    /**
     * Disconnects from the FTP server.
     */
    public void disconnect() throws IOException {
        sendLine("QUIT");
    }

    /**
     * Sends a raw command to the FTP server.
     * 
     * @param line the message that will be send to the FTP server.
     * @throws IOException if sending fails.
     */
    private void sendLine(String line) throws IOException {
        writer.write(line + "\r\n");
        writer.flush();
    }

    /**
     * Reads a line from the response of the FTP server.
     * 
     * @return line from the response of the FTP server.
     * @throws IOException if reading fails.
     */
    private String readLine() throws IOException {
        String line = reader.readLine();
        return line;
    }
}
