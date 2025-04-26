/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.enums.ResponseMessage;
import me.amlu.shop.amlume_shop.exceptions.EmailSendingFailedException;
import me.amlu.shop.amlume_shop.payload.CreateEmailRequest;
import me.amlu.shop.amlume_shop.payload.GetResponse;
import me.amlu.shop.amlume_shop.security.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

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
            return new GetResponse("Email Sent Successfully", createEmailRequest.toList(), ResponseMessage.SUCCESS);
        } catch (Exception e) {
            log.error("sendEmail() | Error : {}", e.getMessage());
            throw new EmailSendingFailedException(e);
        }
    }

}