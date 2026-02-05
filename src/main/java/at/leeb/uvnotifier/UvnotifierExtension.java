/*
 *
 * Copyright (c) 2026 Joel Strasser <strasser999@gmail.com>
 *
 * Licensed under the EUPL-1.2 license.
 *
 * For the full license text consult the 'LICENSE.txt' file from this repository.
 *
 */
package at.leeb.uvnotifier;

import org.springframework.stereotype.Component;

/**
 *
 * @author j.strasser
 */
@Component
public class UvnotifierExtension {
    public static String ENV_UVNOTIFIER_ENABLED = "UVNOTIFIER_ENABLED";
    public static String ENV_UVNOTIFIER_EMAILFROM = "UVNOTIFIER_EMAILFROM";
    public static String ENV_UVNOTIFIER_EMAILTO = "UVNOTIFIER_EMAILTO";
    public static String ENV_UVNOTIFIER_BASEURL = "UVNOTIFIER_BASEURL";
}
