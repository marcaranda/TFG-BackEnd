package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Exception;
import TFG.Data_Analysis.Repository.Entity.UserEntity;
import TFG.Data_Analysis.Repository.UserRepo;
import TFG.Data_Analysis.Service.Model.UserModel;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserService {
    @Autowired
    UserRepo userRepo;

    //region Get Methods
    public ArrayList<UserModel> getUsers() throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        ArrayList<UserModel> users = new ArrayList<>();

        userRepo.findAll().forEach(elementB -> users.add(modelMapper.map(elementB, UserModel.class)));
        return users;
    }

    public UserModel getUserId(Long userId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(userRepo.findById(userId), UserModel.class);
    }
    //endregion

    //region Post/Put Methods
    public UserModel saveUser(UserModel user) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(userRepo.save(modelMapper.map(user, UserEntity.class)), UserModel.class);
    }
    //endregion

    //region Delete Methods
    public void deleteUser(Long userId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        UserModel user = getUserId(userId);
        if (user == null) {
            throw new Exception("Error al intentar eliminar el usuario. EL usuario no existe.");
        }

        user.setDeleted(true);
        saveUser(user);
    }
    //endregion
}
