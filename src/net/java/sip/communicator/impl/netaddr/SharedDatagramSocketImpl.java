package net.java.sip.communicator.impl.netaddr;

import java.net.*;
import java.io.IOException;

/**
 * <p> </p>
 *
 * <p> </p>
 *
 * <p> </p>
 *
 * <p> </p>
 *
 * @author Emil Ivov
 */
public class SharedDatagramSocketImpl
    extends DatagramSocket
{
    private DatagramPacket ourPack = null;
    private DatagramSocket encapsulatedSocket = null;

    SharedDatagramSocketImpl()
        throws java.net.SocketException
    {
        super();
        encapsulatedSocket = new DatagramSocket();
    }

    public void receive(DatagramPacket pack)
        throws IOException
    {

        if(ourPack == null)
        {
            ourPack = new DatagramPacket(pack.getData(), pack.getLength() );
            synchronized (ourPack)
            {
                try
                {
                    ourPack.wait();
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
            super.receive(pack);
        }
    }

    private class SocketReceiveThread extends Thread
    {
        public void run()
        {
            try
            {
                encapsulatedSocket.receive(ourPack);
            }
            catch (IOException ex)
            {
            }

        }
    }

}
