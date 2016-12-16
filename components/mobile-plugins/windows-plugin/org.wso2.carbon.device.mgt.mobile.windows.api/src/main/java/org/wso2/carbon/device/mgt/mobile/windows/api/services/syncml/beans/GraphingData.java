package org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.beans;

import org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.impl.SyncmlServiceImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thisari on 12/1/16.
 *
 */
public class GraphingData extends JPanel {

    List<DeviceStatistics> storeData = SyncmlServiceImpl.getDeviceStatistics();

    java.util.List<Integer> data = new ArrayList<>();
    java.util.List<Integer> data2 = new ArrayList<>();

    final int PAD = 30;

    protected void paintComponent(Graphics g) {

        for(DeviceStatistics d : storeData){

            //test w10
            System.out.println("ststus : " + d.getBat_status());
            System.out.println("Runtime : " + d.getBat_runtime());
            System.out.println("------------------------------------------");

            if(d.getBat_status()==1){
                data.add(1000);
                data2.add(d.getBat_runtime());
            }else{
                data2.add(d.getBat_runtime());
                data.add(d.getBat_status());
            }

        }

        //test w10
        for (int i=0 ; i<data.size() ; i++){
            System.out.print(data.get(i) + " , ");
        }
        System.out.println();
        for (int i=0 ; i<data2.size() ; i++){
            System.out.print(data2.get(i) + " , ");
        }


        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        // Draw ordinate.
        g2.draw(new Line2D.Double(PAD, PAD, PAD, h-PAD));
        // Draw abcissa.
        g2.draw(new Line2D.Double(PAD, h-PAD, w-PAD, h-PAD));
        // Draw labels.
        Font font = g2.getFont();
        FontRenderContext frc = g2.getFontRenderContext();
        LineMetrics lm = font.getLineMetrics("0", frc);
        float sh = lm.getAscent() + lm.getDescent();

        // Ordinate label.
        String s = "status and runtime";
        float sy = PAD + ((h - 2*PAD) - s.length()*sh)/2 + lm.getAscent();
        for(int i = 0; i < s.length(); i++) {
            String letter = String.valueOf(s.charAt(i));
            float sw = (float)font.getStringBounds(letter, frc).getWidth();
            float sx = (PAD - sw)/2;
            g2.drawString(letter, sx, sy);
            sy += sh;
        }
        // Abcissa label.
        s = "Time";
        sy = h - PAD + (PAD - sh)/2 + lm.getAscent();
        float sw = (float)font.getStringBounds(s, frc).getWidth();
        float sx = (w - sw)/2;
        g2.drawString(s, sx, sy);
        // Draw lines.
        double xInc = (double)(w - 2*PAD)/(data.size()-1);
        double scale = (double)(h - 2*PAD)/getMax();                   //set maximum
        g2.setPaint(Color.green.darker());


        // draw lines
        for(int i = 0; i < data.size()-1; i++) {
            double x1 = PAD + i*xInc;
            double y1 = h - PAD - scale*data.get(i);
            double x2 = PAD + (i+1)*xInc;
            double y2 = h - PAD - scale*data.get(i+1);
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }
        // Mark data points.
        g2.setPaint(Color.red);
        for(int i = 0; i < data.size(); i++) {
            double x = PAD + i*xInc;
            double y = h - PAD - scale*data.get(i);
            g2.fill(new Ellipse2D.Double(x-2, y-2, 4, 4));
        }

        g2.setPaint(Color.green);
        for(int i = 0; i < data2.size(); i++) {
            double x = PAD + i*xInc;
            double y = h - PAD - scale*data2.get(i);
            g2.fill(new Ellipse2D.Double(x-2, y-2, 4, 4));
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setPaint(Color.black.darker());


        // draw lines
        for(int i = 0; i < data2.size()-1; i++) {
            double x1 = PAD + i*xInc;
            double y1 = h - PAD - scale*data2.get(i);
            double x2 = PAD + (i+1)*xInc;
            double y2 = h - PAD - scale*data2.get(i+1);
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }
        // Mark data points.
        g2.setPaint(Color.blue);
        for(int i = 0; i < data2.size(); i++) {
            double x = PAD + i*xInc;
            double y = h - PAD - scale*data2.get(i);
            g2.fill(new Ellipse2D.Double(x-2, y-2, 4, 4));
        }


    }

    private int getMax() {
        int max = -Integer.MAX_VALUE;
        for(int i = 0; i < data2.size(); i++) {
            if(data2.get(i) > max)
                max = data2.get(i);
        }
        return max;
    }
}
