package core.models;

import core.models.*;
import core.models.enums.*;
import core.controllers.utils.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modelo de prescripción médica.
 * Vincula un medicamento con una cita específica.
 * 
 * @author jjlora
 */
public class Prescription {

    private Appointment appointment;
    private String medicationName;
    private double dose;
    private String administrationRoute;
    private int treatmentDuration;
    private String additionalInstructions;
    private int frequency;

    public Prescription(Appointment appointment, String medicationName, double dose,
                        String administrationRoute, int treatmentDuration,
                        String additionalInstructions, int frequency) {
        this.appointment = appointment;
        this.medicationName = medicationName;
        this.dose = dose;
        this.administrationRoute = administrationRoute;
        this.treatmentDuration = treatmentDuration;
        this.additionalInstructions = additionalInstructions;
        this.frequency = frequency;
    }

    // --- Getters ---

    public Appointment getAppointment() {
        return appointment;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public double getDose() {
        return dose;
    }

    public String getAdministrationRoute() {
        return administrationRoute;
    }

    public int getTreatmentDuration() {
        return treatmentDuration;
    }

    public String getAdditionalInstructions() {
        return additionalInstructions;
    }

    public int getFrequency() {
        return frequency;
    }

    /**
     * Serializa la prescripción a un Map para enviar a la vista.
     */
    public Map<String, String> serialize() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("appointmentId", appointment.getId());
        data.put("medicationName", medicationName);
        data.put("dose", String.valueOf(dose));
        data.put("administrationRoute", administrationRoute);
        data.put("treatmentDuration", String.valueOf(treatmentDuration));
        data.put("additionalInstructions", additionalInstructions);
        data.put("frequency", String.valueOf(frequency));
        return data;
    }
}


