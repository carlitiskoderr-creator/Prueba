import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class prueba extends JFrame {

    private JButton btnUsuario;
    private JPasswordField txtContrasena;
    private JButton btnIngresar;
    private String[] tiposUsuario = { "Dueño", "Cajero", "Contador" };
    private int indiceUsuario = 0;

    public prueba() {
        setTitle("La Jacarandita");
        setSize(380, 460);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradiente = new GradientPaint(
                        0, 0, PaletaColores.GRADIENT_START,
                        getWidth(), getHeight(), PaletaColores.GRADIENT_END);
                g2d.setPaint(gradiente);
                g2d.fillRect(0, 0, getWidth(), getHeight());

            }
        };
        panel.setLayout(null);

        add(panel);

        JLabel lblTitulo = new JLabel("Bienvenido");
        lblTitulo.setFont(new Font("Comic Sans MS", Font.BOLD, 28));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBounds(125, 30, 200, 40);
        panel.add(lblTitulo);

        JLabel lblUsuario = new JLabel("Usuario");
        lblUsuario.setFont(new Font("Comic Sans MS", Font.PLAIN, 13));
        lblUsuario.setForeground(Color.WHITE);
        lblUsuario.setBounds(80, 100, 100, 20);
        panel.add(lblUsuario);

        // Botón para cambiar tipo de usuario
        btnUsuario = new JButton(tiposUsuario[indiceUsuario]);
        btnUsuario.setBounds(80, 125, 220, 35);
        btnUsuario.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnUsuario.setForeground(Color.DARK_GRAY);
        btnUsuario.setBackground(new Color(190, 140, 140));
        btnUsuario.setFocusPainted(false);
        btnUsuario.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        btnUsuario.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(btnUsuario);

        // Acción para cambiar entre tipos de usuario
        btnUsuario.addActionListener(e -> {
            indiceUsuario = (indiceUsuario + 1) % tiposUsuario.length;
            btnUsuario.setText(tiposUsuario[indiceUsuario]);
        });

        JLabel lblContrasena = new JLabel("Contraseña");
        lblContrasena.setFont(new Font("Comic Sans MS", Font.PLAIN, 13));
        lblContrasena.setForeground(Color.WHITE);
        lblContrasena.setBounds(80, 170, 100, 20);
        panel.add(lblContrasena);

        txtContrasena = new JPasswordField();
        txtContrasena.addActionListener(e -> validarLogin());
        txtContrasena.setBounds(80, 195, 220, 35);
        txtContrasena.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtContrasena.setHorizontalAlignment(JTextField.CENTER);
        panel.add(txtContrasena);

        btnIngresar = new JButton("Ingresar");
        btnIngresar.setBounds(120, 250, 140, 40);
        btnIngresar.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setBackground(new Color(50, 50, 50));
        btnIngresar.setFocusPainted(false);
        panel.add(btnIngresar);

        btnIngresar.addActionListener(e -> validarLogin());
    }

    public void mostrarConfirmacionTemporal(String mensaje) {
    }

    private void validarLogin() {
        String usuario = tiposUsuario[indiceUsuario];
        String contrasena = new String(txtContrasena.getPassword());

        // Validar que se haya ingresado una contraseña
        if (contrasena.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese la contraseña", "Error",
                    JOptionPane.WARNING_MESSAGE);
            txtContrasena.requestFocus();
            return;
        }

        try (Connection conn = ConexionBD.conectar()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar con la base de datos");
                return;
            }

            String sql = "SELECT rol FROM usuarios WHERE rol=? AND contrasena=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, usuario);
            ps.setString(2, contrasena);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String rol = rs.getString("rol");

                if (rol.equalsIgnoreCase("Dueño")) {
                    new VentanaDueño().setVisible(true);
                } else if (rol.equalsIgnoreCase("Cajero")) {
                    new VentanaCajero().setVisible(true);
                } else if (rol.equalsIgnoreCase("Contador")) {
                    new VentanaContador().setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Rol desconocido: " + rol);
                    return;
                }

                dispose();
            } else {
                mostrarError();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error en la consulta: " + e.getMessage());
        }
    }

    private void mostrarError() {
        JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos", "Error", JOptionPane.ERROR_MESSAGE);
        txtContrasena.setText("");
        txtContrasena.requestFocus();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> new prueba().setVisible(true));
    }
}