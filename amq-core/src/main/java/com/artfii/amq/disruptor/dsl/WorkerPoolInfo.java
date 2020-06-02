package com.artfii.amq.disruptor.dsl;

import com.artfii.amq.disruptor.*;

import java.util.concurrent.Executor;

class WorkerPoolInfo<T> implements ConsumerInfo
{
    private final WorkerPool<T> workerPool;
    private final SequenceBarrier sequenceBarrier;
    private boolean endOfChain = true;

    WorkerPoolInfo(final WorkerPool<T> workerPool, final SequenceBarrier sequenceBarrier)
    {
        this.workerPool = workerPool;
        this.sequenceBarrier = sequenceBarrier;
    }

    @Override
    public Sequence[] getSequences()
    {
        return workerPool.getWorkerSequences();
    }

    @Override
    public SequenceBarrier getBarrier()
    {
        return sequenceBarrier;
    }

    @Override
    public boolean isEndOfChain()
    {
        return endOfChain;
    }

    @Override
    public void start(Executor executor)
    {
        workerPool.start(executor);
    }

    @Override
    public void halt()
    {
        workerPool.halt();
    }

    @Override
    public void markAsUsedInBarrier()
    {
        endOfChain = false;
    }

    @Override
    public boolean isRunning()
    {
        return workerPool.isRunning();
    }
}
