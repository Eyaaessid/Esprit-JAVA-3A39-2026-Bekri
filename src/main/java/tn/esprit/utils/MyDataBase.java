package tn.esprit.utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

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

    public synchronized Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed() || !cnx.isValid(2)) {
                Properties props = new Properties();
                try (InputStream is = MyDataBase.class.getResourceAsStream("/config.properties")) {
                    if (is != null) props.load(is);
                }
                String url = firstNonBlank(props.getProperty("db.url"), URL);
                String username = firstNonBlank(props.getProperty("db.username"), USERNAME);
                String password = firstNonBlank(props.getProperty("db.password"), PASSWORD);
                Class.forName("com.mysql.cj.jdbc.Driver");
                cnx = DriverManager.getConnection(url, username, password);
                System.out.println("Connexion JDBC OK (reconnected)");
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible de se connecter à la base de données: " + e.getMessage(), e);
        }
        return cnx;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred == null || preferred.isBlank()) {
            return fallback;
        }
        return preferred.trim();
    }
}
