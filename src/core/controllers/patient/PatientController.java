package core.controllers.patient;

import core.models.*;
import core.models.enums.*;
import core.controllers.utils.*;

import java.time.LocalDate;

import core.models.storage.IStorage;

/**
 * Controlador de pacientes.
 * Gestiona registro y actualización.
 * Todas las validaciones se realizan aquí, no en la vista.
 */
public class PatientController implements IPatientController {

    private final IStorage database;

    public PatientController(IStorage database) {
        this.database = database;
    }

    /**
     * Registra un nuevo paciente en el sistema.
     * Valida: ID (12 dígitos, único), email, teléfono (10 dígitos), fecha, contraseñas.
     */
    public Response registerPatient(String idStr, String username, String firstname, String lastname,
                                     String password, String confirmPassword, String email,
                                     String birthdateStr, boolean gender, String phoneStr, String address) {
        // Validar campos vacíos
        Response check;
        if ((check = Validator.checkRequired(firstname, "El nombre no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(lastname, "El apellido no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(username, "El nombre de usuario no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(password, "La contraseña no puede estar vacía.")) != null) return check;
        if ((check = Validator.checkEquals(password, confirmPassword, "Las contraseñas no coinciden.")) != null) return check;

        // Validar ID
        if ((check = Validator.validateId(idStr)) != null) return check;
        long id = Long.parseLong(idStr.trim());
        if (!database.isIdUnique(id))
            return new Response("Ya existe un usuario con ese ID.", Status.CONFLICT, null);

        // Validar username único
        if (!database.isUsernameUnique(username.trim()))
            return new Response("Ya existe un usuario con ese nombre de usuario.", Status.CONFLICT, null);

        // Validar teléfono: 10 dígitos
        if ((check = Validator.validateRegex(phoneStr, "^\\d{10}$", "El teléfono debe tener exactamente 10 dígitos.")) != null) return check;
        long phone = Long.parseLong(phoneStr.trim());

        // Validar email: formato XXXXX@XXXXX.com
        if ((check = Validator.validateRegex(email, "^[^@\\s]+@[^@\\s]+\\.com$", "El formato del email no es válido (ej: usuario@dominio.com).")) != null) return check;

        // Validar fecha de nacimiento
        if ((check = Validator.validateDate(birthdateStr, "Formato de fecha inválido. Use AAAA-MM-DD.")) != null) return check;
        LocalDate birthdate = LocalDate.parse(birthdateStr.trim());

        // Crear paciente
        Patient patient = new Patient(id, username.trim(), firstname.trim(), lastname.trim(),
                password, email.trim(), birthdate, gender, phone, address != null ? address.trim() : "");
        database.addUser(patient);

        return new Response("Paciente registrado exitosamente.", Status.OK, patient.serialize());
    }

    /**
     * Actualiza los datos de un paciente existente.
     * No permite cambiar el ID.
     */
    public Response updatePatient(long currentId, String username, String firstname, String lastname,
                                   String password, String confirmPassword, String email,
                                   String birthdateStr, boolean gender, String phoneStr, String address) {
        User user = database.findUserById(currentId);
        if (!(user instanceof Patient))
            return new Response("Paciente no encontrado.", Status.NOT_FOUND, null);

        Patient patient = (Patient) user;

        Response check;
        if ((check = Validator.checkRequired(firstname, "El nombre no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(lastname, "El apellido no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(username, "El nombre de usuario no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(password, "La contraseña no puede estar vacía.")) != null) return check;
        if ((check = Validator.checkEquals(password, confirmPassword, "Las contraseñas no coinciden.")) != null) return check;

        // Validar username único (excepto si es el mismo)
        if (!username.trim().equals(patient.getUsername()) && !database.isUsernameUnique(username.trim()))
            return new Response("Ya existe un usuario con ese nombre de usuario.", Status.CONFLICT, null);

        // Validar teléfono
        if ((check = Validator.validateRegex(phoneStr, "^\\d{10}$", "El teléfono debe tener exactamente 10 dígitos.")) != null) return check;
        long phone = Long.parseLong(phoneStr.trim());

        // Validar email
        if ((check = Validator.validateRegex(email, "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", "El formato del email no es válido.")) != null) return check;

        // Validar fecha
        if ((check = Validator.validateDate(birthdateStr, "Formato de fecha inválido. Use AAAA-MM-DD.")) != null) return check;
        LocalDate birthdate = LocalDate.parse(birthdateStr.trim());

        // Aplicar cambios
        patient.setUsername(username.trim());
        patient.setFirstname(firstname.trim());
        patient.setLastname(lastname.trim());
        patient.setPassword(password);
        patient.setEmail(email.trim());
        patient.setBirthdate(birthdate);
        patient.setGender(gender);
        patient.setPhone(phone);
        patient.setAddress(address != null ? address.trim() : "");

        database.notifyUserUpdated(patient);
        return new Response("Datos actualizados exitosamente.", Status.OK, patient.serialize());
    }

    /**
     * Retorna los datos serializados del paciente para cargar en la vista.
     * @param patientId ID del paciente
     * @return Response con Map<String,String> de datos del paciente, o 404 si no existe
     */
    public Response getPatientData(long patientId) {
        User user = database.findUserById(patientId);
        if (!(user instanceof Patient))
            return new Response("Paciente no encontrado.", Status.NOT_FOUND, null);
        return new Response("OK", Status.OK, user.serialize());
    }

    /**
     * Retorna la lista de doctores serializada para poblar ComboBox en la vista.
     * Cada elemento es un Map con id, firstname, lastname, specialty.
     * @return Response con List<Map<String,String>>
     */
    public Response getDoctorList() {
        java.util.List<Doctor> doctors = new java.util.ArrayList<>();
        for (User u : database.getUsers()) {
            if (u instanceof Doctor) {
                doctors.add((Doctor) u);
            }
        }
        return new Response("OK", Status.OK, Serializer.serializeList(doctors));
    }

    /**
     * Retorna la lista de doctores de una especialidad, para el ComboBox de citas por especialidad.
     * @param specialtyName nombre del enum Specialty
     * @return Response con List<Map<String,String>>
     */
    public Response getDoctorsBySpecialty(String specialtyName) {
        Specialty specialty;
        try {
            specialty = Specialty.valueOf(specialtyName.replaceAll(" & ", "_").replaceAll(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return new Response("Especialidad no valida.", Status.BAD_REQUEST, null);
        }
        return new Response("OK", Status.OK, Serializer.serializeList(database.findDoctorsBySpecialty(specialty)));
    }

    /**
     * Retorna las hospitalizaciones del paciente serializadas.
     * @param patientId ID del paciente
     * @return Response con List<Map<String,String>>
     */
    public Response getPatientHospitalizations(long patientId) {
        java.util.List<Hospitalization> patientHosps = new java.util.ArrayList<>();
        for (core.models.Hospitalization h : database.getHospitalizations()) {
            if (h.getPatient().getId() == patientId) {
                patientHosps.add(h);
            }
        }
        return new Response("OK", Status.OK, Serializer.serializeList(patientHosps));
    }

}
