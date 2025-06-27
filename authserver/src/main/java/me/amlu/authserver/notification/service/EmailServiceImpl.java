/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.notification.service;

import jakarta.mail.internet.MimeMessage;

import me.amlu.authserver.enums.ResponseMessage;
import me.amlu.authserver.exceptions.EmailSendingFailedException;
import me.amlu.authserver.notification.dto.CreateEmailRequest;
import me.amlu.authserver.notification.dto.GetEmailResponse;
import me.amlu.authserver.security.service.EmailService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EmailServiceImpl.class);
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public Object sendMail(CreateEmailRequest createEmailRequest) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(createEmailRequest.toList());
            mimeMessageHelper.setSubject(createEmailRequest.subject());
            mimeMessageHelper.setText(createEmailRequest.body());

            javaMailSender.send(mimeMessage);

            log.info("Message Sent Successfully to: {}", createEmailRequest.toList());
            return new GetEmailResponse("Email Sent Successfully", createEmailRequest.toList(), ResponseMessage.SUCCESS);
        } catch (Exception e) {
            log.error("sendEmail() | Error : {}", e.getMessage());
            throw new EmailSendingFailedException(e);
        }
    }

}
