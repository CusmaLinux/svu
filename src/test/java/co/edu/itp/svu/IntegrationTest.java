package co.edu.itp.svu;

import co.edu.itp.svu.config.AsyncSyncConfiguration;
import co.edu.itp.svu.config.EmbeddedMongo;
import co.edu.itp.svu.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { VentanillaUnicaApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class },
    properties = { "spring.profiles.active=test" }
)
@EmbeddedMongo
public @interface IntegrationTest {
}
