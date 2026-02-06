/*
 *
 * Copyright (c) 2026 Joel Strasser <strasser999@gmail.com>
 *
 * Licensed under the EUPL-1.2 license.
 *
 * For the full license text consult the 'LICENSE.txt' file from this repository.
 *
 */
package at.leeb.uvnotifier.eventhandlers;

import at.leeb.uvnotifier.UvnotifierExtension;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.simplejavamail.utils.mail.smime.SmimeKey;
import org.simplejavamail.utils.mail.smime.SmimeKeyStore;
import org.simplejavamail.utils.mail.smime.SmimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.synyx.urlaubsverwaltung.application.application.ApplicationAllowedEvent;
import org.synyx.urlaubsverwaltung.mail.MailProperties;
import org.synyx.urlaubsverwaltung.mail.SmimeProperties;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysCountService;

/**
 * 
 * @author j.strasser
 */
@Component
public class EventHandlerApplicationAllowedEvent {
    private static final System.Logger LOG = System.getLogger(EventHandlerApplicationAllowedEvent.class.getName());
    
    private boolean uvNotifierEnabled = false;
    private String uvNotifierEmailTo = "";
    
    private final JavaMailSender javaMailSender;
    private final WorkDaysCountService workDaysCountService;
    private final SmimeProperties smimeProperties;
    private final MailProperties mailProperties;
    
    @Autowired
    public EventHandlerApplicationAllowedEvent(JavaMailSender javaMailSender, WorkDaysCountService workDaysCountService, SmimeProperties smimeProperties, MailProperties mailProperties) {
        this.javaMailSender = javaMailSender;
        this.workDaysCountService = workDaysCountService;
        this.smimeProperties = smimeProperties;
        this.mailProperties = mailProperties;
        this.getFromEnvironment();
    }
    
    @EventListener
    public void onApplicationEvent(ApplicationAllowedEvent event) {
        
        if (this.uvNotifierEnabled) {
            return;
        }
        
        var application = event.application();
        var applier = application.getApplier().getNiceName();
        var days = workDaysCountService.getWorkDaysCount(application.getDayLength(), application.getStartDate(), application.getEndDate(), application.getApplier()).toBigInteger();
        
        var startTimestampString = application.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (application.getStartTime() != null) {
            startTimestampString = startTimestampString + " " + application.getStartTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
        }
        var endTimestampString = application.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (application.getEndTime() != null) {
            endTimestampString = endTimestampString + " " + application.getEndTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
        }
        var webUrl = this.mailProperties.getApplicationUrl() + "/web/application/" + application.getId();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        sb.append("<tr><td colspan=\"2\">Eine neue Abwesenheit wurde genehmigt:</td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td colspan=\"2\">--------------------------------------------------</td></tr>");
        sb.append("<tr><td>Mitarbeiter:</td><td><b>").append(applier).append("</b></td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td>Von:</td><td><b>").append(startTimestampString).append("</b></td></tr>");
        sb.append("<tr><td>Bis:</td><td><b>").append(endTimestampString).append("</b></td></tr>");
        sb.append("<tr><td>Tage:</td><td>").append(days).append("</td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td>Antragsart:</td><td><b>").append(application.getVacationType().getLabel(Locale.GERMAN)).append("</b></td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td>Grund:</td><td>").append(application.getReason()).append("</td></tr>");
        sb.append("<tr><td colspan=\"2\">--------------------------------------------------</td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td colspan=\"2\"><a href=\"").append(webUrl).append("\">").append(webUrl).append("</a></td></tr>");
        sb.append("</table>");
        
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            message.setSubject("Eine neue Abwesenheit wurde genehmigt");
            MimeMessageHelper helper;
            helper = new MimeMessageHelper(message, true);
            helper.setFrom(this.mailProperties.getFrom());
            helper.setTo(this.uvNotifierEmailTo);
            helper.setText(sb.toString(), true);
            
            if (this.smimeProperties.isEnabled()) {
                message = signMessage(((JavaMailSenderImpl)javaMailSender).getSession(), message);
            }
            
            javaMailSender.send(message);
        } catch (Exception ex) {
            LOG.log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    private void getFromEnvironment() {
        var uvNotifierEnabledL = System.getenv(UvnotifierExtension.ENV_UVNOTIFIER_ENABLED);
        if (uvNotifierEnabledL != null) {
            this.uvNotifierEnabled = Boolean.parseBoolean(uvNotifierEnabledL);
            this.uvNotifierEmailTo = System.getenv(UvnotifierExtension.ENV_UVNOTIFIER_EMAILTO);
            LOG.log(System.Logger.Level.INFO, "Enabled hook for allowed applications.");
        } else {
            LOG.log(System.Logger.Level.INFO, "Disabled hook for allowed applications.");
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
