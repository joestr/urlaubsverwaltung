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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.synyx.urlaubsverwaltung.application.application.ApplicationCancelledEvent;
import org.synyx.urlaubsverwaltung.application.application.ApplicationService;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysCountService;

/**
 * 
 * @author j.strasser
 */
@Component
public class EventHandlerApplicationCancelledEvent {
    private static final System.Logger LOG = System.getLogger(EventHandlerApplicationCancelledEvent.class.getName());
    
    private boolean uvNotifierEnabled = false;
    private String uvNotifierEmailFrom = "";
    private String uvNotifierEmailTo = "";
    private String uvNotifierBaseUrl = "";
    
    private final JavaMailSender javaMailSender;
    private final WorkDaysCountService workDaysCountService;
    
    @Autowired
    public EventHandlerApplicationCancelledEvent(JavaMailSender javaMailSender, PersonService personService, ApplicationService applicationService, WorkDaysCountService workDaysCountService) {
        this.javaMailSender = javaMailSender;
        this.workDaysCountService = workDaysCountService;
        this.getFromEnvironment();
    }
    
    @EventListener
    public void onApplicationEvent(ApplicationCancelledEvent event) {
        
        if (uvNotifierEnabled == false) {
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
        var webUrl = this.uvNotifierBaseUrl + "/web/application/" + application.getId();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        sb.append("<tr><td colspan=\"2\">Eine genehmigte Abwesenheit wurde storniert:</td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td colspan=\"2\">---------------[ S T O R N O ]---------------</td></tr>");
        sb.append("<tr><td>Mitarbeiter:</td><td><b>").append(applier).append("</b></td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td>Von:</td><td><b>").append(startTimestampString).append("</b></td></tr>");
        sb.append("<tr><td>Bis:</td><td><b>").append(endTimestampString).append("</b></td></tr>");
        sb.append("<tr><td>Tage:</td><td>").append(days).append("</td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td>Antragsart:</td><td><b>").append(application.getVacationType().getLabel(Locale.GERMAN)).append("</b></td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td>Grund:</td><td>").append(application.getReason()).append("</td></tr>");
        sb.append("<tr><td colspan=\"2\">---------------[ S T O R N O ]---------------</td></tr>");
        sb.append("<tr><td colspan=\"2\"><span>&nbsp;</span></td></tr>");
        sb.append("<tr><td colspan=\"2\"><a href=\"").append(webUrl).append("\">").append(webUrl).append("</a></td></tr>");
        sb.append("</table>");
        
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            message.setSubject("Eine genehmigte Abwesenheit wurde storniert");
            MimeMessageHelper helper;
            helper = new MimeMessageHelper(message, true);
            helper.setFrom(this.uvNotifierEmailFrom);
            helper.setTo(this.uvNotifierEmailTo);
            helper.setText(sb.toString(), true);
            javaMailSender.send(message);
        } catch (MessagingException ex) {
            LOG.log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    private void getFromEnvironment() {
        var uvNotifierEnabledL = System.getenv(UvnotifierExtension.ENV_UVNOTIFIER_ENABLED);
        if (uvNotifierEnabledL != null) {
            this.uvNotifierEnabled = Boolean.parseBoolean(uvNotifierEnabledL);
            this.uvNotifierEmailFrom = System.getenv(UvnotifierExtension.ENV_UVNOTIFIER_EMAILFROM);
            this.uvNotifierEmailTo = System.getenv(UvnotifierExtension.ENV_UVNOTIFIER_EMAILTO);
            this.uvNotifierBaseUrl = System.getenv(UvnotifierExtension.ENV_UVNOTIFIER_BASEURL);
        } else {
            LOG.log(System.Logger.Level.INFO, "Disabled hook for cancelled applications.");
        }
    }
}
