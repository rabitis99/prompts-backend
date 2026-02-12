package org.example.sharedprompts.module.domain.production.service.format.pdf;

public interface HtmlToPdfConverter {

    byte[] convert(String html, String css);
}

