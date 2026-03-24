
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class VentanaDueño extends JFrame {
    private Connection conexion;
    private CardLayout cardLayout;
    private JPanel panelContenido;

    private final Color COLOR_PRIMARIO = PaletaColores.TERTIARY;
    private final Color COLOR_SECUNDARIO = PaletaColores.SECONDARY;
    private final Color COLOR_TERCIARIO = PaletaColores.PRIMARY;
    private final Color COLOR_ACENTO = PaletaColores.ACCENT;
    private final Color COLOR_CARD = PaletaColores.BACKGROUND.brighter();
    private final Color GRIS = Color.lightGray;

    // Variables para Pedidos
    private JComboBox<String> comboTipoInventario, comboProductos, comboProveedores, comboTipoPedidosExistentes;
    private JTextField txtCantidadPedido;
    private DefaultTableModel modeloPedido, modeloPedidosExistentes;
    private JTable tablaPedido, tablaPedidosExistentes;
    private JLabel lblTotalPedido;
    private java.util.Map<String, Integer> mapaProductos = new java.util.HashMap<>();
    private java.util.Map<String, Integer> mapaProveedores = new java.util.HashMap<>();

    // Variables para Ventas e Historial
    private DefaultTableModel modeloVentas, modeloHistorial;
    private JTable tablaVentas, tablaHistorial;

    // Variables para Consultar Datos
    private JComboBox<String> comboTablas;
    private DefaultTableModel modeloTablas;
    private JTable tablaConsulta;
    private JTextField txtBusqueda;
    private JPopupMenu popupSugerenciasDueño;
    private JList<String> listaSugerenciasDueño;
    private DefaultListModel<String> modeloSugerenciasDueño;
    private java.util.List<Object[]> registrosEncontradosDueño;
    private String ultimaColumnaClaveBusqueda;

    // Variables para estadísticas
    private JLabel lblVentasHoy, lblTotalProductos, lblEmpleadosActivos, lblStockBajo;

    public VentanaDueño() {
        setTitle("Panel de Administración - Dueño");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);

        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(Color.DARK_GRAY);
        setContentPane(panelPrincipal);

        panelPrincipal.add(crearPanelLateral(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        panelContenido = new JPanel(cardLayout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setPaint(PaletaColores.gradientFor(getWidth(), getHeight()));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        panelContenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        conexion = ConexionBD.conectar();
        if (conexion == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos");
            return;
        }

        panelContenido.add(crearVistaConsultarDatos(), "consultar");
        panelContenido.add(crearVistaPedidos(), "pedido");
        panelContenido.add(crearVistaVentas(), "ventas");
        panelContenido.add(crearVistaHistorial(), "servicios");

        panelPrincipal.add(panelContenido, BorderLayout.CENTER);
        cargarEstadisticas();
        cardLayout.show(panelContenido, "consultar");
    }

    private JPanel crearPanelLateral() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 8, 8));
        panel.setBackground(PaletaColores.BACKGROUND);
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JButton btnConsultar = crearBotonLateral("Consultar Datos", COLOR_PRIMARIO);
        JButton btnPedido = crearBotonLateral("Agregar Pedido", COLOR_SECUNDARIO);
        JButton btnVentas = crearBotonLateral("Historial de Ventas", COLOR_TERCIARIO);
        JButton btnServicios = crearBotonLateral("Historial de Compras", COLOR_PRIMARIO);
        JButton btnCerrar = crearBotonLateral("Cerrar Sesión", COLOR_ACENTO);

        panel.add(btnConsultar);
        panel.add(btnPedido);
        panel.add(btnVentas);
        panel.add(btnServicios);
        panel.add(btnCerrar);

        btnConsultar.addActionListener(e -> {
            cargarEstadisticas();
            cardLayout.show(panelContenido, "consultar");
        });
        btnPedido.addActionListener(e -> {
            cargarPedidosExistentes();
            cardLayout.show(panelContenido, "pedido");
        });
        btnVentas.addActionListener(e -> {
            cargarVentas();
            cardLayout.show(panelContenido, "ventas");
        });
        btnServicios.addActionListener(e -> {
            cargarHistorial();
            cardLayout.show(panelContenido, "servicios");
        });
        btnCerrar.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "¿Deseas cerrar sesión?", "Confirmar",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                dispose();
                new prueba().setVisible(true);
            }
        });
        return panel;
    }

    private JButton crearBotonLateral(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(color);
        btn.setForeground(isLight(color) ? Color.BLACK : Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 5, 12, 5));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private JButton crearBotonAccion(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private boolean isLight(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 128;
    }

    private void aplicarEstiloTabla(JTable tabla) {
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.setRowHeight(25);
        tabla.setGridColor(new Color(230, 230, 230));
        tabla.setShowGrid(true);
        tabla.setSelectionBackground(new Color(184, 207, 229));
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) {
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                    comp.setForeground(Color.BLACK);
                }
                return comp;
            }
        });
        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(240, 240, 240));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    private void ocultarColumna(JTable tabla, int col) {
        tabla.getColumnModel().getColumn(col).setMinWidth(0);
        tabla.getColumnModel().getColumn(col).setMaxWidth(0);
        tabla.getColumnModel().getColumn(col).setPreferredWidth(0);
    }

    // ==================== VISTA PEDIDOS ====================
    private JPanel crearVistaPedidos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // PANEL SUPERIOR:
        JPanel panelSuperior = new JPanel(new BorderLayout(0, 8));
        panelSuperior.setOpaque(false);

        // Formulario de entrada
        JPanel panelEntrada = new JPanel(new GridBagLayout());
        panelEntrada.setBackground(Color.WHITE);
        panelEntrada.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_TERCIARIO, 2),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panelEntrada.add(new JLabel("Tipo:") {
            {
                setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
        }, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        comboTipoInventario = new JComboBox<>(new String[] { "Consumibles", "Desechables", "Limpieza", "Agricolas" });
        comboTipoInventario.addActionListener(e -> cargarProductosPorTipo());
        panelEntrada.add(comboTipoInventario, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panelEntrada.add(new JLabel("Producto:") {
            {
                setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
        }, gbc);
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        comboProductos = new JComboBox<>();
        panelEntrada.add(comboProductos, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton btnAgregar = crearBotonAccion("Agregar", COLOR_SECUNDARIO);
        btnAgregar.addActionListener(e -> agregarPedidoALista());
        panelEntrada.add(btnAgregar, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panelEntrada.add(new JLabel("Proveedor:") {
            {
                setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
        }, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        comboProveedores = new JComboBox<>();
        panelEntrada.add(comboProveedores, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panelEntrada.add(new JLabel("Cantidad:") {
            {
                setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
        }, gbc);
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        txtCantidadPedido = new JTextField("1");
        panelEntrada.add(txtCantidadPedido, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton btnGuardar = crearBotonAccion("Guardar Pedidos", COLOR_TERCIARIO);
        btnGuardar.addActionListener(e -> guardarPedidosEnBD());
        panelEntrada.add(btnGuardar, gbc);

        cargarProveedoresPedido();
        cargarProductosPorTipo();
        panelSuperior.add(panelEntrada, BorderLayout.NORTH);

        // Header tabla nuevos pedidos
        JPanel panelHeaderNuevos = new JPanel(new BorderLayout());
        panelHeaderNuevos.setOpaque(false);
        JLabel lblLista = new JLabel("  Pedidos a registrar:");
        lblLista.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLista.setForeground(Color.WHITE);

        lblTotalPedido = new JLabel("En lista: 0  ");
        lblTotalPedido.setForeground(Color.WHITE);
        lblTotalPedido.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel panelBotonesNuevos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        panelBotonesNuevos.setOpaque(false);
        JButton btnEliminar = crearBotonAccion("Eliminar", COLOR_ACENTO);
        JButton btnLimpiar = crearBotonAccion("Limpiar", COLOR_PRIMARIO);
        btnEliminar.addActionListener(e -> {
            int f = tablaPedido.getSelectedRow();
            if (f >= 0) {
                modeloPedido.removeRow(f);
                lblTotalPedido.setText("En lista: " + modeloPedido.getRowCount() + "  ");
            }
        });
        btnLimpiar.addActionListener(e -> {
            modeloPedido.setRowCount(0);
            lblTotalPedido.setText("En lista: 0  ");
        });
        panelBotonesNuevos.add(lblTotalPedido);
        panelBotonesNuevos.add(btnEliminar);
        panelBotonesNuevos.add(btnLimpiar);

        panelHeaderNuevos.add(lblLista, BorderLayout.WEST);
        panelHeaderNuevos.add(panelBotonesNuevos, BorderLayout.EAST);

        // Tabla nuevos pedidos
        modeloPedido = new DefaultTableModel(
                new String[] { "Tipo", "IDProd", "Producto", "IDProv", "Proveedor", "Cantidad", "Estado" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaPedido = new JTable(modeloPedido);
        aplicarEstiloTabla(tablaPedido);
        ocultarColumna(tablaPedido, 1);
        ocultarColumna(tablaPedido, 3);

        JPanel panelTablaNuevos = new JPanel(new BorderLayout(0, 4));
        panelTablaNuevos.setOpaque(false);
        panelTablaNuevos.add(panelHeaderNuevos, BorderLayout.NORTH);
        JScrollPane scrollNuevos = new JScrollPane(tablaPedido);
        scrollNuevos.setPreferredSize(new Dimension(0, 130));
        panelTablaNuevos.add(scrollNuevos, BorderLayout.CENTER);

        panelSuperior.add(panelTablaNuevos, BorderLayout.CENTER);
        panel.add(panelSuperior, BorderLayout.NORTH);

        // PANEL INFERIOR:
        JPanel panelInferior = new JPanel(new BorderLayout(0, 4));
        panelInferior.setOpaque(false);

        // Header pedidos existentes
        JPanel panelHeaderExistentes = new JPanel(new BorderLayout());
        panelHeaderExistentes.setOpaque(false);

        JPanel panelFiltroIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelFiltroIzq.setOpaque(false);
        JLabel lblExistentes = new JLabel("Pedidos registrados:");
        lblExistentes.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblExistentes.setForeground(Color.WHITE);
        comboTipoPedidosExistentes = new JComboBox<>(
                new String[] { "Consumibles", "Desechables", "Limpieza", "Agricolas" });
        comboTipoPedidosExistentes.addActionListener(e -> cargarPedidosExistentes());
        panelFiltroIzq.add(lblExistentes);
        panelFiltroIzq.add(comboTipoPedidosExistentes);

        JPanel panelFiltroDer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelFiltroDer.setOpaque(false);
        JLabel lblLeyenda = new JLabel(
                "<html><span style='color:#ff6666;'>● Pendiente</span>  <span style='color:#ffcc00;'>● En tránsito</span>  <span style='color:#66cc66;'>● Entregado</span></html>");
        lblLeyenda.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JButton btnCambiarEstado = crearBotonAccion("Cambiar Estado", COLOR_PRIMARIO);
        btnCambiarEstado.addActionListener(e -> cambiarEstadoPedido());
        JButton btnRefrescar = crearBotonAccion("Refrescar", COLOR_SECUNDARIO);
        btnRefrescar.addActionListener(e -> cargarPedidosExistentes());
        panelFiltroDer.add(lblLeyenda);
        panelFiltroDer.add(btnCambiarEstado);
        panelFiltroDer.add(btnRefrescar);

        panelHeaderExistentes.add(panelFiltroIzq, BorderLayout.WEST);
        panelHeaderExistentes.add(panelFiltroDer, BorderLayout.EAST);
        panelInferior.add(panelHeaderExistentes, BorderLayout.NORTH);

        // Tabla pedidos existentes
        modeloPedidosExistentes = new DefaultTableModel(
                new String[] { "ID", "Producto", "Proveedor", "Cantidad", "Fecha", "Estado" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaPedidosExistentes = new JTable(modeloPedidosExistentes);
        aplicarEstiloTabla(tablaPedidosExistentes);
        tablaPedidosExistentes.getColumnModel().getColumn(0).setPreferredWidth(50);
        tablaPedidosExistentes.getColumnModel().getColumn(3).setPreferredWidth(60);

        tablaPedidosExistentes.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) {
                    String estado = (String) t.getValueAt(r, 5);
                    if ("Pendiente".equalsIgnoreCase(estado))
                        comp.setBackground(new Color(255, 220, 220));
                    else if ("Entregado".equalsIgnoreCase(estado))
                        comp.setBackground(new Color(210, 255, 210));
                    else if ("En tránsito".equalsIgnoreCase(estado))
                        comp.setBackground(new Color(255, 250, 200));
                    else
                        comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                    comp.setForeground(Color.BLACK);
                }
                return comp;
            }
        });

        panelInferior.add(new JScrollPane(tablaPedidosExistentes), BorderLayout.CENTER);
        panel.add(panelInferior, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::cargarPedidosExistentes);
        return panel;
    }

    private void cargarProveedoresPedido() {
        comboProveedores.removeAllItems();
        mapaProveedores.clear();
        try (Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id_proveedor, nombre_proveedor FROM Proveedores ORDER BY nombre_proveedor")) {
            while (rs.next()) {
                comboProveedores.addItem(rs.getString("nombre_proveedor"));
                mapaProveedores.put(rs.getString("nombre_proveedor"), rs.getInt("id_proveedor"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarProductosPorTipo() {
        if (comboProductos == null)
            return;
        comboProductos.removeAllItems();
        mapaProductos.clear();
        String tipo = (String) comboTipoInventario.getSelectedItem();
        if (tipo == null)
            return;
        try (Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id_producto, nombre_producto FROM Inventario" + tipo + " ORDER BY nombre_producto")) {
            while (rs.next()) {
                comboProductos.addItem(rs.getString("nombre_producto"));
                mapaProductos.put(rs.getString("nombre_producto"), rs.getInt("id_producto"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void agregarPedidoALista() {
        String tipo = (String) comboTipoInventario.getSelectedItem();
        String producto = (String) comboProductos.getSelectedItem();
        String proveedor = (String) comboProveedores.getSelectedItem();
        String cantStr = txtCantidadPedido.getText().trim();
        if (tipo == null || producto == null || proveedor == null || cantStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa todos los campos");
            return;
        }
        try {
            int cant = Integer.parseInt(cantStr);
            if (cant <= 0) {
                JOptionPane.showMessageDialog(this, "Cantidad debe ser mayor a 0");
                return;
            }
            modeloPedido.addRow(new Object[] { tipo, mapaProductos.get(producto), producto,
                    mapaProveedores.get(proveedor), proveedor, cant, "Pendiente" });
            lblTotalPedido.setText("En lista: " + modeloPedido.getRowCount() + "  ");
            txtCantidadPedido.setText("1");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida");
        }
    }

    private void guardarPedidosEnBD() {
        if (modeloPedido.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay pedidos en la lista");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Guardar " + modeloPedido.getRowCount() + " pedido(s)?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        int guardados = 0;
        for (int i = 0; i < modeloPedido.getRowCount(); i++) {
            String tipo = (String) modeloPedido.getValueAt(i, 0);
            int idProd = (int) modeloPedido.getValueAt(i, 1), idProv = (int) modeloPedido.getValueAt(i, 3),
                    cant = (int) modeloPedido.getValueAt(i, 5);
            String nomProv = (String) modeloPedido.getValueAt(i, 4);
            try (PreparedStatement ps = conexion.prepareStatement("INSERT INTO Pedido" + tipo
                    + " (id_producto, id_proveedor, nombre_proveedor, cantidad, fecha_solicitud, estado) VALUES (?, ?, ?, ?, CURDATE(), 'Pendiente')")) {
                ps.setInt(1, idProd);
                ps.setInt(2, idProv);
                ps.setString(3, nomProv);
                ps.setInt(4, cant);
                ps.executeUpdate();
                guardados++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        JOptionPane.showMessageDialog(this, "Se guardaron " + guardados + " pedido(s)");
        modeloPedido.setRowCount(0);
        lblTotalPedido.setText("En lista: 0  ");
        cargarPedidosExistentes();
    }

    private void cargarPedidosExistentes() {
        if (modeloPedidosExistentes == null)
            return;
        modeloPedidosExistentes.setRowCount(0);
        String tipo = (String) comboTipoPedidosExistentes.getSelectedItem();
        if (tipo == null)
            return;
        try (Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT p.id_pedido, i.nombre_producto, p.nombre_proveedor, p.cantidad, p.fecha_solicitud, p.estado FROM Pedido"
                                + tipo + " p JOIN Inventario" + tipo
                                + " i ON p.id_producto = i.id_producto ORDER BY FIELD(p.estado, 'Pendiente', 'En tránsito', 'Entregado'), p.fecha_solicitud DESC LIMIT 100")) {
            while (rs.next())
                modeloPedidosExistentes.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), rs.getDate(5), rs.getString(6) });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cambiarEstadoPedido() {
        int fila = tablaPedidosExistentes.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un pedido");
            return;
        }
        int id = (int) modeloPedidosExistentes.getValueAt(fila, 0);
        String estadoActual = (String) modeloPedidosExistentes.getValueAt(fila, 5);
        String tipo = (String) comboTipoPedidosExistentes.getSelectedItem();
        String nuevoEstado = (String) JOptionPane.showInputDialog(this,
                "Estado actual: " + estadoActual + "\nNuevo estado:", "Cambiar Estado #" + id,
                JOptionPane.QUESTION_MESSAGE, null, new String[] { "Pendiente", "En tránsito", "Entregado" },
                estadoActual);
        if (nuevoEstado == null || nuevoEstado.equals(estadoActual))
            return;
        try (PreparedStatement ps = conexion
                .prepareStatement("UPDATE Pedido" + tipo + " SET estado = ? WHERE id_pedido = ?")) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, id);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this, "Estado actualizado");
                cargarPedidosExistentes();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ==================== VISTA CONSULTAR DATOS ====================
    private JPanel crearVistaConsultarDatos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelControles.setOpaque(false);
        JLabel lblTabla = new JLabel("Tabla:");
        lblTabla.setForeground(Color.WHITE);
        lblTabla.setFont(new Font("Segoe UI", Font.BOLD, 13));

        comboTablas = new JComboBox<>(new String[] { "Empleados", "Clientes", "Proveedores", "InventarioConsumibles",
                "InventarioDesechables", "InventarioLimpieza", "InventarioAgricolas" });
        comboTablas.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboTablas.setPreferredSize(new Dimension(200, 35));
        comboTablas.addActionListener(e -> {
            cargarEstadisticas();
            cargarTabla((String) comboTablas.getSelectedItem());
            txtBusqueda.setText("");
        });

        JButton btnActualizar = crearBotonAccion("Actualizar", COLOR_TERCIARIO);
        btnActualizar.addActionListener(e -> {
            cargarEstadisticas();
            cargarTabla((String) comboTablas.getSelectedItem());
            txtBusqueda.setText("");
        });

        panelControles.add(lblTabla);
        panelControles.add(comboTablas);
        panelControles.add(btnActualizar);
        panelSuperior.add(panelControles, BorderLayout.EAST);
        panel.add(panelSuperior, BorderLayout.NORTH);

        JPanel panelCentral = new JPanel(new BorderLayout(0, 8));
        panelCentral.setOpaque(false);
        panelCentral.add(crearPanelEstadisticas(), BorderLayout.NORTH);

        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelBusqueda.setBackground(Color.WHITE);
        panelBusqueda.setBorder(
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                        BorderFactory.createEmptyBorder(2, 5, 2, 5)));

        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtBusqueda = new JTextField(35);
        txtBusqueda.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBusqueda.setPreferredSize(new Dimension(350, 30));

        JButton btnEditar = crearBotonAccion("Editar Registro", COLOR_ACENTO);
        JButton btnGuardar = crearBotonAccion("Guardar Cambios", COLOR_TERCIARIO);
        JButton btnCancelar = crearBotonAccion("Cancelar", COLOR_SECUNDARIO);
        JButton btnBorrar = crearBotonAccion("Borrar Registro", new Color(200, 50, 50));

        panelBusqueda.add(lblBuscar);
        panelBusqueda.add(txtBusqueda);
        panelBusqueda.add(btnEditar);
        panelBusqueda.add(btnGuardar);
        panelBusqueda.add(btnCancelar);
        panelBusqueda.add(btnBorrar);

        JPanel panelDatos = new JPanel(new BorderLayout(0, 5));
        panelDatos.setOpaque(false);
        panelDatos.add(panelBusqueda, BorderLayout.NORTH);

        modeloTablas = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaConsulta = new JTable(modeloTablas);
        aplicarEstiloTabla(tablaConsulta);
        panelDatos.add(new JScrollPane(tablaConsulta), BorderLayout.CENTER);

        panelCentral.add(panelDatos, BorderLayout.CENTER);
        panel.add(panelCentral, BorderLayout.CENTER);

        configurarEventosBusqueda();
        btnEditar.addActionListener(e -> habilitarEdicion());
        btnGuardar.addActionListener(e -> guardarCambios());
        btnCancelar.addActionListener(e -> cancelarEdicion());
        btnBorrar.addActionListener(e -> borrarRegistro());

        cargarTabla("Empleados");
        return panel;
    }

    private void configurarEventosBusqueda() {
        popupSugerenciasDueño = new JPopupMenu();
        modeloSugerenciasDueño = new DefaultListModel<>();
        listaSugerenciasDueño = new JList<>(modeloSugerenciasDueño);
        listaSugerenciasDueño.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        registrosEncontradosDueño = new java.util.ArrayList<>();

        txtBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                buscarEnTabla();
            }

            public void removeUpdate(DocumentEvent e) {
                buscarEnTabla();
            }

            public void changedUpdate(DocumentEvent e) {
                buscarEnTabla();
            }
        });

        txtBusqueda.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && popupSugerenciasDueño.isVisible()) {
                    int idx = listaSugerenciasDueño.getSelectedIndex();
                    if (idx < modeloSugerenciasDueño.size() - 1)
                        listaSugerenciasDueño.setSelectedIndex(idx + 1);
                } else if (e.getKeyCode() == KeyEvent.VK_UP && popupSugerenciasDueño.isVisible()) {
                    int idx = listaSugerenciasDueño.getSelectedIndex();
                    if (idx > 0)
                        listaSugerenciasDueño.setSelectedIndex(idx - 1);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && popupSugerenciasDueño.isVisible())
                    seleccionarSugerenciaDueño();
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                    popupSugerenciasDueño.setVisible(false);
            }
        });
        listaSugerenciasDueño.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    seleccionarSugerenciaDueño();
            }
        });
    }

    private void buscarEnTabla() {
        String texto = txtBusqueda.getText().trim();
        String tabla = (String) comboTablas.getSelectedItem();
        if (texto.isEmpty()) {
            popupSugerenciasDueño.setVisible(false);
            cargarTabla(tabla);
            return;
        }
        modeloSugerenciasDueño.clear();
        registrosEncontradosDueño.clear();

        try (Statement stmt = conexion.createStatement();
                ResultSet rsTest = stmt.executeQuery("SELECT * FROM " + tabla + " LIMIT 1")) {
            ResultSetMetaData meta = rsTest.getMetaData();
            int cols = meta.getColumnCount();
            String primeraCol = meta.getColumnName(1);
            ultimaColumnaClaveBusqueda = primeraCol;

            java.util.List<String> colsNombre = new java.util.ArrayList<>();
            for (int i = 1; i <= cols; i++) {
                String col = meta.getColumnName(i).toLowerCase();
                if (col.contains("nombre") || col.contains("desc"))
                    colsNombre.add(meta.getColumnName(i));
            }

            if (texto.matches("\\d+")) {
                cargarFilasPorPrefijo(tabla, primeraCol, texto);
                return;
            }
            if (texto.length() < 2) {
                popupSugerenciasDueño.setVisible(false);
                return;
            }

            java.util.List<String> colsBusq = colsNombre.isEmpty() ? java.util.Arrays.asList(primeraCol) : colsNombre;
            StringBuilder where = new StringBuilder();
            for (int i = 0; i < colsBusq.size(); i++) {
                if (i > 0)
                    where.append(" OR ");
                where.append(colsBusq.get(i)).append(" LIKE ?");
            }

            try (PreparedStatement ps = conexion
                    .prepareStatement("SELECT * FROM " + tabla + " WHERE " + where + " LIMIT 8")) {
                for (int i = 1; i <= colsBusq.size(); i++)
                    ps.setString(i, texto + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Object[] fila = new Object[cols];
                        for (int c = 0; c < cols; c++)
                            fila[c] = rs.getObject(c + 1);
                        registrosEncontradosDueño.add(fila);
                        modeloSugerenciasDueño.addElement(rs.getString(primeraCol) + " | "
                                + (colsBusq.size() > 0 ? rs.getString(colsBusq.get(0)) : ""));
                    }
                }
            }
            if (!modeloSugerenciasDueño.isEmpty())
                mostrarPopupSugerencias();
            else
                popupSugerenciasDueño.setVisible(false);
        } catch (SQLException e) {
            popupSugerenciasDueño.setVisible(false);
        }
    }

    private void mostrarPopupSugerencias() {
        listaSugerenciasDueño.setVisibleRowCount(Math.min(modeloSugerenciasDueño.size(), 6));
        JScrollPane sp = new JScrollPane(listaSugerenciasDueño);
        sp.setPreferredSize(new Dimension(txtBusqueda.getWidth(), 150));
        popupSugerenciasDueño.removeAll();
        popupSugerenciasDueño.add(sp);
        popupSugerenciasDueño.show(txtBusqueda, 0, txtBusqueda.getHeight());
        if (modeloSugerenciasDueño.size() > 0)
            listaSugerenciasDueño.setSelectedIndex(0);
    }

    private void seleccionarSugerenciaDueño() {
        int idx = listaSugerenciasDueño.getSelectedIndex();
        if (idx >= 0 && idx < registrosEncontradosDueño.size()) {
            Object[] fila = registrosEncontradosDueño.get(idx);
            popupSugerenciasDueño.setVisible(false);
            if (fila[0] != null) {
                String val = String.valueOf(fila[0]);
                cargarFilasPorClave((String) comboTablas.getSelectedItem(), ultimaColumnaClaveBusqueda, val);
                txtBusqueda.setText(val);
            }
        }
    }

    private void cargarFilasPorClave(String tabla, String col, String val) {
        modeloTablas.setRowCount(0);
        modeloTablas.setColumnCount(0);
        try (Statement stmt = conexion.createStatement();
                ResultSet rsTest = stmt.executeQuery("SELECT * FROM " + tabla + " LIMIT 1")) {
            ResultSetMetaData meta = rsTest.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                modeloTablas.addColumn(meta.getColumnName(i));
        } catch (SQLException e) {
            return;
        }
        try (PreparedStatement ps = conexion.prepareStatement("SELECT * FROM " + tabla + " WHERE " + col + " = ?")) {
            ps.setString(1, val);
            try (ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] fila = new Object[cols];
                    for (int i = 0; i < cols; i++)
                        fila[i] = rs.getObject(i + 1);
                    modeloTablas.addRow(fila);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarFilasPorPrefijo(String tabla, String col, String prefijo) {
        modeloTablas.setRowCount(0);
        modeloTablas.setColumnCount(0);
        try (Statement stmt = conexion.createStatement();
                ResultSet rsTest = stmt.executeQuery("SELECT * FROM " + tabla + " LIMIT 1")) {
            ResultSetMetaData meta = rsTest.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                modeloTablas.addColumn(meta.getColumnName(i));
        } catch (SQLException e) {
            return;
        }
        try (PreparedStatement ps = conexion
                .prepareStatement("SELECT * FROM " + tabla + " WHERE " + col + " LIKE ? LIMIT 200")) {
            ps.setString(1, prefijo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] fila = new Object[cols];
                    for (int i = 0; i < cols; i++)
                        fila[i] = rs.getObject(i + 1);
                    modeloTablas.addRow(fila);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void habilitarEdicion() {
        int fila = tablaConsulta.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un registro");
            return;
        }
        String[] cols = new String[modeloTablas.getColumnCount()];
        for (int i = 0; i < cols.length; i++)
            cols[i] = modeloTablas.getColumnName(i);
        modeloTablas = new DefaultTableModel(modeloTablas.getDataVector(),
                new java.util.Vector<>(java.util.Arrays.asList(cols))) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c > 0;
            }
        };
        tablaConsulta.setModel(modeloTablas);
        tablaConsulta.setSelectionBackground(new Color(255, 255, 200));
        JOptionPane.showMessageDialog(this, "Modo edición activado");
    }

    private void guardarCambios() {
        String tabla = (String) comboTablas.getSelectedItem();
        try {
            for (int i = 0; i < modeloTablas.getRowCount(); i++) {
                StringBuilder sql = new StringBuilder("UPDATE " + tabla + " SET ");
                for (int j = 1; j < modeloTablas.getColumnCount(); j++) {
                    if (j > 1)
                        sql.append(", ");
                    sql.append(modeloTablas.getColumnName(j)).append(" = ?");
                }
                sql.append(" WHERE ").append(modeloTablas.getColumnName(0)).append(" = ?");
                try (PreparedStatement ps = conexion.prepareStatement(sql.toString())) {
                    for (int j = 1; j < modeloTablas.getColumnCount(); j++)
                        ps.setObject(j, modeloTablas.getValueAt(i, j));
                    ps.setObject(modeloTablas.getColumnCount(), modeloTablas.getValueAt(i, 0));
                    ps.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(this, "Cambios guardados");
            cancelarEdicion();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void cancelarEdicion() {
        cargarTabla((String) comboTablas.getSelectedItem());
        tablaConsulta.setSelectionBackground(new Color(184, 207, 229));
    }

    private void borrarRegistro() {
        int fila = tablaConsulta.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un registro para borrar");
            return;
        }
        String tabla = (String) comboTablas.getSelectedItem();
        Object idValor = modeloTablas.getValueAt(fila, 0);
        String columnaId = modeloTablas.getColumnName(0);

        // Confirmación del usuario
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que deseas borrar este registro?\n" +
                        columnaId + ": " + idValor,
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try (PreparedStatement ps = conexion.prepareStatement(
                "DELETE FROM " + tabla + " WHERE " + columnaId + " = ?")) {
            ps.setObject(1, idValor);
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "Registro borrado exitosamente");
                cargarEstadisticas();
                cargarTabla(tabla);
                txtBusqueda.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo borrar el registro");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al borrar: " + e.getMessage());
        }
    }

    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 8, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        lblVentasHoy = new JLabel("$0.00");
        lblTotalProductos = new JLabel("0");
        lblEmpleadosActivos = new JLabel("0");
        lblStockBajo = new JLabel("0");
        panel.add(crearTarjetaEstadistica("Ventas Hoy", lblVentasHoy, GRIS));
        panel.add(crearTarjetaEstadistica("Total de Registros", lblTotalProductos, GRIS));
        panel.add(crearTarjetaEstadistica("Empleados Activos", lblEmpleadosActivos, GRIS));
        panel.add(crearTarjetaEstadistica("Stock Bajo", lblStockBajo, GRIS));
        return panel;
    }

    private JPanel crearTarjetaEstadistica(String titulo, JLabel lblValor, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitulo.setForeground(Color.WHITE);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblValor.setForeground(color);
        lblValor.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(lblTitulo, BorderLayout.NORTH);
        p.add(lblValor, BorderLayout.CENTER);
        card.add(p, BorderLayout.CENTER);
        return card;
    }

    private void cargarEstadisticas() {
        try {
            try (Statement stmt = conexion.createStatement();
                    ResultSet rs = stmt
                            .executeQuery("SELECT COALESCE(SUM(total), 0) FROM Ventas WHERE DATE(fecha) = CURDATE()")) {
                if (rs.next())
                    lblVentasHoy.setText(String.format("$%.2f", rs.getDouble(1)));
            }
            // Contar registros en la tabla seleccionada
            String tablaActual = (String) comboTablas.getSelectedItem();
            if (tablaActual != null) {
                try (Statement s = conexion.createStatement();
                        ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + tablaActual)) {
                    if (r.next())
                        lblTotalProductos.setText(String.valueOf(r.getInt(1)));
                }
            }
            // Contar solo empleados activos
            try (Statement stmt = conexion.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Empleados WHERE estado = 'Activo'")) {
                if (rs.next())
                    lblEmpleadosActivos.setText(String.valueOf(rs.getInt(1)));
            }
            int stockBajo = 0;
            for (String inv : new String[] { "InventarioConsumibles", "InventarioDesechables", "InventarioLimpieza",
                    "InventarioAgricolas" }) {
                try (Statement s = conexion.createStatement();
                        ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + inv + " WHERE cantidad < 10")) {
                    if (r.next())
                        stockBajo += r.getInt(1);
                }
            }
            lblStockBajo.setText(String.valueOf(stockBajo));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarTabla(String tabla) {
        modeloTablas.setRowCount(0);
        modeloTablas.setColumnCount(0);
        try (Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + tabla + " LIMIT 100")) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++)
                modeloTablas.addColumn(meta.getColumnName(i));
            while (rs.next()) {
                Object[] fila = new Object[cols];
                for (int i = 0; i < cols; i++)
                    fila[i] = rs.getObject(i + 1);
                modeloTablas.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ==================== VISTA VENTAS ====================
    private JPanel crearVistaVentas() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel lbl = new JLabel("");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(lbl, BorderLayout.NORTH);
        modeloVentas = new DefaultTableModel(new String[] { "ID Venta", "Fecha", "Hora", "Método Pago", "Total" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaVentas = new JTable(modeloVentas);
        aplicarEstiloTabla(tablaVentas);
        panel.add(new JScrollPane(tablaVentas), BorderLayout.CENTER);
        return panel;
    }

    private void cargarVentas() {
        modeloVentas.setRowCount(0);
        try (Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id_venta, fecha, hora, metodo_pago, total FROM Ventas ORDER BY fecha DESC, hora DESC LIMIT 100")) {
            while (rs.next())
                modeloVentas.addRow(new Object[] { rs.getInt(1), rs.getDate(2), rs.getTime(3), rs.getString(4),
                        String.format("$%.2f", rs.getDouble(5)) });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ==================== VISTA HISTORIAL ====================
    private JPanel crearVistaHistorial() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel lbl = new JLabel("");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(lbl, BorderLayout.NORTH);
        modeloHistorial = new DefaultTableModel(new String[] { "ID Compra", "Proveedor", "Fecha", "Hora", "Total" },
                0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaHistorial = new JTable(modeloHistorial);
        aplicarEstiloTabla(tablaHistorial);
        panel.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);
        return panel;
    }

    private void cargarHistorial() {
        modeloHistorial.setRowCount(0);
        try (Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id_compra, id_proveedor, fecha, hora, total FROM Compras ORDER BY fecha DESC, hora DESC LIMIT 100")) {
            while (rs.next()) {
                String prov = obtenerNombreProveedor(rs.getInt(2));
                modeloHistorial.addRow(new Object[] { rs.getInt(1), prov, rs.getDate(3), rs.getTime(4),
                        String.format("$%.2f", rs.getDouble(5)) });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private String obtenerNombreProveedor(int id) {
        try (PreparedStatement ps = conexion
                .prepareStatement("SELECT nombre_proveedor FROM Proveedores WHERE id_proveedor = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Desconocido";
    }

    @Override
    public void dispose() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        super.dispose();
    }
}
