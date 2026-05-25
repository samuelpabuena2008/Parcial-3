package core.observer;

import core.models.*;
import core.models.enums.*;
import core.controllers.utils.Response;

/**
 * Interfaz que deben implementar las clases que emiten notificaciones de cambios.
 * En este proyecto, Database es el principal Observable.
 */
public interface Observable {

    /**
     * Registra un observer para recibir notificaciones.
     */
    void addObserver(Observer observer);

    /**
     * Elimina un observer registrado.
     */
    void removeObserver(Observer observer);

    /**
     * Notifica a todos los observers registrados sobre un evento.
     *
     * @param event tipo de evento
     * @param data  datos asociados al evento
     */
    void notifyObservers(EventType event, Object data);
}


