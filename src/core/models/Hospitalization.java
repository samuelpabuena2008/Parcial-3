package core.models;

import core.models.enums.HospitalizationStatus;
import core.models.enums.RoomType;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modelo de hospitalización.
 * Vincula un paciente con un doctor, incluye habitación, razón y estado.
 * 
 * @author edangulo
 */
public class Hospitalization {

    private final String id;
    private Patient patient;
    private Doctor doctor;
    private LocalDate date;
    private String reason;
    private RoomType roomType;
    private String observations;
    private HospitalizationStatus status;

    /**
     * Constructor con estado por defecto REQUESTED.
     */
    public Hospitalization(String id, Patient patient, Doctor doctor, LocalDate date,
                           String reason, RoomType roomType, String observations) {
        this.id = id;
        this.patient = patient;
        patient.setHospitalization(this);
        this.doctor = doctor;
        doctor.addHospitalization(this);
        this.date = date;
        this.reason = reason;
        this.roomType = roomType;
        this.observations = observations;
        this.status = HospitalizationStatus.REQUESTED;
    }

    /**
     * Constructor con estado explícito (usado para hospitalizaciones directas desde cita).
     */
    public Hospitalization(String id, Patient patient, Doctor doctor, LocalDate date,
                           String reason, RoomType roomType, String observations,
                           HospitalizationStatus status) {
        this.id = id;
        this.patient = patient;
        patient.setHospitalization(this);
        this.doctor = doctor;
        doctor.addHospitalization(this);
        this.date = date;
        this.reason = reason;
        this.roomType = roomType;
        this.observations = observations;
        this.status = status;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getReason() {
        return reason;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public String getObservations() {
        return observations;
    }

    public HospitalizationStatus getStatus() {
        return status;
    }

    // --- Setters ---

    public void setStatus(HospitalizationStatus status) {
        this.status = status;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    /**
     * Serializa la hospitalización a un Map para enviar a la vista.
     */
    public Map<String, String> serialize() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("patientName", patient.getFirstname() + " " + patient.getLastname());
        data.put("patientId", String.valueOf(patient.getId()));
        data.put("doctorName", doctor.getFirstname() + " " + doctor.getLastname());
        data.put("doctorId", String.valueOf(doctor.getId()));
        data.put("date", date != null ? date.toString() : "");
        data.put("reason", reason != null ? reason : "");
        data.put("roomType", roomType.name());
        data.put("observations", observations != null ? observations : "");
        data.put("status", status.name());
        return data;
    }
}


