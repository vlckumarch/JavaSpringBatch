@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
            .driverClassName("org.postgresql.Driver")
            .url("jdbc:postgresql://localhost:5432/mydatabase")
            .username("myusername")
            .password("mypassword")
            .build();
    }

    @Bean
    public JdbcPagingItemReader<MyEntity> itemReader(DataSource dataSource, PagingQueryProvider queryProvider) {
        JdbcPagingItemReader<MyEntity> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(100);
        reader.setRowMapper(new BeanPropertyRowMapper<>(MyEntity.class));
        reader.setQueryProvider(queryProvider);
        return reader;
    }

    @Bean
    public PagingQueryProvider queryProvider(DataSource dataSource) {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("SELECT id, name, description");
        queryProvider.setFromClause("FROM my_table");
        queryProvider.setWhereClause("WHERE condition = 'value'");
        queryProvider.setSortKey("id");
        try {
            return queryProvider.getObject();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the query provider", e);
        }
    }

    @Bean
    public Step step1(StepBuilderFactory stepBuilderFactory, 
                      ItemReader<MyEntity> reader, 
                      ItemProcessor<MyEntity, MyEntity> processor, 
                      ItemWriter<MyEntity> writer) {
        return stepBuilderFactory.get("step1")
            .<MyEntity, MyEntity>chunk(100)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public Job importUserJob(JobBuilderFactory jobBuilderFactory, Step step1) {
        return jobBuilderFactory.get("importUserJob")
            .incrementer(new RunIdIncrementer())
            .flow(step1)
            .end()
            .build();
    }

    @Bean
    public ItemProcessor<MyEntity, MyEntity> processor() {
        return item -> {
            // Processing logic
            return item;
        };
    }

    @Bean
    public ItemWriter<MyEntity> writer() {
        return items -> {
            for (MyEntity item : items) {
                System.out.println("Writing item: " + item);
            }
        };
    }
}
