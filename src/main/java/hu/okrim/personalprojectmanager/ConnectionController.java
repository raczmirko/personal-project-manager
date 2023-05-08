package hu.okrim.personalprojectmanager;

import java.sql.*;

public class ConnectionController {
    public static String getConnectionURLSS(String server, String db, String user, String password){
        String conn = "jdbc:sqlserver://" + server + ";"
                        + "database=" + db + ";"
                        + "user=" + user + ";"
                        + "password=" + password + ";"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=30;";
        return conn;
    }

    public static Connection establishConnection(String URL) throws SQLException{
        return DriverManager.getConnection(URL);
    }


}
