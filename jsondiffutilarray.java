package com.example.jsonaudit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class JsonDiffUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

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
            if (arrayHasUuidObjects(oldNode) && arrayHasUuidObjects(newNode)) {
                changes.addAll(compareArrayByUuid(oldNode, newNode, path));
            } else {
                changes.addAll(compareArrayAsUnorderedSet(oldNode, newNode, path));
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

    private static List<Change> compareArrayByUuid(JsonNode oldArray, JsonNode newArray, String path) {
        List<Change> changes = new ArrayList<>();

        Map<String, JsonNode> oldMap = mapArrayByUuid(oldArray);
        Map<String, JsonNode> newMap = mapArrayByUuid(newArray);

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
                changes.addAll(compareJson(oldElem, newElem, arrayPath));
            }
        }

        return changes;
    }

    private static List<Change> compareArrayAsUnorderedSet(JsonNode oldArray, JsonNode newArray, String path) {
        List<Change> changes = new ArrayList<>();

        Set<String> oldSet = new HashSet<>();
        for (JsonNode elem : oldArray) {
            oldSet.add(normalizeJson(elem));
        }

        Set<String> newSet = new HashSet<>();
        for (JsonNode elem : newArray) {
            newSet.add(normalizeJson(elem));
        }

        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);
        for (String addedElem : added) {
            try {
                JsonNode addedNode = mapper.readTree(addedElem);
                changes.add(new Change(path + "[added]", ChangeType.ADDED, null, addedNode));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);
        for (String removedElem : removed) {
            try {
                JsonNode removedNode = mapper.readTree(removedElem);
                changes.add(new Change(path + "[removed]", ChangeType.REMOVED, removedNode, null));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return changes;
    }

    private static String normalizeJson(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JsonNode", e);
        }
    }
}