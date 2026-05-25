package core.controllers.patient;

import core.controllers.utils.Response;

public interface IPatientController {
    Response registerPatient(String idStr, String username, String firstname, String lastname, String password, String confirmPassword, String email, String birthdateStr, boolean gender, String phoneStr, String address);
    Response updatePatient(long currentId, String username, String firstname, String lastname, String password, String confirmPassword, String email, String birthdateStr, boolean gender, String phoneStr, String address);
    Response getPatientData(long patientId);
    Response getDoctorList();
    Response getDoctorsBySpecialty(String specialtyName);
    Response getPatientHospitalizations(long patientId);
}


