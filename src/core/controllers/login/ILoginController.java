package core.controllers.login;

import core.controllers.utils.Response;

public interface ILoginController {
    Response login(String username, String password);
}


