/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.ulaw;

import java.io.*;
import javax.media.*;

/**
 * Overrides the ULaw Packetizer with a different packet size
 * @author Thomas Hofer
 */
public class Packetizer 
    extends com.sun.media.codec.audio.ulaw.Packetizer
{    
    public Packetizer()
    {
        super();
        packetSize = 240;
        setPacketSize(packetSize);
        
        // Workaround to use our ulaw packetizer
        PlugInManager.removePlugIn("com.sun.media.codec.audio.ulaw.Packetizer",
            PlugInManager.CODEC);
        try
        {
            PlugInManager.commit();
        }
        catch (IOException e)
        {
        }
        PLUGIN_NAME = "ULaw Packetizer";
    }
}
