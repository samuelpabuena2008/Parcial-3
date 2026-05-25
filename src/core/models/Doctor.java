package core.models;

import core.models.*;
import core.models.enums.*;

import java.util.ArrayList;
import java.util.Map;

/**
 * Modelo de doctor. Hereda de User y agrega especialidad, licencia y oficina.
 * Contiene las listas de citas y hospitalizaciones asignadas.
 * 
 * @author edangulo
 */
public class Doctor extends User {

    private Specialty specialty;
    private String licenceNumber;
    private String assignedOffice;
    private ArrayList<Appointment> appointments;
    private ArrayList<Hospitalization> hospitalizations;

    public Doctor(long id, String username, String firstname, String lastname, String password,
                  Specialty specialty, String licenceNumber, String assignedOffice) {
        super(id, username, firstname, lastname, password);
        this.specialty = specialty;
        this.licenceNumber = licenceNumber;
        this.assignedOffice = assignedOffice;
        this.appointments = new ArrayList<>();
        this.hospitalizations = new ArrayList<>();
    }

    // --- Getters ---

    public Specialty getSpecialty() {
        return specialty;
    }

    public String getLicenceNumber() {
        return licenceNumber;
    }

    public String getAssignedOffice() {
        return assignedOffice;
    }

    public ArrayList<Appointment> getAppointments() {
        return appointments;
    }

    public ArrayList<Hospitalization> getHospitalizations() {
        return hospitalizations;
    }

    // --- Setters ---

    public void setSpecialty(Specialty specialty) {
        this.specialty = specialty;
    }

    public void setLicenceNumber(String licenceNumber) {
        this.licenceNumber = licenceNumber;
    }

    public void setAssignedOffice(String assignedOffice) {
        this.assignedOffice = assignedOffice;
    }

    public void addAppointment(Appointment a) {
        this.appointments.add(a);
    }

    public boolean addHospitalization(Hospitalization hosp) {
        return hospitalizations.add(hosp);
    }

    @Override
    public Map<String, String> serialize() {
        Map<String, String> data = super.serialize();
        data.put("type", "doctor");
        data.put("specialty", specialty.name());
        data.put("licenceNumber", licenceNumber);
        data.put("assignedOffice", assignedOffice);
        return data;
    }
}


