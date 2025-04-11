import java.util.*;
import java.util.stream.Collectors;

class Record {
    int id;
    double amount;

    Record(int id, double amount) {
        this.id = id;
        this.amount = amount;
    }

    public String toString() {
        return id + " (" + amount + ")";
    }
}

public class ReconciliationFixed {

    public static List<String> reconcile(List<Record> side1, List<Record> side2, double variance) {
        List<String> results = new ArrayList<>();
        Set<Integer> matchedSide2 = new HashSet<>();

        for (Record s1 : side1) {
            Record bestMatch = null;
            double minDiff = Double.MAX_VALUE;

            for (Record s2 : side2) {
                if (matchedSide2.contains(s2.id)) continue;

                double diff = Math.abs(s1.amount - s2.amount);
                if (diff <= variance && diff < minDiff) {
                    bestMatch = s2;
                    minDiff = diff;
                }
            }

            if (bestMatch != null) {
                results.add("Side1: " + s1 + " <-> Side2: " + bestMatch);
                matchedSide2.add(bestMatch.id);
            } else {
                results.add("Side1: " + s1 + " <-> No Match");
            }
        }

        for (Record s2 : side2) {
            if (!matchedSide2.contains(s2.id)) {
                results.add("Side2: " + s2 + " <-> No Match");
            }
        }

        return results;
    }

    public static void main(String[] args) {
        List<Record> side1 = Arrays.asList(
                new Record(1, 4),
                new Record(2, 4),
                new Record(3, 4),
                new Record(4, 3)
        );

        List<Record> side2 = Arrays.asList(
                new Record(11, 4),
                new Record(12, 4.5),
                new Record(13, 3),
                new Record(14, 3.5)
        );

        double variance = 1.5;
        List<String> matches = reconcile(side1, side2, variance);

        for (String match : matches) {
            System.out.println(match);
        }
    }
}