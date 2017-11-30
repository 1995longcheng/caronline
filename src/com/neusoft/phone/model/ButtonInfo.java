package com.neusoft.phone.model;

/**
 * Tab title list info class.
 *
 * @author neusoft
 */
public class ButtonInfo {
    /** Tab name.*/
    private String name;
    /** Tab icon.*/
    private int appImage;

    /**
     * Constructor.
     *
     * @param name Tab name
     * @param appImage Tab icon
     */
    public ButtonInfo(String name, int appImage) {
        super();
        this.name = name;
        this.appImage = appImage;
    }

    public int getAppImage() {
        return appImage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
