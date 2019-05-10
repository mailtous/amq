package com.artlongs.amq.disruptor;

public interface BatchStartAware
{
    void onBatchStart(long batchSize);
}
