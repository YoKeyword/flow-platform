/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.controller;

import com.flow.platform.api.domain.EmailSettingContent;
import com.flow.platform.api.domain.MessageType;
import com.flow.platform.api.domain.response.SmtpAuthResponse;
import com.flow.platform.api.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */

@RestController
@RequestMapping(path = "/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping(path = "/email/settings")
    public EmailSettingContent createEmailSetting(@RequestBody EmailSettingContent emailSetting) {
        return (EmailSettingContent) messageService.save(emailSetting);
    }

    @GetMapping(path = "/email/settings")
    public EmailSettingContent showEmailSetting() {
        return (EmailSettingContent) messageService.find(MessageType.EMAIl);
    }

    @PatchMapping(path = "/email/settings")
    public EmailSettingContent updateEmailSetting(@RequestBody EmailSettingContent emailSetting) {
        return (EmailSettingContent) messageService.update(emailSetting);
    }

    @PostMapping(path = "/email/settings/auth")
    public SmtpAuthResponse authEmailSetting(@RequestBody EmailSettingContent emailSetting) {
        if (messageService.authEmailSetting(emailSetting)) {
            return new SmtpAuthResponse(true);
        } else {
            return new SmtpAuthResponse(false);
        }
    }
}
