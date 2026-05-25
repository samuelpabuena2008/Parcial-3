package core;

import com.formdev.flatlaf.FlatDarkLaf;
import core.models.storage.Database;
import core.models.storage.IStorage;
import core.views.LoginView;

/**
 * Punto de entrada único de la aplicación Ospedale.
 * Inicializa el look and feel, carga los datos desde JSON y lanza la vista de login.
 */
public class Main {

    public static void main(String[] args) {
        // Configurar FlatLaf Dark como Look and Feel
        FlatDarkLaf.setup();

        // Cargar datos iniciales desde JSON
        IStorage database = Database.getInstance();
        database.loadFromJSON("json/users.json");

        // Lanzar la vista de login
        java.awt.EventQueue.invokeLater(() -> {
            new LoginView(database).setVisible(true);
        });
    }
}


