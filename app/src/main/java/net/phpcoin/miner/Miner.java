package net.phpcoin.miner;

import com.google.gson.Gson;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class Miner {

    public boolean started;
    private MinerListener minerListener;
    public MinerStatus minerStatus;
    private Wallet wallet;

    public static String node = "https://miner1.phpcoin.net";

    public Miner(Wallet wallet, MinerListener minerListener) {
        this.wallet = wallet;
        this.minerListener = minerListener;
        this.minerStatus = new MinerStatus();
    }

    public void start() throws IOException {
        started = true;
        this.minerListener.onStatus();
        NodeResponse nodeResponse = getMineInfo();

        BlockData blockData = BlockData.parse(nodeResponse.data);
        Integer nodeTime = blockData.time;
        Integer height = blockData.height + 1;
        Long difficulty = Long.parseLong(blockData.difficulty);
        Long now = System.currentTimeMillis() / 1000;
        Long offset = nodeTime - now;

        boolean blockFound = false;
        int cnt = 0;
        Long startTime = System.currentTimeMillis();
        String argon = null;
        String nonce = null;
        Long elapsed = null;
        String address = this.wallet.account.address;
        while (started && !blockFound) {
            cnt++;
            now = System.currentTimeMillis() / 1000;
            Integer prevBlockDate = blockData.date;
            elapsed = now - offset - prevBlockDate.longValue();
            argon = calcualateArgon(prevBlockDate, elapsed, blockData.hashingOptions);
            String chainId = blockData.chain_id;
//            System.out.println(argon);
            nonce = calculateNonce(chainId, address, prevBlockDate, elapsed, argon);
//            System.out.println(nonce);
            Long hit = calculateHit(address, nonce, height, difficulty);
//            System.out.println(hit);
            Long target = calculateTarget(elapsed, difficulty);
//            System.out.println(target);
            blockFound = hit > 0 && target > 0 && hit > target;
            Long endTime = System.currentTimeMillis();
            Long diff = endTime - startTime;
            Double speed = (double) cnt / (diff / 1000);
            this.minerStatus = new MinerStatus();
            this.minerStatus.elapsed = elapsed;
            this.minerStatus.hit = hit;
            this.minerStatus.target = target;
            this.minerStatus.speed = speed;
            this.minerListener.onStatus();
            System.out.println("height=" + height + " elapsed=" + elapsed + " hit=" + hit + " target=" + target + " speed=" + speed + " H/s BLOCK FOUND=" + blockFound);
            if(blockFound) {
                submitHash(argon, nonce, height, difficulty, address, nodeTime + elapsed, elapsed);
                blockFound = false;
            }
        }
    }
    public void stop() {
        started = false;
        this.minerStatus = new MinerStatus();
        this.minerStatus.elapsed = 1;
        this.minerListener.onStatus();
    }

    public static abstract class MinerListener {
        public abstract void onStatus();
    }

    public static class MinerStatus {
        public long elapsed;
        public long hit;
        public long target;
        public double speed;
    }

    private NodeResponse getMineInfo() throws IOException {

        String  websiteURL = node + "/mine.php?q=info";
        URL url = new URL(websiteURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println(content);

        Gson gson = new Gson();
        NodeResponse nodeResponse = gson.fromJson(String.valueOf(content), NodeResponse.class);

        return nodeResponse;
    }

    private static void submitHash(String argon, String nonce, int height, long difficulty, String address, long date, long elapsed) throws IOException {
        String  websiteURL = node + "/mine.php?q=submitHash";
        URL url = new URL(websiteURL);
        Gson gson = new Gson();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        List<AbstractMap.SimpleEntry> params = new ArrayList<AbstractMap.SimpleEntry>();
        params.add(new AbstractMap.SimpleEntry("argon", argon));
        params.add(new AbstractMap.SimpleEntry("nonce", nonce));
        params.add(new AbstractMap.SimpleEntry("height", height));
        params.add(new AbstractMap.SimpleEntry("difficulty", difficulty));
        params.add(new AbstractMap.SimpleEntry("address", address));
        params.add(new AbstractMap.SimpleEntry("date", date));
        params.add(new AbstractMap.SimpleEntry("elapsed", elapsed));
        params.add(new AbstractMap.SimpleEntry("minerInfo", "JavaMiner 0.1"));

//        JsonObject data = new JsonObject();
//        data.addProperty("argon", argon);
//        data.addProperty("nonce", nonce);
//        data.addProperty("height", String.valueOf(height));
//        data.addProperty("difficulty", String.valueOf(difficulty));
//        data.addProperty("address", address);
//        data.addProperty("date", String.valueOf(date));
//        data.addProperty("elapsed", String.valueOf(elapsed));
//        data.addProperty("minerInfo", "JavaMiner 0.1");

        OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println(content);

        NodeResponse nodeResponse = gson.fromJson(String.valueOf(content), NodeResponse.class);
//        System.out.println(nodeResponse);

    }

    private static String getQuery(List<AbstractMap.SimpleEntry> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode((String) pair.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(String.valueOf(pair.getValue()), "UTF-8"));
        }

        return result.toString();
    }

    private static Long calculateTarget(Long elapsed, Long difficulty) {
        if(elapsed == null || elapsed ==0) {
            return null;
        }
        BigInteger target = (BigInteger.valueOf(difficulty).multiply(BigInteger.valueOf(60)).divide(BigInteger.valueOf(elapsed)));
        return target.longValue();
    }

    private static Long calculateHit(String miner, String nonce, Integer height, Long difficulty) {
        String base = miner + "-" + nonce + "-" + height + "-" + difficulty;
        String hash = CryptoUtil.hash("SHA-256", base);
        hash = CryptoUtil.hash("SHA-256", hash);
        String hashPart = hash.substring(0, 8);
//        System.out.println(hashPart);
        BigInteger value = new BigInteger(hashPart, 16);
        BigInteger max = new BigInteger("ffffffff", 16);
        BigInteger hit = max.multiply(BigInteger.valueOf(1000)).divide(value);
        return hit.longValue();
    }

    private static String calculateNonce(String chainId, String miner, Integer prevBlockDate, Long elapsed, String argon) {
        String nonceBase = chainId + miner +"-" + prevBlockDate +"-" + elapsed + "-" + argon;
        String calcNonce = CryptoUtil.hash("SHA-256", nonceBase);
        return calcNonce;
    }

    private static byte[] generateSalt16Byte() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private static String calcualateArgon(Integer prevBlockDate, Long elapsed, HashingOptions hashingOptions) {
        String base = prevBlockDate + "-" + elapsed;
//        System.out.println(base);
        byte[] salt = generateSalt16Byte();
        byte[] hash = new byte[32];
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_i).
                withSalt(salt).
                withParallelism(hashingOptions.threads).
                withMemoryAsKB(hashingOptions.memory_cost).
                withIterations(hashingOptions.time_cost).
                build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        generator.generateBytes(base.toString().toCharArray(), hash);
        return Argon2EncodingUtils.encode(hash, params);
    }

}
