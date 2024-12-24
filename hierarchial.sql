WITH Hierarchy AS (
    -- Anchor: Start from the top-level node (ParentName is NULL)
    SELECT 
        Name,
        Type,
        ParentName,
        1 AS Level
    FROM HierarchyTable
    WHERE ParentName IS NULL

    UNION ALL

    -- Recursive: Find child nodes
    SELECT 
        ht.Name,
        ht.Type,
        ht.ParentName,
        h.Level + 1 AS Level
    FROM HierarchyTable ht
    INNER JOIN Hierarchy h ON ht.ParentName = h.Name
)
SELECT *
FROM Hierarchy
WHERE Type = 'Type 2' -- Replace 'Type 2' with the desired type
ORDER BY Level, Name;