package TFG.Data_Analysis.Repository.Entity;

import TFG.Data_Analysis.Helpers.Pair;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "Datasets")
public class DatasetEntity {
    //region Dataset Attribute
    @Id
    private long datasetId;
    private List<ObjectId> fileIds;
    private double eigenEntropy;
    private long userId;
    private String datasetName;
    private long version;
    //endregion

    //region Getters & Setters
    public long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
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
