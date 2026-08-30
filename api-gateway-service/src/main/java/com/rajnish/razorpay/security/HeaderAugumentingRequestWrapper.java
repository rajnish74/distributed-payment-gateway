package com.rajnish.razorpay.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class HeaderAugumentingRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> extraHeaders = new LinkedHashMap<>();

    public HeaderAugumentingRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getAuthType() {
        return super.getAuthType();
    }

    public void putHeader(String name, String value) {
        extraHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String value = extraHeaders.get(name);
        return value != null ? value : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = extraHeaders.get(name);
        return value != null ? Collections.enumeration(Collections.singleton(value)) : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        var names = new LinkedHashSet<String>(extraHeaders.keySet());
        Collections.list(super.getHeaderNames()).forEach(names::add);
        return Collections.enumeration(names);
    }
}
