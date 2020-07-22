package com.techexpert.indianvaarta;

public class contacts
{

    private String name,status,image,uid;

    public contacts()
    {

    }

    public contacts(String name, String status, String image, String uid)
    {
        this.name = name;
        this.status = status;
        this.image = image;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUid(){
        return uid;
    }

    public void setUid(String uid){
        this.uid = uid;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

}