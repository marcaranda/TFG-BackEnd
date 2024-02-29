package TFG.Data_Analysis.Service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileReaderService {
    /*public void fileReader(String path) throws IOException {
        Map<Integer, Map<String, Float>> dataset = new HashMap<>();
        int numFila = 1;

        try (Reader reader = Files.newBufferedReader(Paths.get(path));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            Map<String, Integer> headerMap = csvParser.getHeaderMap();

            for (CSVRecord csvRecord : csvParser) {
                Map<String, Float> fila = new HashMap<>();
                for (String columnName : headerMap.keySet()) {
                    Float columnValue = Float.valueOf(csvRecord.get(columnName));
                    // Procesar los datos como se requiera
                    fila.put(columnName, columnValue);
                }
                dataset.put(numFila, fila);
                ++numFila;
            }
        }
    }*/

    public double fileReader(String path) throws IOException {
        ArrayList<ArrayList<Double>> dataset = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(Paths.get(path));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            Map<String, Integer> headerMap = csvParser.getHeaderMap();

            for (CSVRecord csvRecord : csvParser) {
                ArrayList<Double> fila = new ArrayList<>();
                boolean primeraColumna = true;

                for (String columnName : headerMap.keySet()) {
                    if (!primeraColumna) {
                        Double columnValue = Double.valueOf(csvRecord.get(columnName));
                        // Procesar los datos como se requiera
                        fila.add(columnValue);
                    }
                    else primeraColumna = false;
                }
                dataset.add(fila);
            }
        }

        return calculateEigenEntropy(dataset);
    }

    public double calculateEigenEntropy(ArrayList<ArrayList<Double>> dataset) {
        double[][] dataMatrix = convertToMatrix(dataset);
        Array2DRowRealMatrix realMatrix = new Array2DRowRealMatrix(dataMatrix);
        EigenDecomposition eigenDecomposition = new EigenDecomposition(realMatrix);
        double[] eigenValues = eigenDecomposition.getRealEigenvalues();

        double eigenEntropy = 0.0;
        for (double lambda: eigenValues) {
            if (lambda > 0) eigenEntropy -= lambda * Math.log(lambda);
            System.out.println(lambda + " - " + lambda * Math.log(lambda));
        }
        return eigenEntropy;
    }

    public double[][] convertToMatrix (ArrayList<ArrayList<Double>> dataset) {
        double[][] dataMatrix = new double[dataset.size()][];
        for (int i = 0; i < dataset.size(); ++i) {
            ArrayList<Double> fila = dataset.get(i);
            dataMatrix[i] = fila.stream().mapToDouble(Double::doubleValue).toArray();
        }
        return dataMatrix;
    }
}
