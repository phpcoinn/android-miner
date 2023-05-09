package net.phpcoin.miner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;

public class MainActivity extends AppCompatActivity {

    private Wallet wallet;
    private Miner miner;
    TextView txtAddress;
    TextView textView;
    TextView txtElapsed;
    TextView txtHit;
    TextView txtTarget;
    TextView txtSpeed;
    Button btnVerify;
    Button btnStart;
    Button btnStop;
    Button btnCopy;
    LinearLayout lMinerStat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBouncyCastle();
        setContentView(R.layout.activity_main);
        wallet = new Wallet(this, new Wallet.WalletListener() {
            @Override
            public void onChange() {
                updateView();
            }
        });
        miner = new Miner(wallet, new Miner.MinerListener() {
            @Override
            public void onStatus() {
                updateView();
            }
        });
        txtAddress = (TextView)findViewById(R.id.txtAddress);
        textView = (TextView)findViewById(R.id.textView);
        txtElapsed = (TextView)findViewById(R.id.txtElapsed);
        txtHit = (TextView)findViewById(R.id.txtHit);
        txtTarget = (TextView)findViewById(R.id.txtTarget);
        txtSpeed = (TextView)findViewById(R.id.txtSpeed);
        btnVerify = ((Button)findViewById(R.id.btnVerify));
        btnCopy = ((Button)findViewById(R.id.btnCopy));
        btnStart = ((Button)findViewById(R.id.btnStart));
        btnStop = ((Button)findViewById(R.id.btnStop));
        lMinerStat = ((LinearLayout)findViewById(R.id.lMinerStat));
        updateView();
        wallet.checkVerified();
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wallet.verify();
            }
        });
        btnCopy.setVisibility(View.GONE);
        txtAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(textView.getText().equals("Address")) {
                    textView.setText("Private key");
                    txtAddress.setText(wallet.account.privateKey);
                    btnCopy.setVisibility(View.VISIBLE);
                } else {
                    textView.setText("Address");
                    txtAddress.setText(wallet.account.address);
                    btnCopy.setVisibility(View.GONE);
                }
            }
        });
        btnCopy.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Private key", wallet.account.privateKey);
                clipboard.setPrimaryClip(clip);
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            miner.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                miner.stop();
            }
        });
    }

    private void updateView() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtAddress.setText(wallet.account.address);
                    btnVerify.setVisibility(!wallet.verified ? View.VISIBLE : View.GONE);
                    btnStart.setVisibility((wallet.verified && !miner.started) ? View.VISIBLE : View.GONE);
                    btnStop.setVisibility((wallet.verified && miner.started) ? View.VISIBLE : View.GONE);
                    lMinerStat.setVisibility(wallet.verified ? View.VISIBLE : View.GONE);
                    txtElapsed.setText(String.valueOf(miner.minerStatus.elapsed));
                    txtHit.setText(String.valueOf(miner.minerStatus.hit));
                    txtTarget.setText(String.valueOf(miner.minerStatus.target));
                    txtSpeed.setText(String.valueOf(miner.minerStatus.speed));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

}