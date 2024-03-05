package TFG.Data_Analysis.Service;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class FileService {
    private Map<Integer, Map<String, Double>> dataset = new HashMap<>();
    private Map<Integer, Map<Integer, Map<String, Double>>> versions = new HashMap<>();
    private int version = 0;

    public double fileReader(String path) throws IOException {
        try (Reader reader = Files.newBufferedReader(Paths.get(path));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            Map<String, Integer> headerMap = csvParser.getHeaderMap();

            int numRow = 1;
            for (CSVRecord csvRecord : csvParser) {
                Map<String, Double> row = new LinkedHashMap<>();

                for (String columnName : headerMap.keySet()) {
                    Double columnValue = Double.valueOf(csvRecord.get(columnName));
                    // Procesar los datos como se requiera
                    row.put(columnName, columnValue);
                }
                dataset.put(numRow, row);
                ++numRow;
            }
        }

        versions.put(version, dataset);
        ++version;

        return calculateEigenEntropy(dataset);
    }

    public void downloadFile(Integer downloadVersion, HttpServletResponse response) throws Exception {
        if (!versions.containsKey(downloadVersion)) {
            throw new Exception("La versión solicitada no existe.");
        }

        String fileName = downloadVersion == 0 ? "Dataset.csv" : "Dataset_" + downloadVersion + ".csv";
        // Configurar la respuesta para descargar el archivo
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        Map<Integer, Map<String, Double>> downloadDataset = versions.get(downloadVersion);

        try (PrintWriter csvWriter = response.getWriter()) {
            // Escribir encabezados de columnas
            Map<String, Double> firstRow = downloadDataset.entrySet().iterator().next().getValue();
            for (String columnName : firstRow.keySet()) {
                csvWriter.append(columnName);
                csvWriter.append(",");
            }
            csvWriter.append("\n");

            // Escribir datos
            for (Map<String, Double> rowData : downloadDataset.values()) {
                for (Double value : rowData.values()) {
                    csvWriter.append(String.valueOf(value));
                    csvWriter.append(",");
                }
                csvWriter.append("\n");
            }

            csvWriter.flush();
        }
    }

    public double applyFilter(List<String> filter) {
        Map<Integer, Map<String, Double>> newDataset = new HashMap<>();
        int numRow = 1;
        for (Map<String, Double> entry : dataset.values()) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String columnName : filter) {
                Double columnValue = entry.get(columnName); // Obtener el valor del mapa usando la clave
                row.put(columnName, columnValue);
            }
            newDataset.put(numRow, row);
            numRow++;
        }

        versions.put(version, newDataset);
        ++version;

        return calculateEigenEntropy(newDataset);
    }

    public double calculateEigenEntropy(Map<Integer, Map<String, Double>> dataset) {
        double[][] dataMatrix = convertToMatrix(dataset);
        Array2DRowRealMatrix realMatrix = new Array2DRowRealMatrix(dataMatrix);
        EigenDecomposition eigenDecomposition = new EigenDecomposition(realMatrix);
        double[] eigenValues = eigenDecomposition.getRealEigenvalues();

        double eigenEntropy = 0.0;
        for (double lambda: eigenValues) {
            if (lambda > 0) eigenEntropy -= lambda * Math.log(lambda);
        }
        return eigenEntropy;
    }

    public double[][] convertToMatrix (Map<Integer, Map<String, Double>> dataset) {
        int numRows = dataset.size();
        int numColumns = dataset.isEmpty() ? 0 : dataset.entrySet().iterator().next().getValue().size();

        Double[][] dataMatrix;
        if (numRows > numColumns) {
            dataMatrix = new Double[numRows][numRows];
        }
        else {
            dataMatrix = new Double[numColumns][numColumns];
        }

        int row = 0;
        for (Map.Entry<Integer, Map<String, Double>> entry : dataset.entrySet()) {
            Map<String, Double> values = entry.getValue();
            boolean firstColumn = true;
            int column = 0;
            for (Double value : values.values()) {
                if (!firstColumn) {
                    if (value != null) {
                        dataMatrix[row][column] = value;
                    } else {
                        dataMatrix[row][column] = 0.0;
                    }
                    ++column;
                }
                else {
                    firstColumn = false;
                }
            }
            ++row;
        }

        double[][] finalDataMatrix = new double[dataMatrix.length][dataMatrix.length];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (dataMatrix[i][j] == null) {
                    finalDataMatrix[i][j] = 0; // Establecer cualquier valor nulo como 0
                }
                else {
                    finalDataMatrix[i][j] = dataMatrix[i][j];
                }
            }
        }

        return finalDataMatrix;
    }
}