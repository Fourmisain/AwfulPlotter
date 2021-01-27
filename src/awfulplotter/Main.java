package awfulplotter;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {
        AwfulPlotter plotter = new AwfulPlotter();

        // simple example plot
        double r0 = 0.75;
        double r1 = 0.25;
        plotter.plot((x) -> r0 + SmoothNoise.smoothstep(x) * (r1 - r0));

        JFrame frame = AwfulPlotter.createPlotterFrame(plotter);

        // adding GUI elements
        SwingUtilities.invokeAndWait(() -> {
            JButton clear = new JButton("Clear Plots");
            clear.addActionListener((e) -> plotter.clearPlots());
            frame.getContentPane().add(clear);

            // adds a complex state driven MathFunction
            JButton noise = new JButton("Add Noise Function");
            noise.addActionListener((e) -> plotter.plot(new SmoothNoise()));
            frame.getContentPane().add(noise);
        });

        // convenience method
        AwfulPlotter.makeFrameVisible(frame);
    }
}
