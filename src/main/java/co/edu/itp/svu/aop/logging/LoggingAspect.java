package co.edu.itp.svu.aop.logging;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tech.jhipster.config.JHipsterConstants;

/**
 * Aspect for logging execution of service and repository Spring components.
 *
 * By default, it only runs with the "dev" profile.
 */
@Aspect
public class LoggingAspect {

    private final Environment env;

    public LoggingAspect(Environment env) {
        this.env = env;
    }

    /**
     * Pointcut that matches all repositories, services and Web REST endpoints.
     */
    @Pointcut(
        "within(@org.springframework.stereotype.Repository *)" +
        " || within(@org.springframework.stereotype.Service *)" +
        " || within(@org.springframework.web.bind.annotation.RestController *)"
    )
    public void springBeanPointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    /**
     * Pointcut that matches all Spring beans in the application's main packages.
     */
    @Pointcut("within(co.edu.itp.svu.repository..*)" + " || within(co.edu.itp.svu.service..*)" + " || within(co.edu.itp.svu.web.rest..*)")
    public void applicationPackagePointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    /**
     * Retrieves the {@link Logger} associated to the given {@link JoinPoint}.
     *
     * @param joinPoint join point we want the logger for.
     * @return {@link Logger} associated to the given {@link JoinPoint}.
     */
    private Logger logger(JoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
    }

    // Directory and inbox methods may receive or return personal data, even in the development profile.
    @Pointcut(
        "within(co.edu.itp.svu.service.notification..*) || within(co.edu.itp.svu..*Notificacion*) || within(co.edu.itp.svu..*Notification*) || within(co.edu.itp.svu..*Oficina*) || within(co.edu.itp.svu..*Office*) || within(co.edu.itp.svu..*User*)"
    )
    public void directoryOrNotificationPointcut() {}

    @Pointcut(
        "within(co.edu.itp.svu..*Respuesta*) || within(co.edu.itp.svu.repository.ResponseQueryRepository+) " +
        "|| execution(* co.edu.itp.svu.service.PqrsService.findPublicPqrsByAccessToken(..)) " +
        "|| execution(* co.edu.itp.svu.service.PqrsService.addPublicResponseToPqrs(..)) " +
        "|| execution(* co.edu.itp.svu.service.PqrsService.findPqrsById(..)) " +
        "|| execution(* co.edu.itp.svu.web.rest.PqrsResource.getPublicPqrsByAccessToken(..)) " +
        "|| execution(* co.edu.itp.svu.web.rest.PqrsResource.submitPublicResponse(..)) " +
        "|| execution(* co.edu.itp.svu.web.rest.PqrsResource.getPqrs(..))"
    )
    public void responseDataPointcut() {}

    @Pointcut(
        "within(co.edu.itp.svu.service.storage.AttachmentOperationCoordinator+) " +
        "|| execution(* co.edu.itp.svu.service.PqrsService.createPublicPqrs(..)) " +
        "|| execution(* co.edu.itp.svu.web.rest.PqrsResource.createPublicPqrs(..)) " +
        "|| execution(* co.edu.itp.svu.web.rest.PqrsResource.createPublicPqrsMultipart(..)) " +
        "|| execution(* co.edu.itp.svu.web.rest.PqrsResource.downloadPublicCaseAttachment(..)) " +
        "|| execution(* co.edu.itp.svu.web.rest.PqrsResource.downloadCaseAttachment(..)) " +
        "|| execution(* co.edu.itp.svu.service.ArchivoAdjuntoService.openForCitizenToken(..)) " +
        "|| execution(* co.edu.itp.svu.service.ArchivoAdjuntoService.openForCase(..))"
    )
    public void attachmentSensitivePointcut() {}

    // Statistical filters and small counts should stay out of argument and result logs.
    @Pointcut("within(co.edu.itp.svu..PqrsStatistics*)")
    public void statisticsPointcut() {}

    /**
     * Advice that logs methods throwing exceptions.
     *
     * @param joinPoint join point for advice.
     * @param e exception.
     */
    @AfterThrowing(
        pointcut = "applicationPackagePointcut() && springBeanPointcut() && !directoryOrNotificationPointcut() && !responseDataPointcut() && !attachmentSensitivePointcut() && !statisticsPointcut()",
        throwing = "e"
    )
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        if (env.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))) {
            logger(joinPoint).error(
                "Exception in {}() with cause = '{}' and exception = '{}'",
                joinPoint.getSignature().getName(),
                e.getCause() != null ? e.getCause() : "NULL",
                e.getMessage(),
                e
            );
        } else {
            logger(joinPoint).error(
                "Exception in {}() with cause = {}",
                joinPoint.getSignature().getName(),
                e.getCause() != null ? String.valueOf(e.getCause()) : "NULL"
            );
        }
    }

    /**
     * Advice that logs when a method is entered and exited.
     *
     * @param joinPoint join point for advice.
     * @return result.
     * @throws Throwable throws {@link IllegalArgumentException}.
     */
    @Around(
        "applicationPackagePointcut() && springBeanPointcut() && !directoryOrNotificationPointcut() && !responseDataPointcut() && !attachmentSensitivePointcut() && !statisticsPointcut()"
    )
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = logger(joinPoint);
        if (log.isDebugEnabled()) {
            log.debug("Enter: {}() with argument[s] = {}", joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
        }
        try {
            Object result = joinPoint.proceed();
            if (log.isDebugEnabled()) {
                log.debug("Exit: {}() with result = {}", joinPoint.getSignature().getName(), result);
            }
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument: {} in {}()", Arrays.toString(joinPoint.getArgs()), joinPoint.getSignature().getName());
            throw e;
        }
    }
}
