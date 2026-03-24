
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

public class NuevoPedidoDialog extends JDialog {

    private Connection conexion;

    // --- Modelos y tablas ---
    private DefaultTableModel modeloTabla;
    private DefaultTableModel modeloSeleccionados;
    private JTable tablaProductos;
    private JTable tablaSeleccionados;

    // --- Controles ---
    private JTextField txtBuscar;
    private JTextField txtCliente;
    private JComboBox<String> comboCategoria;

    // --- Datos temporales ---
    private java.util.List<Object[]> productosSeleccionados = new ArrayList<>();

    public NuevoPedidoDialog(JFrame parent, Connection conexion) {
        super(parent);
        this.conexion = conexion;

        setTitle("Nuevo Pedido");
        setSize(800, 500);
        setModal(true);
        setLocationRelativeTo(parent);
        setLocation(getX() + 90, getY()); // Desplazar 100 píxeles a la derecha
        setLayout(new BorderLayout());

        agregarComponentes();
        cargarProductos("");
    }

    // --------------------------------------------------------
    // PANELES PRINCIPALES
    // --------------------------------------------------------
    private void agregarComponentes() {

        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTop.add(new JLabel("Cliente:"));
        txtCliente = new JTextField(20);
        panelTop.add(txtCliente);

        comboCategoria = new JComboBox<>(new String[] {
                "agrícola", "consumibles", "desechables", "limpieza"
        });
        panelTop.add(new JLabel("Categoría:"));
        panelTop.add(comboCategoria);

        add(panelTop, BorderLayout.NORTH);

        // -------------------------------
        // Panel centro (Dos tablas)
        // -------------------------------
        JPanel panelCentro = new JPanel(new GridLayout(1, 2));

        // TABLA IZQUIERDA (Productos)
        modeloTabla = new DefaultTableModel(
                new String[] { "ID", "Producto", "Descripción", "Stock", "Precio" }, 0);
        tablaProductos = new JTable(modeloTabla);
        tablaProductos.setRowHeight(25);

        // Buscar
        JPanel panelBuscar = new JPanel(new BorderLayout());
        txtBuscar = new JTextField();
        panelBuscar.add(new JLabel("Buscar: "), BorderLayout.WEST);
        panelBuscar.add(txtBuscar, BorderLayout.CENTER);

        JPanel panelIzquierda = new JPanel(new BorderLayout());
        panelIzquierda.add(panelBuscar, BorderLayout.NORTH);
        panelIzquierda.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);

        panelCentro.add(panelIzquierda);

        // TABLA DERECHA (Seleccionados)
        modeloSeleccionados = new DefaultTableModel(
                new String[] { "ID", "Producto", "Cantidad", "P. Unit", "Subtotal" }, 0);
        tablaSeleccionados = new JTable(modeloSeleccionados);
        tablaSeleccionados.setRowHeight(25);

        panelCentro.add(new JScrollPane(tablaSeleccionados));

        add(panelCentro, BorderLayout.CENTER);

        // ----------------------------------------------------
        // Panel inferior (Botones)
        // ----------------------------------------------------
        JPanel panelBotones = new JPanel();

        JButton btnAgregar = new JButton("Agregar Producto");
        JButton btnQuitar = new JButton("Quitar");
        JButton btnGuardar = new JButton("Guardar Pedido");
        JButton btnCancelar = new JButton("Cancelar");

        panelBotones.add(btnAgregar);
        panelBotones.add(btnQuitar);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        add(panelBotones, BorderLayout.SOUTH);

        // EVENTOS
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                buscarCoincidencias();
            }

            public void removeUpdate(DocumentEvent e) {
                buscarCoincidencias();
            }

            public void changedUpdate(DocumentEvent e) {
                buscarCoincidencias();
            }
        });

        btnAgregar.addActionListener(e -> agregarProducto());
        btnQuitar.addActionListener(e -> quitarProducto());
        btnGuardar.addActionListener(e -> guardarPedido());
        btnCancelar.addActionListener(e -> dispose());
    }

    // --------------------------------------------------------
    // BUSCAR
    // --------------------------------------------------------
    private void buscarCoincidencias() {
        cargarProductos(txtBuscar.getText().trim());
    }

    // --------------------------------------------------------
    // CARGAR PRODUCTOS
    // --------------------------------------------------------
    private void cargarProductos(String filtro) {

        modeloTabla.setRowCount(0);

        String categoria = comboCategoria.getSelectedItem().toString();

        String tablaSQL = "";
        switch (categoria) {
            case "agrícola":
                tablaSQL = "InventarioAgricolas";
                break;
            case "consumibles":
                tablaSQL = "InventarioConsumibles";
                break;
            case "desechables":
                tablaSQL = "InventarioDesechables";
                break;
            case "limpieza":
                tablaSQL = "InventarioLimpieza";
                break;
        }

        String sql = "SELECT id_producto, nombre_producto, descripcion, cantidad, precio_unitario "
                + "FROM " + tablaSQL
                + " WHERE nombre_producto LIKE ? OR descripcion LIKE ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {

            String f = "%" + filtro + "%";
            ps.setString(1, f);
            ps.setString(2, f);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                modeloTabla.addRow(new Object[] {
                        rs.getInt("id_producto"),
                        rs.getString("nombre_producto"),
                        rs.getString("descripcion"),
                        rs.getInt("cantidad"),
                        rs.getDouble("precio_unitario")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar productos: " + e.getMessage());
        }
    }

    // --------------------------------------------------------
    // AGREGAR PRODUCTO
    // --------------------------------------------------------
    private void agregarProducto() {

        int fila = tablaProductos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto");
            return;
        }

        int id = (int) modeloTabla.getValueAt(fila, 0);
        String nombre = (String) modeloTabla.getValueAt(fila, 1);
        int stock = (int) modeloTabla.getValueAt(fila, 3);
        double precio = (double) modeloTabla.getValueAt(fila, 4);

        String cantidadStr = JOptionPane.showInputDialog(this,
                "Cantidad (stock: " + stock + "):");

        if (cantidadStr == null)
            return;

        int cantidad = Integer.parseInt(cantidadStr);

        if (cantidad > stock || cantidad <= 0) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }

        double subtotal = cantidad * precio;

        modeloSeleccionados.addRow(new Object[] {
                id, nombre, cantidad, precio, subtotal
        });
    }

    // --------------------------------------------------------
    // QUITAR PRODUCTO
    // --------------------------------------------------------
    private void quitarProducto() {
        int fila = tablaSeleccionados.getSelectedRow();
        if (fila != -1)
            modeloSeleccionados.removeRow(fila);
    }

    // --------------------------------------------------------
    // GUARDAR PEDIDO
    // --------------------------------------------------------
    private void guardarPedido() {
        try {

            if (txtCliente.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese el nombre del cliente");
                return;
            }

            if (modeloSeleccionados.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No hay productos en el pedido");
                return;
            }

            // Insertar pedido principal
            String sqlPedido = "INSERT INTO pedidos (nombre_cliente, fecha, estado, saldo_pendiente) "
                    + "VALUES (?, NOW(), 'Pendiente', ?)";

            double total = calcularTotal();

            PreparedStatement psPedido = conexion.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS);
            psPedido.setString(1, txtCliente.getText().trim());
            psPedido.setDouble(2, total);
            psPedido.executeUpdate();

            ResultSet rs = psPedido.getGeneratedKeys();
            rs.next();
            int idPedido = rs.getInt(1);

            // Insertar detalles
            for (int i = 0; i < modeloSeleccionados.getRowCount(); i++) {

                int idProd = (int) modeloSeleccionados.getValueAt(i, 0);
                int cant = (int) modeloSeleccionados.getValueAt(i, 2);
                double precioU = (double) modeloSeleccionados.getValueAt(i, 3);

                String categoria = comboCategoria.getSelectedItem().toString();

                PreparedStatement psDetalle = conexion.prepareStatement(
                        "INSERT INTO pedido_detalles (id_pedido, id_producto, categoria, cantidad, precio_unitario) "
                                + "VALUES (?, ?, ?, ?, ?)");

                psDetalle.setInt(1, idPedido);
                psDetalle.setInt(2, idProd);
                psDetalle.setString(3, categoria);
                psDetalle.setInt(4, cant);
                psDetalle.setDouble(5, precioU);

                psDetalle.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Pedido guardado. ID: " + idPedido);
            dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar pedido: " + e.getMessage());
        }
    }

    private double calcularTotal() {
        double total = 0;
        for (int i = 0; i < modeloSeleccionados.getRowCount(); i++) {
            total += (double) modeloSeleccionados.getValueAt(i, 4);
        }
        return total;
    }
}
