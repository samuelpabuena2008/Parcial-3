package core.controllers.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Clase utilitaria para serializar colecciones de entidades.
 * Utiliza reflexión para evitar un acoplamiento rígido con modelos específicos
 * y permitir serializar cualquier clase que contenga un método 'serialize()'.
 */
public class Serializer {

    /**
     * Convierte una lista de objetos serializables en una lista de mapas.
     *
     * @param items Lista de objetos a serializar
     * @return Lista de mapas serializados con formato clave-valor
     */
    public static List<Map<String, String>> serializeList(List<?> items) {
        List<Map<String, String>> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (Object item : items) {
            if (item == null) {
                continue;
            }
            try {
                java.lang.reflect.Method method = item.getClass().getMethod("serialize");
                @SuppressWarnings("unchecked")
                Map<String, String> serialized = (Map<String, String>) method.invoke(item);
                result.add(serialized);
            } catch (Exception e) {
                throw new RuntimeException("Error al serializar el objeto de tipo " + item.getClass().getName(), e);
            }
        }
        return result;
    }
}
