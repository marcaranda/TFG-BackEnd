package TFG.Data_Analysis.Repository;

import TFG.Data_Analysis.Repository.Entity.DatasetEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepo extends MongoRepository<DatasetEntity, Long> {
    @Query("{ 'userId' : ?0, 'datasetName' :  ?1, 'version' :  ?2}")
    DatasetEntity findByUserIdAndDatasetNameAndVersion(long userId, String datasetName, int version);

    @Query("{ 'userId' : ?0}")
    List<DatasetEntity> findAllByUserId(long userId);

    @Query("{ 'userId' : ?0, 'datasetName' :  ?1}")
    List<DatasetEntity> findAllByUserIdAndDatasetName(long userId, String datasetName);
}
