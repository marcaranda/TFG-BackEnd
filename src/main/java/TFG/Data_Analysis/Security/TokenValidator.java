package TFG.Data_Analysis.Security;

import org.springframework.security.core.context.SecurityContextHolder;

public class TokenValidator {
    /**
     * Validates the user id with the token
     * @param user_id The user id to validate
     * @return True if the user id is the same as the one in the token
     */
    public boolean validate_id_with_token(long user_id) {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        return details instanceof UserDetailsAux user_details_aux && user_details_aux.getUserId() == user_id;
    }
}
