package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Error.BadRequest;
import TFG.Data_Analysis.Helpers.Error.Forbidden;
import TFG.Data_Analysis.Helpers.Error.NotFound;
import TFG.Data_Analysis.Repository.Entity.UserEntity;
import TFG.Data_Analysis.Repository.UserRepo;
import TFG.Data_Analysis.Security.TokenValidator;
import TFG.Data_Analysis.Service.Model.UserModel;
import org.mindrot.jbcrypt.BCrypt;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {
    @Autowired
    UserRepo userRepo;

    //region Get Methods
    public ArrayList<UserModel> getUsers() {
        ModelMapper modelMapper = new ModelMapper();
        ArrayList<UserModel> users = new ArrayList<>();

        userRepo.findAll().forEach(elementB -> users.add(modelMapper.map(elementB, UserModel.class)));
        return users;
    }

    public UserModel getUser(Long userId) throws NotFound, Forbidden {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();
            UserModel user = modelMapper.map(userRepo.findById(userId), UserModel.class);

            if (user == null) {
                throw new NotFound("User not found");
            }

            return user;
        }
        else {
            throw new Forbidden("El userId enviado es diferente al especificado en el token");
        }
    }
    //endregion

    //region Post/Put Methods
    public UserModel saveUser(UserModel user) throws BadRequest {
        ModelMapper modelMapper = new ModelMapper();

        /* Comprobación validez correo electrónico */
        if(!validateEmail(user.getEmail())) {
            throw new BadRequest("El correo electrónico no es válido.");
        }
        /* Comprobación validez número de teléfono */
        String tlf = user.getPhone();
        if(tlf != null && !validatePhoneNumber(tlf)) {
            throw new BadRequest("El número de teléfono no es válido.");
        }
        /* Encriptado de contraseña -- IMPORTANTE: Al hacer log-in, tenemos que comparar la contraseña introducida con la encriptada en la BD. Para ello,
        usamos la función checkpw(psw, pswCryp) de la clase BCrypt, pero antes tenemos que encriptar la contraseña introducida por el usuario.*/
        user.setPassword(encryptPassowrd(user.getPassword()));
        user.setUser_id(autoIncrement());

        return modelMapper.map(userRepo.save(modelMapper.map(user, UserEntity.class)), UserModel.class);
    }

    private long autoIncrement(){
        ModelMapper modelMapper = new ModelMapper();
        List<UserModel> userDatasets = new ArrayList<>();

        userRepo.findAll().forEach(elementB -> userDatasets.add(modelMapper.map(elementB, UserModel.class)));

        return userDatasets.isEmpty() ? 1 :
                userDatasets.stream().max(Comparator.comparing(UserModel::getUser_id)).get().getUser_id() + 1;
    }

    public UserModel editUser(UserModel user) throws NotFound, BadRequest, Forbidden {
        if(new TokenValidator().validate_id_with_token(user.getUser_id())) {
            ModelMapper modelMapper = new ModelMapper();

            if (user == null || user.isDeleted()) {
                throw new NotFound("Error al intentar actualizar el usuario. El usuario no existe.");
            }
            else {
                /* Comprobación validez correo electrónico */
                if(!validateEmail(user.getEmail())) {
                    throw new BadRequest("El correo electrónico no es válido.");
                }
                /* Comprobación validez número de teléfono */
                String tlf = user.getPhone();
                if(tlf != null && !validatePhoneNumber(tlf)) {
                    throw new BadRequest("El número de teléfono no es válido.");
                }
            }
            return modelMapper.map(userRepo.save(modelMapper.map(user, UserEntity.class)), UserModel.class);
        }
        else {
            throw new Forbidden("El userId enviado es diferente al especificado en el token");
        }
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) throws NotFound, BadRequest, Forbidden {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();

            UserModel user = modelMapper.map(userRepo.findById(userId), UserModel.class);

            if (user == null || user.isDeleted()) {
                throw new NotFound("Error al intentar actualizar el usuario. El usuario no existe.");
            }

            if (decryptPassword(currentPassword, user.getPassword())) {
                user.setPassword(newPassword);
                userRepo.save(modelMapper.map(user, UserEntity.class));
                return true;
            } else {
                throw new BadRequest("Current password does not match");
            }
        }
        else {
            throw new Forbidden("El userId enviado es diferente al especificado en el token");
        }
    }
    //endregion

    //region Delete Methods
    public void deleteUser(Long userId) throws Forbidden, NotFound, BadRequest {
        if(new TokenValidator().validate_id_with_token(userId)) {
            ModelMapper modelMapper = new ModelMapper();

            UserModel user = getUser(userId);
            if (user == null || user.isDeleted()) {
                throw new NotFound("Error al intentar eliminar el usuario. EL usuario no existe.");
            }

            user.setDeleted(true);
            saveUser(user);
        }
        else {
            throw new Forbidden("El userId enviado es diferente al especificado en el token");
        }
    }
    //endregion

    //region Private Methods
    /**
     * This method validates the user's email.
     * @param email - Email to be validated.
     * @return boolean - True if the email is valid, false otherwise.
     */
    private boolean validateEmail(String email) {
        String patron = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        Pattern pattern = Pattern.compile(patron);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * This method validates the user's phone number.
     * @param phone - Phone number to be validated.
     * @return boolean - True if the phone number is valid, false otherwise.
     */
    private boolean validatePhoneNumber(String phone) {
        String patron = "^\\+(?:[0-9] ?){6,14}[0-9]$";
        Pattern pattern = Pattern.compile(patron);
        Matcher matcher = pattern.matcher(phone);
        return matcher.matches();
    }

    /**
     * This method validates the user's password.
     * @param password - Password to be validated.
     * @return boolean - True if the password is valid, false otherwise.
     */
    private String encryptPassowrd(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    private boolean decryptPassword(String password, String encPassword) {
        return BCrypt.checkpw(password, encPassword);
    }
    //endregion
}
