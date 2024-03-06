package TFG.Data_Analysis.Security;

import TFG.Data_Analysis.Repository.UserRepo;
import TFG.Data_Analysis.Service.DatasetService;
import TFG.Data_Analysis.Service.Model.UserModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

@Component
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private DatasetService datasetService;
    private UserRepo userRepo;

    public JWTAuthenticationFilter(UserRepo userRepo, DatasetService datasetService, AuthenticationManager authManager) {
        this.userRepo = userRepo;
        this.datasetService = datasetService;
        setAuthenticationManager(authManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        AuthCredentials credentials = new AuthCredentials();
        try {
            credentials = new ObjectMapper().readValue(request.getReader(), AuthCredentials.class);
        } catch (Exception e) {
        }

        //Obtener el usuario de la base de datos a través del correo electrónico ingresado en el formulario de inicio de sesión
        ModelMapper model_mapper = new ModelMapper();
        UserModel us = model_mapper.map(userRepo.findByEmail(credentials.getEmail()), UserModel.class);

        //Verificar si el usuario existe y no está eliminado
        if (us != null && !us.isDeleted()) {
            //Si el usuario no está eliminado, proceder a autenticarlo
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    credentials.getEmail(),
                    credentials.getPassword(),
                    Collections.emptyList()
            );
            datasetService.chargeUserDatasets(credentials.getEmail());
            return getAuthenticationManager().authenticate(authenticationToken);
        }
        else {
            String errorMessage = "El usuario no existe";
            Exception exception = new Exception(errorMessage);

            throw new BadCredentialsException(errorMessage, exception);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authResult.getPrincipal();
        String token = TokenUtils.generateAccessToken(userDetails.getId(), userDetails.getUsername());

        PrintWriter w = response.getWriter();
        w.println(token);
        w.flush();

        super.successfulAuthentication(request, response, chain, authResult);
    }
}
