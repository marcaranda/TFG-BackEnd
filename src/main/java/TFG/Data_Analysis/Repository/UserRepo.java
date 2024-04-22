package TFG.Data_Analysis.Repository;

import TFG.Data_Analysis.Repository.Entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends MongoRepository<UserEntity, Long> {
    @Query("{ 'deleted' : false, 'user_id' : ?0 }")
    Optional<UserEntity> findById(Long user_id);

    UserEntity findByEmail(String email);
}
