@Component
public class MapItemWriter implements ItemWriter<Map<String, LinkedList<Object>>> {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void write(List<? extends Map<String, LinkedList<Object>>> items) throws Exception {
        for (Map<String, LinkedList<Object>> item : items) {
            // Iterate over the map and update the database
            for (Map.Entry<String, LinkedList<Object>> entry : item.entrySet()) {
                String key = entry.getKey();
                LinkedList<Object> values = entry.getValue();

                // Assuming values LinkedList contains two elements: column1, column2
                if (values.size() >= 2) {
                    String sql = "UPDATE your_table SET column1 = :column1, column2 = :column2 WHERE key_column = :key";
                    
                    Map<String, Object> params = new HashMap<>();
                    params.put("key", key);
                    params.put("column1", values.get(0)); // Assuming first element is for column1
                    params.put("column2", values.get(1)); // Assuming second element is for column2

                    jdbcTemplate.update(sql, params);
                }
            }
        }
    }
}

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private MapItemWriter mapItemWriter;

    @Bean
    public Job updateJob() {
        return jobBuilderFactory.get("updateJob")
            .start(updateStep())
            .build();
    }

    @Bean
    public Step updateStep() {
        return stepBuilderFactory.get("updateStep")
            .<Map<String, LinkedList<Object>>, Map<String, LinkedList<Object>>>chunk(10)
            .reader(itemReader()) // You need to define how you're reading the Map data
            .writer(mapItemWriter)
            .build();
    }

    // Define an ItemReader to provide Map<String, LinkedList<Object>> data
    @Bean
    public ItemReader<Map<String, LinkedList<Object>>> itemReader() {
        return new CustomMapItemReader();
    }
}

public class CustomMapItemReader implements ItemReader<Map<String, LinkedList<Object>>> {

    private List<Map<String, LinkedList<Object>>> data;
    private int index = 0;

    public CustomMapItemReader() {
        // Initialize your Map data here. You can also read it from a file or another source.
        data = new ArrayList<>();
        Map<String, LinkedList<Object>> sampleData = new HashMap<>();
        LinkedList<Object> values = new LinkedList<>();
        values.add("value1");
        values.add("value2");
        sampleData.put("key1", values);
        data.add(sampleData);
    }

    @Override
    public Map<String, LinkedList<Object>> read() throws Exception {
        if (index < data.size()) {
            return data.get(index++);
        } else {
            return null; // End of reading
        }
    }
}

