package com.nhnacademy.aimnistpractice.service;

import com.nhnacademy.aimnistpractice.config.ModelProperties;
import com.nhnacademy.aimnistpractice.dto.MnistPredictionRequest;
import com.nhnacademy.aimnistpractice.dto.MnistPredictionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.LocalFileModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.ClassificationScoreCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.ScoreImprovementEpochTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MnistModelService {
    private final ModelProperties modelProperties;
    private MultiLayerNetwork trainedModel;

    @EventListener(ApplicationReadyEvent.class)
    public void initModel() throws IOException {
        File modelFile = new File(modelProperties.getModelPath());
        if(modelFile.exists()) {
            log.info("Load model from file {}", modelFile.getAbsolutePath());
            trainedModel = ModelSerializer.restoreMultiLayerNetwork(modelFile);
            return;
        }
        log.info("No model file found, Starting training...");
        trainAndSaveModel();
    }

    private void trainAndSaveModel() throws IOException {
        int seed = 7919;
        int batchSize = 64;
        int epoch = 100;
        int patience = 10;

        String modelPath = modelProperties.getModelPath();

        DataSetIterator trainIter = new MnistDataSetIterator(batchSize, true, seed);
        DataSetIterator testIter = new MnistDataSetIterator(batchSize, false, seed);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(new Adam(1e-3))
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(28*28)
                        .nOut(256)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(256)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(128)
                        .nOut(10)
                        .activation(Activation.SOFTMAX)
                        .build())
                .build();
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        EarlyStoppingConfiguration<MultiLayerNetwork> esConf =
                new EarlyStoppingConfiguration.Builder<MultiLayerNetwork>()
                        .epochTerminationConditions(
                                new MaxEpochsTerminationCondition(epoch),
                                new ScoreImprovementEpochTerminationCondition(patience)
                        )
                        .evaluateEveryNEpochs(1)
                        .scoreCalculator(
                                new ClassificationScoreCalculator(
                                        Evaluation.Metric.ACCURACY,
                                        testIter
                                )
                        )
                        .modelSaver(
                                new LocalFileModelSaver(modelProperties.getModelSaverPath())
                        )
                        .build();

        EarlyStoppingTrainer trainer =
                new EarlyStoppingTrainer(esConf, model, trainIter);

        EarlyStoppingResult<MultiLayerNetwork> result = trainer.fit();

        MultiLayerNetwork bestModel = result.getBestModel();
        ModelSerializer.writeModel(bestModel, modelPath, true);
        trainedModel = bestModel;
        log.info("Model Successfully saved: {}", modelPath);
    }

    public MnistPredictionResponse predict(MnistPredictionRequest request) {
        if (trainedModel == null) {
            throw new IllegalStateException("MNIST model not found");
        }

        INDArray output;
        try (INDArray input = Nd4j.create(request.pixels()).reshape(1, 28L * 28)) {
            output = trainedModel.output(input, false);
        }

        double[] outputValues = output.toDoubleVector();

        if (outputValues.length < 10) {
            throw new IllegalStateException("Invalid output value length. expect: 10, actual: " + outputValues.length);
        }

        Map<Integer, Double> probabilities = new LinkedHashMap<>();
        int prediction = 0;
        for (int digit = 0; digit < 10; digit++) {
            double probability = outputValues[digit];
            probabilities.put(digit, probability);
            if (probability > outputValues[prediction]) {
                prediction = digit;
            }
        }

        return new MnistPredictionResponse(prediction, probabilities);
    }
}
