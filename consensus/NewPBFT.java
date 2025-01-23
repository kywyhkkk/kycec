package org.sde.cec.consensus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sde.cec.DisaggregatedStorageCompute.NeedResult;
import org.sde.cec.DisaggregatedStorageCompute.NeedThing;
import org.sde.cec.DisaggregatedStorageCompute.Result;
import org.sde.cec.DisaggregatedStorageCompute.ResultState;
import org.sde.cec.account.Account;
import org.sde.cec.account.RSAKeyPair;
import org.sde.cec.ledger.simpleBlockchain;
import org.sde.cec.model.*;
import org.sde.cec.scvm.SCBT_Dao;
import org.sde.cec.scvm.VM;
import org.sde.cec.util.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.sde.cec.account.RSAKeyStorage.*;
import static org.sde.cec.scvm.ContractCollection.scAddressList;
import static org.sde.cec.scvm.VM.scSet;
import static org.sde.cec.account.NewaccountBalance.accountBalance;
import static org.sde.cec.account.NewaccountBalance.nounce;

public class NewPBFT {
    public static String EXPER_MODEL= "simple";

    public static Map<String, Integer> newprepare_voteCollect = new HashMap<String, Integer>();
    public static Map<Integer, Integer> newnounce_voteCollect = new HashMap<Integer, Integer>();
    public static Map<String, Integer> newcommit_voteCollect = new HashMap<String, Integer>();

    public static Map<String, Integer> newsync_voteCollect = new HashMap<String, Integer>();
    public String startNewConsensus(consensusTxSet txSet) throws Exception {
        int totalTxNumber=txSet.getTxlist().size();
        if(EXPER_MODEL== "simple"){

//            if(tv.sigVerify(txSet,keyStorage)==false){
//                System.out.println("测试模式，忽略验签失败");
//                //return "txSigVerify_false";
//            }
            //txSet里边有交易列表

            blockHeader bh=new blockHeader();
            blockBody bb=new blockBody();
            hashCreat hc=new hashCreat();
            //
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
            //System.out.println(1);
            //需要传给存储节点的需要账户
            //System.out.println("txSet.txlist的交易数量"+txSet.txlist.size());
            //从这里判断需要什么类型的，或者这里不判断，直接把交易
            //类型需要有一个对照表
            //List<String> needthing=Needthing(txSet.txlist);
            NeedThing needThing=new NeedThing();
            System.out.println("输入saart里边nounce"+nounce);
            needThing.setNounce(nounce);
            nounce=nounce+1;
            System.out.println("nounce+1"+nounce);
            needThing.setB(b);
            //needThing.setAddresses(needthing);
            //附加信息为account表
            // 方法一 动态创建与 size 相同的数组，性能最好
            //String[] array = needthing.toArray(new String[0]);
            // listStrings.toArray(new String[listStrings.size()]);
            //System.out.println(Arrays.toString(array));
            //txSet.setAppend(Joiner.on(",").join(needthing));
            P2PMessage pm2=new P2PMessage();
            pm2.setNetAddress(NodeList.local);
            pm2.setMessageType("needsomething");
            Date currentDate = new Date();  // 获取当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
            String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
            pm2.setMessageTime(formattedDate.toString());
            pm2.setSig(NodeList.localsig);
            pm2.setMessage(new ObjectMapper().writeValueAsString(needThing));
            P2PBroadcasting p2pb=new P2PBroadcasting();
            //最后一个做为存储节点
            //主节点将需要什么发送给存储节点

            System.out.println(p2pb.springbootRpcSend(pm2,NodeList.nodelist.get(NodeList.nodelist.size()-1)));
        }
        return "";
    }
    //此时是给账户的地址
    //函数判断需要什么数据,根据绑定的地址，那needthing可以就传输绑定的地址，然后存储节点自己判断需要传回什么状态
    public List<String> Needthing(List<Transaction> transactions){
        List<String> needthing=new ArrayList<>();
        for(Transaction t:transactions){
            //System.out.println("输出t.bindContractAddress"+t.bindContractAddress);
            //假如是第二个地址，则是放置fromaddress和toaddress
            if (t.bindContractAddress==scAddressList.get(2)){
                if(!needthing.contains(t.fromAddress)){
                    needthing.add(t.fromAddress);
                }
                if(!needthing.contains(t.toAddress)){
                    needthing.add(t.toAddress);
                }
            }
        }
        return needthing;
    }
    //修改缓存
    public String Cachebalance(String P2PMessage) throws IOException {
        System.out.println("修改缓存");
        NeedResult needResult=new NeedResult();
        ObjectMapper objectMapper = new ObjectMapper();
        String message=P2PMessage;
        Type type1 = P2PMessage.class;
        JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
        P2PMessage pMessage=objectMapper.readValue(message, javaType1);
        String aaa=pMessage.getMessage();
        Type type2= NeedResult.class;
        JavaType javaType2 = objectMapper.getTypeFactory().constructType(type2);
        needResult=objectMapper.readValue(aaa, javaType2);
        //List<Transaction> txslist=needResult.getB().getBlockbody().getCtxset().txlist;
        for(int i=0;i<needResult.getNeedresults().size();i++){
            Map<String, String> resultMap = objectMapper.readValue(needResult.getNeedresults().get(String.valueOf(i)), HashMap.class);
            String fromStateTotal=resultMap.get("from");
            Account fromAccount = objectMapper.readValue(fromStateTotal, Account.class);
            String toStateTotal=resultMap.get("to");
            Account toAccount = objectMapper.readValue(toStateTotal, Account.class);
            //替换一下现有的Account
            accountList.put(fromAccount.getAddress(),fromAccount);
            accountList.put(toAccount.getAddress(),toAccount);

        }
        //nounce=needResult.getNounce();
        P2PBroadcasting p2pb=new P2PBroadcasting();
        P2PMessage pm=new P2PMessage();
        pm.setNetAddress(NodeList.local);
        pm.setMessageType("newsyncnounce");
        Date currentDate = new Date();  // 获取当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
        String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
        pm.setMessageTime(formattedDate.toString());
        pm.setSig(NodeList.localsig);
        pm.setMessage(new ObjectMapper().writeValueAsString(needResult));
        //P2PBroadcasting p2pb=new P2PBroadcasting();
        System.out.println(p2pb.springbootRpcSend(pm,NodeList.nodelist.get(0)));
        return "";
    }
    public String Newpre_Prepare(String P2PMessage) throws IOException, NoSuchAlgorithmException {
        System.out.println("进入newpre_prepare");
        if(EXPER_MODEL== "simple"){
            NeedResult needResult=new NeedResult();
            //System.out.println("收到pre_prepare消息，广播prepare消息");
            //NeedResult needResult=new NeedResult();
            ObjectMapper objectMapper = new ObjectMapper();
            String message=P2PMessage;
            Type type1 = P2PMessage.class;
            JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
            P2PMessage pMessage=objectMapper.readValue(message, javaType1);
            String aaa= pMessage.getMessage();
            Type type2= NeedResult.class;
            JavaType javaType2 = objectMapper.getTypeFactory().constructType(type2);
            needResult=objectMapper.readValue(aaa, javaType2);
            newnounce_voteCollect.putIfAbsent(needResult.getNounce(), 1);//默认自己投自己一票
            int j=newnounce_voteCollect.get(needResult.getNounce())+1;  //收集prepare投票
            newnounce_voteCollect.put(needResult.getNounce(),j);
            int voteNum = newnounce_voteCollect.get(needResult.getNounce());
            System.out.println("第"+needResult.getNounce()+"个投票数"+voteNum);
            int limitnum=4;
            if (limitnum==voteNum){


//            Type type2= nounce;
//            JavaType javaType2 = objectMapper.getTypeFactory().constructType(type2);
//            needResult=objectMapper.readValue(aaa, javaType2);
//            nounce=needResult.getNounce();
            //设置缓存
//            System.out.println("设置缓存");
//            Map<String, Integer> needbalances = needResult.getNeedresults();
//            System.out.println("result大小"+needbalances.size());
//            for (Map.Entry<String, Integer> entry : needbalances.entrySet()) {
//                accountBalance.put(entry.getKey(),entry.getValue());
//                System.out.println("key:"+entry.getKey());
//                System.out.println("value:"+entry.getValue());
//            }
            //收到了来自存储节点的结果，接下来要打包区块 执行交易
            consensusTxSet consensusTxSet1=needResult.getB().getBlockbody().getCtxset();
            List<Transaction> transactions=consensusTxSet1.getTxlist();
                System.out.println("tran的数量"+transactions.size());
            TransactionDependency transactionDependency=new TransactionDependency();
            transactionDependency=transactionDependency.createTransactionDependency(transactions);
            //执行交易
            String result=TransactionExcute(transactionDependency,3);
            System.out.println("执行结束");
            //交易执行完毕需要将交易执行结果构造成一个东西放进区块——
            blockHeader bh=new blockHeader();
            blockBody bb=new blockBody();
            hashCreat hc=new hashCreat();
            bb.setCtxset(consensusTxSet1);
            bb.setReceipt(result);
            bh.setBlockBodyHash(hc.hashSHA_256(new ObjectMapper().writeValueAsString(bb)));
            bh.setBlockTxSize(transactions.size());
            ///设置父区块
            if(!simpleBlockchain.blockHashList.isEmpty()){
                bh.setPre_blockTotalHash(simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));
            }else {
                bh.setPre_blockTotalHash("Genesis");
            }
            bh.setBlockTotalHash(hc.hashSHA_256(new ObjectMapper().writeValueAsString(bh)));
            block b=new block();
            b.setNouncee(nounce);
            b.setBlockbody(bb);
            b.setBlockheader(bh);
            P2PMessage pm=new P2PMessage();
            pm.setNetAddress(NodeList.local);
            pm.setMessageType("newprepare");
            Date currentDate = new Date();  // 获取当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
            String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
            pm.setMessageTime(formattedDate.toString());
            pm.setSig(NodeList.localsig);
            pm.setMessage(new ObjectMapper().writeValueAsString(b));
            P2PBroadcasting p2pb=new P2PBroadcasting();
            List<String> nodeaddresses=new ArrayList<>();
            for(int i = 0; i < NodeList.nodelist.size()-1; i++){
                nodeaddresses.add( NodeList.nodelist.get(i));
            }
            //发送给其他执行节点
            System.out.println(p2pb.springbootRpcBroadcastingzhi(pm,nodeaddresses));
        }}
        return "";
    }

    public String TransactionExcute(TransactionDependency transactionDependency,int n){
        List<TransactionDependency.SubDependency> subDependencies=transactionDependency.subDependencies;
        //交易总数量
        System.out.println("sub数量为"+subDependencies.size());
        List<List<Transaction>> transactiongroups=new ArrayList<>();
        //Collections.sort(subDependencies,Comparator.comparingInt(TransactionDependency.SubDependency -> TransactionDependency.SubDependency.getTransactions().size());
        //从大到小排序

        subDependencies.sort(Comparator.comparingInt(subDependency -> subDependency.getTransactions().size()));
        int k=0;
        for(TransactionDependency.SubDependency subDependency:subDependencies){
            if(transactiongroups.size()<n){
                //transactiongroups.add()
                transactiongroups.add(subDependency.getTransactions());
            }else{
                System.out.println("第"+k+"ge");
                int minSize=transactiongroups.get(0).size();
                int minIdx=0;
                System.out.println("第0组交易数量"+minSize);
                //已经有了这么多分组，所以我们放在当前交易数量最小的组
                for(int i=1;i<n;i++){
                    System.out.println("第"+i+"组交易数量"+transactiongroups.get(i).size());
                    if(transactiongroups.get(i).size()<minSize){
                        minIdx=i;
                        minSize=transactiongroups.get(i).size();
                    }
                }
                System.out.println("将"+k+"放入组"+minIdx+minSize);
                System.out.println("放入的交易数量为"+transactiongroups.get(minIdx).size());
                System.out.println("放之前"+minIdx+"交易数量"+transactiongroups.get(minIdx).size());
                List<Transaction> tt =transactiongroups.get(minIdx);
                tt.addAll(subDependency.getTransactions());
                transactiongroups.set(minIdx,tt);
                System.out.println("放之hou"+minIdx+"交易数量"+transactiongroups.get(minIdx).size());
            }
            k++;
        }
        System.out.println("输出groups的数量");
        System.out.println(transactiongroups.get(0).size());
        System.out.println(transactiongroups.get(1).size());
        System.out.println(transactiongroups.get(2).size());
        //此时将交易划分了，需要开始多线程执行
        final String[] receipt=new String[181];
        ExecutorService executor = Executors.newFixedThreadPool(n);

        for(int i=0;i<3;i++){
            final int finalI = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    VM vm=new VM();
                    //scSet.put("0xtest","SC_BalanceAndStateTransfers");
                    scSet.put(scAddressList.get(2),"SC_BalanceTransfers");
                    System.out.println("在执行交易组"+finalI);
                    for(Transaction tran: transactiongroups.get(finalI)){
                        System.out.println("在执行交易"+tran.txid);
                        //构造合约入参，此处入参的最终构造结果理论上存储在交易数据字段内
                        SCBT_Dao dao=new SCBT_Dao();
                        dao.setFrom(tran.fromAddress);
                        dao.setTo(tran.toAddress);
                        dao.setTransferAmount(Integer.parseInt(tran.txData));
                        //System.out.println("输出"+tran.fromAddress);
                        //System.out.println("输出"+tran.fromAddress+"de"+accountBalance.get(tran.fromAddress));
                        dao.setFromAccount(accountList.get(tran.fromAddress));
                        dao.setToAccount(accountList.get(tran.toAddress));
//                        dao.setFrombalance(accountBalance.get(tran.fromAddress));
//                        dao.setTobalance(accountBalance.get(tran.toAddress));
                        try {
                            receipt[Integer.parseInt(tran.txid)]=vm.scExecute(scAddressList.get(2),"SC_BalanceTransfers",new ObjectMapper().writeValueAsString(dao));
                            //receipt[tran.txid] = receipt[finalI] +vm.scExecute(scAddressList.get(2),"SC_BalanceTransfers",new ObjectMapper().writeValueAsString(dao));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
//            executor.execute(() -> {
//                // 执行具体的任务逻辑
//
//            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // 处理中断异常
            e.printStackTrace();
        }
        for (int i=0;i<180;i++){
            System.out.println("receipt[]"+i+receipt[i]);
        }
        String rece=Arrays.toString(receipt);
        return rece;
    }


    public String NewPrepare(P2PMessage pm) throws IOException {
        System.out.println("处理NewPrepare");
        if(EXPER_MODEL== "simple"){
            ObjectMapper objectMapper = new ObjectMapper();
            String message=pm.getMessage();
            Type type1 = block.class;
            JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
            block b = objectMapper.readValue(message, javaType1);
            blockHeader bh = b.getBlockheader();
            newprepare_voteCollect.putIfAbsent(bh.getBlockTotalHash(), 1);//默认自己投自己一票
            //判断是否已经收到最新状态
            //假如已经收到了最新状态
            System.out.println("b.getNouncee()"+b.getNouncee());
            //System.out.println(nounce);
            //if(b.getNouncee()==nounce) {
                consensusTxSet consensusTxSet1 = b.getBlockbody().getCtxset();
                List<Transaction> transactions = consensusTxSet1.getTxlist();
                System.out.println("tranaa" + transactions.size());
                TransactionDependency transactionDependency = new TransactionDependency();
                transactionDependency = transactionDependency.createTransactionDependency(transactions);
                //执行交易
                String result = TransactionExcute(transactionDependency, 3);
                System.out.println("result" + result);
                System.out.println("b.getBlockbody().getReceipt()" + b.getBlockbody().getReceipt());
                //执行结果相同，通过验证
                //投票
                int i = newprepare_voteCollect.get(bh.getBlockTotalHash()) + 1;  //收集prepare投票
                newprepare_voteCollect.put(bh.getBlockTotalHash(), i);
                int voteNum = newprepare_voteCollect.get(bh.getBlockTotalHash());
                int limit = 2; // 2f+1
                System.out.println("当前prepare投票数量为" + voteNum);
                if (voteNum == limit) {
                    if (result.equals(b.getBlockbody().getReceipt())) {
                        P2PMessage pm2 = new P2PMessage();
                    pm2.setNetAddress(NodeList.local);
                    pm2.setMessageType("newcommit");
                    Date currentDate = new Date();  // 获取当前时间
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
                    String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
                    pm2.setMessageTime(formattedDate.toString());
                    pm2.setSig(NodeList.localsig);
                    pm2.setMessage(pm.getMessage());
                    P2PBroadcasting p2pb = new P2PBroadcasting();
                        //springbootRpcBroadcastingzhi(P2PMessage pm, List<String> nodeaddresses)
                    List<String> nodeaddresses=new ArrayList<>();
                        for (int l=0;l<NodeList.nodelist.size()-1;l++){
                            nodeaddresses.add(NodeList.nodelist.get(l));
                        }
                    System.out.println(p2pb.springbootRpcBroadcastingzhi(pm2,nodeaddresses));
                }
                    else{
                        System.out.println("执行结果不同，不通过验证");
                    }
            }
               // }
            }

        return "";
    }
    public static  void main(String args[]) throws Exception {
        //测试交易执行
        for(int i=0;i<100;i++){
            CryptographyUtil cu=new CryptographyUtil();
            hashCreat hc=new hashCreat();
            KeyPair tempKey=cu.RSAKeyConstruction(1024);
//            String pubKey=Base64.getEncoder().encodeToString(tempKey.getPublic().getEncoded());
//            String priKey=Base64.getEncoder().encodeToString(tempKey.getPrivate().getEncoded());
//            RSAKeyBase64 rb64=new RSAKeyBase64();
//            rb64.priKey=priKey;
//            rb64.pubKey=pubKey;
            String accountAddress= hc.hashSHA_256(tempKey.getPublic().toString());
            //System.out.println(accountAddress);
            accountAddressList.add(accountAddress);
            RSAKeyPair rkp=new RSAKeyPair();
            rkp.pubKey=tempKey.getPublic();
            rkp.priKey=tempKey.getPrivate();
            keyStorage.put(accountAddress,rkp);
            accountBalance.put(accountAddress,100);
        }
        TransactionGenerator transactionGenerator=new TransactionGenerator();
        transactionGenerator.generateTransactions();
        transactionGenerator.writeTransactionsToFile("transaction.txt");
        List<Transaction> transactions=transactionGenerator.readTransactionsFromFile("transaction.txt");
        //consensusTxSet cts=new consensusTxSet();
        //cts.setTxlistTxlist(transactions);
        TransactionDependency transactionDependency=new TransactionDependency();
        transactionDependency=transactionDependency.createTransactionDependency(transactions);
        int sum=0;
        for(TransactionDependency.SubDependency subDependency:transactionDependency.subDependencies){
            int i=subDependency.getTransactions().size();
            System.out.println("i"+i);
            sum=sum+i;
        }
        System.out.println("总数量："+sum);
        //执行交易
        //NewPBFT newPBFT=new NewPBFT();
        //String result=newPBFT.TransactionExcute(transactionDependency,3);
        System.out.println("执行结束");
    }
    public String NewCommit(P2PMessage pm) throws IOException {
        if(EXPER_MODEL== "simple"){
            ObjectMapper objectMapper = new ObjectMapper();
            String message=pm.getMessage();
            Type type1 = block.class;
            JavaType javaType1 = objectMapper.getTypeFactory().constructType(type1);
            block b = objectMapper.readValue(message, javaType1);
            blockHeader bh = b.getBlockheader();
            newcommit_voteCollect.putIfAbsent(bh.getBlockTotalHash(), 1);//默认自己投自己一票
            int i=newcommit_voteCollect.get(bh.getBlockTotalHash())+1;  //收集commit投票
            newcommit_voteCollect.put(bh.getBlockTotalHash(),i);
            int voteNum = newcommit_voteCollect.get(bh.getBlockTotalHash());
            System.out.println("当前commit投票数量为"+voteNum);
            int limit = 3; // 2f+1
            if (voteNum==limit){
                //需要将当前区块的内容发送给存储节点,获取account和其对应的值
                simpleBlockchain.nowledger.put(bh.getBlockTotalHash(),b);
                //将当前区块完整hash写入区块链区块哈希暂存器，便于后续查找
                simpleBlockchain.blockHashList.add(bh.getBlockTotalHash());
                System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块高度"+simpleBlockchain.blockHashList.size());
                System.out.println("从节点"+NodeList.local+"区块上链 当前最新区块hash"+simpleBlockchain.blockHashList.get(simpleBlockchain.blockHashList.size()-1));
                //String str[] = b.getBlockbody().getCtxset().getAppend().split(",");
                //String receipt=b.getBlockbody().getReceipt();

                //应该要按照交易类型生成一下结果
                List<Transaction> txslist=b.getBlockbody().getCtxset().getTxlist();
                ResultState resultState=new ResultState();
                Map<String, String> results=new HashMap<>();
                for(int j=0;j<txslist.size();j++){
                    Transaction tx=txslist.get(i);
                    if(tx.bindContractAddress==scAddressList.get(2)){
                        List<String> reallist=new ArrayList<>();
                        reallist.add("accountList");
                        String list=new ObjectMapper().writeValueAsString(reallist);
                        String result=resultState.getResultState(list,tx.getFromAddress(),tx.getToAddress(),null,null,null,null);

                        results.put(String.valueOf(j),result);
                    }
                }


                //List<String> accountaddresses= Arrays.asList(str);
                //Map<String, Integer> accountBalancees=new HashMap<>();
//                for (String address : accountaddresses) {
//                    // Perform operations with 'address'
//                    //System.out.println(address);
//                    accountBalancees.put(address,accountBalance.get(address));
//                    //System.out.println("Address: " + address + ", Balance: " + accountBalance.get(address));
//                }
                Result result=new Result();
                result.setNeedresults(results);
                result.setB(b);
                result.setNounce(b.getNouncee());
                result.setList(txslist);
                P2PMessage pm2=new P2PMessage();
                pm2.setNetAddress(NodeList.local);
                pm2.setMessageType("newsync");
                Date currentDate = new Date();  // 获取当前时间
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");  // 指定日期格式
                String formattedDate = sdf.format(currentDate);  // 格式化日期为指定格式
                pm2.setMessageTime(formattedDate.toString());
                pm2.setSig(NodeList.localsig);
                pm2.setMessage(new ObjectMapper().writeValueAsString(result));
                P2PBroadcasting p2pb=new P2PBroadcasting();
                //将消息发送给存储节点
                System.out.println(p2pb.springbootRpcSend(pm2,NodeList.nodelist.get(NodeList.nodelist.size()-1)));
            }
        }

        return "";
    }
}
