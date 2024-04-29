package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Error.BadRequest;
import TFG.Data_Analysis.Helpers.Error.Forbidden;
import TFG.Data_Analysis.Helpers.Error.NotFound;
import TFG.Data_Analysis.Helpers.Error.ServerConflict;
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

    public DatasetModel fileReader(MultipartFile file, long userId, String rowsDenied) throws Forbidden, BadRequest, ServerConflict {
        if(new TokenValidator().validate_id_with_token(userId)) {
            if (file.isEmpty()) {
                throw new BadRequest("No file provided or the file was empty.");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {

                Map<Integer, Map<Integer, Pair<String, String>>> dataset = new HashMap<>();
                int numRow = 1;

                for (CSVRecord csvRecord : csvParser) {
                    Map<Integer, Pair<String, String>> row = new HashMap<>();
                    int numColumn = 1;

                    for (Map.Entry<String, Integer> entry : csvParser.getHeaderMap().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .toList()) {
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

                String[] rowsSeparated = rowsDenied.replaceAll("[^0-9,]", "").split(",");
                List<Integer> rowsToDenie = new ArrayList<>();
                for (String number : rowsSeparated) {
                    rowsToDenie.add(Integer.parseInt(number));
                }

                //double eigenEntropy = entropyService.getEigenEntropy(dataset);
                double eigenEntropy = entropyService.calculateEigenEntropy(dataset, rowsToDenie, false);
                return saveDataset(dataset, eigenEntropy, userId, datasetName, rowsToDenie);
            }
            catch (Exception e) {
                throw new ServerConflict("Error while reading the file");
            }
        }
        else {
            throw new Forbidden("El user_id enviado es diferente al especificado en el token");
        }
    }

    public void downloadFile(long datasetId, HttpServletResponse response) throws Forbidden, NotFound, ServerConflict {
        DatasetModel datasetModel = getDataset(datasetId);
        if (datasetModel == null) {
            throw new NotFound("La versión solicitada no existe.");
        }

        if(new TokenValidator().validate_id_with_token(datasetModel.getUserId())) {

            String fileName = datasetModel.getVersion() == 0 ? datasetModel.getDatasetName() + ".csv" : datasetModel.getDatasetName() + "_" + datasetModel.getVersion() + ".csv";
            // Configurar la respuesta para descargar el archivo
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            Map<Integer, Map<Integer, Pair<String, String>>> downloadDataset = datasetModel.getDataset();
            StringJoiner csvRow = new StringJoiner(",");

            try (PrintWriter csvWriter = response.getWriter()) {
                // Escribir encabezados de columnas
                Map<Integer, Pair<String, String>> firstRow = downloadDataset.get(1);
                for (Map.Entry<Integer, Pair<String, String>> entry : firstRow.entrySet()) {
                    //csvWriter.append(entry.getValue().getColumn());
                    //csvWriter.append(",");
                    csvRow.add(entry.getValue().getColumn());
                }
                csvWriter.append(csvRow.toString());
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
            catch (Exception e) {
                throw new ServerConflict("Error while reading the file");
            }
        }
        else {
            throw new Forbidden("El user_id enviado es diferente al especificado en el token");
        }
    }

    public  Map<String, List<DatasetModel>> getHistory(long userId, String order, String search, String datasetName) throws Forbidden {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();
            List<DatasetModel> datasets = datasetRepo.findAllByUserId(userId).stream()
                    .map(elementB -> modelMapper.map(elementB, DatasetModel.class))
                    .toList();

            Map<String, List<DatasetModel>> history = datasets.stream()
                    .collect(Collectors.groupingBy(DatasetModel::getDatasetName));

            if (datasetName != null && !datasetName.isEmpty()) {
                Map <String, List<DatasetModel>> aux = new HashMap<>();


                if (order != null && !order.isEmpty()) aux.put(datasetName, orderHistory(history.get(datasetName), order));
                else aux.put(datasetName, history.get(datasetName));

                history = aux;
            }
            else if (search != null && !search.isEmpty()) history = searchHistory(history, search);

            return history;
        }
        else {
            throw new Forbidden("El user_id enviado es diferente al especificado en el token");
        }
    }

    public DatasetModel applyFilter(List<String> filter, List<Boolean> rowsWanted, long datasetId) throws BadRequest, Forbidden, NotFound, ServerConflict {
        DatasetModel datasetModel = getDataset(datasetId);

        Map<Integer, Map<Integer, Pair<String, String>>> dataset = datasetModel.getDataset();
        Map<Integer, Map<Integer, Pair<String, String>>> newDataset = new HashMap<>();

        int numRow = 1;
        for (int i = 0; i < rowsWanted.size(); ++i) {
            if (rowsWanted.get(i)) {
                Map<Integer, Pair<String, String>> entry = dataset.get(i + 1);
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
        }

        if (newDataset.isEmpty()) {
            throw new BadRequest("No marked rows");
        }

        //double eigenEntropy = entropyService.getEigenEntropy(dataset);
        double eigenEntropy = entropyService.calculateEigenEntropy(newDataset, datasetModel.getRowsDenied(), true);
        return saveDataset(newDataset, eigenEntropy, datasetModel.getUserId(), datasetModel.getDatasetName(), new ArrayList<>());
    }

    public DatasetModel applySampleFilter(long datasetId, String improve, String type, int numInitialRows, int numWantedRows, double sliderValue, List<Boolean> initialRows) throws BadRequest, ServerConflict, Forbidden, NotFound {
        DatasetModel datasetModel = getDataset(datasetId);
        DatasetModel newDataset;

        if (type.equals("Incremental Sampling")) {
            newDataset = entropyService.sampleIncremental(datasetModel, numInitialRows, numWantedRows, initialRows, improve, sliderValue);
        } else if (type.equals("Elimination Sampling")) {
            newDataset = entropyService.sampleElimination(datasetModel, numWantedRows, improve);
        } else {
            throw new BadRequest("Incorrect Sample Filter Type");
        }
        return saveDataset(newDataset.getDataset(), newDataset.getEigenEntropy(), newDataset.getUserId(), newDataset.getDatasetName(), datasetModel.getRowsDenied());
    }

    //region DataBase
    private DatasetModel saveDataset(Map<Integer, Map<Integer, Pair<String, String>>> dataset, double eigenEntropy, long userId, String datasetName, List<Integer> rowsDenied) throws ServerConflict {
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

        DatasetModel datasetModel = new DatasetModel(datasetId, dataset, fileIds, eigenEntropy, userId, datasetName, version, dataset.size(), dataset.get(1).size(), rowsDenied);
        datasetRepo.save(modelMapper.map(datasetModel, DatasetEntity.class));

        return datasetModel;
    }

    public DatasetModel getDataset(long datasetId) throws NotFound, Forbidden {
        ModelMapper modelMapper = new ModelMapper();
        DatasetModel datasetModel = modelMapper.map(datasetRepo.findById(datasetId), DatasetModel.class);

        if (datasetModel == null) {
            throw new NotFound("La versión solicitada no existe.");
        }

        if(new TokenValidator().validate_id_with_token(datasetModel.getUserId())) {
            //DatasetModel datasetModel = modelMapper.map(datasetRepo.findByUserIdAndDatasetNameAndVersion(userId, datasetName, version), DatasetModel.class);
            datasetModel.setDataset(getDatasetMap(datasetModel.getFileIds()));
            return datasetModel;
        }
        else {
            throw new Forbidden("El user_id enviado es diferente al especificado en el token");
        }
    }

    public void deleteDataset(long datasetId) throws Forbidden {
        ModelMapper modelMapper = new ModelMapper();
        DatasetModel datasetModel = modelMapper.map(datasetRepo.findById(datasetId), DatasetModel.class);

        if(new TokenValidator().validate_id_with_token(datasetModel.getUserId())) {
            deleteDatasetMap(datasetModel.getFileIds());
            datasetRepo.delete(modelMapper.map(datasetModel, DatasetEntity.class));
        }
        else {
            throw new Forbidden("El user_id enviado es diferente al especificado en el token");
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

    private List<ObjectId> saveDatasetMap (Map<Integer, Map<Integer, Pair<String, String>>> dataset, long datasetId) throws ServerConflict {
        try {
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
        catch (Exception e) {
            throw new ServerConflict("Error while saving the file");
        }
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
                            .thenComparing(DatasetModel::getVersion).reversed())
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

    private Map<String, List<DatasetModel>> searchHistory (Map<String, List<DatasetModel>> history, String search) {
        Map<String, List<DatasetModel>> searchedHistory = new HashMap<>();

        String searchLowerCase = search.toLowerCase();

        for (Map.Entry<String, List<DatasetModel>> entry : history.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(searchLowerCase)) {
                searchedHistory.put(entry.getKey(), entry.getValue());
            }
        }

        return searchedHistory;
    }
    //endregion
}