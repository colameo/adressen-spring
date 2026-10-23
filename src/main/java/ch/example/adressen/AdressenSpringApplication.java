package ch.example.adressen;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdressenSpringApplication {

    public static void main(String[] args) {
        // Historical local-mean-time offsets (e.g. Switzerland before 1894) are not whole minutes;
        // with a non-UTC JVM default, java.sql.Date's calendar-based conversion reads such stored
        // dates back one day off. Fixing the JVM default (not just a Hibernate property - this also
        // covers plain java.sql.Date/Calendar use outside Hibernate) avoids the whole class of bug.
        // Verified: without this, publishedAt=1843-10-01 round-tripped as 1843-09-30.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(AdressenSpringApplication.class, args);
    }
}
