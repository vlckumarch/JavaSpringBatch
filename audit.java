Perfect — this is a very common real-world scenario:

👉 You have JSON arrays of objects,
👉 Each object has an id (or uuid) field,
👉 You want to compare arrays by uuid, not just by index,
👉 Detect ADDED / REMOVED / UPDATED per object.


---

Enhanced Design

✅ The Change class will include a ChangeType:

enum ChangeType {
    ADDED, REMOVED, UPDATED
}

✅ For arrays of objects with uuid, we will:

Build maps of uuid → object for old and new arrays,

Do set-based comparison:

If uuid is in new only → ADDED,

If uuid is in old only → REMOVED,

If uuid in both → compare recursively (UPDATED if needed).




---

Full Enhanced Example


---

1️⃣ ChangeType.java

package com.example.jsonaudit;

public enum ChangeType {
    ADDED,
    REMOVED,
    UPDATED
}


---

2️⃣ Change.java

package com.example.jsonaudit;

import com.fasterxml.jackson.databind.JsonNode;

public class Change {
    private String path;
    private ChangeType changeType;
    private JsonNode oldValue;
    private JsonNode newValue;

    public Change(String path, ChangeType changeType, JsonNode oldValue, JsonNode newValue) {
        this.path = path;
        this.changeType = changeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getPath() {
        return path;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public JsonNode getOldValue() {
        return oldValue;
    }

    public JsonNode getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return "Change{" +
                "path='" + path + '\'' +
                ", changeType=" + changeType +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}


---

3️⃣ JsonDiffUtil.java

package com.example.jsonaudit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class JsonDiffUtil {

    public static List<Change> compareJson(JsonNode oldNode, JsonNode newNode, String path) {
        List<Change> changes = new ArrayList<>();

        if (oldNode == null && newNode == null) {
            return changes;
        }

        if (oldNode == null) {
            changes.add(new Change(path, ChangeType.ADDED, null, newNode));
            return changes;
        } else if (newNode == null) {
            changes.add(new Change(path, ChangeType.REMOVED, oldNode, null));
            return changes;
        }

        if (oldNode.isObject() && newNode.isObject()) {
            Set<String> fieldNames = new HashSet<>();
            oldNode.fieldNames().forEachRemaining(fieldNames::add);
            newNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String field : fieldNames) {
                JsonNode oldValue = oldNode.get(field);
                JsonNode newValue = newNode.get(field);
                String currentPath = path.isEmpty() ? field : path + "." + field;
                changes.addAll(compareJson(oldValue, newValue, currentPath));
            }
        }

        else if (oldNode.isArray() && newNode.isArray()) {
            // Check if array contains objects with uuid/id
            if (arrayHasUuidObjects(oldNode) && arrayHasUuidObjects(newNode)) {
                // Compare by uuid
                Map<String, JsonNode> oldMap = mapArrayByUuid(oldNode);
                Map<String, JsonNode> newMap = mapArrayByUuid(newNode);

                Set<String> allUuids = new HashSet<>();
                allUuids.addAll(oldMap.keySet());
                allUuids.addAll(newMap.keySet());

                for (String uuid : allUuids) {
                    JsonNode oldElem = oldMap.get(uuid);
                    JsonNode newElem = newMap.get(uuid);
                    String arrayPath = path + "[uuid=" + uuid + "]";

                    if (oldElem == null) {
                        changes.add(new Change(arrayPath, ChangeType.ADDED, null, newElem));
                    } else if (newElem == null) {
                        changes.add(new Change(arrayPath, ChangeType.REMOVED, oldElem, null));
                    } else {
                        // Compare recursively
                        changes.addAll(compareJson(oldElem, newElem, arrayPath));
                    }
                }
            } else {
                // Compare by index (fallback)
                int maxSize = Math.max(oldNode.size(), newNode.size());
                for (int i = 0; i < maxSize; i++) {
                    JsonNode oldElem = i < oldNode.size() ? oldNode.get(i) : null;
                    JsonNode newElem = i < newNode.size() ? newNode.get(i) : null;
                    String arrayPath = path + "[" + i + "]";
                    changes.addAll(compareJson(oldElem, newElem, arrayPath));
                }
            }
        }

        else if (!oldNode.equals(newNode)) {
            changes.add(new Change(path, ChangeType.UPDATED, oldNode, newNode));
        }

        return changes;
    }

    private static boolean arrayHasUuidObjects(JsonNode arrayNode) {
        if (!arrayNode.isArray() || arrayNode.size() == 0) {
            return false;
        }
        JsonNode first = arrayNode.get(0);
        return first.isObject() && (first.has("uuid") || first.has("id"));
    }

    private static Map<String, JsonNode> mapArrayByUuid(JsonNode arrayNode) {
        Map<String, JsonNode> map = new HashMap<>();
        for (JsonNode elem : arrayNode) {
            if (elem.isObject()) {
                String uuid = elem.has("uuid") ? elem.get("uuid").asText() :
                              elem.has("id") ? elem.get("id").asText() : null;
                if (uuid != null) {
                    map.put(uuid, elem);
                }
            }
        }
        return map;
    }
}


---

4️⃣ MainApp.java (runner class)

package com.example.jsonaudit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class MainApp {

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String oldJson = """
                {
                  "customer": {
                    "name": "John Doe",
                    "address": {
                      "city": "Bangalore",
                      "zip": "560001"
                    }
                  },
                  "order": {
                    "items": [
                      { "uuid": "1", "name": "Item1", "price": 100 },
                      { "uuid": "2", "name": "Item2", "price": 200 }
                    ],
                    "status": "Processing"
                  }
                }
                """;

        String newJson = """
                {
                  "customer": {
                    "name": "John Doe",
                    "address": {
                      "city": "Chennai",
                      "zip": "560001",
                      "country": "India"
                    }
                  },
                  "order": {
                    "items": [
                      { "uuid": "1", "name": "Item1", "price": 100 },
                      { "uuid": "2", "name": "Item2", "price": 220 },
                      { "uuid": "3", "name": "Item3", "price": 300 }
                    ],
                    "status": "Shipped"
                  }
                }
                """;

        JsonNode oldNode = mapper.readTree(oldJson);
        JsonNode newNode = mapper.readTree(newJson);

        List<Change> changes = JsonDiffUtil.compareJson(oldNode, newNode, "");

        System.out.println("Detected Changes:");
        changes.forEach(change -> {
            System.out.printf("Path: %-50s | Type: %-8s | Old: %-20s | New: %-20s%n",
                    change.getPath(),
                    change.getChangeType(),
                    change.getOldValue(),
                    change.getNewValue());
        });
    }
}


---

Sample Output

Detected Changes:
Path: customer.address.city                        | Type: UPDATED  | Old: "Bangalore"         | New: "Chennai"           
Path: customer.address.country                     | Type: ADDED    | Old: null                | New: "India"             
Path: order.items[uuid=2].price                    | Type: UPDATED  | Old: 200                 | New: 220                 
Path: order.items[uuid=3]                          | Type: ADDED    | Old: null                | New: {"uuid":"3","name":"Item3","price":300}
Path: order.status                                 | Type: UPDATED  | Old: "Processing"        | New: "Shipped"


---

Summary

✅ Now it detects ADDED, REMOVED, UPDATED
✅ Arrays of objects are compared by uuid automatically
✅ Works recursively for nested objects
✅ Works with both object and array changes


---

Next Steps

If you want, I can also give you:

✅ Version with configurable id field name (you may have uuid or id or something else)
✅ JSON diff formatted for easy DB storage
✅ Spring Boot service wrapper to inject this logic as a @Component


---

Would you like me to provide that too? Just say:
"YES, give the ready-to-use Spring Boot version" — I’ll give you the full component you can drop into your app. 🚀

