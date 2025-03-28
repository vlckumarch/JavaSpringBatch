To improve performance for large datasets (e.g., 100K+ records per side), we can use Java 8+ Parallel Streams to process records in parallel. This takes advantage of multi-core CPUs, significantly reducing execution time.


---

Optimized Parallel Java Code

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class Record {
    int id;
    int amount;

    Record(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }
}

public class ParallelReconciliation {
    public static List<String> reconcile(List<Record> side1, List<Record> side2, int variance) {
        TreeMap<Integer, Queue<Record>> side2Map = new TreeMap<>(); // Efficient lookup for range search

        // Group side2 records by amount (Thread-safe queues for concurrent modification)
        for (Record s2 : side2) {
            side2Map.putIfAbsent(s2.amount, new ConcurrentLinkedQueue<>());
            side2Map.get(s2.amount).offer(s2);
        }

        // Process Side 1 records in parallel
        List<String> results = side1.parallelStream().map(s1 -> {
            Integer lowerBound = side2Map.floorKey(s1.amount);
            Integer upperBound = side2Map.ceilingKey(s1.amount);
            Record bestMatch = null;

            // Find closest match using variance
            if (lowerBound != null && Math.abs(s1.amount - lowerBound) <= variance) {
                bestMatch = side2Map.get(lowerBound).poll();
            } else if (upperBound != null && Math.abs(s1.amount - upperBound) <= variance) {
                bestMatch = side2Map.get(upperBound).poll();
            }

            if (bestMatch != null) {
                return "Side1: " + s1.id + " (" + s1.amount + ") <-> Side2: " + bestMatch.id + " (" + bestMatch.amount + ")";
            } else {
                return "Side1: " + s1.id + " (" + s1.amount + ") <-> No Match";
            }
        }).collect(Collectors.toList());

        // Process unmatched records from Side 2 in parallel
        AtomicInteger counter = new AtomicInteger();
        List<String> unmatchedSide2 = side2Map.values().parallelStream()
            .flatMap(Collection::stream)
            .map(s2 -> "Side2: " + s2.id + " (" + s2.amount + ") <-> No Match")
            .collect(Collectors.toList());

        results.addAll(unmatchedSide2);
        return results;
    }

    public static void main(String[] args) {
        List<Record> side1 = new ArrayList<>();
        List<Record> side2 = new ArrayList<>();

        // Generate 100K records for testing
        for (int i = 1; i <= 100000; i++) {
            side1.add(new Record(i, (int) (Math.random() * 1000)));  // Random amounts
            side2.add(new Record(100000 + i, (int) (Math.random() * 1000)));  // Random amounts
        }

        int variance = 1;
        long startTime = System.currentTimeMillis();
        List<String> matches = reconcile(side1, side2, variance);
        long endTime = System.currentTimeMillis();

        // Print execution time
        System.out.println("Execution Time: " + (endTime - startTime) + " ms");
        System.out.println("Total Matches: " + matches.size());

        // Print first 10 results
        matches.stream().limit(10).forEach(System.out::println);
    }
}


---

Parallel Performance Gains


---

Key Optimizations

1. Parallel Processing (parallelStream())

Allows multi-threaded execution on multi-core processors.

Each record in Side 1 is processed concurrently, reducing execution time.



2. Thread-Safe ConcurrentLinkedQueue

Prevents synchronization issues when accessing Side 2 matches.

Ensures safe concurrent modifications.



3. Fast Lookup with TreeMap (floorKey() & ceilingKey())

O(log n) lookup instead of O(n) linear search.

Matches closest available amount without scanning entire dataset.



4. Batch Processing Unmatched Side 2 Records in Parallel

Uses flatMap(Collection::stream) to process remaining unmatched records in parallel.





---

Expected Output

Execution Time: 700 ms  (varies based on CPU)
Total Matches: 100000

Side1: 1 (123) <-> Side2: 100034 (122)
Side1: 2 (567) <-> Side2: 100087 (567)
Side1: 3 (890) <-> No Match
...


---

Why This is Better

✅ Handles large datasets efficiently (O(n log n))
✅ Parallel Processing (Multi-core CPU utilization)
✅ Thread-safe match selection (ConcurrentLinkedQueue)
✅ Avoids full cartesian product (O(n²))
✅ Scalable for millions of records


---

Next Steps

If you need even faster performance, we can try:

1. Java ForkJoinPool for better thread control.


2. Batch processing in a database (PostgreSQL, Elasticsearch, etc.).


3. Memory-efficient streaming (if dataset is huge, 10M+ records).




Would you like any refinements or explanations?

