
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DetallesPedidoDialog extends JDialog {

    private JTable tabla;
    private DefaultTableModel modelo;
    private Connection conexion;
    private int idPedido;

    public DetallesPedidoDialog(int idpedido, Connection conexion) {
        this.idPedido = idpedido;
        this.conexion = conexion;

        setTitle("Detalles del Pedido #" + idpedido);
        setSize(650, 400);
        setLocationRelativeTo(null);
        setModal(true);

        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(
                new String[] { "Producto", "Cantidad", "Precio Unit.", "Subtotal" },
                0);

        tabla = new JTable(modelo);
        tabla.setRowHeight(25);

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        cargarDetalles();
    }

    private void cargarDetalles() {
        modelo.setRowCount(0);

        String sql = """
                SELECT p.nombre_producto, d.cantidad, p.precio_unitario,
                       (d.cantidad * p.precio_unitario) AS subtotal
                FROM pedido_detalles d
                JOIN productos p ON p.id_producto = d.id_producto
                WHERE d.id_pedido = ?
                """;

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                modelo.addRow(new Object[] {
                        rs.getString("nombre_producto"),
                        rs.getInt("cantidad"),
                        rs.getDouble("precio_unitario"),
                        rs.getDouble("subtotal")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar detalles: " + e.getMessage());
        }
    }
}
