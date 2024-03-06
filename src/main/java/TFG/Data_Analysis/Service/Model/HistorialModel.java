package TFG.Data_Analysis.Service.Model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.TreeMap;

@Document(collection = "UserHistorial")
public class HistorialModel {
    //region Atributes
    private long userId;
    private Map<String, TreeMap<Integer, DatasetModel>> versions;
    //endregion

    //region Constructors
    public HistorialModel() {
    }

    public HistorialModel(long userId, Map<String, TreeMap<Integer, DatasetModel>> versions) {
        this.userId = userId;
        this.versions = versions;
    }
    //endregion

    //region Getters & Setters
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Map<String, TreeMap<Integer, DatasetModel>> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, TreeMap<Integer, DatasetModel>> versions) {
        this.versions = versions;
    }
    //endregion
}
