package com.patta.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PapelUsuarioService {

    private final Set<String> uidsAdministrativos;

    public PapelUsuarioService(@Value("${app.admin.uids:}") String uidsAdministrativos) {
        this.uidsAdministrativos = Arrays.stream(uidsAdministrativos.split(","))
                .map(String::trim)
                .filter(uid -> !uid.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean ehAdministrador(String uid) {
        return uidsAdministrativos.contains(uid);
    }

    public String buscarPapel(String uid) {
        return ehAdministrador(uid) ? "ADMIN" : "USER";
    }

    public List<SimpleGrantedAuthority> buscarAutoridades(String uid) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + buscarPapel(uid)));
    }
}
