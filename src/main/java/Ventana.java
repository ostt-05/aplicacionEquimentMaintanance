import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Ventana {

    // 1. Datos de conexión (Cámbialos por los tuyos)
    private static final String URL = "jdbc:postgresql://localhost:5432/equipmentMaintanance";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    public static void main(String[] args) {
        // Probamos la conexión y las operaciones
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.println("✅ Conexión exitosa a PostgreSQL!");

            // Ejemplo de uso de las funciones:

            leerUsuarios(conn);

        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
        }
    }

    // --- C: CREATE (Agregar) ---

    // --- R: READ (Sacar datos de la tabla) ---
    private static void leerUsuarios(Connection conn) throws SQLException {
        String pene= "FROM";
        String sql = "SELECT * "+pene+ " personnel";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Lista de Usuarios ---");
            while (rs.next()) {
                // Asumiendo que tienes una columna 'id', 'nombre' y 'email'
                int id = rs.getInt("id_persona");
                String nombre = rs.getString("nombre");
                String email = rs.getString("apellido_materno");
                System.out.println(id + " | " + nombre + " | " + email);
            }
            System.out.println("-------------------------");
        }
    }

    // --- U: UPDATE (Editar) ---


    // --- D: DELETE (Eliminar) ---

}