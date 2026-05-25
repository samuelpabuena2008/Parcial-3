package core.controllers.login;

import core.controllers.utils.Response;
import core.controllers.utils.Status;

public interface ILoginController {
    Response login(String username, String password);
}


