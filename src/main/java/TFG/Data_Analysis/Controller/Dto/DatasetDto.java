package TFG.Data_Analysis.Controller.Dto;

import TFG.Data_Analysis.Helpers.Pair;
import org.bson.types.ObjectId;
import org.ejml.simple.SimpleMatrix;

import java.util.List;
import java.util.Map;

public class DatasetDto {
    //region Dataset Attribute
    private long datasetId;
    private SimpleMatrix dataset;
    private List<String> headers;
    private List<ObjectId> fileIds;
    private List<ObjectId> headerIds;
    private double eigenEntropy;
    private long userId;
    private String datasetName;
    private long version;
    private int rows;
    private int columns;
    //endregion

    //region Constructor
    public DatasetDto() {
    }

    public DatasetDto(long datasetId, SimpleMatrix dataset, List<String> headers, List<ObjectId> fileIds, List<ObjectId> headerIds, double eigenEntropy, long userId, String datasetName, long version, int rows, int columns) {
        this.datasetId = datasetId;
        this.dataset = dataset;
        this.headers = headers;
        this.fileIds = fileIds;
        this.headerIds = headerIds;
        this.eigenEntropy = eigenEntropy;
        this.userId = userId;
        this.datasetName = datasetName;
        this.version = version;
        this.rows = rows;
        this.columns = columns;
    }

    //endregion

    //region Getters & Setters
    public long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
    }

    public SimpleMatrix getDataset() {
        return dataset;
    }

    public void setDataset(SimpleMatrix dataset) {
        this.dataset = dataset;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<ObjectId> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<ObjectId> fileIds) {
        this.fileIds = fileIds;
    }

    public List<ObjectId> getHeaderIds() {
        return headerIds;
    }

    public void setHeaderIds(List<ObjectId> headerIds) {
        this.headerIds = headerIds;
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
    //endregion
}
