package core.controllers.utils;

public class Response {
    private String message;
    private int status;
    private Object data;

    public Response(String message, int status) {
        this.message = message;
        this.status = status;
    }
    
    public Response(String message, int status, Object data) {
        this.message = message;
        this.status = status;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }
}
