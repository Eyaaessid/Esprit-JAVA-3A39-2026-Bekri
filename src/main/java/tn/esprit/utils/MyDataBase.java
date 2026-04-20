package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDataBase {
    private static MyDataBase instance;
    private Connection cnx;

    private static final String URL =
        "jdbc:mysql://127.0.0.1:3306/bekri_db" +
        "?useSSL=false&serverTimezone=UTC" +
        "&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private MyDataBase() {
        try {
            this.cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion JDBC OK");
            try (Statement st = cnx.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS profil_psychologique ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                                + "utilisateur_id INT NOT NULL,"
                                + "score_global INT NOT NULL,"
                                + "profil_type VARCHAR(100) NOT NULL,"
                                + "date_evaluation DATETIME DEFAULT CURRENT_TIMESTAMP,"
                                + "ai_feedback TEXT,"
                                + "FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE"
                                + ")");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Connexion JDBC échouée : " + e.getMessage(), e);
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) instance = new MyDataBase();
        return instance;
    }

    public Connection getCnx() { return cnx; }
}
