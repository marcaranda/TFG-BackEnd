package TFG.Data_Analysis.Security;

import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    /**
     * Validates if the user is an api user
     * @return True if the user is an api user
     * /
    public boolean not_userAPI() throws OurException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if(details instanceof UserDetailsAux user_details_aux) {
            //Si el usuario es un usuario con el rol API no puede acceder a ciertas funcionalidades
            if(user_details_aux.getUser_api()) {
                throw new OurException("El usuario API no puede acceder a esta funcionalidad");
            }
        }
        return true;
    }

    /**
     * Validates if the user is a premium user
     * @return True if the user is a premium user
     * /
    public boolean premium_user() throws OurException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if(details instanceof UserDetailsAux user_details_aux) {
            //Si el usuario es un usuario no es premium no puede acceder a esa funcionalidad
            if(!user_details_aux.getPremium()) {
                throw new OurException("El usuario no es premium");
            }
        }
        return true;
    }*/

    /*
    public boolean can_delete_user() throws OurException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if(details instanceof UserDetailsAux user_details_aux) {
            //Si el usuario es un usuario con el rol API no puede borrar usuarios
            if(user_details_aux.getUser_api()) {
                throw new OurException("El usuario API no puede eliminarse");
            }
        }
        return true;
    }

    public boolean can_make_premium() throws OurException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if(details instanceof UserDetailsAux user_details_aux) {
            //Si el usuario es un usuario con el rol API no puede hacerse premium
            if(user_details_aux.getUser_api()) {
                throw new OurException("El usuario API no puede hacerse premium");
            }
        }
        return true;
    }

    public boolean can_unmake_premium() throws OurException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if(details instanceof UserDetailsAux user_details_aux) {
            //Si el usuario es un usuario con el rol API no puede darse de baja de premium
            if(user_details_aux.getUser_api()) {
                throw new OurException("El usuario API no puede darse de baja de premium");
            }
        }
        return true;
    }


    public boolean can_view_profile() throws OurException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if(details instanceof UserDetailsAux user_details_aux) {
            if(user_details_aux.getUser_api()) {
                throw new OurException("El usuario API no puede visualizar perfiles");
            }
        }
        return true;
    }

    public boolean can_edit_profile() throws OurException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if(details instanceof UserDetailsAux user_details_aux) {
            if(user_details_aux.getUser_api()) {
                throw new OurException("El usuario API no puede editar su perfil");
            }
        }
        return true;
    }
    */
}
