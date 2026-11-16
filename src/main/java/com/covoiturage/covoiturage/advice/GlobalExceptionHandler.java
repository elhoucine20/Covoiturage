package com.covoiturage.covoiturage.advice;

import com.covoiturage.covoiturage.advice.ErrorResponse;
import com.covoiturage.covoiturage.exception.BusinessRuleException;
import com.covoiturage.covoiturage.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Gere les erreurs de validation des donnees recues
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ErrorResponse handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.BAD_REQUEST.value(), request);
    }

    // Gere les erreurs liees a un JSON absent ou mal forme
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ErrorResponse handleJsonIllisible(
            HttpMessageNotReadableException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.BAD_REQUEST.value(), request);
    }

    // Gere les erreurs de conversion des parametres. Exemple : envoyer une chaine au lieu d'un identifiant Long.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ErrorResponse handleTypeInvalide(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.BAD_REQUEST.value(), request);
    }

    // Gere les parametres obligatoires absents d'une requete. Exemple : un @RequestParam obligatoire n'est pas envoye.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public ErrorResponse handleParametreManquant(
            MissingServletRequestParameterException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.BAD_REQUEST.value(), request);
    }

    // Gere les cas : "rechercher un trajet avec un identifiant inexistant"
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // 404
    public ErrorResponse handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.NOT_FOUND.value(), request);
    }

    // Gere les demandes visant une ressource inexistante.
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // 404
    public ErrorResponse handleRouteInconnue(
            NoResourceFoundException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.NOT_FOUND.value(), request);
    }

    // Gere les methodes HTTP non supportees par un endpoint.

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED) // 405
    public ErrorResponse handleMethodeInterdite(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.METHOD_NOT_ALLOWED.value(), request);
    }

    // Gere les exceptions liees aux regles metier de l'application
    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public ErrorResponse handleBusinessRule(
            BusinessRuleException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.CONFLICT.value(), request);
    }

    // Gere les contraintes de la base de donnees, Exemple: contrainte UNIQUE ou NOT NULL.
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public ErrorResponse handleIntegrite(
            DataIntegrityViolationException ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.CONFLICT.value(), request);
    }

    // Gere les exceptions globales qui ne sont pas traitees par les autres methodes.

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500
    public ErrorResponse handleInterne(
            Exception ex, WebRequest request) {

        return buildTheError(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), request);
    }

   // construire une reponse
    private ErrorResponse buildTheError(
            Exception ex, int status, WebRequest request) {

        return ErrorResponse.builder()
                .timestamp(new Date())
                .statusCode(status)
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
    }
}
