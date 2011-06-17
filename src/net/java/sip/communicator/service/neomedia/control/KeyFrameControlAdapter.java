/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.control;

import java.util.*;

/**
 * Provides a default implementation of {@link KeyFrameControl}.
 *
 * @author Lyubomir Marinov
 */
public class KeyFrameControlAdapter
    implements KeyFrameControl
{
    /**
     * The <tt>KeyFrameRequestee</tt>s made available by this
     * <tt>KeyFrameControl</tt>.
     */
    private List<KeyFrameRequestee> keyFrameRequestees
        = new ArrayList<KeyFrameRequestee>(0);

    /**
     * The <tt>KeyFrameRequester</tt>s made available by this
     * <tt>KeyFrameControl</tt>.
     */
    private List<KeyFrameRequester> keyFrameRequesters
        = new ArrayList<KeyFrameRequester>(0);

    /**
     * An unmodifiable view of {@link #keyFrameRequestees} appropriate to be
     * returned by {@link #getKeyFrameRequestees()}.
     */
    private List<KeyFrameRequestee> unmodifiableKeyFrameRequestees;

    /**
     * An unmodifiable view of {@link #keyFrameRequesters} appropriate to be
     * returned by {@link #getKeyFrameRequesters()}.
     */
    private List<KeyFrameRequester> unmodifiableKeyFrameRequesters;

    /**
     * Implements
     * {@link KeyFrameControl#addKeyFrameRequestee(int, KeyFrameRequestee)}.
     *
     * {@inheritDoc}
     */
    public void addKeyFrameRequestee(
            int index,
            KeyFrameRequestee keyFrameRequestee)
    {
        if (keyFrameRequestee == null)
            throw new NullPointerException("keyFrameRequestee");
        synchronized (this)
        {
            if (!keyFrameRequestees.contains(keyFrameRequestee))
            {
                List<KeyFrameRequestee> newKeyFrameRequestees
                    = new ArrayList<KeyFrameRequestee>(
                            keyFrameRequestees.size() + 1);

                newKeyFrameRequestees.addAll(keyFrameRequestees);
                /*
                 * If this KeyFrameControl is to determine the index at which
                 * keyFrameRequestee is to be added according to its own
                 * internal logic, then it will prefer KeyFrameRequestee
                 * implementations from outside of neomedia rather than from its
                 * inside.
                 */
                if (-1 == index)
                {
                    if (keyFrameRequestee.getClass().getName().contains(
                            ".neomedia."))
                        index = newKeyFrameRequestees.size();
                    else
                        index = 0;
                }
                newKeyFrameRequestees.add(index, keyFrameRequestee);

                keyFrameRequestees = newKeyFrameRequestees;
                unmodifiableKeyFrameRequestees = null;
            }
        }
    }

    /**
     * Implements
     * {@link KeyFrameControl#addKeyFrameRequester(int, KeyFrameRequester)}.
     *
     * {@inheritDoc}
     */
    public void addKeyFrameRequester(
            int index,
            KeyFrameRequester keyFrameRequester)
    {
        if (keyFrameRequester == null)
            throw new NullPointerException("keyFrameRequester");
        synchronized (this)
        {
            if (!keyFrameRequesters.contains(keyFrameRequester))
            {
                List<KeyFrameRequester> newKeyFrameRequesters
                    = new ArrayList<KeyFrameRequester>(
                            keyFrameRequesters.size() + 1);

                newKeyFrameRequesters.addAll(keyFrameRequesters);
                /*
                 * If this KeyFrameControl is to determine the index at which
                 * keyFrameRequester is to be added according to its own
                 * internal logic, then it will prefer KeyFrameRequester
                 * implementations from outside of neomedia rather than from its
                 * inside.
                 */
                if (-1 == index)
                {
                    if (keyFrameRequester.getClass().getName().contains(
                            ".neomedia."))
                        index = newKeyFrameRequesters.size();
                    else
                        index = 0;
                }
                newKeyFrameRequesters.add(index, keyFrameRequester);

                keyFrameRequesters = newKeyFrameRequesters;
                unmodifiableKeyFrameRequesters = null;
            }
        }
    }

    /**
     * Implements {@link KeyFrameControl#getKeyFrameRequestees()}.
     *
     * {@inheritDoc}
     */
    public List<KeyFrameRequestee> getKeyFrameRequestees()
    {
        synchronized (this)
        {
            if (unmodifiableKeyFrameRequestees == null)
            {
                unmodifiableKeyFrameRequestees
                    = Collections.unmodifiableList(keyFrameRequestees);
            }
            return unmodifiableKeyFrameRequestees;
        }
    }

    /**
     * Implements {@link KeyFrameControl#getKeyFrameRequesters()}.
     *
     * {@inheritDoc}
     */
    public List<KeyFrameRequester> getKeyFrameRequesters()
    {
        synchronized (this)
        {
            if (unmodifiableKeyFrameRequesters == null)
            {
                unmodifiableKeyFrameRequesters
                    = Collections.unmodifiableList(keyFrameRequesters);
            }
            return unmodifiableKeyFrameRequesters;
        }
    }

    /**
     * Implements {@link KeyFrameControl#keyFrameRequest()}.
     *
     * {@inheritDoc}
     */
    public boolean keyFrameRequest()
    {
        for (KeyFrameRequestee keyFrameRequestee : getKeyFrameRequestees())
        {
            try
            {
                if (keyFrameRequestee.keyFrameRequest())
                    return true;
            }
            catch (Exception e)
            {
                /*
                 * A KeyFrameRequestee has malfunctioned, do not let it
                 * interfere with the others.
                 */
            }
        }
        return false;
    }

    /**
     * Implements
     * {@link KeyFrameControl#removeKeyFrameRequestee(KeyFrameRequestee)}.
     *
     * {@inheritDoc}
     */
    public boolean removeKeyFrameRequestee(KeyFrameRequestee keyFrameRequestee)
    {
        synchronized (this)
        {
            int index = keyFrameRequestees.indexOf(keyFrameRequestee);

            if (-1 != index)
            {
                List<KeyFrameRequestee> newKeyFrameRequestees
                    = new ArrayList<KeyFrameRequestee>(keyFrameRequestees);

                newKeyFrameRequestees.remove(index);

                keyFrameRequestees = newKeyFrameRequestees;
                unmodifiableKeyFrameRequestees = null;

                return true;
            }
            else
                return false;
        }
    }

    /**
     * Implements
     * {@link KeyFrameControl#removeKeyFrameRequester(KeyFrameRequester)}.
     *
     * {@inheritDoc}
     */
    public boolean removeKeyFrameRequester(KeyFrameRequester keyFrameRequester)
    {
        synchronized (this)
        {
            int index = keyFrameRequesters.indexOf(keyFrameRequester);

            if (-1 != index)
            {
                List<KeyFrameRequester> newKeyFrameRequesters
                    = new ArrayList<KeyFrameRequester>(keyFrameRequesters);

                newKeyFrameRequesters.remove(index);

                keyFrameRequesters = newKeyFrameRequesters;
                unmodifiableKeyFrameRequesters = null;

                return true;
            }
            else
                return false;
        }
    }
}
