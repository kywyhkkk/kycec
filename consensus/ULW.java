package org.sde.cec.consensus;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.util.RamUsageEstimator;
import org.sde.cec.account.Account;
import org.sde.cec.account.RSAKeyPair;
import org.sde.cec.account.RSAKeyStorage;
import org.sde.cec.basicModel.MerklePatriciaTree;
import org.sde.cec.ledger.simpleBlockchain;
import org.sde.cec.model.*;
import org.sde.cec.scvm.VM;
import org.sde.cec.util.NodeList;
import org.sde.cec.util.P2PBroadcasting;
import org.sde.cec.util.hashCreat;

import java.lang.reflect.Type;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public  class ULW {
    //UltraLightWeight共识机制，主节点发包直接共识通过，单阶段共识模式
    //适用于不需要考虑共识性能的实验

    public static Map<String, Integer> vote= new HashMap<String, Integer>();
    public String ULWLeader(consensusTxSet txSet, Map<String, RSAKeyPair> keyStorage) throws Exception {
        TransactionVerify tv=new TransactionVerify();
        ObjectMapper objectMapper = new ObjectMapper();
        int totalTxNumber=txSet.getTxlist().size();
        MerklePatriciaTree mpt = new MerklePatriciaTree();
        MerklePatriciaTree accountStateMPT = new MerklePatriciaTree();
        MerklePatriciaTree tmpt = new MerklePatriciaTree();
        if(!simpleBlockchain.blockHashList.isEmpty()){
            accountStateMPT = simpleBlockchain.nowledger.get(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1)).getBlockbody().getAccountStateMPT();
        }
        VM vm=new VM();
        try{
            if(tv.sigVerify(txSet,keyStorage)==false){
                System.out.println("测试模式，忽略验签失败");
                //return "txSigVerify_false";
            }
        }catch(NullPointerException e){
            System.out.println("测试模式，忽略验签exception");
        }
        List<Transaction> txList = txSet.getTxlist();
        for(int i=0;i<txList.size();i++){
            try{
                //此处不做任何合约执行失败的判断
                String res=vm.scExecute(txList.get(i).getBindContractAddress(),txList.get(i).getBindContractFunction(),txList.get(i).getTxData());
                System.out.println("合约执行结果："+res);
                mpt.inse(txList.get(i).getTxHash(),res);
                tmpt.inse(txList.get(i).getTxHash(),new ObjectMapper().writeValueAsString(txList.get(i)));
                if(res.split(";").length==2){
                    String From = res.split(";")[0];
                    String To = res.split(";")[1];
                    ObjectMapper objectMapper2 = new ObjectMapper();
                    Type type2 = Account.class;
                    JavaType javaType2 = objectMapper.getTypeFactory().constructType(type2);
                    Account account1 = objectMapper.readValue(From, javaType2);
                    Account account2 = objectMapper.readValue(To, javaType2);
                    accountStateMPT.inse(account1.getAddress(),new ObjectMapper().writeValueAsString(account1));
                    accountStateMPT.inse(account2.getAddress(),new ObjectMapper().writeValueAsString(account2));
                    //更新两个账户快速索引交易的缓存
                    RSAKeyStorage.updateAcountTxs(account1.getAddress(),txList.get(i).txHash,"from");
                    RSAKeyStorage.updateAcountTxs(account2.getAddress(),txList.get(i).txHash,"to");
                }
            }catch (Exception e){
                System.out.println("测试模式，忽略合约执行失败，成功与否均直接入块");
            }
        }

        blockHeader bh=new blockHeader();
        blockBody bb=new blockBody();
        hashCreat hc=new hashCreat();
        bb.setCtxset(txSet);
        bh.setBlockBodyHash(hc.hashSHA_256(new ObjectMapper().writeValueAsString(bb)));
        bh.setBlockTxSize(totalTxNumber);
        long sizeInBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        bh.setBlockSize((int) sizeInBytes);
        if(simpleBlockchain.blockHashList.size()>0){
            bh.setPre_blockTotalHash(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));
        }else {
            bh.setPre_blockTotalHash("Genesis");
        }

        bh.setBlockTotalHash(hc.hashSHA_256(new ObjectMapper().writeValueAsString(bh)));
        block b=new block();
        b.setBlockbody(bb);
        b.setBlockheader(bh);
        P2PMessage pm=new P2PMessage();
        pm.setNetAddress(NodeList.local);
        pm.setMessageType("ulwleader");
        Date currentDate = new Date();  // 获取当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
        String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
        pm.setMessageTime(formattedDate.toString());
        pm.setSig(NodeList.localsig);
        pm.setMessage(new ObjectMapper().writeValueAsString(b));
        P2PBroadcasting p2pb=new P2PBroadcasting();
        System.out.println(p2pb.springbootRpcBroadcasting(pm));
        //将当前区块写入区块链账本
        simpleBlockchain.nowledger.put(bh.getBlockTotalHash(),b);
        //将当前区块完整hash写入区块链区块哈希暂存器，便于后续查找
        simpleBlockchain.blockHashList.add(bh.getBlockTotalHash());
        if(vote.get(bh.getBlockTotalHash())==null){
            vote.put(bh.getBlockTotalHash(),1);//默认自己投自己一票
        }else{
            int i=vote.get(bh.getBlockTotalHash())+1;
            vote.put(bh.getBlockTotalHash(),i);
        }
        System.out.println("主节点区块上链 当前最新区块高度"+simpleBlockchain.blockHashList.size());
        System.out.println("主节点区块上链 当前最新区块hash"+simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));
        return "ULWLeader_Finish";
    }
    public String ULWSlave(block b) throws Exception{
        VM vm=new VM();
        ObjectMapper objectMapper = new ObjectMapper();
        blockHeader bh=b.getBlockheader();
        //将当前区块写入区块链账本
        simpleBlockchain.nowledger.put(bh.getBlockTotalHash(),b);
        //将当前区块完整hash写入区块链区块哈希暂存器，便于后续查找
        simpleBlockchain.blockHashList.add(bh.getBlockTotalHash());
        System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块高度"+simpleBlockchain.blockHashList.size());
        System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块hash"+simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));
        blockBody bb=b.getBlockbody();
        //从节点执行交易以同步状态
        consensusTxSet txSet=bb.getCtxset();
        MerklePatriciaTree mpt = new MerklePatriciaTree();
        MerklePatriciaTree accountStateMPT = new MerklePatriciaTree();
        MerklePatriciaTree tmpt = new MerklePatriciaTree();
        List<Transaction> txList = txSet.getTxlist();
        for(int i=0;i<txList.size();i++){
            try{
                //此处不做任何合约执行失败的判断
                String res=vm.scExecute(txList.get(i).getBindContractAddress(),txList.get(i).getBindContractFunction(),txList.get(i).getTxData());
                System.out.println("合约执行结果："+res);
                mpt.inse(txList.get(i).getTxHash(),res);
                tmpt.inse(txList.get(i).getTxHash(),new ObjectMapper().writeValueAsString(txList.get(i)));
                if(res.split(";").length==2) {
                    String From = res.split(";")[0];
                    String To = res.split(";")[1];
                    ObjectMapper objectMapper2 = new ObjectMapper();
                    Type type2 = Account.class;
                    JavaType javaType2 = objectMapper.getTypeFactory().constructType(type2);
                    Account account1 = objectMapper.readValue(From, javaType2);
                    Account account2 = objectMapper.readValue(To, javaType2);
                    accountStateMPT.inse(account1.getAddress(), new ObjectMapper().writeValueAsString(account1));
                    accountStateMPT.inse(account2.getAddress(), new ObjectMapper().writeValueAsString(account2));
                    //更新两个账户快速索引交易的缓存
                    RSAKeyStorage.updateAcountTxs(account1.getAddress(), txList.get(i).txHash, "from");
                    RSAKeyStorage.updateAcountTxs(account2.getAddress(), txList.get(i).txHash, "to");
                }
            }catch (Exception e){
                System.out.println("测试模式，忽略合约执行失败，成功与否均直接入块");
            }
        }
        try{
            if(vote.get(bh.getBlockTotalHash())==null){
                vote.put(bh.getBlockTotalHash(),1);//默认自己投自己一票
            }else{
                int i=vote.get(bh.getBlockTotalHash())+1;
                vote.put(bh.getBlockTotalHash(),i);
            }
        }catch (NullPointerException e){
            System.out.println("nullpointer");
            vote.put(bh.getBlockTotalHash(),1);
        }
        System.out.println("投票数量："+vote.get(bh.getBlockTotalHash()));
        P2PMessage pm=new P2PMessage();

        pm.setMessageType("ulwvote");
        Date currentDate = new Date();  // 获取当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
        String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
        pm.setMessageTime(formattedDate.toString());
        pm.setSig(NodeList.localsig);
        pm.setMessage(bh.getBlockTotalHash());
        pm.setNetAddress(NodeList.local);
        P2PBroadcasting p2pb=new P2PBroadcasting();
        System.out.println(p2pb.springbootRpcBroadcasting(pm));
        currentDate = new Date();  // 获取当前时间
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
        formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
        System.out.println(formattedDate);
        return "ULWSlave_Finish";
    }
    //该投票方法不影响区块上链，仅作为静态java变量实现全局非初始化计票的技术验证
    public String ULWVote(String blockHash) throws Exception{
        System.out.println("收到新的对区块"+blockHash+"的投票");
        if(vote.get(blockHash)==null){
            vote.put(blockHash,1);
        }else{
            int i=vote.get(blockHash)+1;
            vote.put(blockHash,i);
        }
        System.out.println("投票数量："+vote.get(blockHash));
        return "voteRecive_finish";
    }
}
