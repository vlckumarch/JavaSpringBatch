@Component
public class MapEntityItemWriter implements ItemWriter<Map<String, LinkedList<EntityClass>>> {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void write(List<? extends Map<String, LinkedList<EntityClass>>> items) throws Exception {
        for (Map<String, LinkedList<EntityClass>> item : items) {
            // Iterate over the map and update the database
            for (Map.Entry<String, LinkedList<EntityClass>> entry : item.entrySet()) {
                String key = entry.getKey();
                LinkedList<EntityClass> entityList = entry.getValue();

                // Iterate through each EntityClass object in the LinkedList
                for (EntityClass entity : entityList) {
                    String sql = "UPDATE your_table SET column1 = :column1, column2 = :column2 WHERE id = :id";

                    // Extract properties from the EntityClass and map to the SQL parameters
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", entity.getId());
                    params.put("column1", entity.getColumn1());
                    params.put("column2", entity.getColumn2());

                    jdbcTemplate.update(sql, params);
                }
            }
        }
    }
}
public class EntityClass {
    private Long id;
    private String column1;
    private String column2;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getColumn1() { return column1; }
    public void setColumn1(String column1) { this.column1 = column1; }

    public String getColumn2() { return column2; }
    public void setColumn2(String column2) { this.column2 = column2; }
}

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private MapEntityItemWriter mapEntityItemWriter;

    @Bean
    public Job updateJob() {
        return jobBuilderFactory.get("updateJob")
            .start(updateStep())
            .build();
    }

    @Bean
    public Step updateStep() {
        return stepBuilderFactory.get("updateStep")
            .<Map<String, LinkedList<EntityClass>>, Map<String, LinkedList<EntityClass>>>chunk(10)
            .reader(itemReader()) // You need to define how you're reading the Map data
            .writer(mapEntityItemWriter)
            .build();
    }

    // Define an ItemReader to provide Map<String, LinkedList<EntityClass>> data
    @Bean
    public ItemReader<Map<String, LinkedList<EntityClass>>> itemReader() {
        return new CustomMapItemReader();
    }
}
public class CustomMapItemReader implements ItemReader<Map<String, LinkedList<EntityClass>>> {

    private List<Map<String, LinkedList<EntityClass>>> data;
    private int index = 0;

    public CustomMapItemReader() {
        // Initialize your Map data here. You can also read it from a file or another source.
        data = new ArrayList<>();
        Map<String, LinkedList<EntityClass>> sampleData = new HashMap<>();
        LinkedList<EntityClass> entityList = new LinkedList<>();
        entityList.add(new EntityClass(1L, "value1", "value2"));
        sampleData.put("key1", entityList);
        data.add(sampleData);
    }

    @Override
    public Map<String, LinkedList<EntityClass>> read() throws Exception {
        if (index < data.size()) {
            return data.get(index++);
        } else {
            return null; // End of reading
        }
    }
}
