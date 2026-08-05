package com.ukgqtm.app.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping(value = {"/auth/callback", "/auth/logout", "/access-denied", "/session-expired"})
    public String forwardKnownSpaRoutes() {
        return "forward:/index.html";
    }
}
