import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OracleReadTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    public OracleReadTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Example query to fetch data
        String sql = "SELECT * FROM your_table";
        
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            // Map each row to a desired object or perform an action with the data
            System.out.println("Column1: " + rs.getString("column1"));
            System.out.println("Column2: " + rs.getString("column2"));
            return null; // Return a mapped object if needed
        });
        
        return RepeatStatus.FINISHED;
    }
}
