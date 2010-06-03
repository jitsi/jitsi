/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.imageio.*;

import com.ctreber.aclib.image.ico.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A class that implements threaded retrieval for rss images. We launch from
 * within the presence operation set in order to retrieve image icons without
 * blocking.
 *
 * @author Emil Ivov
 */
public class ImageRetriever extends Thread
{
    private static final Logger logger = Logger.getLogger(ImageRetriever.class);

    /**
     * list with the accounts with missing image
     */
    private Vector<ContactRssImpl> contactsForUpdate = new Vector<ContactRssImpl>();

    /**
     * The operation set that created us.
     */
    private OperationSetPersistentPresenceRssImpl parentOperationSet = null;

    /**
     * The path within the bundle for the default RSS 64x64 icon.
     */
    private String defaultIconId = "pageImageRss";

    /**
     * Creates the thread
     */
    ImageRetriever(OperationSetPersistentPresenceRssImpl parentOpSet)
    {
        super("RssImageRetriever");
        setDaemon(true);
        this.parentOperationSet = parentOpSet;
    }

    /**
     * Updates all contacts that we add to this retriever, and gets back to
     * sleep if there aren't any.
     */
    public void run()
    {
        try
        {
            Collection<ContactRssImpl> copyContactsForUpdate = null;
            while (true)
            {
                synchronized (contactsForUpdate)
                {
                    if (contactsForUpdate.isEmpty())
                        contactsForUpdate.wait();

                    copyContactsForUpdate = new Vector<ContactRssImpl>(
                                    contactsForUpdate);
                    contactsForUpdate.clear();
                }

                Iterator<ContactRssImpl> iter = copyContactsForUpdate
                                .iterator();
                while (iter.hasNext())
                {
                    ContactRssImpl contact = iter.next();

                    byte[] imgBytes = getAvatar(contact);

                    if (imgBytes != null)
                    {
                        byte[] oldImage = contact.getImage(false);
                        contact.setImage(imgBytes);

                        parentOperationSet.fireContactPropertyChangeEvent(
                            ContactPropertyChangeEvent.PROPERTY_IMAGE,
                            contact,
                            oldImage,
                            imgBytes);
                    }
                }
            }
        }
        catch (InterruptedException ex)
        {
            logger.error("NickRetriever error waiting will stop now!", ex);
        }
    }

    /**
     * Add contact for retreiving if the provider is register notify the
     * retreiver to get the nicks if we are not registered add a listener to
     * wiat for registering
     *
     * @param contact
     *            the contact that we'd like to add for image retrieval.
     */
    synchronized void addContact(ContactRssImpl contact)
    {
        synchronized (contactsForUpdate)
        {
            if (!contactsForUpdate.contains(contact))
            {
                contactsForUpdate.add(contact);
                contactsForUpdate.notifyAll();
            }
        }
    }

    /**
     * Executes the actual image retriever.
     *
     * @param contact
     *            the contact that we'd like to retrieve an image for.
     *
     * @return a byte array with the image of the specified contact.
     */
    private byte[] getAvatar(ContactRssImpl contact)
    {
        try
        {
            //we need to also use findFavIconFromSiteIndex but right now
            //i can't find a quick way to reliably convert a URL (possibly a
            //PNG an ICO or a 301 redirection) into an image byte array.
            byte[] image = getFavIconFromSiteRoot(contact);

            if (image == null)
            {
                image = getDefaultRssIcon();
            }


            return image;
        }
        catch (Exception exc)
        {
            if (logger.isTraceEnabled())
                logger.trace("Cannot load image for contact " + this + " : "
                            + exc.getMessage(), exc);

            return null;
        }
    }

    /**
     * Returns the address of a favicon (i.e. html "icon" or "shortcut icon") if
     * any is contained by the page on <tt>urlAddress</tt> or <tt>null</tt> if
     * the page on the specified address does not contain a link to a favicon.
     *
     * @param urlAddress
     *            the address of the page that we'd liek to check for a fav
     *            icon.
     *
     * @return the address of the favicon for the <tt>urlAddress</tt> page or
     *         <tt>null</tt> if the page does not define such an icon.
     */
    private String findFavIconFromSiteIndex(ContactRssImpl contact)
    {
        try
        {
            URL feedLocation = new URL(contact.getRssFeedReader().getURL());

            URL siteIndex = new URL(feedLocation.getProtocol() + "://"
                            + feedLocation.getHost());

            BufferedReader in = new BufferedReader(new InputStreamReader(
                            siteIndex.openStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null)
            {
                int index = 0;
                while (index != -1)
                {
                    int tagBeginIndex = inputLine.toLowerCase().indexOf(
                                    "<link", index);

                    if (tagBeginIndex == -1)
                    {
                        break;
                    }

                    int tagEndIndex = inputLine.toLowerCase().indexOf(">",
                                    tagBeginIndex);
                    if (tagEndIndex != -1)
                    {
                        String linkTag = inputLine.substring(tagBeginIndex,
                                        tagEndIndex + 1);

                        boolean isIconTag = linkTag.toLowerCase().matches(
                                        ".*rel=.icon..*");

                        if (!isIconTag)
                            isIconTag = linkTag.toLowerCase().matches(
                                            ".*rel=.shortcut icon..*");

                        if (isIconTag)
                        {
                            // find the value of the href tag
                            int hrefInd = linkTag.toLowerCase()
                                            .indexOf("href=");
                            hrefInd += ("href=".length() + 1);
                            char startQuote = linkTag.charAt(hrefInd - 1);
                            int endQuoteInd = linkTag.indexOf(startQuote,
                                            hrefInd);

                            String iconLink = linkTag.substring(hrefInd,
                                            endQuoteInd);

                            return iconLink;
                        }
                    }
                    index = tagEndIndex + 1;
                }
            }
            in.close();
        }
        catch (Exception exc)
        {
            if (logger.isTraceEnabled())
                logger.trace("Failed to retrieve link image.", exc);
        }

        return null;
    }

    /**
     * Looks for and fetches a possible favicon.ico icon from the host that
     * hosts the specified URI.
     *
     * @return
     */
    private byte[] getFavIconFromSiteRoot(ContactRssImpl contact)
    {
        Image selectedIcon;
        URL location = null;

        // we use these to get the best possible icon in case our favicon is a
        // multi-page icon.
        int maxWidth = 0;
        int maxColors = 0;
        int crtDescriptor = -1;

        // used for ICO to PNG translation. Uses PNG as it's the "safest"
        // choice.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] result = null;

        try
        {
            URL feedLocation = new URL(contact.getRssFeedReader().getURL());

            location = new URL(feedLocation.getProtocol() + "://"
                            + feedLocation.getHost() + "/favicon.ico");

            ICOFile favicon = new ICOFile(location);

            if (logger.isTraceEnabled())
                logger.trace("Icon has " + favicon.getImageCount() + " pages");

            for (int i = 0; i < favicon.getDescriptors().size(); i++)
            {
                BitmapDescriptor bmpDesc = favicon.getDescriptor(i);
                if ((maxWidth < bmpDesc.getWidth()))
                {
                    maxWidth = bmpDesc.getWidth();
                    maxColors = bmpDesc.getColorCount();
                    crtDescriptor = i;
                }

                if ((maxColors < bmpDesc.getColorCount()))
                {
                    maxWidth = bmpDesc.getWidth();
                    maxColors = bmpDesc.getColorCount();
                    crtDescriptor = i;
                }
            }

            // if icons is either invalid or contains no data, return the
            // default
            // RSS icon.
            if (crtDescriptor == -1)
            {
                return null;
            }

            selectedIcon = favicon.getDescriptor(crtDescriptor).getImageRGB();

            // decode ICO as a PNG and return the result
            ImageIO.write((BufferedImage) selectedIcon, "PNG", output);
            result = output.toByteArray();

            if (logger.isTraceEnabled())
                logger.trace("Result has " + result.length + " bytes");
            if (logger.isTraceEnabled())
                logger.trace("Icon is " + maxWidth + " px X " + maxWidth + " px @ "
                            + maxColors + " colors");

            output.close();
            return result;
        }
        catch (MalformedURLException murlex)
        {
            // this shouldn't happen. Ever.
            logger.error("Malformed URL " + murlex, murlex);
        }
        catch (IOException ioex)
        {
            logger.warn("I/O Error on favicon retrieval. " + ioex.getMessage());
            if (logger.isDebugEnabled())
                logger.debug("I/O Error on favicon retrieval. " + ioex, ioex);
        }
        catch (Exception ex)
        {
            logger.warn("Unknown error on favicon retrieval. " + ex +
                    ". Error for location: " + location, ex);
            if (logger.isDebugEnabled())
                logger.debug("", ex);
        }

        return null;
    }

    /**
     * Returns the default icon in case the feed has no favicon on the server.
     * Uses the <tt>defaultIconPath</tt> constant to locate the default icon to
     * be displayed.
     *
     * @return binary representation of the default icon.
     */
    private byte[] getDefaultRssIcon()
    {
        if (logger.isTraceEnabled())
            logger.trace("Loading default icon at " + defaultIconId);

        return ProtocolIconRssImpl.getImageInBytes(defaultIconId);
    }
}
