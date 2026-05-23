package core.models.storage;

import java.time.LocalDateTime;
import java.util.List;

import core.models.Appointment;
import core.models.Doctor;
import core.models.Hospitalization;
import core.models.Patient;
import core.models.User;
import core.models.enums.Specialty;
import core.observer.Observable;

public interface IStorage extends Observable {
    void loadFromJSON(String path);
    User findUserByUsername(String username);
    User findUserById(long id);
    boolean isUsernameUnique(String username);
    boolean isIdUnique(long id);
    List<Doctor> findDoctorsBySpecialty(Specialty specialty);
    List<Doctor> getAllDoctors();
    List<Patient> getAllPatients();
    void addUser(User user);
    void notifyUserUpdated(User user);
    void addAppointment(Appointment appointment);
    void notifyAppointmentUpdated(Appointment appointment);
    Appointment findAppointmentById(String id);
    List<Appointment> getAppointmentsByPatient(long patientId);
    List<Appointment> getAppointmentsByDoctor(long doctorId, boolean pendingOnly);
    boolean isDoctorAvailable(Doctor doctor, LocalDateTime dateTime);
    void addHospitalization(Hospitalization hospitalization);
    void notifyHospitalizationUpdated(Hospitalization hospitalization);
    Hospitalization findHospitalizationById(String id);
    List<Hospitalization> getHospitalizationsByDoctor(long doctorId);
    String generateAppointmentId(long patientId);
    String generateHospitalizationId(long patientId);
    List<User> getUsers();
    List<Appointment> getAppointments();
    List<Hospitalization> getHospitalizations();
}
