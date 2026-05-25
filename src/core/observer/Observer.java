package core.observer;

/**
 * Interfaz que deben implementar las clases que desean recibir notificaciones
 * de cambios en los datos (patrón Observer).
 */
public interface Observer {

    /**
     * Llamado cuando ocurre un evento en un Observable al que este Observer está suscrito.
     *
     * @param event tipo de evento que ocurrió
     * @param data  datos asociados al evento (puede ser null)
     */
    void update(EventType event, Object data);
}


