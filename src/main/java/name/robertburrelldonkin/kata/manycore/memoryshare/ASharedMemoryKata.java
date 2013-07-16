/*
 * Copyright 2013 Robert Burrell Donkin http://robertburrelldonkin.name
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.robertburrelldonkin.kata.manycore.memoryshare;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h3>A Code Kata about Shared Memory</h3>
 * <ul>
 * <li><code>Counter</code> is a toy summarizer, making additions to a
 * <code>long</code> total.</li>
 * <li><code>CounterClient</code> exercises the <code>Counter</code> API.</li>
 * <li><code>Harness</code> prepares and runs <code>CounterClient</code>
 * instances concurrently.</li>
 * </ul>
 * <p>
 * The main method is simple and tucked away at the bottom. Take a look at the
 * code and then run. Explain what you observe.
 * </p>
 * <p>
 * The objective of this Code Kata is to broaden and deepen knowledge of
 * concurrency techniques. There are many ways to fail, some to succeed and
 * several quite different good solutions.
 * </p>
 */
public class ASharedMemoryKata {

    /** A toy summarizer, adding to a running <code>long</code> total */
    static class Counter {

        private long count = 0;

        Counter() {
        }

        void add(final int addition) {
            count = count + addition;
        }

        long getCount() {
            return count;
        }
    }

    /** Exercises the <code>Counter</code> API */
    static class CounterClient {
        final int addThisValue;
        final Counter sharedCounter;
        final int numberOfRepeatsInOneRun;

        CounterClient(final int addThisValue, final Counter sharedCounter,
                final int numberOfRepeatsInOneRun) {
            super();
            this.addThisValue = addThisValue;
            this.sharedCounter = sharedCounter;
            this.numberOfRepeatsInOneRun = numberOfRepeatsInOneRun;
        }

        void run() throws Exception {
            for (int i = 0; i < numberOfRepeatsInOneRun; i++) {
                sharedCounter.add(addThisValue);
            }
        }

        int totalAddedEachRun() {
            return numberOfRepeatsInOneRun * addThisValue;
        }
    }

    /**
     * Prepares and runs <code>CounterClient</code> instances
     * concurrently
     */
    static class Harness {

        private final int addThisValue;
        private final int numberOfRepeatsInOneRun;

        Harness(final int addThisValue, final int numberOfRepeatsInOneRun) {
            super();
            this.addThisValue = addThisValue;
            this.numberOfRepeatsInOneRun = numberOfRepeatsInOneRun;
        }

        void startThreadsNumbering(final int numberOfThreads) throws Exception {

            final long expectedCount = numberOfThreads
                    * numberOfRepeatsInOneRun * addThisValue;

            final Counter counter = new Counter();
            final CountDownLatch holdUntilAllThreadsAreReady = new CountDownLatch(
                    numberOfThreads);
            final CountDownLatch waitUntilAllThreadsStop = new CountDownLatch(
                    numberOfThreads);
            final AtomicInteger failureCount = new AtomicInteger(0);

            System.out.println("Preparing threads...");
            for (int i = 0; i < numberOfThreads; i++) {
                final int threadNumber = i;
                final String threadName = "Thread " + threadNumber;
                new Thread() {

                    @Override
                    public void run() {
                        System.out.println(threadName + " started running.");
                        try {
                            CounterClient client = new CounterClient(
                                    addThisValue, counter,
                                    numberOfRepeatsInOneRun);
                            System.out.println("Holding " + threadName);
                            holdUntilAllThreadsAreReady.await();
                            System.out.println("Running client using "
                                    + threadName);
                            client.run();
                        }
                        catch (Throwable t) {
                            final int numberOfFailuresSoFar = failureCount
                                    .incrementAndGet();
                            System.out.println("Failure number "
                                    + numberOfFailuresSoFar + " ("
                                    + t.getClass().getName() + ":"
                                    + t.getMessage() + ")");

                        }
                        finally {
                            waitUntilAllThreadsStop.countDown();
                            System.out.println(threadName + " finished, "
                                    + waitUntilAllThreadsStop.getCount()
                                    + " remaining.");
                        }
                    }
                }.start();

                holdUntilAllThreadsAreReady.countDown();
            }

            waitUntilAllThreadsStop.await();
            if (failureCount.get() > 0) {
                System.out
                        .println("********************************************");
                System.out.println("FAILURES: " + failureCount.get());
                System.out
                        .println("********************************************");
            } else {
                final long actualCount = counter.getCount();
                // Give System.out a little time to finish printing
                Thread.sleep(100);
                if (expectedCount == actualCount) {
                    System.out.println("SUCCESS");
                } else {
                    System.out.println("OOPS expected count to be "
                            + expectedCount + " but was " + actualCount);
                }
            }
        }
    }

    /** Simply runs the harness. Vary the parameters and observe the results. */
    public static void main(String[] args) throws Exception {
        new Harness(7, 1000).startThreadsNumbering(1000);
    }
}
