package core.controllers.doctor;

import core.controllers.utils.Response;
import core.controllers.utils.Status;

public interface IDoctorController {
    Response registerDoctor(String idStr, String username, String firstname, String lastname, String password, String confirmPassword, String specialtyStr, String licenceNumber, String assignedOffice);
    Response updateDoctor(long currentId, String username, String firstname, String lastname, String password, String confirmPassword, String specialtyStr, String licenceNumber, String assignedOffice);
    Response getDoctorData(long doctorId);
    Response getPatientList();
    Response getDoctorHospitalizations(long doctorId);
}


