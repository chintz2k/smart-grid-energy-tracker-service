package com.energytracker.influx.util;

import org.springframework.stereotype.Service;

/**
 * @author André Heinen
 */
@Service
public class ChartColorPicker {

	public ChartColorPicker() {

	}

	public String generateColor(int index, int total, double alpha) {
		double hue = (index * 360.0 / total) % 360; // Gleichmäßige Verteilung auf 360 Grad
		double saturation = 70.0; // Sättigung: 70 %
		double lightness = 50.0; // Helligkeit: 50 %

		// HSL -> RGB Konvertierung
		double c = (1 - Math.abs(2 * lightness / 100.0 - 1)) * saturation / 100.0;
		double x = c * (1 - Math.abs((hue / 60.0) % 2 - 1));
		double m = lightness / 100.0 - c / 2;

		double r, g, b;
		if (hue < 60) {
			r = c; g = x; b = 0;
		} else if (hue < 120) {
			r = x; g = c; b = 0;
		} else if (hue < 180) {
			r = 0; g = c; b = x;
		} else if (hue < 240) {
			r = 0; g = x; b = c;
		} else if (hue < 300) {
			r = x; g = 0; b = c;
		} else {
			r = c; g = 0; b = x;
		}

		// Umrechnen von [0,1]-Bereich auf [0,255]-Bereich und Alpha anwenden
		int red = (int) ((r + m) * 255);
		int green = (int) ((g + m) * 255);
		int blue = (int) ((b + m) * 255);

		return String.format("rgba(%d, %d, %d, %.1f)", red, green, blue, alpha);
	}
}
