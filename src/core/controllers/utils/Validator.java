package core.controllers.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Clase utilitaria para realizar validaciones comunes en los controladores.
 * Ayuda a remover la lógica de validación manual repetitiva.
 */
public class Validator {

    /**
     * Verifica si una cadena de texto es nula o vacía (o solo espacios en blanco).
     */
    public static boolean isNullOrEmpty(String val) {
        return val == null || val.trim().isEmpty();
    }

    /**
     * Valida que un campo obligatorio no sea nulo ni vacío con un mensaje personalizado.
     * Retorna una respuesta de BAD_REQUEST si falla la validación; de lo contrario, null.
     */
    public static Response checkRequired(String value, String customMessage) {
        if (isNullOrEmpty(value)) {
            return new Response(customMessage, Status.BAD_REQUEST, null);
        }
        return null;
    }

    /**
     * Valida que dos cadenas sean idénticas (por ejemplo, contraseñas).
     */
    public static Response checkEquals(String val1, String val2, String errorMessage) {
        if (val1 == null || !val1.equals(val2)) {
            return new Response(errorMessage, Status.BAD_REQUEST, null);
        }
        return null;
    }

    /**
     * Valida el formato de un ID de usuario (debe tener exactamente 12 dígitos y ser positivo).
     */
    public static Response validateId(String idStr) {
        if (isNullOrEmpty(idStr)) {
            return new Response("El ID no puede estar vacío.", Status.BAD_REQUEST, null);
        }
        long id;
        try {
            id = Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            return new Response("El ID debe contener solo dígitos.", Status.BAD_REQUEST, null);
        }
        if (idStr.trim().length() != 12) {
            return new Response("El ID debe tener exactamente 12 dígitos.", Status.BAD_REQUEST, null);
        }
        if (id <= 0) {
            return new Response("El ID debe ser un número positivo.", Status.BAD_REQUEST, null);
        }
        return null;
    }

    /**
     * Valida una cadena de texto contra una expresión regular.
     */
    public static Response validateRegex(String value, String regex, String errorMessage) {
        if (value == null || !value.trim().matches(regex)) {
            return new Response(errorMessage, Status.BAD_REQUEST, null);
        }
        return null;
    }

    /**
     * Valida que una fecha tenga el formato AAAA-MM-DD y sea válida.
     */
    public static Response validateDate(String dateStr, String errorMessage) {
        if (isNullOrEmpty(dateStr)) {
            return new Response(errorMessage, Status.BAD_REQUEST, null);
        }
        try {
            LocalDate.parse(dateStr.trim());
            return null;
        } catch (DateTimeParseException e) {
            return new Response(errorMessage, Status.BAD_REQUEST, null);
        }
    }

    /**
     * Valida que una hora tenga el formato HH:mm y sea válida.
     */
    public static Response validateTime(String timeStr, String errorMessage) {
        if (isNullOrEmpty(timeStr)) {
            return new Response(errorMessage, Status.BAD_REQUEST, null);
        }
        try {
            LocalTime.parse(timeStr.trim());
            return null;
        } catch (DateTimeParseException e) {
            return new Response(errorMessage, Status.BAD_REQUEST, null);
        }
    }
}
