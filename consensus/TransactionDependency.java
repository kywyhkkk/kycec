package org.sde.cec.consensus;

import org.sde.cec.model.Transaction;

import java.util.*;

public class TransactionDependency {
    //Map<String, SumDependency> dependencyMap;


    //class SumDependency {
    public List<SubDependency> subDependencies;
    //}

    public class SubDependency {
        //String subid;
        List<Transaction> transactions;
        List<String> addresses=new ArrayList<>();
        Graph graph;
        public SubDependency() {
            this.transactions = new ArrayList<>();
            this.addresses = new ArrayList<>();
            this.graph = new Graph();
        }
        public SubDependency(Transaction transaction,Node node) {
            this.transactions = new ArrayList<>();
            transactions.add(transaction);
            this.addresses = new ArrayList<>();
            addresses.add(transaction.fromAddress);
            addresses.add(transaction.toAddress);
            this.graph = new Graph();
            this.graph.nodes=new HashMap<>();
            this.graph.nodes.put(node.getID(),node);
        }
        public SubDependency(List<Transaction> transactions, List<String> addresses, Graph graph) {
            this.transactions = transactions;
            this.addresses = addresses;
            this.graph = graph;
        }

        public  List<Transaction> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        public List<String> getAddresses() {
            return addresses;
        }

        public void setAddresses(List<String> addresses) {
            this.addresses = addresses;
        }

        public Graph getGraph() {
            return graph;
        }

        public void setGraph(Graph graph) {
            this.graph = graph;
        }
    }

    class Graph {
        Map<String, Node> nodes;

        public void setNodes(Map<String, Node> mergednodes) {
            this.nodes=mergednodes;
        }
    }

    class Node {
        Transaction transaction;
        String ID;
        int inDegree;
        int outDegree;
        List<Node> children;

        List<Node> parent;

        public Transaction getTransaction() {
            return transaction;
        }

        public void setTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        public String getID() {
            return ID;
        }

        public void setID(String ID) {
            this.ID = ID;
        }

        public int getInDegree() {
            return inDegree;
        }

        public void setInDegree(int inDegree) {
            this.inDegree = inDegree;
        }

        public int getOutDegree() {
            return outDegree;
        }

        public void setOutDegree(int outDegree) {
            this.outDegree = outDegree;
        }

        public List<Node> getChildren() {
            return children;
        }

        void addChild(Node child) {
            children.add(child);
            child.inDegree++;
            outDegree++;
        }
        void addParent(Node parentt) {
            parent.add(parentt);
        }
        void removeChild(Node child) {
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i).ID.equals(child.ID)) {
                    children.remove(i);
                    child.inDegree--;
                    outDegree--;
                    return;
                }
            }
        }
    }
    public  TransactionDependency createTransactionDependency(List<Transaction> transactions) {
        TransactionDependency transactionDependency=new TransactionDependency();
        int i=1;
        //遍历每条交易
        for (Transaction t : transactions) {
            //设置交易node
            Node node = new Node();
            node.setID(t.txid);
            node.setTransaction(t);
            node.setInDegree(0);
            node.setOutDegree(0);
            node.children = new ArrayList<>();
            node.parent = new ArrayList<>();
            //用来放有相关的地址的sub
            List<SubDependency> subDe = new ArrayList<>();
            //假如是第一个交易
            if(i==1){
            //List<SubDependency> subDependencies1=new ArrayList<>();
            // 遍历 subDependencies,把涉及的子交易依赖图拎出来
            //其实最多应该是两个相关，一个和fromaddress另一个和toaddress相关
                SubDependency subDependency = new SubDependency(t, node);
                //UUID.randomUUID().toString()可以生成唯一的时间戳
                //subDependency.subid=UUID.randomUUID().toString();
                transactionDependency.subDependencies=new ArrayList<>();
                transactionDependency.subDependencies.add(subDependency);
                i++;
            }else {
            for (SubDependency subDependency : transactionDependency.subDependencies) {

                if (subDependency.addresses.contains(t.fromAddress)) {
                    subDe.add(subDependency);
                    //subDependencies1.add(entry.getValue());
                } else if (subDependency.addresses.contains(t.toAddress)) {
                    subDe.add(subDependency);
                    //subDependencies1.add(entry.getValue());
                }
                // 处理每个 SubDependency 对象
                // ...
            }
            //处理每个相关的sub
            //判断有几个涉及
            //假如没有，重新新建一个
            if (subDe.size() == 0) {
                SubDependency subDependency = new SubDependency(t, node);
                //UUID.randomUUID().toString()可以生成唯一的时间戳
                //subDependency.subid=UUID.randomUUID().toString();
                transactionDependency.subDependencies.add(subDependency);
                //假如有一个涉及
            } else if (subDe.size() == 1) {
                SubDependency subDependency = subDe.get(0);
                //在这个交易列表中添加交易
                subDependency.transactions.add(t);
                //添加地址
                if(!subDependency.addresses.contains(t.fromAddress)){
                    subDependency.addresses.add(t.fromAddress);
                }
                if(!subDependency.addresses.contains(t.toAddress)){
                    subDependency.addresses.add(t.toAddress);
                }
                //遍历里边的每个交易
                for (Transaction tran : subDe.get(0).getTransactions()) {
                    //假如相同，添加节点
                    //tran为旧的
                    if (tran.fromAddress == t.fromAddress || tran.fromAddress == t.toAddress || tran.toAddress == t.toAddress || tran.toAddress == tran.fromAddress) {

                        subDependency.graph.nodes.put(node.getID(), node);
                        Node alnode = subDependency.graph.nodes.get(tran.txid);
                        //已经存在的节点添加孩子，并且修改出入度
                        alnode.addChild(node);
                        node.addParent(alnode);
                    }
                }
                transactionDependency.subDependencies.remove(subDe.get(0));
                transactionDependency.subDependencies.add(subDependency);
            } else {
                SubDependency subDependency = new SubDependency();
                //subDependency.subid=UUID.randomUUID().toString();
                List<Transaction> mergedtransactions = new ArrayList<>();
                List<String> mergedAddresses = new ArrayList<>();
                Graph mergedgraph = new Graph();
                Map<String, Node> mergednodes = new HashMap<>();
                for (SubDependency subDependency2 : subDe) {
                    //SubDependency subDependency1=transactionDependency.subDependencies.get(key);
                    mergedtransactions.addAll(subDependency2.getTransactions());
                    mergedAddresses.addAll(subDependency2.getAddresses());
                    mergednodes.putAll(subDependency2.getGraph().nodes);
                    //删除
                    transactionDependency.subDependencies.remove(subDependency2);
                }
                mergedtransactions.add(t);
                //添加地址
                if(!mergedAddresses.contains(t.fromAddress)){
                    subDependency.addresses.add(t.fromAddress);
                }
                if(!mergedAddresses.contains(t.toAddress)){
                    subDependency.addresses.add(t.toAddress);
                }
                subDependency.setTransactions(mergedtransactions);
                subDependency.setAddresses(mergedAddresses);
                //graph的合并
                mergedgraph.setNodes(mergednodes);
                subDependency.setGraph(mergedgraph);
                //遍历里边的每个交易
                for (Transaction tran : subDependency.transactions) {
                    //假如相同，添加节点
                    //tran为旧的
                    if (tran.fromAddress == t.fromAddress || tran.fromAddress == t.toAddress || tran.toAddress == t.toAddress || tran.toAddress == tran.fromAddress) {
                        subDependency.graph.nodes.put(node.getID(), node);
                        Node alnode = subDependency.graph.nodes.get(tran.txid);
                        //已经存在的节点添加孩子，并且修改出入度
                        alnode.addChild(node);
                        node.addParent(alnode);
                    }
                }
                transactionDependency.subDependencies.add(subDependency);
            }
        }
            //合并
        }
        return transactionDependency;
    }
}
