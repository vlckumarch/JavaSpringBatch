Optimized Approach

Instead of using a cartesian product (O(n²)), we can optimize matching by:

1. Using a HashMap to group amounts from Side 2 for quick lookup (O(1) search).


2. Processing Side 1 sequentially and fetching the best match efficiently.



This approach reduces the time complexity to O(n log n) or even O(n) if pre-sorted.


---

Optimized Java Code (Using HashMap & TreeMap)

import java.util.*;

class Record {
    int id;
    int amount;

    Record(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }
}

public class OptimizedReconciliation {
    public static List<String> reconcile(List<Record> side1, List<Record> side2, int variance) {
        List<String> result = new ArrayList<>();
        TreeMap<Integer, Queue<Record>> side2Map = new TreeMap<>(); // Efficient lookup for range search

        // Group side2 records by amount
        for (Record s2 : side2) {
            side2Map.putIfAbsent(s2.amount, new LinkedList<>());
            side2Map.get(s2.amount).offer(s2);
        }

        // Process Side 1
        for (Record s1 : side1) {
            Integer lowerBound = side2Map.floorKey(s1.amount); // Find closest lower match
            Integer upperBound = side2Map.ceilingKey(s1.amount); // Find closest upper match

            Record bestMatch = null;
            if (lowerBound != null && Math.abs(s1.amount - lowerBound) <= variance && !side2Map.get(lowerBound).isEmpty()) {
                bestMatch = side2Map.get(lowerBound).poll();
            } else if (upperBound != null && Math.abs(s1.amount - upperBound) <= variance && !side2Map.get(upperBound).isEmpty()) {
                bestMatch = side2Map.get(upperBound).poll();
            }

            if (bestMatch != null) {
                result.add("Side1: " + s1.id + " (" + s1.amount + ") <-> Side2: " + bestMatch.id + " (" + bestMatch.amount + ")");
            } else {
                result.add("Side1: " + s1.id + " (" + s1.amount + ") <-> No Match");
            }
        }

        // Add remaining unmatched records from Side 2
        for (Queue<Record> queue : side2Map.values()) {
            while (!queue.isEmpty()) {
                Record s2 = queue.poll();
                result.add("Side2: " + s2.id + " (" + s2.amount + ") <-> No Match");
            }
        }

        return result;
    }

    public static void main(String[] args) {
        List<Record> side1 = Arrays.asList(
            new Record(1, 4),
            new Record(2, 6),
            new Record(3, 5)
        );

        List<Record> side2 = Arrays.asList(
            new Record(9, 3),
            new Record(8, 5),
            new Record(10, 4)
        );

        int variance = 1;
        List<String> matches = reconcile(side1, side2, variance);
        
        for (String match : matches) {
            System.out.println(match);
        }
    }
}


---

Key Optimizations

1. Use a TreeMap<Integer, Queue<Record>> for Side 2

Enables fast lookup for closest matching amounts (floorKey() and ceilingKey() take O(log n))

Uses queues to handle multiple records with the same amount efficiently.



2. Matching in O(n log n) Time

Instead of iterating over all pairs (O(n²)), we do:

O(n log n) for inserting into TreeMap

O(n log n) for matching records efficiently.




3. Handles Duplicate Amounts on Both Sides

Uses FIFO queueing (first-come-first-served) to distribute multiple matches fairly.





---

Example Output

Side1: 1 (4) <-> Side2: 10 (4)
Side1: 2 (6) <-> Side2: 8 (5)
Side1: 3 (5) <-> No Match
Side2: 9 (3) <-> No Match


---

Why This is Better

✔ Handles large datasets efficiently (O(n log n))
✔ Avoids full cartesian product (O(n²))
✔ Works well with multiple same amounts
✔ Balances performance and matching fairness

Would you like additional customization, such as prioritizing closest values instead of first come first served?

