package com.bekri.services;

import com.bekri.entities.Utilisateur;
import com.bekri.utils.MyDataBase;
import com.bekri.utils.UtilisateurResultSetMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    @Override
    public UserDetails loadUserByUsername(String username) {
        try {
            String sql = "SELECT * FROM utilisateur WHERE email=?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new UsernameNotFoundException("Utilisateur introuvable : " + username);
            }
            Utilisateur u = UtilisateurResultSetMapper.mapRow(rs);
            rs.close();
            ps.close();
            return u;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Utilisateur loadUserById(Integer id) {
        try {
            String sql = "SELECT * FROM utilisateur WHERE id=?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new UsernameNotFoundException("Utilisateur introuvable : " + id);
            }
            Utilisateur u = UtilisateurResultSetMapper.mapRow(rs);
            rs.close();
            ps.close();
            return u;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
