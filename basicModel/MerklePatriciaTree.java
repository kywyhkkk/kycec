package org.sde.cec.basicModel;

import org.sde.cec.util.hashCreat;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static java.util.Arrays.copyOf;

public class MerklePatriciaTree {
    public int db;
    public BranchNode root;
    public int cnt;

    // Compact编码首先将Hex尾部标记byte去掉，然后将原本每2 nibble的数据合并到1byte；
    // 增添1byte在输出数据头部以放置Compact格式标记位；
    // 如果输入Hex格式字符串有效长度为奇数，还可以将Hex字符串的第一个nibble放置在标记位byte里的低4bit。


    //返回这个hash是否有标志位
    public boolean hasTerm(byte[] s){
        return (s.length>0)&&(s[s.length-1]==16);
    }


    public void decodeNibbles(byte[] nibbles,byte[] bytes){
        for(int bi=0,ni=0;ni<nibbles.length;bi=bi+1,ni=ni+2){
            bytes[bi] = (byte) (nibbles[ni]<<4 | nibbles[ni+1]);
        }
    }


    //转码
    public byte[] hexToCompact(byte[] hex){
        byte terminator = 0;
        if(hasTerm(hex)){
            terminator =1;
            byte[] res = null;
            for(int i=0;i<hex.length;++i){
                res[i]=hex[i];
            }
            hex=res;
        }
        byte[] buf=null;
        for(int i=0;i<=hex.length/2+1;++i){
            buf[i]=0;
        }
        buf[0] = (byte) ((byte)terminator << (byte)5);
        if((hex[hex.length]&1) == 1){
            buf[0] |= 1<<4;
            buf[0] |= hex[0];
            byte[] newhex = new byte[hex.length-1];
            System.arraycopy(hex,1,newhex,0,hex.length-1);
            hex = newhex;
        }
        byte[] bu = new byte[buf.length-1];
        for(int i=1;i<=buf.length;++i){
            bu[i-1] = buf[i];
        }
        decodeNibbles(hex,bu);
        return buf;
    }

    //转码
    public byte[] keybytesToHex(byte[] str){
        int l=str.length*2+1;
        byte[] nibbles = new byte[1];
        for(int i=0,b=0;i<=str.length-1;++i,++b){
            nibbles[i*2] = (byte) (b/16);
            nibbles[i*2+1] = (byte) (b%16);
        }
        nibbles[l-1] =16;
        return nibbles;
    }

    //转码
    public byte[] compactToHex(byte[] compact){
        byte[] tmp = keybytesToHex(compact);
        byte[] base = new byte[tmp.length-1];
        for(int i=0;i<=tmp.length-1;++i){
            base[i] = tmp[i];
        }
        if(base[0]>=2){
            byte[] newArray = copyOf(base, base.length + 1);
            newArray[base.length] = 16;
            base = newArray;
        }
        byte chop = (byte) (2-base[0]&1);
        byte[] newBase = new byte[base.length - chop];
        System.arraycopy(base, chop, newBase, 0, newBase.length);
        base = newBase;
        return base;
    }


    //转码
    public byte[] hexToKeybytes(byte[] hex){
        if(hasTerm(hex)){
            byte[] newArray = copyOf(hex, hex.length - 1);

        }
        if((hex.length&1)!=0){
            throw new RuntimeException("can't convert hex key of odd length");
        }
        byte[] key = new byte[(hex.length+1)/2];
        decodeNibbles(hex,key);
        return key;
    }

    public byte[] concat(byte[] x,byte[] y){
        byte[] tmp = new byte[x.length+y.length];
        for(int i=0;i<x.length;++i){
            tmp[i] = x[i];
        }
        for(int i=0;i<y.length;++i){
            tmp[x.length+i] = y[i];
        }
        return tmp;
    }

    //求出两个编码的公共前缀长度
    public int prefixLen(byte[] a,byte[] b){
        int i=0;
        int len=0;
        len = a.length;
        if(b.length<len){
            len=b.length;
        }
        for(;i<len;++i){
            if(a[i]!=b[i]){
                break;
            }
        }
        return i;
    }

    //初始化
    public MerklePatriciaTree(){
        this.root = new BranchNode();
    }

    //需要自定义数据时的初始化
    public MerklePatriciaTree(byte[] s,int db)throws Exception{
        if(db==0){
            throw new Exception("trie.New called without a database");
        }
        this.db = db;
        this.root = new BranchNode();
    }

    public boolean ins(Node cur,byte[] prefix, byte[] key, String value,String path,String keyz) throws NoSuchAlgorithmException {

        if(cur instanceof MPTLeafNode){ //如果抵达了叶子节点直接插入
            hashCreat hashcreat = new hashCreat();
            cur.val = value;
            cur.fla = keyz;
            cur.hash = hashcreat.hashSHA_256(keyz);
            ((MPTLeafNode) cur).hashPath = path+cur.hash;
//            System.out.println(((MPTLeafNode) cur).hashPath);
//            for(int k=0;k<key.length;++k){
//                System.out.println(cur.key[k]);
//            }
            return true;
        }
        if(cur instanceof ExtensionNode){
            int jis=0;
            for(int i=0;i<cur.key.length;++i){
                if(i>=key.length-1){
                    break;
                }
                if(cur.key[i] == key[i]){
                    jis++;
                }else{
                    break;
                }
            }
            if(jis==0){

                return false;
            }
            if(jis>=cur.key.length){
//                if(jis==key.length){
//                    byte[] t1 = prefix;
//                    for(int i=0;i<cur.key.length-1;++i){
//                        t1[prefix.length+i] = key[i];
//                    }
//                    byte[] t2 = {key[key.length-1]};
//                    return ins(cur.child,t1,t2,value);
//                }else{

                    byte[] t1 = new byte[prefix.length+cur.key.length];
                    for(int i=0;i<prefix.length;++i){
                        t1[i] = prefix[i];
                    }
                    for(int i=0;i<cur.key.length;++i){
                        t1[prefix.length+i] = key[i];
                    }
                    byte[] t2 = new byte[key.length - cur.key.length];

                    for(int i=cur.key.length;i<key.length;++i){

                        t2[i-cur.key.length] = key[i];
                    }
                    return ins(cur.child,t1,t2,value,path+cur.hash,keyz);
//                }


            }else{
                byte[] tmp = new byte[jis];
                for(int i=0;i<jis;++i){
                    tmp[i] = cur.key[i];
                }
                byte[] tt = new byte[cur.key.length-jis];
                for(int i=jis;i<cur.key.length;++i){
                    tt[i-jis] = cur.key[i];
                }
                cur.key = tmp;
                BranchNode res = new BranchNode();
                res.branchList[0] = cur.child;
                res.pre[0] = tt;
                byte[] t1 = prefix;
                for(int i=0;i<jis;++i){
                    t1[prefix.length+i] = key[i];
                }
                byte[] t2 = null;
                for(int i=jis;i<key.length;++i){
                    t2[i-jis] = key[i];
                }
                return ins(cur.child,t1,t2,value,path+cur.hash,keyz);
            }
        }   //扩展结点的使用不多，查找前缀后直接往后跳就行
        if(cur instanceof BranchNode){
            for(int i=0;i<cur.cnt;++i){ //遍历分支节点所有的孩子
                Node tmp = cur.branchList[i];
//                System.out.println(tmp.val);
                //找一下公共前缀
                byte[] tmpb = cur.pre[i];
                int jis=0;
                for(int j=0;j<tmpb.length;++j){
                    if(tmpb[j] == key[j]){
                        jis++;
                    }else{
                        break;
                    }
                }
                if(jis==0){     //没有前缀就退出
                    continue;
                }

                if(jis>=tmpb.length){   //如果包含了完整的前缀，就直接往下找
                    byte[] t1 = new byte[prefix.length+jis];
                    for(int j=0;j<prefix.length;++j){
                        t1[j]=prefix[j];
                    }
                    for(int j=0;j<jis;++j){
                        t1[prefix.length+j] = key[j];
                    }
                    byte[] t2 = new byte[key.length-jis];
                    for(int j=jis;j<key.length;++j){
                        t2[j-jis] = key[j];
                    }
                    return ins(tmp,t1,t2,value,path+cur.hash,keyz);
                }else{      //否则重新构建MPT树，先新增分支节点之后改变后续结构
                    byte[] lin = new byte[jis];
                    for(int j=0;j<jis;++j){
                        lin[j] = tmpb[j];
                    }
                    byte[] tt = new byte[tmpb.length-jis];  //要重构的节点的值
                    for(int j=jis;j<tmpb.length;++j){
                        tt[j-jis] = tmpb[j];
                    }
                    cur.pre[i] = lin;
                    BranchNode res = new BranchNode();      //新建一个分支节点
                    res.cnt =1;                     //进行重构
                    res.branchList[0] = tmp;
                    res.pre[0] = tt;
                    hashCreat hashcreat = new hashCreat();
                    res.hash = hashcreat.hashSHA_256(Arrays.toString(key));
                    cur.branchList[i] = res;
                    byte[] t1 = new byte[prefix.length+jis];
                    for(int j=0;j<prefix.length;++j){
                        t1[j] = prefix[j];
                    }
                    for(int j=0;j<jis;++j){
                        t1[prefix.length+j] = key[j];
                    }
                    byte[] t2 = new byte[key.length-jis];
                    for(int j=jis;j<key.length;++j){
                        t2[j-jis] = key[j];
                    }
                    return ins(res,t1,t2,value,path+cur.hash,keyz);
                }
            }
            MPTLeafNode newleaf = new MPTLeafNode();    //如果没找到当前前缀，新建一个前缀
            cur.pre[cur.cnt] = key;
            cur.cnt++;
            cur.branchList[cur.cnt-1] = newleaf;
            hashCreat hashcreat = new hashCreat();
            cur.hash = hashcreat.hashSHA_256(prefix.toString());
//            for(int k=0;k<key.length;++k){
//                System.out.println(cur.pre[cur.cnt-1][k]);
//            }
            byte[] tt = new byte[prefix.length+key.length];
            for(int j=0;j<prefix.length;++j){
                tt[j] = prefix[j];
            }
            for(int j=0;j<key.length;++j){
                tt[j+prefix.length] = key[j];
            }
            return ins(newleaf,tt,null,value,path+cur.hash,keyz);
        }

        return false;
    }
    public boolean inse(String has,String val) throws Exception {   //外部引用这个函数插入
        int len = has.length();
        for(int i=len-1;i>=0;--i){      //String转byte
            byte[] tmp = new byte[i+1];
            byte[] res = new byte[len-i-1];
            for(int j=0;j<=i;++j){
                tmp[j] = (byte) (has.charAt(j) - '0');
            }
            for(int j=i+1;j<len;++j){
                res[j] = (byte) (has.charAt(j) - '0');
            }
//            System.out.println(i);
            boolean b = ins(this.root,res,tmp,val,"",has);
            return b;
        }
        return false;
    }


/*
    public boolean encodepath(String path,String val,Node cur){
        if(cur instanceof MPTLeafNode){
            if(path == ((MPTLeafNode) cur).hashPath && val == cur.val){
                return true;
            }
            return false;
        }
        if(cur instanceof ExtensionNode){
            return encodepath(path,val,cur.child);
        }
        if(cur instanceof BranchNode){
           for(Node i:cur.branchList){
               if()
           }
        }
        return false;
    }

    public boolean encopath(String path,String val){
//        int len = path.length();
//        int i = path.length()-1;
//            byte[] tmp = new byte[i+1];
//            byte[] res = new byte[len-i-1];
//            for(int j=0;j<=i;++j){
//                tmp[j] = (byte) (path.charAt(j) - '0');
//            }
//            for(int j=i+1;j<len;++j){
//                res[j] = (byte) (path.charAt(j) - '0');
//            }
//            String b = triepath(root,res,tmp,"",has);
//            return b;
//        return null;
        return encodepath(path,val,this.root);
    }

 */
    public String getPath(String has) throws Exception{ //外部引用这个函数查找路径
        int len = has.length();
        for(int i=len-1;i>=0;--i){
            byte[] tmp = new byte[i+1];
            byte[] res = new byte[len-i-1];
            for(int j=0;j<=i;++j){
                tmp[j] = (byte) (has.charAt(j) - '0');  //转为byte
            }
            for(int j=i+1;j<len;++j){
                res[j] = (byte) (has.charAt(j) - '0');
            }
            String b = triepath(root,res,tmp,"",has);
            return b;
        }
        return null;
    }

    public String triepath(Node cur, byte[] prefix, byte[] key,String p,String has){    //内部使用查找路径

        if(cur instanceof MPTLeafNode){     //叶子节点，返回当前路径
            if(has == cur.fla){
                ((MPTLeafNode) cur).hashPath = p+cur.hash;
                return p+cur.hash;
            }

        }
        if(cur instanceof ExtensionNode){   //扩展结点，直接往下找就行
            int jis=0;
            for(int i=0;i<cur.key.length;++i){
                if(i>=key.length){
                    break;
                }
                if(cur.key[i] == key[i]){
                    jis++;
                }else{
                    break;
                }
            }
            if(jis<cur.key.length){
                return null;
            }
            byte[] t1 = prefix;
            for(int i=0;i<cur.key.length;++i){
                t1[prefix.length+i] = key[i];
            }
            byte[] t2 = null;
            for(int i=cur.key.length;i<key.length;++i){
                t2[i-cur.key.length] = key[i];
            }
            return triepath(cur.child,t1,t2,p+cur.hash,has);
        }
        if(cur instanceof BranchNode){      //分支节点，查找是否含有一个当前前缀
            for(int i=0;i<cur.cnt;++i){
                byte[] tmp = cur.pre[i];
                int jis = prefixLen(tmp,key);
                if(jis<tmp.length){
                    continue;
                }
                if(jis>=tmp.length){
                    byte[] t1 = new byte[prefix.length+tmp.length];
                    for(int j=0;j<prefix.length;++j){
                        t1[j] = prefix[j];
                    }
                    for(int j=0;j<tmp.length;++j){
                        t1[prefix.length+j] = tmp[j];
                    }
                    byte[] t2 = new byte[key.length-tmp.length];
                    for(int j=tmp.length;j<key.length;++j){
                        t2[j-tmp.length] = key[j];
                    }
                    return triepath(cur.branchList[i],t1,t2,p+cur.hash,has);
                }
            }
        }
        return null;
    }
    public String serch(String has) throws Exception {  //外部引用这个函数查找一个key对应的值
        int len = has.length();
        for(int i=len-1;i>=0;--i){
            byte[] tmp = new byte[i+1];
            byte[] res = new byte[len-i-1];
            for(int j=0;j<=i;++j){
                tmp[j] = (byte) (has.charAt(j) - '0');
            }
            for(int j=i+1;j<len;++j){
                res[j] = (byte) (has.charAt(j) - '0');
            }
            String b = triege(root,res,tmp,has);
            return b;
        }
        return null;
    }



    public String triege(Node cur, byte[] prefix, byte[] key,String has){   //内部使用，查找value

        if(cur instanceof MPTLeafNode){     //叶子节点，直接查看当前值
//            for(byte i:key){
//                System.out.print(i);
//            }
//            System.out.println();
//            for(byte i:cur.key){
//                System.out.print(i);
//            }
//            System.out.println();
            if(cur.fla.equals(has))
                return cur.val;
        }
        if(cur instanceof ExtensionNode){   //扩展结点，往下找
            int jis=0;
            for(int i=0;i<cur.key.length;++i){
                if(i>=key.length){
                    break;
                }
                if(cur.key[i] == key[i]){
                    jis++;
                }else{
                    break;
                }
            }
            if(jis<cur.key.length){
                return null;
            }
            byte[] t1 = prefix;
            for(int i=0;i<cur.key.length;++i){
                t1[prefix.length+i] = key[i];
            }
            byte[] t2 = null;
            for(int i=cur.key.length;i<key.length;++i){
                t2[i-cur.key.length] = key[i];
            }
            return triege(cur.child,t1,t2,has);
        }
        if(cur instanceof BranchNode){  //分支节点
            for(int i=0;i<cur.cnt;++i){
                byte[] tmp = cur.pre[i];        //查找每一个存储的前缀，查看是否匹配
                int jis = prefixLen(tmp,key);
                if(jis<tmp.length){
                    continue;
                }
                if(jis>=tmp.length){
                    byte[] t1 = new byte[prefix.length+tmp.length];
                    for(int j=0;j<prefix.length;++j){
                        t1[j] = prefix[j];
                    }
                    for(int j=0;j<tmp.length;++j){
                        t1[prefix.length+j] = tmp[j];
                    }
                    byte[] t2 = new byte[key.length-tmp.length];
                    for(int j=tmp.length;j<key.length;++j){
                        t2[j-tmp.length] = key[j];
                    }
//                    System.out.println(jis+tmp.length);
//                    for(int j=0;j<t1.length;++j){
//                        System.out.println(t1[j]);
//                    }
                    return triege(cur.branchList[i],t1,t2,has);
                }
            }
        }
        return null;
    }
   /* private Node trieget(Node n, byte[] prefix, byte[] key) throws Exception{
        if(n == null){
            return this.root;
        }
        if (key.length == 0) {
            Node tmp = null;
            tmp.flag = false;
            for(int i=0;i<this.root.branchList.length;++i){
                if(this.root.branchList[i].key == prefix){
//                    this.root.branchList[i].value = value.val;
                    return this.root.branchList[i];
                }
            }
            return tmp;
//            Node tmp = null;
//            if (n instanceof MPTLeafNode) {
//
//                tmp = n;
//            }
//            return tmp;
        }

        if (n instanceof MPTLeafNode) {
            MPTLeafNode sn = (MPTLeafNode) n;
            int matchlen = prefixLen(key, sn.key);

            if (matchlen == sn.key.length) {

//                Node nn = result[1];
                Node result = trieget(sn, copyOf(prefix, prefix.length + matchlen), Arrays.copyOfRange(key, matchlen, key.length));
//                boolean dirty = (boolean) result[0];

                if (!result.flag) {
                    return result;
                }
                try {
                    result = trieget(sn, concat(prefix, Arrays.copyOfRange(key, 0, matchlen)), Arrays.copyOfRange(key, matchlen, key.length));
                    if (!result.flag) return result;
                } catch (Exception e) {
                    throw e;
                }
                return result;
            }
        }

        if (n instanceof BranchNode) {
            BranchNode fn = (BranchNode) n;
            int pd=0;
            MPTLeafNode res = new MPTLeafNode();
            res.key = prefix;
            for(int i=0;i<fn.branchList.length;++i){
                if(fn.branchList[i].key == prefix){
                    pd=1;
//                    res = fn.branchList[i];
                }
            }
            if(pd==0){

                Node tmp = null;
                tmp.flag = false;
                return tmp;
            }
            Node dirty = trieget(res, Arrays.copyOf(prefix, prefix.length + 1), Arrays.copyOfRange(key, 1, key.length));

            if (!dirty.flag) {
                return dirty;
            }
            return dirty;
        }

        if (n instanceof ExtensionNode) {
            Node tmp =null;
            if(((ExtensionNode) n).key==null){
                tmp.flag = false;
                return tmp;
            }
            if(((ExtensionNode) n).key != null){
                if(((ExtensionNode) n).key != prefix){
                    tmp.flag = false;
                }
                return tmp;
            }

            tmp.flag=true;
            tmp = trieget(((ExtensionNode) n).child,prefix,key);
            return tmp;
        }

        throw new RuntimeException(String.format("%s: invalid node: %s", n.getClass().getName(), n));
    }
    */
    public int newFlag() {
        cnt++;
        return cnt;
    }

}

