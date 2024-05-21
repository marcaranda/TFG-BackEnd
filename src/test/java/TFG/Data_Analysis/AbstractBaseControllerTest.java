package TFG.Data_Analysis;

import TFG.Data_Analysis.Repository.DatasetRepo;
import TFG.Data_Analysis.Repository.UserRepo;
import TFG.Data_Analysis.Service.DatasetService;
import TFG.Data_Analysis.Service.EntropyService;
import TFG.Data_Analysis.Service.UserService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;

public class AbstractBaseControllerTest {
    @MockBean
    protected UserService userService;
    @MockBean
    protected DatasetService datasetService;
    @MockBean
    protected EntropyService entropyService;
    @MockBean
    protected UserRepo userRepo;
    @MockBean
    protected DatasetRepo datasetRepo;
    @MockBean
    protected AuthenticationManager authenticationManager;
}
