package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Pair;
import TFG.Data_Analysis.Helpers.SimpleMatrixDeserializer;
import TFG.Data_Analysis.Repository.DatasetRepo;
import TFG.Data_Analysis.Repository.Entity.DatasetEntity;
import TFG.Data_Analysis.Security.TokenValidator;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.client.gridfs.model.GridFSFile;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.types.ObjectId;
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
    EntropyService entropyService;
    @Autowired
    GridFsTemplate gridFsTemplate;

    public DatasetModel fileReader(MultipartFile file, long userId) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            if (file.isEmpty()) {
                throw new Exception("No file provided or the file was empty.");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
                List<CSVRecord> rows = csvParser.getRecords();

                List<String> headers = csvParser.getHeaderNames();
                double[][] dataMatrix = new double[rows.size()][csvParser.getHeaderMap().size()];
                int numRow = 0;

                for (CSVRecord csvRecord : rows) {
                    int numColumn = 0;
                    for (Map.Entry<String, Integer> entry : csvParser.getHeaderMap().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .toList()) {
                        dataMatrix[numRow][numColumn] = Double.parseDouble(csvRecord.get(entry.getKey()));
                        ++numColumn;
                    }
                    ++numRow;
                }

                SimpleMatrix dataset = new SimpleMatrix(dataMatrix);

                String datasetName = file.getOriginalFilename();
                datasetName = datasetName.replace(".csv", "");

                //double eigenEntropy = entropyService.getEigenEntropy(dataset);
                double eigenEntropy = entropyService.calculateEigenEntropy(dataset);
                return saveDataset(dataset, headers, eigenEntropy, userId, datasetName);
            }
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public void downloadFile(long datasetId, HttpServletResponse response) throws Exception {
        DatasetModel datasetModel = getDataset(datasetId);
        if (datasetModel == null) {
            throw new Exception("La versión solicitada no existe.");
        }

        String fileName = datasetModel.getVersion() == 0 ? datasetModel.getDatasetName() + ".csv" : datasetModel.getDatasetName() + "_" + datasetModel.getVersion() + ".csv";
        // Configurar la respuesta para descargar el archivo
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        SimpleMatrix downloadDataset = datasetModel.getDataset();
        StringJoiner csvRow = new StringJoiner(",");

            try (PrintWriter csvWriter = response.getWriter()) {
                // Escribir encabezados de columnas
                /*Map<Integer, Pair<String, String>> firstRow = downloadDataset.get(1);
                for (Map.Entry<Integer, Pair<String, String>> entry : firstRow.entrySet()) {
                    //csvWriter.append(entry.getValue().getColumn());
                    //csvWriter.append(",");
                    csvRow.add(entry.getValue().getColumn());
                }
                csvWriter.append(csvRow.toString());
                csvWriter.append("\n");*/

            // Escribir datos
            for (int i = 0; i < downloadDataset.getNumRows(); ++i) {
                for (int j = 0; j < downloadDataset.getNumCols(); ++i) {
                    csvWriter.append(String.valueOf(downloadDataset.get(i, j)));
                    csvWriter.append(",");
                }
                csvWriter.append("\n");
            }

            csvWriter.flush();
        }
    }

    public List<DatasetModel> getHistory(long userId, String order) throws Exception {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();
            List<DatasetModel> history = datasetRepo.findAllByUserId(userId).stream()
                    .map(elementB -> modelMapper.map(elementB, DatasetModel.class))
                    .collect(Collectors.toList());

            if (order != null && !order.isEmpty()) history = orderHistory(history, order);

            return history;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public DatasetModel applyFilter(List<String> filter, List<Boolean> rowsWanted, long datasetId) throws Exception {
        DatasetModel datasetModel = getDataset(datasetId);

        SimpleMatrix dataset = datasetModel.getDataset();
        List<String> headers = datasetModel.getHeaders();
        List<List<Double>> auxNewDataMatrix = new ArrayList<>();

        for (int i = 0; i < dataset.getNumRows(); ++i) {
            if (rowsWanted.get(i)) {
                List<Double> newRow = new ArrayList<>();
                for (int j = 0; j < dataset.getNumCols(); ++j) {
                    if (filter.contains(headers.get(j))) {
                        newRow.add(dataset.get(i, j));
                    }
                }
                if (!newRow.isEmpty()) {
                    auxNewDataMatrix.add(newRow);
                }
            }
        }

        double[][] newDataMatrix = new double[auxNewDataMatrix.size()][];
        for (int i = 0; i < auxNewDataMatrix.size(); ++i) {
            List<Double> row = auxNewDataMatrix.get(i);
            newDataMatrix[i] = new double[row.size()];
            for (int j = 0; j < row.size(); ++j) {
                newDataMatrix[i][j] = row.get(j);
            }
        }

        SimpleMatrix newDataset = new SimpleMatrix(newDataMatrix);

        //double eigenEntropy = entropyService.getEigenEntropy(newDataset);
        double eigenEntropy = entropyService.calculateEigenEntropy(newDataset);
        return saveDataset(newDataset, headers, eigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName());
    }

    public DatasetModel applySampleFilter(long datasetId, String improve, String type, int numInitialRows, int numWantedRows, double sliderValue, List<Boolean> initialRows) throws Exception {
        DatasetModel datasetModel = getDataset(datasetId);
        DatasetModel newDataset;

        if (type.equals("Incremental Sampling")) {
            newDataset = entropyService.sampleIncremental(datasetModel, numInitialRows, numWantedRows, initialRows, improve, sliderValue);
        } else if (type.equals("Elimination Sampling")) {
            newDataset = entropyService.sampleElimination(datasetModel, numWantedRows, improve);
        } else {
            throw new Exception("Incorrect Sample Filter Type");
        }

        /*
        if (improve.equals("Homogeneity") && type.equals("Incremental Sampling")) {
            newDataset = entropyService.sampleHomoIncremental(datasetModel, numInitialRows, numWantedRows, initialRows);
        } else if (improve.equals("Homogeneity") && type.equals("Elimination Sampling")) {
            newDataset = entropyService.sampleHomoElimination(datasetModel, numWantedRows);
        } else if (improve.equals("Heterogeneity") && type.equals("Incremental Sampling")) {
            newDataset = entropyService.sampleHeteIncremental(datasetModel, numInitialRows, numWantedRows, initialRows);
        } else if (improve.equals("Heterogeneity") && type.equals("Elimination Sampling")){
            newDataset = entropyService.sampleHeteElimination(datasetModel, numWantedRows);
        } else {
            throw new Exception("Incorrect Sample Filter Type");
        }*/
        return saveDataset(newDataset.getDataset(), datasetModel.getHeaders(), newDataset.getEigenEntropy(), newDataset.getUserId(), newDataset.getDatasetName());
    }

    //region DataBase
    private DatasetModel saveDataset(SimpleMatrix dataset, List<String> headers, double eigenEntropy, long userId, String datasetName) throws JsonProcessingException {
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
        List<ObjectId> headerIds = saveHeaders(headers, datasetId);

        DatasetModel datasetModel = new DatasetModel(datasetId, dataset, headers, fileIds, headerIds, eigenEntropy, userId, datasetName, version, dataset.getNumRows(), dataset.getNumCols());
        datasetRepo.save(modelMapper.map(datasetModel, DatasetEntity.class));

        return datasetModel;
    }

    public DatasetModel getDataset(long datasetId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        DatasetModel datasetModel = modelMapper.map(datasetRepo.findById(datasetId), DatasetModel.class);

        if(new TokenValidator().validate_id_with_token(datasetModel.getUserId())) {
            datasetModel.setDataset(getDatasetMap(datasetModel.getFileIds()));
            datasetModel.setHeaders(getHeaders(datasetModel.getHeaderIds()));
            return datasetModel;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

    public void deleteDataset(long datasetId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        DatasetModel datasetModel = modelMapper.map(datasetRepo.findById(datasetId), DatasetModel.class);

        if(new TokenValidator().validate_id_with_token(datasetModel.getUserId())) {
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

    private List<ObjectId> saveDatasetMap (SimpleMatrix dataset, long datasetId) throws JsonProcessingException {
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

    private List<ObjectId> saveHeaders(List<String> headers, long datasetId) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(headers);
        byte[] datasetBytes = json.getBytes(StandardCharsets.UTF_8);

        final int MAX_SIZE = 16 * 1024 * 1024; //16MB
        int length = datasetBytes.length;
        int offset = 0;
        List<ObjectId> headerIds = new ArrayList<>();

        while (offset < length) {
            int chunkSize = Math.min(length - offset, MAX_SIZE);
            byte[] chunk = Arrays.copyOfRange(datasetBytes, offset, offset + chunkSize);

            ObjectId headerId = gridFsTemplate.store(new ByteArrayInputStream(chunk), "header" + Long.toString(datasetId) + "-" + headerIds.size());
            headerIds.add(headerId);

            offset += chunkSize;
        }

        return headerIds;
    }

    private SimpleMatrix getDatasetMap (List<ObjectId> fileIds) {
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
            SimpleModule module = new SimpleModule();
            module.addDeserializer(SimpleMatrix.class, new SimpleMatrixDeserializer());
            objectMapper.registerModule(module);
            return objectMapper.readValue(json, SimpleMatrix.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getHeaders (List<ObjectId> headerIds) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            List<GridFSFile> files = new ArrayList<>();
            for (ObjectId fileId : headerIds) {
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
            TypeReference<List<String>> typeRef = new TypeReference<>() {
            };
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDatasetMap(List<ObjectId> fileIds) {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileIds)));
    }

    private List<DatasetModel> orderHistory (List<DatasetModel> history, String order) {
        List<DatasetModel> orderedHistory = new ArrayList<>();

        switch (order) {
            case "name" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getDatasetName)
                            .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
            case "-name" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getDatasetName).reversed()
                            .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
            case "entropy" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getEigenEntropy).reversed()
                            .thenComparing(DatasetModel::getDatasetName)
                            .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
            case "-entropy" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getEigenEntropy)
                                .thenComparing(DatasetModel::getDatasetName)
                                .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
            case "row" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getRows).reversed()
                                .thenComparing(DatasetModel::getDatasetName)
                                .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
            case "-row" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getRows)
                                .thenComparing(DatasetModel::getDatasetName)
                                .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
            case "column" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getColumns).reversed()
                                .thenComparing(DatasetModel::getDatasetName)
                                .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
            case "-column" -> {
                orderedHistory = history.stream()
                        .sorted(Comparator.comparing(DatasetModel::getColumns)
                                .thenComparing(DatasetModel::getDatasetName)
                                .thenComparing(DatasetModel::getVersion))
                        .collect(Collectors.toList());
            }
        }

        return orderedHistory;
    }
    //endregion
}