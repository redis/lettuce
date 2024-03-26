package io.lettuce.core.protocol;

/**
 * Interface to items recording a latency. Unit of time depends on the actual implementation.
 *
 * @author Mark Paluch
 */
interface WithLatency {

    /**
     * Sets the time of sending the item.
     *
     * @param time the time of when the item was sent.
     */
    void sent(long time);

    /**
     * Sets the time of the first response.
     *
     * @param time the time of the first response.
     */
    void firstResponse(long time);

    /**
     * Set the time of completion.
     *
     * @param time the time of completion.
     */
    void completed(long time);

    /**
     * @return the time of when the item was sent.
     */
    long getSent();

    /**
     *
     * @return the time of the first response.
     */
    long getFirstResponse();

    /**
     *
     * @return the time of completion.
     */
    long getCompleted();

}
