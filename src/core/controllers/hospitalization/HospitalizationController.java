package core.controllers.hospitalization;

import core.models.*;
import core.models.enums.*;
import core.controllers.utils.*;

import core.models.Appointment;
import core.models.Doctor;
import core.models.Patient;
import core.models.User;
import core.models.Hospitalization;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import core.models.storage.IStorage;

/**
 * Controlador de hospitalizaciones.
 * Gestiona solicitudes, aprobaciones y visualización de hospitalizaciones.
 */
public class HospitalizationController implements IHospitalizationController {

    private final IStorage database;

    public HospitalizationController(IStorage database) {
        this.database = database;
    }

    /**
     * Solicita una hospitalización para un paciente.
     */
    public Response requestHospitalization(long patientId, long doctorId, String dateStr,
                                            String reason, String roomTypeStr, String observations) {
        User patientUser = database.findUserById(patientId);
        if (!(patientUser instanceof Patient))
            return new Response("Paciente no encontrado.", Status.NOT_FOUND, null);

        User doctorUser = database.findUserById(doctorId);
        if (!(doctorUser instanceof Doctor))
            return new Response("Doctor no encontrado.", Status.NOT_FOUND, null);

        Patient patient = (Patient) patientUser;
        Doctor doctor = (Doctor) doctorUser;

        // Validar fecha
        Response check;
        if ((check = Validator.validateDate(dateStr, "Formato de fecha inválido. Use AAAA-MM-DD.")) != null) return check;
        LocalDate date = LocalDate.parse(dateStr.trim());

        // Validar tipo de habitación
        RoomType roomType;
        try {
            roomType = RoomType.valueOf(roomTypeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return new Response("Tipo de habitación no válido.", Status.BAD_REQUEST, null);
        }

        if ((check = Validator.checkRequired(reason, "La razón de hospitalización no puede estar vacía.")) != null) return check;

        String hospId = database.generateHospitalizationId(patientId);
        Hospitalization hosp = new Hospitalization(hospId, patient, doctor, date,
                reason.trim(), roomType, observations != null ? observations.trim() : "");
        database.addHospitalization(hosp);

        return new Response("Hospitalización solicitada exitosamente. ID: " + hospId, Status.OK, hosp.serialize());
    }

    /**
     * Aprueba una hospitalización (cambia de REQUESTED a ONGOING).
     */
    public Response approveHospitalization(String hospitalizationId) {
        Hospitalization h = database.findHospitalizationById(hospitalizationId);
        if (h == null)
            return new Response("Hospitalización no encontrada.", Status.NOT_FOUND, null);
        if (h.getStatus() != HospitalizationStatus.REQUESTED)
            return new Response("Solo se pueden aprobar hospitalizaciones en estado REQUESTED.", Status.BAD_REQUEST, null);

        h.setStatus(HospitalizationStatus.ONGOING);
        database.notifyHospitalizationUpdated(h);
        return new Response("Hospitalización aprobada.", Status.OK, h.serialize());
    }

    /**
     * Deniega una hospitalización (cambia de REQUESTED a CANCELED).
     */
    public Response denyHospitalization(String hospitalizationId) {
        Hospitalization h = database.findHospitalizationById(hospitalizationId);
        if (h == null)
            return new Response("Hospitalización no encontrada.", Status.NOT_FOUND, null);
        if (h.getStatus() != HospitalizationStatus.REQUESTED)
            return new Response("Solo se pueden denegar hospitalizaciones en estado REQUESTED.", Status.BAD_REQUEST, null);

        h.setStatus(HospitalizationStatus.CANCELED);
        database.notifyHospitalizationUpdated(h);
        return new Response("Hospitalización denegada.", Status.OK, h.serialize());
    }

    /**
     * Hospitaliza a un paciente directamente desde una cita.
     * La cita pasa a COMPLETED y la hospitalización se crea con estado ONGOING.
     */
    public Response hospitalizeFromAppointment(String appointmentId,
                                                String dateStr, String reason,
                                                String roomTypeStr, String observations) {
        Appointment a = database.findAppointmentById(appointmentId);
        if (a == null)
            return new Response("Cita no encontrada.", Status.NOT_FOUND, null);

        Patient patient = a.getPatient();
        Doctor doctor = a.getDoctor();

        Response check;
        if ((check = Validator.validateDate(dateStr, "Formato de fecha inválido. Use AAAA-MM-DD.")) != null) return check;
        LocalDate date = LocalDate.parse(dateStr.trim());

        RoomType roomType;
        try {
            roomType = RoomType.valueOf(roomTypeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return new Response("Tipo de habitación no válido.", Status.BAD_REQUEST, null);
        }

        // Completar la cita
        a.setStatus(AppointmentStatus.COMPLETED);
        database.notifyAppointmentUpdated(a);

        // Crear hospitalización directamente en ONGOING
        String hospId = database.generateHospitalizationId(patient.getId());
        Hospitalization hosp = new Hospitalization(hospId, patient, doctor, date,
                reason != null ? reason.trim() : "", roomType,
                observations != null ? observations.trim() : "",
                HospitalizationStatus.ONGOING);
        database.addHospitalization(hosp);

        return new Response("Paciente hospitalizado exitosamente. ID: " + hospId, Status.OK, hosp.serialize());
    }

    /**
     * Retorna las hospitalizaciones de un doctor serializadas.
     */
    public Response getDoctorHospitalizations(long doctorId) {
        return new Response("Hospitalizaciones obtenidas.", Status.OK, Serializer.serializeList(database.getHospitalizationsByDoctor(doctorId)));
    }
}
