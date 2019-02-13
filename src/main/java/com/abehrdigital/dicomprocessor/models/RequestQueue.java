/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * @author admin
 */
@Entity
@Table(name = "request_queue")
public class
RequestQueue {
    @Id
    @Column(name = "request_queue")
    private String requestQueue;
    @Column(name = "maximum_active_threads")
    private int maximumActiveThreads;
    @Column(name = "total_active_thread_count")
    private int totalActiveThreadCount;
    @Column(name = "total_execute_count")
    private int totalExecuteCount;
    @Column(name = "total_success_count")
    private int totalSuccessCount;
    @Column(name = "total_fail_count")
    private int totalFailCount;
    @Column(name = "busy_yield_ms")
    private int busyYieldMs;
    @Column(name = "idle_yield_ms")
    private int idleYieldMs;
    @Column(name = "last_poll_date")
    private Timestamp lastPollDate;
    @Column(name = "last_thread_spawn_date")
    private Timestamp lastThreadSpawnDate;


    @Column(name = "last_thread_spawn_request_id")
    private Integer lastThreadSpawnRequestId;

    public RequestQueue() {
    }

    public RequestQueue(Builder builder) {
        this.requestQueue = builder.requestQueue;
        this.maximumActiveThreads = builder.maximumActiveThreads;
        this.totalActiveThreadCount = builder.totalActiveThreadCount;
        this.totalExecuteCount = builder.totalExecuteCount;
        this.busyYieldMs = builder.busyYieldMs;
        this.idleYieldMs = builder.idleYieldMs;
        this.lastPollDate = builder.lastPollDate;
        this.lastThreadSpawnDate = builder.lastThreadSpawnDate;
        this.lastThreadSpawnRequestId = builder.lastThreadSpawnRequestId;
    }

    public void incrementExecuteCount() {
        totalExecuteCount++;
    }

    public void setTotalActiveThreadCount(int currentActiveThreads) {
        totalActiveThreadCount = currentActiveThreads;
    }

    public void incrementSuccessCount(int count) {
        totalSuccessCount += count;
    }

    public void incrementFailCount(int failedRoutineCount) {
        totalFailCount += failedRoutineCount;
    }

    public void updateTotalExecuteCount() {
        totalExecuteCount = totalSuccessCount + totalFailCount;
    }

    public void setLastThreadSpawnDateToCurrentTimestamp() {
        this.lastThreadSpawnDate = new Timestamp(Calendar.getInstance().getTimeInMillis());
    }

    public void setLastThreadSpawnRequestId(int requestId) {
        this.lastThreadSpawnRequestId = requestId;
    }

    public static class Builder {
        //Required
        private final String requestQueue;
        private final int maximumActiveThreads;
        private final int totalExecuteCount;
        private final int busyYieldMs;
        private final int idleYieldMs;

        private int totalActiveThreadCount = 0;
        private Timestamp lastPollDate = null;
        private Timestamp lastThreadSpawnDate = null;
        private Integer lastThreadSpawnRequestId = null;

        public Builder(String requestQueue, int maximumActiveThreads,
                       int totalExecuteCount, int busyYieldMs, int idleYieldMs) {
            this.requestQueue = requestQueue;
            this.maximumActiveThreads = maximumActiveThreads;
            this.totalExecuteCount = totalExecuteCount;
            this.busyYieldMs = busyYieldMs;
            this.idleYieldMs = idleYieldMs;
        }

        public Builder totalActiveThreadCount(int val) {
            totalActiveThreadCount = val;
            return this;
        }

        public Builder lastPollDate(Timestamp val) {
            lastPollDate = val;
            return this;
        }

        public Builder lastThreadSpawnDate(Timestamp val) {
            lastThreadSpawnDate = val;
            return this;
        }

        public Builder lastThreadSpawnRequestId(int val) {
            lastThreadSpawnRequestId = val;
            return this;
        }

        public RequestQueue build() {
            return new RequestQueue(this);
        }

    }

    public String getRequestQueueName() {
        return requestQueue;
    }

    public int getMaximumActiveThreads() {
        return maximumActiveThreads;
    }

    public int getBusyYieldMs() {
        return busyYieldMs;
    }

    public int getIdleYieldMs() {
        return idleYieldMs;
    }
}
