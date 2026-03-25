package thederpgamer.edencore.utils;

import org.newdawn.slick.Color;

import javax.vecmath.Vector4f;

public class ColorUtils {
	public static final char TRANSPARENT = '0';
	public static final char WHITE = '1';
	public static final char LIGHT_GREY = '2';
	public static final char GREY = '3';
	public static final char DARK_GREY = '4';
	public static final char BLACK = '5';
	public static final char YELLOW = 'y';
	public static final char ORANGE = 'o';
	public static final char RED = 'r';
	public static final char MAGENTA = 'm';
	public static final char PINK = 'p';
	public static final char BLUE = 'b';
	public static final char CYAN = 'c';
	public static final char GREEN = 'g';

	public static Vector4f toVector4f(Color color) {
		float r = (float) color.getRed() / 255;
		float g = (float) color.getGreen() / 255;
		float b = (float) color.getBlue() / 255;
		float a = (float) color.getAlpha() / 255;
		return new Vector4f(r, g, b, a);
	}

	public static Color fromCode(char code) {
		switch(code) {
			case TRANSPARENT:
				return Color.transparent;
			case WHITE:
				return Color.white;
			case LIGHT_GREY:
				return Color.lightGray;
			case GREY:
				return Color.gray;
			case DARK_GREY:
				return Color.darkGray;
			case BLACK:
				return Color.black;
			case YELLOW:
				return Color.yellow;
			case ORANGE:
				return Color.orange;
			case RED:
				return Color.red;
			case MAGENTA:
				return Color.magenta;
			case PINK:
				return Color.pink;
			case BLUE:
				return Color.blue;
			case CYAN:
				return Color.cyan;
			case GREEN:
				return Color.green;
			default:
				return Color.white;
		}
	}
}