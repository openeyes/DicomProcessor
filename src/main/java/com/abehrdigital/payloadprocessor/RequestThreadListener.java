package com.abehrdigital.payloadprocessor;

public interface RequestThreadListener {
    void deQueue(int requestId, int successfulRoutineCount, int failedRoutineCount);
}
