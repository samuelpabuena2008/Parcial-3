package core.controllers.appointment;

import core.controllers.utils.Response;

public interface IAppointmentController {
    Response requestAppointmentByDoctor(long patientId, long doctorId, String dateStr, String timeStr, String reason, boolean inPerson);
    Response requestAppointmentBySpecialty(long patientId, String specialtyName, String dateStr, String timeStr, String reason, boolean inPerson);
    Response cancelAppointment(String appointmentId);
    Response getPatientAppointments(long patientId);
    
    Response acceptAppointment(String appointmentId);
    Response completeAppointment(String appointmentId, String diagnosis, String observations, String recommendedTreatment, String followUp);
    Response rescheduleAppointment(String appointmentId, String newTimeStr, String reason);
    Response prescribeMedication(String appointmentId, String medicationName, String doseStr, String administrationRoute, String treatmentDurationStr, String additionalInstructions, String frequencyStr);
    Response getDoctorAppointments(long doctorId, boolean pendingOnly);
}


