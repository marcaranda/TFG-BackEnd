package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Pair;
import TFG.Data_Analysis.Repository.DatasetRepo;
import TFG.Data_Analysis.Repository.Entity.DatasetEntity;
import TFG.Data_Analysis.Security.TokenValidator;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.types.ObjectId;
import org.ejml.dense.row.decomposition.eig.SwitchingEigenDecomposition_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatasetService {
    @Autowired
    DatasetRepo datasetRepo;
    @Autowired
    GridFsTemplate gridFsTemplate;

    public DatasetModel fileReader(MultipartFile file, long userId) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            if (file.isEmpty()) {
                throw new Exception("No file provided or the file was empty.");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
                Map<String, Integer> headerMap = csvParser.getHeaderMap();

                Map<Integer, Map<Integer, Pair<String, String>>> dataset = new HashMap<>();
                int numRow = 1;

                for (CSVRecord csvRecord : csvParser) {
                    Map<Integer, Pair<String, String>> row = new HashMap<>();
                    int numColumn = 1;

                    for (Map.Entry<String, Integer> entry : csvParser.getHeaderMap().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .collect(Collectors.toList())) {
                        String columnName = entry.getKey();
                        String columnValue = csvRecord.get(columnName);
                        // Procesar los datos como se requiera
                        Pair<String, String> rowValue = new Pair<>(columnName, columnValue);
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

    public void downloadFile(Long userId, String datasetName, Integer downloadVersion, HttpServletResponse response) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            DatasetModel dataset = getDataset(userId, datasetName, downloadVersion);
            if (dataset == null) {
                throw new Exception("La versión solicitada no existe.");
            }

            String fileName = downloadVersion == 0 ? datasetName + ".csv" : datasetName + "_" + downloadVersion + ".csv";
            // Configurar la respuesta para descargar el archivo
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            Map<Integer, Map<Integer, Pair<String, String>>> downloadDataset = dataset.getDataset();

            try (PrintWriter csvWriter = response.getWriter()) {
                // Escribir encabezados de columnas
                Map<Integer, Pair<String, String>> firstRow = downloadDataset.get(1);
                for (Map.Entry<Integer, Pair<String, String>> entry : firstRow.entrySet()) {
                    csvWriter.append(entry.getValue().getColumn());
                    csvWriter.append(",");
                }
                csvWriter.append("\n");

                // Escribir datos
                for (Map<Integer, Pair<String, String>> row : downloadDataset.values()) {
                    for (Pair<String, String> rowData : row.values()) {
                        csvWriter.append(String.valueOf(rowData.getValue()));
                        csvWriter.append(",");
                    }
                    csvWriter.append("\n");
                }

                csvWriter.flush();
            }

        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }

    }

    public List<DatasetModel> getHistory(long userId) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();
            List<DatasetModel> history = new ArrayList<>();

            datasetRepo.findAllByUserId(userId).forEach(elementB -> {
                DatasetModel datasetModel = modelMapper.map(elementB, DatasetModel.class);
                datasetModel.setDataset(getDatasetMap(datasetModel.getFileIds()));
                history.add(datasetModel);
            });
            return history;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public double applyFilter(List<String> filter, long userId, String datasetName, int version) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();
            Map<Integer, Map<Integer, Pair<String, String>>> originalDataset = getDataset(userId, datasetName, version).getDataset();

            int numRow = 1;
            for (Map<Integer, Pair<String, String>> entry : originalDataset.values()) {
                Map<Integer, Pair<String, String>> row = new HashMap<>();
                int numColumn = 1;
                for (Pair<String, String> subEntry : entry.values()) {
                    if (filter.contains(subEntry.getColumn())) {
                        Pair<String, String> rowValue = new Pair<>(subEntry.getColumn(), subEntry.getValue());
                        row.put(numColumn, rowValue);
                        ++numColumn;
                    }
                }
                newDataset.put(numRow, row);
                numRow++;
            }

            double eigenEntropy = calculateEigenEntropy(newDataset);
            saveDataset(newDataset, eigenEntropy, userId, datasetName);
            return eigenEntropy;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    private double calculateEigenEntropy(Map<Integer, Map<Integer, Pair<String, String>>> dataset) {
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

    private double[][] convertToMatrix (Map<Integer, Map<Integer, Pair<String, String>>> dataset) {
        int numRows = dataset.size();
        int numColumns = dataset.isEmpty() ? 0 : dataset.entrySet().iterator().next().getValue().size() - 1;

        double[][] dataMatrix;
        if (numRows > numColumns) {
            dataMatrix = new double[numRows][numRows];
        }
        else {
            dataMatrix = new double[numColumns][numColumns];
        }

        int row = 0;
        for (Map<Integer, Pair<String, String>> entry : dataset.values()) {
            int column = 0;
            for (Pair<String, String> value : entry.values()) {
                if (column > 0) {
                    if (value.getValue() != null) {
                        dataMatrix[row][column - 1] = Double.parseDouble(value.getValue());
                    } else {
                        dataMatrix[row][column - 1] = 0.0;
                    }
                }
                ++column;
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
    private DatasetModel saveDataset(Map<Integer, Map<Integer, Pair<String, String>>> dataset, double eigenEntropy, long userId, String datasetName) throws JsonProcessingException {
        ModelMapper modelMapper = new ModelMapper();

        List<DatasetModel> datasetsVersions = new ArrayList<>();
        datasetRepo.findAllByUserIdAndDatasetName(userId, datasetName).forEach(elementB -> datasetsVersions.add(modelMapper.map(elementB, DatasetModel.class)));
        long version = 0;


        if (!datasetsVersions.isEmpty()) {
            for (DatasetModel datasetVersion : datasetsVersions) {
                if (datasetVersion.getVersion() + 1 > version) version = datasetVersion.getVersion() + 1;
            }
        }

        long datasetId = autoIncrementId();

        List<ObjectId> fileIds = saveDatasetMap(dataset, datasetId);

        DatasetModel datasetModel = new DatasetModel(datasetId, dataset, fileIds, eigenEntropy, userId, datasetName, version);
        datasetRepo.save(modelMapper.map(datasetModel, DatasetEntity.class));

        return datasetModel;
    }

    public DatasetModel getDataset(long userId, String datasetName, Integer version) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();

            DatasetModel datasetModel = modelMapper.map(datasetRepo.findByUserIdAndDatasetNameAndVersion(userId, datasetName, version), DatasetModel.class);
            datasetModel.setDataset(getDatasetMap(datasetModel.getFileIds()));
            return datasetModel;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public void deleteDataset(long userId, String datasetName, Integer version) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();

            DatasetModel datasetModel = modelMapper.map(datasetRepo.findByUserIdAndDatasetNameAndVersion(userId, datasetName, version), DatasetModel.class);
            deleteDatasetMap(datasetModel.getFileIds());
            datasetRepo.delete(modelMapper.map(datasetModel, DatasetEntity.class));
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }
    //endregion

    //region Private Methods
    private long autoIncrementId() {
        ModelMapper modelMapper = new ModelMapper();
        List<DatasetModel> userDatasets = new ArrayList<>();

        datasetRepo.findAll().forEach(elementB -> userDatasets.add(modelMapper.map(elementB, DatasetModel.class)));

        return userDatasets.isEmpty() ? 1 :
                userDatasets.stream().max(Comparator.comparing(DatasetModel::getDatasetId)).get().getDatasetId() + 1;
    }

    private List<ObjectId> saveDatasetMap (Map<Integer, Map<Integer, Pair<String, String>>> dataset, long datasetId) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(dataset);
        byte[] datasetBytes = json.getBytes(StandardCharsets.UTF_8);

        final int MAX_SIZE = 16 * 1024 * 1024; //16MB
        int length = datasetBytes.length;
        int offset = 0;
        List<ObjectId> fileIds = new ArrayList<>();

        while (offset < length) {
            int chunkSize = Math.min(length - offset, MAX_SIZE);
            byte[] chunk = Arrays.copyOfRange(datasetBytes, offset, offset + chunkSize);

            ObjectId fileId = gridFsTemplate.store(new ByteArrayInputStream(chunk), Long.toString(datasetId) + "-" + fileIds.size());
            fileIds.add(fileId);

            offset += chunkSize;
        }


        return fileIds;
    }

    private Map<Integer, Map<Integer, Pair<String, String>>> getDatasetMap (List<ObjectId> fileIds) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            List<GridFSFile> files = new ArrayList<>();
            for (ObjectId fileId : fileIds) {
                GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));
                if (gridFSFile != null) {
                    files.add(gridFSFile);
                }
            }

            for (GridFSFile file : files) {
                GridFsResource resource = gridFsTemplate.getResource(file);
                InputStream inputStream = resource.getInputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }

            String json = baos.toString(StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<Map<Integer, Map<Integer, Pair<String, String>>>> typeRef = new TypeReference<>() {
            };
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDatasetMap(List<ObjectId> fileIds) {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileIds)));
    }
    //endregion
}

















