package org.sde.cec.consensus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sde.cec.account.RSAKeyPair;
import org.sde.cec.account.RSAKeyStorage;
import org.sde.cec.model.Cache;
import org.sde.cec.model.P2PMessage;
import org.sde.cec.scvm.ContractCollection;
import org.sde.cec.scvm.VM;
import org.sde.cec.util.NodeList;
import org.sde.cec.util.P2PBroadcasting;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class CacheSynchronization {
    //缓存同步专用共识机制，在节点初始化时需要执行
    //主节点缓存广播算法，包括所有合约虚拟机初始化的缓存和所有账户缓存，不包含合约实例的缓存
    public String leaderCache() throws IOException {
        Cache cache=new Cache();
        //主节点获取所有缓存并组装
        cache.setAccountList(RSAKeyStorage.accountList);
        cache.setKeyStorageBase64(RSAKeyStorage.keyStorageBase64);
        cache.setAccountAddressList(RSAKeyStorage.accountAddressList);
        cache.setScSet(VM.scSet);
        cache.setScAccountMap(ContractCollection.scAccountMap);
        cache.setScMapBase64(ContractCollection.scMapBase64);
        cache.setScAddressList(ContractCollection.scAddressList);
        cache.setScClassName(ContractCollection.scClassName);
        String cachejson = new ObjectMapper().writeValueAsString(cache);
        //构造广播消息
        P2PMessage pm=new P2PMessage();
        pm.setNetAddress(NodeList.local);
        pm.setMessageType("leaderCacheSync");
        Date currentDate = new Date();  // 获取当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
        String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
        pm.setMessageTime(formattedDate.toString());
        pm.setSig(NodeList.localsig);
        pm.setMessage(cachejson);
        //广播
        P2PBroadcasting p2pb=new P2PBroadcasting();
        p2pb.springbootRpcBroadcasting(pm);
        System.out.println("广播缓存完成"+cache.hashCode());
        return "leaderCache_Finish";
    }
    public String salveCache(String P2PMessage) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Cache realCache=new Cache();
        ObjectMapper objectMapper = new ObjectMapper();
        String message=P2PMessage;
        Type type1 = P2PMessage.class;
        JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
        P2PMessage pMessage=objectMapper.readValue(message, javaType1);
        String cache=pMessage.getMessage();
        Type type2 = Cache.class;
        JavaType javaType2 = objectMapper.getTypeFactory().constructType(type2);
        realCache=objectMapper.readValue(cache, javaType2);
        RSAKeyStorage.accountAddressList=realCache.getAccountAddressList();
        RSAKeyStorage.accountList=realCache.getAccountList();
        //逐个恢复密钥对
        for(int i=0;i<RSAKeyStorage.accountAddressList.size();i++){
            String tempPubkey=realCache.getKeyStorageBase64().get(RSAKeyStorage.accountAddressList.get(i)).pubKey;
            String tempPrikey=realCache.getKeyStorageBase64().get(RSAKeyStorage.accountAddressList.get(i)).priKey;
            PublicKey pubKey= KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(tempPubkey)));
            PrivateKey priKey= KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(tempPrikey)));
            RSAKeyPair rkp=new RSAKeyPair();
            rkp.priKey=priKey;
            rkp.pubKey=pubKey;
            RSAKeyStorage.keyStorage.put(RSAKeyStorage.accountAddressList.get(i),rkp);


        }
        RSAKeyStorage.keyStorageBase64=realCache.getKeyStorageBase64();
        //RSAKeyStorage.keyStorage=realCache.getKeyStorage();

        VM.scSet=realCache.getScSet();
        ContractCollection.scAccountMap=realCache.getScAccountMap();
        ContractCollection.scMap=realCache.getScMap();
        ContractCollection.scAddressList=realCache.getScAddressList();
        //逐个恢复密钥对
        for(int i=0;i<ContractCollection.scAddressList.size();i++){
            String tempPubkey=realCache.getScMapBase64().get(ContractCollection.scAddressList.get(i)).pubKey;
            String tempPrikey=realCache.getScMapBase64().get(ContractCollection.scAddressList.get(i)).priKey;
            PublicKey pubKey= KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(tempPubkey)));
            PrivateKey priKey= KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(tempPrikey)));
            RSAKeyPair rkp=new RSAKeyPair();
            rkp.priKey=priKey;
            rkp.pubKey=pubKey;
            ContractCollection.scMap.put(ContractCollection.scAddressList.get(i),rkp);


        }
        ContractCollection.scClassName=realCache.getScClassName();
        //System.out.println("测试数据同步"+VM.scSet.get(ContractCollection.scAddressList.get(0)));
        //System.out.println("测试数据同步"+ContractCollection.scAddressList.get(0));
        System.out.println("同步缓存完成"+realCache.hashCode());
        return "salveCache_Finish";
    }
}
