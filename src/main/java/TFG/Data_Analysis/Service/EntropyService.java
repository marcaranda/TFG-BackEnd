package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Pair;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.decomposition.eig.SwitchingEigenDecomposition_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EntropyService {
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

    //region Sample
    /*public double homogeneusSamples(Integer newRows) {
        Map<Integer, Map<String, Double>> newDataset = dataset;
        int numRow = dataset.size() + 1;
        double newEigenEntropy = 0.0;

        for (int i = 0; i < newRows; ++i) {
            Map<String, Double> artificialRow = generateArtificialRow(newDataset);

            newDataset.put(numRow, artificialRow);
            double posibleEigenEntropy = calculateEigenEntropy(newDataset);
            if (posibleEigenEntropy < eigenEntropy) {
                newEigenEntropy = posibleEigenEntropy;
                ++numRow;
            }
            else {
                newDataset.remove(numRow);
                --i;
            }
        }

        return newEigenEntropy;
    }

    private Map<String, Double> generateArtificialRow(Map<Integer, Map<String, Double>> newDataset) {
        List<Integer> keys = new ArrayList<>(newDataset.keySet());
        Integer randomKey = keys.get(new Random().nextInt(keys.size()));
        Map<String, Double> randomSample = newDataset.get(randomKey);

        int k = 2;
        List<Map<String, Double>> neighbors = findKNearestNeighbors(newDataset, randomSample, k);

        Map<String, Double> neighborSample = neighbors.get(new Random().nextInt(neighbors.size()));

        return generateNewRow(randomSample, neighborSample);
    }

    private List<Map<String, Double>> findKNearestNeighbors(Map<Integer, Map<String, Double>> newDataset, Map<String, Double> randomSample, int k) {
        return newDataset.values().stream()
                .sorted(Comparator.comparingDouble(s -> calculateEuclideanDistance(randomSample, s)))
                .limit(k)
                .collect(Collectors.toList());
    }

    private double calculateEuclideanDistance(Map<String, Double> sample1, Map<String, Double> sample2) {
        double sum = 0;
        for (String key : sample1.keySet()) {
            double diff = sample1.get(key) - sample2.getOrDefault(key, 0.0);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private Map<String, Double> generateNewRow(Map<String, Double> randomSample, Map<String, Double> neighborSample) {
         Map<String, Double> artificialRow = new LinkedHashMap<>();
         Random random = new Random();
         for (String key : randomSample.keySet()) {
             double diff = neighborSample.get(key) - randomSample.get(key);
             double gap = random.nextDouble();
             artificialRow.put(key, randomSample.get(key) + gap * diff);
         }

         return artificialRow;
    }*/
    //endregion
}
