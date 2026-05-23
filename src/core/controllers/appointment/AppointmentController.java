package core.controllers.appointment;

import core.models.*;
import core.models.enums.*;
import core.controllers.utils.*;
import core.models.storage.IStorage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controlador de citas.
 * Gestiona solicitud, cancelación, aceptación, reagendamiento y prescripciones de citas.
 */
public class AppointmentController implements IAppointmentController {

    private final IStorage database;

    public AppointmentController(IStorage database) {
        this.database = database;
    }

    /**
     * Solicita una cita seleccionando un doctor específico.
     * Valida disponibilidad del doctor y formato de hora (cuartos de hora).
     */
    public Response requestAppointmentByDoctor(long patientId, long doctorId,
                                                String dateStr, String timeStr,
                                                String reason, boolean inPerson) {
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

        // Validar hora: formato HH:mm, minutos en cuartos
        if ((check = Validator.validateTime(timeStr, "Formato de hora inválido. Use HH:mm.")) != null) return check;
        LocalTime time = LocalTime.parse(timeStr.trim());
        if (time.getMinute() % 15 != 0)
            return new Response("Los minutos deben ser 00, 15, 30 o 45.", Status.BAD_REQUEST, null);

        LocalDateTime dateTime = LocalDateTime.of(date, time);

        // Verificar disponibilidad
        if (!database.isDoctorAvailable(doctor, dateTime))
            return new Response("El doctor no tiene disponibilidad en ese horario.", Status.BAD_REQUEST, null);

        // Crear cita
        String appointmentId = database.generateAppointmentId(patientId);
        Appointment appointment = new Appointment(appointmentId, patient, doctor,
                doctor.getSpecialty(), dateTime, reason, inPerson);
        database.addAppointment(appointment);

        return new Response("Cita solicitada exitosamente. ID: " + appointmentId, Status.OK, appointment.serialize());
    }

    /**
     * Solicita una cita seleccionando una especialidad.
     * El sistema busca un doctor disponible de esa especialidad automáticamente.
     */
    public Response requestAppointmentBySpecialty(long patientId, String specialtyName,
                                                   String dateStr, String timeStr,
                                                   String reason, boolean inPerson) {
        User patientUser = database.findUserById(patientId);
        if (!(patientUser instanceof Patient))
            return new Response("Paciente no encontrado.", Status.NOT_FOUND, null);

        Patient patient = (Patient) patientUser;

        // Parsear especialidad
        Specialty specialty;
        try {
            specialty = Specialty.valueOf(specialtyName.replaceAll(" & ", "_").replaceAll(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return new Response("Especialidad no válida.", Status.BAD_REQUEST, null);
        }

        // Validar fecha
        Response check;
        if ((check = Validator.validateDate(dateStr, "Formato de fecha inválido. Use AAAA-MM-DD.")) != null) return check;
        LocalDate date = LocalDate.parse(dateStr.trim());

        // Validar hora
        if ((check = Validator.validateTime(timeStr, "Formato de hora inválido. Use HH:mm.")) != null) return check;
        LocalTime time = LocalTime.parse(timeStr.trim());
        if (time.getMinute() % 15 != 0)
            return new Response("Los minutos deben ser 00, 15, 30 o 45.", Status.BAD_REQUEST, null);

        LocalDateTime dateTime = LocalDateTime.of(date, time);

        // Buscar doctor disponible de esa especialidad
        List<Doctor> doctors = database.findDoctorsBySpecialty(specialty);
        Doctor availableDoctor = null;
        for (Doctor d : doctors) {
            if (database.isDoctorAvailable(d, dateTime)) {
                availableDoctor = d;
                break;
            }
        }
        if (availableDoctor == null)
            return new Response("No hay doctores disponibles de esa especialidad en ese horario.", Status.BAD_REQUEST, null);

        // Crear cita
        String appointmentId = database.generateAppointmentId(patientId);
        Appointment appointment = new Appointment(appointmentId, patient, availableDoctor,
                specialty, dateTime, reason, inPerson);
        database.addAppointment(appointment);

        return new Response("Cita agendada exitosamente\ncon Dr. " + availableDoctor.getFirstname() + " " + availableDoctor.getLastname() + "\nID: " + appointmentId, Status.OK, appointment.serialize());
    }

    /**
     * Cancela una cita. Solo se puede cancelar si no está en estado COMPLETED.
     */
    public Response cancelAppointment(String appointmentId) {
        Appointment appointment = database.findAppointmentById(appointmentId);
        if (appointment == null)
            return new Response("Cita no encontrada.", Status.NOT_FOUND, null);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED)
            return new Response("No se puede cancelar una cita completada.", Status.BAD_REQUEST, null);
        if (appointment.getStatus() == AppointmentStatus.CANCELED)
            return new Response("La cita ya está cancelada.", Status.BAD_REQUEST, null);

        appointment.setStatus(AppointmentStatus.CANCELED);
        database.notifyAppointmentUpdated(appointment);
        return new Response("Cita cancelada exitosamente.", Status.OK, appointment.serialize());
    }

    /**
     * Retorna las citas de un paciente serializadas para mostrar en la vista.
     */
    public Response getPatientAppointments(long patientId) {
        return new Response("Citas obtenidas.", Status.OK, Serializer.serializeList(database.getAppointmentsByPatient(patientId)));
    }

    /**
     * Acepta una cita (cambia de REQUESTED a PENDING).
     */
    public Response acceptAppointment(String appointmentId) {
        Appointment a = database.findAppointmentById(appointmentId);
        if (a == null)
            return new Response("Cita no encontrada.", Status.NOT_FOUND, null);
        if (a.getStatus() != AppointmentStatus.REQUESTED)
            return new Response("Solo se pueden aceptar citas en estado REQUESTED.", Status.BAD_REQUEST, null);

        a.setStatus(AppointmentStatus.PENDING);
        database.notifyAppointmentUpdated(a);
        return new Response("Cita aceptada exitosamente.", Status.OK, a.serialize());
    }

    /**
     * Completa una cita (cambia de PENDING a COMPLETED) con diagnóstico y observaciones.
     */
    public Response completeAppointment(String appointmentId, String diagnosis,
                                         String observations, String recommendedTreatment,
                                         String followUp) {
        Appointment a = database.findAppointmentById(appointmentId);
        if (a == null)
            return new Response("Cita no encontrada.", Status.NOT_FOUND, null);
        if (a.getStatus() != AppointmentStatus.PENDING)
            return new Response("Solo se pueden completar citas en estado PENDING.", Status.BAD_REQUEST, null);

        a.setStatus(AppointmentStatus.COMPLETED);
        a.setDiagnosis(diagnosis != null ? diagnosis.trim() : "");
        a.setObservations(observations != null ? observations.trim() : "");
        a.setRecommendedTreatment(recommendedTreatment != null ? recommendedTreatment.trim() : "");
        a.setFollowUp(followUp != null ? followUp.trim() : "");

        database.notifyAppointmentUpdated(a);
        return new Response("Cita completada exitosamente.", Status.OK, a.serialize());
    }

    /**
     * Reagenda una cita cambiando la hora (no el día).
     * La nueva hora debe ser válida (cuartos de hora) y la razón se añade a la existente.
     */
    public Response rescheduleAppointment(String appointmentId, String newTimeStr, String reason) {
        Appointment a = database.findAppointmentById(appointmentId);
        if (a == null)
            return new Response("Cita no encontrada.", Status.NOT_FOUND, null);
        if (a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.CANCELED)
            return new Response("No se puede reagendar una cita completada o cancelada.", Status.BAD_REQUEST, null);

        // Validar hora
        Response check;
        if ((check = Validator.validateTime(newTimeStr, "Formato de hora inválido. Use HH:mm.")) != null) return check;
        LocalTime newTime = LocalTime.parse(newTimeStr.trim());
        if (newTime.getMinute() % 15 != 0)
            return new Response("Los minutos deben ser 00, 15, 30 o 45.", Status.BAD_REQUEST, null);

        if ((check = Validator.checkRequired(reason, "Debe indicar la razón del cambio de hora.")) != null) return check;

        // Mantener el mismo día, cambiar hora
        LocalDateTime newDateTime = a.getDatetime().with(newTime);

        // Verificar disponibilidad del doctor en el nuevo horario
        if (!database.isDoctorAvailable(a.getDoctor(), newDateTime))
            return new Response("El doctor no tiene disponibilidad en ese horario.", Status.BAD_REQUEST, null);

        // Añadir razón del cambio a la razón original
        String currentReason = a.getReason() != null ? a.getReason() : "";
        a.setReason(currentReason + " | Reagendado: " + reason.trim());
        a.setDatetime(newDateTime);

        database.notifyAppointmentUpdated(a);
        return new Response("Cita reagendada exitosamente.", Status.OK, a.serialize());
    }

    /**
     * Prescribe un medicamento a una cita que esté en estado PENDING.
     */
    public Response prescribeMedication(String appointmentId, String medicationName, String doseStr,
                                         String administrationRoute, String treatmentDurationStr,
                                         String additionalInstructions, String frequencyStr) {
        Appointment a = database.findAppointmentById(appointmentId);
        if (a == null)
            return new Response("Cita no encontrada.", Status.NOT_FOUND, null);
        if (a.getStatus() != AppointmentStatus.PENDING)
            return new Response("Solo se puede prescribir en citas con estado PENDING.", Status.BAD_REQUEST, null);

        Response check;
        if ((check = Validator.checkRequired(medicationName, "El nombre del medicamento no puede estar vacío.")) != null) return check;

        double dose;
        try {
            dose = Double.parseDouble(doseStr.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return new Response("La dosis debe ser un número válido.", Status.BAD_REQUEST, null);
        }

        int treatmentDuration;
        try {
            treatmentDuration = Integer.parseInt(treatmentDurationStr.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return new Response("La duración del tratamiento debe ser un número entero.", Status.BAD_REQUEST, null);
        }

        int frequency;
        try {
            frequency = Integer.parseInt(frequencyStr.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return new Response("La frecuencia debe ser un número entero.", Status.BAD_REQUEST, null);
        }

        Prescription prescription = new Prescription(a, medicationName.trim(), dose,
                administrationRoute != null ? administrationRoute.trim() : "",
                treatmentDuration, additionalInstructions != null ? additionalInstructions.trim() : "",
                frequency);
        a.addPrescription(prescription);

        database.notifyAppointmentUpdated(a);
        return new Response("Prescripción agregada exitosamente.", Status.OK, prescription.serialize());
    }

    /**
     * Retorna las citas del doctor serializadas.
     */
    public Response getDoctorAppointments(long doctorId, boolean pendingOnly) {
        return new Response("OK", Status.OK, Serializer.serializeList(database.getAppointmentsByDoctor(doctorId, pendingOnly)));
    }
}
