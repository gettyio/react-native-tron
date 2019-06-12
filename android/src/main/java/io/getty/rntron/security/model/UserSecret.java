package io.getty.rntron.security.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class UserSecret extends RealmObject {


    @PrimaryKey
    private String  privateKey;

    private String address;
    private Boolean confirmed = true;
    private String  mnemonic;
    private String type = "mnemonics";
    private Boolean hide = false;

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey =  privateKey;
    }


    public void setHide(Boolean hide) {
        this.hide = hide;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
