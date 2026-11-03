package io.github.aaronfilson.studiomanager.platform.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicAddressesTests {

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", "127.255.255.254", "10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.1.1",
            "169.254.169.254", "100.64.0.1", "0.0.0.0", "224.0.0.1", "255.255.255.255", "198.18.0.1",
            "192.0.2.10", "::1", "::", "fe80::1", "fd00::1", "fc00::1", "ff02::1", "2001:db8::1",
            // IPv6 forms that carry a private IPv4 address
            "::ffff:127.0.0.1", "::ffff:10.0.0.1", "64:ff9b::a9fe:a9fe", "2002:7f00:0001::1", "::127.0.0.1",
            // Odd spellings Java resolves to loopback
            "2130706433", "127.1" })
    void refusesWhatIsntPublic(String address) throws UnknownHostException {
        assertThat(PublicAddresses.whyNot(InetAddress.getByName(address))).as(address).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = { "8.8.8.8", "1.1.1.1", "52.0.0.1", "172.32.0.1", "100.128.0.1", "2606:4700:4700::1111",
            "::ffff:8.8.8.8", "64:ff9b::808:808" })
    void allowsPublicAddresses(String address) throws UnknownHostException {
        assertThat(PublicAddresses.whyNot(InetAddress.getByName(address))).as(address).isEmpty();
    }

    @org.junit.jupiter.api.Test
    void saysWhy() throws UnknownHostException {
        assertThat(PublicAddresses.whyNot(InetAddress.getByName("169.254.169.254")))
                .hasValueSatisfying(why -> assertThat(why).contains("metadata"));
    }

}
