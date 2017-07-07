package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

/**
 * Created by bbaldino on 7/6/17.
 */
public class SourceRidGroupPacketExtension extends SourceGroupPacketExtension
{
    /**
     * The name of the "ssrc-group" element.
     */
    public static final String ELEMENT_NAME = "rid-group";

    public SourceRidGroupPacketExtension() {
        super(ELEMENT_NAME);
    }
}
