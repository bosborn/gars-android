package mil.nga.gars.tile;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import java.util.List;

import mil.nga.gars.features.Line;
import mil.nga.gars.features.Point;
import mil.nga.gars.grid.Label;
import mil.nga.gars.grid.style.Grid;

/**
 * Tile draw utilities for lines and labels
 *
 * @author osbornb
 */
public class TileDraw {

    /**
     * Draw the lines on the tile
     *
     * @param lines  lines to draw
     * @param tile   tile
     * @param grid   grid
     * @param canvas draw canvas
     */
    public static void drawLines(List<Line> lines, GARSTile tile, Grid grid, Canvas canvas) {

        for (Line line : lines) {

            Path linePath = new Path();
            addPolyline(tile, linePath, line);
            Paint paint = grid.getLinePaint(line.getGridType());
            canvas.drawPath(linePath, paint);

        }

    }

    /**
     * Add the polyline to the path
     *
     * @param tile tile
     * @param path line path
     * @param line line to draw
     */
    private static void addPolyline(GARSTile tile, Path path, Line line) {

        line = line.toMeters();
        Point point1 = line.getPoint1();
        Point point2 = line.getPoint2();

        Pixel pixel = point1.getPixel(tile);
        path.moveTo(pixel.getX(), pixel.getY());

        Pixel pixel2 = point2.getPixel(tile);
        path.lineTo(pixel2.getX(), pixel2.getY());

    }

    /**
     * Draw the labels on the tile
     *
     * @param labels labels to draw
     * @param buffer grid zone edge buffer
     * @param tile   tile
     * @param canvas draw canvas
     * @param paint  label paint
     */
    public static void drawLabels(List<Label> labels, double buffer, GARSTile tile, Canvas canvas, Paint paint) {
        for (Label label : labels) {
            drawLabel(label, buffer, tile, canvas, paint);
        }
    }

    /**
     * Draw the label
     *
     * @param label  label to draw
     * @param buffer grid zone edge buffer
     * @param tile   tile
     * @param canvas draw canvas
     * @param paint  label paint
     */
    public static void drawLabel(Label label, double buffer, GARSTile tile, Canvas canvas, Paint paint) {

        String name = label.getName();

        // Determine the text bounds
        Rect textBounds = new Rect();
        paint.getTextBounds(name, 0, name.length(), textBounds);
        float textWidth = paint.measureText(name);
        int textHeight = textBounds.height();

        // Determine the pixel width and height of the label grid zone to the tile
        PixelRange pixelRange = label.getBounds().getPixelRange(tile);

        // Determine the maximum width and height a label in the grid should be
        double gridPercentage = 1.0 - (2 * buffer);
        double maxWidth = gridPercentage * pixelRange.getWidth();
        double maxHeight = gridPercentage * pixelRange.getHeight();

        // If it fits, draw the label in the center of the grid zone
        if (textWidth <= maxWidth && textHeight <= maxHeight) {
            Pixel centerPixel = label.getCenter().getPixel(tile);
            canvas.drawText(name, centerPixel.getX() - textBounds.exactCenterX(), centerPixel.getY() - textBounds.exactCenterY(), paint);
        }

    }

}
