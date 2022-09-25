package de.dwienzek.emailtopaperless.util;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;

@Data
@EqualsAndHashCode(callSuper = true)
public class UsernamePasswordAuthenticator extends Authenticator {

    private final String username;
    private final String password;

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
    }

}
