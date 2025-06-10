package com.example.jsonaudit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class MainApp {

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String oldJson = """
                [
                  {
                    "Action": "feed",
                    "Action value": [
                      {
                        "Account": 1,
                        "Grp": 1
                      }
                    ]
                  }
                ]
                """;

        String newJson = """
                [
                  {
                    "Action": "feed",
                    "Action value": [
                      {
                        "Account": 1,
                        "Grp": 1
                      },
                      {
                        "Account": 1,
                        "Grp": 2
                      }
                    ]
                  },
                  {
                    "Action": "",
                    "Action value": [
                      {
                        "Account": 1,
                        "Grp": 2
                      }
                    ]
                  }
                ]
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