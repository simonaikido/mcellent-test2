package com.bim.seif.config;

import com.bim.seif.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@Configuration
//@Profile("prod")
public class DataInitializer {


//    @Bean
//    CommandLineRunner initDatabase(CustomerRepository usuarioRepository, PasswordEncoder passwordEncoder) {
//
//        return args -> {
//        };
//    }

//    @Bean
//    public JavaMailSender javaMailSender() throws NamingException {
//
//        InitialContext ctx = new InitialContext();
//        Session session = (Session) ctx.lookup("mail/MailSessionSeif");
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setSession(session);
//        return mailSender;
//    }

//    @Bean
//    public DataSource dataSource() throws NamingException {
//        Context ctx = new InitialContext();
//        return (DataSource) ctx.lookup("jdbc/seifDS");
//    }

}
