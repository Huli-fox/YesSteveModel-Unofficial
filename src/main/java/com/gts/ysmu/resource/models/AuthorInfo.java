package com.gts.ysmu.resource.models;

import com.gts.ysmu.util.data.OrderedStringMap;

public class AuthorInfo {
    private final String name;
    private final String role;
    private final OrderedStringMap<String, String> contact;
    private final String comment;

    public AuthorInfo(String name, String role, OrderedStringMap<String, String> contact, String comment) {
        this.name = name;
        this.role = role;
        this.contact = contact;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public OrderedStringMap<String, String> getContact() {
        return contact;
    }

    public String getComment() {
        return comment;
    }
}
