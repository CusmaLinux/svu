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

@ChangeUnit(id = "v007-add-Front-desk-cs-authority", order = "007", author = "luiscarlosjo157")
public class V007_AddFrontDeskCsAuthority {

    private final Logger LOG = LoggerFactory.getLogger(V007_AddFrontDeskCsAuthority.class);

    private MongoTemplate mongoTemplate;

    public V007_AddFrontDeskCsAuthority(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
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
            LOG.info("Created Authority: {}", frontDeskCsRoleName);
        } else {
            LOG.info("Authority {} already exists. Using existing authority.", frontDeskCsRoleName);
        }

        Query userQuery = Query.query(Criteria.where("login").is("frontdesk"));
        boolean userExists = mongoTemplate.exists(userQuery, User.class);
        if (!userExists) {
            User defaultUser = new User();
            defaultUser.setLogin("frontdesk");
            defaultUser.setPassword("$2a$10$Mq1.rf19CNUpuhnia88cH.j/xV5QbNrEyp03iUTNVaqUY7SpY3rp2");
            defaultUser.setFirstName("front Desk");
            defaultUser.setLastName("front Desk");
            defaultUser.setEmail("frontdesk@gmail");
            defaultUser.setActivated(true);
            defaultUser.setLangKey("es");

            Set<Authority> authorities = new HashSet<>();
            if (frontDeskCsAuthority != null) {
                authorities.add(frontDeskCsAuthority);
            }
            defaultUser.setAuthorities(authorities);

            mongoTemplate.save(defaultUser);
            LOG.info("Created default user '{}' with authority '{}'", "frontdesk", frontDeskCsRoleName);
        } else {
            LOG.warn("Default user '{}' already exists. Skipping user creation.", "frontdesk");
        }
    }

    @RollbackExecution
    public void rollback() {
        String frontDeskCsRoleName = AuthoritiesConstants.FRONT_DESK_CS;
        String defaultUserLogin = "frontdesk";

        Query userQuery = Query.query(Criteria.where("login").is(defaultUserLogin));
        long deletedUsersCount = mongoTemplate.remove(userQuery, User.class).getDeletedCount();
        if (deletedUsersCount > 0) {
            LOG.info("Rolled back (removed) default user: {}", defaultUserLogin);
        } else {
            LOG.warn("Default user {} not found during rollback.", defaultUserLogin);
        }

        Query authorityQuery = Query.query(Criteria.where("name").is(frontDeskCsRoleName));
        long deletedAuthoritiesCount = mongoTemplate.remove(authorityQuery, Authority.class).getDeletedCount();
        if (deletedAuthoritiesCount > 0) {
            LOG.info("Rolled back (removed) Authority: {}", frontDeskCsRoleName);
        } else {
            LOG.warn("Authority {} not found during rollback.", frontDeskCsRoleName);
        }
    }
}
