package TFG.Data_Analysis.Repository;

import TFG.Data_Analysis.Repository.Entity.HistorialEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialRepository extends MongoRepository<HistorialEntity, Long> {
    @Query("{ 'userId' :  ?0}")
    HistorialEntity findByUserId(Long userId);
}
