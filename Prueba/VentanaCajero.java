import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class VentanaCajero extends JFrame {

    private CardLayout cardLayout;
    private JPanel panelContenido;
    private Connection conexion;
    private JTable tablaProductos;
    private DefaultTableModel modeloProductos;
    private DefaultTableModel modeloCarrito;
    private JLabel lblTotal;
    private JTextField txtCodigo;
    private JTextField txtCantidad;
    private JList<String> listaSugerencias;
    private DefaultListModel<String> modeloSugerencias;
    private JPopupMenu popupSugerencias;
    private List<Producto> productosEncontrados;

    // Componentes para módulo de pedidos
    private JTable tablaPedidos;
    private DefaultTableModel modeloPedidos;

    // Usar la paleta centralizada
    private static final java.awt.Color PALETTE_DEEP_MAGENTA = PaletaColores.TERTIARY;
    private static final java.awt.Color PALETTE_RED = PaletaColores.SECONDARY;
    private static final java.awt.Color PALETTE_ORANGE = PaletaColores.PRIMARY;
    private static final java.awt.Color PALETTE_WARM = PaletaColores.ACCENT;
    private static final java.awt.Color PALETTE_BLUE = PaletaColores.PRIMARY;

    // Colors chosen to style buttons harmoniously
    private static final java.awt.Color BTN_PRIMARY = PaletaColores.PRIMARY;
    private static final java.awt.Color BTN_SECONDARY = PaletaColores.SECONDARY;
    private static final java.awt.Color BTN_ACCENT = PaletaColores.ACCENT;
    private static final java.awt.Color BTN_DANGER = PaletaColores.TERTIARY;
    private static final java.awt.Color BTN_DARK = PaletaColores.BACKGROUND;

    // Helper: determina si un color es claro
    private static boolean isLight(Color c) {
        double r = c.getRed() / 255.0;
        double g = c.getGreen() / 255.0;
        double b = c.getBlue() / 255.0;
        double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return lum > 0.75;
    }

    // Clase interna para manejar productos
    private class Producto {
        String id;
        String nombre;
        double precio;
        int stock;
        String tipo;

        public Producto(String id, String nombre, double precio, int stock, String tipo) {
            this.id = id;
            this.nombre = nombre;
            this.precio = precio;
            this.stock = stock;
            this.tipo = tipo;
        }
    }

    public VentanaCajero() {
        setTitle("Sistema de Punto de Venta - Tienda JC");
        setSize(1000, 650);
        setMinimumSize(new Dimension(800, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        // Inicializar el modelo del carrito
        String[] columnas = { "Código", "Cantidad", "Descripción", "Precio", "Subtotal" };
        modeloCarrito = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Inicializar componentes de autocompletado
        modeloSugerencias = new DefaultListModel<>();
        listaSugerencias = new JList<>(modeloSugerencias);
        listaSugerencias.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        listaSugerencias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        popupSugerencias = new JPopupMenu();
        productosEncontrados = new ArrayList<>();

        // Inicializar modelo de pedidos
        modeloPedidos = new DefaultTableModel(new String[] { "ID", "Cliente", "Fecha", "Estado", "Saldo" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Panel lateral con botones
        JPanel panelLateral = crearPanelLateral();
        add(panelLateral, BorderLayout.WEST);

        // Panel central con CardLayout
        cardLayout = new CardLayout();
        panelContenido = new JPanel(cardLayout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                GradientPaint gradiente = PaletaColores.gradientFor(getWidth(), getHeight());
                g2d.setPaint(gradiente);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };

        panelContenido.add(new JPanel(), "inicio");
        JPanel ventaPanel = crearPanelVenta();
        ventaPanel.setOpaque(false);
        panelContenido.add(ventaPanel, "venta");

        JPanel productosPanel = crearPanelProductos();
        productosPanel.setOpaque(false);
        panelContenido.add(productosPanel, "productos");

        JPanel pedidosPanel = crearPanelPedidos();
        pedidosPanel.setOpaque(false);
        panelContenido.add(pedidosPanel, "pedidos");

        cardLayout.show(panelContenido, "venta");
        add(panelContenido, BorderLayout.CENTER);

        // Listener para redimensionar
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarLayoutDinamico();
            }
        });

        // Inicializar conexión a la base de datos
        conexion = ConexionBD.conectar();
        if (conexion == null) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar a la base de datos. Algunas funciones estarán deshabilitadas.",
                    "Error de Conexión",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel crearPanelLateral() {
        JPanel panelLateral = new JPanel();
        panelLateral.setLayout(new GridLayout(5, 1, 8, 8));
        panelLateral.setBackground(PaletaColores.BACKGROUND);
        panelLateral.setPreferredSize(new Dimension(180, 0));
        panelLateral.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JButton btnVenta = crearBotonLateral("Iniciar Venta", BTN_PRIMARY);
        JButton btnProductos = crearBotonLateral("Consultar Productos", BTN_SECONDARY);
        JButton btnPedidos = crearBotonLateral("Gestión de Pedidos", BTN_ACCENT);
        JButton btnReportes = crearBotonLateral("Reportes", BTN_DARK);
        JButton btnCerrar = crearBotonLateral("Cerrar Sesión", BTN_DANGER);

        panelLateral.add(btnVenta);
        panelLateral.add(btnProductos);
        panelLateral.add(btnPedidos);
        panelLateral.add(btnReportes);
        panelLateral.add(btnCerrar);

        btnVenta.addActionListener(e -> cardLayout.show(panelContenido, "venta"));
        btnProductos.addActionListener(e -> {
            cargarProductos();
            cardLayout.show(panelContenido, "productos");
        });
        btnPedidos.addActionListener(e -> {
            cargarPedidos("");
            cardLayout.show(panelContenido, "pedidos");
        });
        btnReportes.addActionListener(e -> mostrarReportes());
        btnCerrar.addActionListener(e -> cerrarSesion());

        return panelLateral;
    }

    private JButton crearBotonLateral(String texto, Color color) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        boton.setBackground(color);
        boton.setForeground(isLight(color) ? Color.BLACK : Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(12, 5, 12, 5));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        boton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                Color hover = color.brighter();
                boton.setBackground(hover);
                boton.setForeground(isLight(hover) ? Color.BLACK : Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                boton.setBackground(color);
                boton.setForeground(isLight(color) ? Color.BLACK : Color.WHITE);
            }
        });

        return boton;
    }

    private JPanel crearPanelVenta() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setOpaque(false);

        // Panel superior con búsqueda
        JPanel panelBusqueda = crearPanelBusqueda();

        // Panel central con carrito
        JPanel panelCarrito = crearPanelCarrito();

        // Panel inferior con acciones
        JPanel panelAcciones = crearPanelAcciones();

        panel.add(panelBusqueda, BorderLayout.NORTH);
        panel.add(panelCarrito, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelBusqueda() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Código del producto
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel lblCodigo = new JLabel("Código o nombre:");
        lblCodigo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lblCodigo, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        txtCodigo = new JTextField();
        txtCodigo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCodigo.setPreferredSize(new Dimension(150, 35));
        panel.add(txtCodigo, gbc);

        // Cantidad
        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblCantidad = new JLabel("Cantidad:");
        lblCantidad.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lblCantidad, gbc);

        gbc.gridx = 3;
        JPanel panelCantidad = new JPanel(new BorderLayout());
        panelCantidad.setBackground(Color.WHITE);
        panelCantidad.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panelCantidad.setPreferredSize(new Dimension(120, 35));

        txtCantidad = new JTextField("1");
        txtCantidad.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCantidad.setHorizontalAlignment(JTextField.CENTER);

        JButton btnDecrementar = new JButton("-");
        btnDecrementar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDecrementar.setBackground(new Color(240, 240, 240));
        btnDecrementar.setFocusPainted(false);
        btnDecrementar.setPreferredSize(new Dimension(30, 35));

        JButton btnIncrementar = new JButton("+");
        btnIncrementar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnIncrementar.setBackground(new Color(240, 240, 240));
        btnIncrementar.setFocusPainted(false);
        btnIncrementar.setPreferredSize(new Dimension(30, 35));

        panelCantidad.add(btnDecrementar, BorderLayout.WEST);
        panelCantidad.add(txtCantidad, BorderLayout.CENTER);
        panelCantidad.add(btnIncrementar, BorderLayout.EAST);
        panel.add(panelCantidad, gbc);

        // Botones
        gbc.gridx = 4;
        JButton btnAgregar = new JButton("Agregar");
        btnAgregar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAgregar.setBackground(BTN_PRIMARY);
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFocusPainted(false);
        btnAgregar.setPreferredSize(new Dimension(100, 35));
        panel.add(btnAgregar, gbc);

        gbc.gridx = 5;
        JButton btnBuscar = new JButton("(Buscar)");
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBuscar.setBackground(BTN_SECONDARY);
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFocusPainted(false);
        btnBuscar.setPreferredSize(new Dimension(100, 35));
        panel.add(btnBuscar, gbc);

        // Configurar eventos
        configurarEventosBusqueda(btnAgregar, btnBuscar, btnDecrementar, btnIncrementar);

        return panel;
    }

    private JPanel crearPanelCarrito() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Carrito de Compras"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JTable tablaCarrito = new JTable(modeloCarrito);
        tablaCarrito.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tablaCarrito.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        tablaCarrito.setRowHeight(22);
        tablaCarrito.setSelectionBackground(new Color(220, 240, 255));

        JScrollPane scroll = new JScrollPane(tablaCarrito);
        panel.add(scroll, BorderLayout.CENTER);

        // Evento doble click para eliminar
        tablaCarrito.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tablaCarrito.getSelectedRow();
                    if (fila != -1) {
                        eliminarProductoCarrito(fila);
                    }
                }
            }
        });

        return panel;
    }

    private JPanel crearPanelAcciones() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Total
        gbc.gridx = 0;
        gbc.weightx = 1.5;
        lblTotal = new JLabel("Total: $0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotal.setForeground(new Color(0, 100, 0));
        lblTotal.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTotal, gbc);

        // Botones
        gbc.weightx = 1.0;
        gbc.gridx = 1;
        JButton btnLimpiar = crearBotonAccion("Limpiar", BTN_DANGER);
        panel.add(btnLimpiar, gbc);

        gbc.gridx = 2;
        JButton btnImprimir = crearBotonAccion("Imprimir", BTN_PRIMARY);
        panel.add(btnImprimir, gbc);

        gbc.gridx = 3;
        JButton btnCobrar = crearBotonAccion("Cobrar", BTN_ACCENT);
        panel.add(btnCobrar, gbc);

        // Configurar eventos
        btnLimpiar.addActionListener(e -> limpiarCarrito());
        btnImprimir.addActionListener(e -> imprimirTicket());
        btnCobrar.addActionListener(e -> procesarCobro());

        return panel;
    }

    private void ajustarLayoutDinamico() {
        SwingUtilities.invokeLater(() -> {
            panelContenido.revalidate();
            panelContenido.repaint();
        });
    }

    private void configurarEventosBusqueda(JButton btnAgregar, JButton btnBuscar,
            JButton btnDecrementar, JButton btnIncrementar) {
        // Autocompletado
        txtCodigo.getDocument().addDocumentListener(new DocumentListener() {
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

        txtCodigo.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && popupSugerencias.isVisible()) {
                    e.consume();
                    if (!modeloSugerencias.isEmpty()) {
                        listaSugerencias.setSelectedIndex(0);
                        seleccionarSugerencia();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && popupSugerencias.isVisible()) {
                    listaSugerencias.requestFocus();
                    if (modeloSugerencias.size() > 0) {
                        listaSugerencias.setSelectedIndex(0);
                    }
                }
            }
        });

        listaSugerencias.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    seleccionarSugerencia();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popupSugerencias.setVisible(false);
                    txtCodigo.requestFocus();
                }
            }
        });

        listaSugerencias.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    seleccionarSugerencia();
                }
            }
        });

        // Cantidad
        btnIncrementar.addActionListener(e -> {
            try {
                int cantidad = Integer.parseInt(txtCantidad.getText());
                txtCantidad.setText(String.valueOf(cantidad + 1));
            } catch (NumberFormatException ex) {
                txtCantidad.setText("1");
            }
        });

        btnDecrementar.addActionListener(e -> {
            try {
                int cantidad = Integer.parseInt(txtCantidad.getText());
                if (cantidad > 1) {
                    txtCantidad.setText(String.valueOf(cantidad - 1));
                }
            } catch (NumberFormatException ex) {
                txtCantidad.setText("1");
            }
        });

        btnBuscar.addActionListener(e -> {
            if (txtCodigo.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Ingrese código o nombre del producto para buscar",
                        "Búsqueda",
                        JOptionPane.INFORMATION_MESSAGE);
                txtCodigo.requestFocus();
            } else {
                buscarCoincidencias();
            }
        });

        btnAgregar.addActionListener(e -> agregarProductoAlCarrito());
        txtCodigo.addActionListener(e -> agregarProductoAlCarrito());
        txtCantidad.addActionListener(e -> agregarProductoAlCarrito());
    }

    private void mostrarNotificacionVenta(boolean exitosa, String mensaje) {
        JWindow notification = new JWindow(this);
        notification.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(147, 112, 219), 3),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));
        panel.setBackground(Color.WHITE);

        // Icono
        JLabel lblIcono = new JLabel(exitosa ? "✅" : "❌");
        lblIcono.setFont(new Font("Segoe UI", Font.PLAIN, 45));
        panel.add(lblIcono, BorderLayout.WEST);

        // Mensaje
        JLabel lblMensaje = new JLabel("<html><div style='text-align: center;'>" + mensaje + "</div></html>");
        lblMensaje.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMensaje.setForeground(exitosa ? new Color(76, 175, 80) : new Color(244, 67, 54));
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblMensaje, BorderLayout.CENTER);

        notification.add(panel);
        notification.pack();

        // Centrar en la ventana principal
        Point location = getLocationOnScreen();
        Dimension size = getSize();
        Dimension notifSize = notification.getSize();
        notification.setLocation(
                location.x + (size.width - notifSize.width) / 2,
                location.y + (size.height - notifSize.height) / 2);

        notification.setOpacity(0.0f);
        notification.setVisible(true);

        // Animación de entrada más lenta
        Timer fadeIn = new Timer(30, null);
        fadeIn.addActionListener(new ActionListener() {
            float opacity = 0.0f;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.08f;
                if (opacity >= 0.98f) {
                    opacity = 0.98f;
                    notification.setOpacity(opacity);
                    fadeIn.stop();

                    // Esperar 2 segundos y luego fade out
                    Timer pause = new Timer(2000, ev -> {
                        Timer fadeOut = new Timer(30, null);
                        fadeOut.addActionListener(new ActionListener() {
                            float op = 0.98f;

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                op -= 0.08f;
                                if (op <= 0.0f) {
                                    notification.dispose();
                                    fadeOut.stop();
                                } else {
                                    notification.setOpacity(op);
                                }
                            }
                        });
                        fadeOut.start();
                    });
                    pause.setRepeats(false);
                    pause.start();
                } else {
                    notification.setOpacity(opacity);
                }
            }
        });
        fadeIn.start();
    }

    private void buscarCoincidencias() {
        String texto = txtCodigo.getText().trim();
        boolean soloNumeros = texto.matches("\\d+");

        if (soloNumeros) {
            if (texto.length() < 1) {
                popupSugerencias.setVisible(false);
                return;
            }
        } else {
            if (texto.length() < 2) {
                popupSugerencias.setVisible(false);
                return;
            }
        }

        productosEncontrados.clear();
        modeloSugerencias.clear();

        try {
            String[] tablas = { "InventarioConsumibles", "InventarioDesechables", "InventarioLimpieza",
                    "InventarioAgricolas" };
            String[] tipos = { "Consumible", "Desechable", "Limpieza", "Agrícola" };

            for (int i = 0; i < tablas.length; i++) {
                String sql;
                String parametroBusqueda = texto;
                if (soloNumeros && texto.length() > 4) {
                    parametroBusqueda = texto.substring(0, 4);
                }

                if (soloNumeros) {
                    sql = "SELECT id_producto, nombre_producto, precio_unitario, cantidad FROM " + tablas[i]
                            + " WHERE id_producto LIKE ? LIMIT 8";
                    try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                        ps.setString(1, parametroBusqueda + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String id = rs.getString("id_producto");
                                String nombre = rs.getString("nombre_producto");
                                double precio = rs.getDouble("precio_unitario");
                                int stock = rs.getInt("cantidad");

                                Producto producto = new Producto(id, nombre, precio, stock, tipos[i]);
                                productosEncontrados.add(producto);

                                String sugerencia = String.format("%-8s | %-25s | $%-8.2f | %s",
                                        id,
                                        nombre.length() > 25 ? nombre.substring(0, 22) + "..." : nombre,
                                        precio,
                                        tipos[i]);
                                modeloSugerencias.addElement(sugerencia);
                            }
                        }
                    }
                } else {
                    sql = "SELECT id_producto, nombre_producto, precio_unitario, cantidad FROM " + tablas[i]
                            + " WHERE id_producto LIKE ? OR nombre_producto LIKE ? LIMIT 8";
                    try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                        ps.setString(1, parametroBusqueda + "%");
                        ps.setString(2, parametroBusqueda + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String id = rs.getString("id_producto");
                                String nombre = rs.getString("nombre_producto");
                                double precio = rs.getDouble("precio_unitario");
                                int stock = rs.getInt("cantidad");

                                Producto producto = new Producto(id, nombre, precio, stock, tipos[i]);
                                productosEncontrados.add(producto);

                                String sugerencia = String.format("%-8s | %-25s | $%-8.2f | %s",
                                        id,
                                        nombre.length() > 25 ? nombre.substring(0, 22) + "..." : nombre,
                                        precio,
                                        tipos[i]);
                                modeloSugerencias.addElement(sugerencia);
                            }
                        }
                    }
                }
            }

            if (!modeloSugerencias.isEmpty()) {
                mostrarPopupSugerencias();
            } else {
                popupSugerencias.setVisible(false);
            }

        } catch (SQLException ex) {
            // Silenciar errores de búsqueda
        }
    }

    private void mostrarPopupSugerencias() {
        if (modeloSugerencias.isEmpty())
            return;

        listaSugerencias.setVisibleRowCount(Math.min(modeloSugerencias.size(), 6));

        JScrollPane scrollPane = new JScrollPane(listaSugerencias);
        scrollPane.setPreferredSize(new Dimension(txtCodigo.getWidth(), 150));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        popupSugerencias.removeAll();
        popupSugerencias.add(scrollPane);
        popupSugerencias.setFocusable(false);

        popupSugerencias.show(txtCodigo, 0, txtCodigo.getHeight());
        popupSugerencias.revalidate();
        popupSugerencias.repaint();

        if (modeloSugerencias.size() > 0) {
            listaSugerencias.setSelectedIndex(0);
        }
    }

    private void seleccionarSugerencia() {
        int index = listaSugerencias.getSelectedIndex();
        if (index >= 0 && index < productosEncontrados.size()) {
            Producto producto = productosEncontrados.get(index);
            txtCodigo.setText(producto.id);
            popupSugerencias.setVisible(false);
            txtCantidad.requestFocus();
            txtCantidad.selectAll();
        }
    }

    private void agregarProductoAlCarrito() {
        String codigo = txtCodigo.getText().trim();
        String cantidadStr = txtCantidad.getText().trim();

        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa el código del producto", "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            txtCodigo.requestFocus();
            return;
        }

        if (cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa la cantidad", "Advertencia", JOptionPane.WARNING_MESSAGE);
            txtCantidad.requestFocus();
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadStr);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a 0", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Producto producto = buscarProductoPorCodigo(codigo);
        if (producto == null) {
            JOptionPane.showMessageDialog(this, "Producto no encontrado", "Error", JOptionPane.ERROR_MESSAGE);
            txtCodigo.requestFocus();
            txtCodigo.selectAll();
            return;
        }

        if (cantidad > producto.stock) {
            JOptionPane.showMessageDialog(this,
                    String.format("Stock insuficiente. Solo hay %d unidades disponibles", producto.stock),
                    "Stock Insuficiente",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        double subtotal = cantidad * producto.precio;
        String descripcion = producto.nombre + " (" + producto.tipo + ")";

        boolean productoExistente = false;
        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            if (modeloCarrito.getValueAt(i, 0).equals(codigo)) {
                int cantidadActual = (int) modeloCarrito.getValueAt(i, 1);
                modeloCarrito.setValueAt(cantidadActual + cantidad, i, 1);
                modeloCarrito.setValueAt((cantidadActual + cantidad) * producto.precio, i, 4);
                productoExistente = true;
                break;
            }
        }

        if (!productoExistente) {
            modeloCarrito.addRow(new Object[] { codigo, cantidad, descripcion, producto.precio, subtotal });
        }

        actualizarTotal();

        txtCodigo.setText("");
        txtCantidad.setText("1");
        txtCodigo.requestFocus();
        popupSugerencias.setVisible(false);
    }

    private Producto buscarProductoPorCodigo(String codigo) {
        try {
            String[] tablas = { "InventarioConsumibles", "InventarioDesechables", "InventarioLimpieza",
                    "InventarioAgricolas" };
            String[] tipos = { "Consumible", "Desechable", "Limpieza", "Agrícola" };

            for (int i = 0; i < tablas.length; i++) {
                String sql = "SELECT id_producto, nombre_producto, precio_unitario, cantidad FROM " +
                        tablas[i] + " WHERE id_producto = ?";
                try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                    ps.setString(1, codigo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String id = rs.getString("id_producto");
                            String nombre = rs.getString("nombre_producto");
                            double precio = rs.getDouble("precio_unitario");
                            int stock = rs.getInt("cantidad");
                            return new Producto(id, nombre, precio, stock, tipos[i]);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar producto: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void eliminarProductoCarrito(int fila) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este producto del carrito?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            modeloCarrito.removeRow(fila);
            actualizarTotal();
        }
    }

    private void actualizarTotal() {
        double total = 0;
        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            Object val = modeloCarrito.getValueAt(i, 4);
            if (val instanceof Number) {
                total += ((Number) val).doubleValue();
            }
        }
        lblTotal.setText(String.format("Total: $%.2f", total));
    }

    private void limpiarCarrito() {
        if (modeloCarrito.getRowCount() == 0) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres limpiar el carrito?",
                "Confirmar Limpieza",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            modeloCarrito.setRowCount(0);
            actualizarTotal();
            mostrarNotificacionVenta(true, "Carrito limpiado");
        }
    }

    private void imprimirTicket() {
        if (modeloCarrito.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            StringBuilder ticketContent = new StringBuilder();

            ticketContent.append("================================\n");
            ticketContent.append("         TIENDA JC\n");
            ticketContent.append("================================\n");
            ticketContent.append("Fecha: ").append(new SimpleDateFormat("dd/MM/yyyy").format(new Date())).append("\n");
            ticketContent.append("Hora: ").append(new SimpleDateFormat("HH:mm:ss").format(new Date())).append("\n");
            ticketContent.append("--------------------------------\n");
            ticketContent.append("CANT DESCRIPCION       PRECIO  \n");
            ticketContent.append("--------------------------------\n");

            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                String cantidad = modeloCarrito.getValueAt(i, 1).toString();
                String descripcion = modeloCarrito.getValueAt(i, 2).toString();
                String precio = String.format("$%.2f", modeloCarrito.getValueAt(i, 3));

                ticketContent.append(String.format("%-4s %-15s %-8s\n",
                        cantidad,
                        descripcion.length() > 15 ? descripcion.substring(0, 15) : descripcion,
                        precio));
            }

            ticketContent.append("--------------------------------\n");
            ticketContent.append(String.format("TOTAL: $%.2f\n", calcularTotal()));
            ticketContent.append("================================\n");
            ticketContent.append("     ¡GRACIAS POR SU COMPRA!\n");
            ticketContent.append("================================\n");

            String[] opcionesPago = { "Efectivo", "Tarjeta de crédito", "Tarjeta de débito", "Transferencia" };
            String metodoPago = (String) JOptionPane.showInputDialog(this,
                    "Selecciona el método de pago:",
                    "Método de Pago",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcionesPago,
                    opcionesPago[0]);

            if (metodoPago == null) {
                return; // Usuario canceló
            }

            // Preguntar si desea imprimir
            int opcionImprimir = JOptionPane.showConfirmDialog(this,
                    "¿Desea imprimir el ticket?\n\n" + ticketContent.toString() +
                            "\nMétodo de pago: " + metodoPago,
                    "Confirmar Impresión",
                    JOptionPane.YES_NO_OPTION);

            // Guardar la venta SIEMPRE (con o sin impresión)
            if (guardarVentaEnBD(metodoPago)) {
                // Mostrar notificación de venta realizada
                double total = calcularTotal();
                mostrarNotificacionVenta(true,
                        String.format("VENTA REALIZADA<br/>Total: $%.2f<br/>Método: %s", total, metodoPago));

                // Si eligió imprimir, proceder con la impresión
                if (opcionImprimir == JOptionPane.YES_OPTION) {
                    try {
                        PrinterJob job = PrinterJob.getPrinterJob();
                        job.setJobName("Ticket de Venta - Tienda JC");

                        job.setPrintable(new Printable() {
                            @Override
                            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                                    throws PrinterException {
                                if (pageIndex > 0) {
                                    return NO_SUCH_PAGE;
                                }

                                Graphics2D g2d = (Graphics2D) graphics;
                                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                                Font font = new Font("Monospaced", Font.PLAIN, 9);
                                g2d.setFont(font);

                                String[] lines = ticketContent.toString().split("\n");

                                int y = 15;
                                for (String line : lines) {
                                    g2d.drawString(line, 10, y);
                                    y += 12;
                                }

                                g2d.drawString("Método de pago: " + metodoPago, 10, y + 10);

                                return PAGE_EXISTS;
                            }
                        });

                        if (job.printDialog()) {
                            job.print();
                        }
                    } catch (PrinterException ex) {
                        JOptionPane.showMessageDialog(this,
                                "La venta fue registrada pero hubo un error al imprimir: " + ex.getMessage(),
                                "Aviso",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }

                // Limpiar carrito después de 2.5 segundos (cuando desaparezca la notificación)
                Timer timer = new Timer(2500, e -> {
                    modeloCarrito.setRowCount(0);
                    actualizarTotal();
                    txtCodigo.requestFocus();
                });
                timer.setRepeats(false);
                timer.start();
            }

        } catch (Exception ex) {
            mostrarNotificacionVenta(false, "Error al procesar venta:<br/>" + ex.getMessage());
        }
    }

    private double calcularTotal() {
        double total = 0;
        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            Object val = modeloCarrito.getValueAt(i, 4);
            if (val instanceof Number) {
                total += ((Number) val).doubleValue();
            }
        }
        return total;
    }

    private boolean guardarVentaEnBD(String metodoPago) {
        Connection conn = null;
        PreparedStatement psVenta = null;
        PreparedStatement psDetalle = null;
        PreparedStatement psUpdateStock = null;
        ResultSet rs = null;

        try {
            conn = ConexionBD.conectar();
            if (conn == null) {
                mostrarNotificacionVenta(false, "Error: No hay conexión a la base de datos");
                return false;
            }

            conn.setAutoCommit(false);

            String sqlVenta = "INSERT INTO Ventas (fecha, hora, metodo_pago, total) VALUES (CURDATE(), CURTIME(), ?, ?)";
            psVenta = conn.prepareStatement(sqlVenta, PreparedStatement.RETURN_GENERATED_KEYS);
            psVenta.setString(1, metodoPago);
            double totalVenta = calcularTotal();
            psVenta.setDouble(2, totalVenta);
            psVenta.executeUpdate();

            rs = psVenta.getGeneratedKeys();
            int idVenta = 0;
            if (rs.next()) {
                idVenta = rs.getInt(1);
            }

            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                String codigoProducto = modeloCarrito.getValueAt(i, 0).toString();
                int cantidad = (int) modeloCarrito.getValueAt(i, 1);
                String nombreProducto = modeloCarrito.getValueAt(i, 2).toString();
                double precio = (double) modeloCarrito.getValueAt(i, 3);

                String[] tablasInventario = { "InventarioConsumibles", "InventarioDesechables", "InventarioLimpieza",
                        "InventarioAgricolas" };
                String[] tablasDetalle = { "DetalleVentaConsumibles", "DetalleVentaDesechables", "DetalleVentaLimpieza",
                        "DetalleVentaAgricolas" };

                for (int j = 0; j < tablasInventario.length; j++) {
                    String sqlCheck = "SELECT COUNT(*) FROM " + tablasInventario[j] + " WHERE id_producto = ?";
                    try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                        psCheck.setString(1, codigoProducto);
                        ResultSet rsCheck = psCheck.executeQuery();
                        if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                            String sqlDetalle = "INSERT INTO " + tablasDetalle[j] +
                                    " (id_venta, id_producto, nombre_producto, cantidad, precio_unitario) " +
                                    "VALUES (?, ?, ?, ?, ?)";
                            psDetalle = conn.prepareStatement(sqlDetalle);
                            psDetalle.setInt(1, idVenta);
                            psDetalle.setString(2, codigoProducto);
                            psDetalle.setString(3, nombreProducto);
                            psDetalle.setInt(4, cantidad);
                            psDetalle.setDouble(5, precio);
                            psDetalle.executeUpdate();

                            String sqlUpdate = "UPDATE " + tablasInventario[j] +
                                    " SET cantidad = cantidad - ? WHERE id_producto = ?";
                            psUpdateStock = conn.prepareStatement(sqlUpdate);
                            psUpdateStock.setInt(1, cantidad);
                            psUpdateStock.setString(2, codigoProducto);
                            psUpdateStock.executeUpdate();

                            break;
                        }
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException ex) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            mostrarNotificacionVenta(false, "Error al guardar venta: " + ex.getMessage());
            return false;
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (psVenta != null)
                    psVenta.close();
                if (psDetalle != null)
                    psDetalle.close();
                if (psUpdateStock != null)
                    psUpdateStock.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void procesarCobro() {
        if (modeloCarrito.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        imprimirTicket();
    }

    // ====================================================================
    // MÓDULO DE CONSULTA DE PRODUCTOS (MEJORADO)
    // ====================================================================
    private JPanel crearPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        modeloProductos = new DefaultTableModel(new String[] { "Código", "Descripción", "Precio", "Stock", "Tipo" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaProductos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaProductos.setRowHeight(25);

        // Agregar sorter a la tabla
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modeloProductos);
        tablaProductos.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(tablaProductos);
        panel.add(scroll, BorderLayout.CENTER);

        // Panel superior con controles
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.setBackground(BTN_ACCENT);
        btnRefrescar.setForeground(Color.WHITE);
        btnRefrescar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefrescar.addActionListener(e -> cargarProductos());

        // Campo de búsqueda
        JTextField txtBuscar = new JTextField(20);
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        top.add(btnRefrescar);
        top.add(new JLabel("Buscar: "));
        top.add(txtBuscar);

        // Listener para filtrar en tiempo real
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }

            private void filtrar() {
                String texto = txtBuscar.getText().trim();
                if (texto.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
                }
            }
        });

        panel.add(top, BorderLayout.NORTH);

        if (conexion != null) {
            cargarProductos();
        }

        return panel;
    }

    private void cargarProductos() {
        modeloProductos.setRowCount(0);
        if (conexion == null)
            return;

        String[] tablas = { "InventarioConsumibles", "InventarioDesechables", "InventarioLimpieza",
                "InventarioAgricolas" };
        String[] tipos = { "Consumible", "Desechable", "Limpieza", "Agrícola" };

        try {
            for (int i = 0; i < tablas.length; i++) {
                String sql = "SELECT id_producto, nombre_producto, precio_unitario, cantidad FROM " + tablas[i];
                try (PreparedStatement ps = conexion.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Object codigo = rs.getObject("id_producto");
                        Object descripcion = rs.getObject("nombre_producto");
                        Object precio = rs.getObject("precio_unitario");
                        Object stock = rs.getObject("cantidad");
                        modeloProductos.addRow(new Object[] { codigo, descripcion, precio, stock, tipos[i] });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar productos: " + e.getMessage());
        }
    }

    // ====================================================================
    // MÓDULO DE GESTIÓN DE PEDIDOS (NUEVO)
    // ====================================================================
    private JPanel crearPanelPedidos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tabla de pedidos
        tablaPedidos = new JTable(modeloPedidos);
        tablaPedidos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaPedidos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaPedidos.setRowHeight(25);

        JScrollPane scroll = new JScrollPane(tablaPedidos);
        panel.add(scroll, BorderLayout.CENTER);

        // Panel superior con controles
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Campo de búsqueda
        JTextField txtBuscarPedido = new JTextField(20);
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setBackground(BTN_PRIMARY);
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnRefrescar = new JButton("Actualizar");
        btnRefrescar.setBackground(BTN_SECONDARY);
        btnRefrescar.setForeground(Color.WHITE);
        btnRefrescar.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnVerDetalles = new JButton("Ver Detalles");
        btnVerDetalles.setBackground(BTN_ACCENT);
        btnVerDetalles.setForeground(Color.WHITE);
        btnVerDetalles.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnNuevoPedido = new JButton("Nuevo Pedido");
        btnNuevoPedido.setBackground(BTN_ACCENT);
        btnNuevoPedido.setForeground(Color.WHITE);
        btnNuevoPedido.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnCambiarEstado = new JButton("Cambiar Estado");
        btnCambiarEstado.setBackground(BTN_DARK);
        btnCambiarEstado.setForeground(Color.WHITE);
        btnCambiarEstado.setFont(new Font("Segoe UI", Font.BOLD, 12));

        top.add(new JLabel("Buscar: "));
        top.add(txtBuscarPedido);
        top.add(btnBuscar);
        top.add(btnRefrescar);
        top.add(btnVerDetalles);
        top.add(btnNuevoPedido);
        top.add(btnCambiarEstado);

        panel.add(top, BorderLayout.NORTH);

        // Configurar eventos
        btnBuscar.addActionListener(e -> cargarPedidos(txtBuscarPedido.getText().trim()));
        btnRefrescar.addActionListener(e -> {
            txtBuscarPedido.setText("");
            cargarPedidos("");
        });
        btnVerDetalles.addActionListener(e -> verDetallesPedido());
        btnNuevoPedido.addActionListener(e -> abrirNuevoPedido());
        btnCambiarEstado.addActionListener(e -> cambiarEstadoPedido());

        // Búsqueda en tiempo real
        txtBuscarPedido.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                cargarPedidos(txtBuscarPedido.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                cargarPedidos(txtBuscarPedido.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                cargarPedidos(txtBuscarPedido.getText());
            }
        });

        return panel;
    }

    private void cargarPedidos(String filtro) {
        modeloPedidos.setRowCount(0);

        if (conexion == null)
            return;

        String sql = "SELECT * FROM pedidos WHERE " +
                "nombre_cliente LIKE ? OR " +
                "estado LIKE ? OR " +
                "id_pedido LIKE ? " +
                "ORDER BY id_pedido DESC";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            String f = "%" + filtro + "%";
            ps.setString(1, f);
            ps.setString(2, f);
            ps.setString(3, f);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                modeloPedidos.addRow(new Object[] {
                        rs.getInt("id_pedido"),
                        rs.getString("nombre_cliente"),
                        rs.getString("fecha"),
                        rs.getString("estado"),
                        rs.getDouble("saldo_pendiente")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar pedidos: " + e.getMessage());
        }
    }

    private void verDetallesPedido() {
        int fila = tablaPedidos.getSelectedRow();

        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un pedido de la lista.");
            return;
        }

        int idPedido = (int) modeloPedidos.getValueAt(fila, 0);

        DetallesPedidoDialog dialog = new DetallesPedidoDialog(idPedido, conexion);
        dialog.setVisible(true);
    }

    private void abrirNuevoPedido() {
        NuevoPedidoDialog dialog = new NuevoPedidoDialog(this, conexion);
        dialog.setVisible(true);

        // Refrescar la tabla después de cerrar el diálogo
        cargarPedidos("");
    }

    private void cambiarEstadoPedido() {
        int fila = tablaPedidos.getSelectedRow();

        if (fila == -1) {
            JOptionPane.showMessageDialog(this,
                    "Selecciona un pedido de la tabla.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idPedido = (int) modeloPedidos.getValueAt(fila, 0);

        String[] opciones = { "Pendiente", "Entregado", "Retrasado", "Pagado" };

        String nuevoEstado = (String) JOptionPane.showInputDialog(
                this,
                "Selecciona el nuevo estado:",
                "Cambiar estado",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (nuevoEstado == null)
            return;

        actualizarEstadoEnBD(idPedido, nuevoEstado);
        cargarPedidos("");
    }

    private void actualizarEstadoEnBD(int idPedido, String estado) {
        String sql = "UPDATE pedidos SET estado = ? WHERE id_pedido = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Estado actualizado correctamente.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al actualizar estado: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton crearBotonAccion(String texto, Color color) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boton.setBackground(color);
        boton.setForeground(isLight(color) ? Color.BLACK : Color.WHITE);
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        boton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                Color hover = color.brighter();
                boton.setBackground(hover);
                boton.setForeground(isLight(hover) ? Color.BLACK : Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                boton.setBackground(color);
                boton.setForeground(isLight(color) ? Color.BLACK : Color.WHITE);
            }
        });

        return boton;
    }

    private void mostrarReportes() {
        JOptionPane.showMessageDialog(this,
                "Módulo de Reportes - Próximamente\n\n" +
                        "• Ventas del día\n" +
                        "• Productos más vendidos\n" +
                        "• Inventario bajo\n" +
                        "• Reportes financieros",
                "Reportes",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void cerrarSesion() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres cerrar sesión?",
                "Cerrar Sesión",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            prueba p = new prueba();
            p.setVisible(true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                // Silenciar
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
            new VentanaCajero().setVisible(true);
        });
    }
}