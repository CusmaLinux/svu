package co.edu.itp.svu.config.dbmigrations;

import co.edu.itp.svu.domain.Authority;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.security.AuthoritiesConstants;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Migration to create the initial set of offices and their responsible users.
 */
@ChangeUnit(id = "v015-offices-and-users-initialization", order = "015")
public class V015_OfficeCreationMigration {

    private final Logger LOG = LoggerFactory.getLogger(V015_OfficeCreationMigration.class);
    private final MongoTemplate template;
    private final PasswordEncoder passwordEncoder;

    public V015_OfficeCreationMigration(MongoTemplate template, PasswordEncoder passwordEncoder) {
        this.template = template;
        this.passwordEncoder = passwordEncoder;
    }

    @Execution
    public void changeSet() {
        LOG.info("Deleting all existing offices.");
        template.dropCollection(Oficina.class);

        Authority userAuthority = template.findById(AuthoritiesConstants.FUNCTIONARY, Authority.class);
        if (userAuthority == null) {
            LOG.error("Authority '{}' not found. Cannot create users for offices.", AuthoritiesConstants.FUNCTIONARY);
            throw new RuntimeException("Required authority '" + AuthoritiesConstants.FUNCTIONARY + "' not found.");
        }

        List<String> officeNames = Arrays.asList(
            "Consejo Directivo",
            "Consejo Académico",
            "Rectoría",
            "Vicerrectoría Académica",
            "Vicerrectoría Administrativo",
            "Control interno",
            "Planeación",
            "Ciecyt",
            "Internacionalización",
            "Proyección social",
            "Egresados",
            "Facultad de Ingeniería y ciencias básicas",
            "Facultad de Administración, ciencias económicas y contables",
            "Bienestar Universitario",
            "Autoevaluación",
            "Presupuesto",
            "Contabilidad",
            "Talento Humano",
            "Admisiones, Registro y control",
            "Mantenimiento y Recursos físicos",
            "Contratación",
            "Jurídica",
            "Tesorería",
            "Comunicaciones",
            "Tics",
            "Archivo",
            "Ventanilla Unica"
        );

        for (String officeName : officeNames) {
            String rawPassword = generateStrongPassword();

            User responsibleUser = createUserForOffice(officeName, rawPassword, userAuthority);
            User savedUser = template.save(responsibleUser);

            Oficina office = createOffice(officeName, savedUser);
            template.save(office);
        }
        LOG.info("Office and responsible user creation migration completed successfully.");
    }

    /**
     * Creates a User entity for a given office.
     */
    private User createUserForOffice(String officeName, String rawPassword, Authority userAuthority) {
        User user = new User();
        String login = formatToLogin(officeName);

        if (login.length() > 50) {
            login = login.substring(0, 50);
        }

        user.setLogin(login);
        user.setEmail(login + "@itp.edu.co");

        String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(encodedPassword);

        String firstName = officeName;
        if (firstName.length() > 50) {
            firstName = firstName.substring(0, 50);
        }

        user.setFirstName(firstName);
        user.setLastName("Responsable");
        user.setActivated(true);
        user.setLangKey("es");
        user.setCreatedBy("system");
        user.setCreatedDate(Instant.now());
        user.getAuthorities().add(userAuthority);

        return user;
    }

    /**
     * Creates an Oficina entity.
     */
    private Oficina createOffice(String officeName, User responsibleUser) {
        Oficina office = new Oficina();
        office.setNombre(officeName);
        office.setDescripcion("Descripción de " + officeName);
        office.setNivel("1");
        office.setResponsable(responsibleUser);
        office.setOficinaSuperior("Consejo Directivo");
        return office;
    }

    /**
     * Utility to generate a secure random password.
     */
    private String generateStrongPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String allChars = upper + lower + digits + special;

        SecureRandom random = new SecureRandom();
        List<Character> passwordChars = new java.util.ArrayList<>();

        passwordChars.add(upper.charAt(random.nextInt(upper.length())));
        passwordChars.add(lower.charAt(random.nextInt(lower.length())));
        passwordChars.add(digits.charAt(random.nextInt(digits.length())));
        passwordChars.add(special.charAt(random.nextInt(special.length())));

        for (int i = 4; i < 16; i++) {
            passwordChars.add(allChars.charAt(random.nextInt(allChars.length())));
        }

        Collections.shuffle(passwordChars, random);

        return passwordChars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    /**
     * Formats an office name into a login-friendly string (lowercase, no spaces).
     */
    private String formatToLogin(String officeName) {
        return officeName
            .toLowerCase()
            .replace(" ", "-")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .toLowerCase()
            .replaceAll("[^a-z0-9\\-]", "");
    }

    @RollbackExecution
    public void rollback() {
        LOG.warn("Rolling back the office creation migration. Deleting created offices and potentially users.");
        template.dropCollection(Oficina.class);
    }
}
