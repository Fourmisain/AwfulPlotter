package awfulplotter;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.MouseInputListener;

import static java.awt.RenderingHints.*;
import static java.awt.event.InputEvent.*;

public class AwfulPlotter extends JPanel implements MouseInputListener, MouseWheelListener, ComponentListener {

	public static final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

	public static JFrame createPlotterFrame(AwfulPlotter plotter) throws ExecutionException, InterruptedException {
		RunnableFuture<JFrame> rf = new FutureTask<>(() -> {
			JFrame win = new JFrame("AwfulPlotter");
			win.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			win.setLocationByPlatform(true);
			win.setContentPane(plotter);
			return win;
		});

		SwingUtilities.invokeLater(rf);

		return rf.get();
	}

	public static void makeFrameVisible(JFrame frame) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(() -> {
			frame.pack();
			frame.setVisible(true);
		});
	}

	// basically a generator interface
	public interface DrawInstructor {
		void provideInstructions(Consumer<Pair<Double, DrawInstruction>> consumer, double minX, double maxX);
	}

	public static final int CIRCLE_RADIUS = 3;

	public enum DrawInstruction {
		FILL_CIRCLE,
		DRAW_CIRCLE
	}

	public static void fillCircle(Graphics2D g2d, double x, double y, double r) {
		g2d.fill(new Ellipse2D.Double(x-r, y-r, 2*r, 2*r));
	}

	public static void drawCircle(Graphics2D g2d, double x, double y, double r) {
		g2d.draw(new Ellipse2D.Double(x-r, y-r, 2*r, 2*r));
	}

	protected static class Plot {
		MathFunction f;
		String name;
		Color color;
	}

	protected final List<Plot> plots = new ArrayList<>();
	protected final List<Color> colors = List.of(Color.BLUE, new Color(206, 140, 101), Color.RED, new Color(64, 128, 64));

	protected double xUnit = 50; // unit in pixels
	protected double yUnit = 50; // unit in pixels

	protected int w, h;
	protected int xOffset, yOffset;
	protected int mouseX, mouseY;

	public AwfulPlotter() throws Exception {
		this(800, 800);
	}

	public AwfulPlotter(int width, int height) throws Exception {
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addComponentListener(this);

		w = width;
		h = height;
		mouseX = xOffset = w / 2;
		mouseY = yOffset = h / 2;

		SwingUtilities.invokeAndWait(() -> {
			setPreferredSize(new Dimension(w, h));
			setBackground(Color.WHITE);
		});
	}

	public synchronized void plot(MathFunction f) {
		plot(f, colors.get(plots.size() % colors.size()));
	}

	public synchronized void plot(MathFunction f, Color color) {
		plot(f, color, "f" + (plots.size() + 1));
	}

	public synchronized void plot(MathFunction f, Color color, String name) {
		Plot p = new Plot();
		p.f = f;
		p.name = name;
		p.color = color;
		plots.add(p);
		repaint();
	}

	public synchronized void clearPlots() {
		plots.clear();
		repaint();
	}

	public int toXPixel(double x) {
		return (int)Math.round(xUnit * x) + xOffset;
	}

	public double fromXPixel(int x) {
		return (x - xOffset) / xUnit;
	}

	public int toYPixel(double y) {
		return (int)Math.round(yUnit * -y + yOffset);
	}

	public double fromYPixel(int y) {
		return (-y + yOffset) / yUnit;
	}

	@Override public void mousePressed(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();
	}
	@Override public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
			xOffset = w/2;
			yOffset = h/2;
			repaint();
			return;
		}

		for (Plot p : plots)
			System.out.printf("%s(%.2f) = %.2f%n", p.name, fromXPixel(mouseX), p.f.apply(fromXPixel(mouseX)));
	}

	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

	@Override public void mouseDragged(MouseEvent e) {
		int dx = e.getX() - mouseX;
		int dy = e.getY() - mouseY;
		mouseX = e.getX();
		mouseY = e.getY();

		xOffset += dx;
		yOffset += dy;
		repaint();
	}

	@Override public void mouseMoved(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();
		repaint();
	}

	@Override public void mouseWheelMoved(MouseWheelEvent e) {
		// e.getWheelRotation() can be (-)127 when "left scrolling", so we simply ignore the number of clicks
		int direction = -(int)Math.signum(e.getWheelRotation());
		double xScale = 1;
		double yScale = 1;

		if ((e.getModifiersEx() & CTRL_DOWN_MASK) != 0) {
			xScale = Math.sqrt(2);
		} else if ((e.getModifiersEx() & SHIFT_DOWN_MASK) != 0) {
			yScale = Math.sqrt(2);
		} else {
			xScale = yScale = Math.sqrt(2);
		}
		xScale = direction > 0 ? xScale : 1 / xScale;
		yScale = direction > 0 ? yScale : 1 / yScale;

		xUnit *= xScale;
		yUnit *= yScale;

		// use mouse position as zoom fix point
		double x0 = mouseX;
		double y0 = mouseY;
		xOffset = (int)Math.round(x0 - xScale * (x0 - xOffset));
		yOffset = (int)Math.round(y0 - yScale * (y0 - yOffset));

		repaint();
	}

	@Override public void componentResized(ComponentEvent e) {
		xOffset += (getWidth()  - w) / 2.0;
		yOffset += (getHeight() - h) / 2.0;

		w = getWidth();
		h = getHeight();
	}

	@Override public void componentMoved(ComponentEvent e) { }
	@Override public void componentShown(ComponentEvent e) { }
	@Override public void componentHidden(ComponentEvent e) { }

	@Override public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setFont(font);

		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

		int stringYOffset = 0;
		g.drawString(String.format("xUnit: %.2f px", xUnit), 12, stringYOffset += 16);
		g.drawString(String.format("yUnit: %.2f px", yUnit), 12, stringYOffset += 16);
		g.drawString(String.format("xOffset: %d px", xOffset), 12, stringYOffset += 16);
		g.drawString(String.format("yOffset: %d px", yOffset), 12, stringYOffset += 16);
		g.drawString(String.format("Origin: (%d, %d) px", toXPixel(0), toYPixel(0)), 12, stringYOffset += 16);
		g.drawString(String.format("MouseX: %4d px -> %6.2f", mouseX, fromXPixel(mouseX)), 12, stringYOffset += 16);
		g.drawString(String.format("MouseY: %4d px -> %6.2f", mouseY, fromYPixel(mouseY)), 12, stringYOffset += 16);

		int x0 = toXPixel(0);
		int y0 = toYPixel(0);

		// x-axis
		g.drawLine(0, y0, w, y0);
		// x unit markers
		for (int i = 0; i <= w/xUnit+1; i++)
			g.drawLine((int)Math.round(x0 % xUnit + i*xUnit), y0-4, (int)Math.round(x0 % xUnit + i*xUnit), y0+4);

		// y-axis
		g.drawLine(x0, 0, x0, h);
		// y unit markers
		for (int i = 0; i <= h/yUnit+1; i++)
			g.drawLine(x0-4, (int)Math.round(y0 % yUnit + i*yUnit), x0+4, (int)Math.round(y0 % yUnit + i*yUnit));

		for (Plot p : plots) {
			g.setColor(p.color);

			// plot points
			Path2D path = new Path2D.Double();
			path.moveTo(0, toYPixel(p.f.apply(fromXPixel(0))));
			for (int x = 1; x <= w; x++)
				path.lineTo(x, toYPixel(p.f.apply(fromXPixel(x))));
			g2d.draw(path);

			if (p.f instanceof DrawInstructor) {
				DrawInstructor drawInstructor = (DrawInstructor)p.f;

				// execute custom draw instructions
				drawInstructor.provideInstructions((pair) -> {
					double x = pair.getKey(); // in function domain

					switch (pair.getValue()) {
						case FILL_CIRCLE:
							fillCircle(g2d, toXPixel(x), toYPixel(p.f.apply(x)), CIRCLE_RADIUS);
							break;
						case DRAW_CIRCLE:
							drawCircle(g2d, toXPixel(x), toYPixel(p.f.apply(x)), CIRCLE_RADIUS);
							break;
					}
				}, fromXPixel(-CIRCLE_RADIUS), fromXPixel(w-1 + CIRCLE_RADIUS));
			}
		}
	}
}