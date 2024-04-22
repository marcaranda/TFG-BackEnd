package TFG.Data_Analysis.Repository.Entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

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
    private int rows;
    private int columns;
    private List<Integer> rowsDenied;
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
