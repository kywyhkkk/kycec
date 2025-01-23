package org.sde.cec.consensus;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sde.cec.account.Account;
import org.sde.cec.account.RSAKeyPair;
import org.sde.cec.account.RSAKeyStorage;
import org.sde.cec.basicModel.MerklePatriciaTree;
import org.sde.cec.ledger.simpleBlockchain;
import org.sde.cec.model.*;
import org.sde.cec.scvm.VM;
import org.sde.cec.util.NodeList;
import org.sde.cec.util.NodeList2;
import org.sde.cec.util.P2PBroadcasting;
import org.sde.cec.util.hashCreat;
import org.sde.cec.offChain.offChainCommunication;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class POST {
    public static String EXPER_MODEL= "simple";

    public String txs="{}";
    public static Map<String, Integer> prepare_voteCollect = new HashMap<String, Integer>();

    public static Map<String, Integer> commit_voteCollect = new HashMap<String, Integer>();

    public static Map<String, Integer> sync_voteCollect = new HashMap<String, Integer>();

    public String start(OC_Cache oc_cache0, Map<String, RSAKeyPair> keyStorage) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Type type = consensusTxSet.class;
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        String ctsjson0 = oc_cache0.ctsjson;
        consensusTxSet txSet = objectMapper.readValue(ctsjson0, javaType);
        int totalTxNumber=txSet.getTxlist().size();
        if(EXPER_MODEL== "simple"){
            TransactionVerify tv=new TransactionVerify();
            try{
                if(tv.sigVerify(txSet,keyStorage)==false){
                    System.out.println("测试模式，忽略验签失败");
                    //return "txSigVerify_false";
                }
            }catch(NullPointerException e){
                System.out.println("测试模式，忽略验签exception");
            }
            hashCreat hc=new hashCreat();
            blockHeader bh=new blockHeader();
            blockBody bb=new blockBody();
            bb.setCtxset(txSet);
            bh.setBlockBodyHash(hc.hashSHA_256(new ObjectMapper().writeValueAsString(bb)));
            bh.setBlockTxSize(totalTxNumber);
            //bh.setBlockSize((int) RamUsageEstimator.sizeOf(bb));
            if(!simpleBlockchain.blockHashList.isEmpty()){
                bh.setPre_blockTotalHash(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));
            }else {
                bh.setPre_blockTotalHash("Genesis");
            }
            bh.setBlockTotalHash(hc.hashSHA_256(new ObjectMapper().writeValueAsString(bh)));
            OC_Cache oc_cache = new OC_Cache();
            block b = new block();
            b.setBlockbody(bb);
            b.setBlockheader(bh);
            //oc_cache.block = b;
            String ctsjson = new ObjectMapper().writeValueAsString(txSet);
            oc_cache.ctsjson = ctsjson;
            oc_cache.accountList= RSAKeyStorage.accountList;
            //System.out.println("2");
            P2PMessage pm=new P2PMessage();
            pm.setNetAddress(NodeList.local);
            //System.out.println("3");
            pm.setMessageType("offc_receive");//调用OCContractExecution(pMessage);
            Date currentDate = new Date();  // 获取当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
            String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
            pm.setMessageTime(formattedDate.toString());
            pm.setSig(NodeList.localsig);
            pm.setMessage(new ObjectMapper().writeValueAsString(oc_cache));
            offChainCommunication occ = new offChainCommunication();
            occ.offChainCommunication(pm, "127.0.0.1:9805");//一个链下的地址 类似nodelist写一个list，然后用get(i)


        }
        return "";
    }


    public String Commit(P2PMessage pm) throws Exception {
        OC_Cache realCache = new OC_Cache();
        ObjectMapper objectMapper = new ObjectMapper();

        String cache = pm.getMessage();
        Type type2 = OC_Cache.class;
        JavaType javaType2 = objectMapper.getTypeFactory().constructType(type2);
        realCache = objectMapper.readValue(cache, javaType2);
        List<String> resList = new ArrayList<>();
        resList =realCache.scResList;
        //block b = realCache.block;
        blockBody bb = new blockBody();
        //consensusTxSet txSet=bb.getCtxset();
        Type type4 = consensusTxSet.class;
        JavaType javaType4 = objectMapper.getTypeFactory().constructType(type4);
        consensusTxSet txSet=objectMapper.readValue(realCache.ctsjson,javaType4) ;
        List<Transaction> txList = txSet.getTxlist();
        MerklePatriciaTree mpt = new MerklePatriciaTree();
        MerklePatriciaTree tmpt = new MerklePatriciaTree();
        MerklePatriciaTree accountStateMPT = new MerklePatriciaTree();
        for(int i=0;i<resList.size();i++)
        {
            String res=resList.get(i);
            System.out.println("链下执行交易hash： "+txList.get(i).getTxHash());
            mpt.inse(txList.get(i).getTxHash(),res);
            tmpt.inse(txList.get(i).getTxHash(),new ObjectMapper().writeValueAsString(txList.get(i)));
            if(res.split(";").length==4){
                String From = res.split(";")[1];
                String To = res.split(";")[3];
                ObjectMapper objectMapper2 = new ObjectMapper();
                Type type3 = Account.class;
                JavaType javaType3 = objectMapper.getTypeFactory().constructType(type2);
                Account account1 = objectMapper.readValue(From, javaType3);
                Account account2 = objectMapper.readValue(To, javaType3);
                accountStateMPT.inse(account1.getAddress(),new ObjectMapper().writeValueAsString(account1));
                accountStateMPT.inse(account2.getAddress(),new ObjectMapper().writeValueAsString(account2));
            }
        }
        bb.setTxMPT(tmpt);
        bb.setRecipientMPT(mpt);
        bb.setAccountStateMPT(accountStateMPT);
        block b=new block();
        b.setBlockbody(bb);
        blockHeader bh =new blockHeader();
        hashCreat hc=new hashCreat();
        bh.setBlockBodyHash(hc.hashSHA_256(new ObjectMapper().writeValueAsString(bb)));
        bh.setBlockTxSize(txSet.txlist.size());
        //将当前区块写入区块链账本
        simpleBlockchain.nowledger.put(bh.getBlockTotalHash(),b);
        //将当前区块完整hash写入区块链区块哈希暂存器，便于后续查找
        simpleBlockchain.blockHashList.add(bh.getBlockTotalHash());

        System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块高度"+simpleBlockchain.blockHashList.size());
        System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块hash"+simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));
        return "";
    }

}
