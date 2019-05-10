package com.artlongs.amq.disruptor.dsl;

import com.artlongs.amq.disruptor.Sequence;
import com.artlongs.amq.disruptor.SequenceBarrier;

import java.util.concurrent.Executor;

interface ConsumerInfo
{
    Sequence[] getSequences();

    SequenceBarrier getBarrier();

    boolean isEndOfChain();

    void start(Executor executor);

    void halt();

    void markAsUsedInBarrier();

    boolean isRunning();
}
