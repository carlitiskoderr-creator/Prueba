
import java.awt.Color;
import java.awt.GradientPaint;

public class PaletaColores {
    public static final Color PRIMARY = new Color(0xEE, 0x45, 0x40); // #EE4540
    public static final Color SECONDARY = new Color(0xC7, 0x2C, 0x41); // #C72C41
    public static final Color TERTIARY = new Color(0x80, 0x13, 0x36); // #801336
    public static final Color ACCENT = new Color(0x51, 0x0A, 0x32); // #510A32
    public static final Color BACKGROUND = new Color(0x2D, 0x14, 0x2C); // #2D142C

    public static final Color TEXT_ON_DARK = Color.WHITE;
    public static final Color TEXT_ON_LIGHT = Color.BLACK;

    public static final Color GRADIENT_START = PRIMARY;
    public static final Color GRADIENT_END = BACKGROUND;

    private PaletaColores() {
    }

    public static GradientPaint gradientFor(int width, int height) {
        return new GradientPaint(0, 0, GRADIENT_START, width, height, GRADIENT_END);
    }
}
