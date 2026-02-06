package org.synyx.urlaubsverwaltung.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import java.security.Security;
import java.util.UUID;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.simplejavamail.utils.mail.smime.SmimeKey;
import org.simplejavamail.utils.mail.smime.SmimeKeyStore;
import org.simplejavamail.utils.mail.smime.SmimeUtil;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Service
@EnableConfigurationProperties(SmimeProperties.class)
class MailSenderService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final JavaMailSender mailSender;
    private final SmimeProperties smimeProperties;

    @Autowired
    MailSenderService(JavaMailSender mailSender, SmimeProperties smimeProperties) {
        this.mailSender = mailSender;
        this.smimeProperties = smimeProperties;
        
        if (this.smimeProperties.isEnabled()) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Send a mail with the given subject and text to the given recipients.
     *
     * @param from      mail address from where the mail is sent
     * @param recipient mail address where the mail should be sent to
     * @param subject   mail subject
     * @param text      mail body
     */
    void sendEmail(String from, String replyTo, @Nullable String recipient, String subject, String text) {
        this.sendEmail(from, replyTo, recipient, subject, text, List.of());
    }

    /**
     * Send a mail with the given subject and text to the given recipients.
     *
     * @param from            mail address from where the mail is sent
     * @param recipient       mail address where the mail should be sent to
     * @param subject         mail subject
     * @param text            mail body
     * @param mailAttachments List of attachments to add to the mail
     */
    void sendEmail(String from, String replyTo, @Nullable String recipient, String subject, String text, List<MailAttachment> mailAttachments) {

        if (recipient == null || recipient.isBlank()) {
            LOG.warn("Could not send email to empty recipients!");
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(from);
            helper.setReplyTo(replyTo);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(text);

            for (MailAttachment mailAttachment : mailAttachments) {
                helper.addAttachment(mailAttachment.getName(), mailAttachment.getContent());
            }
            
            if (smimeProperties.isEnabled()) {
                mimeMessage = signMessage(((JavaMailSenderImpl)mailSender).getSession(), mimeMessage);
            }
            
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            LOG.error("Sending email to {} failed", recipient, e);
        }
    }
    
    private MimeMessage signMessage(Session session, MimeMessage message) throws Exception {
	SmimeKey smimeKey = getSmimeKey();
	return SmimeUtil.sign(session, UUID.randomUUID().toString(), message, smimeKey);
    }
    
    private SmimeKey getSmimeKey() throws FileNotFoundException {
        var pkey = new FileInputStream(this.smimeProperties.getPkcs12StorePath());
        return
            new SmimeKeyStore(pkey, this.smimeProperties.getPkcs12StorePassword().toCharArray())
            .getPrivateKey(this.smimeProperties.getPkcs12StoreKeyAlias(), this.smimeProperties.getPkcs12StoreKeyPassword().toCharArray());
    }
}
