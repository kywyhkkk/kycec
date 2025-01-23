package org.sde.cec.basicModel;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Scanner;


public class test {
    public static void main(String args[]) throws Exception {
        MerklePatriciaTree mpt = new MerklePatriciaTree();
        for(int i=101;i<=1111209;i++){
            //System.out.println(i);
            mpt.inse(String.valueOf(i),String.valueOf(i));
        }

       // mpt.inse("af","a");
       // mpt.inse("132","922892137");
        //mpt.inse("139","929");
       // mpt.inse(String.valueOf('b'),String.valueOf('b'));
       // mpt.inse(String.valueOf('c'),String.valueOf('c'));
       // mpt.inse(String.valueOf('d'),String.valueOf('d'));
       // mpt.inse(String.valueOf('e'),String.valueOf('e'));
       // mpt.inse(String.valueOf('f'),String.valueOf('f'));
        for(int i=101;i<=1111209;i++){
            //System.out.println(i +":"+mpt.serch(String.valueOf(i)));
            mpt.serch(String.valueOf(i));
        }
        //System.out.println(mpt.serch("af"));
       // System.out.println(mpt.serch(String.valueOf('b')));
        //System.out.println(mpt.serch(String.valueOf('c')));
       // System.out.println(mpt.serch(String.valueOf('d')));
       // System.out.println(mpt.serch(String.valueOf('e')));
       // System.out.println(mpt.serch(String.valueOf('f')));
      // System.out.println(mpt.serch("132"));
        //System.out.println(mpt.serch("139"));
        System.out.println(new ObjectMapper().writeValueAsString(mpt));
    }
}