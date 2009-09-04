/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import java.io.*;
import java.util.*;

import org.xmlpull.v1.*;

/**
 * This class represents the "mail-thread-info" element the Google use in their
 * mail notifications to deliver detailed thread information.
 *
 * @author Emil Ivov
 */
public class MailThreadInfo
{
    /**
     * The name of the "mail-thread-info" element.
     */
    public static final String ELEMENT_NAME = "mail-thread-info";

    /**
     * The name of the XML tag element containing the list of all senders.
     */
    public static final String SENDERS_ELEMENT_NAME = "senders";

    /**
     * The name of the XML tag element containing a pipe separated list of
     * labels assigned to this thread.
     */
    public static final String LABELS_ELEMENT_NAME = "labels";

    /**
     * The name of the XML tag element containing the thread subject.
     */
    public static final String SUBJECT_ELEMENT_NAME = "subject";

    /**
     * The name of the XML tag element containing a snippet of the thread.
     */
    public static final String SNIPPET_ELEMENT_NAME = "snippet";

    /**
     * Contains the list of senders that have participated in this thread.
     */
    private List<Sender> senders = new LinkedList<Sender>();

    /**
     * The thread id of this thread.
     */
    private String tid = null;

    /**
     * Indicates that the local user has not participated in this thread.
     */
    public static final int PARTICIPATION_NONE = 0;

    /**
     * Indicates that the user is one of many recipients listed in the thread.
     */
    public static final int PARTICIPATION_ONE_OF_MANY = 1;

    /**
     * PARTICIPATION_SOLE_RECIPIENT indicates that the user is the sole
     * recipient for messages in this thread.
     */
    public static final int PARTICIPATION_SOLE_RECIPIENT = 2;

    /**
     * A number indicating the local user's participation level in this thread.
     * One of the PARTICIPATION_XXX constants defined above.
     */
    private int participation = -1;

    /**
     * The number of messages in the thread.
     */
    private int messages;

    /**
     * A timestamp of the most recent message, in milliseconds since the UNIX
     * epoch.
     */
    private long date = -1;

    /**
     * The URL linking to this thread (as opposed to the URL delivered in the
     * <tt>MailboxIQ</tt> that links to the mailbox itself).
     */
    private String url = null;

    /**
     * A tag that contains a pipe ('|') delimited list of labels applied to
     * this thread.
     */
    private String labels = null;

    /**
     * The subject of this e-mail thread.
     */
    private String subject = null;

    /**
     * An html-encoded snippet from the body of the email.
     */
    private String snippet = null;

    /**
     * The class describes a single participant in this email thread.
     */
    public class Sender
    {
        /**
         * The name of the XML tag element containing information for an
         * individual sender and represented by this class.
         */
        public static final String ELEMENT_NAME = "sender";
        /**
         * The email address of the sender.
         */
        public String address = null;

        /**
         * The display name of the sender.
         */
        public String name = null;

        /**
         * Indicates whether this sender originated this thread.
         */
        public boolean originator = false;

        /**
         * Indicates whether or not the thread contains an unread message from
         * this sender.
         */
        public boolean unread = false;
    }

    /**
     * Returns the participation index for this thread. The participation index
     * is a number indicating the local user's participation level in this
     * thread: PARTICIPATION_NONE indicates that the user has not participated;
     * PARTICIPATION_ONE_OF_MANY indicates that the user is one of many
     * recipients listed in the thread; PARTICIPATION_SOLE_RECIPIENT indicates
     * that the user is the sole recipient for messages in this thread.
     *
     * @return one of the PARTICIPATION_XXX values defines in this class and
     * indicating whether the local is the sole, one of many or not a
     * participant of this thread.
     */
    public int getParticipation()
    {
        return participation;
    }

    /**
     * Specifies the participation index for this thread. The participation
     * index  is a number indicating the local user's participation level in
     * this thread: PARTICIPATION_NONE indicates that the user has not
     * participated; PARTICIPATION_ONE_OF_MANY indicates that the user is one of
     * many recipients listed in the thread; PARTICIPATION_SOLE_RECIPIENT
     * indicates that the user is the sole recipient for messages in this
     * thread.
     *
     * @param participation one of the PARTICIPATION_XXX values defines in this
     * class and indicating whether the local is the sole, one of many or not a
     * participant of this thread.
     */
    protected void setParticipation(int participation)
    {
        this.participation = participation;
    }

    /**
     * Returns an iterator over a list of one or more sender instances, each of
     * which describes a participant in this thread.
     *
     * @return an iterator over a list of one or more sender instances, each of
     * which describes a participant in this thread.
     */
    public Iterator<Sender> senders()
    {
        return senders.iterator();
    }

    /**
     * Adds <tt>sender</tt> to the list of senders in this thread.
     *
     * @param sender the sender that we are adding.
     */
    protected void addSender(Sender sender)
    {
        senders.add(sender);
    }

    /**
     * Returns the number of messages in this thread.
     *
     * @return the number of messages in this thread.
     */
    public int getMessageCount()
    {
        return messages;
    }

    /**
     * Sets the number of messages in this thread.
     *
     * @param messageCount the number of messages in this thread.
     */
    protected void setMessageCount(int messageCount)
    {
        this.messages = messageCount;
    }

    /**
     * Returns the date of the most recent message in this thread.
     *
     * @return a timestamp of the most recent message, in milliseconds since
     * the UNIX epoch.
     */
    public long getDate()
    {
        return date;
    }

    /**
     * Sets the date of the most recent message in this thread.
     *
     * @param date a timestamp of the most recent message in this thread.
     */
    protected void setDate(long date)
    {
        this.date = date;
    }

    /**
     * Returns an URL linking to this thread. It is important to distinguish
     * between this URL and the one returned by the <tt>MailboxIQ</tt> which
     * points to the whole mailbox.
     *
     * @return the URL linking to this particular thread.
     */
    public String getURL()
    {
        return url;
    }

    /**
     * Sets an URL linking to this thread. It is important to distinguish
     * between this URL and the one returned by the <tt>MailboxIQ</tt> which
     * points to the whole mailbox.
     *
     * @param url the URL linking to this particular thread.
     */
    protected void setURL(String url)
    {
        this.url = url;
    }

    /**
     * Returns a pipe ('|') delimited list of labels applied to this thread.
     *
     * @return a pipe ('|') delimited list of labels applied to this thread.
     */
    public String getLabels()
    {
        return labels;
    }

    /**
     * Sets a pipe ('|') delimited list of labels that apply to this thread.
     *
     * @param labels a pipe ('|') delimited list of labels that apply to this
     * thread.
     */
    protected void setLabels(String labels)
    {
        this.labels = labels;
    }

    /**
     * Returns the ID of this thread.
     *
     * @return the ID of this thread.
     */
    public String getTid()
    {
        return tid;
    }

    /**
     * Specifies the ID of this thread.
     *
     * @param tid the ID of this thread.
     */
    protected void setTid(String tid)
    {
        this.tid = tid;
    }

    /**
     * Returns the subject of this e-mail thread.
     *
     * @return the subject of this e-mail thread.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * Sets the subject of this e-mail thread.
     *
     * @param subject the subject of this e-mail thread.
     */
    protected void setSubject(String subject)
    {
        this.subject = subject;
    }

    /**
     * Returns an html-encoded snippet from the body of the email.
     *
     * @return an html-encoded snippet from the body of the email.
     */
    public String getSnippet()
    {
        return snippet;
    }

    /**
     *Sets an html-encoded snippet from the body of the email.
     *
     * @param snippet an html-encoded snippet from the body of the email.
     */
    protected void setSnippet(String snippet)
    {
        this.snippet = snippet;
    }

    /**
     * Creates and initializes a <tt>MailThreadInfo</tt> instance according to
     * the details that come with the parser.
     *
     * @param parser the parse that we are to read the <tt>MailThreadInfo</tt>
     * from.
     *
     * @return the newly created <tt>MailThreadInfo</tt> instance.
     *
     * @throws XmlPullParserException if something goes wrong while parsing
     * the document.
     * @throws NumberFormatException in case we fail to parse any of the
     * elements that we expect to be numerical.
     * @throws IOException in case reading the input xml fails.
     */
    public static MailThreadInfo parse(XmlPullParser parser)
        throws XmlPullParserException, NumberFormatException, IOException
    {
        MailThreadInfo info = new MailThreadInfo();

        //we start by parsing the thread tag itself which should look something
        //like this:
        // <mail-thread-info tid='1172320964060972012' participation='1'
        //  messages='28' date='1118012394209'
        //  url='http://mail.google.com/mail?view=cv'>

        info.setTid(parser.getAttributeValue("", "tid"));
        info.setParticipation( Integer.parseInt(
                        parser.getAttributeValue("", "participation")));
        info.setMessageCount( Integer.parseInt(
                        parser.getAttributeValue("", "messages")));
        info.setDate( Long.parseLong(
                        parser.getAttributeValue("", "date")));
        info.setURL( parser.getAttributeValue("", "url"));

        //now parse the rest of the message
        int eventType = parser.next();
        while(eventType != XmlPullParser.END_TAG)
        {
            if (eventType == XmlPullParser.START_TAG)
            {
                String name = parser.getName();

                if(SENDERS_ELEMENT_NAME.equals(name))
                {
                    //senders
                    info.parseSenders(parser);
                }
                else if( LABELS_ELEMENT_NAME.equals(name))
                {
                    //labels
                    info.setLabels(parser.nextText());
                }
                else if( SUBJECT_ELEMENT_NAME.equals(name))
                {
                    //subject
                    info.setSubject(parser.nextText());
                }
                else if( SNIPPET_ELEMENT_NAME.equals(name))
                {
                    //snippet
                    info.setSnippet(parser.nextText());
                }
            }

            eventType = parser.next();
        }

        return info;
    }

    /**
     * Parses a list of senders for this thread.
     *
     * @param parser the parser containing the sender list.
     *
     * @throws XmlPullParserException if something goes wrong while parsing
     * the document.
     * @throws NumberFormatException in case we fail to parse any of the
     * elements that we expect to be numerical.
     * @throws IOException in case reading the input xml fails.
     */
    private void parseSenders(XmlPullParser parser)
        throws XmlPullParserException, NumberFormatException, IOException
    {
        //This method parses the list of senders in google mail notifications
        //following is an example of what such a list could look like:
        //
        //<senders>
        //  <sender name='Me' address='romeo@gmail.com' originator='1' />
        //  <sender name='Benvolio' address='benvolio@gmail.com' />
        //  <sender name='Mercutio' address='mercutio@gmail.com' unread='1'/>
        //</senders>

        int eventType = parser.next();

        //looping all senders
        while(eventType != XmlPullParser.END_TAG)
        {

            //looping a single sender ... or in other words not really looping
            //but just making sure that we consume the end tag.
            while(eventType != XmlPullParser.END_TAG)
            {
                String name = parser.getName();

                if(Sender.ELEMENT_NAME.equals(name))
                {
                    Sender sender = new Sender();

                    sender.address = parser.getAttributeValue("", "name");
                    sender.name = parser.getAttributeValue("", "address");

                    String originatorStr
                        = parser.getAttributeValue("", "originator");

                    if(originatorStr != null)
                        sender.originator
                            = (Integer.parseInt(originatorStr) == 1);

                    String unreadStr
                        = parser.getAttributeValue("", "unread");

                    if(unreadStr !=null)
                        sender.unread = Integer.parseInt(unreadStr) == 1;

                    addSender(sender);
                }

                eventType = parser.next();
            }

            eventType = parser.next();
        }

    }
}
