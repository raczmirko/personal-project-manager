package hu.okrim.personalprojectmanager;

public class ConnectionController {
    public static String getConnectionURLSS(String server, String db, String user, String password){
        String conn = "jdbc:sqlserver://" + server + ";"
                        + "database=" + db + ";"
                        + "user=" + user + ";"
                        + "password=" +password + ";"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=30;";
        return conn;
    }



}
