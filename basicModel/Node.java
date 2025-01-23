package org.sde.cec.basicModel;

public class Node {
    public boolean flag;
    public String val;
    public Node[] branchList=new Node[16];

    public byte[] key;

    public byte[][] pre = new byte[100][];
    public Node child;
    public String hash;
    public int cnt=0;

    public String fla;

    public String hashPath;

    public String value;
    public String append;
}
