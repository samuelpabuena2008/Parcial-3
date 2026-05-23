package core.models;

import core.models.*;
import core.models.enums.*;
import core.controllers.utils.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de cita médica.
 * Vincula un paciente con un doctor, incluye diagnóstico, prescripciones y estado.
 * 
 * @author edangulo
 */
public class Appointment {

    private final String id;
    private Patient patient;
    private Doctor doctor;
    private Specialty specialty;
    private LocalDateTime datetime;
    private String reason;
    private boolean type; // true = presencial, false = virtual
    private ArrayList<Prescription> prescriptions;
    private AppointmentStatus status;
    private String diagnosis;
    private String observations;
    private String recommendedTreatment;
    private String followUp;

    public Appointment(String id, Patient patient, Doctor doctor, Specialty specialty,
                       LocalDateTime datetime, String reason, boolean type) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.specialty = specialty;
        this.datetime = datetime;
        this.reason = reason;
        this.type = type;
        this.status = AppointmentStatus.REQUESTED;
        this.prescriptions = new ArrayList<>();
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

    public Specialty getSpecialty() {
        return specialty;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public String getReason() {
        return reason;
    }

    public boolean isType() {
        return type;
    }

    public ArrayList<Prescription> getPrescriptions() {
        return prescriptions;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getObservations() {
        return observations;
    }

    public String getRecommendedTreatment() {
        return recommendedTreatment;
    }

    public String getFollowUp() {
        return followUp;
    }

    // --- Setters ---

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public void setRecommendedTreatment(String recommendedTreatment) {
        this.recommendedTreatment = recommendedTreatment;
    }

    public void setFollowUp(String followUp) {
        this.followUp = followUp;
    }

    public boolean addPrescription(Prescription prescrip) {
        return this.prescriptions.add(prescrip);
    }

    /**
     * Serializa la cita a un Map de Strings para enviar a la vista.
     * No expone los objetos Patient ni Doctor directamente.
     */
    public Map<String, String> serialize() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("patientName", patient.getFirstname() + " " + patient.getLastname());
        data.put("patientId", String.valueOf(patient.getId()));
        data.put("doctorName", doctor.getFirstname() + " " + doctor.getLastname());
        data.put("doctorId", String.valueOf(doctor.getId()));
        data.put("specialty", specialty.name());
        data.put("datetime", datetime.toString());
        data.put("reason", reason != null ? reason : "");
        data.put("type", type ? "In-person" : "Virtual");
        data.put("status", status.name());
        data.put("diagnosis", diagnosis != null ? diagnosis : "");
        data.put("observations", observations != null ? observations : "");
        data.put("recommendedTreatment", recommendedTreatment != null ? recommendedTreatment : "");
        data.put("followUp", followUp != null ? followUp : "");
        return data;
    }

    /**
     * Serializa las prescripciones de esta cita como lista de Maps.
     */
    public List<Map<String, String>> serializePrescriptions() {
        List<Map<String, String>> list = new ArrayList<>();
        for (Prescription p : prescriptions) {
            list.add(p.serialize());
        }
        return list;
    }
}


