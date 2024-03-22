package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Exception;
import TFG.Data_Analysis.Helpers.Pair;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.decomposition.eig.SwitchingEigenDecomposition_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EntropyService {

    private List<Integer> initialIndex;
    private boolean end;

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
                correlationMatrix.set(j, i, correlation);
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
    public DatasetModel sampleHomoReduce(DatasetModel datasetModel, int numInitialRows, int numRowsWanted) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        if (numRowsWanted >= dataset.size() && numInitialRows >= dataset.size()) {
            throw new Exception("Number of wanted rows can't be higher or equal to the current rows");
        }

        initialIndex = new ArrayList<>();
        end = false;
        double newEigenEntropy = 0;
        int numNewRow = numInitialRows + 1;

        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = initialReducedDataset(dataset, numInitialRows);
        double eigenEntropy = calculateEigenEntropy(newDataset);

        while (numNewRow <= numRowsWanted && !end) {
            Map<Integer, Pair<String, String>> newRow = getRow(dataset);
            if (newRow != null) {
                newDataset.put(numNewRow, newRow);

                newEigenEntropy = calculateEigenEntropy(newDataset);

                if (newEigenEntropy < eigenEntropy) ++numNewRow;
                else newDataset.remove(numNewRow);
            }
        }

        return new DatasetModel(newDataset, newEigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    //Increase number of rows to improve Homogenity
    public DatasetModel sampleHomoIncrease(DatasetModel datasetModel, int numRowsWanted) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        if (numRowsWanted >= dataset.size()) {
            throw new Exception("Number of wanted rows can't be higher or equal to the current rows");
        }

        initialIndex = new ArrayList<>();
        end = false;
        double newEigenEntropy = 0;
        int numDeletedRows = 0;

        Map<Integer, Map<Integer, Pair<String, String>>> auxDataset = dataset;
        double eigenEntropy = datasetModel.getEigenEntropy();

        Random random = new Random();
        while (numDeletedRows < numRowsWanted && !end) {
            if (initialIndex.size() >= dataset.size()){
                end = true;
            }
            else {
                int index = random.nextInt(dataset.size()) + 1;
                while (initialIndex.contains(index)) index = random.nextInt(dataset.size()) + 1;
                initialIndex.add(index);

                Map<Integer, Pair<String, String>> row = dataset.get(index);
                auxDataset.remove(index);

                newEigenEntropy = calculateEigenEntropy(auxDataset);

                if (newEigenEntropy < eigenEntropy) ++numDeletedRows;
                else auxDataset.put(index, row);
            }
        }

        int numRow = 1;
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();

        for (Map<Integer, Pair<String, String>> row : auxDataset.values()) {
            newDataset.put(numRow, row);
            ++numRow;
        }

        return new DatasetModel(newDataset, newEigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    //Reduce number of rows to improve Heterogenity
    public DatasetModel sampleHeteReduce(DatasetModel datasetModel, int numInitialRows, int numRowsWanted) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        if (numRowsWanted >= dataset.size() && numInitialRows >= dataset.size()) {
            throw new Exception("Number of wanted rows can't be higher or equal to the current rows");
        }

        initialIndex = new ArrayList<>();
        end = false;
        double newEigenEntropy = 0;
        int numNewRow = numInitialRows + 1;

        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = initialReducedDataset(dataset, numInitialRows);
        double eigenEntropy = calculateEigenEntropy(newDataset);

        while (numNewRow <= numRowsWanted && !end) {
            Map<Integer, Pair<String, String>> newRow = getRow(dataset);
            if (newRow != null) {
                newDataset.put(numNewRow, newRow);

                newEigenEntropy = calculateEigenEntropy(newDataset);

                if (newEigenEntropy > eigenEntropy) ++numNewRow;
                else newDataset.remove(numNewRow);
            }
        }

        return new DatasetModel(newDataset, newEigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    //Increase number of rows to improve Heterogenity
    public DatasetModel sampleHeteIncrease(DatasetModel datasetModel, int numRowsWanted) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        if (numRowsWanted >= dataset.size()) {
            throw new Exception("Number of wanted rows can't be higher or equal to the current rows");
        }

        initialIndex = new ArrayList<>();
        end = false;
        double newEigenEntropy = 0;
        int numDeletedRows = 0;

        Map<Integer, Map<Integer, Pair<String, String>>> auxDataset = dataset;
        double eigenEntropy = datasetModel.getEigenEntropy();

        Random random = new Random();
        while (numDeletedRows < numRowsWanted && !end) {
            if (initialIndex.size() >= dataset.size()){
                end = true;
            }
            else {
                int index = random.nextInt(dataset.size()) + 1;
                while (initialIndex.contains(index)) index = random.nextInt(dataset.size()) + 1;
                initialIndex.add(index);

                Map<Integer, Pair<String, String>> row = dataset.get(index);
                auxDataset.remove(index);

                newEigenEntropy = calculateEigenEntropy(auxDataset);

                if (newEigenEntropy > eigenEntropy) ++numDeletedRows;
                else auxDataset.put(index, row);
            }
        }

        int numRow = 1;
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();

        for (Map<Integer, Pair<String, String>> row : auxDataset.values()) {
            newDataset.put(numRow, row);
            ++numRow;
        }

        return new DatasetModel(newDataset, newEigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    private Map<Integer, Map<Integer, Pair<String, String>>> initialReducedDataset(Map<Integer, Map<Integer, Pair<String, String>>> dataset, int initialRows) {
        Random random = new Random();
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();

        for (int i = 1; i <= initialRows; ++i) {
            int index = random.nextInt(dataset.size()) + 1;
            while (initialIndex.contains(index)) index = random.nextInt(dataset.size()) + 1;
            initialIndex.add(index);

            Map<Integer, Pair<String, String>> row = dataset.get(index);
            newDataset.put(i, row);
        }

        return newDataset;
    }

    private Map<Integer, Pair<String, String>> getRow(Map<Integer, Map<Integer, Pair<String, String>>> dataset) {
        Random random = new Random();

        if (initialIndex.size() >= dataset.size()){
            end = true;
            return null;
        }
        else {
            int index = random.nextInt(dataset.size()) + 1;
            while (initialIndex.contains(index)) index = random.nextInt(dataset.size()) + 1;
            initialIndex.add(index);
            return dataset.get(index);
        }
    }
    //endregion
}
