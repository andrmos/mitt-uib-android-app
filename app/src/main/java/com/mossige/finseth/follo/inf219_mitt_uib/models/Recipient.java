package com.mossige.finseth.follo.inf219_mitt_uib.models;

/**
 * Created by Follo on 20.03.2016.
 */
public class Recipient {

    private String id;
    private String name;
    private String group;

    public Recipient(String id, String name) {
        this.id = id;
        this.name = name;
        this.group = null;

    }

    public Recipient(String id, String name, String group) {
        this.id = id;
        this.name = name;
        this.group = group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}