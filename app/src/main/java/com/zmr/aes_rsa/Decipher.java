package com.zmr.aes_rsa;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * 描述：
 * 作者：徐小星 on 2016/9/2 0002 17:52
 * 邮箱：xx@yougudongli.com
 */
public class Decipher {

    /**
     * 解密返回的数据
     *
     * @param context
     * @param result
     * @return
     */
    public static String decryptResult(Context context, String result) {
        try {
            result = new Decipher().decryptData(context, result);
            System.out.println("result==" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 解密1
     *
     * @return
     */
    public String decryptData(Context context, String post) throws Exception {

        // 将返回的json串转换为map

        TreeMap<String, String> map = JSON.parseObject(post,
                new TypeReference<TreeMap<String, String>>() {
                });
        String encryptkey = map.get("encryptkey");
        String data = map.get("data");


        System.out.println("返回的数据encryptkey============>" + encryptkey);
        System.out.println("data===========>" + data);

        String path = "rsa/user-rsa.pfx";
        AssetsCopyTOSDcard assetsCopyTOSDcard = new AssetsCopyTOSDcard(context);
        assetsCopyTOSDcard.AssetToSD(path, Environment.getExternalStorageDirectory().toString() + "/" + path);

        // 获取自己私钥解密
        PrivateKey pvkformPfx = RSA.getPvkformPfx(Environment.getExternalStorageDirectory().toString() + "/" + path,
                ReapalConfig.password);
        System.out.println("pvkformPfxpvkformPfx===========>" + data);
        String decryptData = RSA.decrypt(encryptkey, pvkformPfx);

        post = AES.decryptFromBase64(data, decryptData);

        return post;
    }

    /**
     * 解密2
     *
     * @param data
     * @param encryptkey
     * @return
     */
    public String decryptData(Context context, String encryptkey, String data)
            throws Exception {
        AssetManager assetManager = context.getAssets();
        AssetFileDescriptor fileDescriptor = assetManager.openFd("rsa/user-rsa.pfx");
        FileInputStream fis = fileDescriptor.createInputStream();

        // 获取自己私钥解密
        PrivateKey pvkformPfx = RSA.getPvkformPfx("", ReapalConfig.password);

        String decryptKey = RSA.decrypt(encryptkey, pvkformPfx);

        return AES.decryptFromBase64(data, decryptKey);

    }


    /**
     * 加密
     *
     * @return
     */
    public Map<String, String> encryptData(Map<String, String> param) throws Exception {

        String json = map2json(param);
        System.out.println("json数据=============>" + json);
        InputStream is = getClass().getClassLoader().getResourceAsStream("assets/public-rsa.cer");
        // 商户获取融宝公钥
        PublicKey pubKeyFromCrt = RSA.getPubKeyFromCRT(is);
        // 随机生成16数字
        String key = getRandom(16);

        // 使用RSA算法将商户自己随机生成的AESkey加密
        String encryptKey = RSA.encrypt(key, pubKeyFromCrt);
        // 使用AES算法用随机生成的AESkey，对json串进行加密
        String encryData = AES.encryptToBase64(json, key);

        System.out.println("密文key============>" + encryptKey);
        System.out.println("密文数据===========>" + encryData);

        Map<String, String> map = new HashMap<String, String>();
        map.put("data", encryData);
        map.put("encryptkey", encryptKey);

        Map<String, String> result = new HashMap<String, String>();
        result.put("jsonstr", map2json(map));

        Log.e("jsonstr=", map2json(result));
        return result;
    }

    public static Random random = new Random();

    public static String getRandom(int length) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < length; i++) {
            boolean isChar = (random.nextInt(2) % 2 == 0);// 输出字母还是数字
            if (isChar) { // 字符串
                int choice = (random.nextInt(2) % 2 == 0) ? 65 : 97; // 取得大写字母还是小写字母
                ret.append((char) (choice + random.nextInt(26)));
            } else { // 数字
                ret.append(Integer.toString(random.nextInt(10)));
            }
        }
        return ret.toString();
    }
    /**
     * map转换为json
     *
     * @param map
     * @return
     */
    public static String map2json(Map<String, String> map) {
        Gson gson = new Gson();
        String result = gson.toJson(map);
        return result;
    }


}

