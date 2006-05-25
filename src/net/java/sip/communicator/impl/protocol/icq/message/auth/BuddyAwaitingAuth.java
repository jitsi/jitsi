/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

import java.io.*;

import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joscar.tlv.*;

/**
 * Used to add the buddy to the contact list as awaiting authorization one
 *
 * @author Damian Minkov
 */
public class BuddyAwaitingAuth
    extends SsiItem
{
    private static final Logger logger =
        Logger.getLogger(BuddyAwaitingAuth.class);

    private static final int TYPE_LOCALLY_SPECIFIED_BUDDY_NAME = 0x0131;
    private static final int TYPE_AWAITING_AUTHORIZATION = 0x0066;

    private SsiItem originalItem = null;
    public BuddyAwaitingAuth(SsiItem originalItem)
    {
        super(
            originalItem.getName(),
            originalItem.getParentId(),
            originalItem.getId(),
            originalItem.getItemType(),
            getSpecTlvData());

        this.originalItem = originalItem;
    }

    public void write(OutputStream out) throws IOException
    {
        byte[] namebytes = BinaryTools.getAsciiBytes(originalItem.getName());
        BinaryTools.writeUShort(out, namebytes.length);
        out.write(namebytes);

        BinaryTools.writeUShort(out, originalItem.getParentId());
        BinaryTools.writeUShort(out, originalItem.getId());
        BinaryTools.writeUShort(out, originalItem.getItemType());

        ByteBlock data = getData();
        // here we are nice and let data be null
        int len = data == null ? 0 : data.getLength();
        BinaryTools.writeUShort(out, len);
        if (data != null)
        {
            data.write(out);
        }
    }

    private static ByteBlock getSpecTlvData()
    {
        try
        {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
//            Tlv.getStringInstance(
//                TYPE_LOCALLY_SPECIFIED_BUDDY_NAME,
//                "damencho").write(o);
            new Tlv(TYPE_AWAITING_AUTHORIZATION).write(o);

            ByteBlock block = ByteBlock.wrap(o.toByteArray());
            return block;
        }
        catch (IOException ex)
        {
            logger.error("Error creating buddy awaiting auth tlv", ex);
            return null;
        }
    }
}
