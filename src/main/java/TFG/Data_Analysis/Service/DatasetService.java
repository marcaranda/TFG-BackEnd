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

                double eigenEntropy = entropyService.calculateEigenEntropy(dataset);
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

            double eigenEntropy = entropyService.calculateEigenEntropy(newDataset);
            saveDataset(newDataset, eigenEntropy, userId, datasetName);
            return eigenEntropy;
        }
        else {
            throw new Exception("El user_id enviado es diferente al especificado en el token");
        }
    }

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

















