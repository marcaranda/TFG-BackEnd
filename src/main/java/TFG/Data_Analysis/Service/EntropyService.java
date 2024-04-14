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
import java.util.stream.Collectors;

@Service
public class EntropyService {

    private List<Integer> initialIndex;
    private boolean end;

    //region Calculate Eigen Entropy
    public double getEigenEntropy(Map<Integer, Map<Integer, Pair<String, String>>> dataset) throws Exception {
        dataset = normalizeMap(dataset);
        return calculateEigenEntropy(dataset);
    }

    public double calculateEigenEntropy(Map<Integer, Map<Integer, Pair<String, String>>> dataset) throws Exception {
        SimpleMatrix dataMatrix = new SimpleMatrix(convertToMatrix(dataset));
        dataMatrix = convertToCorrelationMatrix(dataMatrix);

        if (!MatrixFeatures_DDRM.isSymmetric(dataMatrix.getDDRM(), 1e-8)) {
            throw new Exception("La matriz no es simétrica.");
        }

        SwitchingEigenDecomposition_DDRM eigDecomp = (SwitchingEigenDecomposition_DDRM) DecompositionFactory_DDRM.eig(dataMatrix.getNumRows(), true);
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
    //Desde un pequeño dataset S de X de numInitialRows --> añadir filas hasta numRowsWanted
    public DatasetModel sampleIncremental(DatasetModel datasetModel, int numInitialRows, int numRowsWanted, List<Boolean> initialRows, String improve, double sliderValue) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        //Map<Integer, Map<Integer, Pair<String, String>>> normDataset = normalizeMap(datasetModel.getDataset());
        Map<Integer, Map<Integer, Pair<String, String>>> normDataset = dataset;
        if (numRowsWanted >= dataset.size() && numInitialRows >= dataset.size()) {
            throw new Exception("Number of wanted rows can't be higher or equal to the current rows");
        }

        initialIndex = new ArrayList<>();
        end = false;
        int numNewRow = numInitialRows + 1;

        Map<Integer, Map<Integer, Pair<String, String>>> auxDataset = initialReducedDataset(normDataset, numInitialRows, initialRows);
        double eigenEntropy = calculateEigenEntropy(auxDataset);
        double initialEigenEntropy = eigenEntropy;

        while (numNewRow <= numRowsWanted && !end) {
            Map<Integer, Pair<String, String>> newRow = getRow(normDataset);
            if (newRow != null) {
                auxDataset.put(numNewRow, newRow);

                double newEigenEntropy = calculateEigenEntropy(auxDataset);

                if (improve.equals("Homogeneity")) {
                    if (newEigenEntropy < eigenEntropy) {
                        ++numNewRow;
                        eigenEntropy = newEigenEntropy;
                    } else {
                        auxDataset.remove(numNewRow);
                    }
                } else if (improve.equals("Heterogeneity")) {
                    if (newEigenEntropy > eigenEntropy) {
                        ++numNewRow;
                        eigenEntropy = newEigenEntropy;
                    } else {
                        auxDataset.remove(numNewRow);
                    }
                } else {
                    throw new Exception("Incorrect Sample Filter Type");
                }
            }

            if (sliderValue > (initialEigenEntropy / eigenEntropy) * 100) end = true;
        }

        int numRow = 1;
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Pair<String, String>>> entryRow : auxDataset.entrySet()) {
            newDataset.put(numRow, dataset.get(entryRow.getKey()));
            ++numRow;
        }

        return new DatasetModel(newDataset, eigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    //Dado un dataset X --> quita filas hasta que X tenga numRowsWanted
    public DatasetModel sampleElimination(DatasetModel datasetModel, int numRowsWanted, String improve) throws Exception {
        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        //Map<Integer, Map<Integer, Pair<String, String>>> auxDataset = normalizeMap(dataset);
        Map<Integer, Map<Integer, Pair<String, String>>> auxDataset = dataset;
        if (numRowsWanted >= dataset.size()) {
            throw new Exception("Number of wanted rows can't be higher or equal to the current rows");
        }

        initialIndex = new ArrayList<>();
        end = false;
        double newEigenEntropy = 0;
        double eigenEntropy = datasetModel.getEigenEntropy();
        int numDeletedRows = 0;
        int numRowsToDelete = dataset.size() - numRowsWanted;


        Random random = new Random();
        while (numDeletedRows < numRowsToDelete && !end) {
            if (initialIndex.size() >= dataset.size()){
                end = true;
            }
            else {
                int index = random.nextInt(dataset.size()) + 1;
                while (initialIndex.contains(index)) index = random.nextInt(dataset.size()) + 1;
                initialIndex.add(index);

                Map<Integer, Pair<String, String>> row = auxDataset.get(index);
                auxDataset.remove(index);

                newEigenEntropy = calculateEigenEntropy(auxDataset);

                if (improve.equals("Homogeneity")) {
                    if (newEigenEntropy < eigenEntropy) {
                        ++numDeletedRows;
                        eigenEntropy = newEigenEntropy;
                    } else auxDataset.put(index, row);
                } else if (improve.equals("Heterogeneity")) {
                    if (newEigenEntropy > eigenEntropy) {
                        ++numDeletedRows;
                        eigenEntropy = newEigenEntropy;
                    }
                    else auxDataset.put(index, row);
                } else {
                    throw new Exception("Incorrect Sample Filter Type");
                }
            }
        }

        int numRow = 1;
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Pair<String, String>>> entryRow : auxDataset.entrySet()) {
            newDataset.put(numRow, dataset.get(entryRow.getKey()));
            ++numRow;
        }

        return new DatasetModel(newDataset, eigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    private Map<Integer, Map<Integer, Pair<String, String>>> initialReducedDataset(Map<Integer, Map<Integer, Pair<String, String>>> dataset, int numInitialRows, List<Boolean> initialRows) throws Exception {
        Random random = new Random();
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();

        int numRow = 1;
        for (int i = 0; i < initialRows.size(); ++i) {
            if (initialRows.get(i)) {
                Map<Integer, Pair<String, String>> entry = dataset.get(i + 1);
                Map<Integer, Pair<String, String>> row = new HashMap<>();
                int numColumn = 1;
                for (Pair<String, String> subEntry : entry.values()) {
                    Pair<String, String> rowValue = new Pair<>(subEntry.getColumn(), subEntry.getValue());
                    row.put(numColumn, rowValue);
                    ++numColumn;
                }
                newDataset.put(numRow, row);
                numRow++;
                initialIndex.add(i + 1);
            }
        }

        if (newDataset.size() > numInitialRows) {
            throw new Exception("Incorrect number of marked rows");
        }

        while (numRow <= numInitialRows) {
            int index = random.nextInt(dataset.size()) + 1;
            while (initialIndex.contains(index)) index = random.nextInt(dataset.size()) + 1;
            initialIndex.add(index);

            Map<Integer, Pair<String, String>> row = dataset.get(index);
            newDataset.put(numRow, row);
            ++numRow;
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

    private Map<Integer, Map<Integer, Pair<String, String>>> normalizeMap (Map<Integer, Map<Integer, Pair<String, String>>> dataset) {
        DoubleSummaryStatistics maxmin = dataset.values().stream()
                .flatMap(row -> row.entrySet().stream()
                        .filter(entry -> entry.getKey() > 1)
                        .map(Map.Entry::getValue))
                .mapToDouble(pair -> Double.parseDouble(pair.getValue()))
                .summaryStatistics();
        double max = maxmin.getMax();
        double min = maxmin.getMin();

        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();
        int numRow = 1;
        for (Map.Entry<Integer, Map<Integer, Pair<String, String>>> entryRow : dataset.entrySet()) {
            Map<Integer, Pair<String, String>> entry = entryRow.getValue();
            Map<Integer, Pair<String, String>> row = new HashMap<>();
            int numColumn = 1;
            for (Pair<String, String> subEntry : entry.values()) {
                String stringValue = subEntry.getValue().replaceAll("[^0-9]", "");
                double value = (Double.parseDouble(stringValue) - min) / (max - min);
                Pair<String, String> rowValue = new Pair<>(subEntry.getColumn(), String.valueOf(value));
                row.put(numColumn, rowValue);
                ++numColumn;
            }
            newDataset.put(numRow, row);
            numRow++;
        }

        return newDataset;
    }
    //endregion
}
