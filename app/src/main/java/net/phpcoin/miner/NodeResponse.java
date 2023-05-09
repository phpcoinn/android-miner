package net.phpcoin.miner;

import com.google.gson.internal.LinkedTreeMap;

class TransactionData{
    public int date;
    public String dst;
    public String fee;
    public String id;
    public String message;
    public String public_key;
    public String signature;
    public String src;
    public int type;
    public String val;
}

class BlockData{
    public String difficulty;
    public String block;
    public int height;
    public int date;
    public Object data;
    public int time;
    public String reward;
    public String version;
    public String generator;
    public String ip;
    public HashingOptions hashingOptions;
    public String chain_id;

    public static BlockData parse(Object data) {
        LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) data;
        BlockData blockData = new BlockData();
        blockData.difficulty = (String) map.get("difficulty");
        blockData.block = (String) map.get("block");
        blockData.height = ((Double) map.get("height")).intValue();
        blockData.date = ((Double) map.get("date")).intValue();
        blockData.data = map.get("data");
        blockData.time = ((Double) map.get("time")).intValue();
        blockData.reward = (String) map.get("reward");
        blockData.version = (String) map.get("version");
        blockData.generator = (String) map.get("generator");
        blockData.ip = (String) map.get("ip");
        blockData.chain_id = (String) map.get("chain_id");
        blockData.hashingOptions = HashingOptions.parse(map.get("hashingOptions"));
        return blockData;
    }
}

class HashingOptions{
    public int memory_cost;
    public int time_cost;
    public int threads;

    public static HashingOptions parse(Object obj) {
        LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) obj;
        HashingOptions hashingOptions = new HashingOptions();
        hashingOptions.memory_cost = ((Double) map.get("memory_cost")).intValue();
        hashingOptions.time_cost = ((Double) map.get("time_cost")).intValue();
        hashingOptions.threads = ((Double) map.get("threads")).intValue();
        return hashingOptions;
    }
}

public class NodeResponse{
    public String status;
    public Object data;
    public String coin;
    public String version;
    public String network;
    public String chain_id;
}