import java.util.*;

public class InternalNode {

    public static final int MAX = 10;

    List<Integer> keys;
    List<Integer> keySlotArr;

    List<InternalNode> internalChildren;
    List<LeafNode> leafChildren;

    List<Integer> childrenSlotArr;

    public InternalNode(){
        keys = new ArrayList<>();
        keySlotArr = new ArrayList<>();
        internalChildren = new ArrayList<>();
        leafChildren = new ArrayList<>();
        childrenSlotArr = new ArrayList<>();
    }

    public void insert(int key) {
        //convert to a binary search later
        int i = keys.size() - 1;

        while(i >= 0 && keys.get(keySlotArr.get(i)) > key) { //no NVM reads because of slot array use
            i--;
        }
        i++;

        if(leafChildren.size() != 0) {
            if(leafChildren.get(childrenSlotArr.get(i)).keys.size() == MAX) { //1 read
                leafChildren.get(childrenSlotArr.get(i)).split(this); //1 read
                
                i = keys.size() - 1;

                while(i >= 0 && keys.get(keySlotArr.get(i)) > key) {
                    i--;
                }
                i++;
            }
            leafChildren.get(childrenSlotArr.get(i)).insert(key); //1 read
            
            WbTree.numReads += 2;
        }else{
            if(internalChildren.get(childrenSlotArr.get(i)).keys.size() == MAX) { //1 read
                internalChildren.get(childrenSlotArr.get(i)).split(this); //1 read
                
                i = keys.size() - 1;

                while(i >= 0 && keys.get(keySlotArr.get(i)) > key) {
                    i--;
                }
                i++;
            }
            internalChildren.get(childrenSlotArr.get(i)).insert(key); //1 read

            WbTree.numReads += 2;
        }
        WbTree.numReads += 1;
    }

    public void split(InternalNode parent) {
        //create the new node
        InternalNode newNode = new InternalNode();

        int promote = keys.get(keySlotArr.get(MAX/2));

        //put the larger half of the keys into the new node
        for(int i = MAX/2 + 1; i < MAX; i++) {
            newNode.keys.add(keys.get(keySlotArr.get(i))); 
            newNode.keySlotArr.add(newNode.keys.size() - 1); //slot array that can be loaded into RAM
        }

        //restructure the smaller half of the keys
        List<Integer> newKeys = new ArrayList<>();
        List<Integer> newSlotArr = new ArrayList<>();

        for(int i = 0; i < MAX/2; i++) {
            newKeys.add(keys.get(keySlotArr.get(i)));
            newSlotArr.add(newKeys.size() - 1);
        }

        keys = newKeys;
        keySlotArr = newSlotArr;

        //split the pointers
        if(leafChildren.size() != 0) { //this internal node had leaf children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.leafChildren.add(leafChildren.get(childrenSlotArr.get(i))); //1 write
                newNode.childrenSlotArr.add(newNode.leafChildren.size() - 1); //slot array which can be loaded into RAM
            }

            //restructure the smaller half of the keys
            List<LeafNode> newChildren = new ArrayList<>();
            List<Integer> newChildrenSlotArr = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(leafChildren.get(childrenSlotArr.get(i)));
                newChildrenSlotArr.add(newChildren.size() - 1);
            }

            leafChildren = newChildren;
            childrenSlotArr = newChildrenSlotArr;
        } else { //internal node had internal children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.internalChildren.add(internalChildren.get(childrenSlotArr.get(i))); //1 write
                newNode.childrenSlotArr.add(newNode.internalChildren.size() - 1); //slot array which can be loaded into RAM
            }

            //restructure the smaller half of the keys
            List<InternalNode> newChildren = new ArrayList<>();
            List<Integer> newChildrenSlotArr = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(internalChildren.get(childrenSlotArr.get(i)));
                newChildrenSlotArr.add(newChildren.size() - 1);
            }

            internalChildren = newChildren;
            childrenSlotArr = newChildrenSlotArr;
        }

        //key promotion
        if(parent.keys.size() == 0) { //parent is a brand new internal node
            parent.keys.add(promote); //1 write
            parent.keySlotArr.add(0); //slot array loaded into RAM

            parent.internalChildren.add(this); //1 write
            parent.internalChildren.add(newNode); //1 write

            parent.childrenSlotArr.add(0); //slot array loaded into RAM
            parent.childrenSlotArr.add(1);

            WbTree.numWrites += 3;
        } else { //parent already has this node, need to find the right insertion point for newNode
            //add the promoted key
            parent.keys.add(promote); //1 write
            WbTree.numWrites++;
            int slotArrIndex = parent.keys.size() - 1;

            int l = 0, r = parent.keySlotArr.size();
            while (l < r) {
                int mid = (l + r) / 2;
                if (parent.keys.get(parent.keySlotArr.get(mid)) < promote) { //1 read
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
                WbTree.numReads++;
            }

            parent.keySlotArr.add(l, slotArrIndex); //1 write
            WbTree.numWrites++;

            //find the right spot for newNode in the parent
            parent.internalChildren.add(newNode); //1 write
            WbTree.numWrites++;

            int childrenSlotArrIndex = parent.internalChildren.size() - 1;

            l = 0; 
            r = parent.childrenSlotArr.size();
            while (l < r) {
                int mid = (l + r) / 2;
                InternalNode searchNode = parent.internalChildren.get(parent.childrenSlotArr.get(mid)); //1 read
                if (searchNode.keys.get(searchNode.keySlotArr.get(searchNode.keySlotArr.size() - 1)) < promote) {
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
                WbTree.numReads++;
            }

            parent.childrenSlotArr.add(l, childrenSlotArrIndex);
        }
    }
}
