/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.view.helper;

import gg.jte.Content;
import gg.jte.TemplateOutput;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * A JTE content component that renders a hidden HTML input field containing a CSRF token.
 * This component is designed to be used in web forms to protect against Cross-Site Request Forgery (CSRF) attacks
 * by including a secure token in form submissions.
 *
 * <p>The generated HTML will be in the format:
 * {@code <input type="hidden" name="_csrf" value="token-value">}
 *
 * <p>This component implements the {@link Content} interface from the JTE template engine,
 * allowing it to be easily integrated into JTE templates.
 *
 * @see org.springframework.security.web.csrf.CsrfToken
 * @see gg.jte.Content
 */
public class CsrfHiddenInput implements Content {

    private final CsrfToken csrfToken;

    public CsrfHiddenInput(CsrfToken csrfToken) {
        this.csrfToken = csrfToken;
    }

    /**
     * Writes a hidden HTML input field containing the CSRF token to the template output.
     * The generated input field will have the CSRF token's parameter name and value.
     * If no CSRF token is available, no output will be written.
     *
     * @param templateOutput the template output to write the hidden input field to
     */
    @Override
    public void writeTo(TemplateOutput templateOutput) {
        if (this.csrfToken != null) {
            templateOutput.writeContent("<input type=\"hidden\" name=\"%s\" value=\"%s\">"
                    .formatted(csrfToken.getParameterName(), csrfToken.getToken()));
        }
    }
}
