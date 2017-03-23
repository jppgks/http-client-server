package http;

public enum Method {
    GET("GET"), PUT("PUT"), POST("POST"), HEAD("HEAD");

    private String name;

    Method(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}