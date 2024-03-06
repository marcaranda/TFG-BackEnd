package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Helpers.Exception;
import TFG.Data_Analysis.Repository.Entity.UserEntity;
import TFG.Data_Analysis.Repository.UserRepo;
import TFG.Data_Analysis.Service.Model.UserModel;
import org.mindrot.jbcrypt.BCrypt;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {
    @Autowired
    UserRepo userRepo;

    /*
    if(new TokenValidator().validate_id_with_token(user_id)) {
     --------
    }
    else {
        throw new OurException("El user_id enviado es diferente al especificado en el token");
    }
     */

    //region Get Methods
    public ArrayList<UserModel> getUsers() throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        ArrayList<UserModel> users = new ArrayList<>();

        userRepo.findAll().forEach(elementB -> users.add(modelMapper.map(elementB, UserModel.class)));
        return users;
    }

    public UserModel getUser(Long userId) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(userRepo.findById(userId), UserModel.class);
    }

    public long getUserIdByEmail(String email) {
        return userRepo.findByEmail(email).getUser_id();
    }
    //endregion

    //region Post/Put Methods
    public UserModel saveUser(UserModel user) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        /* Comprobación validez correo electrónico */
        if(!validateEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El correo electrónico no es válido.");
        }
        /* Comprobación validez número de teléfono */
        String tlf = user.getPhone();
        if(tlf != null && !validatePhoneNumber(tlf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El número de teléfono no es válido.");
        }

        /* Encriptado de contraseña -- IMPORTANTE: Al hacer log-in, tenemos que comparar la contraseña introducida con la encriptada en la BD. Para ello,
        usamos la función checkpw(psw, pswCryp) de la clase BCrypt, pero antes tenemos que encriptar la contraseña introducida por el usuario.*/
        user.setPassword(encryptPassowrd(user.getPassword()));

        return modelMapper.map(userRepo.save(modelMapper.map(user, UserEntity.class)), UserModel.class);
    }
    //endregion

    //region Delete Methods
    public void deleteUser(Long userId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        UserModel user = getUser(userId);
        if (user == null) {
            throw new Exception("Error al intentar eliminar el usuario. EL usuario no existe.");
        }

        user.setDeleted(true);
        saveUser(user);
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
    //endregion
}
