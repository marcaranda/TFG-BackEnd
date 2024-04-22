package TFG.Data_Analysis.Controller.Dto;

import TFG.Data_Analysis.Helpers.Pair;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

public class DatasetDto {
    //region Dataset Attribute
    private long datasetId;
    private Map<Integer, Map<Integer, Pair<String, String>>> dataset;
    private List<ObjectId> fileIds;
    private double eigenEntropy;
    private long userId;
    private String datasetName;
    private long version;
    private int rows;
    private int columns;
    private List<Integer> rowsDenied;
    //endregion

    //region Constructor
    public DatasetDto() {
    }

    public DatasetDto(long datasetId, Map<Integer, Map<Integer, Pair<String, String>>> dataset, List<ObjectId> fileIds, double eigenEntropy, long userId, String datasetName, long version, int rows, int columns, List<Integer> rowsDenied) {
        this.datasetId = datasetId;
        this.dataset = dataset;
        this.fileIds = fileIds;
        this.eigenEntropy = eigenEntropy;
        this.userId = userId;
        this.datasetName = datasetName;
        this.version = version;
        this.rows = rows;
        this.columns = columns;
        this.rowsDenied = rowsDenied;
    }

    //endregion

    //region Getters & Setters
    public long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
    }

    public Map<Integer, Map<Integer, Pair<String, String>>> getDataset() {
        return dataset;
    }

    public void setDataset(Map<Integer, Map<Integer, Pair<String, String>>> dataset) {
        this.dataset = dataset;
    }

    public List<ObjectId> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<ObjectId> fileIds) {
        this.fileIds = fileIds;
    }

    public double getEigenEntropy() {
        return eigenEntropy;
    }

    public void setEigenEntropy(double eigenEntropy) {
        this.eigenEntropy = eigenEntropy;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public List<Integer> getRowsDenied() {
        return rowsDenied;
    }

    public void setRowsDenied(List<Integer> rowsDenied) {
        this.rowsDenied = rowsDenied;
    }

    //endregion
}
