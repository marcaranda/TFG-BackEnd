package TFG.Data_Analysis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import TFG.Data_Analysis.Controller.UserController;
import TFG.Data_Analysis.Helpers.Error.BadRequest;
import TFG.Data_Analysis.Helpers.Error.NotFound;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import TFG.Data_Analysis.Controller.Dto.UserDto;
import TFG.Data_Analysis.Service.Model.UserModel;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserController.class)
public class UserControllerTest extends AbstractBaseControllerTest {
    @Autowired
    MockMvc mockMvc;

    //region Register User
    @Test
    public void shouldRegisterUser() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setName("test");
        userDto.setEmail("test@example.com");

        ObjectMapper objectMapper = new ObjectMapper();
        String userJson = objectMapper.writeValueAsString(userDto);

        UserModel expectedUserModel = new UserModel();
        expectedUserModel.setUser_id(1L);
        expectedUserModel.setName("test");
        expectedUserModel.setEmail("test@example.com");

        when(userService.saveUser(any(UserModel.class))).thenReturn(expectedUserModel);

        mockMvc.perform(post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_id").value(1))
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).saveUser(argThat(userModel -> userModel.getName().equals("test") &&
                userModel.getEmail().equals("test@example.com")));
    }

    @Test
    public void shouldReturnBadEmail() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setName("test");
        userDto.setEmail("test");

        ObjectMapper objectMapper = new ObjectMapper();
        String userJson = objectMapper.writeValueAsString(userDto);

        when(userService.saveUser(any(UserModel.class))).thenThrow(new BadRequest("El correo electrónico no es válido."));

        mockMvc.perform(post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isBadRequest());

        verify(userService).saveUser(argThat(userModel -> userModel.getName().equals("test") &&
                userModel.getEmail().equals("test")));
    }

    @Test
    public void shouldReturnBadPhoneNumber() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setName("test");
        userDto.setEmail("test");

        ObjectMapper objectMapper = new ObjectMapper();
        String userJson = objectMapper.writeValueAsString(userDto);

        when(userService.saveUser(any(UserModel.class))).thenThrow(new BadRequest("El número de teléfono no es válido."));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest());

        verify(userService).saveUser(argThat(userModel -> userModel.getName().equals("test") &&
                userModel.getEmail().equals("test")));
    }
    //endregion

    //region GetUser
    @Test
    public void shoulReturnUserProfile() throws Exception {
        UserModel expectedUserModel = new UserModel();
        expectedUserModel.setUser_id(1L);
        expectedUserModel.setName("test");
        expectedUserModel.setEmail("test@example.com");

        when(userService.getUser(1L)).thenReturn(expectedUserModel);

        mockMvc.perform(get("/user/userId/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_id").value(1))
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void shouldReturnUserNotFound() throws Exception {
        when(userService.getUser(1L)).thenThrow(new NotFound("User not found"));

        mockMvc.perform(get("/usert/userId/1"))
                .andExpect(status().isNotFound());
    }
    //endregion
}
