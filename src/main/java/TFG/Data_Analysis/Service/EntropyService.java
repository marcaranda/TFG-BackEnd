package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Pair;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.decomposition.eig.SwitchingEigenDecomposition_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class EntropyService {

    //region Calculate Eigen Entropy
    public double calculateEigenEntropy(Map<Integer, Map<Integer, Pair<String, String>>> dataset) throws Exception {
        SimpleMatrix dataMatrix = new SimpleMatrix(convertToMatrix(dataset));
        dataMatrix = convertToCorrelationMatrix(dataMatrix);

        if (!MatrixFeatures_DDRM.isSymmetric(dataMatrix.getDDRM(), 1e-8)) {
            throw new Exception("La matriz no es simétrica.");
        }

        SwitchingEigenDecomposition_DDRM eigDecomp = (SwitchingEigenDecomposition_DDRM) DecompositionFactory_DDRM.eig(dataMatrix.numRows(), true);
        eigDecomp.decompose(dataMatrix.getMatrix());

        double sum = 0;
        for (int i = 0; i < eigDecomp.getNumberOfEigenvalues(); i++) {
            double eigenvalue = eigDecomp.getEigenvalue(i).getReal();
            sum += eigenvalue;
        }

        // Calcular entropía de Shannon para los valores propios normalizados
        double eigenEntropy = 0;
        for (int i = 0; i < eigDecomp.getNumberOfEigenvalues(); i++) {
            double eigenvalue = eigDecomp.getEigenvalue(i).getReal();
            double p = eigenvalue / sum;
            if (p > 0) {
                eigenEntropy -= p * Math.log(p);
            }
        }

        return eigenEntropy;
    }

    private double[][] convertToMatrix (Map<Integer, Map<Integer, Pair<String, String>>> dataset) {
        int numRows = dataset.size();
        int numColumns = dataset.isEmpty() ? 0 : dataset.entrySet().iterator().next().getValue().size() - 1;

        double[][] dataMatrix = new double[numRows][numColumns];

        int row = 0;
        for (Map<Integer, Pair<String, String>> entry : dataset.values()) {
            int column = 0;
            for (Pair<String, String> value : entry.values()) {
                if (column > 0) {
                    if (value.getValue() != null ) {
                        dataMatrix[row][column - 1] = Double.parseDouble(value.getValue());
                    }
                }
                ++column;
            }
            ++row;
        }

        return dataMatrix;
    }

    private SimpleMatrix convertToCorrelationMatrix (SimpleMatrix dataMatrix) {
        int numCols = dataMatrix.getNumCols();
        SimpleMatrix correlationMatrix = new SimpleMatrix(numCols, numCols);

        for (int i = 0; i < numCols; i++) {
            for (int j = i; j < numCols; j++) {
                double correlation = computeCorrelation(dataMatrix.extractVector(false, i), dataMatrix.extractVector(false, j));
                correlationMatrix.set(i, j, correlation);
                correlationMatrix.set(j, i, correlation);  // La matriz de correlación es simétrica
            }
        }

        return correlationMatrix;
    }

    private static double computeCorrelation(SimpleMatrix vectorX, SimpleMatrix vectorY) {
        double meanX = vectorX.elementSum() / vectorX.getNumRows();
        double meanY = vectorY.elementSum() / vectorY.getNumRows();

        SimpleMatrix centeredX = vectorX.minus(meanX);
        SimpleMatrix centeredY = vectorY.minus(meanY);

        double stdDevX = centeredX.normF();
        double stdDevY = centeredY.normF();

        if (stdDevX == 0 || stdDevY == 0) {
            return 0;
        }

        double covariance = centeredX.dot(centeredY);
        return covariance / (stdDevX * stdDevY);
    }
    //endregion

    //region Sample
    //Reduce number of rows to improve Homogenity
    public DatasetModel sampleHomoReduce(DatasetModel datasetModel) {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();
        double eigenEntropy = datasetModel.getEigenEntropy();


        return new DatasetModel(newDataset, eigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    //Increase number of rows to improve Homogenity
    public DatasetModel sampleHomoIncrease(DatasetModel datasetModel, int numExtraRows) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = dataset;
        double eigenEntropy = datasetModel.getEigenEntropy();
        double newEigenEntropy = 0;

        int numRows = dataset.size();
        int numMaxRows = numRows + numExtraRows;
        int numNewRow = numRows + 1;

        while (numNewRow <= numMaxRows){
            Map<Integer, Pair<String, String>> newRow = createArtificialRow(dataset);
            newDataset.put(numNewRow, newRow);
            
            newEigenEntropy = calculateEigenEntropy(newDataset);
            
            if (newEigenEntropy < eigenEntropy) ++numNewRow;
            else newDataset.remove(numNewRow);
        }

        return new DatasetModel(newDataset, newEigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    //Reduce number of rows to improve Heterogenity
    public DatasetModel sampleHeteReduce(DatasetModel datasetModel) {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();
        double eigenEntropy = datasetModel.getEigenEntropy();


        return new DatasetModel(newDataset, eigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    //Increase number of rows to improve Heterogenity
    public DatasetModel sampleHeteIncrease(DatasetModel datasetModel, int numExtraRows) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = dataset;
        double eigenEntropy = datasetModel.getEigenEntropy();
        double newEigenEntropy = 0;

        int numRows = dataset.size();
        int numMaxRows = numRows + numExtraRows;
        int numNewRow = numRows + 1;

        while (numNewRow <= numMaxRows){
            Map<Integer, Pair<String, String>> newRow = createArtificialRow(dataset);
            newDataset.put(numNewRow, newRow);

            newEigenEntropy = calculateEigenEntropy(newDataset);

            if (newEigenEntropy > eigenEntropy) ++numNewRow;
            else newDataset.remove(numNewRow);
        }

        return new DatasetModel(newDataset, newEigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    private Map<Integer, Pair<String, String>> createArtificialRow (Map<Integer, Map<Integer, Pair<String, String>>> dataset) {
        //Coger dos filas aleatorias del dataset original
        Random random = new Random();
        int index1 = random.nextInt(dataset.size()) + 1;
        int index2 = random.nextInt(dataset.size()) + 1;
        while (index1 == index2) index2 = random.nextInt(dataset.size()) + 1;
        Map<Integer, Pair<String, String>> row1 = dataset.get(index1);
        Map<Integer, Pair<String, String>> row2 = dataset.get(index2);

        Map<Integer, Pair<String, String>> newRow = new HashMap<>();
        for (int i = 1; i <= dataset.get(1).size(); ++i) {
            double newValue = interpolate(Double.parseDouble(row1.get(i).getValue()), Double.parseDouble(row2.get(i).getValue()));
            Pair<String, String> rowValue = new Pair<>(row1.get(i).getColumn(), String.valueOf(newValue));
            newRow.put(i, rowValue);
        }
        return newRow;
    }

    private double interpolate (double value1, double value2) {
        Random random = new Random();
        double lambda = random.nextDouble();
        return value1 * (1 - lambda) + value2 * lambda;
    }
    //endregion
}
