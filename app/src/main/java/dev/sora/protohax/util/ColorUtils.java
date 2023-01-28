package dev.sora.protohax.util;

public class ColorUtils {
    public static int as() {
        int[] counter;
        int[] arrn = counter = new int[]{0};
        arrn[0] = arrn[0] + 1;
        return getRainbow3(counter[0] * 20);
    }

    public static int getRainbow3(int tick) {
        double d = 0;
        double delay = Math.ceil((double)((System.currentTimeMillis() + (long)(tick * 2)) / 5L));
        float rainbow = (double)((float)(d / 360.0)) < 0.5 ? -((float)(delay / 360.0)) : (float)((delay %= 360.0) / 360.0);
        return Color.HSBtoRGB(rainbow, 0.5f, 1.0f);
    }
    public static Color getChromaRainbow(double x, double y) {
        float v = 2000.0f;
        return new Color(Color.HSBtoRGB(((float)(((double)System.currentTimeMillis() - x * 10.0 * 1 - y * 10.0 * 1) % (double)v) / v), 0.8f, 1f));
    }
    public static Color getGradientOffset(final Color color1, final Color color2, final double index) {
        double offs = (Math.abs(((System.currentTimeMillis()) / 16D)) / 60D) + index;
        if(offs >1)
        {
            double left = offs % 1;
            int off = (int) offs;
            offs = off % 2 == 0 ? left : 1 - left;
        }

        final double inverse_percent = 1 - offs;
        int redPart = (int) (color1.r * inverse_percent + color2.r * offs);
        int greenPart = (int) (color1.g * inverse_percent + color2.g * offs);
        int bluePart = (int) (color1.b * inverse_percent + color2.b * offs);
        return new Color(redPart, greenPart, bluePart);
    }

}
