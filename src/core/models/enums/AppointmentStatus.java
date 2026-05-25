package core.models.enums;

import core.models.enums.*;

/**
 * Estados posibles de una cita médica.
 * REQUESTED → PENDING → COMPLETED (o CANCELED en cualquier momento antes de COMPLETED).
 * 
 * @author edangulo
 */
public enum AppointmentStatus {

    REQUESTED,
    PENDING,
    COMPLETED,
    CANCELED
}


