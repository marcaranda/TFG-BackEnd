package TFG.Data_Analysis.Controller;

import TFG.Data_Analysis.Controller.Dto.UserDto;
import TFG.Data_Analysis.Helpers.Error.BadRequest;
import TFG.Data_Analysis.Helpers.Error.Forbidden;
import TFG.Data_Analysis.Helpers.Error.NotFound;
import TFG.Data_Analysis.Service.Model.UserModel;
import TFG.Data_Analysis.Service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    UserService userService;

    //region Get Methods
    @GetMapping
    public ArrayList<UserDto> getUsers() {
        ModelMapper modelMapper = new ModelMapper();
        ArrayList<UserDto> users = (ArrayList<UserDto>) userService.getUsers().stream()
                .map(elementB -> modelMapper.map(elementB, UserDto.class))
                .collect(Collectors.toList());
        return users;
    }

    @GetMapping(path = "/userId/{userId}")
    public UserDto getUserId(@PathVariable("userId") Long userId) throws Forbidden, NotFound {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(userService.getUser(userId), UserDto.class);
    }
    //endregion

    //region Post Methods
    @PostMapping(path = "/register")
    public UserDto saveUser(@RequestBody UserDto user) throws BadRequest {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(userService.saveUser(modelMapper.map(user, UserModel.class)), UserDto.class);
    }
    //endregion

    //region Put Methods
    @PutMapping
    public UserDto editUser(@RequestBody UserDto user) throws BadRequest, Forbidden, NotFound {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(userService.editUser(modelMapper.map(user, UserModel.class)), UserDto.class);
    }

    @PutMapping(path = "/userId/{userId}/changePassword")
    public boolean changePassword(@PathVariable("userId") Long userId, @RequestAttribute("cp") String currentPassword,
                                  @RequestAttribute("np") String newPassword) throws BadRequest, Forbidden, NotFound {
        return userService.changePassword(userId, currentPassword, newPassword);
    }
    //endregion

    //region Delete Methods
    @DeleteMapping(path = "/userId/{userId}")
    public void deleteUser(@PathVariable("userId") Long userId) throws BadRequest, Forbidden, NotFound {
        userService.deleteUser(userId);
    }
    //endregion
}
