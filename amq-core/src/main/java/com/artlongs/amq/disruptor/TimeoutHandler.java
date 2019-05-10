package com.artlongs.amq.disruptor;

public interface TimeoutHandler
{
    void onTimeout(long sequence) throws Exception;
}
