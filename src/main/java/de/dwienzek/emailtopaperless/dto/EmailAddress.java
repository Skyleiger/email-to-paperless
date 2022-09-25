package de.dwienzek.emailtopaperless.dto;

import lombok.Data;

import javax.mail.internet.InternetAddress;

@Data
public class EmailAddress {

    private final String address;
    private final String addressName;

    public EmailAddress(InternetAddress address) {
        this.address = address.getAddress();
        addressName = address.getPersonal();
    }

}
