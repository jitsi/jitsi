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
package net.java.sip.communicator.impl.protocol.irc;

/**
 * Small container class that can contain a result and/or an exception.
 *
 * The container can be used for synchronizing between threads and
 * simultaneously provide a container for storing a result and/or an exception
 * that occurred within the separate thread that should be passed on to the
 * calling thread.
 *
 * @author Danny van Heumen
 *
 * @param <T> type of the value
 * @param <E> type of the exception
 */
public class Result<T, E extends Exception>
{
    /**
     * Boolean flag done.
     */
    private boolean done = false;

    /**
     * The result.
     */
    private T value = null;

    /**
     * The (possible) exception.
     */
    private E exception = null;

    /**
     * Constructor for result without initial value.
     */
    public Result()
    {
    }

    /**
     * Constructor for result with initial value.
     *
     * @param initialValue initial value
     */
    public Result(final T initialValue)
    {
        this.value = initialValue;
    }

    /**
     * Check whether it is actually done.
     *
     * @return return true when done or false otherwise
     */
    public boolean isDone()
    {
        return this.done;
    }

    /**
     * Set done without setting anything else.
     */
    public void setDone()
    {
        this.done = true;
    }

    /**
     * Set done and provide a result.
     *
     * @param value the result
     */
    public void setDone(final T value)
    {
        this.value = value;
        this.setDone();
    }

    /**
     * Set done and provide an exception.
     *
     * @param exception the exception
     */
    public void setDone(final E exception)
    {
        this.exception = exception;
        this.setDone();
    }

    /**
     * Set done and set both result and exception.
     *
     * @param value the value
     * @param exception the exception
     */
    public void setDone(final T value, final E exception)
    {
        this.value = value;
        this.exception = exception;
        this.setDone();
    }

    /**
     * Get the value.
     *
     * @return return the value
     */
    public T getValue()
    {
        return this.value;
    }

    /**
     * Get the exception.
     *
     * @return return the exception
     */
    public E getException()
    {
        return this.exception;
    }
}
