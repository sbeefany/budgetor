package budgetor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;
import java.sql.Connection;
import java.sql.ResultSet;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DatabaseSchemaTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsAndTablesExist() throws Exception {
        assertThat(dataSource).isNotNull();

        try (Connection conn = dataSource.getConnection();
                ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })) {

            boolean categoriesFound = false;
            boolean transactionsFound = false;
            boolean goalsFound = false;

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME").toLowerCase();
                if ("categories".equals(tableName))
                    categoriesFound = true;
                if ("transactions".equals(tableName))
                    transactionsFound = true;
                if ("goals".equals(tableName))
                    goalsFound = true;
            }

            assertThat(categoriesFound).as("Table categories should exist").isTrue();
            assertThat(transactionsFound).as("Table transactions should exist").isTrue();
            assertThat(goalsFound).as("Table goals should exist").isTrue();
        }
    }
}
