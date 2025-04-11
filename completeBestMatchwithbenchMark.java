import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

// Record class
class Record {
    int id;
    double amount;

    Record(int id, double amount) {
        this.id = id;
        this.amount = amount;
    }
}

// Functional interface for reconcilers
@FunctionalInterface
interface Reconciler {
    List<String> reconcile(List<Record> side1, List<Record> side2, double variance);
}

public class ParallelReconciliationBenchmark {

    // Reconciler 1: Linear Scan
    public static Reconciler linearScanReconciler = (side1, side2, variance) -> {
        NavigableMap<Double, Queue<Record>> side2Map = new TreeMap<>();
        for (Record s2 : side2) {
            side2Map.computeIfAbsent(s2.amount, k -> new ConcurrentLinkedQueue<>()).offer(s2);
        }

        List<String> results = side1.parallelStream().map(s1 -> {
            Record bestMatch = null;
            double minDiff = Double.MAX_VALUE;
            Double bestKey = null;

            for (Map.Entry<Double, Queue<Record>> entry : side2Map.entrySet()) {
                if (Math.abs(s1.amount - entry.getKey()) <= variance && !entry.getValue().isEmpty()) {
                    double diff = Math.abs(s1.amount - entry.getKey());
                    if (diff < minDiff) {
                        minDiff = diff;
                        bestKey = entry.getKey();
                    }
                }
            }

            if (bestKey != null) {
                bestMatch = side2Map.get(bestKey).poll();
            }

            return bestMatch != null
                    ? "Side1: " + s1.id + " (" + s1.amount + ") <-> Side2: " + bestMatch.id + " (" + bestMatch.amount + ")"
                    : "Side1: " + s1.id + " (" + s1.amount + ") <-> No Match";
        }).collect(Collectors.toList());

        List<String> unmatched = side2Map.values().parallelStream()
                .flatMap(Collection::stream)
                .map(s2 -> "Side2: " + s2.id + " (" + s2.amount + ") <-> No Match")
                .collect(Collectors.toList());

        results.addAll(unmatched);
        return results;
    };

    // Reconciler 2: Optimized SubMap
    public static Reconciler optimizedSubMapReconciler = (side1, side2, variance) -> {
        NavigableMap<Double, Queue<Record>> side2Map = new TreeMap<>();
        for (Record s2 : side2) {
            side2Map.computeIfAbsent(s2.amount, k -> new ConcurrentLinkedQueue<>()).offer(s2);
        }

        List<String> results = side1.parallelStream().map(s1 -> {
            Record bestMatch = null;
            double minDiff = Double.MAX_VALUE;
            Double bestKey = null;

            NavigableMap<Double, Queue<Record>> candidates =
                    side2Map.subMap(s1.amount - variance, true, s1.amount + variance, true);

            for (Map.Entry<Double, Queue<Record>> entry : candidates.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    double diff = Math.abs(s1.amount - entry.getKey());
                    if (diff < minDiff) {
                        minDiff = diff;
                        bestKey = entry.getKey();
                    }
                }
            }

            if (bestKey != null) {
                bestMatch = side2Map.get(bestKey).poll();
            }

            return bestMatch != null
                    ? "Side1: " + s1.id + " (" + s1.amount + ") <-> Side2: " + bestMatch.id + " (" + bestMatch.amount + ")"
                    : "Side1: " + s1.id + " (" + s1.amount + ") <-> No Match";
        }).collect(Collectors.toList());

        List<String> unmatched = side2Map.values().parallelStream()
                .flatMap(Collection::stream)
                .map(s2 -> "Side2: " + s2.id + " (" + s2.amount + ") <-> No Match")
                .collect(Collectors.toList());

        results.addAll(unmatched);
        return results;
    };

    // Reconciler 3: Sorted List Decimal-safe
    public static Reconciler sortedListDecimalSafeReconciler = (side1, side2, variance) -> {
        List<Record> sortedSide2 = new ArrayList<>(side2);
        sortedSide2.sort(Comparator.comparingDouble(r -> r.amount));

        Map<Record, Boolean> used = new ConcurrentHashMap<>();

        List<String> results = side1.parallelStream().map(s1 -> {
            Record bestMatch = null;
            double minDiff = Double.MAX_VALUE;

            for (Record s2 : sortedSide2) {
                if (used.containsKey(s2)) continue;

                double diff = Math.abs(s1.amount - s2.amount);
                if (diff <= variance && diff < minDiff) {
                    bestMatch = s2;
                    minDiff = diff;
                } else if (s2.amount - s1.amount > variance) {
                    break;
                }
            }

            if (bestMatch != null) {
                used.put(bestMatch, true);
                return "Side1: " + s1.id + " (" + s1.amount + ") <-> Side2: " + bestMatch.id + " (" + bestMatch.amount + ")";
            } else {
                return "Side1: " + s1.id + " (" + s1.amount + ") <-> No Match";
            }
        }).collect(Collectors.toList());

        sortedSide2.stream()
                .filter(s2 -> !used.containsKey(s2))
                .map(s2 -> "Side2: " + s2.id + " (" + s2.amount + ") <-> No Match")
                .forEach(results::add);

        return results;
    };

    // Benchmark runner
    public static void benchmark(Reconciler reconciler, String label, List<Record> side1, List<Record> side2, double variance) {
        long start = System.currentTimeMillis();
        List<String> matches = reconciler.reconcile(side1, side2, variance);
        long end = System.currentTimeMillis();

        System.out.println("[" + label + "] Execution Time: " + (end - start) + " ms");
        System.out.println("[" + label + "] Total Matches: " + matches.size());
        matches.stream().limit(5).forEach(System.out::println);
        System.out.println();
    }

    // Main method
    public static void main(String[] args) {
        List<Record> side1 = new ArrayList<>();
        List<Record> side2 = new ArrayList<>();

        for (int i = 1; i <= 100_000; i++) {
            side1.add(new Record(i, Math.round(Math.random() * 1000 * 10.0) / 10.0));
            side2.add(new Record(100000 + i, Math.round(Math.random() * 1000 * 10.0) / 10.0));
        }

        double variance = 1.5;

        benchmark(linearScanReconciler, "Linear Scan", side1, side2, variance);
        benchmark(optimizedSubMapReconciler, "Optimized SubMap", side1, side2, variance);
        benchmark(sortedListDecimalSafeReconciler, "Sorted List Decimal-Safe", side1, side2, variance);
    }
}