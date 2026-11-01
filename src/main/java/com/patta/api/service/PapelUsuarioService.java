package com.patta.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PapelUsuarioService {

    private final Set<String> uidsAdministrativos;
    private final Set<String> uidsProfissionais;

    @Autowired
    public PapelUsuarioService(@Value("${app.admin.uids:}") String uidsAdministrativos,
                               @Value("${app.professional.uids:}") String uidsProfissionais) {
        this.uidsAdministrativos = parsearUids(uidsAdministrativos);
        this.uidsProfissionais = parsearUids(uidsProfissionais);
    }

    public PapelUsuarioService(@Value("${app.admin.uids:}") String uidsAdministrativos) {
        this(uidsAdministrativos, "");
    }

    private Set<String> parsearUids(String uids) {
        return Arrays.stream(uids.split(","))
                .map(String::trim)
                .filter(uid -> !uid.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean ehAdministrador(String uid) {
        return uid != null && uidsAdministrativos.contains(uid);
    }

    public boolean ehProfissional(String uid) {
        return uid != null && uidsProfissionais.contains(uid);
    }

    public boolean podeGerenciarAtendimento(String uid) {
        return ehAdministrador(uid) || ehProfissional(uid);
    }

    public String buscarPapel(String uid) {
        if (ehAdministrador(uid)) return "ADMIN";
        return ehProfissional(uid) ? "PROFISSIONAL" : "USER";
    }

    public List<SimpleGrantedAuthority> buscarAutoridades(String uid) {
        return switch (buscarPapel(uid)) {
            case "ADMIN" -> List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_PROFISSIONAL"),
                    new SimpleGrantedAuthority("ROLE_USER"));
            case "PROFISSIONAL" -> List.of(
                    new SimpleGrantedAuthority("ROLE_PROFISSIONAL"),
                    new SimpleGrantedAuthority("ROLE_USER"));
            default -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
        };
    }
}
