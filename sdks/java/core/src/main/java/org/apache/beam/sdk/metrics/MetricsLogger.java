/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.metrics;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.annotations.Experimental.Kind;
import org.slf4j.Logger;

@Experimental(Kind.METRICS)
public interface MetricsLogger extends Serializable {
  Lock REPORTING_LOCK = new ReentrantLock();
  AtomicLong LAST_REPORTED_MILLIS = new AtomicLong(System.currentTimeMillis());

  default void tryLoggingMetrics(
      String header,
      String namespace,
      String name,
      long minimumLoggingFrequencyMillis,
      boolean resetMetrics) {
    if (REPORTING_LOCK.tryLock()) {
      try {
        long currentTimeMillis = System.currentTimeMillis();
        long lastReported = LAST_REPORTED_MILLIS.get();
        if (currentTimeMillis - lastReported > minimumLoggingFrequencyMillis) {
          StringBuilder logMessage = new StringBuilder();
          logMessage.append(header);
          logMessage.append(getCumulativeString(namespace, name));
          if (resetMetrics) {
            reset();
            logMessage.append(String.format("(last reported at %s)%n", new Date(lastReported)));
          }
          getMetricLogger().info(logMessage.toString());
          LAST_REPORTED_MILLIS.set(currentTimeMillis);
        }
      } finally {
        REPORTING_LOCK.unlock();
      }
    }
  }

  Logger getMetricLogger();

  String getCumulativeString(String namespace, String name);

  void reset();
}
