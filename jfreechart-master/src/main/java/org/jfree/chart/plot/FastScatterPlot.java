/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2022, by David Gilbert and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates. 
 * Other names may be trademarks of their respective owners.]
 *
 * --------------------
 * FastScatterPlot.java
 * --------------------
 * (C) Copyright 2002-2022, by David Gilbert.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   Arnaud Lelievre;
 *                   Ulrich Voigt (patch #307);
 *
 */

package org.jfree.chart.plot;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import org.jfree.chart.ChartElementVisitor;

import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.api.RectangleEdge;
import org.jfree.chart.api.RectangleInsets;
import org.jfree.chart.internal.ArrayUtils;
import org.jfree.chart.internal.PaintUtils;
import org.jfree.chart.internal.Args;
import org.jfree.chart.internal.SerialUtils;
import org.jfree.data.Range;

/**
 * A fast scatter plot.
 */
public class FastScatterPlot extends Plot implements ValueAxisPlot, Pannable,
        Zoomable, Cloneable, Serializable {

    private FastScatterPlotProduct fastScatterPlotProduct = new FastScatterPlotProduct();

	/** For serialization. */
    private static final long serialVersionUID = 7871545897358563521L;

    /** The default grid line stroke. */
    public static final Stroke DEFAULT_GRIDLINE_STROKE = new BasicStroke(0.5f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[]
            {2.0f, 2.0f}, 0.0f);

    /** The default grid line paint. */
    public static final Paint DEFAULT_GRIDLINE_PAINT = Color.lightGray;

    /** The data. */
    private float[][] data;

    /** The x data range. */
    private final Range xDataRange;

    /** The y data range. */
    private final Range yDataRange;

    /** The domain axis (used for the x-values). */
    private ValueAxis domainAxis;

    /** The range axis (used for the y-values). */
    private ValueAxis rangeAxis;

    /**
     * A flag that controls whether or not panning is enabled for the domain
     * axis.
     */
    private boolean domainPannable;

    /**
     * A flag that controls whether or not panning is enabled for the range
     * axis.
     */
    private boolean rangePannable;

    /** The resourceBundle for the localization. */
    protected static ResourceBundle localizationResources
            = ResourceBundle.getBundle("org.jfree.chart.plot.LocalizationBundle");

    /**
     * Creates a new instance of {@code FastScatterPlot} with default
     * axes.
     */
    public FastScatterPlot() {
        this(null, new NumberAxis("X"), new NumberAxis("Y"));
    }

    /**
     * Creates a new fast scatter plot.
     * <p>
     * The data is an array of x, y values:  data[0][i] = x, data[1][i] = y.
     *
     * @param data  the data ({@code null} permitted).
     * @param domainAxis  the domain (x) axis ({@code null} not permitted).
     * @param rangeAxis  the range (y) axis ({@code null} not permitted).
     */
    public FastScatterPlot(float[][] data,
                           ValueAxis domainAxis, ValueAxis rangeAxis) {

        super();
        Args.nullNotPermitted(domainAxis, "domainAxis");
        Args.nullNotPermitted(rangeAxis, "rangeAxis");

        this.data = data;
        this.xDataRange = calculateXDataRange(data);
        this.yDataRange = calculateYDataRange(data);
        this.domainAxis = domainAxis;
        this.domainAxis.setPlot(this);
        this.domainAxis.addChangeListener(this);
        this.rangeAxis = rangeAxis;
        this.rangeAxis.setPlot(this);
        this.rangeAxis.addChangeListener(this);

        fastScatterPlotProduct.setPaint2(Color.RED);

        fastScatterPlotProduct.setDomainGridlinesVisible2(true);
        fastScatterPlotProduct.setDomainGridlinePaint2(FastScatterPlot.DEFAULT_GRIDLINE_PAINT);
        fastScatterPlotProduct.setDomainGridlineStroke2(FastScatterPlot.DEFAULT_GRIDLINE_STROKE);

        fastScatterPlotProduct.setRangeGridlinesVisible2(true);
        fastScatterPlotProduct.setRangeGridlinePaint2(FastScatterPlot.DEFAULT_GRIDLINE_PAINT);
        fastScatterPlotProduct.setRangeGridlineStroke2(FastScatterPlot.DEFAULT_GRIDLINE_STROKE);
    }

    /**
     * Returns a short string describing the plot type.
     *
     * @return A short string describing the plot type.
     */
    @Override
    public String getPlotType() {
        return localizationResources.getString("Fast_Scatter_Plot");
    }

    /**
     * Returns the data array used by the plot.
     *
     * @return The data array (possibly {@code null}).
     *
     * @see #setData(float[][])
     */
    public float[][] getData() {
        return this.data;
    }

    /**
     * Sets the data array used by the plot and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param data  the data array ({@code null} permitted).
     *
     * @see #getData()
     */
    public void setData(float[][] data) {
        this.data = data;
        fireChangeEvent();
    }

    /**
     * Returns the orientation of the plot.
     *
     * @return The orientation (always {@link PlotOrientation#VERTICAL}).
     */
    @Override
    public PlotOrientation getOrientation() {
        return PlotOrientation.VERTICAL;
    }

    /**
     * Returns the domain axis for the plot.
     *
     * @return The domain axis (never {@code null}).
     *
     * @see #setDomainAxis(ValueAxis)
     */
    public ValueAxis getDomainAxis() {
        return this.domainAxis;
    }

    /**
     * Sets the domain axis and sends a {@link PlotChangeEvent} to all
     * registered listeners.
     *
     * @param axis  the axis ({@code null} not permitted).
     *
     * @see #getDomainAxis()
     */
    public void setDomainAxis(ValueAxis axis) {
        Args.nullNotPermitted(axis, "axis");
        this.domainAxis = axis;
        fireChangeEvent();
    }

    /**
     * Returns the range axis for the plot.
     *
     * @return The range axis (never {@code null}).
     *
     * @see #setRangeAxis(ValueAxis)
     */
    public ValueAxis getRangeAxis() {
        return this.rangeAxis;
    }

    /**
     * Sets the range axis and sends a {@link PlotChangeEvent} to all
     * registered listeners.
     *
     * @param axis  the axis ({@code null} not permitted).
     *
     * @see #getRangeAxis()
     */
    public void setRangeAxis(ValueAxis axis) {
        Args.nullNotPermitted(axis, "axis");
        this.rangeAxis = axis;
        fireChangeEvent();
    }

    /**
     * Returns the paint used to plot data points.  The default is
     * {@code Color.RED}.
     *
     * @return The paint.
     *
     * @see #setPaint(Paint)
     */
    public Paint getPaint() {
        return this.fastScatterPlotProduct.getPaint();
    }

    /**
     * Sets the color for the data points and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param paint  the paint ({@code null} not permitted).
     *
     * @see #getPaint()
     */
    public void setPaint(Paint paint) {
        fastScatterPlotProduct.setPaint(paint, this);
    }

    /**
     * Returns {@code true} if the domain gridlines are visible, and
     * {@code false} otherwise.
     *
     * @return {@code true} or {@code false}.
     *
     * @see #setDomainGridlinesVisible(boolean)
     * @see #setDomainGridlinePaint(Paint)
     */
    public boolean isDomainGridlinesVisible() {
        return this.fastScatterPlotProduct.getDomainGridlinesVisible();
    }

    /**
     * Sets the flag that controls whether or not the domain grid-lines are
     * visible.  If the flag value is changed, a {@link PlotChangeEvent} is
     * sent to all registered listeners.
     *
     * @param visible  the new value of the flag.
     *
     * @see #getDomainGridlinePaint()
     */
    public void setDomainGridlinesVisible(boolean visible) {
        fastScatterPlotProduct.setDomainGridlinesVisible(visible, this);
    }

    /**
     * Returns the stroke for the grid-lines (if any) plotted against the
     * domain axis.
     *
     * @return The stroke (never {@code null}).
     *
     * @see #setDomainGridlineStroke(Stroke)
     */
    public Stroke getDomainGridlineStroke() {
        return this.fastScatterPlotProduct.getDomainGridlineStroke();
    }

    /**
     * Sets the stroke for the grid lines plotted against the domain axis and
     * sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param stroke  the stroke ({@code null} not permitted).
     *
     * @see #getDomainGridlineStroke()
     */
    public void setDomainGridlineStroke(Stroke stroke) {
        fastScatterPlotProduct.setDomainGridlineStroke(stroke, this);
    }

    /**
     * Returns the paint for the grid lines (if any) plotted against the domain
     * axis.
     *
     * @return The paint (never {@code null}).
     *
     * @see #setDomainGridlinePaint(Paint)
     */
    public Paint getDomainGridlinePaint() {
        return this.fastScatterPlotProduct.getDomainGridlinePaint();
    }

    /**
     * Sets the paint for the grid lines plotted against the domain axis and
     * sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param paint  the paint ({@code null} not permitted).
     *
     * @see #getDomainGridlinePaint()
     */
    public void setDomainGridlinePaint(Paint paint) {
        fastScatterPlotProduct.setDomainGridlinePaint(paint, this);
    }

    /**
     * Returns {@code true} if the range axis grid is visible, and
     * {@code false} otherwise.
     *
     * @return {@code true} or {@code false}.
     *
     * @see #setRangeGridlinesVisible(boolean)
     */
    public boolean isRangeGridlinesVisible() {
        return this.fastScatterPlotProduct.getRangeGridlinesVisible();
    }

    /**
     * Sets the flag that controls whether or not the range axis grid lines are
     * visible.  If the flag value is changed, a {@link PlotChangeEvent} is
     * sent to all registered listeners.
     *
     * @param visible  the new value of the flag.
     *
     * @see #isRangeGridlinesVisible()
     */
    public void setRangeGridlinesVisible(boolean visible) {
        fastScatterPlotProduct.setRangeGridlinesVisible(visible, this);
    }

    /**
     * Returns the stroke for the grid lines (if any) plotted against the range
     * axis.
     *
     * @return The stroke (never {@code null}).
     *
     * @see #setRangeGridlineStroke(Stroke)
     */
    public Stroke getRangeGridlineStroke() {
        return this.fastScatterPlotProduct.getRangeGridlineStroke();
    }

    /**
     * Sets the stroke for the grid lines plotted against the range axis and
     * sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param stroke  the stroke ({@code null} permitted).
     *
     * @see #getRangeGridlineStroke()
     */
    public void setRangeGridlineStroke(Stroke stroke) {
        fastScatterPlotProduct.setRangeGridlineStroke(stroke, this);
    }

    /**
     * Returns the paint for the grid lines (if any) plotted against the range
     * axis.
     *
     * @return The paint (never {@code null}).
     *
     * @see #setRangeGridlinePaint(Paint)
     */
    public Paint getRangeGridlinePaint() {
        return this.fastScatterPlotProduct.getRangeGridlinePaint();
    }

    /**
     * Sets the paint for the grid lines plotted against the range axis and
     * sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param paint  the paint ({@code null} not permitted).
     *
     * @see #getRangeGridlinePaint()
     */
    public void setRangeGridlinePaint(Paint paint) {
        fastScatterPlotProduct.setRangeGridlinePaint(paint, this);
    }

    /**
     * Receives a chart element visitor.
     * 
     * @param visitor  the visitor ({@code null} not permitted).
     */
    @Override
    public void receive(ChartElementVisitor visitor) {
        this.domainAxis.receive(visitor);
        this.rangeAxis.receive(visitor);
        super.receive(visitor);
    }

    /**
     * Draws the fast scatter plot on a Java 2D graphics device (such as the
     * screen or a printer).
     *
     * @param g2  the graphics device.
     * @param area   the area within which the plot (including axis labels)
     *                   should be drawn.
     * @param anchor  the anchor point ({@code null} permitted).
     * @param parentState  the state from the parent plot (ignored).
     * @param info  collects chart drawing information ({@code null}
     *              permitted).
     */
    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
                     PlotState parentState, PlotRenderingInfo info) {

        // set up info collection...
        if (info != null) {
            info.setPlotArea(area);
        }

        // adjust the drawing area for plot insets (if any)...
        RectangleInsets insets = getInsets();
        insets.trim(area);

        AxisSpace space = new AxisSpace();
        space = this.domainAxis.reserveSpace(g2, this, area,
                RectangleEdge.BOTTOM, space);
        space = this.rangeAxis.reserveSpace(g2, this, area, RectangleEdge.LEFT,
                space);
        Rectangle2D dataArea = space.shrink(area, null);

        if (info != null) {
            info.setDataArea(dataArea);
        }

        // draw the plot background and axes...
        drawBackground(g2, dataArea);

        AxisState domainAxisState = this.domainAxis.draw(g2,
                dataArea.getMaxY(), area, dataArea, RectangleEdge.BOTTOM, info);
        AxisState rangeAxisState = this.rangeAxis.draw(g2, dataArea.getMinX(),
                area, dataArea, RectangleEdge.LEFT, info);
        drawDomainGridlines(g2, dataArea, domainAxisState.getTicks());
        drawRangeGridlines(g2, dataArea, rangeAxisState.getTicks());

        Shape originalClip = g2.getClip();
        Composite originalComposite = g2.getComposite();

        g2.clip(dataArea);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                getForegroundAlpha()));

        render(g2, dataArea, info, null);

        g2.setClip(originalClip);
        g2.setComposite(originalComposite);
        drawOutline(g2, dataArea);

    }

    /**
     * Draws a representation of the data within the dataArea region.  The
     * {@code info} and {@code crosshairState} arguments may be
     * {@code null}.
     *
     * @param g2  the graphics device.
     * @param dataArea  the region in which the data is to be drawn.
     * @param info  an optional object for collection dimension information.
     * @param crosshairState  collects crosshair information ({@code null}
     *                        permitted).
     */
    public void render(Graphics2D g2, Rectangle2D dataArea,
                       PlotRenderingInfo info, CrosshairState crosshairState) {
        g2.setPaint(this.fastScatterPlotProduct.getPaint());

        // if the axes use a linear scale, you can uncomment the code below and
        // switch to the alternative transX/transY calculation inside the loop
        // that follows - it is a little bit faster then.
        //
        // int xx = (int) dataArea.getMinX();
        // int ww = (int) dataArea.getWidth();
        // int yy = (int) dataArea.getMaxY();
        // int hh = (int) dataArea.getHeight();
        // double domainMin = this.domainAxis.getLowerBound();
        // double domainLength = this.domainAxis.getUpperBound() - domainMin;
        // double rangeMin = this.rangeAxis.getLowerBound();
        // double rangeLength = this.rangeAxis.getUpperBound() - rangeMin;

        if (this.data != null) {
            for (int i = 0; i < this.data[0].length; i++) {
                float x = this.data[0][i];
                float y = this.data[1][i];

                //int transX = (int) (xx + ww * (x - domainMin) / domainLength);
                //int transY = (int) (yy - hh * (y - rangeMin) / rangeLength);
                int transX = (int) this.domainAxis.valueToJava2D(x, dataArea,
                        RectangleEdge.BOTTOM);
                int transY = (int) this.rangeAxis.valueToJava2D(y, dataArea,
                        RectangleEdge.LEFT);
                g2.fillRect(transX, transY, 1, 1);
            }
        }
    }

    /**
     * Draws the gridlines for the plot, if they are visible.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param ticks  the ticks.
     */
    protected void drawDomainGridlines(Graphics2D g2, Rectangle2D dataArea,
            List ticks) {
        if (!isDomainGridlinesVisible()) {
            return;
        }
        Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, 
                RenderingHints.VALUE_STROKE_NORMALIZE);
        for (Object o : ticks) {
            ValueTick tick = (ValueTick) o;
            double v = this.domainAxis.valueToJava2D(tick.getValue(),
                    dataArea, RectangleEdge.BOTTOM);
            Line2D line = new Line2D.Double(v, dataArea.getMinY(), v,
                    dataArea.getMaxY());
            g2.setPaint(getDomainGridlinePaint());
            g2.setStroke(getDomainGridlineStroke());
            g2.draw(line);
        }
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
    }

    /**
     * Draws the gridlines for the plot, if they are visible.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param ticks  the ticks.
     */
    protected void drawRangeGridlines(Graphics2D g2, Rectangle2D dataArea,
            List ticks) {

        if (!isRangeGridlinesVisible()) {
            return;
        }
        Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, 
                RenderingHints.VALUE_STROKE_NORMALIZE);

        for (Object o : ticks) {
            ValueTick tick = (ValueTick) o;
            double v = this.rangeAxis.valueToJava2D(tick.getValue(),
                    dataArea, RectangleEdge.LEFT);
            Line2D line = new Line2D.Double(dataArea.getMinX(), v,
                    dataArea.getMaxX(), v);
            g2.setPaint(getRangeGridlinePaint());
            g2.setStroke(getRangeGridlineStroke());
            g2.draw(line);
        }
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
    }

    /**
     * Returns the range of data values to be plotted along the axis, or
     * {@code null} if the specified axis isn't the domain axis or the
     * range axis for the plot.
     *
     * @param axis  the axis ({@code null} permitted).
     *
     * @return The range (possibly {@code null}).
     */
    @Override
    public Range getDataRange(ValueAxis axis) {
        Range result = null;
        if (axis == this.domainAxis) {
            result = this.xDataRange;
        }
        else if (axis == this.rangeAxis) {
            result = this.yDataRange;
        }
        return result;
    }

    /**
     * Calculates the X data range.
     *
     * @param data  the data ({@code null} permitted).
     *
     * @return The range.
     */
    private Range calculateXDataRange(float[][] data) {

        Range result = null;

        if (data != null) {
            float lowest = Float.POSITIVE_INFINITY;
            float highest = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < data[0].length; i++) {
                float v = data[0][i];
                if (v < lowest) {
                    lowest = v;
                }
                if (v > highest) {
                    highest = v;
                }
            }
            if (lowest <= highest) {
                result = new Range(lowest, highest);
            }
        }

        return result;

    }

    /**
     * Calculates the Y data range.
     *
     * @param data  the data ({@code null} permitted).
     *
     * @return The range.
     */
    private Range calculateYDataRange(float[][] data) {

        Range result = null;
        if (data != null) {
            float lowest = Float.POSITIVE_INFINITY;
            float highest = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < data[0].length; i++) {
                float v = data[1][i];
                if (v < lowest) {
                    lowest = v;
                }
                if (v > highest) {
                    highest = v;
                }
            }
            if (lowest <= highest) {
                result = new Range(lowest, highest);
            }
        }
        return result;

    }

    /**
     * Multiplies the range on the domain axis by the specified factor.
     *
     * @param factor  the zoom factor.
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    @Override
    public void zoomDomainAxes(double factor, PlotRenderingInfo info,
                               Point2D source) {
        this.domainAxis.resizeRange(factor);
    }

    /**
     * Multiplies the range on the domain axis by the specified factor.
     *
     * @param factor  the zoom factor.
     * @param info  the plot rendering info.
     * @param source  the source point (in Java2D space).
     * @param useAnchor  use source point as zoom anchor?
     *
     * @see #zoomRangeAxes(double, PlotRenderingInfo, Point2D, boolean)
     */
    @Override
    public void zoomDomainAxes(double factor, PlotRenderingInfo info,
                               Point2D source, boolean useAnchor) {

        if (useAnchor) {
            // get the source coordinate - this plot has always a VERTICAL
            // orientation
            double sourceX = source.getX();
            double anchorX = this.domainAxis.java2DToValue(sourceX,
                    info.getDataArea(), RectangleEdge.BOTTOM);
            this.domainAxis.resizeRange2(factor, anchorX);
        }
        else {
            this.domainAxis.resizeRange(factor);
        }

    }

    /**
     * Zooms in on the domain axes.
     *
     * @param lowerPercent  the new lower bound as a percentage of the current
     *                      range.
     * @param upperPercent  the new upper bound as a percentage of the current
     *                      range.
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    @Override
    public void zoomDomainAxes(double lowerPercent, double upperPercent,
                               PlotRenderingInfo info, Point2D source) {
        this.domainAxis.zoomRange(lowerPercent, upperPercent);
    }

    /**
     * Multiplies the range on the range axis/axes by the specified factor.
     *
     * @param factor  the zoom factor.
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    @Override
    public void zoomRangeAxes(double factor, PlotRenderingInfo info, 
            Point2D source) {
        this.rangeAxis.resizeRange(factor);
    }

    /**
     * Multiplies the range on the range axis by the specified factor.
     *
     * @param factor  the zoom factor.
     * @param info  the plot rendering info.
     * @param source  the source point (in Java2D space).
     * @param useAnchor  use source point as zoom anchor?
     *
     * @see #zoomDomainAxes(double, PlotRenderingInfo, Point2D, boolean)
     */
    @Override
    public void zoomRangeAxes(double factor, PlotRenderingInfo info,
                              Point2D source, boolean useAnchor) {

        if (useAnchor) {
            // get the source coordinate - this plot has always a VERTICAL
            // orientation
            double sourceY = source.getY();
            double anchorY = this.rangeAxis.java2DToValue(sourceY,
                    info.getDataArea(), RectangleEdge.LEFT);
            this.rangeAxis.resizeRange2(factor, anchorY);
        }
        else {
            this.rangeAxis.resizeRange(factor);
        }

    }

    /**
     * Zooms in on the range axes.
     *
     * @param lowerPercent  the new lower bound as a percentage of the current
     *                      range.
     * @param upperPercent  the new upper bound as a percentage of the current
     *                      range.
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    @Override
    public void zoomRangeAxes(double lowerPercent, double upperPercent,
                              PlotRenderingInfo info, Point2D source) {
        this.rangeAxis.zoomRange(lowerPercent, upperPercent);
    }

    /**
     * Returns {@code true}.
     *
     * @return A boolean.
     */
    @Override
    public boolean isDomainZoomable() {
        return true;
    }

    /**
     * Returns {@code true}.
     *
     * @return A boolean.
     */
    @Override
    public boolean isRangeZoomable() {
        return true;
    }

    /**
     * Returns {@code true} if panning is enabled for the domain axes,
     * and {@code false} otherwise.
     *
     * @return A boolean.
     */
    @Override
    public boolean isDomainPannable() {
        return this.domainPannable;
    }

    /**
     * Sets the flag that enables or disables panning of the plot along the
     * domain axes.
     *
     * @param pannable  the new flag value.
     */
    public void setDomainPannable(boolean pannable) {
        this.domainPannable = pannable;
    }

    /**
     * Returns {@code true} if panning is enabled for the range axes,
     * and {@code false} otherwise.
     *
     * @return A boolean.
     */
    @Override
    public boolean isRangePannable() {
        return this.rangePannable;
    }

    /**
     * Sets the flag that enables or disables panning of the plot along
     * the range axes.
     *
     * @param pannable  the new flag value.
     */
    public void setRangePannable(boolean pannable) {
        this.rangePannable = pannable;
    }

    /**
     * Pans the domain axes by the specified percentage.
     *
     * @param percent  the distance to pan (as a percentage of the axis length).
     * @param info the plot info
     * @param source the source point where the pan action started.
     */
    @Override
    public void panDomainAxes(double percent, PlotRenderingInfo info,
            Point2D source) {
        if (!isDomainPannable() || this.domainAxis == null) {
            return;
        }
        double length = this.domainAxis.getRange().getLength();
        double adj = percent * length;
        if (this.domainAxis.isInverted()) {
            adj = -adj;
        }
        this.domainAxis.setRange(this.domainAxis.getLowerBound() + adj,
                this.domainAxis.getUpperBound() + adj);
    }

    /**
     * Pans the range axes by the specified percentage.
     *
     * @param percent  the distance to pan (as a percentage of the axis length).
     * @param info the plot info
     * @param source the source point where the pan action started.
     */
    @Override
    public void panRangeAxes(double percent, PlotRenderingInfo info,
            Point2D source) {
        if (!isRangePannable() || this.rangeAxis == null) {
            return;
        }
        double length = this.rangeAxis.getRange().getLength();
        double adj = percent * length;
        if (this.rangeAxis.isInverted()) {
            adj = -adj;
        }
        this.rangeAxis.setRange(this.rangeAxis.getLowerBound() + adj,
                this.rangeAxis.getUpperBound() + adj);
    }

    /**
     * Tests an arbitrary object for equality with this plot.  Note that
     * {@code FastScatterPlot} carries its data around with it (rather
     * than referencing a dataset), and the data is included in the
     * equality test.
     *
     * @param obj  the object ({@code null} permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof FastScatterPlot)) {
            return false;
        }
        FastScatterPlot that = (FastScatterPlot) obj;
        if (this.domainPannable != that.domainPannable) {
            return false;
        }
        if (this.rangePannable != that.rangePannable) {
            return false;
        }
        if (!ArrayUtils.equal(this.data, that.data)) {
            return false;
        }
        if (!Objects.equals(this.domainAxis, that.domainAxis)) {
            return false;
        }
        if (!Objects.equals(this.rangeAxis, that.rangeAxis)) {
            return false;
        }
        if (!PaintUtils.equal(this.fastScatterPlotProduct.getPaint(), that.fastScatterPlotProduct.getPaint())) {
            return false;
        }
        if (this.fastScatterPlotProduct.getDomainGridlinesVisible() != that.fastScatterPlotProduct.getDomainGridlinesVisible()) {
            return false;
        }
        if (!PaintUtils.equal(this.fastScatterPlotProduct.getDomainGridlinePaint(),
                that.fastScatterPlotProduct.getDomainGridlinePaint())) {
            return false;
        }
        if (!Objects.equals(this.fastScatterPlotProduct.getDomainGridlineStroke(), that.fastScatterPlotProduct.getDomainGridlineStroke())) {
            return false;
        }
        if (!this.fastScatterPlotProduct.getRangeGridlinesVisible() == that.fastScatterPlotProduct.getRangeGridlinesVisible()) {
            return false;
        }
        if (!PaintUtils.equal(this.fastScatterPlotProduct.getRangeGridlinePaint(),
                that.fastScatterPlotProduct.getRangeGridlinePaint())) {
            return false;
        }
        if (!Objects.equals(this.fastScatterPlotProduct.getRangeGridlineStroke(), that.fastScatterPlotProduct.getRangeGridlineStroke())) {
            return false;
        }
        return true;
    }

    /**
     * Returns a clone of the plot.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException if some component of the plot does
     *                                    not support cloning.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {

        FastScatterPlot clone = (FastScatterPlot) super.clone();
		clone.fastScatterPlotProduct = (FastScatterPlotProduct) this.fastScatterPlotProduct.clone();
        if (this.data != null) {
            clone.data = ArrayUtils.clone(this.data);
        }
        if (this.domainAxis != null) {
            clone.domainAxis = (ValueAxis) this.domainAxis.clone();
            clone.domainAxis.setPlot(clone);
            clone.domainAxis.addChangeListener(clone);
        }
        if (this.rangeAxis != null) {
            clone.rangeAxis = (ValueAxis) this.rangeAxis.clone();
            clone.rangeAxis.setPlot(clone);
            clone.rangeAxis.addChangeListener(clone);
        }
        return clone;

    }

    /**
     * Provides serialization support.
     *
     * @param stream  the output stream.
     *
     * @throws IOException  if there is an I/O error.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        SerialUtils.writePaint(this.fastScatterPlotProduct.getPaint(), stream);
        SerialUtils.writeStroke(this.fastScatterPlotProduct.getDomainGridlineStroke(), stream);
        SerialUtils.writePaint(this.fastScatterPlotProduct.getDomainGridlinePaint(), stream);
        SerialUtils.writeStroke(this.fastScatterPlotProduct.getRangeGridlineStroke(), stream);
        SerialUtils.writePaint(this.fastScatterPlotProduct.getRangeGridlinePaint(), stream);
    }

    /**
     * Provides serialization support.
     *
     * @param stream  the input stream.
     *
     * @throws IOException  if there is an I/O error.
     * @throws ClassNotFoundException  if there is a classpath problem.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream(stream);
		if (this.domainAxis != null) {
            this.domainAxis.addChangeListener(this);
        }

        if (this.rangeAxis != null) {
            this.rangeAxis.addChangeListener(this);
        }
    }

	private void stream(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		fastScatterPlotProduct.setPaint2(SerialUtils.readPaint(stream));
		fastScatterPlotProduct.setDomainGridlineStroke2(SerialUtils.readStroke(stream));
		fastScatterPlotProduct.setDomainGridlinePaint2(SerialUtils.readPaint(stream));
		fastScatterPlotProduct.setRangeGridlineStroke2(SerialUtils.readStroke(stream));
		fastScatterPlotProduct.setRangeGridlinePaint2(SerialUtils.readPaint(stream));
	}

}
