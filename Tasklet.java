In Spring Batch, when you want to read data from a database without chunk orientation (i.e., not in chunks but as a single process), you can use a task-oriented step instead of the chunk-oriented approach. Here's how you can achieve it:

1. Use a Tasklet Step

A Tasklet step is suitable for scenarios where you want to perform a single operation, like reading data from a database, without chunk processing.

Implementation Steps:

Create a Tasklet: Implement the Tasklet interface and use a JdbcTemplate or JpaRepository to fetch data from the database.


import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseReadTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseReadTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // Query the database
        String query = "SELECT * FROM my_table";
        jdbcTemplate.query(query, (rs, rowNum) -> {
            // Process each row
            System.out.println("Row data: " + rs.getString("column_name"));
            return null;
        });
        return RepeatStatus.FINISHED;
    }
}

Configure the Step: Use this tasklet in a step configuration.


import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchConfig {

    private final StepBuilderFactory stepBuilderFactory;
    private final DatabaseReadTasklet databaseReadTasklet;

    public BatchConfig(StepBuilderFactory stepBuilderFactory, DatabaseReadTasklet databaseReadTasklet) {
        this.stepBuilderFactory = stepBuilderFactory;
        this.databaseReadTasklet = databaseReadTasklet;
    }

    @Bean
    public Step databaseReadStep() {
        return stepBuilderFactory.get("databaseReadStep")
                .tasklet(databaseReadTasklet)
                .build();
    }
}

Include in Job: Add this step to your job configuration.


import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final Step databaseReadStep;

    public JobConfig(JobBuilderFactory jobBuilderFactory, Step databaseReadStep) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.databaseReadStep = databaseReadStep;
    }

    @Bean
    public Job databaseReadJob() {
        return jobBuilderFactory.get("databaseReadJob")
                .start(databaseReadStep)
                .build();
    }
}

2. Advantages of Task-Oriented Processing

Simpler than chunk-oriented processing for one-off tasks.

Provides more control over the database operations.

No need for ItemReader, ItemProcessor, and ItemWriter.


When to Use

When you need to read and process a small amount of data.

When chunk-oriented processing is unnecessary or adds complexity.


By using a Tasklet, you can efficiently read data from a database without chunking, making the process straightforward and highly customizable.

