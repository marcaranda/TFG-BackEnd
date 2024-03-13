package TFG.Data_Analysis.Service.Model;

import TFG.Data_Analysis.Helpers.Pair;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

public class DatasetModel {
    //region Dataset Attribute
    private long datasetId;
    private Map<Integer, Map<Integer, Pair<String, String>>> dataset;
    private List<ObjectId> fileIds;
    private double eigenEntropy;
    private long userId;
    private String datasetName;
    private long version;
    //endregion

    //region Constructor
    public DatasetModel() {
    }

    public DatasetModel(long datasetId, Map<Integer, Map<Integer, Pair<String, String>>> dataset, List<ObjectId> fileIds, double eigenEntropy, long userId, String datasetName, long version) {
        this.datasetId = datasetId;
        this.dataset = dataset;
        this.fileIds = fileIds;
        this.eigenEntropy = eigenEntropy;
        this.userId = userId;
        this.datasetName = datasetName;
        this.version = version;
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
    //endregion
}
