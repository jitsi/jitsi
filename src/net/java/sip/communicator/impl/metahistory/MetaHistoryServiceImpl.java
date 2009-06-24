/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.metahistory;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.history.HistoryReader;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.msghistory.event.MessageHistorySearchProgressListener;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

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

    private ArrayList<HistorySearchProgressListener>
            progressListeners = new ArrayList<HistorySearchProgressListener>();

    /**
     * Returns all the records for the descriptor after the given date.
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByStartDate(String[] services,
            Object descriptor, Date startDate)
        throws RuntimeException
    {
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;
//                mhs.addSearchProgressListener(listener);
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
//                mhs.removeSearchProgressListener(listener);
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
//                chs.addSearchProgressListener(listener)
                result.addAll(chs.findByStartDate(startDate));
//                chs.removeSearchProgressListener(listener)
            }
        }

        return result;
    }

    /**
     * Returns all the records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
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
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

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
                result.addAll(
                    ((CallHistoryService)serv).findByEndDate(endDate));
            }
        }

        return result;
    }

    /**
     * Returns all the records between the given dates
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
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
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

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
                result.addAll(
                    ((CallHistoryService)serv).findByPeriod(startDate, endDate));
            }
        }

        return result;
    }

    /**
     * Returns all the records between the given dates and having the given
     * keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
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
     * @param descriptor CallParticipant address(String),
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
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

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
                Collection<CallRecord> cs =
                    ((CallHistoryService)serv).findByPeriod(startDate, endDate);

                Iterator<CallRecord> iter = cs.iterator();
                while (iter.hasNext())
                {
                    CallRecord callRecord = iter.next();

                    if(matchAnyCallParticipant(
                            callRecord.getParticipantRecords(), keywords, caseSensitive))
                        result.add(callRecord);
                }
            }
        }

        return result;
    }

    /**
     * Returns all the records having the given keyword
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
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
     * @param descriptor CallParticipant address(String),
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
     * @param contact CallParticipant address(String),
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
     * @param descriptor CallParticipant address(String),
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
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

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
                // this will get all call records
                Collection<CallRecord> cs =
                    ((CallHistoryService)serv).findByEndDate(new Date());

                Iterator<CallRecord> iter = cs.iterator();
                while (iter.hasNext())
                {
                    CallRecord callRecord = iter.next();

                    if(matchAnyCallParticipant(
                            callRecord.getParticipantRecords(), keywords, caseSensitive))
                        result.add(callRecord);
                }
            }
        }

        return result;
    }

    /**
     * Returns the supplied number of recent records.
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
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
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

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
                result.addAll(
                    ((CallHistoryService)serv).findLast(count));
            }
        }

        return result;
    }

    /**
     * Returns the supplied number of recent records after the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
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
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

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
                Collection col = ((CallHistoryService)serv).findByStartDate(date);
                if(col.size() > count)
                {
                    List l = new LinkedList(col);
                    result.addAll(l.subList(0, count));
                }
                else
                    result.addAll(col);
            }
        }

        return result;
    }

    /**
     * Returns the supplied number of recent records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallParticipant address(String),
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
        TreeSet result = new TreeSet(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

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
                Collection col = ((CallHistoryService)serv).findByEndDate(date);
                if(col.size() > count)
                {
                    List l = new LinkedList(col);
                    result.addAll(l.subList(l.size() - count, l.size()));
                }
                else
                    result.addAll(col);
            }
        }

        return result;
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

   private boolean matchAnyCallParticipant(
       List<CallParticipantRecord> cps, String[] keywords, boolean caseSensitive)
   {
       Iterator<CallParticipantRecord> iter = cps.iterator();
       while (iter.hasNext())
       {
           CallParticipantRecord callParticipant = iter.next();
           for (int i = 0; i < keywords.length; i++)
           {
               String k = keywords[i];
               if(caseSensitive && callParticipant.getParticipantAddress().contains(k))
                    return true;
                else if(callParticipant.getParticipantAddress().toLowerCase().
                            contains(k.toLowerCase()))
                    return true;
           }
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
        implements Comparator
    {
        private long getDate(Object o)
        {
            long date = 0;
            if(o instanceof MessageDeliveredEvent)
                date = ((MessageDeliveredEvent)o).getTimestamp();
            else if(o instanceof MessageReceivedEvent)
                date = ((MessageReceivedEvent)o).getTimestamp();
            else if(o instanceof ChatRoomMessageDeliveredEvent)
                date = ((ChatRoomMessageDeliveredEvent)o).getTimestamp();
            else if(o instanceof ChatRoomMessageReceivedEvent)
                date = ((ChatRoomMessageReceivedEvent)o).getTimestamp();
            else if(o instanceof CallRecord)
                date = ((CallRecord)o).getStartTime().getTime();
            else if(o instanceof FileRecord)
                date = ((FileRecord)o).getDate();

            return date;
        }
        public int compare(Object o1, Object o2)
        {
            long date1 = getDate(o1);
            long date2 = getDate(o2);

            return (date1 < date2) ? -1 : ((date1 == date2) ? 0 : 1);
        }
    }

    private class MessageProgressWrapper
        implements MessageHistorySearchProgressListener
    {
        ArrayList<HistorySearchProgressListener> pListeners;
        int ix;
        int count;

        MessageProgressWrapper(
            ArrayList<HistorySearchProgressListener> progressListeners,
            int ix, int count)
        {
            this.pListeners = progressListeners;
            this.ix = ix;
            this.count = count;
        }

        public void progressChanged(
            net.java.sip.communicator.service.msghistory.event.ProgressEvent evt)
        {
            ProgressEvent ev = new ProgressEvent(
                MetaHistoryServiceImpl.this,
                evt.getStartDate(),
                evt.getEndDate(),
                evt.getKeywords());

//            ev.setProgress(count);
        }
    }

}
