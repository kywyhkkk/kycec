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
import org.sde.cec.util.P2PBroadcasting;
import org.sde.cec.util.hashCreat;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class PBFT {
    //pbft实验模式，简易模式simple：不对交易做合法性验证，直接进行四阶段共识，在共识过程中由合约做验证
    //严格模式trictness：按照实验制定验证方法，由主节点对交易做验证，示例提供的是对交易签名进行验证(待实现)
    public static String EXPER_MODEL= "simple";

    public String txs="{}";
    public static Map<String, Integer> prepare_voteCollect = new HashMap<String, Integer>();

    public static Map<String, Integer> commit_voteCollect = new HashMap<String, Integer>();

    public static Map<String, Integer> sync_voteCollect = new HashMap<String, Integer>();

    public String startConsensus(consensusTxSet txSet, Map<String, RSAKeyPair> keyStorage) throws Exception {
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
            block b=new block();
            b.setBlockbody(bb);
            b.setBlockheader(bh);
            //System.out.println("2");
            P2PMessage pm=new P2PMessage();
            pm.setNetAddress(NodeList.local);
            //System.out.println("3");
            pm.setMessageType("pre_prepare");
            Date currentDate = new Date();  // 获取当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
            String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
            pm.setMessageTime(formattedDate.toString());
            pm.setSig(NodeList.localsig);
            pm.setMessage(new ObjectMapper().writeValueAsString(b));
            P2PBroadcasting p2pb=new P2PBroadcasting();
            System.out.println(p2pb.springbootRpcBroadcasting(pm));
        }
        return "";
    }

    public String pre_Prepare(P2PMessage pm) throws IOException {
        if(EXPER_MODEL== "simple"){
            //System.out.println("收到pre_prepare消息，广播prepare消息");
            P2PMessage pm2=new P2PMessage();
            pm2.setNetAddress(NodeList.local);
            pm2.setMessageType("prepare");
            Date currentDate = new Date();  // 获取当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
            String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
            pm2.setMessageTime(formattedDate.toString());
            pm2.setSig(NodeList.localsig);
            pm2.setMessage(pm.getMessage());
            P2PBroadcasting p2pb=new P2PBroadcasting();
            System.out.println(p2pb.springbootRpcBroadcasting(pm2));
        }
        return "";
    }

    public String Prepare(P2PMessage pm) throws IOException {
        if(EXPER_MODEL== "simple"){
            ObjectMapper objectMapper = new ObjectMapper();
            String message=pm.getMessage();
            Type type1 = block.class;
            JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
            //System.out.println(message);
            block b = objectMapper.readValue(message, javaType1);
            blockHeader bh = b.getBlockheader();
            prepare_voteCollect.putIfAbsent(bh.getBlockTotalHash(), 1);//默认自己投自己一票
            int i=prepare_voteCollect.get(bh.getBlockTotalHash())+1;  //收集prepare投票
            prepare_voteCollect.put(bh.getBlockTotalHash(),i);


            int voteNum = prepare_voteCollect.get(bh.getBlockTotalHash());
            int limit = 3; // 2f+1
            System.out.println("当前prepare投票数量为"+voteNum);
            if (voteNum==limit){
                P2PMessage pm2=new P2PMessage();
                pm2.setNetAddress(NodeList.local);
                pm2.setMessageType("commit");
                Date currentDate = new Date();  // 获取当前时间
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
                String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
                pm2.setMessageTime(formattedDate.toString());
                pm2.setSig(NodeList.localsig);
                pm2.setMessage(pm.getMessage());
                P2PBroadcasting p2pb=new P2PBroadcasting();
                System.out.println(p2pb.springbootRpcBroadcasting(pm2));
            }
        }
        return "";
    }

    public String Commit(P2PMessage pm) throws Exception {
        if(EXPER_MODEL== "simple"){
            ObjectMapper objectMapper = new ObjectMapper();
            String message=pm.getMessage();
            Type type1 = block.class;
            JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
            block b = objectMapper.readValue(message, javaType1);
            blockHeader bh = b.getBlockheader();
            blockBody bb=b.getBlockbody();
            //bb.setAccountStateMPT(simpleBlockchain.nowledger.get(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1)).getBlockbody().getAccountStateMPT());
            consensusTxSet txSet=bb.getCtxset();
            commit_voteCollect.putIfAbsent(bh.getBlockTotalHash(), 1);//默认自己投自己一票
            int num=commit_voteCollect.get(bh.getBlockTotalHash())+1;  //收集commit投票
            commit_voteCollect.put(bh.getBlockTotalHash(),num);
            int voteNum = commit_voteCollect.get(bh.getBlockTotalHash());
            System.out.println("当前commit投票数量为"+voteNum);
            int limit = 3; // 2f+1

            if (voteNum==limit){
                MerklePatriciaTree mpt = new MerklePatriciaTree();
                hashCreat hc=new hashCreat();
                VM vm=new VM();
                List<Transaction> txList = txSet.getTxlist();
                MerklePatriciaTree accountStateMPT = new MerklePatriciaTree();
                MerklePatriciaTree tmpt = new MerklePatriciaTree();
                if(!simpleBlockchain.blockHashList.isEmpty()){
                    if(simpleBlockchain.nowledger.get(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1)).getBlockbody().getAccountStateMPT()==null){

                    }else{
                        accountStateMPT =simpleBlockchain.nowledger.get(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1)).getBlockbody().getAccountStateMPT();
                    }

                }

                for(int i=0;i<txList.size();i++){
                    try{
                        //解析交易，打包需要同步到链下的数据

                        //利用p2p发送

                        //此处不做任何合约执行失败的判断
                        String res =  vm.scExecute(txList.get(i).getBindContractAddress(),txList.get(i).getBindContractFunction(),txList.get(i).getTxData());
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
                        System.out.println(e.getMessage());
                        System.out.println("测试模式，忽略合约执行失败，成功与否均直接入块");
                    }
                }

                bb.setTxMPT(tmpt);
                bb.setRecipientMPT(mpt);
                bb.setAccountStateMPT(accountStateMPT);
                b.setBlockbody(bb);
                //将当前区块写入区块链账本
                simpleBlockchain.nowledger.put(bh.getBlockTotalHash(),b);
                //将当前区块完整hash写入区块链区块哈希暂存器，便于后续查找
                simpleBlockchain.blockHashList.add(bh.getBlockTotalHash());

                System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块高度"+simpleBlockchain.blockHashList.size());
                System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块hash"+simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));

                P2PMessage pm2=new P2PMessage();
                pm2.setNetAddress(NodeList.local);
                pm2.setMessageType("sync");
                Date currentDate = new Date();  // 获取当前时间
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
                String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
                pm2.setMessageTime(formattedDate.toString());
                pm2.setSig(NodeList.localsig);
                pm2.setMessage(pm.getMessage());
                P2PBroadcasting p2pb=new P2PBroadcasting();
                System.out.println(p2pb.springbootRpcBroadcasting(pm2));
            }
        }
        return "";
    }

    public String Sync(P2PMessage pm) throws IOException {
        if(EXPER_MODEL== "simple"){
            ObjectMapper objectMapper = new ObjectMapper();
            String message=pm.getMessage();
            Type type1 = block.class;
            JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
            block b = objectMapper.readValue(message, javaType1);
            blockHeader bh = b.getBlockheader();
            sync_voteCollect.putIfAbsent(bh.getBlockTotalHash(), 1);//默认自己投自己一票
            int i=sync_voteCollect.get(bh.getBlockTotalHash())+1;
            sync_voteCollect.put(bh.getBlockTotalHash(),i);


            int voteNum = sync_voteCollect.get(bh.getBlockTotalHash());
            int limit = 2; // 2f+1
            if (voteNum==limit){
                if(bh.getBlockTotalHash().equals(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1))){
                    System.out.println("上链同步");
                } else {
                    simpleBlockchain.nowledger.put(bh.getBlockTotalHash(),b);
                    simpleBlockchain.blockHashList.set(simpleBlockchain.blockHashList.size()-1,bh.getBlockTotalHash());
                }

            }

        }
        return "";
    }
}
