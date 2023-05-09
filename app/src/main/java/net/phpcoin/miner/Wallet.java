package net.phpcoin.miner;

import android.content.Context;
import android.preference.PreferenceManager;

import android.widget.Toast;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class Wallet {

    public boolean verified;
    private WalletListener walletListener;
    private Context context;
    public Account account;

    public Wallet(Context context, WalletListener walletListener) {
        this.walletListener = walletListener;
        this.context = context;
        loadOrCreateAccount();
    }

    private void loadOrCreateAccount() {
        if(!accountExists()) {
            creteAndStoreAccount();
        } else {
            readAccount();
        }
    }

    private void readAccount() {
        String accountStr = PreferenceManager.getDefaultSharedPreferences(this.context).getString("account", null);
        Gson gson = new Gson();
        Account account = gson.fromJson(accountStr, Account.class);
        this.account = account;
    }

    private String getAddress(String privateKey) {
        return "PHP...az";
    }

    private void creteAndStoreAccount() {
        try {
            this.account = CryptoUtil.createAccount();
            Gson gson = new Gson();
            String accountStr = gson.toJson(this.account);
            PreferenceManager.getDefaultSharedPreferences(this.context).edit().putString("account", accountStr).commit();
        } catch (IOException | NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    private boolean accountExists() {
        return PreferenceManager.getDefaultSharedPreferences(this.context).contains("account");
    }

    public void checkVerified() {
        verified = false;
        this.walletListener.onChange();
    }

    public void verify() {
        verified = true;
        Toast.makeText(context, "Not implemented!", Toast.LENGTH_LONG).show();
        this.walletListener.onChange();
    }


    public abstract static class WalletListener {
        public abstract void onChange();
    }

    public static class Account implements Serializable {
        public String privateKey;
        public String publicKey;
        public String address;

        public Account(String privateKey, String publicKey, String address) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.address = address;
        }
    }

}

