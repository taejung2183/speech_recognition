package audio.util;

import java.io.File;
import java.io.FileWriter;

public class WUWDetect {

    final String filePath=getPathParent(this.getClass()) + "wake_up_word.txt";

    public static double[][] test;
    public static int i=0;

    public WUWDetect(byte[] buffer) {
        if (!(buffer == null || buffer.length < 1)) {
            extractMFCC(buffer);
        }
    }

    void extractMFCC(byte[] a) {

        try {
            //만약 WUW가 감지되었다면 ExtractMFCC 실행 아니라면 그냥 끝
            new ExtractMFCC(a, (i++) % 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getPathParent(Class c) {
        String p = c.getProtectionDomain().getCodeSource().getLocation().getPath();
        String drp = new File(p).getParent();
        return drp + System.getProperty("file.separator");
    }

    private void saveMFCCToTextFile(double[] mfcc) throws Exception{
        StringBuilder sb=new StringBuilder();
        for(double d:mfcc) {
            sb.append(String.format("%.4f", d) + ",");
        }
        sb.append("0\r\n");

        //mfcc 데이터를 라인 별로 파일에 입력
        FileWriter fw=new FileWriter(filePath,true);
        fw.write(sb.toString());

        fw.close();
    }
}
