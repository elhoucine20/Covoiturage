package com.covoiturage.covoiturage.advice;

import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ForbiddenActionException;
import com.covoiturage.covoiturage.exception.InvalidRequestException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j //cree automatiquement un objet "log" dans la classe (console=>warn,error)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 donnees invalides @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        // Collectors.joining(", ") : colle tous les textes avec une virgule entre eux
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        // Les donnees sont refusees par la validation : on log en WARN
        log.warn("Validation refusee : {}", detail);
        return buildTheError(detail, HttpStatus.BAD_REQUEST.value(), request);
    }

    // 400 JSON absent ou mal form
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ProblemDetail handleJsonIllisible(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.warn("JSON illisible : {}", ex.getMessage());
        return buildTheError("Le corps de la requete est absent ou mal forme",
                HttpStatus.BAD_REQUEST.value(), request);
    }

    // 400 mauvais type de parametr
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ProblemDetail handleTypeInvalide(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Type de parametre invalide : {}", ex.getName());
        return buildTheError("La valeur du parametre '" + ex.getName() + "' est invalide",
                HttpStatus.BAD_REQUEST.value(), request);
    }

    // 400 parametre obligatoire absent
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ProblemDetail handleParametreManquant(
            MissingServletRequestParameterException ex, WebRequest request) {

        log.warn("Parametre manquant : {}", ex.getParameterName());
        return buildTheError(ex, HttpStatus.BAD_REQUEST.value(), request);
    }

    // 404 ressource metier introuvable
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // 404
    public ProblemDetail handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Ressource introuvable : {}", ex.getMessage());
        return buildTheError(ex, HttpStatus.NOT_FOUND.value(), request);
    }

    // 404 URL qui n'existe pas dans l'API
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // 404
    public ProblemDetail handleRouteInconnue(
            NoResourceFoundException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.NOT_FOUND.value(), request);
    }

    // 405 mauvaise methode HTTP
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED) // 405
    public ProblemDetail handleMethodeInterdite(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.METHOD_NOT_ALLOWED.value(), request);
    }

    // 409 regle metier violee (email deja utilise, trajet complet, delai de 24 h depasse...).
    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public ProblemDetail handleBusinessRule(
            BusinessRuleException ex, WebRequest request) {

        log.warn("Regle metier refusee : {}", ex.getMessage());
        return buildTheError(ex, HttpStatus.CONFLICT.value(), request);
    }

    // 409 contrainte de la base violee (UNIQUE, NOT NULL...)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public ProblemDetail handleIntegrite(
            DataIntegrityViolationException ex, WebRequest request) {

        log.error("Violation de contrainte en base", ex);
        return buildTheError("Operation impossible : une contrainte de donnees n'est pas respectee",
                HttpStatus.CONFLICT.value(), request);
    }

    // 500 filet de securite pour toutes les erreurs non prevues (bug, NullPointerException...)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500
    public ProblemDetail handleInterne(
            Exception ex, WebRequest request) {

        log.error("Erreur interne", ex);
        return buildTheError("Une erreur interne est survenue",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), request);
    }

    // 403 action sur la ressource d'un autre
    @ExceptionHandler(ForbiddenActionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // 403
    public ProblemDetail handleForbidden(
            ForbiddenActionException ex, WebRequest request) {

        log.warn("Action interdite : {}", ex.getMessage());
        return buildTheError(ex, HttpStatus.FORBIDDEN.value(), request);
    }

    // 400 donnees refusees par le service (places > capacite)
    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ProblemDetail handleInvalidRequest(
            InvalidRequestException ex, WebRequest request) {

        log.warn("Requete invalide : {}", ex.getMessage());
        return buildTheError(ex, HttpStatus.BAD_REQUEST.value(), request);
    }


    // prend le message directement depuis l'exception, puis appelle la version complete.
    private ProblemDetail buildTheError(Exception ex, int status, WebRequest request) {
        return buildTheError(ex.getMessage(), status, request);
    }

    // Construit la reponse JSON unique de tout le projet (utilisee par tous les handlers).
    private ProblemDetail buildTheError(String message, int status, WebRequest request) {
        HttpStatus httpStatus = HttpStatus.valueOf(status);

        // ProblemDetail : classe de Spring
        // forStatusAndDetail remplit "status" (code HTTP) et "detail" (message explicatif).
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, message);

        // title = type d'erreur (ex : "Bad Request", "Not Found", "Conflict")
        problem.setTitle(httpStatus.getReasonPhrase());

        // setProperty : ajoute un champ personnalise au JSON.
        problem.setProperty("timestamp", Instant.now());

        // request.getDescription(false) renvoie "uri=/api/v1/..." : on enleve le prefixe "uri="
        //problem.setProperty("path", request.getDescription(false).replace("uri=", ""));
        return problem;
    }
}