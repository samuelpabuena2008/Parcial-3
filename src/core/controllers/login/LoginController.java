package core.controllers.login;

import core.controllers.utils.Response;
import core.controllers.utils.Status;
import core.controllers.utils.Validator;
import core.models.User;
import core.models.storage.IStorage;

/**
 * Controlador de autenticación.
 * Gestiona el login de usuarios validando credenciales contra Database.
 */
public class LoginController implements ILoginController {

    private final IStorage database;

    public LoginController(IStorage database) {
        this.database = database;
    }

    /**
     * Intenta autenticar a un usuario con su username y contraseña.
     *
     * @param username nombre de usuario ingresado
     * @param password contraseña ingresada
     * @return Response con código 200 y datos serializados si es exitoso,
     *         400 si faltan campos, 404 si no existe, 400 si contraseña incorrecta
     */
    public Response login(String username, String password) {
        Response check;
        if ((check = Validator.checkRequired(username, "El campo de usuario no puede estar vacío.")) != null) return check;
        if ((check = Validator.checkRequired(password, "El campo de contraseña no puede estar vacío.")) != null) return check;

        User user = database.findUserByUsername(username.trim());
        if (user == null) {
            return new Response("Usuario no encontrado.", Status.NOT_FOUND, null);
        }
        if (!user.getPassword().equals(password)) {
            return new Response("Contraseña incorrecta.", Status.BAD_REQUEST, null);
        }

        return new Response("Login exitoso.", Status.OK, user.serialize());
    }
}


