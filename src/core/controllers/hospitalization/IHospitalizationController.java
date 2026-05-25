package core.controllers.hospitalization;

import core.controllers.utils.Response;

public interface IHospitalizationController {
    Response requestHospitalization(long patientId, long doctorId, String dateStr, String reason, String roomTypeStr, String observations);
    Response approveHospitalization(String hospitalizationId);
    Response denyHospitalization(String hospitalizationId);
    Response hospitalizeFromAppointment(String appointmentId, String dateStr, String reason, String roomTypeStr, String observations);
    Response getDoctorHospitalizations(long doctorId);
}


