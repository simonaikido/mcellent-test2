package com.bim.seif.services;

import com.bim.seif.dto.EventoDto;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EventoService eventoService;

    @Value("${mail.from}")
    private String remitente;

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitente);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    public void enviarCorreo(String destinatario, TipoEvento te, Map<Propiedad, String> data) {
        EventoDto evento = eventoService.obtenerEvento(te);
        String cuerpoCorreo = evento.getCuerpoCorreo();

        // Reemplazar las variables {{propiedad}} por su valor correspondiente
        for (Map.Entry<Propiedad, String> entry : data.entrySet()) {
            String propiedad = entry.getKey().toString(); // ya devuelve {{otp}} por tu override
            String valor = entry.getValue() != null ? entry.getValue() : "";
            cuerpoCorreo = cuerpoCorreo.replace(propiedad, valor);
        }

        sendMimeEmail(destinatario, evento.getAsunto(), cuerpoCorreo);
    }

    private void sendMimeEmail(String to, String subject, String body) {
        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(to);
            helper.setCc(new String[] { "j.mendez@bim.mx", "fpedraza@mcllent.com", "aalcalar@mcllent.com", "e.andrade@bim.mx" });
            helper.setSubject(subject);
            helper.setFrom(remitente);
            helper.setText(body, true);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando correo", e);
        }
    }

}