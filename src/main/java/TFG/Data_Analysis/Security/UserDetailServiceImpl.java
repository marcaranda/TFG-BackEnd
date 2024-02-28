package TFG.Data_Analysis.Security;

import TFG.Data_Analysis.Repository.UserRepo;
import TFG.Data_Analysis.Service.Model.UserModel;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImpl implements UserDetailsService {
    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        ModelMapper model_mapper = new ModelMapper();

        UserModel user = model_mapper.map(userRepo.findByEmail(email), UserModel.class);
        if(user == null) throw new UsernameNotFoundException("The user " + email + " doesn't exist");
        return new UserDetailsImpl(user);
    }
}
