package TFG.Data_Analysis.Service.Model;

import java.util.Map;

public class DatasetModel {
    //region Dataset Attribute
    private long datasetId;
    private Map<Integer, Map<String, Double>> dataset;
    private double eigenEntropy;
    private long userId;
    //endregion

    //region Constructor
    public DatasetModel() {
    }

    public DatasetModel(long datasetId, Map<Integer, Map<String, Double>> dataset, double eigenEntropy, long userId) {
        this.datasetId = datasetId;
        this.dataset = dataset;
        this.eigenEntropy = eigenEntropy;
        this.userId = userId;
    }
    //endregion

    //region Getters & Setters
    public long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
    }

    public Map<Integer, Map<String, Double>> getDataset() {
        return dataset;
    }

    public void setDataset(Map<Integer, Map<String, Double>> dataset) {
        this.dataset = dataset;
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
    //endregion
}
