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
package net.java.sip.communicator.impl.metahistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.event.ProgressEvent;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.msghistory.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The Meta History Service is wrapper around the other known
 * history services. Query them all at once, sort the result and return all
 * merged records in one collection.
 *
 * @author Damian Minkov
 */
public class MetaHistoryServiceImpl
    implements  MetaHistoryService,
                ServiceListener
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(MetaHistoryServiceImpl.class);

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * Caching of the used services
     */
    private Hashtable<String, Object> services = new Hashtable<String, Object>();

    private final List<HistorySearchProgressListener> progressListeners
        = new ArrayList<HistorySearchProgressListener>();

    /**
     * Returns all the records for the descriptor after the given date.
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @return Collection sorted result that consists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByStartDate(String[] services,
            Object descriptor, Date startDate)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        TreeSet<Object> result = new TreeSet<Object>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByStartDate((MetaContact)descriptor, startDate));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findByStartDate((ChatRoom)descriptor, startDate));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByStartDate(
                        (MetaContact)descriptor, startDate));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);
                result.addAll(chs.findByStartDate(startDate));
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(startDate, null, null);

        return result;
    }

    /**
     * Returns all the records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param endDate Date the date of the last record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByEndDate(String[] services,
            Object descriptor, Date endDate)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        TreeSet<Object> result = new TreeSet<Object>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByEndDate((MetaContact)descriptor, endDate));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findByEndDate((ChatRoom)descriptor, endDate));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByEndDate(
                        (MetaContact)descriptor, endDate));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);
                result.addAll(
                    chs.findByEndDate(endDate));
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(null, endDate, null);

        return result;
    }

    /**
     * Returns all the records between the given dates
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @param endDate Date the date of the last record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByPeriod(String[] services,
            Object descriptor, Date startDate, Date endDate)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        LinkedList<Object> result = new LinkedList<Object>();
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByPeriod(
                            (MetaContact)descriptor, startDate, endDate));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findByPeriod(
                            (ChatRoom)descriptor, startDate, endDate));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByPeriod(
                        (MetaContact)descriptor, startDate, endDate));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);
                result.addAll(
                    chs.findByPeriod(startDate, endDate));
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(startDate, endDate, null);

        Collections.sort(result, new RecordsComparator());
        return result;
    }

    /**
     * Returns all the records between the given dates and having the given
     * keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @param endDate Date the date of the last record to return
     * @param keywords array of keywords
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByPeriod(String[] services,
            Object descriptor, Date startDate, Date endDate, String[] keywords)
        throws RuntimeException
    {
        return findByPeriod(services, descriptor, startDate, endDate, keywords, false);
    }

    /**
     * Returns all the records between the given dates and having the given
     * keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @param endDate Date the date of the last record to return
     * @param keywords array of keywords
     * @param caseSensitive is keywords search case sensitive
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByPeriod(String[] services,
            Object descriptor, Date startDate, Date endDate,
            String[] keywords, boolean caseSensitive)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        TreeSet<Object> result = new TreeSet<Object>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByPeriod(
                            (MetaContact)descriptor,
                            startDate, endDate,
                            keywords, caseSensitive));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findByPeriod(
                            (ChatRoom)descriptor,
                            startDate, endDate,
                            keywords, caseSensitive));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByPeriod(
                        (MetaContact)descriptor,
                        startDate, endDate,
                        keywords, caseSensitive));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);
                Collection<CallRecord> cs =
                    chs.findByPeriod(startDate, endDate);

                Iterator<CallRecord> iter = cs.iterator();
                while (iter.hasNext())
                {
                    CallRecord callRecord = iter.next();

                    if(matchCallPeer(
                            callRecord.getPeerRecords(), keywords, caseSensitive))
                        result.add(callRecord);
                }
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(startDate, endDate, keywords);

        return result;
    }

    /**
     * Returns all the records having the given keyword
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keyword keyword
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeyword(String[] services,
            Object descriptor, String keyword)
        throws RuntimeException
    {
        return findByKeyword(services, descriptor, keyword, false);
    }

    /**
     * Returns all the records having the given keyword
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keyword keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeyword(String[] services,
            Object descriptor, String keyword, boolean caseSensitive)
        throws RuntimeException
    {
        return findByKeywords(
            services, descriptor, new String[]{keyword}, caseSensitive);
    }

    /**
     * Returns all the records having the given keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keywords keyword
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeywords(String[] services,
            Object descriptor, String[] keywords)
        throws RuntimeException
    {
        return findByKeywords(services, descriptor, keywords, false);
    }

    /**
     * Returns all the records having the given keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keywords keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeywords(String[] services,
            Object descriptor, String[] keywords, boolean caseSensitive)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        TreeSet<Object> result = new TreeSet<Object>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByKeywords(
                            (MetaContact)descriptor, keywords, caseSensitive));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findByKeywords(
                            (ChatRoom)descriptor,
                            keywords, caseSensitive));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByKeywords(
                        (MetaContact)descriptor,
                        keywords, caseSensitive));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);

                // this will get all call records
                Collection<CallRecord> cs =
                    chs.findByEndDate(new Date());

                Iterator<CallRecord> iter = cs.iterator();
                while (iter.hasNext())
                {
                    CallRecord callRecord = iter.next();

                    if(matchCallPeer(
                            callRecord.getPeerRecords(), keywords, caseSensitive))
                        result.add(callRecord);
                }
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(null, null, keywords);

        return result;
    }

    /**
     * Returns the supplied number of recent records.
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findLast(String[] services,
            Object descriptor, int count)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        TreeSet<Object> result = new TreeSet<Object>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findLast(
                            (MetaContact)descriptor,
                            count));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findLast(
                            (ChatRoom)descriptor,
                            count));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findLast(
                        (MetaContact)descriptor,
                        count));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);
                result.addAll(
                    chs.findLast(count));
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(null, null, null);

        LinkedList<Object> resultAsList = new LinkedList<Object>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Returns the supplied number of recent records after the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param date messages after date
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findFirstMessagesAfter(String[] services,
            Object descriptor, Date date, int count)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        TreeSet<Object> result = new TreeSet<Object>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findFirstMessagesAfter(
                            (MetaContact)descriptor,
                            date,
                            count));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findFirstMessagesAfter(
                            (ChatRoom)descriptor,
                            date,
                            count));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findFirstRecordsAfter(
                        (MetaContact)descriptor,
                        date,
                        count));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);

                Collection<CallRecord> col = chs.findByStartDate(date);
                if(col.size() > count)
                {
                    // before we make a sublist make sure there are sorted in the
                    // right order
                    List<CallRecord> l = new LinkedList<CallRecord>(col);
                    Collections.sort(l, new RecordsComparator());
                    result.addAll(l.subList(0, count));
                }
                else
                    result.addAll(col);
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(date, null, null);
        LinkedList<Object> resultAsList = new LinkedList<Object>(result);

        int toIndex = count;
        if(toIndex > resultAsList.size())
            toIndex = resultAsList.size();

        return resultAsList.subList(0, toIndex);
    }

    /**
     * Returns the supplied number of recent records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param date messages before date
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findLastMessagesBefore(String[] services,
            Object descriptor, Date date, int count)
        throws RuntimeException
    {
        MessageProgressWrapper listenWrapper
            = new MessageProgressWrapper(services.length);

        TreeSet<Object> result = new TreeSet<Object>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
                listenWrapper.setIx(i);
                mhs.addSearchProgressListener(listenWrapper);

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findLastMessagesBefore(
                            (MetaContact)descriptor,
                            date,
                            count));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findLastMessagesBefore(
                            (ChatRoom)descriptor,
                            date,
                            count));
                }
                mhs.removeSearchProgressListener(listenWrapper);
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findLastRecordsBefore(
                        (MetaContact)descriptor,
                        date,
                        count));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                listenWrapper.setIx(i);
                chs.addSearchProgressListener(listenWrapper);

                Collection<CallRecord> col = chs.findByEndDate(date);
                if(col.size() > count)
                {
                    List<CallRecord> l = new LinkedList<CallRecord>(col);
                    result.addAll(l.subList(l.size() - count, l.size()));
                }
                else
                    result.addAll(col);
                chs.removeSearchProgressListener(listenWrapper);
            }
        }
        listenWrapper.fireLastProgress(date, null, null);

        LinkedList<Object> resultAsList = new LinkedList<Object>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
   public void addSearchProgressListener(HistorySearchProgressListener listener)
   {
        synchronized(progressListeners)
        {
            if(!progressListeners.contains(listener))
                progressListeners.add(listener);
        }
   }

   /**
    * Removing progress listener
    *
    * @param listener HistorySearchProgressListener
    */
   public void removeSearchProgressListener(HistorySearchProgressListener listener)
   {
        synchronized(progressListeners)
        {
            progressListeners.remove(listener);
        }
   }

   private Object getService(String name)
   {
       Object serv = services.get(name);

       if(serv == null)
       {
            ServiceReference refHistory = bundleContext.getServiceReference(name);

            serv = bundleContext.getService(refHistory);
       }

       return serv;
   }

   private boolean matchAnyCallPeer(
       List<CallPeerRecord> cps, String[] keywords, boolean caseSensitive)
   {
       for (CallPeerRecord callPeer : cps)
       {
           for (String k : keywords)
           {
               if(caseSensitive && callPeer.getPeerAddress().contains(k))
                    return true;
                else if(callPeer.getPeerAddress().toLowerCase().
                            contains(k.toLowerCase()))
                    return true;
           }
       }
       return false;
   }

   private boolean matchCallPeer(
       List<CallPeerRecord> cps, String[] keywords, boolean caseSensitive)
   {
       Iterator<CallPeerRecord> iter = cps.iterator();
       while (iter.hasNext())
       {
           boolean match = false;
           CallPeerRecord callPeer = iter.next();
           for (int i = 0; i < keywords.length; i++)
           {
               String k = keywords[i];

               if(caseSensitive)
               {
                    if(callPeer.getPeerAddress().contains(k))
                    {
                        match = true;
                    }
                    else
                    {
                        match = false;
                        break;
                    }

                    continue;
               }
               else if(callPeer.getPeerAddress().toLowerCase().
                            contains(k.toLowerCase()))
               {
                   match = true;
               }
               else
               {
                   match = false;
                   break;
               }
           }

           if(match) return true;
       }

       return false;
   }

    public void serviceChanged(ServiceEvent serviceEvent)
    {
        if(serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            Object sService = bundleContext.getService(
                serviceEvent.getServiceReference());

            services.remove(sService.getClass().getName());
        }

    }

    /**
     * starts the service.
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        if (logger.isDebugEnabled())
            logger.debug("Starting the call history implementation.");
        this.bundleContext = bc;

        services.clear();

        // start listening for newly register or removed services
        bc.addServiceListener(this);
    }

    /**
     * stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(this);
        services.clear();
    }

    /**
     * Used to compare various records
     * to be ordered in TreeSet according their timestamp.
     */
    private static class RecordsComparator
        implements Comparator<Object>
    {
        private Date getDate(Object o)
        {
            Date date = new Date(0);
            if(o instanceof MessageDeliveredEvent)
                date = ((MessageDeliveredEvent)o).getTimestamp();
            else if(o instanceof MessageReceivedEvent)
                date = ((MessageReceivedEvent)o).getTimestamp();
            else if(o instanceof ChatRoomMessageDeliveredEvent)
                date = ((ChatRoomMessageDeliveredEvent)o).getTimestamp();
            else if(o instanceof ChatRoomMessageReceivedEvent)
                date = ((ChatRoomMessageReceivedEvent)o).getTimestamp();
            else if(o instanceof CallRecord)
                date = ((CallRecord)o).getStartTime();
            else if(o instanceof FileRecord)
                date = ((FileRecord)o).getDate();

            return date;
        }
        public int compare(Object o1, Object o2)
        {
            Date date1 = getDate(o1);
            Date date2 = getDate(o2);

            return date1.compareTo(date2);
        }
    }

    private class MessageProgressWrapper
        implements MessageHistorySearchProgressListener,
        CallHistorySearchProgressListener
    {
        private final int count;

        private int ix;

        public MessageProgressWrapper(int count)
        {
            this.count = count;
        }

        public void setIx(int ix)
        {
            this.ix = ix;
        }

        private void fireProgress(int origProgress, int maxVal,
            Date startDate, Date endDate, String[] keywords)
        {
            ProgressEvent ev = new ProgressEvent(
                MetaHistoryServiceImpl.this,
                startDate,
                endDate,
                keywords);

            double part1 = origProgress/
                ((double)maxVal*count);
            double convProgress =
                part1*HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE +
                ix*HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE/count;

            ev.setProgress((int)convProgress);

            fireEvent(ev);
        }

        private void fireEvent(ProgressEvent ev)
        {
            Iterable<HistorySearchProgressListener> listeners;
            synchronized(progressListeners)
            {
                listeners
                    = new ArrayList<HistorySearchProgressListener>(
                            progressListeners);
            }
            for (HistorySearchProgressListener listener : listeners)
                listener.progressChanged(ev);
        }

        public void fireLastProgress(
            Date startDate, Date endDate, String[] keywords)
        {
            ProgressEvent ev = new ProgressEvent(
                MetaHistoryServiceImpl.this,
                startDate,
                endDate,
                keywords);
            ev.setProgress(HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE);

            fireEvent(ev);
        }

        public void progressChanged(
            net.java.sip.communicator.service.msghistory.event.ProgressEvent evt)
        {
            fireProgress(
                evt.getProgress(),
                MessageHistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE,
                evt.getStartDate(),
                evt.getEndDate(),
                evt.getKeywords());
        }

        public void progressChanged(net.java.sip.communicator.service.callhistory.event.ProgressEvent evt)
        {
            fireProgress(
                evt.getProgress(),
                CallHistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE,
                evt.getStartDate(),
                evt.getEndDate(),
                null);
        }
    }
}
