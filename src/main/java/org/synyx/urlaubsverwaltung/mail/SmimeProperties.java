package org.synyx.urlaubsverwaltung.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("uv.smime")
public class SmimeProperties {
    
    private boolean enabled;
    
    private String pkcs12StorePath;
    
    private String pkcs12StorePassword;
    
    private String pkcs12StoreKeyAlias;
    
    private String pkcs12StoreKeyPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPkcs12StorePath() {
        return pkcs12StorePath;
    }

    public void setPkcs12StorePath(String pkcs12StorePath) {
        this.pkcs12StorePath = pkcs12StorePath;
    }

    public String getPkcs12StorePassword() {
        return pkcs12StorePassword;
    }

    public void setPkcs12StorePassword(String pkcs12StorePassword) {
        this.pkcs12StorePassword = pkcs12StorePassword;
    }

    public String getPkcs12StoreKeyAlias() {
        return pkcs12StoreKeyAlias;
    }

    public void setPkcs12StoreKeyAlias(String pkcs12StoreKeyAlias) {
        this.pkcs12StoreKeyAlias = pkcs12StoreKeyAlias;
    }

    public String getPkcs12StoreKeyPassword() {
        return pkcs12StoreKeyPassword;
    }

    public void setPkcs12StoreKeyPassword(String pkcs12StoreKeyPassword) {
        this.pkcs12StoreKeyPassword = pkcs12StoreKeyPassword;
    }
    
    
}
