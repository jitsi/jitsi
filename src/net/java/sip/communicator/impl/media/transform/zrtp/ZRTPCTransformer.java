/**
 * Copyright (C) 2006-2008 Werner Dittmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: Werner Dittmann <Werner.Dittmann@t-online.de>
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.impl.media.transform.*;

/**
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */
public class ZRTPCTransformer implements PacketTransformer 
{
    /**
     * ZRTCPTransformer implements PacketTransformer.
     * It encapsulate the encryption / decryption logic for SRTCP packets
     * 
     * This class is currently not used.
     * 
     * @author Bing SU (nova.su@gmail.com)
     */
    //private ZRTPTransformEngine engine;

    /**
     * Constructs a SRTCPTransformer object
     *
     * @param engine The associated ZRTPTransformEngine object
     */
    public ZRTPCTransformer(ZRTPTransformEngine engine) 
    {
        //this.engine = engine;
    }

    /**
     * Encrypt a SRTCP packet
     * 
     * Currently SRTCP packet encryption / decryption is not supported
     * So this method does not change the packet content
     * 
     * @param pkt plain SRTCP packet to be encrypted
     * @return encrypted SRTCP packet
     */
    public RawPacket transform(RawPacket pkt) 
    {
        return pkt;
    }

    /**
     * Decrypt a SRTCP packet
     * 
     * Currently SRTCP packet encryption / decryption is not supported
     * So this method does not change the packet content
     * 
     * @param pkt encrypted SRTCP packet to be decrypted
     * @return decrypted SRTCP packet
     */
    public RawPacket reverseTransform(RawPacket pkt) 
    {
        return pkt;
    }
}
