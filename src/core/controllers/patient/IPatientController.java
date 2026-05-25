package core.controllers.patient;

import core.controllers.utils.Response;
import core.controllers.utils.Status;

public interface IPatientController {
    Response registerPatient(String idStr, String username, String firstname, String lastname, String password, String confirmPassword, String phone, String email, boolean gender, String address, String birthdateStr);
    Response updatePatient(long currentId, String username, String firstname, String lastname, String password, String confirmPassword, String phone, String email, boolean gender, String address, String birthdateStr);
    Response getPatientData(long patientId);
    Response getDoctorList();
    Response getDoctorsBySpecialty(String specialtyName);
    Response getPatientHospitalizations(long patientId);
}


