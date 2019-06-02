package org.deeplearning4j.examples.convolution.mnist.audio.util;


public final class FFT
{
  public static final int FFT_FORWARD = -1;
  public static final int FFT_REVERSE = 1;
  /** used to specify a magnitude Fourier transform */
  public static final int FFT_MAGNITUDE = 2;
  /** used to specify a magnitude phase Fourier transform */
  public static final int FFT_MAGNITUDE_PHASE = 3;
  /** used to specify a normalized power Fourier transform */
  public static final int FFT_NORMALIZED_POWER = 4;
  /** used to specify a power Fourier transform */
  public static final int FFT_POWER = 5;
  /** used to specify a power phase Fourier transform */
  public static final int FFT_POWER_PHASE = 6;
  /** used to specify a inline power phase Fourier transform */
  public static final int FFT_INLINE_POWER_PHASE = 7;


  /** used to specify a rectangular window function */
  public static final int WND_NONE = -1;
  /** used to specify a rectangular window function */
	public static final int WND_RECT = 0;
	/** used to specify a Hamming window function */
	public static final int WND_HAMMING = 1;
	/** used to specify a 61-dB 3-sample Blackman-Harris window function */
	public static final int WND_BH3 = 2;
	/** used to specify a 74-dB 4-sample Blackman-Harris window function */
	public static final int WND_BH4 = 3;
	/** used to specify a minimum 3-sample Blackman-Harris window function */
	public static final int WND_BH3MIN = 4;
	/** used to specify a minimum 4-sample Blackman-Harris window function */
	public static final int WND_BH4MIN = 5;
	/** used to specify a Gaussian window function */
	public static final int WND_GAUSS = 6;
  /** used to specify a Hanning window function */
  public static final int WND_HANNING = 7;
  /** used to specify a Hanning window function */
  public static final int WND_USER_DEFINED = 8;

  private double[] windowFunction;
  private double windowFunctionSum;
  private int windowFunctionType;
  private int transformationType;
  private int windowSize;
  private static final double twoPI = 2 * Math.PI;

  public FFT(int transformationType, int windowSize)
  {
    this(transformationType, windowSize, WND_NONE, windowSize);
  }

  public FFT(int transformationType, int windowSize, int windowFunctionType)
  {
    this(transformationType, windowSize, windowFunctionType, windowSize);
  }

  public FFT(int transformationType, int windowSize, int windowFunctionType, int support)
  {
    //check and set fft type
    this.transformationType = transformationType;
    if(transformationType < -1 || transformationType > 7)
    {
      transformationType = FFT_FORWARD;
      throw new IllegalArgumentException("unknown fft type");
    }

    //check and set windowSize
    this.windowSize = windowSize;
    if (windowSize != (1 << ((int)Math.rint(Math.log(windowSize) / Math.log(2)))))
      throw new IllegalArgumentException("fft data must be power of 2");

    //create window function buffer and set window function
    this.windowFunction = new double[windowSize];
    setWindowFunction(windowFunctionType, support);
  }

  public FFT(int transformationType, int windowSize, double[] windowFunction)
  {
    this(transformationType, windowSize, WND_NONE, windowSize);

    if(windowFunction.length != windowSize)
    {
      throw new IllegalArgumentException("size of window function match window size");
    }
    else
    {
      this.windowFunction = windowFunction;
      this.windowFunctionType = WND_USER_DEFINED;
      calculateWindowFunctionSum();
    }

  }

  public void transform(double[] re, double[] im)
  {
    //check for correct size of the real part data array
    if(re.length < windowSize)
      throw new IllegalArgumentException("data array smaller than fft window size");

    //apply the window function to the real part
    applyWindowFunction(re);

    //perform the transformation
    switch(transformationType)
    {
      case FFT_FORWARD:
        //check for correct size of the imaginary part data array
        if(im.length < windowSize)
          throw new IllegalArgumentException("data array smaller than fft window size");
        else
          fft(re, im, FFT_FORWARD);
        break;
      case FFT_INLINE_POWER_PHASE:
        if(im.length < windowSize)
          throw new IllegalArgumentException("data array smaller than fft window size");
        else
          powerPhaseIFFT(re, im);
        break;
      case FFT_MAGNITUDE:
        magnitudeFFT(re);
        break;
      case FFT_MAGNITUDE_PHASE:
        if(im.length < windowSize)
          throw new IllegalArgumentException("data array smaller than fft window size");
        else
          magnitudePhaseFFT(re, im);
        break;
      case FFT_NORMALIZED_POWER:
        normalizedPowerFFT(re);
        break;
      case FFT_POWER:
        powerFFT(re);
        break;
      case FFT_POWER_PHASE:
        if(im.length < windowSize)
          throw new IllegalArgumentException("data array smaller than fft window size");
        else
          powerPhaseFFT(re, im);
        break;
      case FFT_REVERSE:
        if(im.length < windowSize)
          throw new IllegalArgumentException("data array smaller than fft window size");
        else
          fft(re, im, FFT_REVERSE);
        break;
    }
  }

	public void fft(double re[], double im[], int direction)
  {
		int n = re.length;
		int bits = (int)Math.rint(Math.log(n) / Math.log(2));

		if (n != (1 << bits))
			throw new IllegalArgumentException("fft data must be power of 2");

		int localN;
		int j = 0;
		for (int i = 0; i < n-1; i++)
    {
			if (i < j)
      {
				double temp = re[j];
				re[j] = re[i];
				re[i] = temp;
				temp = im[j];
				im[j] = im[i];
				im[i] = temp;
			}

			int k = n / 2;

      while ((k >= 1) &&  (k - 1 < j))
      {
				j = j - k;
				k = k / 2;
			}

			j = j + k;
		}

		for(int m = 1; m <= bits; m++)
    {
			localN = 1 << m;
			double Wjk_r = 1;
			double Wjk_i = 0;
			double theta = twoPI / localN;
			double Wj_r = Math.cos(theta);
			double Wj_i = direction * Math.sin(theta);
			int nby2 = localN / 2;
			for (j = 0; j < nby2; j++)
      {
				for (int k = j; k < n; k += localN)
        {
					int id = k + nby2;
					double tempr = Wjk_r * re[id] - Wjk_i * im[id];
					double tempi = Wjk_r * im[id] + Wjk_i * re[id];
					re[id] = re[k] - tempr;
					im[id] = im[k] - tempi;
					re[k] += tempr;
					im[k] += tempi;
				}
				double wtemp = Wjk_r;
				Wjk_r = Wj_r * Wjk_r  - Wj_i * Wjk_i;
				Wjk_i = Wj_r * Wjk_i  + Wj_i * wtemp;
			}
		}
	}

	private void powerFFT(double[] re)
  {
		double[] im = new double[re.length];

		fft(re, im, FFT_FORWARD);

		for (int i = 0; i < re.length; i++)
			re[i] = re[i] * re[i] + im[i] * im[i];
	}

  private void magnitudeFFT(double[] re)
  {
    double[] im = new double[re.length];

    fft(re, im, FFT_REVERSE);

    for (int i = 0; i < re.length; i++)
      re[i] = Math.sqrt(re[i] * re[i] + im[i] * im[i]);
  }

  private void normalizedPowerFFT(double[] re)
  {
    double[] im = new double[re.length];
    double r, i;

    fft(re, im, FFT_FORWARD);

    for (int j = 0; j < re.length; j++)
    {
      r = re[j] / windowFunctionSum * 2;
      i = im[j] / windowFunctionSum * 2;
      re[j] = r * r + i * i;
    }
  }

	private void toMagnitude(double[] re)
  {
		for (int i = 0; i < re.length; i++)
			re[i] = Math.sqrt(re[i]);
	}

	private void powerPhaseFFT(double[] re, double[] im)
  {
		fft(re, im, FFT_FORWARD);

		for (int i = 0; i < re.length; i++)
    {
			double pow = re[i] * re[i] + im[i] * im[i];
			im[i] = Math.atan2(im[i], re[i]);
			re[i] = pow;
		}
	}

	private void powerPhaseIFFT(double[] pow, double[] ph)
  {
		toMagnitude(pow);

		for (int i = 0; i < pow.length; i++)
    {
			double re = pow[i] * Math.cos(ph[i]);
			ph[i] = pow[i] * Math.sin(ph[i]);
			pow[i] = re;
		}

		fft(pow, ph, FFT_REVERSE);
	}

	private void magnitudePhaseFFT(double[] re, double[] im)
  {
		powerPhaseFFT(re, im);
		toMagnitude(re);
	}

	private void hamming(int size)
  {
    int start = (windowFunction.length - size) / 2;
		int stop = (windowFunction.length + size) / 2;
		double scale = 1.0 / (double)size / 0.54;
		double factor = twoPI / (double)size;

		for (int i = 0; start < stop; start++, i++)
			windowFunction[i] = scale * (25.0/46.0 - 21.0/46.0 * Math.cos(factor * i));
	}


  private void hanning(int size)
  {
    int start = (windowFunction.length - size) / 2;
    int stop = (windowFunction.length + size) / 2;
    double factor = twoPI / (size - 1.0d);

    for (int i = 0; start < stop; start++, i++)
      windowFunction[i] = 0.5 * (1 - Math.cos(factor * i));
  }

	private void blackmanHarris4sMin(int size)
  {
    int start = (windowFunction.length - size) / 2;
		int stop = (windowFunction.length + size) / 2;
		double scale = 1.0 / (double)size / 0.36;

		for (int i = 0; start < stop; start++, i++)
			windowFunction[i] = scale * ( 0.35875 -
								0.48829 * Math.cos(twoPI * i / size) +
								0.14128 * Math.cos(2 * twoPI * i / size) -
								0.01168 * Math.cos(3 * twoPI * i / size));
	}

	private void blackmanHarris4s(int size)
  {
    int start = (windowFunction.length - size) / 2;
		int stop = (windowFunction.length + size) / 2;
		double scale = 1.0 / (double)size / 0.4;

		for (int i = 0; start < stop; start++, i++)
			windowFunction[i] = scale * ( 0.40217 -
								0.49703 * Math.cos(twoPI * i / size) +
								0.09392 * Math.cos(2 * twoPI * i / size) -
								0.00183 * Math.cos(3 * twoPI * i / size));
	}

	private void blackmanHarris3sMin(int size)
  {
		int start = (windowFunction.length - size) / 2;
		int stop = (windowFunction.length + size) / 2;
		double scale = 1.0 / (double) size / 0.42;

		for (int i = 0; start < stop; start++, i++)
			windowFunction[i] = scale * ( 0.42323 -
								0.49755 * Math.cos(twoPI * i / size) +
								0.07922 * Math.cos(2 * twoPI * i / size));
	}

	private void blackmanHarris3s(int size)
  {
		int start = (windowFunction.length - size) / 2;
		int stop = (windowFunction.length + size) / 2;
		double scale = 1.0 / (double) size / 0.45;

		for (int i = 0; start < stop; start++, i++)
			windowFunction[i] = scale * ( 0.44959 -
								0.49364 * Math.cos(twoPI * i / size) +
								0.05677 * Math.cos(2 * twoPI * i / size));
	}

	private void gauss(int size)
  { // ?? between 61/3 and 74/4 BHW
		int start = (windowFunction.length - size) / 2;
		int stop = (windowFunction.length + size) / 2;
		double delta = 5.0 / size;
		double x = (1 - size) / 2.0 * delta;
		double c = -Math.PI * Math.exp(1.0) / 10.0;
		double sum = 0;

    for (int i = start; i < stop; i++)
    {
			windowFunction[i] = Math.exp(c * x * x);
			x += delta;
			sum += windowFunction[i];
		}

    for (int i = start; i < stop; i++)
			windowFunction[i] /= sum;
	}

	private void rectangle(int size)
  {
		int start = (windowFunction.length - size) / 2;
		int stop = (windowFunction.length + size) / 2;

		for (int i = start; i < stop; i++)
			windowFunction[i] = 1.0 / (double) size;
	}

  public void setWindowFunction(int windowFunctionType, int support)
  {
		if (support > windowSize)
			support = windowSize;

    switch (windowFunctionType)
    {
			case WND_NONE: break;
      case WND_RECT:	rectangle(support);		break;
			case WND_HAMMING:	hamming(support);			break;
      case WND_HANNING:   hanning(support);			break;
      case WND_BH3:	blackmanHarris3s(support);	break;
      case WND_BH4:	blackmanHarris4s(support);	break;
			case WND_BH3MIN:	blackmanHarris3sMin(support);	break;
			case WND_BH4MIN:	blackmanHarris4sMin(support);	break;
			case WND_GAUSS:	gauss(support);			break;
			default:
        windowFunctionType = WND_NONE;
        throw new IllegalArgumentException("unknown window function specified");
		}
    this.windowFunctionType = windowFunctionType;
    calculateWindowFunctionSum();
	}

	private void applyWindowFunction(double[] data)
  {
		if(windowFunctionType != WND_NONE)
    {
      for (int i = 0; i < data.length; i++)
        data[i] *= windowFunction[i];
    }
	}

  private void calculateWindowFunctionSum()
  {
    windowFunctionSum = 0;
    for(int i = 0; i < windowFunction.length; i++)
      windowFunctionSum += windowFunction[i];
  }
}
