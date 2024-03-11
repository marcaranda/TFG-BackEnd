package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Repository.DatasetRepo;
import TFG.Data_Analysis.Repository.Entity.DatasetEntity;
import TFG.Data_Analysis.Security.TokenValidator;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ejml.dense.row.decomposition.eig.SwitchingEigenDecomposition_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class DatasetService {
    @Autowired
    DatasetRepo datasetRepo;
    @Autowired
    UserService userService;
    @Autowired
    HistorialService historialService;
    private Map<String, TreeMap<Integer, DatasetModel>> versions = new HashMap<>();

    public DatasetModel fileReader(MultipartFile file, long userId) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            if (file.isEmpty()) {
                throw new Exception("No file provided or the file was empty.");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
                Map<String, Integer> headerMap = csvParser.getHeaderMap();

                Map<Integer, Map<Integer, Map<String, Double>>> dataset = new HashMap<>();
                int numRow = 1;

                for (CSVRecord csvRecord : csvParser) {
                    Map<Integer, Map<String, Double>> row = new HashMap<>();
                    int numColumn = 1;

                    for (String columnName : headerMap.keySet()) {
                        Map<String, Double> rowValue = new LinkedHashMap<>();
                        String columnValueString = csvRecord.get(columnName);
                        Double columnValue = Double.valueOf(columnValueString.replaceAll("[^0-9]", ""));
                        // Procesar los datos como se requiera
                        rowValue.put(columnName, columnValue);
                        row.put(numColumn, rowValue);
                        ++numColumn;
                    }
                    dataset.put(numRow, row);
                    ++numRow;
                }

                String datasetName = file.getOriginalFilename();
                datasetName = datasetName.replace(".csv", "");

                double eigenEntropy = calculateEigenEntropy(dataset);
                 return saveDataset(dataset, eigenEntropy, userId, datasetName);
            }
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public void downloadFile(String datasetName, Integer downloadVersion, HttpServletResponse response) throws Exception {
        if (!versions.get(datasetName).containsKey(downloadVersion)) {
            throw new Exception("La versión solicitada no existe.");
        }

        String fileName = downloadVersion == 0 ? datasetName + ".csv" : datasetName + "_" + downloadVersion + ".csv";
        // Configurar la respuesta para descargar el archivo
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        Map<Integer, Map<Integer, Map<String, Double>>> downloadDataset = versions.get(datasetName).get(downloadVersion).getDataset();

        try (PrintWriter csvWriter = response.getWriter()) {
            // Escribir encabezados de columnas
            Map<String, Double> firstRow = downloadDataset.entrySet().iterator().next().getValue().entrySet().iterator().next().getValue();
            for (String columnName : firstRow.keySet()) {
                csvWriter.append(columnName);
                csvWriter.append(",");
            }
            csvWriter.append("\n");

            // Escribir datos
            for (Map<Integer, Map<String, Double>> row : downloadDataset.values()) {
                for (Map<String, Double> rowData : row.values()) {
                    for (Double value : rowData.values()) {
                        csvWriter.append(String.valueOf(value));
                        csvWriter.append(",");
                    }
                    csvWriter.append("\n");
                }
            }

            csvWriter.flush();
        }
    }

    public List<String> getHistorial (long userId) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            List<String> historial = new ArrayList<>();

            for (Map.Entry<String, TreeMap<Integer, DatasetModel>> entry : versions.entrySet()) {
                TreeMap<Integer, DatasetModel> innerMap = entry.getValue();
                String datasetName = entry.getKey();

                for (Map.Entry<Integer, DatasetModel> innerEntry : innerMap.entrySet()) {
                    historial.add(innerEntry.getKey() == 0 ? datasetName : datasetName + '_' + innerEntry.getKey());
                }
            }
            return historial;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public double applyFilter(List<String> filter, long userId, String datasetName) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            Map<Integer, Map<Integer, Map<String, Double>>> newDataset = new HashMap<>();
            Map<Integer, Map<Integer, Map<String, Double>>> originalDataset = getDataset(userId, datasetName, 0).getDataset();
            System.out.println(originalDataset);

            int numRow = 1;
            for (Map<Integer, Map<String, Double>> entry : originalDataset.values()) {
                Map<Integer, Map<String, Double>> row = new HashMap<>();
                int numColumn = 1;
                for (Map<String, Double> subEntry : entry.values()) {

                    for (Map.Entry<String, Double> valueEntry : subEntry.entrySet()) {
                        Map<String, Double> rowValue = new HashMap<>();
                        if (filter.contains(valueEntry.getKey())) {
                            rowValue.put(valueEntry.getKey(), valueEntry.getValue());
                            row.put(numColumn, rowValue);
                            ++numColumn;
                        }
                    }
                }
                newDataset.put(numRow, row);
                numRow++;
            }

            System.out.println(newDataset);

            double eigenEntropy = calculateEigenEntropy(newDataset);
            saveDataset(newDataset, eigenEntropy, userId, datasetName);
            return eigenEntropy;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    private double calculateEigenEntropy(Map<Integer, Map<Integer, Map<String, Double>>> dataset) {
        SimpleMatrix dataMatrix = new SimpleMatrix(convertToMatrix(dataset));
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

    private double[][] convertToMatrix (Map<Integer, Map<Integer, Map<String, Double>>> dataset) {
        int numRows = dataset.size() - 1;
        int numColumns = dataset.isEmpty() ? 0 : dataset.entrySet().iterator().next().getValue().size() - 1;

        double[][] dataMatrix;
        if (numRows > numColumns) {
            dataMatrix = new double[numRows][numRows];
        }
        else {
            dataMatrix = new double[numColumns][numColumns];
        }

        int row = 0;
        for (Map.Entry<Integer, Map<Integer, Map<String, Double>>> entry : dataset.entrySet()) {
            Map<Integer, Map<String, Double>> rowsValues = entry.getValue();
            for (Map.Entry<Integer, Map<String, Double>> subEntry : rowsValues.entrySet()) {
                Map<String, Double> values = subEntry.getValue();
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
                    } else {
                        firstColumn = false;
                    }
                }
            }
            ++row;
        }

        return dataMatrix;
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

    //region DataBase
    private DatasetModel saveDataset(Map<Integer, Map<Integer, Map<String, Double>>> dataset, double eigenEntropy, long userId, String datasetName){
        ModelMapper modelMapper = new ModelMapper();

        long datasetId = autoIncrementId();
        TreeMap<Integer, DatasetModel> datasetVersions = versions.get(datasetName);
        int version;
        if (datasetVersions == null || datasetVersions.isEmpty()) {
            datasetVersions = new TreeMap<>();
            version = 0;
        }
        else {
            version = datasetVersions.lastKey() + 1;
        }

        DatasetModel datasetModel = new DatasetModel(datasetId, dataset, eigenEntropy, userId, datasetName, version);

        datasetVersions.put(version, datasetModel);
        versions.put(datasetName, datasetVersions);

        datasetRepo.save(modelMapper.map(datasetModel, DatasetEntity.class));
        historialService.saveDataset(userId, versions);
        return datasetModel;
    }

    private long autoIncrementId() {
        ModelMapper modelMapper = new ModelMapper();
        List<DatasetModel> userDatasets = new ArrayList<>();

        datasetRepo.findAll().forEach(elementB -> userDatasets.add(modelMapper.map(elementB, DatasetModel.class)));

        return userDatasets.isEmpty() ? 1 :
                userDatasets.stream().max(Comparator.comparing(DatasetModel::getDatasetId)).get().getDatasetId() + 1;
    }

    public DatasetModel getDataset(long userId, String datasetName, Integer version) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();

            return modelMapper.map(datasetRepo.findByUserIdAndDatasetNameAndVersion(userId, datasetName, version), DatasetModel.class);
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public void chargeUserDatasets(String email) {
        versions = historialService.chargeUserDatasets(email);
    }

    public void deleteDataset(long userId, String datasetName, Integer version) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();

            TreeMap<Integer, DatasetModel> datasetVersions = versions.get(datasetName);
            datasetVersions.remove(version);
            versions.put(datasetName, datasetVersions);
            historialService.saveDataset(userId, versions);

            DatasetModel datasetModel = modelMapper.map(datasetRepo.findByUserIdAndDatasetNameAndVersion(userId, datasetName, version), DatasetModel.class);
            datasetRepo.delete(modelMapper.map(datasetModel, DatasetEntity.class));
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }
    //endregion
}

















