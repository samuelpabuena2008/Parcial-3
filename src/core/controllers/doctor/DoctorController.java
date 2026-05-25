package core.controllers.doctor;

import core.controllers.utils.Response;
import core.controllers.utils.Serializer;
import core.controllers.utils.Status;
import core.controllers.utils.Validator;
import core.models.Doctor;
import core.models.Patient;
import core.models.User;
import core.models.enums.Specialty;
import core.models.storage.IStorage;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de doctores.
 * Gestiona registro y actualización.
 * Todas las validaciones se realizan aquí, no en la vista.
 */
public class DoctorController implements IDoctorController {

    private final IStorage database;

    public DoctorController(IStorage database) {
        this.database = database;
    }

    /**
     * Registra un nuevo doctor en el sistema (solo puede ser llamado por un admin).
     * Valida: ID (12 dígitos, único), licencia (L-XXXXXXXXXX MTL), oficina (O-XXX).
     */
    public Response registerDoctor(String idStr, String username, String firstname, String lastname,
                                    String password, String confirmPassword, String specialtyStr,
                                    String licenceNumber, String assignedOffice) {
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

        // Validar licencia: formato L-XXXXXXXXXX MTL
        if ((check = Validator.validateRegex(licenceNumber, "^L-\\d{10} MTL$", "Formato de licencia inválido. Use: L-XXXXXXXXXX MTL (X = dígito).")) != null) return check;

        // Validar oficina: formato O-XXX
        if ((check = Validator.validateRegex(assignedOffice, "^O-\\d{3}$", "Formato de oficina inválido. Use: O-XXX (X = dígito).")) != null) return check;

        // Parsear especialidad
        Specialty specialty;
        try {
            specialty = Specialty.valueOf(specialtyStr.replaceAll(" & ", "_").replaceAll(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return new Response("Especialidad no válida.", Status.BAD_REQUEST, null);
        }

        Doctor doctor = new Doctor(id, username.trim(), firstname.trim(), lastname.trim(),
                password, specialty, licenceNumber.trim(), assignedOffice.trim());
        database.addUser(doctor);

        return new Response("Doctor registrado exitosamente.", Status.OK, doctor.serialize());
    }

    /**
     * Actualiza los datos de un doctor existente. No permite cambiar el ID.
     */
    public Response updateDoctor(long currentId, String username, String firstname, String lastname,
                                  String password, String confirmPassword, String specialtyStr,
                                  String licenceNumber, String assignedOffice) {
        User user = database.findUserById(currentId);
        if (!(user instanceof Doctor))
            return new Response("Doctor no encontrado.", Status.NOT_FOUND, null);

        Doctor doctor = (Doctor) user;

        Response check;
        if ((check = Validator.checkRequired(firstname, "El nombre no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(lastname, "El apellido no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(username, "El nombre de usuario no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(password, "La contraseña no puede estar vacía.")) != null) return check;
        if ((check = Validator.checkEquals(password, confirmPassword, "Las contraseñas no coinciden.")) != null) return check;

        if (!username.trim().equals(doctor.getUsername()) && !database.isUsernameUnique(username.trim()))
            return new Response("Ya existe un usuario con ese nombre de usuario.", Status.CONFLICT, null);

        if ((check = Validator.validateRegex(licenceNumber, "^L-\\d{10} MTL$", "Formato de licencia inválido. Use: L-XXXXXXXXXX MTL.")) != null) return check;
        if ((check = Validator.validateRegex(assignedOffice, "^O-\\d{3}$", "Formato de oficina inválido. Use: O-XXX.")) != null) return check;

        Specialty specialty;
        try {
            specialty = Specialty.valueOf(specialtyStr.replaceAll(" & ", "_").replaceAll(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return new Response("Especialidad no válida.", Status.BAD_REQUEST, null);
        }

        doctor.setUsername(username.trim());
        doctor.setFirstname(firstname.trim());
        doctor.setLastname(lastname.trim());
        doctor.setPassword(password);
        doctor.setSpecialty(specialty);
        doctor.setLicenceNumber(licenceNumber.trim());
        doctor.setAssignedOffice(assignedOffice.trim());

        database.notifyUserUpdated(doctor);
        return new Response("Datos del doctor actualizados exitosamente.", Status.OK, doctor.serialize());
    }

    /**
     * Retorna los datos serializados del doctor para cargar en la vista.
     * @param doctorId ID del doctor
     * @return Response con Map<String,String>, o 404
     */
    public Response getDoctorData(long doctorId) {
        User user = database.findUserById(doctorId);
        if (!(user instanceof Doctor))
            return new Response("Doctor no encontrado.", Status.NOT_FOUND, null);
        return new Response("OK", Status.OK, user.serialize());
    }

    /**
     * Retorna la lista de pacientes serializada para poblar ComboBox en DoctorView.
     * @return Response con List<Map<String,String>>
     */
    public Response getPatientList() {
        List<Patient> patients = new ArrayList<>();
        for (User u : database.getUsers()) {
            if (u instanceof Patient) {
                patients.add((Patient) u);
            }
        }
        return new Response("OK", Status.OK, Serializer.serializeList(patients));
    }

    /**
     * Retorna las hospitalizaciones del doctor serializadas.
     * @param doctorId ID del doctor
     * @return Response con List<Map<String,String>>
     */
    public Response getDoctorHospitalizations(long doctorId) {
        return new Response("OK", Status.OK, Serializer.serializeList(database.getHospitalizationsByDoctor(doctorId)));
    }

}
