/*
package dataset;

import audio.util.ExtractMFCC;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;

public class makePNG {
    public static Color getColor(double power) {
        double H = Math.abs(power) * 100; // Hue (note 0.4 = Green, see huge chart below)
        double S =1; // Saturation
        double B=1; // Brightness
        if(power>=0){
            B=1;
        }
        else{
            B=0.5;
        }
        if(-5<power&&power<5){
            S=((power*0.1)+5)/10;
        }
//        System.out.print(S+" ");

        return Color.getHSBColor((float)H/360, (float)S, (float)B);
    }
    public static void main(String[] args){

            try{
                int nX = 127* 2;
                int nY = 13;
                double[][] plotData = new double[nX][nY];
                StringBuilder sb=new StringBuilder();

                BufferedImage theImage = new BufferedImage(nX,nY,BufferedImage.TYPE_INT_RGB);
                double ratio;
                for(int x = 0; x<nX; x++){
                    for(int y = 0; y<nY; y++){
                        ratio = mfcc[x][y];
//                    sb.append(String.format("%.4f",mfcc[x][y])+" ");
                        sb.append(mfcc[x][y]+" ");

                        Color newColor = getColor(ratio);
                        theImage.setRGB(x, y, newColor.getRGB());
                    }
                    sb.append("\r\n");
                }
//            System.out.println();
                File outputfile = new File(ExtractMFCC.command+(ExtractMFCC.count)+".png");

                FileWriter fw=new FileWriter(ExtractMFCC.command+(ExtractMFCC.count++)+".txt");
                fw.write(sb.toString());
                ImageIO.write(theImage, "png", outputfile);

                fw.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
*/
