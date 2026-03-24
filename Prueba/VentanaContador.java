
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class VentanaContador extends JFrame {
    private CardLayout cardLayout;
    private JPanel panelContenido;
    private Connection conexion;

    // Mapa para almacenar los combos de cada panel
    private Map<String, JComboBox<String>> comboPorPanel;
    private Map<String, JTable> tablaPorPanel;
    private Map<String, DefaultTableModel> modeloPorPanel;
    private Map<String, JLabel> tituloPorPanel;

    public VentanaContador() {
        setTitle("Contador");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setLayout(new BorderLayout());

        // Inicializar mapas
        comboPorPanel = new HashMap<>();
        tablaPorPanel = new HashMap<>();
        modeloPorPanel = new HashMap<>();
        tituloPorPanel = new HashMap<>();

        // Inicializar conexión
        conexion = ConexionBD.conectar();

        // ---- PANEL LATERAL ----
        JPanel panelLateral = crearPanelLateral();
        add(panelLateral, BorderLayout.WEST);

        // ---- PANEL CENTRAL CON CARDLAYOUT ----
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

        // Crear y agregar todos los paneles (sin panel de inicio)

        // MÓDULOS CON EDICIÓN (Compras, Ventas, Pedidos)
        panelContenido.add(crearPanelConTabla("", "comprasProv",
                "Proveedores,Compras,DetalleCompraDesechables,DetalleCompraLimpieza,DetalleCompraConsumibles,DetalleCompraAgricolas",
                true), "comprasProv");
        panelContenido.add(crearPanelConTabla("", "ventas",
                "Ventas,DetalleVentaDesechables,DetalleVentaLimpieza,DetalleVentaConsumibles,DetalleVentaAgricolas",
                true), "ventas");
        panelContenido.add(crearPanelConTabla("", "pedidos",
                "PedidoDesechables,PedidoLimpieza,PedidoConsumibles,PedidoAgricolas", true), "pedidos");

        // MÓDULOS DE SOLO LECTURA (Inventarios, Clientes)
        panelContenido.add(crearPanelConTabla("", "inventarios",
                "InventarioDesechables,InventarioLimpieza,InventarioConsumibles,InventarioAgricolas", false),
                "inventarios");
        panelContenido.add(crearPanelConTabla("", "clientes",
                "Clientes", false), "clientes");

        add(panelContenido, BorderLayout.CENTER);

        // Mostrar directamente el primer módulo (Compras y Proveedores)
        cardLayout.show(panelContenido, "comprasProv");
        cargarPrimeraTabla("comprasProv");
    }

    private JPanel crearPanelLateral() {
        JPanel panelLateral = new JPanel();
        panelLateral.setLayout(new GridLayout(6, 1, 10, 10));
        panelLateral.setBackground(PaletaColores.BACKGROUND);
        panelLateral.setPreferredSize(new Dimension(200, 0));
        panelLateral.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        // BOTONES con colores de la paleta
        JButton btnComprasProv = crearBotonLateral("Compras y Proveedores", PaletaColores.SECONDARY);
        JButton btnVentas = crearBotonLateral("Ventas", PaletaColores.TERTIARY);
        JButton btnInventarios = crearBotonLateral("Inventarios", PaletaColores.ACCENT);
        JButton btnPedidos = crearBotonLateral("Pedidos", PaletaColores.PRIMARY);
        JButton btnClientes = crearBotonLateral("Clientes", PaletaColores.SECONDARY);
        JButton btnCerrarSesion = crearBotonLateral("Cerrar Sesión", new Color(120, 0, 0));

        panelLateral.add(btnComprasProv);
        panelLateral.add(btnVentas);
        panelLateral.add(btnInventarios);
        panelLateral.add(btnPedidos);
        panelLateral.add(btnClientes);
        panelLateral.add(btnCerrarSesion);

        // ---- ACCIONES ----
        btnComprasProv.addActionListener(e -> {
            cardLayout.show(panelContenido, "comprasProv");
            cargarPrimeraTabla("comprasProv");
        });

        btnVentas.addActionListener(e -> {
            cardLayout.show(panelContenido, "ventas");
            cargarPrimeraTabla("ventas");
        });

        btnInventarios.addActionListener(e -> {
            cardLayout.show(panelContenido, "inventarios");
            cargarPrimeraTabla("inventarios");
        });

        btnPedidos.addActionListener(e -> {
            cardLayout.show(panelContenido, "pedidos");
            cargarPrimeraTabla("pedidos");
        });

        btnClientes.addActionListener(e -> {
            cardLayout.show(panelContenido, "clientes");
            cargarPrimeraTabla("clientes");
        });

        btnCerrarSesion.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que quieres cerrar sesión?",
                    "Cerrar Sesión",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (conexion != null) {
                    try {
                        conexion.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                dispose();
                new prueba().setVisible(true);
            }
        });

        return panelLateral;
    }

    private JButton crearBotonLateral(String texto, Color color) {
        JButton boton = new JButton("<html><center>" + texto + "</center></html>");
        boton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        boton.setForeground(isLight(color) ? Color.BLACK : Color.WHITE);
        boton.setBackground(color);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(12, 5, 12, 5));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efecto hover
        boton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                boton.setBackground(color.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                boton.setBackground(color);
            }
        });

        return boton;
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

    private void cargarPrimeraTabla(String nombrePanel) {
        JComboBox<String> combo = comboPorPanel.get(nombrePanel);
        if (combo != null && combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    // ---------------- MÉTODOS DE CREACIÓN DE PANELES ----------------

    private JPanel crearPanelConTabla(String tituloTexto, String nombrePanel, String tablasString,
            boolean permitirEdicion) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Panel superior con título y selector
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        panelSuperior.setOpaque(false);

        JLabel lblTitulo = new JLabel(tituloTexto);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);
        tituloPorPanel.put(nombrePanel, lblTitulo);

        // Combo para seleccionar tablas
        JPanel panelSelector = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelSelector.setOpaque(false);

        JLabel lblSelector = new JLabel("Seleccionar tabla:");
        lblSelector.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSelector.setForeground(Color.WHITE);

        JComboBox<String> comboTablas = new JComboBox<>();
        comboTablas.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboTablas.setPreferredSize(new Dimension(220, 30));

        // Agregar las tablas al combo
        String[] tablas = tablasString.split(",");
        for (String tabla : tablas) {
            comboTablas.addItem(tabla.trim());
        }

        comboTablas.addActionListener(e -> {
            String tablaSeleccionada = (String) comboTablas.getSelectedItem();
            if (tablaSeleccionada != null && !tablaSeleccionada.isEmpty()) {
                cargarDatosTabla(nombrePanel, tablaSeleccionada);
            }
        });

        comboPorPanel.put(nombrePanel, comboTablas);

        panelSelector.add(lblSelector);
        panelSelector.add(comboTablas);

        panelSuperior.add(lblTitulo, BorderLayout.WEST);
        panelSuperior.add(panelSelector, BorderLayout.EAST);

        // Tabla de datos
        DefaultTableModel modeloTabla = new DefaultTableModel();
        JTable tablaDatos = new JTable(modeloTabla);
        aplicarEstiloTabla(tablaDatos);

        modeloPorPanel.put(nombrePanel, modeloTabla);
        tablaPorPanel.put(nombrePanel, tablaDatos);

        JScrollPane scrollPane = new JScrollPane(tablaDatos);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel de botones (solo si se permite edición)
        if (permitirEdicion) {
            JPanel panelBotones = crearPanelBotones(nombrePanel);
            panel.add(panelBotones, BorderLayout.SOUTH);
        } else {
            // Agregar mensaje de solo lectura
            JPanel panelSoloLectura = new JPanel();
            panelSoloLectura.setOpaque(false);
            JLabel lblSoloLectura = new JLabel("⚠ Solo lectura - No puede modificar estos datos");
            lblSoloLectura.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblSoloLectura.setForeground(Color.WHITE);
            lblSoloLectura.setHorizontalAlignment(SwingConstants.CENTER);
            lblSoloLectura.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            panelSoloLectura.add(lblSoloLectura);
            panel.add(panelSoloLectura, BorderLayout.SOUTH);
        }

        return panel;
    }

    private JPanel crearPanelBotones(String nombrePanel) {
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelBotones.setOpaque(false);

        JButton btnAgregar = crearBotonAccion("Agregar", PaletaColores.PRIMARY);
        JButton btnEditar = crearBotonAccion("Editar", PaletaColores.SECONDARY);
        JButton btnEliminar = crearBotonAccion("Eliminar", PaletaColores.TERTIARY);
        JButton btnRefrescar = crearBotonAccion("Refrescar", PaletaColores.ACCENT);

        // Acciones de los botones
        btnAgregar.addActionListener(e -> agregarRegistro(nombrePanel));
        btnEditar.addActionListener(e -> editarRegistro(nombrePanel));
        btnEliminar.addActionListener(e -> eliminarRegistro(nombrePanel));
        btnRefrescar.addActionListener(e -> {
            JComboBox<String> combo = comboPorPanel.get(nombrePanel);
            if (combo != null && combo.getSelectedItem() != null) {
                cargarDatosTabla(nombrePanel, (String) combo.getSelectedItem());
            }
        });

        panelBotones.add(btnAgregar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnRefrescar);

        return panelBotones;
    }

    private JButton crearBotonAccion(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(color.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    // ---------- MÉTODOS CRUD ----------

    private void agregarRegistro(String nombrePanel) {
        JComboBox<String> combo = comboPorPanel.get(nombrePanel);
        if (combo == null || combo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una tabla primero");
            return;
        }

        String tablaSeleccionada = (String) combo.getSelectedItem();

        // Verificar si es una tabla de solo lectura (Proveedores o Clientes)
        if (tablaSeleccionada.equals("Proveedores") || tablaSeleccionada.equals("Clientes")) {
            JOptionPane.showMessageDialog(this,
                    "⚠ No tiene permisos para modificar " + tablaSeleccionada,
                    "Acceso Denegado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Función Agregar para tabla: " + tablaSeleccionada + "\n(Implementación pendiente)",
                "Agregar Registro",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void editarRegistro(String nombrePanel) {
        JTable tabla = tablaPorPanel.get(nombrePanel);
        JComboBox<String> combo = comboPorPanel.get(nombrePanel);

        if (tabla == null || combo == null || combo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una tabla primero");
            return;
        }

        String tablaSeleccionada = (String) combo.getSelectedItem();

        // Verificar si es una tabla de solo lectura
        if (tablaSeleccionada.equals("Proveedores") || tablaSeleccionada.equals("Clientes")) {
            JOptionPane.showMessageDialog(this,
                    "⚠ No tiene permisos para modificar " + tablaSeleccionada,
                    "Acceso Denegado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para editar");
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Función Editar para tabla: " + tablaSeleccionada + "\nFila: " + filaSeleccionada
                        + "\n(Implementación pendiente)",
                "Editar Registro",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void eliminarRegistro(String nombrePanel) {
        JTable tabla = tablaPorPanel.get(nombrePanel);
        JComboBox<String> combo = comboPorPanel.get(nombrePanel);

        if (tabla == null || combo == null || combo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una tabla primero");
            return;
        }

        String tablaSeleccionada = (String) combo.getSelectedItem();

        // Verificar si es una tabla de solo lectura
        if (tablaSeleccionada.equals("Proveedores") || tablaSeleccionada.equals("Clientes")) {
            JOptionPane.showMessageDialog(this,
                    "⚠ No tiene permisos para modificar " + tablaSeleccionada,
                    "Acceso Denegado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para eliminar");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de que desea eliminar este registro?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "Función Eliminar para tabla: " + tablaSeleccionada + "\nFila: " + filaSeleccionada
                            + "\n(Implementación pendiente)",
                    "Eliminar Registro",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ---------- MÉTODOS PARA CARGAR DATOS ----------

    private void cargarDatosTabla(String nombrePanel, String nombreTabla) {
        if (conexion == null) {
            JOptionPane.showMessageDialog(this, "No hay conexión a la base de datos");
            return;
        }

        DefaultTableModel modelo = modeloPorPanel.get(nombrePanel);
        JLabel lblTitulo = tituloPorPanel.get(nombrePanel);

        if (modelo == null)
            return;

        modelo.setRowCount(0);
        modelo.setColumnCount(0);

        try {
            String sql = "SELECT * FROM " + nombreTabla + " LIMIT 500";
            try (Statement stmt = conexion.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Agregar columnas al modelo
                for (int i = 1; i <= columnCount; i++) {
                    modelo.addColumn(metaData.getColumnName(i));
                }

                // Agregar filas al modelo
                int contadorFilas = 0;
                while (rs.next()) {
                    Object[] fila = new Object[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        fila[i] = rs.getObject(i + 1);
                    }
                    modelo.addRow(fila);
                    contadorFilas++;
                }

                // Actualizar título con cantidad de registros
                if (lblTitulo != null) {
                    String tituloBase = lblTitulo.getText();
                    if (tituloBase.contains(" - ")) {
                        tituloBase = tituloBase.substring(0, tituloBase.indexOf(" - "));
                    }
                    lblTitulo.setText("");
                }

                if (contadorFilas == 0) {
                    JOptionPane.showMessageDialog(this,
                            "La tabla '" + nombreTabla + "' no contiene datos.",
                            "Información",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar datos de la tabla '" + nombreTabla + "':\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new VentanaContador().setVisible(true);
        });
    }
}