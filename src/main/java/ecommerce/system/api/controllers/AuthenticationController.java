package ecommerce.system.api.controllers;

import ecommerce.system.api.enums.MessagesEnum;
import ecommerce.system.api.models.BaseResponseModel;
import ecommerce.system.api.models.CredentialsModel;
import ecommerce.system.api.models.TokenModel;
import ecommerce.system.api.services.IAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final IAuthenticationService authenticationService;

    @Autowired
    public AuthenticationController(IAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody CredentialsModel credentials) {

        BaseResponseModel<?> response;

        try {
            TokenModel token = this.authenticationService.authenticateUser(credentials);

            if (token == null) {
                response = new BaseResponseModel<>(false, "Autenticação falhou.", "E-mail ou senha incorretos.");

            } else {
                response = new BaseResponseModel<>(true, "Autenticação realizada com sucesso!", token);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {

            logger.error(e.getMessage());

            response = new BaseResponseModel<>(false, MessagesEnum.FAILURE.getMessage(), e.getMessage());

            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
