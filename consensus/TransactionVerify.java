package org.sde.cec.consensus;

import org.sde.cec.account.RSAKeyPair;
import org.sde.cec.model.Transaction;
import org.sde.cec.model.consensusTxSet;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;

public class TransactionVerify {
    public boolean sigVerify(consensusTxSet txSet, Map<String, RSAKeyPair> keyStorage) throws Exception {
        List<Transaction> tempList=txSet.getTxlist();
        for(int i=0;i<txSet.getTxlist().size();i++){
            //此处直接访问公私钥库获得公钥，理论上共识节点只能从库获得私钥，此处假设共识节点不会尝试获取账户私钥，便于实现，直接从库获取，后续可单独建立公钥库
            if(tempList.get(i).verifySignature("",tempList.get(i).getSignature(),keyStorage.get(tempList.get(i).getFromAddress()).pubKey)==false){
                System.out.println("共识准备阶段交易验证失败，失败交易序号:"+i);
                System.out.println("共识准备阶段交易验证失败，失败交易hash:"+tempList.get(i).getTxHash());

                return false;
            }


        }
        return true;
    }
}
