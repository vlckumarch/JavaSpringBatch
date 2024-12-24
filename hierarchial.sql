WITH RECURSIVE Hierarchy AS (
    -- Anchor: Start with the given child
    SELECT 
        Name, 
        Type, 
        ParentName
    FROM HierarchyData
    WHERE Name = 'NodeA1' -- Replace with the child you want to query

    UNION ALL

    -- Recursive: Traverse up the hierarchy
    SELECT 
        h.Name,
        h.Type,
        h.ParentName
    FROM HierarchyData h
    INNER JOIN Hierarchy hr ON h.Name = hr.ParentName
)
-- Find the specific type in the hierarchy
SELECT * 
FROM Hierarchy
WHERE Type = 'Category'; -- Replace with the desired type