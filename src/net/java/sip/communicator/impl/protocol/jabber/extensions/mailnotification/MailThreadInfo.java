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
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import java.io.*;
import java.text.*;
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
     * The format that we are using to display dates when generating html.
     */
    private String formattedDate = null;

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

        /**
         * Tries to parse and return the first name of this sender.
         *
         * @return the first name of this sender.
         */
        public String getFirstName()
        {
            if(name == null || name.trim().length() == 0)
            {
                return null;
            }

            String[] names = name.split("\\s");

            String result = names[0];

            //return 14 chars max
            if(result.length() > 14)
                result = result.substring(0, 14);

            return result;
        }
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
     * Returns the number of people that have been posting in this thread.
     *
     * @return the number of people that have been posting in this thread.
     */
    public int getSenderCount()
    {
        return senders.size();
    }

    /**
     * Returns the number of people that have been posting in this thread and
     * that we have unread messages from.
     *
     * @return the number of people that have been posting in this thread and
     * that we have unread messages from.
     */
    public int getUnreadSenderCount()
    {
        Iterator<Sender> senders = senders();

        int count = 0;
        while(senders.hasNext())
        {
            if(senders.next().unread)
                count ++;
        }

        return count;
    }

    /**
     * Returns the sender that initiated the thread or the first sender in the
     * list if for some reason we couldn't determine the originator.
     *
     * @param firstNameOnly use only first name
     * @return the sender that initiated the thread or the first sender in the
     * list if for some reason we couldn't determine the originator.
     */
    public String findOriginator(boolean firstNameOnly)
    {
        return null;
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
     * Returns a human readable date.
     *
     * @return a human readable date
     */
    private String getFormattedDate()
    {
        if (formattedDate != null)
            return formattedDate;

        StringBuffer dateBuff = new StringBuffer();

        Calendar now = Calendar.getInstance();
        Date threadDate = new Date(getDate());
        Calendar threadDateCal = Calendar.getInstance();
        threadDateCal.setTime(new Date(getDate()));


        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT);
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        if (now.get(Calendar.YEAR) != threadDateCal.get(Calendar.YEAR)
            || now.get(Calendar.MONTH) != threadDateCal.get(Calendar.MONTH)
            || now.get(Calendar.DAY_OF_MONTH)
                    != threadDateCal.get(Calendar.DAY_OF_MONTH))
        {
            //the message is not from today so include the full date.
            dateBuff.append(dateFormat.format(threadDate));
        }

        dateBuff.append(" ").append(timeFormat.format(threadDate));

        return dateBuff.toString();
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

        String participationStr = parser.getAttributeValue("", "participation");

        if(participationStr != null)
            info.setParticipation( Integer.parseInt( participationStr ));

        String messagesStr = parser.getAttributeValue("", "messages");

        if( messagesStr != null )
            info.setMessageCount( Integer.parseInt( messagesStr ));

        String dateStr = parser.getAttributeValue("", "date");

        if(dateStr != null)
            info.setDate( Long.parseLong( dateStr ));

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

                    sender.address = parser.getAttributeValue("", "address");
                    sender.name = parser.getAttributeValue("", "name");

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

    /**
     * Creates an html description of all participant names in the thread.
     * We try to do this in a Gmail-like (although quite simplified) way:<br/>
     * We print the whole name for a sole participant. <br/>
     * We print only the first names for more than one participant. <br/>
     * We print up to three names max. <br/>
     * We show in bold people that we have unread messages for <br/>.
     *
     * @return an html description of <tt>thread</tt>
     */
    private String createParticipantNames()
    {
        StringBuffer participantNames = new StringBuffer();

        //if we have more than one sender we only show first names
        boolean firstNamesOnly = getSenderCount() > 1;

        int unreadSenderCount = getUnreadSenderCount();

        int maximumSndrsAllowed = 3;
        int remainingSndrsAllowed = maximumSndrsAllowed;
        int maximumUnreadAllowed = Math.min(
                        remainingSndrsAllowed, unreadSenderCount);
        int maximumReadAllowed = remainingSndrsAllowed - maximumUnreadAllowed;

        //we now iterate over all senders and include as many unread and read
        //participants as possible.
        Iterator<MailThreadInfo.Sender> senders = senders();
        while(senders.hasNext())
        {
            if(remainingSndrsAllowed == 0 )
                break;

            MailThreadInfo.Sender sender = senders.next();

            String name = firstNamesOnly? sender.getFirstName() : sender.name;

            if (name == null)
            {
                //if there's no name then use the user part of the address
                if (sender.address != null)
                {
                    int atIndex = sender.address.indexOf("@");

                    if(atIndex != -1)
                        return sender.address.substring(0, atIndex);
                    else
                        name = sender.address;
                }
                else
                    name = "unknown";
            }


            if (!sender.unread && maximumReadAllowed == 0)
                continue;

            if(remainingSndrsAllowed < maximumSndrsAllowed)
            {
                //this is not the first name we add so add a coma
                participantNames.append(", ");
            }

            remainingSndrsAllowed--;

            if(sender.unread)
            {
                participantNames.append("<b>").append(name).append("</b>");
                maximumUnreadAllowed --;
            }
            else
            {
                participantNames.append(name);
                maximumReadAllowed--;
            }
        }

        //if we don't show all the senders, then show total number of messages
        int messageCount = getMessageCount();
        if(messageCount > 1)
            participantNames.append(" (").append(messageCount).append(")");

        return participantNames.toString();
    }

    /**
     * Creates an html description (table rows) of the specified thread.
     *
     * @return an html description of <tt>thread</tt>
     */
    public String createHtmlDescription()
    {
        StringBuffer threadBuff = new StringBuffer();

        threadBuff.append("<tr bgcolor=\"#ffffff\">");

        //first get the names of the participants
        threadBuff.append("<td>");
        threadBuff.append(createParticipantNames());
        threadBuff.append("</td>");

        //start a new cell for labels, subject, snippet and thread link
        threadBuff.append("<td>");

        //labels
        threadBuff.append(createLabelList()).append("&nbsp;");

        //add the subject
        threadBuff.append("<a href=\"");
        threadBuff.append(getURL()).append("\"><b>");
        threadBuff.append(getSubject()).append("</b></a>");

        //add mail snippet
        threadBuff.append("<font color=#7777CC> - ");
        threadBuff.append("<a href=\"");
        threadBuff.append(getURL());
        threadBuff.append("\" style=\"text-decoration:none\">");
        threadBuff.append(getSnippet()).append("</a></font>");

        //end thread link
        threadBuff.append("</td>");

        //time and date
        threadBuff.append("<td nowrap>");
        threadBuff.append( getFormattedDate() );
        threadBuff.append("</td></tr>");


        //and we're done
        return threadBuff.toString();
    }

    /**
     * Creates a formatted list of labels.
     *
     * @return a <tt>String</tt> containing a formatted list of labels.
     */
    private String createLabelList()
    {
        String[] labelsArray = labels.split("\\|");
        StringBuffer labelsList = new StringBuffer();

        //get rid of the system labels that start with "^"
        for (int i = 0; i < labelsArray.length; i++)
        {
            String label = labelsArray[i];

            if(label.startsWith("^"))
                continue;

            labelsList.append("<font color=#006633>");
            labelsList.append(label);
            labelsList.append("</font>");

            if(i < labelsArray.length -1)
                labelsList.append(", ");
        }

        return labelsList.toString();
    }

}
