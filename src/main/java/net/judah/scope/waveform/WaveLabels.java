package net.judah.scope.waveform;

import java.util.ArrayList;
import java.util.List;

public class WaveLabels {

    public static List<String> generateLabels(long totalDurationMillis) {
        List<String> labels = new ArrayList<>();
        long[] intervals = { 60000, 30000, 10000, 5000, 1000, 100 };

        for (long interval : intervals) {
            if (totalDurationMillis / interval <= 10) {
                for (long t = 0; t <= totalDurationMillis; t += interval) {
                    labels.add(formatTime(t));
                }
                break;
            }
        }

        return labels;
    }

    private static String formatTime(long millis) {
        long hours = (millis / 3600000) % 24;
        long minutes = (millis / 60000) % 60;
        long seconds = (millis / 1000) % 60;
        long milliseconds = millis % 1000;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
        } else if (minutes > 0) {
            return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
        } else {
            return String.format("%02d.%03d", seconds, milliseconds);
        }
    }

    public static void main(String[] args) {
        long totalDurationMillis = 123456; // Example duration
        List<String> labels = generateLabels(totalDurationMillis);
        for (String label : labels) {
            System.out.println(label);
        }
    }
}