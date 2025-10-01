package com.matrix.hiper.lite.hiper;

import android.content.Context;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.matrix.hiper.lite.utils.FileUtils;
import com.matrix.hiper.lite.utils.StringUtils;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// import com.matrix.hiper.lite.utils.LogUtils;

@SuppressWarnings("ALL")
public class Sites {

    /**
     * configuration example
     *
     *
     * # This is the hiper minimization configuration file. - (x.x.x.x/x)
     * pki:
     *   ca: "-----BEGIN HIPER CERTIFICATE-----\n first line \n second line \n third line \n-----END HIPER CERTIFICATE-----\n"
     *   cert: "-----BEGIN HIPER CERTIFICATE-----\n first line \n second line \n third line \n forth line \n-----END HIPER CERTIFICATE-----\n"
     *   key: "-----BEGIN HIPER X25519 PRIVATE KEY-----\n first line \n-----END HIPER X25519 PRIVATE KEY-----\n"
     *
     * # --------------------------------------------------------------------------------------
     * #                        WARNING >>> AUTO SYNC AREA
     * # --------------------------------------------------------------------------------------
     * # The following configuration will change at any time.
     * # Please do not configure custom content in the above area.
     * # If you need to adjust the configuration, please modify the menu to manual mode.
     * points:
     *   "x.x.x.x":
     *     - "ip : port"
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *   "x.x.x.x":
     *     - "ip : port"
     *
     * dns:
     *     - "223.5.5.5"
     *     - "114.114.114.114"
     *
     * # --------------------------------------------------------------------------------------
     * #                        WARNING <<< AUTO SYNC AREA
     * # --------------------------------------------------------------------------------------
     */

    public static class PKI {
        private final String ca;
        private final String cert;
        private final String key;

        public PKI() {
            this("", "", "");
        }

        public PKI(String ca, String cert, String key) {
            this.ca = ca;
            this.cert = cert;
            this.key = key;
        }
    }

    public static class TUN {
        private final Boolean enable;
        private final String dev;

        public TUN() {
            this(true, "tun0");
        }

        public TUN(Boolean enable, String dev) {
            this.enable = enable;
            this.dev = dev;
        }
    }

    public static class SYNC {
        private final String addition;
        private final String source;

        public SYNC() {
            this("", "");
        }

        public SYNC(String addition, String source) {
            this.addition = addition;
            this.source = source;
        }
    }

    public static class LISTEN {
        private final Number port;

        public LISTEN() {
            this(35533);
        }

        public LISTEN (Number port) {
            this.port = port;
        }
    }

    public static class LOGGING {
        private final String level;

        public LOGGING() {
            this("info");
        }

        public LOGGING (String level) {
            this.level = level;
        }
    }

    public static class IncomingSite{

        private final String name;
        private final String id;
        private HashMap<String, ArrayList<String>> points;
        private final ArrayList<UnsafeRoute> unsafeRoutes;
        @SerializedName("dns")
        private ArrayList<String> dnsResolvers;
        private final String cert;
        private final String ca;
        private final int lhDuration;
        private final int port;
        private final int mtu;
        private final String cipher;
        private final int sortKey;
        private final String logVerbosity;
        private PKI pki;
        private final TUN tun;
        private SYNC sync;
        private LISTEN listen;
        private LOGGING logging;
        @Expose(serialize = false)
        private String key;
//        private String originalYaml;
        public IncomingSite() {
            this("", "", new HashMap<>(), new ArrayList<>(), new ArrayList<>(), "", "", 0, 0, 0, "", 0, "", "", new SYNC(), new LISTEN(), new LOGGING());
        }

        public IncomingSite(String name, String id, HashMap<String, ArrayList<String>> point, ArrayList<UnsafeRoute> unsafeRoutes, ArrayList<String> dnsResolvers, String cert, String ca, int lhDuration, int port, int mtu, String cipher, int sortKey, String logVerbosity, String key, SYNC sync, LISTEN listen, LOGGING logging) {
            this.name = name;
            this.id = id;
            this.points = point;
            this.unsafeRoutes = unsafeRoutes;
            this.dnsResolvers = dnsResolvers;
            this.cert = cert;
            this.ca = ca;
            this.lhDuration = lhDuration;
            this.port = port;
            this.mtu = mtu;
            this.cipher = cipher;
            this.sortKey = sortKey;
            this.logVerbosity = logVerbosity;
            this.key = key;
            this.sync = sync;
            this.pki = new PKI(ca, cert, key);
            this.tun = new TUN();
            this.listen = listen;
            this.logging = logging;
        }

        public void setPoint(HashMap<String, ArrayList<String>> point) {
            this.points = point;
        }

        public void setDnsResolvers(ArrayList<String> dnsResolvers) {
            this.dnsResolvers = dnsResolvers;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        public HashMap<String, ArrayList<String>> getPoint() {
            return points;
        }

        public ArrayList<UnsafeRoute> getUnsafeRoutes() {
            return unsafeRoutes;
        }

        public ArrayList<String> getDnsResolvers() {
            return dnsResolvers;
        }


        public String getCert() {
            return cert;
        }

        public String getCa() {
            return ca;
        }

        public int getLhDuration() {
            return lhDuration;
        }

        public int getPort() {
            return port;
        }

        public int getMtu() {
            return mtu;
        }

        public String getCipher() {
            return cipher;
        }

        public int getSortKey() {
            return sortKey;
        }

        public String getLogVerbosity() {
            return logVerbosity;
        }

        public String getSyncAddition() {
            return sync.addition;
        }

        public String getSyncSource() {
            return sync.source;
        }

        public void save(Context context) {
            String path = context.getFilesDir().getAbsolutePath() + "/" + name + "/hiper_config.json";
            StringUtils.writeFile(path, new Gson().toJson(this));

            String keyPath = context.getFilesDir().getAbsolutePath() + "/" + name + "/hiper.key";
            StringUtils.writeFile(keyPath, key);
        }

        public void update(String conf) {
            if (conf == null || conf.isEmpty()) {
                return;
            }
            Yaml yaml = new Yaml();
            Map<String, Object> object = yaml.load(conf);

            // 安全处理可选的 points 字段
            Object pointsObj = object.get("points");
            HashMap<String, ArrayList<String>> newPoints = new HashMap<>();

            if (pointsObj != null) {
                if (pointsObj instanceof Map) {
                    Map<?, ?> rawPoints = (Map<?, ?>) pointsObj;

                    for (Map.Entry<?, ?> entry : rawPoints.entrySet()) {
                        String key = entry.getKey().toString();
                        Object value = entry.getValue();

                        ArrayList<String> valueList = new ArrayList<>();
                        if (value instanceof List) {
                            // 修复：这里将循环变量名改为 listItem
                            for (Object listItem : (List<?>) value) {
                                if (listItem instanceof String) {
                                    valueList.add((String) listItem);
                                }
                            }
                        } else if (value instanceof String) {
                            valueList.add((String) value);
                        }

                        newPoints.put(key, valueList);
                    }
                } else {
                    // 如果 points 存在但不是 Map 类型，记录警告
                    Log.e("IncomingSite", "Unexpected type for 'points' field: " + pointsObj.getClass().getSimpleName());
                }
            }
            this.points = newPoints;

            // 安全处理可选的 dns 字段
            Object dnsObj = object.get("dns");
            ArrayList<String> newDns = new ArrayList<>();
            if (dnsObj != null) {
                if (dnsObj instanceof List) {
                    // 修复：这里将循环变量名改为 dnsItem
                    for (Object dnsItem : (List<?>) dnsObj) {
                        if (dnsItem instanceof String) {
                            newDns.add((String) dnsItem);
                        }
                    }
                } else if (dnsObj instanceof String) {
                    newDns.add((String) dnsObj);
                }
            }
            this.dnsResolvers = newDns;

            // 处理其他可选字段...
            Object listenObj = object.get("listen");
            if (listenObj instanceof Map) {
                HashMap<String, Number> rawListen = (HashMap<String, Number>) listenObj;
                LISTEN listen = new LISTEN(rawListen.get("port"));
                this.listen = listen;
            }

            Object loggingObj = object.get("logging");
            if (loggingObj instanceof Map) {
                HashMap<String, String> rawLogging = (HashMap<String, String>) loggingObj;
                String level = rawLogging.get("level");
                if (level != null) {
                    LOGGING logging = new LOGGING(level);
                    this.logging = logging;
                }
            }
        }


        public static IncomingSite parse(String name, String id, String conf, String addition) {
            Yaml yaml = new Yaml();
            Map object = yaml.load(conf);

            // 修复：添加对 addition 的 null 检查
            Map additionObject;
            if (addition == null) {
                additionObject = new HashMap<>(); // 使用空的 HashMap 作为默认值
            } else {
                additionObject = yaml.load(addition);
            }

//            Map additionObject = yaml.load(addition);
            HashMap<String, String> rawSync = (HashMap<String, String>) object.get("sync");

            // 处理可能为null的rawSync
            if (rawSync == null) {
                rawSync = new HashMap<>();
            }
            SYNC sync = new SYNC(rawSync.get("addition"), rawSync.get("source"));

//            SYNC sync = new SYNC(rawSync.get("addition"), rawSync.get("source"));
            HashMap<String, String> rawLogging = (HashMap<String, String>) object.get("logging");
            if (rawLogging == null) {
                rawLogging = new HashMap<>();
                rawLogging.put("level", "error");
            }
            LOGGING logging = new LOGGING(rawLogging.get("level"));
            object.putAll(additionObject);
            HashMap<String, String> pki = (HashMap<String, String>) object.get("pki");
            String cert = pki.get("cert");
            String ca = pki.get("ca");
            String key = pki.get("key");


//            HashMap<String, ArrayList<String>> rawPoint = (HashMap<String, ArrayList<String>>) object.get("points");
//            ArrayList<String> dns = (ArrayList<String>) object.get("dns");
            // 安全处理points字段 - 修复关键点
            HashMap<String, ArrayList<String>> rawPoint = new HashMap<>();
            Object pointsObj = object.get("points");

            if (pointsObj instanceof Map) {
                // 正常情况：points是一个映射
                Map<?, ?> pointsMap = (Map<?, ?>) pointsObj;
                for (Map.Entry<?, ?> entry : pointsMap.entrySet()) {
                    String key_1 = entry.getKey().toString();

                    // 处理值可能是单个字符串或字符串列表
                    ArrayList<String> valueList = new ArrayList<>();
                    Object value = entry.getValue();
                    if (value instanceof List) {
                        for (Object item : (List<?>) value) {
                            if (item instanceof String) {
                                valueList.add((String) item);
                            }
                        }
                    } else if (value instanceof String) {
                        valueList.add((String) value);
                    }

                    rawPoint.put(key_1, valueList);
                }
            }
            // 如果points不存在或不是Map类型，rawPoint将保持为空HashMap

            // 安全处理dns字段
            ArrayList<String> dns = new ArrayList<>();
            Object dnsObj = object.get("dns");
            if (dnsObj instanceof List) {
                for (Object item : (List<?>) dnsObj) {
                    if (item instanceof String) {
                        dns.add((String) item);
                    }
                }
            } else if (dnsObj instanceof String) {
                dns.add((String) dnsObj);
            }


            HashMap<String, Number> rawListen = (HashMap<String, Number>) object.get("listen");
            if (rawListen == null) {
                rawListen = new HashMap<>();
                rawListen.put("port", 35533);
            }
            LISTEN listen = new LISTEN(rawListen.get("port"));
            return new IncomingSite(
                    name,
                    id,
                    rawPoint,
                    new ArrayList<>(),
                    dns,
                    cert,
                    ca,
                    0,
                    0,
                    1300,
                    "aes",
                    0,
                    "error",
                    key,
                    sync,
                    listen,
                    logging
            );
        }
    }

    public static class Site {

        private final String name;
        private final String id;
        private final HashMap<String, ArrayList<String>> points;
        private final ArrayList<UnsafeRoute> unsafeRoutes;
        @SerializedName("dns")
        private final ArrayList<String> dnsResolvers;
        private final CertificateInfo cert;
        private final ArrayList<CertificateInfo> ca;
        private final int lhDuration;
        private final int port;
        private final int mtu;
        private final String cipher;
        private final int sortKey;
        private final String logVerbosity;
        private final boolean connected;
        private final String status;
        private final String logFile;
        private final ArrayList<String> errors;

        // Strong representation of the site config
        @Expose(serialize = false)
        private final String config;

        public Site() {
            this("", "", new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new CertificateInfo(), new ArrayList<>(), 0, 0, 0, "", 0, "", false, "", "", new ArrayList<>(), "");
        }

        public Site(String name, String id, HashMap<String, ArrayList<String>> point, ArrayList<UnsafeRoute> unsafeRoutes, ArrayList<String> dnsResolvers, CertificateInfo cert, ArrayList<CertificateInfo> ca, int lhDuration, int port, int mtu, String cipher, int sortKey, String logVerbosity, boolean connected, String status, String logFile, ArrayList<String> errors, String config) {
            this.name = name;
            this.id = id;
            this.points = point;
            this.unsafeRoutes = unsafeRoutes;
            this.dnsResolvers = dnsResolvers;
            this.cert = cert;
            this.ca = ca;
            this.lhDuration = lhDuration;
            this.port = port;
            this.mtu = mtu;
            this.cipher = cipher;
            this.sortKey = sortKey;
            this.logVerbosity = logVerbosity;
            this.connected = connected;
            this.status = status;
            this.logFile = logFile;
            this.errors = errors;
            this.config = config;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        public HashMap<String, ArrayList<String>> getPoint() {
            return points;
        }

        public ArrayList<UnsafeRoute> getUnsafeRoutes() {
            return unsafeRoutes;
        }

        public ArrayList<String> getDnsResolvers() {
            return dnsResolvers;
        }


        public CertificateInfo getCert() {
            return cert;
        }

        public ArrayList<CertificateInfo> getCa() {
            return ca;
        }

        public int getLhDuration() {
            return lhDuration;
        }

        public int getPort() {
            return port;
        }

        public int getMtu() {
            return mtu;
        }

        public String getCipher() {
            return cipher;
        }

        public int getSortKey() {
            return sortKey;
        }

        public String getLogVerbosity() {
            return logVerbosity;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getStatus() {
            return status;
        }

        public String getLogFile() {
            return logFile;
        }

        public ArrayList<String> getErrors() {
            return errors;
        }

        public String getConfig() {
            return config;
        }

        public String getKey(Context context) {
            String keyPath = context.getFilesDir().getAbsolutePath() + "/" + name + "/hiper.key";
            return StringUtils.getStringFromFile(keyPath);
        }

        public static Site fromFile(Context context, String name) {
            String dirPath = context.getFilesDir().getAbsolutePath() + "/" + name;

            // ✅ 修正16: 优先检查YAML是否存在
            String yamlPath = dirPath + "/config.yml";
            if (!new File(yamlPath).exists()) {
                // 尝试从旧JSON恢复
                String jsonPath = dirPath + "/hiper_config.json";
                if (new File(jsonPath).exists()) {
                    String json = StringUtils.getStringFromFile(jsonPath);
                    if (json != null) {
                        try {
                            Gson gson = new Gson();
                            JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
                            if (jsonElement.isJsonObject()) {
                                JsonObject obj = jsonElement.getAsJsonObject();
                                if (obj.has("originalYaml")) {
                                    String yaml = obj.get("originalYaml").getAsString();
                                    if (yaml != null && !yaml.isEmpty()) {
                                        // 重建YAML文件
                                        FileUtils.createDirectory(dirPath);
                                        StringUtils.writeFile(yamlPath, yaml);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Site", "Failed to recover YAML", e);
                        }
                    }
                }
            }

            // ✅ 修正17: 原有逻辑保持不变
            String path = dirPath + "/hiper_config.json";
            String s = StringUtils.getStringFromFile(path);
            if (s == null) return null;

            IncomingSite incomingSite = new Gson().fromJson(s, IncomingSite.class);
            ArrayList<String> errors = new ArrayList<>();
            CertificateInfo cert = new CertificateInfo();
            ArrayList<CertificateInfo> ca = new ArrayList<>();
            try {
                String rawDetails = mobile.Mobile.parseCerts(incomingSite.cert);
                CertificateInfo[] certs = new Gson().fromJson(rawDetails, CertificateInfo[].class);
                if (certs.length == 0) {
                    errors.add("No certificate found");
                }
                cert = certs[0];
                if (!cert.getValidity().isValid()) {
                    errors.add("Certificate is invalid: " + cert.getValidity().getReason());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors.add(e.toString());
            }
            try {
                String rawCa = mobile.Mobile.parseCerts(incomingSite.getCa());
                CertificateInfo[] caArray = new Gson().fromJson(rawCa, CertificateInfo[].class);
                ca = new ArrayList<>(Arrays.asList(caArray));
                boolean hasErrors = false;
                for (CertificateInfo info : ca) {
                    if (!info.getValidity().isValid()) {
                        hasErrors = true;
                        break;
                    }
                }
                if (hasErrors) {
                    errors.add("There are issues with 1 or more ca certificates");
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors.add("Error while loading certificate authorities: " + e);
            }
            return new Site(
                    incomingSite.getName(),
                    incomingSite.getId(),
                    incomingSite.getPoint(),
                    incomingSite.getUnsafeRoutes(),
                    incomingSite.getDnsResolvers(),
                    cert,
                    ca,
                    incomingSite.getLhDuration(),
                    incomingSite.getPort(),
                    incomingSite.getMtu(),
                    incomingSite.getCipher(),
                    incomingSite.getSortKey(),
                    incomingSite.getLogVerbosity(),
                    false,
                    "Disconnected",
                    context.getFilesDir().getAbsolutePath() + "/" + incomingSite.name + "/hiper.log",
                    errors,
                    s
            );
        }


    }

    public static class CertificateDetails {

        private final List<String> name;
        private final String notBefore;
        private final String notAfter;
        private final String publicKey;
        private final List<String> groups;
        private final List<String> network;
        private final List<String> subnets;
        private final boolean isCa;
        private final String issuer;

        public CertificateDetails() {
            this(new ArrayList<>(), "", "", "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false, "");
        }

        public CertificateDetails(List<String> name, String notBefore, String notAfter, String publicKey, List<String> groups, List<String> ips, List<String> subnets, boolean isCa, String issuer) {
            this.name = name;
            this.notBefore = notBefore;
            this.notAfter = notAfter;
            this.publicKey = publicKey;
            this.groups = groups;
            this.network = ips;
            this.subnets = subnets;
            this.isCa = isCa;
            this.issuer = issuer;
        }

        public List<String> getName() {
            return name;
        }

        public String getNotBefore() {
            return notBefore;
        }

        public String getNotAfter() {
            return notAfter;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public List<String> getGroups() {
            return groups;
        }

        public List<String> getIps() {
            return network;
        }

        public List<String> getSubnets() {
            return subnets;
        }

        public boolean isCa() {
            return isCa;
        }

        public String getIssuer() {
            return issuer;
        }

    }

    public static class Certificate {

        private final String fingerprint;
        private final String signature;
        private final CertificateDetails details;

        public Certificate() {
            this("", "", new CertificateDetails());
        }

        public Certificate(String fingerprint, String signature, CertificateDetails details) {
            this.fingerprint = fingerprint;
            this.signature = signature;
            this.details = details;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public String getSignature() {
            return signature;
        }

        public CertificateDetails getDetails() {
            return details;
        }

    }

    public static class CertificateValidity {

        @SerializedName("Valid")
        private final boolean valid;
        @SerializedName("Reason")
        private final String reason;

        public CertificateValidity() {
            this(false, "");
        }

        public CertificateValidity(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

    }

    public static class CertificateInfo {

        @SerializedName("Cert")
        private final Certificate cert;
        @SerializedName("RawCert")
        private final String rawCert;
        @SerializedName("Validity")
        private final CertificateValidity validity;

        public CertificateInfo() {
            this(new Certificate(), "", new CertificateValidity());
        }

        public CertificateInfo(Certificate cert, String rawCert, CertificateValidity validity) {
            this.cert = cert;
            this.rawCert = rawCert;
            this.validity = validity;
        }

        public Certificate getCert() {
            return cert;
        }

        public String getRawCert() {
            return rawCert;
        }

        public CertificateValidity getValidity() {
            return validity;
        }

    }

    public static class StaticHosts {

        private final boolean tower;
        private final List<String> destinations;

        public StaticHosts() {
            this(false, new ArrayList<>());
        }

        public StaticHosts(boolean tower, List<String> destinations) {
            this.tower = tower;
            this.destinations = destinations;
        }

        public boolean isTower() {
            return tower;
        }

        public List<String> getDestinations() {
            return destinations;
        }

    }

    public static class UnsafeRoute {

        private final String route;
        private final String via;
        private final int mtu;

        public UnsafeRoute() {
            this("", "", 0);
        }

        public UnsafeRoute(String route, String via, int mtu) {
            this.route = route;
            this.via = via;
            this.mtu = mtu;
        }

        public String getRoute() {
            return route;
        }

        public String getVia() {
            return via;
        }

        public int getMtu() {
            return mtu;
        }

    }

    private static class Relay{

        private final boolean allowRelay;
        private final ArrayList<String> relays;

        public Relay() {
            this(false, new ArrayList<>());
        }

        public Relay(boolean allowRelay, ArrayList<String> relays) {
            this.allowRelay = allowRelay;
            this.relays = relays;
        }

    }

}
