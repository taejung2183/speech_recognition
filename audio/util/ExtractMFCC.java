package org.deeplearning4j.examples.convolution.mnist.audio.util;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.examples.convolution.mnist.audio.Maths;
import org.deeplearning4j.examples.convolution.mnist.audio.UI;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

public class ExtractMFCC {

    public static double[][] test;

    public static boolean noMike=true;
    final static int t=2;
    public static int count=0;
    public static double[][] mfcc=new double[127*t][13];
    static final int height = 13;
    static final int width = 254;
    static final int channels = 3;
    static byte[] b;
    static String type="command";

    //2초 동안의 음성 데이터를 받는 구간

    public ExtractMFCC(byte[] buffer,boolean isLastFrame) {
        //i가 초를 받는 구간
        extractMFCC(buffer,isLastFrame);
        if(isLastFrame){
//            noMike=false;
            UI.detect=false;
        }
    }


    void extractMFCC(byte[] a,boolean isLastFrame) {

      /*  new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    ByteBuffer bufferWithHeader;
                    if(!isLastFrame) {
                        DataOutputStream outFile  = new DataOutputStream(new FileOutputStream(UI.BASE_PATH+(count)+"copy.wav",false));
                        bufferWithHeader = ByteBuffer.allocate(a.length + 44);
                        bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
                        bufferWithHeader.put("RIFF".getBytes());
                        bufferWithHeader.putInt(a.length*t + 36);
                        bufferWithHeader.put("WAVE".getBytes());
                        bufferWithHeader.put("fmt ".getBytes());
                        bufferWithHeader.putInt(16);
                        bufferWithHeader.putShort((short) 1);
                        bufferWithHeader.putShort((short) 1);
                        bufferWithHeader.putInt(16384);
                        bufferWithHeader.putInt(32768);
                        bufferWithHeader.putShort((short) 2);
                        bufferWithHeader.putShort((short) 16);
                        bufferWithHeader.put("data".getBytes());
                        bufferWithHeader.putInt(a.length*t);
                        bufferWithHeader.put(a);
                        outFile.write(bufferWithHeader.array());
                        outFile.close();
                    }
                    else{
                        DataOutputStream outFile  = new DataOutputStream(new FileOutputStream(UI.BASE_PATH+(count)+"copy.wav",true));
                        bufferWithHeader = ByteBuffer.allocate(a.length);
                        bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
                        bufferWithHeader.put(a);
                        outFile.write(bufferWithHeader.array());
                        outFile.close();
                    }
                }
                catch(Exception e)
                {
                    System.out.println(e.getMessage());
                }
            }
        }).start();*/

        try {
            test = Maths.MFCC(Maths.byteToDoubleArray(a));
            int k=isLastFrame?1:0;
            for(int j=0;j<test.length;j++){
                mfcc[j+127*k]=test[j].clone();
            }
            if(isLastFrame){
                printSepctogram.saveSepctogram(mfcc,type,0);

                UI.stopUI();
                int p = (int) MachineLearning();

                System.out.println("mfcc 결과 : " + p);

                UI.m_PnCmd[p].getGraphics().drawImage(new ImageIcon(UI.m_afterImgPath[p]).getImage(), 0, 0, UI.m_PnCmd[p]);
//                UI.resumeUI();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    double MachineLearning(){
        File file = new File(UI.BASE_PATH+type+(count)+".png");
        NativeImageLoader loader = new NativeImageLoader(height, width, channels);

        //우리는 MFCC 결과를 배열로 배꿔줘야겠지.
        INDArray testImage = null;
        try {
            testImage = loader.asMatrix(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(testImage.get());

        DataNormalization scaler = new ImagePreProcessingScaler();

        scaler.transform(testImage);

        INDArray output = UI.model.output(testImage);

        System.err.println("The neural nets prediction (list of probabilities per label)");
        //log.info("## List of Labels in Order## ");
        // In new versions labels are always in order
        System.err.println(output.toString());
        //  log.info(output.maxNumber().toString());
        //   log.info(labelList.toString());

        String result = output.argMax().toString();
        double totalresult = Double.parseDouble(result);
        totalresult = Math.round(totalresult);
        return totalresult;
    }

}
