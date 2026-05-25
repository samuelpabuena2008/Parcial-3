package core.observer;

/**
 * Tipos de eventos que el sistema puede notificar a los observers.
 * Cada evento corresponde a una operación de cambio en los datos.
 */
public enum EventType {

    USER_ADDED,
    USER_UPDATED,
    APPOINTMENT_ADDED,
    APPOINTMENT_UPDATED,
    HOSPITALIZATION_ADDED,
    HOSPITALIZATION_UPDATED
}


