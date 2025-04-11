public static List<String> reconcile(List<Record> side1, List<Record> side2, double variance) {
    NavigableMap<Double, Queue<Record>> side2Map = new TreeMap<>();

    for (Record s2 : side2) {
        side2Map.computeIfAbsent(s2.amount, k -> new ConcurrentLinkedQueue<>()).offer(s2);
    }

    List<String> results = side1.parallelStream().map(s1 -> {
        Record bestMatch = null;
        double minDiff = Double.MAX_VALUE;
        Double bestKey = null;

        // Use subMap for efficient range lookup
        NavigableMap<Double, Queue<Record>> candidates = side2Map.subMap(s1.amount - variance, true, s1.amount + variance, true);

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

        if (bestMatch != null) {
            return "Side1: " + s1.id + " (" + s1.amount + ") <-> Side2: " + bestMatch.id + " (" + bestMatch.amount + ")";
        } else {
            return "Side1: " + s1.id + " (" + s1.amount + ") <-> No Match";
        }
    }).collect(Collectors.toList());

    List<String> unmatchedSide2 = side2Map.values().parallelStream()
        .flatMap(Collection::stream)
        .map(s2 -> "Side2: " + s2.id + " (" + s2.amount + ") <-> No Match")
        .collect(Collectors.toList());

    results.addAll(unmatchedSide2);
    return results;
}