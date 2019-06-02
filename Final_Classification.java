package org.deeplearning4j.examples.convolution.mnist;


import com.sun.corba.se.spi.ior.Writeable;
import javassist.Loader;
import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.records.reader.BaseRecordReader;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.ComposableRecordReader;
import org.datavec.api.records.reader.impl.csv.*;
import org.datavec.api.records.reader.impl.filebatch.FileBatchRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.api.util.ClassPathResource;

import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator;
import org.deeplearning4j.examples.utilities.DataUtilities;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.GaussianDistribution;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.schedule.MapSchedule;
import org.nd4j.linalg.schedule.ScheduleType;
import org.nd4j.linalg.schedule.StepSchedule;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.io.FileOutputStream;
import java.io.IOException;


public class Final_Classification {


    private static final Logger LOGGER = LoggerFactory.getLogger(Final_Classification.class);
    private static final String BASE_PATH = System.getProperty("java.io.tmpdir") + "/totalImage";


    public static void main(String[] args) throws Exception{
        // 6.5k samples with 13 x 254 x 3 rgb channel images
        int height = 13;
        int width = 254;
        int channels = 3;

        int outputNum = 5;// 5개의 명령어로 분류.
        int batchSize = 64; // 미니 배치.

        int nEpochs =5; // 학습용 데이터 셋 전체를 훈련시키는 횟수.

        int seed = 1234;

        Random randNumGen = new Random(seed);

        LOGGER.info("data vertorization");

        //training data
        File trainData = new File(BASE_PATH + "/training");
        FileSplit trainSplit = new FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);


        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator(); // use parent directory name as the image label
        ImageRecordReader trainRR = new ImageRecordReader(height, width, channels, labelMaker);


        BalancedPathFilter pathFilter= new BalancedPathFilter(randNumGen, NativeImageLoader.ALLOWED_FORMATS, labelMaker);
        InputSplit[] filesInDirSplit = trainSplit.sample(pathFilter, 80, 20);
        InputSplit splitedtrainData = filesInDirSplit[0];
        InputSplit splitedtestData = filesInDirSplit[1];



        trainRR.initialize(splitedtrainData);
        DataSetIterator trainIter = new RecordReaderDataSetIterator(trainRR, batchSize, 1, outputNum);

        DataNormalization imageScaler = new ImagePreProcessingScaler();

        imageScaler.fit(trainIter);

        trainIter.setPreProcessor(imageScaler);





        //data for test
        File testData = new File(BASE_PATH + "/testing");
        FileSplit testSplit = new FileSplit(testData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);

        ImageRecordReader testRR = new ImageRecordReader(height, width, channels, labelMaker);

        testRR.initialize(splitedtestData);
        DataSetIterator testIter = new RecordReaderDataSetIterator(testRR, batchSize, 1, outputNum);
        imageScaler.fit(testIter);
        testIter.setPreProcessor(imageScaler);

        LOGGER.info("Network configuration and training...");

        //double nonZeroBias = 1;
        double dropOut = 0.5;// 전체 weight 중 50%만 사용하여 학습.

        // BatchNormalization 사용
        //BatchNormalization.Builder b = new BatchNormalization.Builder().nOut(40).eps();

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .l2(0.0005) // 과적합 방지를 위한 L2 정규화
            .weightInit(WeightInit.XAVIER) // weight초기화 방법, 입력값과 출력값 사이의 난수를 선택해서 입력값의 제곱근으로 나눔.
            .activation(Activation.RELU)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT) // 확률적 경사 하강 기법 사용.
            //.dist(new NormalDistribution(0.0, 0.01))
            .updater(new Nesterovs(new StepSchedule(ScheduleType.ITERATION, 1e-2,0.1,100000),0.9))
            // Nesteroves : helps gradient descent converges faster
            // .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
            //.biasUpdater(new Nesterovs(new StepSchedule(ScheduleType.ITERATION, 2e-2, 0.1,100000), 0.9))
            .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
            .list()
            .layer(new ConvolutionLayer.Builder(3,7)
                .nIn(channels)
                .stride(1,1)
                .nOut(40)
                .padding(1,3)
                .build())
            //.layer(new LocalResponseNormalization.Builder().name("local1").build())
            .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .kernelSize(2,2)
                .stride(1,1)
                .build()
            )
            .layer(new ConvolutionLayer.Builder(3,7)
                .stride(1,1) // nIn need not specified in later layers
                .nOut(50)
                .padding(1,3)
                .build())
            //.layer(new LocalResponseNormalization.Builder().name("local1").build())
            .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .kernelSize(2,2)
                .stride(1,1)
                .build()
            )
            .layer(new ConvolutionLayer.Builder(3,7)
                .padding(1,3)
                .stride(1,1)
                .nOut(60)
                .build()
            )
            //.layer(new LocalResponseNormalization.Builder().name("local2").build())
            // LocalResponseNormalization : 여러 feature-map에서의 결과를 normalization 시키면
            //생물학적 뉴런에서의 lateral inhibition(강한 자극이 주변의 약한 자극에 전달되는 것을 막는 효과)과
            //같은 효과를 얻을 수 있음, generalization 관점에서는 훨씬 좋아짐.
            .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .kernelSize(2,2)
                .stride(1,1)
                .build()
            )
            .layer(new DenseLayer.Builder()
                .nOut(500)
                .dropOut(dropOut)
                .build()
            )
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nOut(outputNum)
                .activation(Activation.SOFTMAX)
                .build())
            // .backprop(true)
            .setInputType(InputType.convolutionalFlat(height,width,channels))//6.5k samples with 13 x 254 x 3 RGB images
            .build();



        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(10));

        LOGGER.info("Total num of params: {}", net.numParams());


        for(int i =0; i< nEpochs; i++){
            net.fit(trainIter);
            Evaluation eval = net.evaluate(testIter);
            LOGGER.info("반복 회수 :" + i);
            LOGGER.info(eval.stats());

            trainIter.reset();
            testIter.reset();
        }

        File voiceRecogPath = new File(BASE_PATH + "/model-voiceRecognition(naan-300).zip");

        ModelSerializer.writeModel(net, voiceRecogPath, true);
        LOGGER.info("Model saved in -> " + voiceRecogPath.getPath() );



    }
}
