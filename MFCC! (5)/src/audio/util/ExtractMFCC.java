package audio.util;

import audio.Capture;
import audio.Maths;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ExtractMFCC {

    public static double[][] test;

    public static byte[] curBuf;
    private static boolean append=false;
    public static String command="잡음\\잡음";
    public static int i;
    public static boolean noMike=false;
    final static int t=2;
    public static int count=0;
    public static double[][] mfcc=new double[127*t][13];

    //2초 동안의 음성 데이터를 받는 구간

    public ExtractMFCC(byte[] buffer,int index) {
        //i가 초를 받는 구간
        extractMFCC(buffer,index);
        if(index!=0) {
            noMike=true;
            System.out.println("음성 인식 중지");

            System.out.println("Machine Learning....");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(count);
            System.out.println("불 켜");
            System.out.println("음성 인식 시작");
            noMike=false;
            //머신 러닝 완료
        }
    }

    void extractMFCC(byte[] a,int index) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try
                    {
                        ByteBuffer bufferWithHeader;
                        if(index==0) {
                            System.out.println("음성 파일 녹음 시작");
                            DataOutputStream outFile  = new DataOutputStream(new FileOutputStream(System.getProperty("user.dir")+"\\"+command+(count)+".wav",false));
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
                            DataOutputStream outFile  = new DataOutputStream(new FileOutputStream(System.getProperty("user.dir")+"\\"+command+(count)+".wav",true));
                           bufferWithHeader = ByteBuffer.allocate(a.length);
                            bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
                            bufferWithHeader.put(a);
                            outFile.write(bufferWithHeader.array());
                            outFile.close();
                            System.out.println("음성 파일 녹음 끝!!!!!!!!!!!!!!!!!!");
                        }
                    }
                    catch(Exception e)
                    {
                        System.out.println(e.getMessage());
                    }
                }
            }).start();

            try {
                test = Maths.MFCC(Maths.byteToDoubleArray(a));
                for(int j=0;j<test.length;j++){
                    mfcc[j+127*index]=test[j].clone();
//                    System.out.println(Arrays.toString(mfcc[j+127*index]));
                }
                if(index!=0){
                    printSepctogram.saveSepctogram(mfcc);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
