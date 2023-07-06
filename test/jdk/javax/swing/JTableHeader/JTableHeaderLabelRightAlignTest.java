/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.UIManager;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.File;
import javax.imageio.ImageIO;

/*
 * @test
 * @bug 8301606
 * @library /java/awt/regtesthelpers
 * @build PassFailJFrame
 * @summary Test to check if the Right aligned header
 * label doesn't cut off Metal Look&Feel
 * @run main JTableHeaderLabelRightAlignTest
 */

public class JTableHeaderLabelRightAlignTest {
    static JTable table;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private static final double SCALE = 2.25;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        SwingUtilities.invokeAndWait(() -> {
            try {
                JTableHeaderLabelRightAlignTest.Test();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void Test() throws Exception {
        int verticalLineCol;
        int expectedRGB;
        BufferedImage imgHeader;
        double w;
        double h;
        String[][] data = {
                { "1", "1", "Green"},
                { "2", "2", "Blue"}
        };

        String[] columnNames = { "Size", "Size", "Size"};

        table = new JTable(data, columnNames);
        table.setSize(WIDTH,HEIGHT);
        ((JLabel)table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment( JLabel.RIGHT );

        final JTableHeader header = table.getTableHeader();
        TableCellRenderer renderer = header.getDefaultRenderer();
        header.setDefaultRenderer(renderer);
        table.updateUI();

        Dimension size = header.getPreferredSize();
        header.setSize(size);
        w = SCALE * size.width;
        h = SCALE * size.height;
        imgHeader = new BufferedImage((int)(w),(int)(h),BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgHeader.createGraphics();
        g2d.scale(SCALE, SCALE);
        try {
            header.paint(g2d);
        } finally {
            g2d.dispose();
        }

        verticalLineCol = (int)(table.getTableHeader().
                getColumnModel().getColumn(0).getWidth() * SCALE);
        expectedRGB = imgHeader.getRGB(verticalLineCol,1);

        for(int i = 1; i < imgHeader.getHeight()-3; i++) {
            for(int j = verticalLineCol; j < verticalLineCol + 1; j++) {
                if(expectedRGB != imgHeader.getRGB(j, i)) {
                    ImageIO.write(imgHeader, "png",new File("FailureImage.png"));
                    throw new RuntimeException("Test Failed");
                }
            }
        }
        System.out.println("Test Pass!!");
    }
}
