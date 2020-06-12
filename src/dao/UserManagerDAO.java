package dao;

import java.util.Random;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Date;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;

public class UserManagerDAO {

    public UserManagerDAO() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        checkUserTable();
    }

    protected static Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/BLOGDB", "sql_admin", "153226");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean validPassWd(String passWd) {
        if (null == passWd) {
            return false;
        } else if (passWd.length() < 8) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean validUserName(String userName) {
        if (null == userName) {
            return false;
        } else if (userName.length() < 6 || userName.length() > 20) {
            return false;
        } else {
            return true;
        }
    }

    public static String generateSalt() {
        Random ranGen = new SecureRandom();
        byte[] aesKey = new byte[20];
        ranGen.nextBytes(aesKey);

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < aesKey.length; i++) {
            String hex = Integer.toHexString(0xff & aesKey[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static String toMD5(String plainText) {
        String result = "";
        try {
            //生成实现指定摘要算法的 MessageDigest 对象。
            MessageDigest md = MessageDigest.getInstance("MD5");
            //使用指定的字节数组更新摘要。
            md.update(plainText.getBytes());
            //通过执行诸如填充之类的最终操作完成哈希计算。
            byte b[] = md.digest();
            //生成具体的md5密码到buf数组
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
            //System.out.println("16位: " + buf.toString().substring(8, 24));// 16位的加密，其实就是32位加密后的截取
        } catch (Exception e) {
            e.printStackTrace();
        }
 
        return result;
    }

    public static int getUserId(String userName)
    {
        if (null == userName) {
            return 0;
        }

        String sql = "search user_id from USER_TABLE where user_name=?";
        try (Connection conn = getConnection();
            PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.setString(1, userName);
            ResultSet rs = stat.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    protected void checkUserTable() {

        String sql = "CREATE TABLE IF NOT EXISTS USER_TABLE (user_id INT UNSIGNED AUTO_INCREMENT, user_name VARCHAR(100) NOT NULL, user_passwd VARCHAR(100) NOT NULL, create_time DATE, banned tinyint(1), passwd_salt VARCHAR(100) NOT NULL, PRIMARY KEY (user_id ))ENGINE=InnoDB DEFAULT CHARSET=utf8;";
        try (Connection conn = getConnection();
            PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int addUser(String userName, String UserPassWd) {

        if (null == userName) {
            return -1;
        } else if (true != validUserName(userName)) {
            return -2;
        } else if (true != validPassWd(UserPassWd)) {
            return -3;
        } else {
            // log
        }
            
        Date currentDate = new java.sql.Date(System.currentTimeMillis());

        String salt = generateSalt();
        if (null == salt) {
            return -4;
        }

        String PassWdStor = toMD5(toMD5(UserPassWd) + salt);

        String sql = "insert into USER_TABLE values(null, ?, ?, ?, ?, ?);";
        String dupsql = "select user_name from USER_TABLE where user_name = ?;";

        try (Connection conn = getConnection();
                PreparedStatement stat = conn.prepareStatement(sql);
                // add these two parameter to move pointer freely
                PreparedStatement dupstat = conn.prepareStatement(dupsql, ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);) {

            dupstat.setString(1, userName);
            ResultSet rs = dupstat.executeQuery();
            rs.last();
            int row = rs.getRow();
            rs.beforeFirst();
            if (0 != row) {
                // have dup
                return -5;
            }
    
            stat.setString(1, userName);
            stat.setString(2, PassWdStor);
            stat.setString(3, currentDate.toString());
            stat.setInt(4, 0);
            stat.setString(5, salt);

            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return -6;
        }

        return 0;
    }

    public int authUser(String userName, String userPassWd) {
        int status = -3;

        if (userName.equals("admin")) {
            status = -3;
        } else if (true != validUserName(userName)) {
            return -1;
        } else if (true != validPassWd(userPassWd)) {
            return -2;
        } else {
            status = -3;
        }

        String sql = "select user_passwd, banned, passwd_salt from USER_TABLE where user_name = ? limit 1;";
        try (Connection conn = getConnection();
                PreparedStatement stat = conn.prepareStatement(sql)) {
            stat.setString(1, userName);

            ResultSet rs = stat.executeQuery();

            rs.next();
            String passWdStor = rs.getString("user_passwd");
            int banned = rs.getInt("banned");
            String salt = rs.getString("passwd_salt");


            String authPassWd = toMD5(toMD5(userPassWd) + salt);
            if (true == authPassWd.equals(passWdStor)) {
                status = 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -4;
        }

        return status;
    }

    public String getPasswd(String userName) {
        if (null == userName) {
            return null;
        }

        String sql = "select user_passwd from USER_TABLE where user_name = ? limit 1;";
        try (Connection conn = getConnection();
                PreparedStatement stat = conn.prepareStatement(sql)) {
            stat.setString(1, userName);

            ResultSet rs = stat.executeQuery();

            rs.next();
            String passWdStor = rs.getString("user_passwd");
            if (null != passWdStor) {
                return passWdStor;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

};