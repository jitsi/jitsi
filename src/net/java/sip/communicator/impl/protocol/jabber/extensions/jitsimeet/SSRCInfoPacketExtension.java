/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;

import java.lang.*;

/**
 * Packet extension is used to signal owner of media SSRC in jitsi-meet. Owner
 * attribute stores MUC JID of the user to whom it belongs. This extension is
 * inserted as a child of {@link SourcePacketExtension} in 'session-initiate',
 * 'source-add' and 'source-remove' Jingle IQs sent by the focus(Jicofo).
 *
 * @author Pawel Domas
 */
public class SSRCInfoPacketExtension
    extends AbstractPacketExtension
{
    /**
     * XML namespace of this packets extension.
     */
    public static final java.lang.String NAMESPACE = "http://jitsi.org/jitmeet";

    /**
     * XML element name of this packets extension.
     */
    public static final String ELEMENT_NAME = "ssrc-info";

    /**
     * Attribute stores owner JID of parent {@link SourcePacketExtension}.
     */
    public static final String OWNER_ATTR_NAME = "owner";

    /**
     * Attribute stores the type of video SSRC. Can be
     * {@link #CAMERA_VIDEO_TYPE} or {@link #SCREEN_VIDEO_TYPE}.
     */
    public static final String VIDEO_TYPE_ATTR_NAME = "video-type";

    /**
     * Camera video type constant. Inidcates that the user is sending his camera
     * video.
     */
    public static final String CAMERA_VIDEO_TYPE = "camera";

    /**
     * Screen video type constant. Indicates that the user is sharing his
     * screen.
     */
    public static final String SCREEN_VIDEO_TYPE = "screen";

    /**
     * Creates new instance of <tt>SSRCInfoPacketExtension</tt>.
     */
    public SSRCInfoPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns the value of {@link #OWNER_ATTR_NAME}.
     *
     * @return MUC JID of SSRC owner stored by this instance or <tt>null</tt>
     *         if empty.
     */
    public String getOwner()
    {
        return getAttributeAsString(OWNER_ATTR_NAME);
    }

    /**
     * Sets the value of {@link #OWNER_ATTR_NAME}.
     *
     * @param owner MUC JID of SSRC owner to be stored in this packet extension.
     */
    public void setOwner(String owner)
    {
        setAttribute(OWNER_ATTR_NAME, owner);
    }

    /**
     * Returns the value of {@link #VIDEO_TYPE_ATTR_NAME}.
     * @return {@link #CAMERA_VIDEO_TYPE}, {@link #SCREEN_VIDEO_TYPE} or
     *         <tt>null</tt> if not specified or if media SSRC is not a video.
     */
    public String getVideoType()
    {
        return getAttributeAsString(VIDEO_TYPE_ATTR_NAME);
    }

    /**
     * Sets the type of video SSRC.
     * @param videoType {@link #CAMERA_VIDEO_TYPE}, {@link #SCREEN_VIDEO_TYPE}
     *        or <tt>null</tt> if not specified or if media SSRC is not a video.
     */
    public void setVideoType(String videoType)
    {
        setAttribute(VIDEO_TYPE_ATTR_NAME, videoType);
    }
}
