package com.abehrdigital.dicomprocessor;

public interface RequestThreadListener {
    void deQueue(int requestId, int successfulRoutineCount, int failedRoutineCount);
}
