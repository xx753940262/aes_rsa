# aes_rsa
> 公司需求要求进行数据加密(Android)。
> 加密方式：AES和RSA结合
> 一、AES加密
1、先把参数拼接成json字符串，如：{"username":"15000000000","password":"123456"}
   2、生成一个16为的随机数，作为key
   3、用这个key加密json字符串
二、RSA加密
   1、用RSA算法加密AES加密key，密钥为分发的公钥
   2、将加密的key及加密的业务数据拼接为json字符串，如下：
   {"encryptkey":"Ra1Aw0IhTh/dj6LfS+fga3qPXs3PgXcxDQSLwdBOKXO9satr2L/GhwqxBfnfPNULzbJn6fvZw7x2ykS//0lGRUe4D1YUpwe7n0GocHrh17nJEpPjecZpXMWCk0N5sVtYPrbfGMfKKS5Z3WPK2fiGA77ZVwt1coCteCQiaNNACTQ=","data":"wwyrOIHoLUwOyMp6UQyzO484gcqapF2ikSGgccHnYj2EIisHllSnu5RGY0g+fK2G"}
   3、将上一步生成的json串拼接到jsonstr=的后面上送服务器，也就是说，接口只需要一个参数：jsonstr
   加密方法（在Decipher类里面）：
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
三、解密
1、返回的数据，如下：
{"data":"HbSms2HUmYurJLvi4cc17WeLpMpKRQigmwXq4FbImaKkhw7JN1jIbdsNTuCdQuI7alRKoOiXMlG9oOaRkmweLflxmi2/f5NXhcPo78ooAFjvtYkaE7uPh5UIv7s6Spdj","encryptkey":"C/hMayfxrczzsxoU8gLcL39V4YsEdQGeNCwhkgAwCYNyYjpJeL0cYHeluoC/NuY4qpjOXH65HdUahtevo78jeqTsrGRyXFWSlS2PtcNX3u782cIISLtS9tKRyr9XWtW3MnMOyNRiQQhBbSUiXYFxrIP6vdTnZc7X0JLfcuru8Zw="}
2、提取encryptkey并用私钥解密，解密算法为RSA，得到的明文就是AES解密key
3、用AES解密key解密data字段，得到的明文即为业务数据
解密方法（在Decipher类里面）:
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
     * 解密
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

四、如何使用：
1、加密：
Map<String, String> params；
Decipher decipher = new Decipher();
decipher.encryptData(params);
加密完的数据是map类型，提交http数据即可。
2、解密：
String result = Decipher.decryptResult(context, responseStr);
context为上下文，responseStr为返回的加密数据，result为解密后的数据
五、说明
在读取私钥时，用到FileInputStream读取文件(坑：用InputStream读取文件，获取不到私钥)
Android中存放文件，assets和raw中，都测试过用，都打不开私钥。
assets读取文件，AssetManager.open(String filename)，返回的是一个InputSteam类型的字节流。
raw读取文件，InputStream is = getResources().openRawResource(R.id.filename)，
返回的也是一个InputSteam类型的字节流。
所以呢，换了一种实现方式，可能很笨。
把assets文件拷贝到sd卡中，然后用FileInputStream读取文件获取私钥。
哎。。。，坑了好几天，终于解决了。



