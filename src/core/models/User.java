package core.models;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Clase base abstracta para todos los usuarios del sistema.
 * Contiene los atributos comunes: id, username, nombre, apellido y contraseña.
 * 
 * @author edangulo
 */
public abstract class User {

    protected final long id;
    protected String username;
    protected String firstname;
    protected String lastname;
    protected String password;

    public User(long id, String username, String firstname, String lastname, String password) {
        this.id = id;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Serializa los atributos comunes del usuario a un Map de Strings.
     * Las subclases deben llamar a super.serialize() y agregar sus propios campos.
     */
    public Map<String, String> serialize() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(id));
        data.put("username", username);
        data.put("firstname", firstname);
        data.put("lastname", lastname);
        return data;
    }
}


