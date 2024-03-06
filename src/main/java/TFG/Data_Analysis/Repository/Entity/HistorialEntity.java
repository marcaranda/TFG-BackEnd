package TFG.Data_Analysis.Repository.Entity;

import TFG.Data_Analysis.Service.Model.DatasetModel;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.TreeMap;

@Document(collection = "UserHistorial")
public class HistorialEntity {
    //region Atributes
    @Id
    private long userId;
    private Map<String, TreeMap<Integer, DatasetModel>> versions;
    //endregion

    //region Constructors
    public HistorialEntity() {
    }

    public HistorialEntity(long userId, Map<String, TreeMap<Integer, DatasetModel>> versions) {
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
