package co.edu.itp.svu.config.dbmigrations;

import co.edu.itp.svu.domain.Authority;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.security.AuthoritiesConstants;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;

@ChangeUnit(id = "v007-add-Front-desck-cs-authority", order = "007", author = "luiscarlosjo157")
public class V007_AddFrontDeskCsAuthority {

    private final Logger log = LoggerFactory.getLogger(V007_AddFrontDeskCsAuthority.class);

    private MongoTemplate mongoTemplate = null;
    private final PasswordEncoder passwordEncoder;

    public V007_AddFrontDeskCsAuthority(MongoTemplate mongoTemplate, PasswordEncoder passwordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Execution
    public void addFrontDeskCsRoleAndDefaultUser() {
        String frontDeskCsRoleName = AuthoritiesConstants.FRONT_DESK_CS;
        Authority frontDeskCsAuthority;

        Query authorityQuery = Query.query(Criteria.where("name").is(frontDeskCsRoleName));
        frontDeskCsAuthority = mongoTemplate.findOne(authorityQuery, Authority.class);

        if (frontDeskCsAuthority == null) {
            frontDeskCsAuthority = new Authority();
            frontDeskCsAuthority.setName(frontDeskCsRoleName);
            mongoTemplate.save(frontDeskCsAuthority);
            log.info("Created Authority: {}", frontDeskCsRoleName);
        } else {
            log.info("Authority {} already exists. Using existing authority.", frontDeskCsRoleName);
        }

        Query userQuery = Query.query(Criteria.where("login").is("frontDesk"));
        boolean userExists = mongoTemplate.exists(userQuery, User.class);
        if (!userExists) {
            User defaultUser = new User();
            defaultUser.setLogin("frontDesk");
            defaultUser.setPassword(passwordEncoder.encode("frontDesk"));
            defaultUser.setFirstName("front Desk");
            defaultUser.setLastName("front Desk");
            defaultUser.setEmail("frontDesk@gmail");
            defaultUser.setActivated(true);
            defaultUser.setLangKey("es");

            Set<Authority> authorities = new HashSet<>();
            if (frontDeskCsAuthority != null) {
                authorities.add(frontDeskCsAuthority);
            }
            defaultUser.setAuthorities(authorities);

            mongoTemplate.save(defaultUser);
            log.info("Created default user '{}' with authority '{}'", "frontDesk", frontDeskCsRoleName);
        } else {
            log.warn("Default user '{}' already exists. Skipping user creation.", "frontDesk");
        }
    }

    @RollbackExecution
    public void rollback() {
        String frontDeskCsRoleName = AuthoritiesConstants.FRONT_DESK_CS;
        String defaultUserLogin = "frontDesk";

        Query userQuery = Query.query(Criteria.where("login").is(defaultUserLogin));
        long deletedUsersCount = mongoTemplate.remove(userQuery, User.class).getDeletedCount();
        if (deletedUsersCount > 0) {
            log.info("Rolled back (removed) default user: {}", defaultUserLogin);
        } else {
            log.warn("Default user {} not found during rollback.", defaultUserLogin);
        }

        Query authorityQuery = Query.query(Criteria.where("name").is(frontDeskCsRoleName));
        long deletedAuthoritiesCount = mongoTemplate.remove(authorityQuery, Authority.class).getDeletedCount();
        if (deletedAuthoritiesCount > 0) {
            log.info("Rolled back (removed) Authority: {}", frontDeskCsRoleName);
        } else {
            log.warn("Authority {} not found during rollback.", frontDeskCsRoleName);
        }
    }
}
