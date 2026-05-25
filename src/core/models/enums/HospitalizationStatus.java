package core.models.enums;

import core.models.*;
import core.models.enums.*;
import core.controllers.utils.Response;

/**
 * Estados posibles de una hospitalización.
 * REQUESTED → ONGOING (aprobada) o CANCELED (denegada).
 * 
 * @author edangulo
 */
public enum HospitalizationStatus {

    REQUESTED,
    ONGOING,
    CANCELED
}


