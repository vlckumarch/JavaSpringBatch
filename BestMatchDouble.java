import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

class Record {
    int id;
    double amount; // Changed from int to double

    Record(int id, double amount) {
        this.id = id;
        this.amount = amount;
    }
}

public class ParallelReconciliation {
    public static List<String> reconcile(List<Record> side1, List<Record> side2, double variance) {
        NavigableMap<Double, Queue<Record>> side2Map = new TreeMap<>();

        // Group side2 records by amount using ConcurrentLinkedQueue
        for (Record s2 : side2) {
            side2Map.computeIfAbsent(s2.amount, k -> new ConcurrentLinkedQueue<>()).offer(s2);
        }

        List<String> results = side1.parallelStream().map(s1 -> {
            // Look for a match in the range [amount - variance, amount + variance]
            Record bestMatch = null;

            NavigableMap<Double, Queue<Record>> subMap = side2Map.subMap(s1.amount - variance, true, s1.amount + variance, true);

            for (Map.Entry<Double, Queue<Record>> entry : subMap.entrySet()) {
                Queue<Record> candidates = entry.getValue();
                Record match = candidates.poll();
                if (match != null) {
                    bestMatch = match;
                    break; // First closest match found
                }
            }

            if (bestMatch != null) {
                return "Side1: " + s1.id + " (" + s1.amount + ") <-> Side2: " + bestMatch.id + " (" + bestMatch.amount + ")";
            } else {
                return "Side1: " + s1.id + " (" + s1.amount + ") <-> No Match";
            }
        }).collect(Collectors.toList());

        // Process unmatched Side2 records
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

        for (int i = 1; i <= 100000; i++) {
            side1.add(new Record(i, Math.round(Math.random() * 1000 * 100.0) / 100.0));  // Random amounts with 2 decimals
            side2.add(new Record(100000 + i, Math.round(Math.random() * 1000 * 100.0) / 100.0));
        }

        double variance = 0.01; // Accept difference of 1 cent
        long startTime = System.currentTimeMillis();
        List<String> matches = reconcile(side1, side2, variance);
        long endTime = System.currentTimeMillis();

        System.out.println("Execution Time: " + (endTime - startTime) + " ms");
        System.out.println("Total Matches: " + matches.size());

        matches.stream().limit(10).forEach(System.out::println);
    }
}