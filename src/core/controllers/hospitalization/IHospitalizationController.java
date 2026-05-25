package core.controllers.hospitalization;

import core.controllers.utils.Response;
import core.controllers.utils.Status;

public interface IHospitalizationController {
    Response requestHospitalization(long patientId, long doctorId, String dateStr, String expectedDischargeStr, String reason, String roomNumber);
    Response approveHospitalization(String hospitalizationId);
    Response denyHospitalization(String hospitalizationId);
    Response hospitalizeFromAppointment(String appointmentId, String dateStr, String reason, String roomTypeStr, String observations);
    Response getDoctorHospitalizations(long doctorId);
}


