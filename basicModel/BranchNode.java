package org.sde.cec.basicModel;

public class BranchNode extends Node{
    //定长18，前15位为0-f，16位value即叶子结点hash，17位附加信息暂时用不到
//    public Node[] branchList=new MPTLeafNode[16];

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAppend() {
        return append;
    }

    public void setAppend(String append) {
        this.append = append;
    }

    public Node[] getBranchList() {
        return super.branchList;
    }

    public void setBranchList(MPTLeafNode[] branchList) {
        this.branchList = branchList;
    }
    //patricia表示该节点的前缀，若为叶子节点应该为hashpath的倒数第二位
    //若为扩展节点则为扩展节点key的第一位
  /*
    public Node[] addNode(MPTLeafNode leafNode, String patricia) throws JsonProcessingException, NoSuchAlgorithmException {
        switch (patricia){
            case "0":
                branchList[0]=leafNode;
                break;
            case "1":
                branchList[1]=leafNode;
                break;
            case "2":
                branchList[2]=leafNode;
                break;
            case "3":
                branchList[3]=leafNode;
                break;
            case "4":
                branchList[4]=leafNode;
                break;
            case "5":
                branchList[5]=leafNode;
                break;
            case "6":
                branchList[6]=leafNode;
                break;
            case "7":
                branchList[7]=leafNode;
                break;
            case "8":
                branchList[8]=leafNode;
                break;
            case "9":
                branchList[9]=leafNode;
                break;
            case "a":
                branchList[10]=leafNode;
                break;
            case "b":
                branchList[11]=leafNode;
                break;
            case "c":
                branchList[12]=leafNode;
                break;
            case "d":
                branchList[13]=leafNode;
                break;
            case "e":
                branchList[14]=leafNode;
                break;
            case "f":
                branchList[15]=leafNode;
                break;

        }
        String tempNodeStrValueTotal="";
        for(int i=0;i<16;i++){
            if(branchList[i]!=null){
                tempNodeStrValueTotal+=new ObjectMapper().writeValueAsString(branchList[i]);
            }
        }
        //更新分支节点value的hash
        hashCreat hc=new hashCreat();
        value= hc.hashSHA_256(tempNodeStrValueTotal);
        return super.branchList;

    }
   */
}
