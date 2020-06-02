package com.artfii.amq.disruptor;

public interface BatchStartAware
{
    void onBatchStart(long batchSize);
}
