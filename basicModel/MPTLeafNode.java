package org.sde.cec.basicModel;

public class MPTLeafNode extends Node{
    //存储完整的MPT前缀路径，即该叶子节点value的完整hash
//    public String hash;
    public MPTLeafNode(byte[] key, Node value, int newFlag) {
        super();
    }

    public MPTLeafNode() {

    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

//    public String getValue() {
//        return value;
//    }
//
//    public void setValue(String value) {
//        this.value = value;
//    }

    public String getHashPath() {
        return hashPath;
    }

    public void setHashPath(String hashPath) {
        this.hashPath = hashPath;
    }
}
