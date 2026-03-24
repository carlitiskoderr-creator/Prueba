
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {
    private static final String URL = "jdbc:mysql://localhost:3306/JC?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City";
    private static final String USUARIO = "root";// tu usuario real de MySQL
    private static final String CONTRASENA = "3883382025"; // tu contraseña real

    public static Connection conectar() {
        try {
            Connection conn = DriverManager.getConnection(URL, USUARIO, CONTRASENA);
            System.out.println("Conexión exitosa a la base de datos");
            return conn;
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}
