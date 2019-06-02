package audio;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JButton;
import javax.swing.JFrame;

public class Capture extends JFrame {
	/**//* Audio format settings */
	/**/public static int sampleRate = //32
			//64
			//128
			//256
			//512
			//1024
			//2048
			//4096
			//8192
			//11025
			16384//ㅇㅣ것이 이전에 사용하던 것
//			16000
			//22050 //IS NOT POWER OF 2
			//32768
			//44100 //IS NOT POWER OF 2
			//65536
			;
	/**/public static int sampleSizeInBits = 16;
	/**/public static int channels = 1;  //이전에 쓰던 것 안되면 2를 써봐라
	/**/public static boolean signed = true;
	/**/public static boolean bigEndian = false;//true; //이전에 쓰던 것
	/**//* ======================= */

	public static final byte FOURIER = 0;
	public static final byte BUFFER = 1;
	public static final byte MFCC = 2;
	private static byte LIMIT_VIEW = 3;
	private static final byte DFT_STATE = BUFFER;
	
	public static boolean running;
	public static boolean end;
	public static boolean commandStart;//명령어 인식 시작
	public static int state;
	public static boolean sync = true;
	public static byte buffer[];
	public static Object locker = new Object();

	public static byte audio[];
	public static ByteArrayOutputStream rec;

	public static void main(String args[]) {
		new Capture();
        new AudioCanvas();
//		new ExtractMFCC();
	}

	public Capture() {
		super("Capture Sound");

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		Container content = getContentPane();
		setResizable(false);

		final JButton hannign = new JButton("Hanning");
		final JButton bufferView = new JButton("B");
		final JButton fourierView = new JButton("F");
		final JButton mfccView = new JButton("M");
		final JButton stop = new JButton("Stop");
		final JButton linesOrDots = new JButton("Lines/Dots");

		linesOrDots.setEnabled(true);
		hannign.setEnabled(true);
		bufferView.setEnabled(false);
		fourierView.setEnabled(true);
		mfccView.setEnabled(true);
		state = DFT_STATE;
		stop.setEnabled(true);
		running = true;
		end=false;
		commandStart=false;
		captureAudio();
		
		ActionListener captureListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AudioPainter.useWindowFunction = !AudioPainter.useWindowFunction;
				AudioPainter.refreshColors();
			}
		};
		ActionListener switchActivityListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AudioPainter.drL = (AudioPainter.drL+1)%2;
				AudioPainter.refreshColors();
			}
		};
		ActionListener switchViewListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enableViewButton();
				JButton b = (JButton) e.getSource();
				if (b.getText().equals("B")) {
					state = BUFFER;
				} else if (b.getText().equals("F")) {
					state = FOURIER;
				} else if (b.getText().equals("M")) {
					state = MFCC;
				}
				b.setEnabled(false);
			}

			private void enableViewButton() {
				switch (state) {
				case FOURIER:
					fourierView.setEnabled(true);
					return;
				case BUFFER:
					bufferView.setEnabled(true);
					return;
				case MFCC:
					mfccView.setEnabled(true);
					return;
				default:
					System.exit(1);
				}
			}
		};
		ActionListener stopListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				hannign.setEnabled(true);
//				stop.setEnabled(false);
				if (stop.getText().equals("Stop")) {
					stop.setText("Start");
				} else {
 					stop.setText("Stop");
				}
				running = !running;
			}
		};

		hannign.addActionListener(captureListener);

		linesOrDots.addActionListener(switchActivityListener);

		bufferView.addActionListener(switchViewListener);
		fourierView.addActionListener(switchViewListener);
		mfccView.addActionListener(switchViewListener);

		stop.addActionListener(stopListener);

		content.add(hannign, BorderLayout.NORTH);
		content.add(bufferView, BorderLayout.WEST);
		content.add(fourierView, BorderLayout.CENTER);
		content.add(mfccView, BorderLayout.EAST);
		content.add(stop, BorderLayout.SOUTH);
//		content.add(linesOrDots, BorderLayout.SOUTH);

		pack();
		setVisible(true);

	}

	private void captureAudio() {
		try {
			final AudioFormat format = getFormat();

			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			final TargetDataLine line = ((TargetDataLine) AudioSystem.getLine(info));
			line.open(format);
			line.start();

			new Thread(new Runnable() {
				//한 프레임당 4byte
				int bufferSize = (int) format.getSampleRate()* format.getFrameSize();// 16384 * 2
				@Override
				public void run() {
					System.out.println(bufferSize);
					buffer = new byte[bufferSize];
					rec = new ByteArrayOutputStream();

//					running = true;
                    int time=0;
						while(!end) {
							System.out.println("1111111111");
							while (running) {
								int count = 0;
								synchronized (locker) {
									count = line.read(buffer, 0, buffer.length);
								}
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								/*if (count > 0) {
									rec.write(buffer, 0, count);
								}*/

							}
						}
				}
			}).start();
		} catch (LineUnavailableException e) {
			System.err.println("Line unavailable: " + e);
			System.exit(-2);
		}
	}

	private AudioFormat getFormat() {
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	public static int byteArrayToInt(byte[] b)
	{
		int start = 0;
		int low = b[start] & 0xff;
		int high = b[start+1] & 0xff;
		return (int)( high << 8 | low );
	}

	// these two routines convert a byte array to an unsigned integer
	public static long byteArrayToLong(byte[] b)
	{
		int start = 0;
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++)
		{
			tmp[cnt] = b[i];
			cnt++;
		}
		long accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 )
		{
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}


	// ===========================
// CONVERT JAVA TYPES TO BYTES
// ===========================
	// returns a byte array of length 4
	private static byte[] intToByteArray(int i)
	{
		byte[] b = new byte[4];
		b[0] = (byte) (i & 0x00FF);
		b[1] = (byte) ((i >> 8) & 0x000000FF);
		b[2] = (byte) ((i >> 16) & 0x000000FF);
		b[3] = (byte) ((i >> 24) & 0x000000FF);
		return b;
	}

	// convert a short to a byte array
	public static byte[] shortToByteArray(short data)
	{
		return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
	}
}