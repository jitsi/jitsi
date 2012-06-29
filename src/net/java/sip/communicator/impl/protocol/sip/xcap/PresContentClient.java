/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

import java.net.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent.*;

/**
 * XCAP pres-content client interface.
 * <p/>
 * Compliant with Presence Content XDM Specification v1.0
 *
 * @author Grigorii Balutsel
 */
public interface PresContentClient
{
    /**
     * Pres-content content type
     */
    public static String CONTENT_TYPE = "application/vnd.oma.pres-content+xml";

    /**
     * Pres-content namespace
     */
    public static String NAMESPACE = "urn:oma:xml:prs:pres-content";

    /**
     * Pres-content uri format
     */
    public static String DOCUMENT_FORMAT = "oma_status-icon/users/%1s/%2s";

    /**
     * Puts the pres-content to the server.
     *
     * @param content   the pres-content to be saved on the server.
     * @param imageName the image name under which pres-content would be saved.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public void putPresContent(ContentType content, String imageName)
            throws XCapException;

    /**
     * Gets the pres-content from the server.
     *
     * @param imageName the image name under which pres-content is saved.
     * @return the pres-content or null if there is no pres-content on the
     *         server.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public ContentType getPresContent(String imageName)
            throws XCapException;

    /**
     * Deletes the pres-content from the server.
     *
     * @param imageName the image name under which pres-content is saved.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public void deletePresContent(String imageName)
            throws XCapException;

    /**
     * Gets the pres-content image uri.
     *
     * @param imageName the image name under which pres-content is saved.
     * @return the pres-content image uri.
     * @throws IllegalStateException if the user has not been connected.
     */
    public URI getPresContentImageUri(String imageName);

    /**
     * Gets image from the specified uri.
     *
     * @param imageUri the image uri.
     * @return the image.
     * @throws XCapException if there is some error during operation.
     */
    public byte[] getImage(URI imageUri)
            throws XCapException;
}
