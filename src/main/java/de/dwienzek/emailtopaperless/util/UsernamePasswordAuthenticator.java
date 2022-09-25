package de.dwienzek.emailtopaperless.util;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

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
