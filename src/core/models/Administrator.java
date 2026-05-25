package core.models;

import java.util.Map;

/**
 * Modelo de administrador del sistema.
 * El administrador puede realizar las mismas acciones que un paciente y un doctor.
 * 
 * @author edangulo
 */
public class Administrator extends User {

    public Administrator(long id, String username, String firstname, String lastname, String password) {
        super(id, username, firstname, lastname, password);
    }

    @Override
    public Map<String, String> serialize() {
        Map<String, String> data = super.serialize();
        data.put("type", "admin");
        return data;
    }
}


