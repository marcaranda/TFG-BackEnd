package TFG.Data_Analysis.Repository;

import TFG.Data_Analysis.Repository.Entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends CrudRepository<UserEntity, Long> {
    @Query("SELECT u FROM UserEntity u WHERE u.deleted = false AND c.user_id = :user_id")
    public abstract Optional<UserEntity> findById(@Param("user_id") Long user_id);
}
