package core.models.storage;

import core.models.enums.*;

import core.models.Appointment;
import core.models.Doctor;
import core.models.Patient;
import core.models.User;
import core.models.Hospitalization;
import core.models.Administrator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import core.observer.EventType;
import core.observer.Observer;

/**
 * Singleton que actúa como almacenamiento en memoria para el sistema.
 * Carga los datos iniciales desde users.json y gestiona todas las listas
 * de usuarios, citas y hospitalizaciones.
 * Implementa Observable para notificar a las vistas cuando cambian los datos.
 */
public class Database implements IStorage {

    private static Database instance;

    private ArrayList<User> users;
    private ArrayList<Appointment> appointments;
    private ArrayList<Hospitalization> hospitalizations;
    private ArrayList<Observer> observers;

    // Contadores para generar IDs automáticos por paciente
    private Map<Long, Integer> appointmentCounters;
    private Map<Long, Integer> hospitalizationCounters;

    private Database() {
        this.users = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.hospitalizations = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.appointmentCounters = new HashMap<>();
        this.hospitalizationCounters = new HashMap<>();
    }

    /**
     * Retorna la instancia única de Database.
     */
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    // ===================== CARGA DE JSON =====================

    /**
     * Lee el archivo users.json y crea los objetos User correspondientes.
     * Debe llamarse una sola vez al iniciar la aplicación desde Main.java.
     *
     * @param path ruta al archivo users.json
     */
    public void loadFromJSON(String path) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            JSONObject root = new JSONObject(content);
            JSONArray usersArray = root.getJSONArray("users");

            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject obj = usersArray.getJSONObject(i);
                String type = obj.getString("type");
                long id = obj.getLong("id");
                String username = obj.getString("username");
                String firstname = obj.getString("firstname");
                String lastname = obj.getString("lastname");
                String password = obj.getString("password");

                switch (type) {
                    case "admin":
                        users.add(new Administrator(id, username, firstname, lastname, password));
                        break;
                    case "patient":
                        String email = obj.getString("email");
                        LocalDate birthdate = LocalDate.parse(obj.getString("birthdate"));
                        boolean gender = obj.getBoolean("gender");
                        long phone = obj.getLong("phone");
                        String address = obj.getString("address");
                        users.add(new Patient(id, username, firstname, lastname, password,
                                email, birthdate, gender, phone, address));
                        break;
                    case "doctor":
                        Specialty specialty = parseSpecialty(obj.getString("specialty"));
                        String licence = obj.getString("licenceNumber");
                        String office = obj.getString("assignedOffice");
                        users.add(new Doctor(id, username, firstname, lastname, password,
                                specialty, licence, office));
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al cargar users.json: " + e.getMessage());
        }
    }

    // ===================== BÚSQUEDA DE USUARIOS =====================

    /**
     * Busca un usuario por su username.
     * @return el usuario encontrado, o null si no existe
     */
    public User findUserByUsername(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Busca un usuario por su ID.
     * @return el usuario encontrado, o null si no existe
     */
    public User findUserById(long id) {
        for (User u : users) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    /**
     * Verifica si un username ya está en uso.
     */
    public boolean isUsernameUnique(String username) {
        return findUserByUsername(username) == null;
    }

    /**
     * Verifica si un ID ya está en uso.
     */
    public boolean isIdUnique(long id) {
        return findUserById(id) == null;
    }

    /**
     * Retorna todos los doctores de una especialidad específica.
     */
    public List<Doctor> findDoctorsBySpecialty(Specialty specialty) {
        List<Doctor> result = new ArrayList<>();
        for (User u : users) {
            if (u instanceof Doctor) {
                Doctor d = (Doctor) u;
                if (d.getSpecialty() == specialty) {
                    result.add(d);
                }
            }
        }
        return result;
    }

    /**
     * Retorna todos los doctores del sistema.
     */
    public List<Doctor> getAllDoctors() {
        List<Doctor> result = new ArrayList<>();
        for (User u : users) {
            if (u instanceof Doctor) {
                result.add((Doctor) u);
            }
        }
        return result;
    }

    /**
     * Retorna todos los pacientes del sistema.
     */
    public List<Patient> getAllPatients() {
        List<Patient> result = new ArrayList<>();
        for (User u : users) {
            if (u instanceof Patient) {
                result.add((Patient) u);
            }
        }
        return result;
    }

    // ===================== GESTIÓN DE USUARIOS =====================

    /**
     * Agrega un nuevo usuario y notifica a los observers.
     */
    public void addUser(User user) {
        users.add(user);
        notifyObservers(EventType.USER_ADDED, user.serialize());
    }

    /**
     * Notifica que un usuario fue actualizado.
     */
    public void notifyUserUpdated(User user) {
        notifyObservers(EventType.USER_UPDATED, user.serialize());
    }

    // ===================== GESTIÓN DE CITAS =====================

    /**
     * Agrega una cita al sistema, la vincula con el paciente y el doctor,
     * y notifica a los observers.
     */
    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        appointment.getPatient().addAppointment(appointment);
        appointment.getDoctor().addAppointment(appointment);
        notifyObservers(EventType.APPOINTMENT_ADDED, appointment.serialize());
    }

    /**
     * Notifica que una cita fue actualizada (cambio de estado, reagendada, etc.).
     */
    public void notifyAppointmentUpdated(Appointment appointment) {
        notifyObservers(EventType.APPOINTMENT_UPDATED, appointment.serialize());
    }

    /**
     * Busca una cita por su ID.
     */
    public Appointment findAppointmentById(String id) {
        for (Appointment a : appointments) {
            if (a.getId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Retorna las citas de un paciente ordenadas descendentemente por fecha.
     */
    public List<Appointment> getAppointmentsByPatient(long patientId) {
        User u = findUserById(patientId);
        if (u instanceof Patient) {
            Patient p = (Patient) u;
            List<Appointment> sorted = new ArrayList<>(p.getAppointments());
            sorted.sort(Comparator.comparing(Appointment::getDatetime).reversed());
            return sorted;
        }
        return new ArrayList<>();
    }

    /**
     * Retorna las citas de un doctor ordenadas descendentemente por fecha.
     *
     * @param doctorId    ID del doctor
     * @param pendingOnly si true, solo retorna citas con estado PENDING
     */
    public List<Appointment> getAppointmentsByDoctor(long doctorId, boolean pendingOnly) {
        User u = findUserById(doctorId);
        if (u instanceof Doctor) {
            Doctor d = (Doctor) u;
            List<Appointment> result = new ArrayList<>();
            for (Appointment a : d.getAppointments()) {
                if (!pendingOnly || a.getStatus() == AppointmentStatus.PENDING) {
                    result.add(a);
                }
            }
            result.sort(Comparator.comparing(Appointment::getDatetime).reversed());
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * Verifica si un doctor tiene disponibilidad en un horario dado.
     * Un doctor no está disponible si tiene otra cita dentro de un rango de 15 minutos.
     */
    public boolean isDoctorAvailable(Doctor doctor, LocalDateTime dateTime) {
        for (Appointment a : doctor.getAppointments()) {
            if (a.getStatus() == AppointmentStatus.CANCELED || a.getStatus() == AppointmentStatus.COMPLETED) {
                continue;
            }
            long minutesDiff = Math.abs(
                java.time.Duration.between(a.getDatetime(), dateTime).toMinutes()
            );
            if (minutesDiff < 15) {
                return false;
            }
        }
        return true;
    }

    // ===================== GESTIÓN DE HOSPITALIZACIONES =====================

    /**
     * Agrega una hospitalización y notifica a los observers.
     */
    public void addHospitalization(Hospitalization hospitalization) {
        hospitalizations.add(hospitalization);
        notifyObservers(EventType.HOSPITALIZATION_ADDED, hospitalization.serialize());
    }

    /**
     * Notifica que una hospitalización fue actualizada.
     */
    public void notifyHospitalizationUpdated(Hospitalization hospitalization) {
        notifyObservers(EventType.HOSPITALIZATION_UPDATED, hospitalization.serialize());
    }

    /**
     * Busca una hospitalización por su ID.
     */
    public Hospitalization findHospitalizationById(String id) {
        for (Hospitalization h : hospitalizations) {
            if (h.getId().equals(id)) {
                return h;
            }
        }
        return null;
    }

    /**
     * Retorna las hospitalizaciones de un doctor.
     */
    public List<Hospitalization> getHospitalizationsByDoctor(long doctorId) {
        List<Hospitalization> result = new ArrayList<>();
        User u = findUserById(doctorId);
        if (u instanceof Doctor) {
            Doctor d = (Doctor) u;
            result.addAll(d.getHospitalizations());
        }
        return result;
    }

    // ===================== GENERACIÓN DE IDs =====================

    /**
     * Genera un ID automático para citas con formato A-{id_paciente}-NNNN.
     */
    public String generateAppointmentId(long patientId) {
        int count = appointmentCounters.getOrDefault(patientId, 0);
        appointmentCounters.put(patientId, count + 1);
        return String.format("A-%d-%04d", patientId, count);
    }

    /**
     * Genera un ID automático para hospitalizaciones con formato H-{id_paciente}-NNNN.
     */
    public String generateHospitalizationId(long patientId) {
        int count = hospitalizationCounters.getOrDefault(patientId, 0);
        hospitalizationCounters.put(patientId, count + 1);
        return String.format("H-%d-%04d", patientId, count);
    }

    // ===================== OBSERVER =====================

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(EventType event, Object data) {
        for (Observer o : observers) {
            o.update(event, data);
        }
    }

    // ===================== ACCESO A LISTAS (solo lectura) =====================

    public List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public List<Appointment> getAppointments() {
        return new ArrayList<>(appointments);
    }

    public List<Hospitalization> getHospitalizations() {
        return new ArrayList<>(hospitalizations);
    }

    private Specialty parseSpecialty(String specStr) {
        if (specStr == null) return Specialty.GENERAL_MEDICINE;
        switch (specStr.toUpperCase()) {
            case "ORTHOPEDICS":
                return Specialty.TRAUMATOLOGY_ORTHOPEDICS;
            case "GYNECOLOGY":
                return Specialty.GYNECOLOGY_OBSTETRICS;
            default:
                try {
                    return Specialty.valueOf(specStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return Specialty.GENERAL_MEDICINE;
                }
        }
    }
}


